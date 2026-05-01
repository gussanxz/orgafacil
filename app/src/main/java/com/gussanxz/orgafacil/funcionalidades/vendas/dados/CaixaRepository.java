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

    /** Callback exclusivo de {@link #abrirCaixa} — entrega id e nome do caixa criado. */
    public interface AbrirCaixaCallback {
        void onSucesso(String caixaId, String nomeCaixa);
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
     * O {@code numeroCaixa} é calculado contando os caixas já criados no mesmo dia.
     *
     * @param observacao              texto opcional do operador
     * @param permiteLancamentoTardio se true, aceita vendas retroativas mesmo após fechado
     */
    public void abrirCaixa(@Nullable String observacao,
                            boolean permiteLancamentoTardio,
                            @NonNull AbrirCaixaCallback callback) {
        abrirCaixaComPeriodo(observacao, permiteLancamentoTardio,
                System.currentTimeMillis(), 0L, callback);
    }

    public void abrirCaixaComPeriodo(@Nullable String observacao,
                                     boolean permiteLancamentoTardio,
                                     long abertoEmMillis,
                                     long fechadoEmMillis,
                                     @NonNull AbrirCaixaCallback callback) {
        try {
            String caixaId = FirestoreSchema.vendasCaixaCol().document().getId();
            String diaKeyAtual = FirestoreSchema.diaKey(new Date(abertoEmMillis));
            String mesKeyAtual = FirestoreSchema.mesKey(new Date(abertoEmMillis));
            boolean caixaJaFechado = fechadoEmMillis > 0L;

            // Conta caixas do dia para definir o numeroCaixa sequencial
            FirestoreSchema.vendasCaixaCol()
                    .whereEqualTo("diaKey", diaKeyAtual)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int count = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (!CaixaModel.ID_LEGADO.equals(doc.getId())) count++;
                        }
                        int numeroCaixa = count + 1;
                        String nomeCaixaGerado = diaKeyAtual.replace("-", "") + "_" + numeroCaixa;

                        Map<String, Object> data = new HashMap<>();
                        data.put("id",                    caixaId);
                        data.put("status",                caixaJaFechado
                                ? CaixaModel.STATUS_FECHADO
                                : CaixaModel.STATUS_ABERTO);
                        data.put("abertoEmMillis",         abertoEmMillis);
                        data.put("fechadoEmMillis",        caixaJaFechado ? fechadoEmMillis : 0L);
                        data.put("diaKey",                diaKeyAtual);
                        data.put("mesKey",                mesKeyAtual);
                        data.put("observacao",            observacao != null ? observacao : "");
                        data.put("permiteLancamentoTardio", permiteLancamentoTardio);
                        data.put("numeroCaixa",           numeroCaixa);

                        FirestoreSchema.vendasCaixaDoc(caixaId)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(v -> callback.onSucesso(caixaId, nomeCaixaGerado))
                                .addOnFailureListener(e -> callback.onErro(
                                        e.getMessage() != null ? e.getMessage() : "Erro ao abrir caixa."));
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao contar caixas do dia."));
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
        fecharCaixa(caixaId, qtdVendas, valorTotal, System.currentTimeMillis(), callback);
    }

    public void fecharCaixa(@NonNull String caixaId,
                            int qtdVendas,
                            double valorTotal,
                            long fechadoEmMillis,
                            @NonNull VoidCallback callback) {
        try {
            Map<String, Object> patch = new HashMap<>();
            patch.put("status",                 CaixaModel.STATUS_FECHADO);
            patch.put("fechadoEmMillis",         fechadoEmMillis);
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
    /**
     * Busca o caixa fechado mais recente, excluindo o caixa legado.
     */
    public void buscarUltimoCaixaFechado(@NonNull CaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaCol()
                    .orderBy("abertoEmMillis", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (CaixaModel.ID_LEGADO.equals(doc.getId())) continue;

                            CaixaModel caixa = doc.toObject(CaixaModel.class);
                            if (caixa == null) continue;
                            caixa.setId(doc.getId());

                            if (caixa.isFechado()) {
                                callback.onCaixa(caixa);
                                return;
                            }
                        }

                        callback.onCaixa(null);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar ultimo caixa fechado."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuario nao logado");
        }
    }

    /**
     * Reabre um caixa fechado somente se nao houver outro caixa aberto no momento.
     */
    public void reabrirCaixa(@NonNull String caixaId, @NonNull VoidCallback callback) {
        buscarCaixaAberto(new CaixaCallback() {
            @Override
            public void onCaixa(@Nullable CaixaModel caixaAberto) {
                if (caixaAberto != null) {
                    callback.onErro("Ja existe um caixa aberto.");
                    return;
                }

                FirestoreSchema.vendasCaixaDoc(caixaId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                callback.onErro("Caixa nao encontrado.");
                                return;
                            }

                            CaixaModel caixa = doc.toObject(CaixaModel.class);
                            if (caixa == null || CaixaModel.ID_LEGADO.equals(doc.getId())) {
                                callback.onErro("Este caixa nao pode ser reaberto.");
                                return;
                            }

                            if (!caixa.isFechado()) {
                                callback.onErro("Somente caixas fechados podem ser reabertos.");
                                return;
                            }

                            Map<String, Object> patch = new HashMap<>();
                            patch.put("status", CaixaModel.STATUS_ABERTO);
                            patch.put("fechadoEmMillis", 0L);

                            FirestoreSchema.vendasCaixaDoc(caixaId)
                                    .set(patch, SetOptions.merge())
                                    .addOnSuccessListener(v -> callback.onSucesso(caixaId))
                                    .addOnFailureListener(e -> callback.onErro(
                                            e.getMessage() != null ? e.getMessage() : "Erro ao reabrir caixa."));
                        })
                        .addOnFailureListener(e -> callback.onErro(
                                e.getMessage() != null ? e.getMessage() : "Erro ao buscar caixa."));
            }

            @Override
            public void onErro(String erro) {
                callback.onErro(erro);
            }
        });
    }

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

    public void listarCaixasComConflito(@Nullable String caixaIdIgnorado,
                                        long abertoEmMillis,
                                        long fechadoEmMillis,
                                        @NonNull ListaCaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaCol()
                    .orderBy("abertoEmMillis", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        java.util.List<CaixaModel> conflitos = new java.util.ArrayList<>();
                        long fimNovo = fechadoEmMillis > 0L ? fechadoEmMillis : Long.MAX_VALUE;

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (CaixaModel.ID_LEGADO.equals(doc.getId())) continue;
                            if (caixaIdIgnorado != null && caixaIdIgnorado.equals(doc.getId())) continue;

                            CaixaModel caixa = doc.toObject(CaixaModel.class);
                            if (caixa == null) continue;
                            caixa.setId(doc.getId());

                            long inicioExistente = caixa.getAbertoEmMillis();
                            long fimExistente = caixa.getFechadoEmMillis() > 0L
                                    ? caixa.getFechadoEmMillis()
                                    : Long.MAX_VALUE;

                            boolean conflita = abertoEmMillis < fimExistente && fimNovo > inicioExistente;
                            if (conflita) conflitos.add(caixa);
                        }

                        callback.onCaixas(conflitos);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao verificar conflitos de caixa."));
        } catch (IllegalStateException e) {
            callback.onErro("UsuÃ¡rio nÃ£o logado");
        }
    }

    public void excluirCaixaSemVendas(@NonNull String caixaId,
                                      @NonNull VoidCallback callback) {
        if (CaixaModel.ID_LEGADO.equals(caixaId)) {
            callback.onErro("O caixa legado nao pode ser excluido.");
            return;
        }

        try {
            FirestoreSchema.vendasCaixaDoc(caixaId)
                    .get()
                    .addOnSuccessListener(caixaDoc -> {
                        if (!caixaDoc.exists()) {
                            callback.onErro("Caixa nao encontrado.");
                            return;
                        }

                        CaixaModel caixa = caixaDoc.toObject(CaixaModel.class);
                        if (caixa == null) {
                            callback.onErro("Erro ao converter caixa.");
                            return;
                        }
                        caixa.setId(caixaDoc.getId());
                        String nomeCaixa = caixa.getNomeCaixa();

                        FirestoreSchema.vendasVendasCol()
                                .get()
                                .addOnSuccessListener(vendasSnapshot -> {
                                    for (DocumentSnapshot vendaDoc : vendasSnapshot.getDocuments()) {
                                        String vendaCaixaId = vendaDoc.getString("caixaId");
                                        String vendaNomeCaixa = vendaDoc.getString("nomeCaixa");
                                        boolean pertenceAoCaixa = caixaId.equals(vendaCaixaId)
                                                || (nomeCaixa != null && nomeCaixa.equals(vendaNomeCaixa));
                                        if (pertenceAoCaixa) {
                                            callback.onErro("Este caixa possui vendas e nao pode ser excluido.");
                                            return;
                                        }
                                    }

                                    FirestoreSchema.vendasCaixaDoc(caixaId)
                                            .delete()
                                            .addOnSuccessListener(v -> callback.onSucesso(caixaId))
                                            .addOnFailureListener(e -> callback.onErro(
                                                    e.getMessage() != null ? e.getMessage() : "Erro ao excluir caixa."));
                                })
                                .addOnFailureListener(e -> callback.onErro(
                                        e.getMessage() != null ? e.getMessage() : "Erro ao validar vendas do caixa."));
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar caixa."));
        } catch (IllegalStateException e) {
            callback.onErro("UsuÃ¡rio nÃ£o logado");
        }
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
     * Busca um caixa específico pelo ID.
     */
    public void buscarCaixaPorId(@NonNull String caixaId, @NonNull CaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaDoc(caixaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) { callback.onCaixa(null); return; }
                        CaixaModel c = doc.toObject(CaixaModel.class);
                        if (c != null) c.setId(doc.getId());
                        callback.onCaixa(c);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao buscar caixa."));
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
                    .limit(limite + 1L)
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

    public void listarTodosCaixasHistorico(@NonNull ListaCaixaCallback callback) {
        try {
            FirestoreSchema.vendasCaixaCol()
                    .orderBy("abertoEmMillis", Query.Direction.DESCENDING)
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
                        }

                        callback.onCaixas(lista);
                    })
                    .addOnFailureListener(e -> callback.onErro(
                            e.getMessage() != null ? e.getMessage() : "Erro ao listar histórico de caixas."));
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
        }
    }
}
