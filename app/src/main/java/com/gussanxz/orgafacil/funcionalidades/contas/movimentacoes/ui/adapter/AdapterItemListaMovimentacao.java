package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

public class AdapterItemListaMovimentacao {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    public String data;
    public String tituloDia;
    public long saldoDia; // Modificado para long

    public MovimentacaoModel movimentacaoModel;

    public static AdapterItemListaMovimentacao header(String data, String tituloDia, long saldoDia) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        return item;
    }

    public static AdapterItemListaMovimentacao linha(MovimentacaoModel m) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        return item;
    }
}