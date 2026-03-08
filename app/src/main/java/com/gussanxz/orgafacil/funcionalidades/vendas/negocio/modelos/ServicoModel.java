package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

// 1. OBRIGATÓRIO: "implements ItemVenda"
public class ServicoModel implements ItemVendaModel {
    private String id;
    private String descricao;
    private String categoria;
    private String categoriaId;
    private double valor;
    private boolean statusAtivo = true;

    public ServicoModel() {}

    public ServicoModel(String id, String descricao, String categoriaId, String categoria, double valor, boolean statusAtivo) {
        this.id = id;
        this.descricao = descricao;
        this.categoriaId = categoriaId;
        this.categoria = categoria;
        this.valor = valor;
        this.statusAtivo = statusAtivo;
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
    public String getDescricao() { return descricao; }

    @Override
    public double getPreco() { return valor; }

    @Override
    public int getTipo() {
        return ItemVendaModel.TIPO_SERVICO; // Retorna 2 (Isso define a cor Laranja)
    }

    // --- GETTERS E SETTERS PADRÃO ---
    public void setId(String id) { this.id = id; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public boolean isStatusAtivo() { return statusAtivo; }
    public void setStatusAtivo(boolean statusAtivo) { this.statusAtivo = statusAtivo; }
}