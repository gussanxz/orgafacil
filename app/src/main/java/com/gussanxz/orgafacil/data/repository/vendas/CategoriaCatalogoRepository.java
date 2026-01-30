package com.gussanxz.orgafacil.data.repository.vendas;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.gussanxz.orgafacil.data.config.FirestoreSchema;
import com.gussanxz.orgafacil.data.model.Categoria;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoriaCatalogoRepository {

    private final String uid;
    private final StorageReference storageRef;

    // Nome da coleção de Vendas
    private static final String COL_VENDAS_CATEGORIAS = "vendas_categorias";

    public CategoriaCatalogoRepository() {
        this.uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
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
     * Suporta upload de imagem opcional.
     */
    public void salvar(@NonNull Categoria categoria, @Nullable Uri imagemUri, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        // Se tiver imagem, sobe antes
        if (imagemUri != null) {
            uploadImagem(imagemUri, (url, erro) -> {
                if (erro != null) {
                    callback.onErro("Erro ao subir imagem: " + erro);
                } else {
                    categoria.setUrlImagem(url);
                    categoria.setIndexIcone(-1); // Anula ícone
                    salvarNoFirestore(categoria, callback);
                }
            });
        } else {
            salvarNoFirestore(categoria, callback);
        }
    }

    private void salvarNoFirestore(Categoria categoria, Callback callback) {
        boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());
        DocumentReference docRef;

        // Usa a estrutura de USER do schema
        if (isEdicao) {
            docRef = FirestoreSchema.userDoc(uid).collection(COL_VENDAS_CATEGORIAS).document(categoria.getId());
        } else {
            docRef = FirestoreSchema.userDoc(uid).collection(COL_VENDAS_CATEGORIAS).document();
            categoria.setId(docRef.getId());
        }

        // Regra de Vendas: Sobrescreve o objeto (.set sem merge)
        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Atualizado!" : "Criado!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        if (uid == null) return null;

        return FirestoreSchema.userDoc(uid)
                .collection(COL_VENDAS_CATEGORIAS)
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage());
                        return;
                    }
                    List<Categoria> lista = (snapshots != null) ? snapshots.toObjects(Categoria.class) : new ArrayList<>();
                    callback.onNovosDados(lista);
                });
    }

    // --- Helpers Privados ---
    private void uploadImagem(Uri uri, BiConsumer<String, String> callback) {
        String nomeArquivo = UUID.randomUUID().toString() + ".jpg";
        StorageReference fotoRef = storageRef.child("images/users/" + uid + "/categorias/" + nomeArquivo);

        fotoRef.putFile(uri)
                .addOnSuccessListener(task -> fotoRef.getDownloadUrl()
                        .addOnSuccessListener(url -> callback.accept(url.toString(), null)))
                .addOnFailureListener(e -> callback.accept(null, e.getMessage()));
    }

    private interface BiConsumer<T, U> { void accept(T t, U u); }
}