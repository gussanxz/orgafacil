package com.gussanxz.orgafacil.funcionalidades.contas.contas_a_pagar.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.contas_a_pagar.modelos.ContaFuturaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

/**
 * Repository para agendamentos (Contas a Pagar/Receber).
 * Atualiza os contadores de pendências no Resumo Financeiro.
 */
public class ContaFuturaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    /**
     * AGENDA uma nova conta.
     * Além de salvar, incrementa o contador de pendências no Resumo via Batch.
     */
    public void agendar(ContaFuturaModel conta, Callback callback) {
        WriteBatch batch = db.batch();

        // 1. Define referência e ID
        DocumentReference ref = FirestoreSchema.contasFuturasCol().document();
        conta.setId(ref.getId());
        batch.set(ref, conta);

        // 2. Atualiza o contador de pendências no Resumo
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        String campoPendencia = (conta.getTipo() == TipoCategoriaContas.RECEITA.getId())
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(resumoRef, campoPendencia, FieldValue.increment(1));

        // 3. Commit Atômico
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * EXCLUI um agendamento e decrementa a pendência no Resumo.
     */
    public void excluir(ContaFuturaModel conta, Callback callback) {
        WriteBatch batch = db.batch();

        // Deleta o agendamento
        batch.delete(FirestoreSchema.contasFuturaDoc(conta.getId()));

        // Decrementa no Resumo
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        String campoPendencia = (conta.getTipo() == TipoCategoriaContas.RECEITA.getId())
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(resumoRef, campoPendencia, FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Lista contas agendadas filtrando por tipo e ordenando por vencimento.
     */
    public Query listarAgendamentos(int tipoId) {
        return FirestoreSchema.contasFuturasCol()
                .whereEqualTo(ContaFuturaModel.CAMPO_TIPO, tipoId)
                .whereEqualTo(ContaFuturaModel.CAMPO_PAGO, false)
                .orderBy(ContaFuturaModel.CAMPO_DATA_VENCIMENTO, Query.Direction.ASCENDING);
    }

    /**
     * EFETIVAR CONTA: Quando o usuário marca como paga.
     * Esta lógica geralmente chama o MovimentacaoRepository para criar a transação real
     * e este repository para excluir o agendamento.
     */
}