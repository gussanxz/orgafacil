package com.gussanxz.orgafacil.model;

public class Produto implements ItemVenda {
    private String id;
    private String nome;
    private String categoria;
    private double preco;

    public Produto() {} // Construtor vazio para o Firebase

    public Produto(String id, String nome, String categoria, double preco) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
    }

    // --- MÉTODOS OBRIGATÓRIOS DA INTERFACE (CORRIGIDOS) ---

    @Override
    public String getId() { return id; }

    @Override
    public String getNome() { return nome; }

    @Override
    public String getDescricao() {
        return categoria; // <--- CORREÇÃO: Exibe a categoria como "detalhe" na lista
    }

    @Override
    public double getPreco() { return preco; }

    @Override
    public int getTipo() {
        return ItemVenda.TIPO_PRODUTO; // <--- CORREÇÃO: Retorna 1 para o filtro funcionar
    }

    // --- GETTERS E SETTERS PADRÃO ---
    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setPreco(double preco) { this.preco = preco; }
}