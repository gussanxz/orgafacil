package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.view.View;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

/**
 * DespesasActivity
 *
 * Responsável por criar e editar movimentações do tipo DESPESA.
 *
 * Toda a lógica de UI (máscara de moeda, seletor de data/hora, recorrência,
 * validação e persistência) está centralizada em BaseMovimentacaoActivity.
 *
 * Aqui ficam apenas as decisões específicas de DESPESA:
 *  - Tipo da movimentação
 *  - Layout a usar
 *  - Categoria padrão
 *  - Textos do diálogo de exclusão
 */
public class DespesasActivity extends BaseMovimentacaoActivity {

    @Override
    protected TipoCategoriaContas getTipo() {
        return TipoCategoriaContas.DESPESA;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tela_add_despesa;
    }

    @Override
    protected String getCategoriaDefault() {
        return "geral_despesa";
    }

    @Override
    protected String getTituloExclusao() {
        return "Excluir Despesa";
    }

    @Override
    protected String getMensagemExclusao() {
        return "Tem certeza? O saldo será corrigido automaticamente.";
    }

    // Botão de salvar no XML chama este método
    public void salvarDespesa(View view) {
        salvarMovimentacao();
    }
}