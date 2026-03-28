package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums;

import org.junit.Test;
import static org.junit.Assert.*;

public class TipoRecorrenciaTest {

    // ── usaDias() ─────────────────────────────────────────────────────────────

    @Test public void semanal_usaDias() { assertTrue(TipoRecorrencia.SEMANAL.usaDias()); }
    @Test public void quinzenal_usaDias() { assertTrue(TipoRecorrencia.QUINZENAL.usaDias()); }
    @Test public void cadaXDias_usaDias() { assertTrue(TipoRecorrencia.CADA_X_DIAS.usaDias()); }
    @Test public void mensal_naoUsaDias() { assertFalse(TipoRecorrencia.MENSAL.usaDias()); }
    @Test public void parcelado_naoUsaDias() { assertFalse(TipoRecorrencia.PARCELADO.usaDias()); }
    @Test public void cadaXMeses_naoUsaDias() { assertFalse(TipoRecorrencia.CADA_X_MESES.usaDias()); }

    // ── precisaDeQuantidade() ─────────────────────────────────────────────────

    @Test public void semanal_naoPrecisaQuantidade() { assertFalse(TipoRecorrencia.SEMANAL.precisaDeQuantidade()); }
    @Test public void quinzenal_naoPrecisaQuantidade() { assertFalse(TipoRecorrencia.QUINZENAL.precisaDeQuantidade()); }
    @Test public void mensal_precisaQuantidade() { assertTrue(TipoRecorrencia.MENSAL.precisaDeQuantidade()); }
    @Test public void parcelado_precisaQuantidade() { assertTrue(TipoRecorrencia.PARCELADO.precisaDeQuantidade()); }
    @Test public void cadaXDias_precisaQuantidade() { assertTrue(TipoRecorrencia.CADA_X_DIAS.precisaDeQuantidade()); }
    @Test public void cadaXMeses_precisaQuantidade() { assertTrue(TipoRecorrencia.CADA_X_MESES.precisaDeQuantidade()); }

    // ── intervaloPadraoEmDias() ───────────────────────────────────────────────

    @Test public void semanal_intervalo_7() { assertEquals(7, TipoRecorrencia.SEMANAL.intervaloPadraoEmDias()); }
    @Test public void quinzenal_intervalo_15() { assertEquals(15, TipoRecorrencia.QUINZENAL.intervaloPadraoEmDias()); }
    @Test public void mensal_intervalo_menos1() { assertEquals(-1, TipoRecorrencia.MENSAL.intervaloPadraoEmDias()); }
    @Test public void cadaXDias_intervalo_menos1() { assertEquals(-1, TipoRecorrencia.CADA_X_DIAS.intervaloPadraoEmDias()); }
    @Test public void cadaXMeses_intervalo_menos1() { assertEquals(-1, TipoRecorrencia.CADA_X_MESES.intervaloPadraoEmDias()); }

    // ── fromString() ─────────────────────────────────────────────────────────

    @Test public void fromString_mensal_retornaMensal() { assertEquals(TipoRecorrencia.MENSAL, TipoRecorrencia.fromString("MENSAL")); }
    @Test public void fromString_parcelado() { assertEquals(TipoRecorrencia.PARCELADO, TipoRecorrencia.fromString("PARCELADO")); }
    @Test public void fromString_semanal() { assertEquals(TipoRecorrencia.SEMANAL, TipoRecorrencia.fromString("SEMANAL")); }
    @Test public void fromString_nulo_fallbackMensal() { assertEquals(TipoRecorrencia.MENSAL, TipoRecorrencia.fromString(null)); }
    @Test public void fromString_invalido_fallbackMensal() { assertEquals(TipoRecorrencia.MENSAL, TipoRecorrencia.fromString("XPTO_INVALIDO")); }
    @Test public void fromString_vazio_fallbackMensal() { assertEquals(TipoRecorrencia.MENSAL, TipoRecorrencia.fromString("")); }

    // ── descricoes e unidades não nulas ───────────────────────────────────────

    @Test
    public void todosOsTipos_temDescricaoEUnidade() {
        for (TipoRecorrencia t : TipoRecorrencia.values()) {
            assertNotNull("Descrição nula: " + t, t.getDescricao());
            assertNotNull("Unidade nula: " + t, t.getUnidade());
            assertFalse(t.getDescricao().isEmpty());
            assertFalse(t.getUnidade().isEmpty());
        }
    }
}
