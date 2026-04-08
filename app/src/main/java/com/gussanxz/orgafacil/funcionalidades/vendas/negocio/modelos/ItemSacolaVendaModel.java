package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

public class ItemSacolaVendaModel implements java.io.Serializable {

    private final String chave;
    private final String itemId;
    private final String nome;
    private final String categoria;
    private final int tipo;
    private final double precoUnitario;
    private int quantidade;

    // Construtor 1: para adicionar item novo na sacola (vem da interface ItemVendaModel)
    public ItemSacolaVendaModel(ItemVendaModel item) {
        this.chave = item.getTipo() + "_" + (item.getId() != null ? item.getId() : item.getNome());
        this.itemId = item.getId();
        this.nome = item.getNome();
        this.tipo = item.getTipo();
        this.precoUnitario = item.getPreco();
        this.quantidade = 1;
        this.categoria = (item instanceof CatalogoModel) ? ((CatalogoModel) item).getCategoria() : null;
    }

    // Construtor 2: para restaurar sacola a partir de uma venda já registrada
    public ItemSacolaVendaModel(ItemVendaRegistradaModel item) {
        this.chave = item.getTipo() + "_" + item.getItemId();
        this.itemId = item.getItemId();
        this.nome = item.getNome();
        this.tipo = item.getTipo();
        this.precoUnitario = item.getPrecoUnitario();
        this.quantidade = item.getQuantidade();
    }

    public static String gerarChave(ItemVendaModel item) {
        return item.getTipo() + "_" + (item.getId() != null ? item.getId() : item.getNome());
    }

    public void incrementarQuantidade() {
        quantidade++;
    }

    public void decrementarQuantidade() {
        if (quantidade > 0) {
            quantidade--;
        }
    }

    public String getChave() {
        return chave;
    }

    public String getItemId() {
        return itemId;
    }

    public String getNome() {
        return nome;
    }

    public int getTipo() {
        return tipo;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getSubtotal() {
        return precoUnitario * quantidade;
    }

    public String getCategoria() { return categoria; }
}