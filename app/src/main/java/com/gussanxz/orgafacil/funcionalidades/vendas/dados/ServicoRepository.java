package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ServicoModel;

/**
 * ServicoRepository
 *
 * O que faz:
 * - CRUD + listener de "Serviços" (módulo vendas) mantendo o caminho atual:
 *   {ROOT}/{uid}/vendas_servicos/{id}
 *
 * Impacto:
 * - Agora respeita FirestoreSchema.ROOT (ex: "usuarios" / "teste")
 * - Mantém compatibilidade com telas atuais (não migra ainda para catalogoVendas)
 * - Centraliza path via FirestoreSchema.userDoc(uid) evitando hardcode "users"
 */
public class ServicoRepository {

    private final FirebaseFirestore db;
    private final String uid;

    public ServicoRepository() {
        this.db = FirestoreSchema.db();
        this.uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    // Adicione esta interface dentro da classe
    public interface ListaCallback {
        void onNovosDados(java.util.List<ServicoModel> lista);
        void onErro(String erro);
    }

    /**
     * Salvar/Editar serviço
     *
     * Caminho atual preservado:
     * {ROOT}/{uid}/vendas_servicos/{id}
     */
    public void salvar(@NonNull ServicoModel servicoModel, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        DocumentReference docRef;
        boolean isEdicao = (servicoModel.getId() != null && !servicoModel.getId().isEmpty());

        if (isEdicao) {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection("vendas_servicos")
                    .document(servicoModel.getId());
        } else {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection("vendas_servicos")
                    .document();
            servicoModel.setId(docRef.getId());
        }

        docRef.set(servicoModel)
                .addOnSuccessListener(aVoid ->
                        callback.onSucesso(isEdicao ? "Serviço atualizado!" : "Serviço salvo!")
                )
                .addOnFailureListener(e ->
                        callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao salvar serviço")
                );
    }

    /**
     * Listener em tempo real
     *
     * Impacto:
     * - Atualiza a UI em tempo real (custo: cada mudança gera evento)
     * - Bom para lista de serviços/catálogo
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        if (uid == null) return null;

        return FirestoreSchema.userDoc(uid)
                .collection("vendas_servicos")
                .orderBy("descricao") // mantém seu comportamento atual
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro no listener");
                        return;
                    }

                    java.util.List<ServicoModel> lista = new java.util.ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(ServicoModel.class);

                        // Garante IDs (porque toObjects pode não preencher id)
                        int i = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            if (i < lista.size()) lista.get(i).setId(doc.getId());
                            i++;
                        }
                    }

                    callback.onNovosDados(lista);
                });
    }
}