package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;

/**
 * Classe Wrapper (Envelope) para a lista heterogênea.
 * Permite que o RecyclerView exiba tanto Cabeçalhos (Data/Saldo) quanto Itens (Movimentações).
 */
public class ExibirItemListaMovimentacaoContas {

    // Constantes para identificar o tipo de visualização no Adapter
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // --- CAMPOS ESPECÍFICOS DO HEADER ---
    public String data;       // Ex: "14/02/2026"
    public String tituloDia;  // Ex: "Hoje", "Ontem" ou a Data

    // [PRECISÃO]: Mantemos int (centavos) conforme regra do projeto.
    // O Helper faz a soma em long e casta para int aqui.
    public int saldoDia;

    // --- CAMPOS ESPECÍFICOS DO ITEM (LINHA) ---
    public MovimentacaoModel movimentacaoModel;

    // =========================================================================
    // FACTORY METHODS (Construtores Estáticos para clareza)
    // =========================================================================

    /**
     * Cria um item do tipo HEADER (Cabeçalho do Dia).
     * @param data Data formatada para controle.
     * @param tituloDia Título amigável ("Hoje", "Ontem").
     * @param saldoDia Soma dos valores PAGOS do dia (em centavos).
     */
    public static ExibirItemListaMovimentacaoContas header(String data, String tituloDia, int saldoDia) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        item.movimentacaoModel = null; // Header não tem movimentação vinculada
        return item;
    }

    /**
     * Cria um item do tipo MOVIMENTO (Linha da Lista).
     * @param m O objeto de dados vindo do Firebase.
     */
    public static ExibirItemListaMovimentacaoContas linha(MovimentacaoModel m) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        // Campos de header ficam nulos/zerados pois não são usados neste tipo
        return item;
    }
}