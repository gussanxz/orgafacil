package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

public class CatalogoModel implements ItemVendaModel {

    public static final String TIPO_STR_PRODUTO = "produto";
    public static final String TIPO_STR_SERVICO = "servico";

    private String id;
    private String nome;       // Produto: nome do produto / Serviço: nome/descrição do serviço
    private String descricao;  // Detalhe adicional (opcional nos dois casos)
    private String categoriaId;
    private String categoria;
    private double preco;
    private boolean statusAtivo = true;
    private int iconeIndex = 7;
    private String urlFoto = null;
    private String tipo = TIPO_STR_PRODUTO; // "produto" ou "servico"

    public CatalogoModel() {}

    // ── Interface ItemVendaModel ──────────────────────────────────────
    @Override public String getId()       { return id; }
    @Override public String getNome()     { return nome != null ? nome : ""; }
    @Override public String getDescricao(){
        if (descricao != null && !descricao.trim().isEmpty()) return descricao;
        return categoria != null ? categoria : "";
    }
    @Override public double getPreco()    { return preco; }
    @com.google.firebase.firestore.Exclude
    @Override public int getTipo() {
        return TIPO_STR_SERVICO.equals(tipo)
                ? ItemVendaModel.TIPO_SERVICO
                : ItemVendaModel.TIPO_PRODUTO;
    }

    // ── Getters / Setters ─────────────────────────────────────────────
    public void setId(String id)                   { this.id = id; }
    public void setNome(String nome)               { this.nome = nome; }
    public void setDescricao(String descricao)     { this.descricao = descricao; }
    public String getCategoriaId()                 { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public String getCategoria()                   { return categoria; }
    public void setCategoria(String categoria)     { this.categoria = categoria; }
    public void setPreco(double preco)             { this.preco = preco; }
    public boolean isStatusAtivo()                 { return statusAtivo; }
    public void setStatusAtivo(boolean statusAtivo){ this.statusAtivo = statusAtivo; }
    public int getIconeIndex()                     { return iconeIndex; }
    public void setIconeIndex(int iconeIndex)      { this.iconeIndex = iconeIndex; }
    @com.google.firebase.firestore.PropertyName("tipo")
    public String getTipoStr()                     { return tipo; }
    @com.google.firebase.firestore.PropertyName("tipo")
    public void setTipo(String tipo)               { this.tipo = tipo; }

    public boolean isProduto() { return TIPO_STR_PRODUTO.equals(tipo); }
    public boolean isServico() { return TIPO_STR_SERVICO.equals(tipo); }
    @com.google.firebase.firestore.PropertyName("urlFoto")
    public String getUrlFoto() { return urlFoto; }

    @com.google.firebase.firestore.PropertyName("urlFoto")
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
    public boolean temFoto()                { return urlFoto != null && !urlFoto.isEmpty(); }
}