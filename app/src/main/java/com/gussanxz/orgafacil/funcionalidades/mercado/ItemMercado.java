package com.gussanxz.orgafacil.funcionalidades.mercado;

/**
 * Modelo de dados de um item da Lista de Mercado.
 *
 * RN01 – todos os valores financeiros são armazenados em CENTAVOS (int).
 * A conversão para exibição (÷ 100) ocorre exclusivamente na camada de apresentação.
 */
public class ItemMercado {

    private long   id;
    private String nome;
    private String categoria;
    private int    valorCentavos;   // RN01 – ex: R$ 25,90 → 2590
    private int    quantidade;
    private boolean noCarrinho;

    // ─── Construtores ─────────────────────────────────────────────────────────

    public ItemMercado() {}

    public ItemMercado(String nome, String categoria, int valorCentavos, int quantidade) {
        this.nome          = nome;
        this.categoria     = categoria;
        this.valorCentavos = valorCentavos;
        this.quantidade    = quantidade;
        this.noCarrinho    = false;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public long getId()                        { return id; }
    public void setId(long id)                 { this.id = id; }

    public String getNome()                    { return nome; }
    public void setNome(String nome)           { this.nome = nome; }

    public String getCategoria()               { return categoria; }
    public void setCategoria(String cat)       { this.categoria = cat; }

    /** RN01 – valor em centavos (int). */
    public int getValorCentavos()              { return valorCentavos; }
    public void setValorCentavos(int v)        { this.valorCentavos = v; }

    public int getQuantidade()                 { return quantidade; }
    public void setQuantidade(int q)           { this.quantidade = q; }

    /** RF02 – flag "No Carrinho". */
    public boolean isNoCarrinho()              { return noCarrinho; }
    public void setNoCarrinho(boolean nc)      { this.noCarrinho = nc; }

    /**
     * RN01 – subtotal em centavos, calculado aqui para facilitar os totais.
     * Retorna long para evitar overflow antes da verificação (CT04).
     */
    public long getSubtotalCentavos() {
        return (long) valorCentavos * quantidade;
    }
}