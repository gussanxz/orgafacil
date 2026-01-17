package com.gussanxz.orgafacil.model;

public class ItemVenda {

    public static final int TIPO_PRODUTO = 1;
    public static final int TIPO_SERVICO = 2;

    private int id;
    private String nome;
    private String descricao;
    private double preco;
    private int tipo; // Define se é Produto ou Serviço

    public ItemVenda(int id, String nome, String descricao, double preco, int tipo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.tipo = tipo;
    }

    public int getId() {
        return id;
    }
    int setId(int id) {
        return this.id = id;
    }

    public String getNome() {
        return nome;
    }
    String setNome(String nome) {
        return this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }
    String setDescricao(String descricao) {
        return this.descricao = descricao;
    }

    public double getPreco() {
        return preco;
    }
    double setPreco(double preco) {
        return this.preco = preco;
    }

    public int getTipo() {
        return tipo;
    }
    int setTipo(int tipo) {
        return this.tipo = tipo;
    }

}
