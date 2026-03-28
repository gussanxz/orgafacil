package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.StatusSaudeFinanceira;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

import java.io.Serializable;

public class ResumoFinanceiroModel implements Serializable {

    // --- CONSTANTES ---
    public static final String CAMPO_SALDO_ATUAL = "balanco.saldoAtual";
    public static final String CAMPO_RECEITAS_MES = "balanco.receitasMes";
    public static final String CAMPO_DESPESAS_MES = "balanco.despesasMes";
    public static final String CAMPO_BALANCO_MES = "balanco.balancoMes";
    public static final String CAMPO_GASTOS_HOJE = "balanco.gastosHoje";

    public static final String CAMPO_PREVISAO_FINAL_MES = "inteligencia.previsaoFinalMes";
    public static final String CAMPO_LIMITE_GASTOS_MENSAL = "inteligencia.limiteGastosMensal";
    public static final String CAMPO_ECONOMIA_MES = "inteligencia.economiaMes";
    public static final String CAMPO_STATUS_SAUDE_FINANCEIRA = "inteligencia.statusSaudeFinanceira";

    public static final String CAMPO_MAIOR_CAT_ID = "maiorCategoria.id";
    public static final String CAMPO_MAIOR_CAT_NOME = "maiorCategoria.nome";
    public static final String CAMPO_MAIOR_CAT_ICONE = "maiorCategoria.icone";
    public static final String CAMPO_MAIOR_CAT_COR = "maiorCategoria.cor";
    public static final String CAMPO_MAIOR_CAT_TOTAL = "maiorCategoria.total";
    public static final String CAMPO_MAIOR_CAT_TIPO = "maiorCategoria.tipo";

    public static final String CAMPO_PENDENCIAS_PAGAR = "pendencias.pagar";
    public static final String CAMPO_PENDENCIAS_RECEBER = "pendencias.receber";

    public static final String CAMPO_ULTIMA_ATUALIZACAO = "ultimaAtualizacao";
    public static final String CAMPO_DATA_ULTIMO_RESET = "dataUltimoReset";

    private Balanco balanco;
    private Inteligencia inteligencia;
    private MaiorCategoria maiorCategoria;
    private Pendencias pendencias;

    @ServerTimestamp
    private Timestamp ultimaAtualizacao;

    // NOVO CAMPO
    private Timestamp dataUltimoReset;

    public ResumoFinanceiroModel() {
        this.balanco = new Balanco();
        this.inteligencia = new Inteligencia();
        this.maiorCategoria = new MaiorCategoria();
        this.pendencias = new Pendencias();
    }

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

    public Timestamp getDataUltimoReset() { return dataUltimoReset; }
    public void setDataUltimoReset(Timestamp dataUltimoReset) { this.dataUltimoReset = dataUltimoReset; }

    public static class Balanco implements Serializable {
        private long saldoAtual = 0;
        private long receitasMes = 0;
        private long despesasMes = 0;
        private long balancoMes = 0;
        private long gastosHoje = 0;

        public Balanco() {}

        public long getSaldoAtual() { return saldoAtual; }
        public void setSaldoAtual(long saldoAtual) { this.saldoAtual = saldoAtual; }
        public long getReceitasMes() { return receitasMes; }
        public void setReceitasMes(long receitasMes) { this.receitasMes = receitasMes; }
        public long getDespesasMes() { return despesasMes; }
        public void setDespesasMes(long despesasMes) { this.despesasMes = despesasMes; }
        public long getBalancoMes() { return balancoMes; }
        public void setBalancoMes(long balancoMes) { this.balancoMes = balancoMes; }
        public long getGastosHoje() { return gastosHoje; }
        public void setGastosHoje(long gastosHoje) { this.gastosHoje = gastosHoje; }
    }

    public static class Inteligencia implements Serializable {
        private long previsaoFinalMes = 0;
        private long limiteGastosMensal = 0;
        private long economiaMes = 0;
        private int statusSaudeFinanceira = 3;

        public Inteligencia() {}

        public long getPrevisaoFinalMes() { return previsaoFinalMes; }
        public void setPrevisaoFinalMes(long previsaoFinalMes) { this.previsaoFinalMes = previsaoFinalMes; }
        public long getLimiteGastosMensal() { return limiteGastosMensal; }
        public void setLimiteGastosMensal(long limiteGastosMensal) { this.limiteGastosMensal = limiteGastosMensal; }
        public long getEconomiaMes() { return economiaMes; }
        public void setEconomiaMes(long economiaMes) { this.economiaMes = economiaMes; }
        public int getStatusSaudeFinanceira() { return statusSaudeFinanceira; }
        public void setStatusSaudeFinanceira(int statusSaudeFinanceira) { this.statusSaudeFinanceira = statusSaudeFinanceira; }
    }

    public static class MaiorCategoria implements Serializable {
        private String id = "";
        private String nome = "";
        private String icone = "";
        private String cor = "";
        private long total = 0;
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
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public int getTipo() { return tipo; }
        public void setTipo(int tipo) { this.tipo = tipo; }
    }

    public static class Pendencias implements Serializable {
        private long pagar = 0;
        private long receber = 0;

        public Pendencias() {}

        public long getPagar() { return pagar; }
        public void setPagar(long pagar) { this.pagar = pagar; }
        public long getReceber() { return receber; }
        public void setReceber(long receber) { this.receber = receber; }
    }

    // ── helpers @Exclude ──────────────────────────────────────────────────────

    /**
     * Retorna o status de saúde calculado dinamicamente a partir dos dados atuais.
     * calcularStatusSaude() é chamado aqui automaticamente — o Repository não precisa
     * mais invocar isso manualmente após o toObject().
     */
    @Exclude
    public StatusSaudeFinanceira getStatusSaudeEnum() {
        calcularStatusSaude();
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

    @Exclude
    public void calcularStatusSaude() {
        long despesas = balanco.getDespesasMes();
        long receitas = balanco.getReceitasMes();
        long limite = inteligencia.getLimiteGastosMensal();

        if (despesas > receitas || (limite > 0 && despesas > limite)) {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.CRITICO.getId());
            return;
        }

        double percReceita = receitas > 0 ? (double) despesas / receitas : 0;
        double percLimite = limite > 0 ? (double) despesas / limite : 0;

        if (percReceita >= 0.8 || percLimite >= 0.8) {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.ATENCAO.getId());
        } else {
            inteligencia.setStatusSaudeFinanceira(StatusSaudeFinanceira.SAUDAVEL.getId());
        }
    }

    @Exclude
    public double getPercentualLimiteUsado() {
        long limite = inteligencia.getLimiteGastosMensal();
        if (limite <= 0) return 0.0;
        return (double) balanco.getDespesasMes() / limite * 100.0;
    }
}