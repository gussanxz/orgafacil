package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

public class TopGastoDTO {
    private String nomeCategoria;
    private long valorTotal;
    private int percentual;

    public TopGastoDTO(String nomeCategoria, long valorTotal, int percentual) {
        this.nomeCategoria = nomeCategoria;
        this.valorTotal = valorTotal;
        this.percentual = percentual;
    }

    public String getNomeCategoria() { return nomeCategoria; }
    public long getValorTotal() { return valorTotal; }
    public int getPercentual() { return percentual; }
}