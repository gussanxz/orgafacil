package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.StatusSaudeFinanceira;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;

/**
 * ResumoFinanceiroModel (O "Documento Mágico")
 * Centraliza os saldos e indicadores para a Home Screen.
 * * REGRA DE OURO: Todos os valores monetários são INT (armazenando centavos).
 * Exemplo: R$ 10,50 vira 1050.
 * * Local: usuarios > {uid} > contas > resumo_geral
 */
public class ResumoFinanceiroModel {

    // --- Constantes para Mapeamento (Âncoras para Repositories) ---
    public static final String CAMPO_SALDO_ATUAL = "saldo_atual";
    public static final String CAMPO_RECEITAS_MES = "receitas_mes";
    public static final String CAMPO_DESPESAS_MES = "despesas_mes";
    public static final String CAMPO_BALANCO_MES = "balanco_mes";
    public static final String CAMPO_GASTOS_HOJE = "gastos_hoje";
    public static final String CAMPO_PREVISAO_FINAL_MES = "previsao_final_mes";
    public static final String CAMPO_LIMITE_GASTOS_MENSAL = "limite_gastos_mensal";
    public static final String CAMPO_ECONOMIA_MES = "economia_mes";
    public static final String CAMPO_STATUS_SAUDE_FINANCEIRA = "status_saude_financeira";
    public static final String CAMPO_MAIOR_CAT_ID = "maior_cat_id";
    public static final String CAMPO_MAIOR_CAT_NOME = "maior_cat_nome";
    public static final String CAMPO_MAIOR_CAT_ICONE = "maior_cat_icone";
    public static final String CAMPO_MAIOR_CAT_COR = "maior_cat_cor";
    public static final String CAMPO_MAIOR_CAT_TOTAL = "maior_cat_total";
    public static final String CAMPO_MAIOR_CAT_TIPO = "maior_cat_tipo";
    public static final String CAMPO_PENDENCIAS_PAGAR = "pendencias_pagar";
    public static final String CAMPO_PENDENCIAS_RECEBER = "pendencias_receber";
    public static final String CAMPO_ULTIMA_ATUALIZACAO = "ultima_atualizacao";

    // --- Saldos e Fluxo Atual (INT - Centavos) ---
    private int saldo_atual = 0;
    private int receitas_mes = 0;
    private int despesas_mes = 0;
    private int balanco_mes = 0;
    private int gastos_hoje = 0;

    // --- Inteligência e Alertas ---
    private int previsao_final_mes = 0;    // Previsão baseada em contas agendadas
    private int limite_gastos_mensal = 0;  // Teto definido pelo usuário
    private int economia_mes = 0;          // Saldo positivo poupado no mês atual

    // Status de Saúde (Persistido como INT: 1, 2 ou 3 via StatusSaudeFinanceira Enum)
    private int status_saude_financeira = 3; // Começa no neutro/saudável

    // --- Dados da Maior Categoria (Denormalização para Performance) ---
    private String maior_cat_id = "";
    private String maior_cat_nome = "";
    private String maior_cat_icone = "";
    private String maior_cat_cor = "";
    private int maior_cat_total = 0;
    // Tipo da maior categoria (Persistido como INT: 1 ou 2 via TipoCategoria Enum)
    private int maior_cat_tipo = 0;

    // --- Controles de Contagem de Pendência (Quantidade de Contas) ---
    private int pendencias_pagar = 0;
    private int pendencias_receber = 0;

    @ServerTimestamp
    private Timestamp ultima_atualizacao;

    // Construtor padrão obrigatório para o Firebase
    public ResumoFinanceiroModel() {}

    // --- Getters e Setters Padrão (Usados pelo Firestore com PropertyName) ---

    @PropertyName(CAMPO_SALDO_ATUAL)
    public int getSaldo_atual() { return saldo_atual; }
    @PropertyName(CAMPO_SALDO_ATUAL)
    public void setSaldo_atual(int saldo_atual) { this.saldo_atual = saldo_atual; }

    @PropertyName(CAMPO_RECEITAS_MES)
    public int getReceitas_mes() { return receitas_mes; }
    @PropertyName(CAMPO_RECEITAS_MES)
    public void setReceitas_mes(int receitas_mes) { this.receitas_mes = receitas_mes; }

    @PropertyName(CAMPO_DESPESAS_MES)
    public int getDespesas_mes() { return despesas_mes; }
    @PropertyName(CAMPO_DESPESAS_MES)
    public void setDespesas_mes(int despesas_mes) { this.despesas_mes = despesas_mes; }

    @PropertyName(CAMPO_BALANCO_MES)
    public int getBalanco_mes() { return balanco_mes; }
    @PropertyName(CAMPO_BALANCO_MES)
    public void setBalanco_mes(int balanco_mes) { this.balanco_mes = balanco_mes; }

    @PropertyName(CAMPO_GASTOS_HOJE)
    public int getGastos_hoje() { return gastos_hoje; }
    @PropertyName(CAMPO_GASTOS_HOJE)
    public void setGastos_hoje(int gastos_hoje) { this.gastos_hoje = gastos_hoje; }

    @PropertyName(CAMPO_PREVISAO_FINAL_MES)
    public int getPrevisao_final_mes() { return previsao_final_mes; }
    @PropertyName(CAMPO_PREVISAO_FINAL_MES)
    public void setPrevisao_final_mes(int previsao_final_mes) { this.previsao_final_mes = previsao_final_mes; }

