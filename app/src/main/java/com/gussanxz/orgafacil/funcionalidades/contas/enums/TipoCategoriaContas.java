package com.gussanxz.orgafacil.funcionalidades.contas.enums;

/**
 * Define se a categoria ou movimentação é de entrada ou saída.
 * Persistido no Firestore como INT para performance.
 */
public enum TipoCategoriaContas {
    RECEITA(1, "RECEITA"),
    DESPESA(2, "DESPESA"),
    RECEITA_FUTURA(3, "Receita Futura"),
    DESPESA_FUTURA(4, "Despesa Futura");

    private final int id;
    private final String descricao;

    TipoCategoriaContas(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() { return id; }
    public String getDescricao() { return descricao; }

    /**
     * Converte o ID vindo do banco de volta para o Enum.
     */
    public static TipoCategoriaContas desdeId(int id) {
        for (TipoCategoriaContas tipo : values()) {
            if (tipo.id == id) return tipo;
        }
        return DESPESA; // Fallback padrão
    }
}