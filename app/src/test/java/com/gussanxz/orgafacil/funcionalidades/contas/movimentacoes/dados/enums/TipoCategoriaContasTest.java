package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums;

import org.junit.Test;
import static org.junit.Assert.*;

public class TipoCategoriaContasTest {

    @Test
    public void receita_temId1() {
        assertEquals(1, TipoCategoriaContas.RECEITA.getId());
    }

    @Test
    public void despesa_temId2() {
        assertEquals(2, TipoCategoriaContas.DESPESA.getId());
    }

    @Test
    public void desdeId_1_retornaReceita() {
        assertEquals(TipoCategoriaContas.RECEITA, TipoCategoriaContas.desdeId(1));
    }

    @Test
    public void desdeId_2_retornaDespesa() {
        assertEquals(TipoCategoriaContas.DESPESA, TipoCategoriaContas.desdeId(2));
    }

    @Test
    public void desdeId_invalido_retornaDespesaComFallback() {
        assertEquals(TipoCategoriaContas.DESPESA, TipoCategoriaContas.desdeId(999));
        assertEquals(TipoCategoriaContas.DESPESA, TipoCategoriaContas.desdeId(0));
        assertEquals(TipoCategoriaContas.DESPESA, TipoCategoriaContas.desdeId(-1));
    }

    @Test
    public void descricoes_naoSaoNulas() {
        for (TipoCategoriaContas t : TipoCategoriaContas.values()) {
            assertNotNull(t.getDescricao());
            assertFalse(t.getDescricao().isEmpty());
        }
    }
}
