package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

public class VendaRepository {

    private static final String COLECAO = "vendas";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso(String vendaId);
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
}