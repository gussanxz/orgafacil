package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums;

import org.junit.Test;
import static org.junit.Assert.*;

public class StatusSaudeFinanceiraTest {

    // ── IDs corretos ──────────────────────────────────────────────────────────

    @Test
    public void critico_temId1() {
        assertEquals(1, StatusSaudeFinanceira.CRITICO.getId());
    }

    @Test
    public void atencao_temId2() {
        assertEquals(2, StatusSaudeFinanceira.ATENCAO.getId());
    }

    @Test
    public void saudavel_temId3() {
        assertEquals(3, StatusSaudeFinanceira.SAUDAVEL.getId());
    }

    // ── desdeId() ─────────────────────────────────────────────────────────────

    @Test
    public void desdeId_1_retornaCritico() {
        assertEquals(StatusSaudeFinanceira.CRITICO, StatusSaudeFinanceira.desdeId(1));
    }

    @Test
    public void desdeId_2_retornaAtencao() {
        assertEquals(StatusSaudeFinanceira.ATENCAO, StatusSaudeFinanceira.desdeId(2));
    }

    @Test
    public void desdeId_3_retornaSaudavel() {
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, StatusSaudeFinanceira.desdeId(3));
    }

    @Test
    public void desdeId_invalido_retornaSaudavelComFallback() {
        // Firestore pode ter IDs inesperados — fallback não pode quebrar o app
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, StatusSaudeFinanceira.desdeId(0));
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, StatusSaudeFinanceira.desdeId(-1));
        assertEquals(StatusSaudeFinanceira.SAUDAVEL, StatusSaudeFinanceira.desdeId(999));
    }

    // ── descrições ────────────────────────────────────────────────────────────

    @Test
    public void descricoes_naoSaoNulas() {
        for (StatusSaudeFinanceira s : StatusSaudeFinanceira.values()) {
            assertNotNull(s.getDescricao());
            assertFalse(s.getDescricao().isEmpty());
        }
    }
}
