package com.gussanxz.orgafacil.adapter;

import com.gussanxz.orgafacil.model.Movimentacao;

public class MovimentoItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // para header
    public String data;        // "06/12/2025"
    public String tituloDia;   // "Hoje - 06/12/2025" ou "Ontem - ..."
    public double saldoDia;

    // para linha
    public Movimentacao movimentacao;

    public static MovimentoItem header(String data, String tituloDia, double saldoDia) {
        MovimentoItem item = new MovimentoItem();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        return item;
    }

    public static MovimentoItem linha(Movimentacao m) {
        MovimentoItem item = new MovimentoItem();
        item.type = TYPE_MOVIMENTO;
        item.movimentacao = m;
        return item;
    }
}
