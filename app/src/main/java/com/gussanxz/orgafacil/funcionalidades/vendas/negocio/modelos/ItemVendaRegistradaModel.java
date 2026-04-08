package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

import java.io.Serializable;

public class ItemVendaRegistradaModel implements Serializable {
    public static final int TIPO_PRODUTO = 1;
    public static final int TIPO_SERVICO = 2;
    private String itemId;
    private String nome;
    private int tipo;
    private double precoUnitario;
    private int quantidade;
    private double subtotal;
    private String categoria;

    public ItemVendaRegistradaModel() {
    }

    public ItemVendaRegistradaModel(ItemSacolaVendaModel itemSacola) {
        this.itemId = itemSacola.getItemId();
        this.nome = itemSacola.getNome();
        this.tipo = itemSacola.getTipo();
        this.precoUnitario = itemSacola.getPrecoUnitario();
        this.quantidade = itemSacola.getQuantidade();
        this.subtotal = itemSacola.getSubtotal();
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

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}