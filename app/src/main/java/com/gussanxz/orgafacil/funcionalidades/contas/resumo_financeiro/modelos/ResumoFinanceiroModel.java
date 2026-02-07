package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.StatusSaudeFinanceira;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;

import java.io.Serializable;

/**
 * ResumoFinanceiroModel (O "Documento Mágico")
 * Centraliza os saldos e indicadores para a Home Screen.
 * Estrutura no Firestore:
 * - balanco: { saldoAtual, receitasMes, despesasMes... }
 * - inteligencia: { previsaoFinalMes, limiteGastos... }
 * - maiorCategoria: { nome, total, icone... }
 * - pendencias: { pagar, receber }
 */
public class ResumoFinanceiroModel implements Serializable {

    // --- CONSTANTES DE CAMINHO (Dot Notation para Updates Parciais) ---
    // Grupo Balanço
    public static final String CAMPO_SALDO_ATUAL = "balanco.saldoAtual";
    public static final String CAMPO_RECEITAS_MES = "balanco.receitasMes";
    public static final String CAMPO_DESPESAS_MES = "balanco.despesasMes";
    public static final String CAMPO_BALANCO_MES = "balanco.balancoMes";
    public static final String CAMPO_GASTOS_HOJE = "balanco.gastosHoje";

    // Grupo Inteligência
    public static final String CAMPO_PREVISAO_FINAL_MES = "inteligencia.previsaoFinalMes";
    public static final String CAMPO_LIMITE_GASTOS_MENSAL = "inteligencia.limiteGastosMensal";
    public static final String CAMPO_ECONOMIA_MES = "inteligencia.economiaMes";
    public static final String CAMPO_STATUS_SAUDE_FINANCEIRA = "inteligencia.statusSaudeFinanceira";

    // Grupo Maior Categoria
    public static final String CAMPO_MAIOR_CAT_ID = "maiorCategoria.id";
    public static final String CAMPO_MAIOR_CAT_NOME = "maiorCategoria.nome";
    public static final String CAMPO_MAIOR_CAT_ICONE = "maiorCategoria.icone";
    public static final String CAMPO_MAIOR_CAT_COR = "maiorCategoria.cor";
    public static final String CAMPO_MAIOR_CAT_TOTAL = "maiorCategoria.total";
    public static final String CAMPO_MAIOR_CAT_TIPO = "maiorCategoria.tipo";

    // Grupo Pendências
    public static final String CAMPO_PENDENCIAS_PAGAR = "pendencias.pagar";
    public static final String CAMPO_PENDENCIAS_RECEBER = "pendencias.receber";

    // Raiz
    public static final String CAMPO_ULTIMA_ATUALIZACAO = "ultimaAtualizacao";


    // =========================================================================
    // GRUPOS DE DADOS (Mapas)
    // =========================================================================
    private Balanco balanco;
    private Inteligencia inteligencia;
    private MaiorCategoria maiorCategoria;
    private Pendencias pendencias;

    @ServerTimestamp
    private Timestamp ultimaAtualizacao;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public ResumoFinanceiroModel() {
        this.balanco = new Balanco();
        this.inteligencia = new Inteligencia();
        this.maiorCategoria = new MaiorCategoria();
        this.pendencias = new Pendencias();
    }

    // =========================================================================
    // GETTERS E SETTERS DA RAIZ
    // =========================================================================

    public Balanco getBalanco() { return balanco; }
    public void setBalanco(Balanco balanco) { this.balanco = balanco; }

    public Inteligencia getInteligencia() { return inteligencia; }
    public void setInteligencia(Inteligencia inteligencia) { this.inteligencia = inteligencia; }

    public MaiorCategoria getMaiorCategoria() { return maiorCategoria; }
    public void setMaiorCategoria(MaiorCategoria maiorCategoria) { this.maiorCategoria = maiorCategoria; }

    public Pendencias getPendencias() { return pendencias; }
    public void setPendencias(Pendencias pendencias) { this.pendencias = pendencias; }

    public Timestamp getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(Timestamp ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }


    // =========================================================================
    // CLASSES INTERNAS (Mapas)
    // =========================================================================

    public static class Balanco implements Serializable {
        private int saldoAtual = 0;
        private int receitasMes = 0;
        private int despesasMes = 0;
        private int balancoMes = 0;
        private int gastosHoje = 0;

        public Balanco() {}

