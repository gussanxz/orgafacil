package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model;

import com.google.firebase.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Testa ResumoParcelamentoModel.calcular() — lógica pura sem Android/Firestore.
 */
@RunWith(RobolectricTestRunner.class)
public class ResumoParcelamentoModelTest {

    // ── fábrica ───────────────────────────────────────────────────────────────

    private MovimentacaoModel parcela(int numero, long valorCentavos, boolean pago) {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setRecorrencia_id("rec-001");
        m.setParcela_atual(numero);
        m.setTotal_parcelas(3);
        m.setValor(valorCentavos);
        m.setPago(pago);
        m.setData_vencimento(new Timestamp(new Date()));
        return m;
    }

    // ── lista nula / vazia ────────────────────────────────────────────────────

    @Test
    public void calcular_listaNula_retornaModeloVazio() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(null);
        assertEquals(0, r.totalParcelas);
        assertEquals(0, r.parcelasPagas);
        assertEquals(0, r.valorTotalCentavos);
    }

    @Test
    public void calcular_listaVazia_retornaModeloVazio() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Collections.emptyList());
        assertEquals(0, r.totalParcelas);
    }

    // ── totais corretos ───────────────────────────────────────────────────────

    @Test
    public void calcular_3parcelas_totaisCorretos() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 10000, true),   // paga R$100
                parcela(2, 10000, true),   // paga R$100
                parcela(3, 10000, false)   // pendente R$100
        ));

        assertEquals(3, r.totalParcelas);
        assertEquals(2, r.parcelasPagas);
        assertEquals(1, r.parcelasPendentes);
        assertEquals(30000, r.valorTotalCentavos);
        assertEquals(20000, r.valorPagoCentavos);
        assertEquals(10000, r.valorRestanteCentavos);
    }

    @Test
    public void calcular_todasPagas_semPendentes() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 5000, true),
                parcela(2, 5000, true)
        ));

        assertEquals(2, r.parcelasPagas);
        assertEquals(0, r.parcelasPendentes);
        assertNull(r.proximaPendente);
        assertEquals(10000, r.valorPagoCentavos);
        assertEquals(0, r.valorRestanteCentavos);
    }

    @Test
    public void calcular_nenhumaPaga_todasPendentes() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 5000, false),
                parcela(2, 5000, false)
        ));

        assertEquals(0, r.parcelasPagas);
        assertEquals(2, r.parcelasPendentes);
        assertEquals(0, r.valorPagoCentavos);
        assertEquals(10000, r.valorRestanteCentavos);
    }

    // ── proximaPendente — seleciona a de menor parcela_atual ─────────────────

    @Test
    public void calcular_proximaPendente_ehADeMenorNumero() {
        MovimentacaoModel p1 = parcela(1, 10000, true);  // paga
        MovimentacaoModel p2 = parcela(2, 10000, false); // pendente — menor número
        MovimentacaoModel p3 = parcela(3, 10000, false); // pendente

        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(p1, p2, p3));

        assertNotNull(r.proximaPendente);
        assertEquals(2, r.proximaPendente.getParcela_atual());
    }

    @Test
    public void calcular_umaParcelaSoPendente_ehAProxima() {
        MovimentacaoModel p = parcela(5, 15000, false);
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Collections.singletonList(p));
        assertNotNull(r.proximaPendente);
        assertEquals(5, r.proximaPendente.getParcela_atual());
    }

    // ── percentualConcluido() ─────────────────────────────────────────────────

    @Test
    public void percentualConcluido_metadePaga_retorna50() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 10000, true),
                parcela(2, 10000, false)
        ));
        assertEquals(50.0, r.percentualConcluido(), 0.01);
    }

    @Test
    public void percentualConcluido_zeroparcelas_retorna0() {
        ResumoParcelamentoModel r = new ResumoParcelamentoModel();
        assertEquals(0.0, r.percentualConcluido(), 0.01);
    }

    @Test
    public void percentualConcluido_todasPagas_retorna100() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 10000, true),
                parcela(2, 10000, true)
        ));
        assertEquals(100.0, r.percentualConcluido(), 0.01);
    }

    // ── recorrenciaId vem da primeira parcela ─────────────────────────────────

    @Test
    public void calcular_recorrenciaId_vemDaPrimeiraParcela() {
        ResumoParcelamentoModel r = ResumoParcelamentoModel.calcular(Arrays.asList(
                parcela(1, 10000, true),
                parcela(2, 10000, false)
        ));
        assertEquals("rec-001", r.recorrenciaId);
    }
}
