package com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testa os helpers de ContasCategoriaModel:
 * getPercentualUso(), estourouLimite(), getNome(), getTipoEnum()
 *
 * Puro Java — sem Robolectric.
 */
public class ContasCategoriaModelTest {

    private ContasCategoriaModel categoria;

    @Before
    public void setUp() {
        categoria = new ContasCategoriaModel();
    }

    // ── helpers de fábrica ────────────────────────────────────────────────────

    private void setGastoELimite(long gasto, long limite) {
        categoria.getFinanceiro().setTotalGastoMesAtual(gasto);
        categoria.getFinanceiro().setLimiteMensal(limite);
    }

    // ── getPercentualUso() ────────────────────────────────────────────────────

    @Test
    public void percentualUso_semLimite_retorna0() {
        setGastoELimite(5000, 0);
        assertEquals(0.0, categoria.getPercentualUso(), 0.01);
    }

    @Test
    public void percentualUso_metadeUsada_retorna50() {
        setGastoELimite(5000, 10000);
        assertEquals(50.0, categoria.getPercentualUso(), 0.01);
    }

    @Test
    public void percentualUso_totalUsado_retorna100() {
        setGastoELimite(10000, 10000);
        assertEquals(100.0, categoria.getPercentualUso(), 0.01);
    }

    @Test
    public void percentualUso_estourado_retornaMaisQue100() {
        setGastoELimite(12000, 10000);
        assertTrue(categoria.getPercentualUso() > 100.0);
    }

    @Test
    public void percentualUso_zeroGasto_retorna0() {
        setGastoELimite(0, 10000);
        assertEquals(0.0, categoria.getPercentualUso(), 0.01);
    }

    // ── estourouLimite() ──────────────────────────────────────────────────────

    @Test
    public void estourouLimite_abaixoDoLimite_retornaFalse() {
        setGastoELimite(8000, 10000);
        assertFalse(categoria.estourouLimite());
    }

    @Test
    public void estourouLimite_igualAoLimite_retornaFalse() {
        // Igual ainda não "estourou"
        setGastoELimite(10000, 10000);
        assertFalse(categoria.estourouLimite());
    }

    @Test
    public void estourouLimite_acimaDoLimite_retornaTrue() {
        setGastoELimite(10001, 10000);
        assertTrue(categoria.estourouLimite());
    }

    @Test
    public void estourouLimite_semLimite_retornaFalse() {
        // Sem limite definido, nunca pode estourar
        setGastoELimite(99999, 0);
        assertFalse(categoria.estourouLimite());
    }

    // ── getNome() ─────────────────────────────────────────────────────────────

    @Test
    public void getNome_semVisual_retornaStringVazia() {
        // visual é inicializado vazio — não deve lançar NPE
        assertEquals("", categoria.getNome());
    }

    @Test
    public void getNome_comNome_retornaNome() {
        categoria.getVisual().setNome("Alimentação");
        assertEquals("Alimentação", categoria.getNome());
    }

    // ── getTipoEnum() ─────────────────────────────────────────────────────────

    @Test
    public void getTipoEnum_tipo1_retornaReceita() {
        categoria.setTipo(1);
        assertEquals(TipoCategoriaContas.RECEITA, categoria.getTipoEnum());
    }

    @Test
    public void getTipoEnum_tipo2_retornaDespesa() {
        categoria.setTipo(2);
        assertEquals(TipoCategoriaContas.DESPESA, categoria.getTipoEnum());
    }

    @Test
    public void getTipoEnum_tipoInvalido_retornaDespesaFallback() {
        categoria.setTipo(0);
        assertEquals(TipoCategoriaContas.DESPESA, categoria.getTipoEnum());
    }

    // ── getTotalGasto() ───────────────────────────────────────────────────────

    @Test
    public void getTotalGasto_retornaValorCorreto() {
        categoria.getFinanceiro().setTotalGastoMesAtual(7500);
        assertEquals(7500, categoria.getTotalGasto());
    }

    // ── ativa por padrão ─────────────────────────────────────────────────────

    @Test
    public void categoriaNovaEhAtivaPorPadrao() {
        assertTrue(categoria.isAtiva());
    }
}
