package com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;

import java.io.Serializable;

/**
 * Modelo para as categorias de gastos/receitas.
 * Estrutura Firestore:
 * - visual: { nome, icone, cor }
 * - financeiro: { limiteMensal, totalGastoMesAtual }
 */
public class ContasCategoriaModel implements Serializable {

    // --- CONSTANTES DE CAMINHO ---
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_TIPO = "tipo";
    public static final String CAMPO_ATIVA = "ativa";
    public static final String CAMPO_NOME = "visual.nome";
    public static final String CAMPO_ICONE = "visual.icone";
    public static final String CAMPO_COR = "visual.cor";
    public static final String CAMPO_LIMITE_MENSAL = "financeiro.limiteMensal";
    public static final String CAMPO_TOTAL_GASTO_MES = "financeiro.totalGastoMesAtual";

    private String id;
    private int tipo = 0;
    private boolean ativa = true;

    private Visual visual;
    private Financeiro financeiro;

    public ContasCategoriaModel() {
        // Inicialização obrigatória para evitar NullPointer ao usar setters imediatamente
        this.visual = new Visual();
        this.financeiro = new Financeiro();
    }

    // =========================================================================
    // GETTERS E SETTERS (Padrão Firestore)
    // =========================================================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    @PropertyName("ativa") // Garante que o Firebase use "ativa" e não "isAtiva"
    public boolean isAtiva() { return ativa; }

    @PropertyName("ativa")
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public Visual getVisual() { return visual; }
    public void setVisual(Visual visual) { this.visual = visual; }

    public Financeiro getFinanceiro() { return financeiro; }
    public void setFinanceiro(Financeiro financeiro) { this.financeiro = financeiro; }

    // =========================================================================
    // CLASSES INTERNAS (Mapeadas como Sub-Objetos)
    // =========================================================================

    public static class Visual implements Serializable {
        private String nome;
        private String icone;
        private String cor;

        public Visual() {}

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getIcone() { return icone; }
        public void setIcone(String icone) { this.icone = icone; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
    }

    public static class Financeiro implements Serializable {
        private int limiteMensal = 0; // CENTAVOS
        private int totalGastoMesAtual = 0; // CENTAVOS

        public Financeiro() {}

        public int getLimiteMensal() { return limiteMensal; }
        public void setLimiteMensal(int limiteMensal) { this.limiteMensal = limiteMensal; }
        public int getTotalGastoMesAtual() { return totalGastoMesAtual; }
        public void setTotalGastoMesAtual(int totalGastoMesAtual) { this.totalGastoMesAtual = totalGastoMesAtual; }
    }

    // =========================================================================
    // HELPERS DE LÓGICA (Excluídos do Firestore para evitar dados redundantes)
    // =========================================================================

    @Exclude
    public String getNome() {
        return (visual != null && visual.getNome() != null) ? visual.getNome() : "";
    }

    @Exclude
    public int getTotalGasto() {
        return financeiro != null ? financeiro.getTotalGastoMesAtual() : 0;
    }

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        return TipoCategoriaContas.desdeId(this.tipo);
    }

    @Exclude
    public double getPercentualUso() {
        if (financeiro == null || financeiro.getLimiteMensal() <= 0) return 0.0;
        // Cálculo entre inteiros: Multiplicamos por 100.0 (double) para não perder precisão
        return (double) financeiro.getTotalGastoMesAtual() / financeiro.getLimiteMensal() * 100.0;
    }

    @Exclude
    public boolean estourouLimite() {
        return financeiro != null &&
                financeiro.getLimiteMensal() > 0 &&
                financeiro.getTotalGastoMesAtual() > financeiro.getLimiteMensal();
    }
}