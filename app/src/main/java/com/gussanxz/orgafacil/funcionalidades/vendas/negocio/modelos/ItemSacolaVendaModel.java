package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

public class ItemSacolaVendaModel {

    private final String chave;
    private final String itemId;
    private final String nome;
    private final int tipo;
    private final double precoUnitario;
    private int quantidade;

    public ItemSacolaVendaModel(ItemVendaModel item) {
        this.chave = gerarChave(item);
        this.itemId = item.getId();
        this.nome = item.getNome();
        this.tipo = item.getTipo();
        this.precoUnitario = item.getPreco();
        this.quantidade = 1;
    }

    public static String gerarChave(ItemVendaModel item) {
        return item.getTipo() + "_" + (item.getId() != null ? item.getId() : item.getNome());
    }

    public void incrementarQuantidade() {
        quantidade++;
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
}