        public int getSaldoAtual() { return saldoAtual; }
        public void setSaldoAtual(int saldoAtual) { this.saldoAtual = saldoAtual; }
        public int getReceitasMes() { return receitasMes; }
        public void setReceitasMes(int receitasMes) { this.receitasMes = receitasMes; }
        public int getDespesasMes() { return despesasMes; }
        public void setDespesasMes(int despesasMes) { this.despesasMes = despesasMes; }
        public int getBalancoMes() { return balancoMes; }
        public void setBalancoMes(int balancoMes) { this.balancoMes = balancoMes; }
        public int getGastosHoje() { return gastosHoje; }
        public void setGastosHoje(int gastosHoje) { this.gastosHoje = gastosHoje; }
    }

    public static class Inteligencia implements Serializable {
        private int previsaoFinalMes = 0;
        private int limiteGastosMensal = 0;
        private int economiaMes = 0;
        private int statusSaudeFinanceira = 3; // Padrão: 3 (Saudável/Neutro)

        public Inteligencia() {}

        public int getPrevisaoFinalMes() { return previsaoFinalMes; }
        public void setPrevisaoFinalMes(int previsaoFinalMes) { this.previsaoFinalMes = previsaoFinalMes; }
        public int getLimiteGastosMensal() { return limiteGastosMensal; }
        public void setLimiteGastosMensal(int limiteGastosMensal) { this.limiteGastosMensal = limiteGastosMensal; }
        public int getEconomiaMes() { return economiaMes; }
        public void setEconomiaMes(int economiaMes) { this.economiaMes = economiaMes; }
        public int getStatusSaudeFinanceira() { return statusSaudeFinanceira; }
        public void setStatusSaudeFinanceira(int statusSaudeFinanceira) { this.statusSaudeFinanceira = statusSaudeFinanceira; }
    }

    public static class MaiorCategoria implements Serializable {
        private String id = "";
        private String nome = "";
        private String icone = "";
        private String cor = "";
        private int total = 0;
        private int tipo = 0;

        public MaiorCategoria() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getIcone() { return icone; }
        public void setIcone(String icone) { this.icone = icone; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getTipo() { return tipo; }
        public void setTipo(int tipo) { this.tipo = tipo; }
    }

    public static class Pendencias implements Serializable {
        private int pagar = 0;
        private int receber = 0;

        public Pendencias() {}

        public int getPagar() { return pagar; }
        public void setPagar(int pagar) { this.pagar = pagar; }
        public int getReceber() { return receber; }
        public void setReceber(int receber) { this.receber = receber; }
    }

    // =========================================================================
    // LÓGICA E HELPERS (Mantidos e Adaptados para a nova estrutura)
    // =========================================================================

    @Exclude
    public StatusSaudeFinanceira getStatusSaudeEnum() {
        return StatusSaudeFinanceira.desdeId(this.inteligencia.getStatusSaudeFinanceira());
    }

    @Exclude
    public String getStatusTexto() {
        return getStatusSaudeEnum().getDescricao();
    }

    @Exclude
    public TipoCategoriaContas getMaiorCatTipoEnum() {
        return TipoCategoriaContas.desdeId(this.maiorCategoria.getTipo());
    }

    /**
     * Calcula o status de saúde baseado na regra de 80% e estouro de limite.
     * Atualiza o valor dentro do grupo 'inteligencia'.
     */
    @Exclude
    public void calcularStatusSaude() {
        int despesas = balanco.getDespesasMes();
        int receitas = balanco.getReceitasMes();
        int limite = inteligencia.getLimiteGastosMensal();

        // Regra Crítica: Gastou mais que ganhou OU Estourou o limite definido
        if (despesas > receitas || (limite > 0 && despesas > limite)) {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.CRITICO.getId());
            return;
        }

        double percReceita = receitas > 0 ? (double) despesas / receitas : 0;
        double percLimite = limite > 0 ? (double) despesas / limite : 0;

        // Regra Atenção: Gastou mais de 80% da receita ou do limite
        if (percReceita >= 0.8 || percLimite >= 0.8) {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.ATENCAO.getId());
        } else {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.SAUDAVEL.getId());
        }
    }

    @Exclude
    public double getPercentualLimiteUsado() {
        int limite = inteligencia.getLimiteGastosMensal();
        if (limite <= 0) return 0.0;
        return (double) balanco.getDespesasMes() / limite * 100.0;
    }
}