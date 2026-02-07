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

import java.util.List;

/**
 * MovimentacaoRepository (Nova Versão)
 * Gerencia o CRUD de movimentações com impacto automático no Resumo e Categorias.
 * ATUALIZADO: Compatível com estruturas de Mapas (Dot Notation).
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
     * [ATUALIZADO]: Usa a constante CAMPO_DATA_MOVIMENTACAO para acessar "detalhes.data_movimentacao".
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
     * [BUG FIX]: Recupera apenas o HISTÓRICO (Data igual ou menor que agora).
     * Resolve o problema de exibir contas futuras na tela de movimentações passadas.
     */
    public void recuperarHistorico(long timestampReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereLessThanOrEqualTo(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, timestampReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovimentacaoModel> lista = queryDocumentSnapshots.toObjects(MovimentacaoModel.class);
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * [BUG FIX]: Recupera apenas as CONTAS FUTURAS (Data maior que agora).
     * Filtra para que o agendamento não se misture com o que já foi gasto/recebido.
     */
    public void recuperarContasFuturas(long timestampReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereGreaterThan(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, timestampReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MovimentacaoModel> lista = queryDocumentSnapshots.toObjects(MovimentacaoModel.class);
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * SALVAR: Cria a movimentação e atualiza Saldo, Receita/Despesa e Balanço.
     */
    public void salvar(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacoesCol().document();
        mov.setId(movRef.getId());
        batch.set(movRef, mov);

        atualizarSaldosNoBatch(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Lançado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * EXCLUIR: Remove o documento e estorna os valores dos resumos.
     */
    public void excluir(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
        batch.delete(movRef);

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

        atualizarSaldosNoBatch(batch, movAntigo, -1);
        atualizarSaldosNoBatch(batch, movNovo, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Editado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Lógica central de atualização financeira.
     * @param fator 1 para aplicar impacto, -1 para estornar.
     */
    private void atualizarSaldosNoBatch(WriteBatch batch, MovimentacaoModel mov, int fator) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        DocumentReference catRef = FirestoreSchema.contasCategoriaDoc(mov.getCategoria_id());

        int valorCentavos = mov.getValor();
        boolean isReceita = (mov.getTipo() == TipoCategoriaContas.RECEITA.getId());

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