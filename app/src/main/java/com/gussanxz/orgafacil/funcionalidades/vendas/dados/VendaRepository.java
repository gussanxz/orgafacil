package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.util.ArrayList;
import java.util.List;

public class VendaRepository {

    private static final String COLECAO = "vendas";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso(String vendaId);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<VendaModel> lista);
        void onErro(String erro);
    }

    public void salvar(@NonNull VendaModel venda, @NonNull Callback callback) {
        String id = venda.getId();

        if (id == null || id.trim().isEmpty()) {
            id = firestore.collection(COLECAO).document().getId();
            venda.setId(id);
        }

        firestore.collection(COLECAO)
                .document(id)
                .set(venda, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSucesso(id))
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao salvar venda."
                ));
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        return firestore.collection(COLECAO)
                .orderBy("dataHoraMillis", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage() != null
                                ? error.getMessage()
                                : "Erro ao listar vendas.");
                        return;
                    }

                    List<VendaModel> lista = new ArrayList<>();

                    if (snapshot != null) {
                        lista = snapshot.toObjects(VendaModel.class);

                        for (int i = 0; i < lista.size(); i++) {
                            VendaModel venda = lista.get(i);
                            if (venda != null && (venda.getId() == null || venda.getId().trim().isEmpty())) {
                                venda.setId(snapshot.getDocuments().get(i).getId());
                            }
                        }
                    }

                    callback.onNovosDados(lista);
                });
    }
}