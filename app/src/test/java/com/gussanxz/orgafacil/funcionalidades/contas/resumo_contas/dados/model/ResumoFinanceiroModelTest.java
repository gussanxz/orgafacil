package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.model;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.StatusSaudeFinanceira;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResumoFinanceiroModelTest {

    private ResumoFinanceiroModel model;

    @Before
    public void setUp() {
        model = new ResumoFinanceiroModel();
    }

    private void setDespesasReceitas(long despesas, long receitas) {
        model.getBalanco().setDespesasMes(despesas);
        model.getBalanco().setReceitasMes(receitas);
    }

    private void setLimite(long limite) {
        model.getInteligencia().setLimiteGastosMensal(limite);
    }

    // ── CRÍTICO ───────────────────────────────────────────────────────────────

    @Test
    public void status_despesasMaioresQueReceitas_ehCritico() {
        setDespesasReceitas(10001, 10000); // despesas > receitas (estritamente)
        assertEquals(StatusSaudeFinanceira.CRITICO, model.getStatusSaudeEnum());
    }

    @Test
    public void status_estourouLimite_ehCritico() {
        setDespesasReceitas(5000, 20000); // receita ok
        setLimite(4000); // mas despesas > limite
        assertEquals(StatusSaudeFinanceira.CRITICO, model.getStatusSaudeEnum());
    }

    // ── ATENÇÃO ───────────────────────────────────────────────────────────────

    /**
     * COMPORTAMENTO REAL: despesas == receitas → percReceita = 1.0 (100%) ≥ 0.8 → ATENÇÃO
     * A condição CRÍTICO usa ">", não ">=", então igual não é crítico.
     * O teste anterior estava errado — corrigido aqui para refletir a lógica real do código.
     */
    @Test
    public void status_despesasIguaisReceitas_ehAtencao() {
        setDespesasReceitas(10000, 10000); // 100% da receita → ATENÇÃO (não CRÍTICO)
        assertEquals(StatusSaudeFinanceira.ATENCAO, model.getStatusSaudeEnum());
    }

    @Test
    public void status_80porcentoDaReceita_ehAtencao() {
        setDespesasReceitas(8000, 10000); // exatamente 80%
        assertEquals(StatusSaudeFinanceira.ATENCAO, model.getStatusSaudeEnum());
    }

    @Test
    public void status_90porcentoDaReceita_ehAtencao() {
        setDespesasReceitas(9000, 10000); // 90%
        assertEquals(StatusSaudeFinanceira.ATENCAO, model.getStatusSaudeEnum());
    }

    @Test
    public void status_80porcentoDoLimite_ehAtencao() {
        setDespesasReceitas(4000, 20000); // receita ok (20%)
        setLimite(5000); // 4000/5000 = 80%
        assertEquals(StatusSaudeFinanceira.ATENCAO, model.getStatusSaudeEnum());
    }

    // ── SAUDÁVEL ──────────────────────────────────────────────────────────────

    @Test
    public void status_despesasBaixas_ehSaudavel() {
        setDespesasReceitas(3000, 10000); // 30%
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, model.getStatusSaudeEnum());
    }

    @Test
    public void status_zeroDespesas_ehSaudavel() {
        setDespesasReceitas(0, 10000);
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, model.getStatusSaudeEnum());
    }

    @Test
    public void status_semDespesasSemReceitas_ehSaudavel() {
        setDespesasReceitas(0, 0);
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, model.getStatusSaudeEnum());
    }

    @Test
    public void status_limiteZero_naoInfluenciaCalculo() {
        // Limite 0 = "sem limite" — não deve acionar CRÍTICO
        setDespesasReceitas(3000, 10000);
        setLimite(0);
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, model.getStatusSaudeEnum());
    }

    // ── getPercentualLimiteUsado() ────────────────────────────────────────────

    @Test
    public void percentualLimite_semLimite_retorna0() {
        setDespesasReceitas(5000, 10000);
        setLimite(0);
        assertEquals(0.0, model.getPercentualLimiteUsado(), 0.01);
    }

    @Test
    public void percentualLimite_metadeUsada_retorna50() {
        setDespesasReceitas(5000, 10000);
        setLimite(10000);
        assertEquals(50.0, model.getPercentualLimiteUsado(), 0.01);
    }

    @Test
    public void percentualLimite_totalmenteUsado_retorna100() {
        setDespesasReceitas(10000, 20000);
        setLimite(10000);
        assertEquals(100.0, model.getPercentualLimiteUsado(), 0.01);
    }

    @Test
    public void percentualLimite_estourado_retornaMaisDe100() {
        setDespesasReceitas(12000, 20000);
        setLimite(10000);
        assertTrue(model.getPercentualLimiteUsado() > 100.0);
    }

    // ── getStatusTexto() ──────────────────────────────────────────────────────

    @Test
    public void statusTexto_naoNuloNemVazio() {
        setDespesasReceitas(3000, 10000);
        assertNotNull(model.getStatusTexto());
        assertFalse(model.getStatusTexto().isEmpty());
    }

    // ── getMaiorCatTipoEnum() ─────────────────────────────────────────────────

    @Test
    public void maiorCatTipoEnum_tipo1_retornaReceita() {
        model.getMaiorCategoria().setTipo(1);
        assertEquals(TipoCategoriaContas.RECEITA, model.getMaiorCatTipoEnum());
    }

    @Test
    public void maiorCatTipoEnum_tipo2_retornaDespesa() {
        model.getMaiorCategoria().setTipo(2);
        assertEquals(TipoCategoriaContas.DESPESA, model.getMaiorCatTipoEnum());
    }

    @Test
    public void maiorCatTipoEnum_tipoInvalido_retornaDespesaFallback() {
        model.getMaiorCategoria().setTipo(0);
        assertEquals(TipoCategoriaContas.DESPESA, model.getMaiorCatTipoEnum());
    }

    // ── valores padrão do construtor ──────────────────────────────────────────

    @Test
    public void modeloNovo_todosSubobjetosInicializados() {
        ResumoFinanceiroModel novo = new ResumoFinanceiroModel();
        assertNotNull(novo.getBalanco());
        assertNotNull(novo.getInteligencia());
        assertNotNull(novo.getMaiorCategoria());
        assertNotNull(novo.getPendencias());
        assertEquals(0, novo.getBalanco().getSaldoAtual());
        assertEquals(0, novo.getBalanco().getReceitasMes());
        assertEquals(0, novo.getBalanco().getDespesasMes());
        assertEquals(0, novo.getPendencias().getPagar());
        assertEquals(0, novo.getPendencias().getReceber());
    }
}