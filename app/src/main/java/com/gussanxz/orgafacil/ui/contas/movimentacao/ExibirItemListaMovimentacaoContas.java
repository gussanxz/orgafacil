package com.gussanxz.orgafacil.ui.contas.movimentacao;

import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;

/**
 * MODEL: MovimentoItem
 * * RESPONSABILIDADE:
 * Atuar como um contêiner genérico (Wrapper) que permite ao RecyclerView carregar
 * múltiplos tipos de layouts na mesma lista.
 *
 * * O QUE ELA FAZ:
 * 1. Definição de Tipos: Categoriza o item como HEADER (Cabeçalho de data) ou MOVIMENTO (Linha financeira).
 * 2. Armazenamento Flexível: Guarda dados de resumo diário (título e saldo) ou o objeto completo de Movimentação.
 * 3. Factory Methods: Fornece métodos estáticos ('header' e 'linha') para criar instâncias de forma limpa e legível.
 * 4. Suporte ao Adapter: É a classe base que o 'MovimentosAgrupadosAdapter' utiliza para decidir qual XML inflar.
 */
public class ExibirItemListaMovimentacaoContas {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MOVIMENTO = 1;

    public int type;

    // para header
    public String data;        // "06/12/2025"
    public String tituloDia;   // "Hoje - 06/12/2025" ou "Ontem - ..."
    public double saldoDia;

    // para linha
    public MovimentacaoModel movimentacaoModel;

    public static ExibirItemListaMovimentacaoContas header(String data, String tituloDia, double saldoDia) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_HEADER;
        item.data = data;
        item.tituloDia = tituloDia;
        item.saldoDia = saldoDia;
        return item;
    }

    public static ExibirItemListaMovimentacaoContas linha(MovimentacaoModel m) {
        ExibirItemListaMovimentacaoContas item = new ExibirItemListaMovimentacaoContas();
        item.type = TYPE_MOVIMENTO;
        item.movimentacaoModel = m;
        return item;
    }
}