    @PropertyName(CAMPO_LIMITE_GASTOS_MENSAL)
    public int getLimite_gastos_mensal() { return limite_gastos_mensal; }
    @PropertyName(CAMPO_LIMITE_GASTOS_MENSAL)
    public void setLimite_gastos_mensal(int limite_gastos_mensal) { this.limite_gastos_mensal = limite_gastos_mensal; }

    @PropertyName(CAMPO_ECONOMIA_MES)
    public int getEconomia_mes() { return economia_mes; }
    @PropertyName(CAMPO_ECONOMIA_MES)
    public void setEconomia_mes(int economia_mes) { this.economia_mes = economia_mes; }

    @PropertyName(CAMPO_STATUS_SAUDE_FINANCEIRA)
    public int getStatus_saude_financeira() { return status_saude_financeira; }
    @PropertyName(CAMPO_STATUS_SAUDE_FINANCEIRA)
    public void setStatus_saude_financeira(int status_saude_financeira) { this.status_saude_financeira = status_saude_financeira; }

    @PropertyName(CAMPO_MAIOR_CAT_ID)
    public String getMaior_cat_id() { return maior_cat_id; }
    @PropertyName(CAMPO_MAIOR_CAT_ID)
    public void setMaior_cat_id(String maior_cat_id) { this.maior_cat_id = maior_cat_id; }

    @PropertyName(CAMPO_MAIOR_CAT_NOME)
    public String getMaior_cat_nome() { return maior_cat_nome; }
    @PropertyName(CAMPO_MAIOR_CAT_NOME)
    public void setMaior_cat_nome(String maior_cat_nome) { this.maior_cat_nome = maior_cat_nome; }

    @PropertyName(CAMPO_MAIOR_CAT_ICONE)
    public String getMaior_cat_icone() { return maior_cat_icone; }
    @PropertyName(CAMPO_MAIOR_CAT_ICONE)
    public void setMaior_cat_icone(String maior_cat_icone) { this.maior_cat_icone = maior_cat_icone; }

    @PropertyName(CAMPO_MAIOR_CAT_COR)
    public String getMaior_cat_cor() { return maior_cat_cor; }
    @PropertyName(CAMPO_MAIOR_CAT_COR)
    public void setMaior_cat_cor(String maior_cat_cor) { this.maior_cat_cor = maior_cat_cor; }

    @PropertyName(CAMPO_MAIOR_CAT_TOTAL)
    public int getMaior_cat_total() { return maior_cat_total; }
    @PropertyName(CAMPO_MAIOR_CAT_TOTAL)
    public void setMaior_cat_total(int maior_cat_total) { this.maior_cat_total = maior_cat_total; }

    @PropertyName(CAMPO_MAIOR_CAT_TIPO)
    public int getMaior_cat_tipo() { return maior_cat_tipo; }
    @PropertyName(CAMPO_MAIOR_CAT_TIPO)
    public void setMaior_cat_tipo(int maior_cat_tipo) { this.maior_cat_tipo = maior_cat_tipo; }

    @PropertyName(CAMPO_PENDENCIAS_PAGAR)
    public int getPendencias_pagar() { return pendencias_pagar; }
    @PropertyName(CAMPO_PENDENCIAS_PAGAR)
    public void setPendencias_pagar(int pendencias_pagar) { this.pendencias_pagar = pendencias_pagar; }

    @PropertyName(CAMPO_PENDENCIAS_RECEBER)
    public int getPendencias_receber() { return pendencias_receber; }
    @PropertyName(CAMPO_PENDENCIAS_RECEBER)
    public void setPendencias_receber(int pendencias_receber) { this.pendencias_receber = pendencias_receber; }

    @PropertyName(CAMPO_ULTIMA_ATUALIZACAO)
    public Timestamp getUltima_atualizacao() { return ultima_atualizacao; }
    @PropertyName(CAMPO_ULTIMA_ATUALIZACAO)
    public void setUltima_atualizacao(Timestamp ultima_atualizacao) { this.ultima_atualizacao = ultima_atualizacao; }

    // --- Métodos de Inteligência (Excluídos via @Exclude) ---

    @Exclude
    public StatusSaudeFinanceira getStatusSaudeEnum() {
        return StatusSaudeFinanceira.desdeId(this.status_saude_financeira);
    }

    @Exclude
    public String getStatusTexto() {
        return getStatusSaudeEnum().getDescricao();
    }

    @Exclude
    public TipoCategoriaContas getMaiorCatTipoEnum() {
        return TipoCategoriaContas.desdeId(this.maior_cat_tipo);
    }

    /**
     * Calcula o status de saúde baseado na regra de 80% e estouro de limite.
     */
    @Exclude
    public void calcularStatusSaude() {
        if (despesas_mes > receitas_mes || (limite_gastos_mensal > 0 && despesas_mes > limite_gastos_mensal)) {
            this.status_saude_financeira = StatusSaudeFinanceira.CRITICO.getId();
            return;
        }

        double percReceita = receitas_mes > 0 ? (double) despesas_mes / receitas_mes : 0;
        double percLimite = limite_gastos_mensal > 0 ? (double) despesas_mes / limite_gastos_mensal : 0;

        if (percReceita >= 0.8 || percLimite >= 0.8) {
            this.status_saude_financeira = StatusSaudeFinanceira.ATENCAO.getId();
        } else {
            this.status_saude_financeira = StatusSaudeFinanceira.SAUDAVEL.getId();
        }
    }

    @Exclude
    public double getPercentualLimiteUsado() {
        if (limite_gastos_mensal <= 0) return 0.0;
        return (double) despesas_mes / limite_gastos_mensal * 100.0;
    }
}