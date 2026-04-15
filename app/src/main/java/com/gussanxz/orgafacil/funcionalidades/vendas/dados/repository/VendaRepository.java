package com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.main.OrgaFacilApp;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.VendaModel;

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

    // --- ATUALIZADO: total agora é int ---
    public interface TotaisCallback {
        void onTotais(int qtd, int total);
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

    private static final String PREFS_VENDA     = "venda_prefs";
    private static final String KEY_ULTIMO_NUM  = "ultimo_numero_local";

    private void gerarProximoNumero(@NonNull NumeroCallback callback) {
        try {
            DocumentReference contadorRef = FirestoreSchema.vendasResumoDoc()
                    .collection("config")
                    .document("sequencia");

            SharedPreferences prefs = OrgaFacilApp.instance()
                    .getSharedPreferences(PREFS_VENDA, android.content.Context.MODE_PRIVATE);
            int ultimoLocal  = prefs.getInt(KEY_ULTIMO_NUM, 0);
            int fallbackLocal = ultimoLocal + 1;

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
                    .addOnSuccessListener(numero -> {
                        prefs.edit().putInt(KEY_ULTIMO_NUM, (int)(long) numero).apply();
                        callback.onNumero((int)(long) numero);
                    })
                    .addOnFailureListener(e -> {
                        prefs.edit().putInt(KEY_ULTIMO_NUM, fallbackLocal).apply();
                        contadorRef.set(
                                Collections.singletonMap("ultimoNumero", (long) fallbackLocal),
                                SetOptions.merge()
                        );
                        callback.onNumero(fallbackLocal);
                    });
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

    public void alternarStatus(@NonNull VendaModel venda, @NonNull Callback callback) {
        String statusAtual = venda.getStatus();

        if (VendaModel.STATUS_FINALIZADA.equals(statusAtual)) {
            atualizarStatus(venda.getId(), VendaModel.STATUS_CANCELADA, callback);
        } else if (VendaModel.STATUS_CANCELADA.equals(statusAtual)) {
            atualizarStatus(venda.getId(), VendaModel.STATUS_FINALIZADA, callback);
        } else {
            callback.onErro("Operação inválida: apenas vendas finalizadas ou canceladas podem ter o status alternado.");
        }
    }

    // -----------------------------------------------------------
    // HELPER PRIVADO E BUSCAS
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

    /** ATUALIZADO: Soma em int com compatibilidade para Double legado */
    public void buscarTotaisFinalizadasDoCaixa(@NonNull String caixaId,
                                               @NonNull TotaisCallback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("caixaId", caixaId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int qtd = 0;
                        int totalCentavos = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (VendaModel.STATUS_FINALIZADA.equals(doc.getString("status"))) {
                                qtd++;
                                // Tenta ler como Long (padrão int centavos)
                                Long vLong = doc.getLong("valorTotal");
                                if (vLong != null) {
                                    totalCentavos += vLong.intValue();
                                } else {
                                    // Fallback: tenta ler como Double legado e converte
                                    Double vDouble = doc.getDouble("valorTotal");
                                    if (vDouble != null) totalCentavos += (int) Math.round(vDouble * 100);
                                }
                            }
                        }
                        callback.onTotais(qtd, totalCentavos);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao calcular totais."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    /** ATUALIZADO: Soma em int com compatibilidade para Double legado */
    public void buscarTotaisVendasLegadas(@NonNull TotaisCallback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int qtd = 0;
                        int totalCentavos = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Object caixaId = doc.get("caixaId");
                            boolean legado = caixaId == null || "caixa_0".equals(caixaId);
                            if (!legado) continue;

                            if (VendaModel.STATUS_FINALIZADA.equals(doc.getString("status"))) {
                                qtd++;
                                Long vLong = doc.getLong("valorTotal");
                                if (vLong != null) {
                                    totalCentavos += vLong.intValue();
                                } else {
                                    Double vDouble = doc.getDouble("valorTotal");
                                    if (vDouble != null) totalCentavos += (int) Math.round(vDouble * 100);
                                }
                            }
                        }
                        callback.onTotais(qtd, totalCentavos);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao calcular totais legados."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    public ListenerRegistration escutarVendasDoCaixa(@NonNull String caixaId,
                                                     @NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("caixaId", caixaId)
                    .orderBy("dataHoraAberturaMillis", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao carregar vendas do caixa.");
                            return;
                        }
                        callback.onNovosDados(extrairLista(snapshot));
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

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
}