package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.main.OrgaFacilApp;
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

    public interface TotaisCallback {
        void onTotais(int qtd, double total);
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
    // NUMERAÇÃO SEQUENCIAL  (com fallback offline)
    // -----------------------------------------------------------

    private static final String PREFS_VENDA     = "venda_prefs";
    private static final String KEY_ULTIMO_NUM  = "ultimo_numero_local";

    /**
     * Gera o próximo número de venda de forma resiliente a falhas de rede.
     *
     * Estratégia:
     *  1. Lê o último número local salvo nas SharedPreferences como fallback imediato.
     *  2. Tenta executar a transação Firestore (incremento atômico no servidor).
     *  3. Se a transação falha (offline / alta latência):
     *     – usa o fallback local (último número conhecido + 1);
     *     – persiste o fallback localmente para evitar duplicatas em reaberturas;
     *     – enfileira um set() no contador remoto (será sincronizado ao reconnectar).
     *  4. Se a transação tem sucesso, atualiza as SharedPreferences com o valor do servidor.
     *
     * Efeito: a venda é sempre salva, mesmo sem conexão.
     * Gaps numéricos podem ocorrer em cenários de conflito multi-dispositivo,
     * o que é aceitável para uso individual.
     */
    private void gerarProximoNumero(@NonNull NumeroCallback callback) {
        try {
            // Contador em: vendas/resumo_geral/config/sequencia → { ultimoNumero: N }
            DocumentReference contadorRef = FirestoreSchema.vendasResumoDoc()
                    .collection("config")
                    .document("sequencia");

            // Calcula fallback local antes de tentar o servidor
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
                        // Sincroniza o contador local com o valor oficial do servidor
                        prefs.edit().putInt(KEY_ULTIMO_NUM, (int)(long) numero).apply();
                        callback.onNumero((int)(long) numero);
                    })
                    .addOnFailureListener(e -> {
                        // Offline ou falha de rede: usa número local como fallback
                        prefs.edit().putInt(KEY_ULTIMO_NUM, fallbackLocal).apply();
                        // Enfileira a atualização do contador remoto para sync posterior
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
            // Venda EM_ABERTO ou status desconhecido não pode ser alternado por esse fluxo
            callback.onErro("Operação inválida: apenas vendas finalizadas ou canceladas podem ter o status alternado.");
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

    /** Escuta em tempo real todas as vendas de um caixa específico. */
    /** Busca uma vez os totais (qtd + soma) das vendas FINALIZADAS de um caixa específico. */
    public void buscarTotaisFinalizadasDoCaixa(@NonNull String caixaId,
                                               @NonNull TotaisCallback callback) {
        try {
            // Filtro único (evita índice composto); status verificado em memória.
            FirestoreSchema.vendasVendasCol()
                    .whereEqualTo("caixaId", caixaId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int    qtd   = 0;
                        double total = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            VendaModel v = doc.toObject(VendaModel.class);
                            if (v != null && VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) {
                                qtd++;
                                total += v.getValorTotal();
                            }
                        }
                        callback.onTotais(qtd, total);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao calcular totais."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    /**
     * Calcula totais das vendas FINALIZADAS que pertencem ao caixa legado:
     * inclui vendas sem caixaId (campo ausente) E vendas com caixaId = "caixa_0".
     * Toda a filtragem é feita em memória para evitar restrições de índice do Firestore.
     */
    public void buscarTotaisVendasLegadas(@NonNull TotaisCallback callback) {
        try {
            FirestoreSchema.vendasVendasCol()
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int    qtd   = 0;
                        double total = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Object caixaId = doc.get("caixaId");
                            boolean legado = caixaId == null
                                    || "caixa_0".equals(caixaId);
                            if (!legado) continue;
                            VendaModel v = doc.toObject(VendaModel.class);
                            if (v != null && VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) {
                                qtd++;
                                total += v.getValorTotal();
                            }
                        }
                        callback.onTotais(qtd, total);
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

}