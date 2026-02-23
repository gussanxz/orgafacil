package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

/**
 * Representa uma linha (ficha) no RecyclerView.
 * Pode ser um Título de Data (HEADER) ou um Registro Financeiro (MOVIMENTO).
 */
public class AdapterItemListaMovimentacao {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // --- Campos do Cabeçalho (Data e Saldo do Dia) ---
    public String data;
    public String tituloDia;
    public int saldoDia; // Saldo em centavos

    // --- Campos do Registro Financeiro ---
    public MovimentacaoModel movimentacaoModel;

    /**
     * Monta uma linha do tipo CABEÇALHO.
     */
    public static AdapterItemListaMovimentacao header(String data, String tituloDia, int saldoDia) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        return item;
    }

    /**
     * Monta uma linha do tipo MOVIMENTAÇÃO.
     */
    public static AdapterItemListaMovimentacao linha(MovimentacaoModel m) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        return item;
    }
}