package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

public class ProdutoModel implements ItemVendaModel {
    private String id;
    private String nome;
    private String categoriaId;
    private String categoria;
    private double preco;
    private boolean statusAtivo = true;

    public ProdutoModel() {} // Construtor vazio para o Firebase

    public ProdutoModel(String id, String nome, String categoriaId, String categoria, double preco, boolean statusAtivo) {
        this.id = id;
        this.nome = nome;
        this.categoriaId = categoriaId;
        this.categoria = categoria;
        this.preco = preco;
        this.statusAtivo = statusAtivo;
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
        return ItemVendaModel.TIPO_PRODUTO; // <--- CORREÇÃO: Retorna 1 para o filtro funcionar
    }

    // --- GETTERS E SETTERS PADRÃO ---
    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setPreco(double preco) { this.preco = preco; }
    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }
    public boolean isStatusAtivo() { return statusAtivo; }

    public void setStatusAtivo(boolean statusAtivo) { this.statusAtivo = statusAtivo; }
}