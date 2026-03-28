package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Testa TopGastoDTO — construtores e helpers de apresentação.
 */
@RunWith(RobolectricTestRunner.class)
public class TopGastoDTOTest {

    // ── construtor por categoria ──────────────────────────────────────────────

    @Test
    public void construtorCategoria_guardaValoresCorretamente() {
        TopGastoDTO dto = new TopGastoDTO("Alimentação", 30000, 35.5, true);

        assertEquals("Alimentação", dto.getNomeCategoria());
        assertEquals(30000, dto.getValorTotal());
        assertEquals(35.5, dto.getPercentual(), 0.01);
        assertTrue(dto.isDespesa());
        assertFalse(dto.isMovimentacaoIndividual());
        assertNull(dto.getMovimentacaoModel());
    }

    @Test
    public void construtorCategoria_receita_isDespesaFalse() {
        TopGastoDTO dto = new TopGastoDTO("Salário", 500000, 100.0, false);
        assertFalse(dto.isDespesa());
    }

    // ── construtor por movimentação individual ────────────────────────────────

    @Test
    public void construtorMovimentacao_guardaValoresCorretamente() {
        MovimentacaoModel mov = new MovimentacaoModel();
        mov.setDescricao("Netflix");
        mov.setCategoria_nome("Lazer");
        mov.setValor(4490); // R$44,90
        mov.setData_movimentacao(new Timestamp(new Date()));

        TopGastoDTO dto = new TopGastoDTO(mov, 5.5, true);

        assertEquals("Netflix", dto.getDescricao());
        assertEquals("Lazer", dto.getNomeCategoria());
        assertEquals(4490, dto.getValorTotal()); // abs do valor
        assertEquals(5.5, dto.getPercentual(), 0.01);
        assertTrue(dto.isDespesa());
        assertTrue(dto.isMovimentacaoIndividual());
        assertNotNull(dto.getMovimentacaoModel());
    }

    @Test
    public void construtorMovimentacao_valorAbsoluto_semprePositivo() {
        // Mesmo que o valor venha negativo, getValorTotal deve ser positivo
        MovimentacaoModel mov = new MovimentacaoModel();
        mov.setDescricao("Reembolso");
        mov.setCategoria_nome("Outros");
        mov.setValor(-15000); // valor negativo
        mov.setData_movimentacao(new Timestamp(new Date()));

        TopGastoDTO dto = new TopGastoDTO(mov, 10.0, false);
        assertEquals(15000, dto.getValorTotal()); // Math.abs()
    }

    @Test
    public void construtorMovimentacao_categoriaNomeNulo_usaStringVazia() {
        MovimentacaoModel mov = new MovimentacaoModel();
        mov.setDescricao("Compra");
        mov.setCategoria_nome(null);
        mov.setValor(1000);
        mov.setData_movimentacao(new Timestamp(new Date()));

        TopGastoDTO dto = new TopGastoDTO(mov, 1.0, true);
        assertEquals("", dto.getNomeCategoria()); // não deve lançar NPE
    }

    // ── isMovimentacaoIndividual() ────────────────────────────────────────────

    @Test
    public void construtorCategoria_naoEhMovimentacaoIndividual() {
        TopGastoDTO dto = new TopGastoDTO("Cat", 1000, 10.0, true);
        assertFalse(dto.isMovimentacaoIndividual());
    }
}
