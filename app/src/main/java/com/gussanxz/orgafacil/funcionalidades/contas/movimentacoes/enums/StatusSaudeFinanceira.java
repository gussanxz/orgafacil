package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums;

/**
 * Enum que mapeia o status da saúde financeira do usuário.
 * Faz o link entre o código salvo no Firestore (int) e a descrição na UI.
 */
public enum StatusSaudeFinanceira {
    CRITICO(1, "CRÍTICO"),
    ATENCAO(2, "ATENÇÃO"),
    SAUDAVEL(3, "SAUDÁVEL");

    private final int id;
    private final String descricao;

    StatusSaudeFinanceira(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() { return id; }
    public String getDescricao() { return descricao; }

    /**
     * Converte o ID vindo do banco de volta para o Enum.
     */
    public static StatusSaudeFinanceira desdeId(int id) {
        for (StatusSaudeFinanceira status : values()) {
            if (status.id == id) return status;
        }
        return SAUDAVEL; // Fallback padrão
    }
}