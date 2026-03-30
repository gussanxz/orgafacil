package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

public class HeaderDiaVenda {
    public final String titulo;
    public final double totalDia;
    public final int qtdVendasFinalizadas;
    public final int qtdCanceladas;

    public HeaderDiaVenda(String titulo, double totalDia, int qtdVendasFinalizadas, int qtdCanceladas) {
        this.titulo               = titulo;
        this.totalDia             = totalDia;
        this.qtdVendasFinalizadas = qtdVendasFinalizadas;
        this.qtdCanceladas        = qtdCanceladas;
    }
}