package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    private interface NumeroCallback {
        void onNumero(int numero);
        void onErro(String erro);
    }

    // -----------------------------------------------------------
    // SALVAR
    // -----------------------------------------------------------

    public void salvar(@NonNull VendaModel venda, @NonNull Callback callback) {
        boolean isNova = venda.getId() == null || venda.getId().trim().isEmpty();

        if (isNova) {
            // Venda nova: gera número sequencial antes de salvar
            gerarProximoNumero(new NumeroCallback() {
                @Override
                public void onNumero(int numero) {
                    venda.setNumeroVenda(numero);
                    String vendaId = FirestoreSchema.vendasVendasCol().document().getId();
                    venda.setId(vendaId);
                    salvarNoFirestore(venda, callback);
                }

                @Override
                public void onErro(String erro) {
                    callback.onErro(erro);
                }
            });
        } else {
            // Atualização de venda existente — mantém o número que já tem
            salvarNoFirestore(venda, callback);
        }
    }

    private void salvarNoFirestore(@NonNull VendaModel venda, @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(venda.getId())
                    .set(venda, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onSucesso(venda.getId()))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao salvar venda."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // -----------------------------------------------------------
    // NUMERAÇÃO SEQUENCIAL
    // -----------------------------------------------------------

    private void gerarProximoNumero(@NonNull NumeroCallback callback) {
        try {
            // Contador em: vendas/resumo_geral/config/sequencia → { ultimoNumero: N }
            DocumentReference contadorRef = FirestoreSchema.vendasResumoDoc()
                    .collection("config")
                    .document("sequencia");

            FirestoreSchema.vendasResumoDoc()
                    .getFirestore()
                    .runTransaction(transaction -> {
                        DocumentSnapshot snap = transaction.get(contadorRef);
                        long proximo = 1;
                        if (snap.exists() && snap.getLong("ultimoNumero") != null) {
                            proximo = snap.getLong("ultimoNumero") + 1;
                        }
                        transaction.set(
                                contadorRef,
                                Collections.singletonMap("ultimoNumero", proximo),
                                SetOptions.merge()
                        );
                        return proximo;
                    })
                    .addOnSuccessListener(numero -> callback.onNumero((int)(long) numero))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao gerar número da venda."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // -----------------------------------------------------------
    // LISTAGENS
    // -----------------------------------------------------------

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasVendasCol()
                    .orderBy("dataHoraAberturaMillis", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao listar vendas.");
                            return;
                        }
                        callback.onNovosDados(extrairLista(snapshot));
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
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
                        callback.onNovosDados(extrairLista(snapshot));
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

    // -----------------------------------------------------------
    // ATUALIZAR STATUS
    // -----------------------------------------------------------

    public void atualizarStatus(@NonNull String vendaId,
                                @NonNull String novoStatus,
                                @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(vendaId)
                    .update("status", novoStatus)
                    .addOnSuccessListener(unused -> callback.onSucesso(vendaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao atualizar status."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // -----------------------------------------------------------
    // HELPER PRIVADO
    // -----------------------------------------------------------

    private List<VendaModel> extrairLista(
            com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<VendaModel> lista = new ArrayList<>();
        if (snapshot == null) return lista;

        lista = snapshot.toObjects(VendaModel.class);
        for (int i = 0; i < lista.size(); i++) {
            VendaModel v = lista.get(i);
            if (v != null && (v.getId() == null || v.getId().trim().isEmpty())) {
                v.setId(snapshot.getDocuments().get(i).getId());
            }
        }
        return lista;
    }

    public void buscarPorId(@NonNull String vendaId, @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(vendaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            callback.onSucesso(vendaId);
                        } else {
                            callback.onErro("Venda não encontrada.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar venda."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // Novo callback que retorna o objeto VendaModel completo
    public interface VendaCallback {
        void onVenda(VendaModel venda);
        void onErro(String erro);
    }

    public void buscarVendaPorId(@NonNull String vendaId, @NonNull VendaCallback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(vendaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            VendaModel venda = doc.toObject(VendaModel.class);
                            if (venda != null) {
                                venda.setId(doc.getId());
                                callback.onVenda(venda);
                            } else {
                                callback.onErro("Erro ao converter venda.");
                            }
                        } else {
                            callback.onErro("Venda não encontrada.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar venda."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // Novo método: escuta em tempo real as vendas finalizadas hoje
    public ListenerRegistration escutarVendasFinalizadasHoje(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("status", VendaModel.STATUS_FINALIZADA)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao carregar vendas do dia.");
                            return;
                        }
                        // Filtra localmente pelo dia de hoje
                        List<VendaModel> todas = extrairLista(snapshot);
                        List<VendaModel> hoje = new ArrayList<>();
                        String diaHoje = FirestoreSchema.diaKey(new Date());
                        for (VendaModel v : todas) {
                            String diaVenda = FirestoreSchema.diaKey(new Date(v.getDataHoraFechamentoMillis()));
                            if (diaHoje.equals(diaVenda)) {
                                hoje.add(v);
                            }
                        }
                        callback.onNovosDados(hoje);
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

    // Novo método: escuta em tempo real as vendas em aberto (para contagem no resumo)
    public ListenerRegistration escutarVendasEmAberto(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("status", VendaModel.STATUS_EM_ABERTO)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao carregar vendas em aberto.");
                            return;
                        }
                        callback.onNovosDados(extrairLista(snapshot));
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

    public void excluir(@NonNull String vendaId, @NonNull Callback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .document(vendaId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onSucesso(vendaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao excluir venda."
                    ));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }
}