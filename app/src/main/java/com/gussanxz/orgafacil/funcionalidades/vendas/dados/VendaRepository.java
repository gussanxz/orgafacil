package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.util.ArrayList;
import java.util.List;

public class VendaRepository {

    public interface Callback {
        void onSucesso(String vendaId);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<VendaModel> lista);
        void onErro(String erro);
    }

    public void salvar(@NonNull VendaModel venda, @NonNull Callback callback) {
        String vendaId = venda.getId();

        if (vendaId == null || vendaId.trim().isEmpty()) {
            vendaId = FirestoreSchema.vendasVendasCol().document().getId();
            venda.setId(vendaId);
        }

        final String vendaIdFinal = vendaId;

        FirestoreSchema.vendasVendasCol()
                .document(vendaIdFinal)
                .set(venda, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSucesso(vendaIdFinal))
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao salvar venda."
                ));
    }

    public ListenerRegistration listarEmAberto(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("status", VendaModel.STATUS_EM_ABERTO)
                    .orderBy("dataHoraAberturaMillis", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao listar vendas em aberto.");
                            return;
                        }
                        List<VendaModel> lista = new ArrayList<>();
                        if (snapshot != null) {
                            lista = snapshot.toObjects(VendaModel.class);
                            for (int i = 0; i < lista.size(); i++) {
                                VendaModel v = lista.get(i);
                                if (v != null && (v.getId() == null || v.getId().trim().isEmpty())) {
                                    v.setId(snapshot.getDocuments().get(i).getId());
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
    
    public void atualizarStatus(@NonNull String vendaId,
                                @NonNull String novoStatus,
                                @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(vendaId)
                    .update("status", novoStatus)
                    .addOnSuccessListener(unused -> callback.onSucesso(vendaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao atualizar status."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

}