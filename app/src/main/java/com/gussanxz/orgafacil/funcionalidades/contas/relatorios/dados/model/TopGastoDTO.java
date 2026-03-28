package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

public class TopGastoDTO {
    private String nomeCategoria;
    private long valorTotal;
    private double percentual; // Alterado para double para permitir casas decimais (Ex: 35.5%)
    private boolean isDespesa; // Nova flag para o Adapter saber se pinta de verde ou vermelho

    // Movimentação individual — preenchido quando o top exibe movimentações em si
    // em vez de categorias agrupadas. Null quando é agrupamento por categoria.
    private MovimentacaoModel movimentacaoModel;

    // Descrição da movimentação individual para exibição no item da lista.
    // Quando null, o adapter exibe nomeCategoria como título principal.
    private String descricao;

    public TopGastoDTO(String nomeCategoria, long valorTotal, double percentual, boolean isDespesa) {
        this.nomeCategoria = nomeCategoria;
        this.valorTotal = valorTotal;
        this.percentual = percentual;
        this.isDespesa = isDespesa;
    }

    /** Construtor para movimentação individual no top 5. */
    public TopGastoDTO(MovimentacaoModel mov, double percentual, boolean isDespesa) {
        this.movimentacaoModel = mov;
        this.descricao = mov.getDescricao();
        this.nomeCategoria = mov.getCategoria_nome() != null ? mov.getCategoria_nome() : "";
        this.valorTotal = Math.abs(mov.getValor());
        this.percentual = percentual;
        this.isDespesa = isDespesa;
    }

    public String getNomeCategoria() { return nomeCategoria; }
    public long getValorTotal() { return valorTotal; }
    public double getPercentual() { return percentual; }
    public boolean isDespesa() { return isDespesa; }
    public MovimentacaoModel getMovimentacaoModel() { return movimentacaoModel; }
    public String getDescricao() { return descricao; }
    public boolean isMovimentacaoIndividual() { return movimentacaoModel != null; }
}
