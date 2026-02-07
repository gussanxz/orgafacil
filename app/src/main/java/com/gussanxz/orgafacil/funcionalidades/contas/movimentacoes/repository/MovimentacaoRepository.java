package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
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
     * Recupera lista ordenada por data.
     * [ATUALIZADO]: Usa a constante CAMPO_DATA_MOVIMENTACAO para acessar "detalhes.data_movimentacao".
     */
    public void recuperarMovimentacoes(DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // O toObjects converte automaticamente o mapa aninhado para o objeto Java
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

        // 1. Gera ID e salva o documento
        // O Model já cuida de criar a estrutura de mapas (detalhes/categoria)
        DocumentReference movRef = FirestoreSchema.contasMovimentacoesCol().document();
        mov.setId(movRef.getId());
        batch.set(movRef, mov);

        // 2. Aplica o impacto financeiro (fator 1 = somar impacto)
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

        // Estorno (fator -1 = inverter impacto)
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

        // 1. Estorna o antigo (-1)
        atualizarSaldosNoBatch(batch, movAntigo, -1);

        // 2. Aplica o novo (1)
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

        // helper getCategoria_id() funciona pegando de "categoria.id"
        DocumentReference catRef = FirestoreSchema.contasCategoriaDoc(mov.getCategoria_id());

        // helper getValor() funciona pegando de "detalhes.valor"
        int valorCentavos = mov.getValor();
        boolean isReceita = (mov.getTipo() == TipoCategoriaContas.RECEITA.getId());

        // Calculo do Delta para o Balanço (Receita soma, Despesa subtrai)
        // Se for Despesa, o valor entra negativo no balanço
        int deltaBalanco = (isReceita ? valorCentavos : -valorCentavos) * fator;

        // [IMPORTANTE]: Usando as Constantes atualizadas dos Models (Dot Notation)
        // Isso atualiza "balanco.balancoMes"
        batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, FieldValue.increment(deltaBalanco));

        if (isReceita) {
            // Atualiza "balanco.saldoAtual" e "balanco.receitasMes"
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL, FieldValue.increment(valorCentavos * fator));
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, FieldValue.increment(valorCentavos * fator));
        } else {
            // Atualiza "balanco.saldoAtual" (-) e "balanco.despesasMes" (+)
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL, FieldValue.increment(-valorCentavos * fator));
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, FieldValue.increment(valorCentavos * fator));

            // Impacto na Categoria: "financeiro.totalGastoMesAtual"
            batch.update(catRef, ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, FieldValue.increment(valorCentavos * fator));
        }
    }
}