package com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;

import java.io.Serializable;

/**
 * Modelo para as categorias de gastos/receitas.
 * Estrutura:
 * - Raiz: id, tipo, ativa
 * - visual: { nome, icone, cor }
 * - financeiro: { limiteMensal, totalGasto }
 */
public class ContasCategoriaModel implements Serializable {

    // --- CONSTANTES DE CAMINHO (Dot Notation para Updates) ---
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_TIPO = "tipo";
    public static final String CAMPO_ATIVA = "ativa";

    // Caminhos Visual
    public static final String CAMPO_NOME = "visual.nome";
    public static final String CAMPO_ICONE = "visual.icone";
    public static final String CAMPO_COR = "visual.cor";

    // Caminhos Financeiro
    public static final String CAMPO_LIMITE_MENSAL = "financeiro.limiteMensal";
    public static final String CAMPO_TOTAL_GASTO_MES = "financeiro.totalGastoMesAtual";

    // --- ATRIBUTOS DA RAIZ ---
    private String id;
    private int tipo = 0; // 0=Indefinido, 1=Receita, 2=Despesa
    private boolean ativa = true;

    // --- GRUPOS DE DADOS (Mapas) ---
    private Visual visual;
    private Financeiro financeiro;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public ContasCategoriaModel() {
        this.visual = new Visual();
        this.financeiro = new Financeiro();
    }

    // =========================================================================
    // GETTERS E SETTERS DA RAIZ
    // =========================================================================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public Visual getVisual() { return visual; }
    public void setVisual(Visual visual) { this.visual = visual; }

    public Financeiro getFinanceiro() { return financeiro; }
    public void setFinanceiro(Financeiro financeiro) { this.financeiro = financeiro; }

    // =========================================================================
    // CLASSES INTERNAS (Mapas)
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
        // Valores em CENTAVOS (INT)
        private int limiteMensal = 0;
        private int totalGastoMesAtual = 0;

        public Financeiro() {}

        public int getLimiteMensal() { return limiteMensal; }
        public void setLimiteMensal(int limiteMensal) { this.limiteMensal = limiteMensal; }
        public int getTotalGastoMesAtual() { return totalGastoMesAtual; }
        public void setTotalGastoMesAtual(int totalGastoMesAtual) { this.totalGastoMesAtual = totalGastoMesAtual; }
    }

    // =========================================================================
    // HELPERS DE LÓGICA (Mantidos e Adaptados)
    // =========================================================================

    // Atalho para pegar o nome direto (útil para Adapters)
    @Exclude
    public String getNome() {
        return visual != null ? visual.getNome() : "";
    }

    // Atalho para pegar o Total Gasto direto
    @Exclude
    public int getTotalGasto() {
        return financeiro != null ? financeiro.getTotalGastoMesAtual() : 0;
    }

    // Atalho para setar o Total Gasto direto
    @Exclude
    public void setTotalGasto(int valor) {
        if(financeiro != null) financeiro.setTotalGastoMesAtual(valor);
    }

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
        if (financeiro == null || financeiro.getLimiteMensal() <= 0) return 0.0;
        return (double) financeiro.getTotalGastoMesAtual() / financeiro.getLimiteMensal() * 100.0;
    }

    @Exclude
    public boolean estourouLimite() {
        return financeiro != null &&
                financeiro.getLimiteMensal() > 0 &&
                financeiro.getTotalGastoMesAtual() > financeiro.getLimiteMensal();
    }
}