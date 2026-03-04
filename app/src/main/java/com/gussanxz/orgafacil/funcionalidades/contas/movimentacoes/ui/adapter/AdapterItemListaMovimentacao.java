package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

/**
 * Item de lista que representa um cabeçalho de dia ou uma linha de movimentação.
 *
 * ATENÇÃO: saldoDia é long (centavos), nunca int.
 * Um dia com R$ 21.474,84 ou mais já estoura um int silenciosamente.
 * Todos os sites de chamada devem passar long — nunca fazer cast (int).
 */
public class AdapterItemListaMovimentacao {

    public static final int TYPE_HEADER   = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // ── Campos do cabeçalho ────────────────────────────────────────────────────
    public String data;
    public String tituloDia;

    /**
     * Saldo do dia em CENTAVOS — mantido como long para suportar valores
     * acima de R$ 21.474,83 sem overflow silencioso.
     *
     * Para exibir: saldoDia / 100.0
     */
    public long saldoDia;

    // ── Campo da linha de movimentação ─────────────────────────────────────────
    public MovimentacaoModel movimentacaoModel;

    // ── Fábricas estáticas ─────────────────────────────────────────────────────

    /**
     * Cria um item do tipo cabeçalho de dia.
     *
     * @param data      string de data no formato "dd/MM/yyyy" (usada como chave de grupo)
     * @param tituloDia texto exibido ao usuário ("Hoje", "Ontem", "15/03/2025" etc.)
     * @param saldoDia  saldo líquido do dia em CENTAVOS (long — nunca int)
     */
    public static AdapterItemListaMovimentacao header(String data, String tituloDia, long saldoDia) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type      = TYPE_HEADER;
        item.data      = data;
        item.tituloDia = tituloDia;
        item.saldoDia  = saldoDia; // long → long, sem cast
        return item;
    }

    /**
     * Cria um item do tipo linha de movimentação.
     *
     * @param m movimentação a exibir
     */
    public static AdapterItemListaMovimentacao linha(MovimentacaoModel m) {
        AdapterItemListaMovimentacao item = new AdapterItemListaMovimentacao();
        item.type              = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        return item;
    }
}