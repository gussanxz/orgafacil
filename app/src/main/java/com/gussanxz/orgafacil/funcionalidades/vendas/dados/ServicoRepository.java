package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ServicoModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ServicoRepository
 *
 * Responsabilidade:
 * - CRUD e Listener em tempo real para "Serviços" no módulo de vendas.
 * - Utiliza FirebaseSession para identidade e FirestoreSchema para caminhos.
 */
public class ServicoRepository {

    // Nomes de coleções centralizados para facilitar manutenção
    private static final String COL_SERVICOS = "vendas_servicos";

    public ServicoRepository() {
        // Agora o repositório é "stateless" (sem estado de UID fixo)
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<ServicoModel> lista);
        void onErro(String erro);
    }

    /**
     * Salvar ou Editar serviço
     * Utiliza o myUserDoc() do Schema que já resolve o UID internamente.
     */
    public void salvar(@NonNull ServicoModel servicoModel, @NonNull Callback callback) {
        try {
            boolean isEdicao = (servicoModel.getId() != null && !servicoModel.getId().isEmpty());
            DocumentReference docRef;

            if (isEdicao) {
                docRef = FirestoreSchema.myUserDoc()
                        .collection(COL_SERVICOS)
                        .document(servicoModel.getId());
            } else {
                docRef = FirestoreSchema.myUserDoc()
                        .collection(COL_SERVICOS)
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

        } catch (IllegalStateException e) {
            callback.onErro("Sessão expirada: " + e.getMessage());
        }
    }

    /**
     * Listener em tempo real para listagem
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.myUserDoc()
                    .collection(COL_SERVICOS)
                    .orderBy("descricao", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro no listener");
                            return;
                        }

                        List<ServicoModel> lista = new ArrayList<>();
                        if (snapshots != null) {
                            // Converte documentos para objetos da classe ServicoModel
                            lista = snapshots.toObjects(ServicoModel.class);

                            // Sincroniza os IDs dos documentos com os objetos
                            for (int i = 0; i < snapshots.size(); i++) {
                                if (i < lista.size()) {
                                    lista.get(i).setId(snapshots.getDocuments().get(i).getId());
                                }
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