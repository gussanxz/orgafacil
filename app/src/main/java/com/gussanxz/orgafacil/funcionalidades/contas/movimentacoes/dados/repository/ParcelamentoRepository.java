package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.ArrayList;
import java.util.List;

// ParcelamentoRepository.java
public class ParcelamentoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ParcelasCallback {
        void onSucesso(List<MovimentacaoModel> parcelas);
        void onErro(String erro);
    }

    /** Busca todas as parcelas de um grupo pelo recorrencia_id, ordenadas por parcela_atual */
    public void buscarParcelas(String recorrenciaId, ParcelasCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", recorrenciaId)
                .orderBy("parcela_atual", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<MovimentacaoModel> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());
                        lista.add(m);
                    }
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }
}