package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CategoriaCatalogoRepository
 * * Gerencia os dados de categorias do catálogo de vendas.
 * Agora utiliza FirebaseSession para garantir a identidade do usuário.
 */
public class CategoriaCatalogoRepository {

    private final StorageReference storageRef;

    public CategoriaCatalogoRepository() {
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
     */
    public void salvar(@NonNull Categoria categoria, @Nullable Uri imagemUri, @NonNull Callback callback) {
        // Pensa fora da caixa: A validação de login acontece automaticamente
        // quando tentamos acessar o UID via Session. Se não houver login, o app
        // lança a exceção controlada da FirebaseSession.

        try {
            // 1. Se tem imagem NOVA, faz upload primeiro
            if (imagemUri != null) {
                uploadImagem(imagemUri, (url, erro) -> {
                    if (erro != null) {
                        callback.onErro("Erro ao subir imagem: " + erro);
                    } else {
                        categoria.setUrlImagem(url);
                        categoria.setIndexIcone(-1);
                        salvarNoFirestore(categoria, callback);
                    }
                });
            }
            // 2. Se usa ícone, limpa a URL de imagem antiga
            else if (categoria.getIndexIcone() != -1) {
                categoria.setUrlImagem(null);
                salvarNoFirestore(categoria, callback);
            }
            // 3. Edição simples sem alteração de mídia
            else {
                salvarNoFirestore(categoria, callback);
            }
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    private void salvarNoFirestore(Categoria categoria, Callback callback) {
        boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.vendasCategoriaDoc(categoria.getId());
        } else {
            // Usa o helper centralizado para pegar a coleção correta
            docRef = FirestoreSchema.vendasCategoriasCol().document();
            categoria.setId(docRef.getId());
        }

        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Categoria atualizada!" : "Categoria criada!"))
                .addOnFailureListener(e -> callback.onErro("Erro ao salvar: " + e.getMessage()));
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasCategoriasCol()
                    .orderBy("nome", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage());
                            return;
                        }
                        List<Categoria> lista = (snapshots != null) ? snapshots.toObjects(Categoria.class) : new ArrayList<>();
                        callback.onNovosDados(lista);
                    });
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
            return null;
        }
    }

    public void excluir(@NonNull String idCategoria, @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasCategoriaDoc(idCategoria)
                    .delete()
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Categoria excluída com sucesso!"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao excluir: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    // --- Helpers Privados ---

    private void uploadImagem(Uri uri, BiConsumer<String, String> callback) {
        String nomeArquivo = UUID.randomUUID().toString() + ".jpg";

        // Note que agora pedimos o UID diretamente para a Session aqui no upload também
        StorageReference fotoRef = storageRef
                .child("images")
                .child("users")
                .child(FirebaseSession.getUserId())
                .child("vendas")
                .child("categorias")
                .child(nomeArquivo);

        fotoRef.putFile(uri)
                .addOnSuccessListener(task -> fotoRef.getDownloadUrl()
                        .addOnSuccessListener(url -> callback.accept(url.toString(), null)))
                .addOnFailureListener(e -> callback.accept(null, e.getMessage()));
    }

    private interface BiConsumer<T, U> { void accept(T t, U u); }
}