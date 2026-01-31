package com.gussanxz.orgafacil.funcionalidades.comum.dados;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    private static final String COL_VENDAS_CATEGORIAS = "vendas_categorias";

    public interface CategoriaCallback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<Categoria> lista);
        void onErro(String erro);
    }

    public void salvar(@NonNull Categoria categoria, @NonNull CategoriaCallback callback) {
        try {
            boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());
            DocumentReference docRef = isEdicao
                    ? FirestoreSchema.myUserDoc().collection(COL_VENDAS_CATEGORIAS).document(categoria.getId())
                    : FirestoreSchema.myUserDoc().collection(COL_VENDAS_CATEGORIAS).document();

            if (!isEdicao) categoria.setId(docRef.getId());

            docRef.set(categoria)
                    .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Atualizado!" : "Criado!"))
                    .addOnFailureListener(e -> callback.onErro(e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro("Sessão expirada");
        }
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.myUserDoc()
                    .collection(COL_VENDAS_CATEGORIAS)
                    .orderBy("nome", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage());
                            return;
                        }
                        List<Categoria> lista = new ArrayList<>();
                        if (snapshots != null) {
                            lista = snapshots.toObjects(Categoria.class);
                            for (int i = 0; i < snapshots.size(); i++) {
                                lista.get(i).setId(snapshots.getDocuments().get(i).getId());
                            }
                        }
                        callback.onNovosDados(lista);
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }
}