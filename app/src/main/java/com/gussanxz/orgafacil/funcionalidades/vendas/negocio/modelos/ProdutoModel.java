package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

public class ProdutoModel implements ItemVendaModel {
    private String id;
    private String nome;
    private String descricao;
    private String categoriaId;
    private String categoria;
    private double preco;
    private boolean statusAtivo = true;
    private int iconeIndex = 7;
    private int tipo = ItemVendaModel.TIPO_PRODUTO;

    public ProdutoModel() {}

    public ProdutoModel(String id, String nome, String descricao, String categoriaId, String categoria,
                        double preco, boolean statusAtivo, int iconeIndex) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoriaId = categoriaId;
        this.categoria = categoria;
        this.preco = preco;
        this.statusAtivo = statusAtivo;
        this.iconeIndex = iconeIndex;
    }

    // --- MÉTODOS OBRIGATÓRIOS DA INTERFACE (CORRIGIDOS) ---

    @Override
    public String getId() { return id; }

    @Override
    public String getNome() { return nome; }

    @Override
    public String getDescricao() {
        if (descricao != null && !descricao.trim().isEmpty()) {
            return descricao;
        }
        return categoria != null ? categoria : "";
    }

    @Override
    public double getPreco() { return preco; }

    @Override
    public int getTipo() { return ItemVendaModel.TIPO_PRODUTO; }
    public void setTipo(int tipo) {}

    // --- GETTERS E SETTERS PADRÃO ---
    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public void setPreco(double preco) { this.preco = preco; }

    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }

    public boolean isStatusAtivo() { return statusAtivo; }
    public void setStatusAtivo(boolean statusAtivo) { this.statusAtivo = statusAtivo; }

    public int getIconeIndex() { return iconeIndex; }
    public void setIconeIndex(int iconeIndex) { this.iconeIndex = iconeIndex; }
}