package com.gussanxz.orgafacil.funcionalidades.vendas.dados.model;

public class ItemSacolaVendaModel implements java.io.Serializable {

    private final String chave;
    private final String itemId;
    private final String nome;
    private final String categoria;
    private final int tipo;

    // Atualizado para int (centavos)
    private final int precoUnitario;

    private int quantidade;

    // Construtor 1: para adicionar item novo na sacola (vem da interface ItemVendaModel)
    public ItemSacolaVendaModel(ItemVendaModel item) {
        this.chave = item.getTipo() + "_" + (item.getId() != null ? item.getId() : item.getNome());
        this.itemId = item.getId();
        this.nome = item.getNome();
        this.tipo = item.getTipo();

        // ItemVendaModel já foi atualizado para retornar int no getPreco()
        this.precoUnitario = item.getPreco();

        this.quantidade = 1;
        this.categoria = (item instanceof CatalogoModel) ? ((CatalogoModel) item).getCategoria() : null;
    }

    // Construtor 2: para restaurar sacola a partir de uma venda já registrada
    public ItemSacolaVendaModel(ItemVendaRegistradaModel item, String categoria) {
        this.chave = item.getTipo() + "_" + item.getItemId();
        this.itemId = item.getItemId();
        this.nome = item.getNome();
        this.tipo = item.getTipo();

        // Cast para (int) mantido temporariamente até atualizarmos o ItemVendaRegistradaModel
        this.precoUnitario = (int) item.getPrecoUnitario();

        this.quantidade = item.getQuantidade();
        this.categoria = categoria;
    }

    public static String gerarChave(ItemVendaModel item) {
        return item.getTipo() + "_" + (item.getId() != null ? item.getId() : item.getNome());
    }

    public void incrementarQuantidade() {
        quantidade++;
    }

    public void decrementarQuantidade() {
        if (quantidade > 0) {
            quantidade--;
        }
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

    // Atualizado para retornar int
    public int getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    // Atualizado para retornar int
    public int getSubtotal() {
        return precoUnitario * quantidade;
    }

    public String getCategoria() { return categoria; }
}