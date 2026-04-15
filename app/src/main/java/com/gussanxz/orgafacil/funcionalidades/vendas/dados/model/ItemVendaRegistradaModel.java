package com.gussanxz.orgafacil.funcionalidades.vendas.dados.model;

import java.io.Serializable;

public class ItemVendaRegistradaModel implements Serializable {
    public static final int TIPO_PRODUTO = 1;
    public static final int TIPO_SERVICO = 2;
    private String itemId;
    private String nome;
    private int tipo;

    // Atualizado para int (centavos)
    private int precoUnitario;
    private int quantidade;

    // Atualizado para int (centavos)
    private int subtotal;

    private String categoria;

    public ItemVendaRegistradaModel() {
    }

    public ItemVendaRegistradaModel(ItemSacolaVendaModel itemSacola) {
        this.itemId = itemSacola.getItemId();
        this.nome = itemSacola.getNome();
        this.tipo = itemSacola.getTipo();
        this.precoUnitario = itemSacola.getPrecoUnitario(); // Agora já puxa int
        this.quantidade = itemSacola.getQuantidade();
        this.subtotal = itemSacola.getSubtotal(); // Agora já puxa int
        this.categoria = itemSacola.getCategoria();
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    // Atualizado para retornar int
    public int getPrecoUnitario() {
        return precoUnitario;
    }

    // Atualizado para receber int
    public void setPrecoUnitario(int precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    // Atualizado para retornar int
    public int getSubtotal() {
        return subtotal;
    }

    // Atualizado para receber int
    public void setSubtotal(int subtotal) {
        this.subtotal = subtotal;
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}