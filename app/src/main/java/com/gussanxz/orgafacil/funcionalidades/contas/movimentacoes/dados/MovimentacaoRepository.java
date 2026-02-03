package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MovimentacaoRepository {

    public interface Callback {
        void onSucesso(String msg);
        void onErro(String erro);
    }

    /**
     * Salva ou edita uma movimentação usando WriteBatch.
     * Inclui atualização de saldo acumulado e último lançamento.
     */
    public void salvar(MovimentacaoModel mov, Callback callback) {
        try {
            WriteBatch batch = ConfiguracaoFirestore.getFirestore().batch();
            boolean isNovo = (mov.getKey() == null || mov.getKey().trim().isEmpty());

            // 1. Referência da Movimentação
            DocumentReference movRef = isNovo
                    ? FirestoreSchema.contasMovimentacoesCol().document()
                    : FirestoreSchema.contasMovimentacaoDoc(mov.getKey());

            if (isNovo) mov.setKey(movRef.getId());

            // 2. Preparar Dados
            Map<String, Object> doc = mapearParaMap(mov);
            doc.put("updatedAt", FieldValue.serverTimestamp());
            if (isNovo) doc.put("createdAt", FieldValue.serverTimestamp());

            batch.set(movRef, doc, SetOptions.merge());

            // 3. Atualizar Resumos e Saldo somente para novos registros
            if (isNovo) {
                adicionarResumoNoBatch(batch, mov);
                atualizarSaldoGeralNoBatch(batch, mov);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> callback.onSucesso(isNovo ? "Criado!" : "Editado!"))
                    .addOnFailureListener(e -> callback.onErro(e.getMessage()));

        } catch (IllegalStateException e) {
            callback.onErro("Sessão expirada: " + e.getMessage());
        } catch (Exception e) {
            callback.onErro("Erro inesperado: " + e.getMessage());
        }
    }

    /**
     * Atualiza o Saldo Total de forma atômica no servidor.
     */
    private void atualizarSaldoGeralNoBatch(WriteBatch batch, MovimentacaoModel mov) {
        // usuarios/{uid}/moduloSistema/contas/contas_resumos/geral
        DocumentReference geralRef = FirestoreSchema.userDoc(FirestoreSchema.requireUid())
                .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_RESUMOS).document("geral");

        int valorCent = (int) Math.round(mov.getValor() * 100.0);
        int incremento = "r".equals(mov.getTipo()) ? valorCent : -valorCent;

        Map<String, Object> data = new HashMap<>();
        data.put("saldoTotalCent", FieldValue.increment(incremento));
        data.put("updatedAt", FieldValue.serverTimestamp());

        batch.set(geralRef, data, SetOptions.merge());
    }

    /**
     * Adiciona o snapshot do último lançamento ao lote.
     */
    private void adicionarResumoNoBatch(WriteBatch batch, MovimentacaoModel mov) {
        DocumentReference ultimosRef = FirestoreSchema.contasResumoUltimosDoc();
        String campo = "r".equals(mov.getTipo()) ? "ultimaEntrada" : "ultimaSaida";

        Map<String, Object> info = new HashMap<>();
        info.put("movId", mov.getKey());
        info.put("valorCent", (int) Math.round(mov.getValor() * 100.0));
        info.put("categoriaNomeSnapshot", mov.getCategoria());
        info.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> payload = new HashMap<>();
        payload.put(campo, info);
        payload.put("updatedAt", FieldValue.serverTimestamp());

        batch.set(ultimosRef, payload, SetOptions.merge());
    }

    /**
     * Lista movimentações de um mês específico.
     */
    public Query listarPorMes(String mesKey) {
        return FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("mesKey", mesKey)
                .orderBy("data", Query.Direction.DESCENDING);
    }

    /**
     * Exclui uma movimentação.
     */
    public Task<Void> excluir(String movId) {
        return FirestoreSchema.contasMovimentacaoDoc(movId).delete();
    }

    /**
     * Recupera o snapshot do último lançamento.
     */
    public Task<DocumentSnapshot> obterSugestaoUltimos() {
        return FirestoreSchema.contasResumoUltimosDoc().get();
    }

    /**
     * Recalcula o ponteiro de 'último lançamento' após exclusão.
     */
    public void recalcularUltimoPonteiro(String tipoNovo, Runnable onComplete) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("tipo", tipoNovo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Object> patch = new HashMap<>();
                    String campo = "Receita".equalsIgnoreCase(tipoNovo) ? "ultimaEntrada" : "ultimaSaida";

                    if (snap == null || snap.isEmpty()) {
                        patch.put(campo, null);
                    } else {
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        Map<String, Object> info = new HashMap<>();
                        info.put("movId", doc.getId());
                        info.put("valorCent", doc.get("valorCent"));
                        info.put("categoriaNomeSnapshot", doc.get("categoriaNome"));
                        info.put("createdAt", doc.get("createdAt"));
                        patch.put(campo, info);
                    }

                    patch.put("updatedAt", FieldValue.serverTimestamp());
                    FirestoreSchema.contasResumoUltimosDoc()
                            .set(patch, SetOptions.merge())
                            .addOnCompleteListener(t -> onComplete.run());
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private Map<String, Object> mapearParaMap(MovimentacaoModel mov) {
        Map<String, Object> doc = new HashMap<>();
        Date dateObj = parseDate(mov.getData());

        doc.put("diaKey", FirestoreSchema.diaKey(dateObj));
        doc.put("mesKey", FirestoreSchema.mesKey(dateObj));
        doc.put("tipo", "r".equals(mov.getTipo()) ? "Receita" : "Despesa");
        doc.put("valorCent", (int) Math.round(mov.getValor() * 100.0));
        doc.put("data", mov.getData());
        doc.put("hora", mov.getHora());
        doc.put("descricao", mov.getDescricao());
        doc.put("categoriaNome", mov.getCategoria());
        return doc;
    }

    private Date parseDate(String d) {
        if (d == null || d.isEmpty()) return new Date();
        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(d);
        } catch (Exception e) {
            return new Date();
        }
    }
}