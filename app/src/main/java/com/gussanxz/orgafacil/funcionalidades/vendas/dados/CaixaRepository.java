package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CaixaModel;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * CaixaRepository
 *
 * Gerencia abertura e fechamento do caixa de vendas.
 * Regras:
 *  - Só pode haver 1 caixa ABERTO por vez.
 *  - Vendas legadas (sem caixaId) pertencem ao ID reservado "caixa_0".
 */
public class CaixaRepository {

    // ── Callbacks ─────────────────────────────────────────────────────

    public interface CaixaCallback {
        /** @param caixa caixa aberto atual, ou null se não houver nenhum. */
        void onCaixa(@Nullable CaixaModel caixa);
        void onErro(String erro);
    }

    public interface VoidCallback {
        void onSucesso(String caixaId);
        void onErro(String erro);
    }

    // ── Listeners em tempo real ────────────────────────────────────────

    /**
     * Escuta em tempo real o caixa atualmente aberto.
     * Chama onCaixa(null) quando não há caixa aberto.
     */
    public ListenerRegistration escutarCaixaAberto(@NonNull CaixaCallback callback) {
        try {
            return FirestoreSchema.vendasCaixaCol()
                    .whereEqualTo("status", CaixaModel.STATUS_ABERTO)
                    .limit(1)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null
                                    ? error.getMessage() : "Erro ao escutar caixa.");
                            return;
                        }
                        if (snapshot == null || snapshot.isEmpty()) {
                            callback.onCaixa(null);
                            return;
                        }
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        CaixaModel caixa = doc.toObject(CaixaModel.class);
                        if (caixa != null) caixa.setId(doc.getId());
                        callback.onCaixa(caixa);
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

    // ── Busca pontual ─────────────────────────────────────────────────

    /** Busca uma vez o caixa aberto atual. */
    public void buscarCaixaAberto(@NonNull CaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaCol()
                    .whereEqualTo("status", CaixaModel.STATUS_ABERTO)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            callback.onCaixa(null);
                            return;
                        }
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        CaixaModel caixa = doc.toObject(CaixaModel.class);
                        if (caixa != null) caixa.setId(doc.getId());
                        callback.onCaixa(caixa);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar caixa."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // ── Abrir caixa ───────────────────────────────────────────────────

