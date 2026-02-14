package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.Date;
import java.util.List;

/**
 * MovimentacaoRepository (Versão Final Corrigida - Fix Timestamp)
 * Gerencia o CRUD de movimentações com impacto automático no Resumo e Categorias.
 * [CORREÇÃO]: Queries de data agora recebem Date em vez de long para compatibilidade com Firestore Timestamp.
 */
public class MovimentacaoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso(String msg);
        void onErro(String erro);
    }

    public interface DadosCallback {
        void onSucesso(List<MovimentacaoModel> lista);
        void onErro(String erro);
    }

    /**
     * Recupera lista completa ordenada por data.
     */
    public void recuperarMovimentacoes(DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovimentacaoModel> lista = queryDocumentSnapshots.toObjects(MovimentacaoModel.class);
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * [BUG FIX]: Recebe Date em vez de long.
     * O Firestore armazena Timestamp. Comparar Timestamp com Long falha silenciosamente.
     * Comparar Timestamp com Date funciona.
     */
    public void recuperarHistorico(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereLessThanOrEqualTo(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, dataReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovimentacaoModel> lista = queryDocumentSnapshots.toObjects(MovimentacaoModel.class);
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * [BUG FIX]: Recebe Date em vez de long.
     */
    public void recuperarContasFuturas(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereGreaterThan(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, dataReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovimentacaoModel> lista = queryDocumentSnapshots.toObjects(MovimentacaoModel.class);
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * SALVAR: Cria a movimentação.
     */
    public void salvar(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacoesCol().document();
        mov.setId(movRef.getId());
        batch.set(movRef, mov);

        // Aplica impacto (Fator 1) se estiver pago
        atualizarSaldosNoBatch(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Lançado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * EXCLUIR: Remove o documento.
     */
    public void excluir(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
        batch.delete(movRef);

        // Estorna impacto (Fator -1) se estava pago
        atualizarSaldosNoBatch(batch, mov, -1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * EDITAR: Estorna o impacto do modelo antigo e aplica o novo.
     */
    public void editar(MovimentacaoModel movAntigo, MovimentacaoModel movNovo, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(movNovo.getId());
        batch.set(movRef, movNovo);

        // Remove impacto do antigo (se estava pago)
        atualizarSaldosNoBatch(batch, movAntigo, -1);
        // Adiciona impacto do novo (se estiver pago)
        atualizarSaldosNoBatch(batch, movNovo, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Editado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * CONFIRMAR MOVIMENTAÇÃO (CHECK):
     */
    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(mov.getId());

        // 1. Atualiza o status para pago no banco
        batch.update(movRef, "pago", true);

        // 2. Forçamos o objeto local a ser 'pago' para calcular impacto
        mov.setPago(true);

        // 3. Aplica o impacto financeiro (Fator 1)
        atualizarSaldosNoBatch(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Confirmado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Lógica central de atualização financeira.
     */
    private void atualizarSaldosNoBatch(WriteBatch batch, MovimentacaoModel mov, int fator) {

        // Se a movimentação NÃO está paga, não mexe no saldo
        if (!mov.isPago()) {
            return;
        }

        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        DocumentReference catRef = FirestoreSchema.contasCategoriaDoc(mov.getCategoria_id());

        int valorCentavos = mov.getValor();
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);

        int deltaBalanco = (isReceita ? valorCentavos : -valorCentavos) * fator;

        batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, FieldValue.increment(deltaBalanco));

        if (isReceita) {
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL, FieldValue.increment(valorCentavos * fator));
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, FieldValue.increment(valorCentavos * fator));
        } else {
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL, FieldValue.increment(-valorCentavos * fator));
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, FieldValue.increment(valorCentavos * fator));
            batch.update(catRef, ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, FieldValue.increment(valorCentavos * fator));
        }
    }
}