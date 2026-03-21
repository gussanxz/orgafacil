package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

public class TopGastoDTO {
    private String nomeCategoria;
    private long valorTotal;
    private double percentual; // Alterado para double para permitir casas decimais (Ex: 35.5%)
    private boolean isDespesa; // Nova flag para o Adapter saber se pinta de verde ou vermelho

    public TopGastoDTO(String nomeCategoria, long valorTotal, double percentual, boolean isDespesa) {
        this.nomeCategoria = nomeCategoria;
        this.valorTotal = valorTotal;
        this.percentual = percentual;
        this.isDespesa = isDespesa;
    }

    public String getNomeCategoria() { return nomeCategoria; }
    public long getValorTotal() { return valorTotal; }
    public double getPercentual() { return percentual; }
    public boolean isDespesa() { return isDespesa; }
}