    /**
     * Abre um novo caixa.
     *
     * @param observacao              texto opcional do operador
     * @param permiteLancamentoTardio se true, aceita vendas retroativas mesmo após fechado
     */
    public void abrirCaixa(@Nullable String observacao,
                            boolean permiteLancamentoTardio,
                            @NonNull VoidCallback callback) {
        try {
            String caixaId = FirestoreSchema.vendasCaixaCol().document().getId();
            long agora = System.currentTimeMillis();

            Map<String, Object> data = new HashMap<>();
            data.put("id",                    caixaId);
            data.put("status",                CaixaModel.STATUS_ABERTO);
            data.put("abertoEmMillis",         agora);
            data.put("fechadoEmMillis",        0L);
            data.put("diaKey",                FirestoreSchema.diaKey(new Date(agora)));
            data.put("mesKey",                FirestoreSchema.mesKey(new Date(agora)));
            data.put("observacao",            observacao != null ? observacao : "");
            data.put("permiteLancamentoTardio", permiteLancamentoTardio);

            FirestoreSchema.vendasCaixaDoc(caixaId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> callback.onSucesso(caixaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao abrir caixa."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // ── Fechar caixa ──────────────────────────────────────────────────

    /**
     * Fecha o caixa, gravando um snapshot de totais para exibição no histórico.
     *
     * @param qtdVendas  quantidade de vendas finalizadas no período
     * @param valorTotal soma dos valores finalizados
     */
    public void fecharCaixa(@NonNull String caixaId,
                            int qtdVendas,
                            double valorTotal,
                            @NonNull VoidCallback callback) {
        try {
            Map<String, Object> patch = new HashMap<>();
            patch.put("status",                 CaixaModel.STATUS_FECHADO);
            patch.put("fechadoEmMillis",         System.currentTimeMillis());
            patch.put("qtdVendasFechamento",     qtdVendas);
            patch.put("valorTotalFechamento",    valorTotal);

            FirestoreSchema.vendasCaixaDoc(caixaId)
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> callback.onSucesso(caixaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao fechar caixa."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // ── Caixa legado ─────────────────────────────────────────────────

    /**
     * Garante que o documento "caixa_0" existe.
     * Chamado na primeira execução do app após a implantação do fluxo de caixa.
     * Vendas sem caixaId são tratadas como pertencentes a este caixa legado.
     */
    public void garantirCaixaLegado(@NonNull VoidCallback callback) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id",             CaixaModel.ID_LEGADO);
            data.put("status",         CaixaModel.STATUS_FECHADO);
            data.put("abertoEmMillis",  0L);
            data.put("fechadoEmMillis", 0L);
            data.put("diaKey",         "legado");
            data.put("mesKey",         "legado");
            data.put("observacao",     "Vendas registradas antes do fluxo de caixa");
            data.put("permiteLancamentoTardio", false);

            FirestoreSchema.vendasCaixaDoc(CaixaModel.ID_LEGADO)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> callback.onSucesso(CaixaModel.ID_LEGADO))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao criar caixa legado."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // ── Migração de vendas legadas ────────────────────────────────────

    /**
     * Percorre TODAS as vendas em páginas de 500 (ordenado por ID de documento),
     * filtra em memória as que não têm o campo caixaId (campo ausente ≠ null no Firestore)
     * e atualiza cada uma para "caixa_0".
     *
     * onSucesso recebe o total de documentos migrados. Chamado uma única vez ao final.
     */
    public void migrarVendasLegadas(@NonNull VoidCallback callback) {
        migrarPagina(null, 0, callback);
    }

    private void migrarPagina(@Nullable DocumentSnapshot cursor,
                              int totalMigrado,
                              @NonNull VoidCallback callback) {
        try {
            Query query = FirestoreSchema.vendasVendasCol()
                    .orderBy(FieldPath.documentId())
                    .limit(500);
            if (cursor != null) query = query.startAfter(cursor);

            query.get().addOnSuccessListener((QuerySnapshot snapshot) -> {
                if (snapshot.isEmpty()) {
                    callback.onSucesso(String.valueOf(totalMigrado));
                    return;
                }

                // Filtra em memória os documentos sem o campo caixaId
                java.util.List<DocumentSnapshot> semCaixa = new java.util.ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    if (doc.get("caixaId") == null) semCaixa.add(doc);
                }

                DocumentSnapshot ultimo = snapshot.getDocuments()
                        .get(snapshot.size() - 1);
                boolean temMais = snapshot.size() == 500;

                if (semCaixa.isEmpty()) {
                    // Página sem pendências — avança ou conclui
                    if (temMais) {
                        migrarPagina(ultimo, totalMigrado, callback);
                    } else {
                        callback.onSucesso(String.valueOf(totalMigrado));
                    }
                    return;
                }

                WriteBatch batch = FirestoreSchema.vendasVendasCol()
                        .getFirestore().batch();
                for (DocumentSnapshot doc : semCaixa) {
                    batch.update(doc.getReference(), "caixaId", CaixaModel.ID_LEGADO);
                }

                batch.commit().addOnSuccessListener(v -> {
                    int novoTotal = totalMigrado + semCaixa.size();
                    if (temMais) {
                        migrarPagina(ultimo, novoTotal, callback);
                    } else {
                        callback.onSucesso(String.valueOf(novoTotal));
                    }
                }).addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao migrar vendas."));

            }).addOnFailureListener(e -> callback.onErro(
                    e.getMessage() != null ? e.getMessage() : "Erro ao buscar vendas."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    // ── Listar caixas por data ────────────────────────────────────────

    public interface ListaCaixaCallback {
        void onCaixas(java.util.List<CaixaModel> lista);
        void onErro(String erro);
    }

    /** Atualiza os campos de snapshot (qtdVendasFechamento / valorTotalFechamento) de qualquer caixa. */
    public void atualizarSnapshotTotais(@NonNull String caixaId,
                                        int qtdVendas,
                                        double valorTotal,
                                        @NonNull VoidCallback callback) {
        try {
            java.util.Map<String, Object> patch = new java.util.HashMap<>();
            patch.put("qtdVendasFechamento",  qtdVendas);
            patch.put("valorTotalFechamento", valorTotal);

            FirestoreSchema.vendasCaixaDoc(caixaId)
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> callback.onSucesso(caixaId))
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao atualizar totais."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    /** Busca o documento do caixa legado (caixa_0). Retorna null se não existir. */
    public void buscarCaixaLegado(@NonNull CaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaDoc(CaixaModel.ID_LEGADO)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            callback.onCaixa(null);
                            return;
                        }
                        CaixaModel c = doc.toObject(CaixaModel.class);
                        if (c != null) c.setId(doc.getId());
                        callback.onCaixa(c);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar caixa legado."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }

    /**
     * Lista os caixas mais recentes, excluindo o legado (caixa_0).
     * Ordena apenas por abertoEmMillis para evitar índice composto.
     * Aumenta o limite em 1 para compensar a filtragem do legado em memória.
     */
    public void listarCaixasRecentes(int limite, @NonNull ListaCaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaCol()
                    .orderBy("abertoEmMillis", Query.Direction.DESCENDING)
                    .limit(limite + 1L) // +1 para absorver o caixa_0 se vier no topo
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        java.util.List<CaixaModel> lista = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (CaixaModel.ID_LEGADO.equals(doc.getId())) continue;
                            CaixaModel c = doc.toObject(CaixaModel.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                lista.add(c);
                            }
                            if (lista.size() >= limite) break;
                        }
                        callback.onCaixas(lista);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao listar caixas."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }
}
