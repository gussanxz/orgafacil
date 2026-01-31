package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoriaCatalogoRepository {

    private final String uid;
    private final StorageReference storageRef;

    public CategoriaCatalogoRepository() {
        this.uid = FirestoreSchema.uidOrNull(); // Usa o helper do Schema
        this.storageRef = FirebaseStorage.getInstance().getReference();
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<Categoria> lista);
        void onErro(String erro);
    }

    /**
     * Salva Categoria do Catálogo (Vendas).
     * Caminho: usuarios/{uid}/moduloSistema/vendas/vendas_categorias/{id}
     */
    public void salvar(
            @NonNull Categoria categoria,
            @Nullable Uri imagemUri,
            @NonNull Callback callback
    ) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        // 1. Se tem imagem NOVA, faz upload primeiro
        if (imagemUri != null) {
            uploadImagem(imagemUri, (url, erro) -> {
                if (erro != null) {
                    callback.onErro("Erro ao subir imagem: " + erro);
                } else {
                    categoria.setUrlImagem(url);
                    categoria.setIndexIcone(-1); // Garante que usa a foto
                    salvarNoFirestore(categoria, callback);
                }
            });
        }
        // 2. Se NÃO tem imagem nova, mas o usuário escolheu usar ÍCONE
        else if (categoria.getIndexIcone() != -1) {
            categoria.setUrlImagem(null);
            salvarNoFirestore(categoria, callback);
        }
        // 3. Mantém como estava
        else {
            salvarNoFirestore(categoria, callback);
        }
    }

    private void salvarNoFirestore(Categoria categoria, Callback callback) {
        boolean isEdicao = categoria.getId() != null && !categoria.getId().isEmpty();
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.vendasCategoriaDoc(uid, categoria.getId());
        } else {
            docRef = FirestoreSchema.vendasCategoriasCol(uid).document();
            categoria.setId(docRef.getId());
        }

        docRef.set(categoria)
                .addOnSuccessListener(aVoid ->
                        callback.onSucesso(isEdicao ? "Categoria atualizada!" : "Categoria criada!")
                )
                .addOnFailureListener(e ->
                        callback.onErro("Erro ao salvar: " + e.getMessage())
                );
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        if (uid == null) return null;

        return FirestoreSchema.vendasCategoriasCol(uid)
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage());
                        return;
                    }

                    List<Categoria> lista =
                            (snapshots != null)
                                    ? snapshots.toObjects(Categoria.class)
                                    : new ArrayList<>();

                    callback.onNovosDados(lista);
                });
    }

    public void excluir(@NonNull String idCategoria, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        FirestoreSchema.vendasCategoriaDoc(uid, idCategoria)
                .delete()
                .addOnSuccessListener(aVoid ->
                        callback.onSucesso("Categoria excluída com sucesso!")
                )
                .addOnFailureListener(e ->
                        callback.onErro("Erro ao excluir: " + e.getMessage())
                );
    }

    // --- Helpers Privados ---

    private void uploadImagem(Uri uri, BiConsumer<String, String> callback) {
        String nomeArquivo = UUID.randomUUID() + ".jpg";

        StorageReference fotoRef = storageRef
                .child("images")
                .child("users")
                .child(uid)
                .child("vendas")
                .child("categorias")
                .child(nomeArquivo);

        fotoRef.putFile(uri)
                .addOnSuccessListener(task ->
                        fotoRef.getDownloadUrl()
                                .addOnSuccessListener(url ->
                                        callback.accept(url.toString(), null)
                                )
                )
                .addOnFailureListener(e ->
                        callback.accept(null, e.getMessage())
                );
    }

    private interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}

// Nota: Para um sistema perfeito, futuramente poderíamos deletar a imagem
        // do Storage aqui também, mas deletando do banco já some do app.
