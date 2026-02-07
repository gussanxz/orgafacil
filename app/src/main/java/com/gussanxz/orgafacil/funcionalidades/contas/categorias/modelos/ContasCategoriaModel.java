package com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;

/**
 * Modelo para as categorias de gastos/receitas (Ex: Alimentação, Lazer).
 * Define os limites mensais que alimentam a saúde financeira do usuário.
 * REGRA DE OURO: Valores monetários em INT (centavos).
 */
public class ContasCategoriaModel {

    // --- Constantes para Mapeamento (Âncoras para Repositories) ---
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_NOME = "nome";
    public static final String CAMPO_ICONE = "icone";
    public static final String CAMPO_COR = "cor";
    public static final String CAMPO_TIPO = "tipo";
    public static final String CAMPO_LIMITE_MENSAL = "limite_mensal";
    public static final String CAMPO_TOTAL_GASTO_MES = "total_gasto_mes_atual";
    public static final String CAMPO_ATIVA = "ativa";

    private String id;
    private String nome;
    private String icone;
    private String cor;

    // Começa em 0 (indefinido) para forçar a escolha correta na criação
    private int tipo = 0;

    // Valores em centavos começam zerados
    private int limite_mensal = 0;
    private int total_gasto_mes_atual = 0;

    private boolean ativa = true;

    public ContasCategoriaModel() {}

    // --- Getters e Setters com PropertyName ---

    @PropertyName(CAMPO_ID)
    public String getId() { return id; }
    @PropertyName(CAMPO_ID)
    public void setId(String id) { this.id = id; }

    @PropertyName(CAMPO_NOME)
    public String getNome() { return nome; }
    @PropertyName(CAMPO_NOME)
    public void setNome(String nome) { this.nome = nome; }

    @PropertyName(CAMPO_ICONE)
    public String getIcone() { return icone; }
    @PropertyName(CAMPO_ICONE)
    public void setIcone(String icone) { this.icone = icone; }

    @PropertyName(CAMPO_COR)
    public String getCor() { return cor; }
    @PropertyName(CAMPO_COR)
    public void setCor(String cor) { this.cor = cor; }

    @PropertyName(CAMPO_TIPO)
    public int getTipo() { return tipo; }
    @PropertyName(CAMPO_TIPO)
    public void setTipo(int tipo) { this.tipo = tipo; }

    @PropertyName(CAMPO_LIMITE_MENSAL)
    public int getLimiteMensal() { return limite_mensal; }
    @PropertyName(CAMPO_LIMITE_MENSAL)
    public void setLimiteMensal(int limite_mensal) { this.limite_mensal = limite_mensal; }

    @PropertyName(CAMPO_TOTAL_GASTO_MES)
    public int getTotalGastoMesAtual() { return total_gasto_mes_atual; }
    @PropertyName(CAMPO_TOTAL_GASTO_MES)
    public void setTotalGastoMesAtual(int total_gasto_mes_atual) { this.total_gasto_mes_atual = total_gasto_mes_atual; }

    @PropertyName(CAMPO_ATIVA)
    public boolean isAtiva() { return ativa; }
    @PropertyName(CAMPO_ATIVA)
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    // --- Helpers de Enum e Lógica (Excluídos do Firestore) ---

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        return TipoCategoriaContas.desdeId(this.tipo);
    }

    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) {
            this.tipo = tipoEnum.getId();
        }
    }

    @Exclude
    public double getPercentualUso() {
        if (limite_mensal <= 0) return 0.0;
        return (double) total_gasto_mes_atual / limite_mensal * 100.0;
    }

    @Exclude
    public boolean estourouLimite() {
        return limite_mensal > 0 && total_gasto_mes_atual > limite_mensal;
    }
}