package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.view.View;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

/**
 * ReceitasActivity
 *
 * Responsável por criar e editar movimentações do tipo RECEITA.
 *
 * Toda a lógica de UI (máscara de moeda, seletor de data/hora, recorrência,
 * validação e persistência) está centralizada em BaseMovimentacaoActivity.
 *
 * Aqui ficam apenas as decisões específicas de RECEITA:
 *  - Tipo da movimentação
 *  - Layout a usar
 *  - Categoria padrão
 *  - Textos do diálogo de exclusão
 */
public class ReceitasActivity extends BaseMovimentacaoActivity {

    @Override
    protected TipoCategoriaContas getTipo() {
        return TipoCategoriaContas.RECEITA;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tela_add_receita;
    }

    @Override
    protected String getCategoriaDefault() {
        return "geral_receita";
    }

    @Override
    protected String getTituloExclusao() {
        return "Excluir Receita";
    }

    @Override
    protected String getMensagemExclusao() {
        return "Deseja apagar este lançamento? O saldo será corrigido.";
    }

    // Botão de salvar no XML chama este método
    public void salvarProventos(View view) {
        salvarMovimentacao();
    }
}