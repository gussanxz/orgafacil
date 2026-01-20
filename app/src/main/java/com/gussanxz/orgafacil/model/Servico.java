package com.gussanxz.orgafacil.model;

// 1. OBRIGATÓRIO: "implements ItemVenda"
public class Servico implements ItemVenda {
    private String id;
    private String descricao;
    private String categoria;
    private double valor;

    public Servico() {}

    public Servico(String id, String descricao, String categoria, double valor) {
        this.id = id;
        this.descricao = descricao;
        this.categoria = categoria;
        this.valor = valor;
    }

    // --- MÉTODOS QUE FAZEM O TEXTO APARECER NA LISTA ---

    @Override
    public String getId() { return id; }

    @Override
    public String getNome() {
        // O Adapter busca "Nome", mas Serviço tem "Descrição".
        // Se retornar "" aqui, o cartão fica vazio!
        return descricao;
    }

    @Override
    public String getDescricao() { return categoria; }

    @Override
    public double getPreco() { return valor; }

    @Override
    public int getTipo() {
        return ItemVenda.TIPO_SERVICO; // Retorna 2 (Isso define a cor Laranja)
    }

    // --- GETTERS E SETTERS PADRÃO ---
    public void setId(String id) { this.id = id; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}