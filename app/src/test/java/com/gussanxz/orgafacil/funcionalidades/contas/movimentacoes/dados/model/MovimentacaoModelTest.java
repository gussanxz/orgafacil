package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model;

import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MovimentacaoModelTest {

    // ── fábrica ───────────────────────────────────────────────────────────────

    /** Cria modelo com data_vencimento = agora + horasOffset horas */
    private MovimentacaoModel comVencimentoEmHoras(int horasOffset, boolean pago) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, horasOffset);
        MovimentacaoModel m = new MovimentacaoModel();
        m.setData_vencimento(new Timestamp(cal.getTime()));
        m.setPago(pago);
        return m;
    }

    /** Cria modelo com data_vencimento = meia-noite do dia (hoje + diasOffset) */
    private MovimentacaoModel comVencimentoEmDias(int diasOffset, boolean pago) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, diasOffset);
        cal.set(Calendar.HOUR_OF_DAY, 12); // meio-dia — longe das bordas de meia-noite
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        MovimentacaoModel m = new MovimentacaoModel();
        m.setData_vencimento(new Timestamp(cal.getTime()));
        m.setPago(pago);
        return m;
    }

    private MovimentacaoModel comMovimentacaoEmDias(int diasOffset, boolean pago) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, diasOffset);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        MovimentacaoModel m = new MovimentacaoModel();
        m.setData_movimentacao(new Timestamp(cal.getTime()));
        m.setPago(pago);
        return m;
    }

    // ── estaVencida() ─────────────────────────────────────────────────────────

    @Test
    public void estaVencida_ontem_naoPago_deveSerTrue() {
        assertTrue(comVencimentoEmDias(-1, false).estaVencida());
    }

    @Test
    public void estaVencida_anteontem_naoPago_deveSerTrue() {
        assertTrue(comVencimentoEmDias(-2, false).estaVencida());
    }

    @Test
    public void estaVencida_hoje_naoPago_deveSerFalse() {
        // Hoje truncado não é "antes de hoje" — não está vencida
        assertFalse(comVencimentoEmDias(0, false).estaVencida());
    }

    @Test
    public void estaVencida_amanha_naoPago_deveSerFalse() {
        assertFalse(comVencimentoEmDias(1, false).estaVencida());
    }

    @Test
    public void estaVencida_pago_sempreRetornaFalse() {
        assertFalse(comVencimentoEmDias(-5, true).estaVencida());
        assertFalse(comVencimentoEmDias(-1, true).estaVencida());
        assertFalse(comVencimentoEmDias(0,  true).estaVencida());
    }

    @Test
    public void estaVencida_semDataVencimento_usaMovimentacaoComoFallback() {
        assertTrue(comMovimentacaoEmDias(-2, false).estaVencida());
    }

    @Test
    public void estaVencida_semNenhumaData_retornaFalseSemCrash() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setPago(false);
        assertFalse(m.estaVencida());
    }

    // ── venceEmBreve() ────────────────────────────────────────────────────────

    /**
     * "Hoje" com offset de dias cria a data às 12h — pode já ter passado 12h
     * do dia atual, o que coloca o vencimento ANTES do momento atual, saindo
     * da janela [0, 48h]. Por isso usamos offset em HORAS: +1h garante que
     * a data está sempre dentro da janela de 48h sem depender do horário do teste.
     */
    @Test
    public void venceEmBreve_em1Hora_deveSerTrue() {
        assertTrue(comVencimentoEmHoras(1, false).venceEmBreve());
    }

    @Test
    public void venceEmBreve_em24Horas_deveSerTrue() {
        assertTrue(comVencimentoEmHoras(24, false).venceEmBreve());
    }

    @Test
    public void venceEmBreve_em47Horas_deveSerTrue() {
        assertTrue(comVencimentoEmHoras(47, false).venceEmBreve());
    }

    @Test
    public void venceEmBreve_em72Horas_deveSerFalse() {
        // 72h está fora da janela de 48h
        assertFalse(comVencimentoEmHoras(72, false).venceEmBreve());
    }

    @Test
    public void venceEmBreve_ontem_deveSerFalse() {
        // Ontem é VENCIDA — não deve gerar notificação dupla
        assertFalse(comVencimentoEmDias(-1, false).venceEmBreve());
    }

    @Test
    public void venceEmBreve_pago_sempreRetornaFalse() {
        assertFalse(comVencimentoEmHoras(1,  true).venceEmBreve());
        assertFalse(comVencimentoEmHoras(24, true).venceEmBreve());
    }

    @Test
    public void venceEmBreve_semData_retornaFalseSemCrash() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setPago(false);
        assertFalse(m.venceEmBreve());
    }

    // ── consistência estaVencida x venceEmBreve ───────────────────────────────

    @Test
    public void vencida_nunca_aparece_como_emBreve() {
        // Contrato fundamental que o VencimentoWorker depende
        MovimentacaoModel m = comVencimentoEmDias(-1, false);
        assertTrue(m.estaVencida());
        assertFalse(m.venceEmBreve());
    }

    @Test
    public void emBreve_nunca_aparece_como_vencida() {
        MovimentacaoModel m = comVencimentoEmHoras(24, false);
        assertFalse(m.estaVencida());
        assertTrue(m.venceEmBreve());
    }

    // ── diasParaVencimento() ──────────────────────────────────────────────────

    @Test
    public void diasParaVencimento_hoje_retorna0() {
        assertEquals(0, comVencimentoEmDias(0, false).diasParaVencimento());
    }

    @Test
    public void diasParaVencimento_amanha_retorna1() {
        assertEquals(1, comVencimentoEmDias(1, false).diasParaVencimento());
    }

    @Test
    public void diasParaVencimento_ontem_retornaNegativo() {
        assertTrue(comVencimentoEmDias(-1, false).diasParaVencimento() < 0);
    }

    @Test
    public void diasParaVencimento_semData_retornaMaxValue() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setPago(false);
        assertEquals(Integer.MAX_VALUE, m.diasParaVencimento());
    }

    @Test
    public void diasParaVencimento_pago_retornaMaxValue() {
        assertEquals(Integer.MAX_VALUE, comVencimentoEmDias(-1, true).diasParaVencimento());
    }

    // ── getTipoEnum() ─────────────────────────────────────────────────────────

    @Test
    public void getTipoEnum_receita_retornaReceita() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setTipoEnum(TipoCategoriaContas.RECEITA);
        assertEquals(TipoCategoriaContas.RECEITA, m.getTipoEnum());
    }

    @Test
    public void getTipoEnum_despesa_retornaDespesa() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setTipoEnum(TipoCategoriaContas.DESPESA);
        assertEquals(TipoCategoriaContas.DESPESA, m.getTipoEnum());
    }

    @Test
    public void getTipoEnum_tipoNulo_retornaDespesaFallback() {
        MovimentacaoModel m = new MovimentacaoModel();
        assertEquals(TipoCategoriaContas.DESPESA, m.getTipoEnum());
    }

    @Test
    public void getTipoIdLegacy_receita_retorna1() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setTipoEnum(TipoCategoriaContas.RECEITA);
        assertEquals(1, m.getTipoIdLegacy());
    }

    @Test
    public void getTipoIdLegacy_despesa_retorna2() {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setTipoEnum(TipoCategoriaContas.DESPESA);
        assertEquals(2, m.getTipoIdLegacy());
    }
}