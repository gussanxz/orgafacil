package com.gussanxz.orgafacil.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.data.model.Produto;

public class ProdutoRepository {

    private final FirebaseFirestore db;
    private final String uid;

    public ProdutoRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public void salvar(Produto produto, Callback callback) {
        if (uid == null) { callback.onErro("Usuário não logado"); return; }

        DocumentReference docRef;
        boolean isEdicao = (produto.getId() != null && !produto.getId().isEmpty());

        // CAMINHO: users/{uid}/vendas_produtos/{id}
        if (isEdicao) {
            docRef = db.collection("users").document(uid)
                    .collection("vendas_produtos").document(produto.getId());
        } else {
            docRef = db.collection("users").document(uid)
                    .collection("vendas_produtos").document();
            produto.setId(docRef.getId());
        }

        docRef.set(produto)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Produto atualizado!" : "Produto salvo!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // Adicione esta interface dentro da classe
    public interface ListaCallback {
        void onNovosDados(java.util.List<Produto> lista);
        void onErro(String erro);
    }

    // Adicione este método
    public com.google.firebase.firestore.ListenerRegistration listarTempoReal(ListaCallback callback) {
        if (uid == null) return null;

        return db.collection("users").document(uid)
                .collection("vendas_produtos") // Caminho dos produtos
                .orderBy("nome") // Ordena por nome
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage());
                        return;
                    }
                    java.util.List<Produto> lista = new java.util.ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(Produto.class);
                        // Garante IDs
                        int i = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            if(i < lista.size()) lista.get(i).setId(doc.getId());
                            i++;
                        }
                    }
                    callback.onNovosDados(lista);
                });
    }
}