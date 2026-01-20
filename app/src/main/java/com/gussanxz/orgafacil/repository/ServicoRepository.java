package com.gussanxz.orgafacil.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.model.Servico;

public class ServicoRepository {

    private final FirebaseFirestore db;
    private final String uid;

    public ServicoRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public void salvar(Servico servico, Callback callback) {
        if (uid == null) { callback.onErro("Usuário não logado"); return; }

        DocumentReference docRef;
        boolean isEdicao = (servico.getId() != null && !servico.getId().isEmpty());

        // CAMINHO: users/{uid}/vendas_servicos/{id}
        if (isEdicao) {
            docRef = db.collection("users").document(uid)
                    .collection("vendas_servicos").document(servico.getId());
        } else {
            docRef = db.collection("users").document(uid)
                    .collection("vendas_servicos").document();
            servico.setId(docRef.getId());
        }

        docRef.set(servico)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Serviço atualizado!" : "Serviço salvo!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // Adicione esta interface dentro da classe
    public interface ListaCallback {
        void onNovosDados(java.util.List<Servico> lista);
        void onErro(String erro);
    }

    // Adicione este método
    public com.google.firebase.firestore.ListenerRegistration listarTempoReal(ListaCallback callback) {
        if (uid == null) return null;

        return db.collection("users").document(uid)
                .collection("vendas_servicos") // Caminho dos serviços
                .orderBy("descricao") // Ordena por descrição
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage());
                        return;
                    }
                    java.util.List<Servico> lista = new java.util.ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(Servico.class);
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