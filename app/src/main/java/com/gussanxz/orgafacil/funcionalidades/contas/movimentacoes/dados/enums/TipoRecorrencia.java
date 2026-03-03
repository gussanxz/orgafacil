package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums;

/**
 * Define os tipos de recorrência disponíveis ao criar lançamentos repetidos.
 *
 * Persistido no Firestore como String (nome do enum) para legibilidade.
 *
 * PARCELADO       → divide o valor total pelas parcelas (ex: R$1200 em 12x = R$100/mês)
 * SEMANAL         → repete a cada 7 dias
 * QUINZENAL       → repete a cada 15 dias
 * MENSAL          → repete a cada 1 mês (mesmo dia do mês)
 * CADA_X_DIAS     → repete a cada N dias (N definido pelo usuário)
 * CADA_X_MESES    → repete a cada N meses (N definido pelo usuário)
 */
public enum TipoRecorrencia {

    PARCELADO("Parcelar (dividir valor)", "parcelas"),
    SEMANAL("Semanal", "semanas"),
    QUINZENAL("Quinzenal", "quinzenas"),
    MENSAL("Mensal", "meses"),
    CADA_X_DIAS("A cada X dias", "dias"),
    CADA_X_MESES("A cada X meses", "meses");

    private final String descricao;
    private final String unidade; // label da caixa de quantidade

    TipoRecorrencia(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() { return descricao; }
    public String getUnidade()   { return unidade; }

    /** Retorna true se este tipo usa intervalo fixo em DIAS (não meses). */
    public boolean usaDias() {
        return this == SEMANAL || this == QUINZENAL || this == CADA_X_DIAS;
    }

    /** Retorna true se o campo de quantidade de repetições deve aparecer. */
    public boolean precisaDeQuantidade() {
        return this != SEMANAL && this != QUINZENAL;
    }

    /**
     * Intervalo padrão em dias para tipos que não precisam que o usuário informe.
     * Retorna -1 para os tipos baseados em meses ou que o usuário precisa informar.
     */
    public int intervaloPadraoEmDias() {
        switch (this) {
            case SEMANAL:    return 7;
            case QUINZENAL:  return 15;
            default:         return -1; // usuário informa
        }
    }

    /** Fallback seguro ao deserializar do Firestore. */
    public static TipoRecorrencia fromString(String valor) {
        if (valor == null) return MENSAL;
        try {
            return TipoRecorrencia.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return MENSAL;
        }
    }
}