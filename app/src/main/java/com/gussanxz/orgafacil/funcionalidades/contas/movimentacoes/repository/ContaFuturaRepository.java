package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

/**
 * ContaFuturaRepository (Versão Unificada)
 * Gerencia agendamentos dentro da coleção única de movimentações.
 * Atualiza os contadores de pendências (A Pagar/Receber) no Resumo.
 */
public class ContaFuturaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    /**
     * AGENDA uma nova conta futura.
     * Salva na coleção única de movimentações com status pago = false.
     */
    public void agendar(MovimentacaoModel conta, Callback callback) {
        WriteBatch batch = db.batch();

        // 1. Força o status de pagamento como falso para agendamentos
        conta.getDetalhes().setPago(false);

        // 2. Define referência na coleção ÚNICA de movimentações
        DocumentReference ref = FirestoreSchema.contasMovimentacoesCol().document();
        conta.setId(ref.getId());
        batch.set(ref, conta);

        // 3. Atualiza o contador de pendências no Resumo Financeiro
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        // Identifica se incrementa "A Receber" ou "A Pagar"
        boolean isReceita = (conta.getTipo() == TipoCategoriaContas.RECEITA.getId());
        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(resumoRef, campoPendencia, FieldValue.increment(1));

        // 4. Commit Atômico
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * EXCLUI um agendamento e decrementa a pendência no Resumo.
     */
    public void excluir(MovimentacaoModel conta, Callback callback) {
        WriteBatch batch = db.batch();

        // Deleta o documento da coleção única
        batch.delete(FirestoreSchema.contasMovimentacaoDoc(conta.getId()));

        // Decrementa no Resumo baseado no tipo
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        boolean isReceita = (conta.getTipo() == TipoCategoriaContas.RECEITA.getId());
        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(resumoRef, campoPendencia, FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * [ATUALIZADO]: Busca agendamentos filtrando por Tipo e Status de Pagamento.
     * Usa a coleção única de movimentações.
     */
    public Query listarAgendamentos(TipoCategoriaContas tipo) {
        return FirestoreSchema.contasMovimentacoesCol()
                // Filtra pelo Nome do Enum (String) conforme novo Modelo
                .whereEqualTo(MovimentacaoModel.CAMPO_TIPO, tipo.name())
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING);
    }

    /**
     * EFETIVAR CONTA: Marca uma conta futura como paga.
     * Altera o saldo real e remove a pendência.
     */
    public void efetivar(MovimentacaoModel conta, Callback callback) {
        WriteBatch batch = db.batch();
        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(conta.getId());
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        // 1. Marca como paga no banco (usando Dot Notation)
        batch.update(movRef, MovimentacaoModel.CAMPO_PAGO, true);

        // 2. Decrementa o contador de pendências
        boolean isReceita = (conta.getTipo() == TipoCategoriaContas.RECEITA.getId());
        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;
        batch.update(resumoRef, campoPendencia, FieldValue.increment(-1));

        // 3. Atualiza o saldo real (Aqui você aplica a lógica financeira de somar/subtrair)
        int valor = conta.getValor();
        batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL,
                FieldValue.increment(isReceita ? valor : -valor));

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }
}