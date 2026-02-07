package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;

public class ExibirItemListaMovimentacaoContas {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // Campos do Header
    public String data;
    public String tituloDia;
    public int saldoDia; // Mudei para int (centavos)

    // Campos do Item
    public MovimentacaoModel movimentacaoModel;

    // Factory para Header
    public static ExibirItemListaMovimentacaoContas header(String data, String tituloDia, int saldoDia) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        return item;
    }

    // Factory para Linha
    public static ExibirItemListaMovimentacaoContas linha(MovimentacaoModel m) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        return item;
    }
}