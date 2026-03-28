package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class ContasFiltroEngineTest {

    private ContasFiltroEngine engine;

    @Before
    public void setUp() {
        engine = new ContasFiltroEngine();
    }

    @After
    public void tearDown() {
        engine.encerrar();
    }

    // ── fábrica ───────────────────────────────────────────────────────────────

    private MovimentacaoModel mov(String desc, String catId, String catNome,
                                  long valor, boolean pago,
                                  TipoCategoriaContas tipo, int diasOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, diasOffset);
        MovimentacaoModel m = new MovimentacaoModel();
        m.setDescricao(desc);
        m.setCategoria_id(catId);
        m.setCategoria_nome(catNome);
        m.setValor(valor);
        m.setPago(pago);
        m.setTipoEnum(tipo);
        m.setData_movimentacao(new Timestamp(cal.getTime()));
        return m;
    }

    private MovimentacaoModel despesaPaga(String desc, String catId, String catNome, long valor) {
        return mov(desc, catId, catNome, valor, true, TipoCategoriaContas.DESPESA, -1);
    }

    private MovimentacaoModel despesaFutura(String desc, String catId, String catNome, long valor) {
        return mov(desc, catId, catNome, valor, false, TipoCategoriaContas.DESPESA, 5);
    }

    private MovimentacaoModel receitaPaga(String desc, long valor) {
        return mov(desc, "r1", "Receitas", valor, true, TipoCategoriaContas.RECEITA, -1);
    }

    /**
     * Executa o filtro e aguarda o callback de forma confiável.
     *
     * Problema original: ShadowLooper.runUiThreadTasksIncludingDelayedTasks() drenava
     * o MainLooper MAS o executor background (SingleThreadExecutor da ContasFiltroEngine)
     * ainda não tinha terminado de calcular. O callback chegava no MainLooper DEPOIS
     * do drain, então o AtomicReference ficava null.
     *
     * Solução: CountDownLatch no callback + loop que drena o Looper enquanto aguarda.
     * O loop é necessário porque o MainLooper precisa ser drenado para o Runnable
     * postado pelo executor ser executado — sem isso o latch nunca conta down.
     */
    private <T> T filtrar(
            List<MovimentacaoModel> hist,
            List<MovimentacaoModel> fut,
            String query,
            TipoCategoriaContas tipo,
            String catId,
            long valorMin, long valorMax,
            java.util.function.Function<ContasFiltroEngine.FiltroCallback, Void> invoker) {

        AtomicReference<T> resultado = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Chamada real — o invoker define qual overload usar (com ou sem valor)
        invoker.apply((h, f, sh, sf) -> {
            // @SuppressWarnings necessário pois T é genérico; o cast é seguro
            // pois cada teste passa o lambda correto
            resultado.set((T) new FiltroResultado(h, f, sh, sf));
            latch.countDown();
        });

        // Drena o Looper em loop até o latch contar down ou timeout (3s)
        long deadline = System.currentTimeMillis() + 3_000;
        while (latch.getCount() > 0 && System.currentTimeMillis() < deadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            if (latch.getCount() > 0) {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }

        assertNotNull("Callback não foi chamado — timeout ou executor falhou", resultado.get());
        return resultado.get();
    }

    /** Container simples para o resultado do callback. */
    static class FiltroResultado {
        final List<MovimentacaoModel> historico;
        final List<MovimentacaoModel> futuro;
        final long saldoHist;
        final long saldoFut;

        FiltroResultado(List<MovimentacaoModel> h, List<MovimentacaoModel> f, long sh, long sf) {
            this.historico = h; this.futuro = f; this.saldoHist = sh; this.saldoFut = sf;
        }
    }

    private FiltroResultado executar(List<MovimentacaoModel> hist, List<MovimentacaoModel> fut,
                                     String query, TipoCategoriaContas tipo, String catId) {
        return filtrar(hist, fut, query, tipo, catId, -1, -1, cb -> {
            engine.filtrarAsync(hist, fut, query, null, null, tipo, catId, cb);
            return null;
        });
    }

    private FiltroResultado executarComValor(List<MovimentacaoModel> hist, List<MovimentacaoModel> fut,
                                             long min, long max) {
        return filtrar(hist, fut, null, null, null, min, max, cb -> {
            engine.filtrarAsync(hist, fut, null, null, null, null, null, min, max, cb);
            return null;
        });
    }

    // ── sem filtros ───────────────────────────────────────────────────────────

    @Test
    public void semFiltros_retornaTodosHistoricoEFuturo() {
        MovimentacaoModel h = despesaPaga("Mercado", "c1", "Alimentação", 5000);
        MovimentacaoModel f = despesaFutura("Aluguel", "c2", "Moradia", 100000);

        FiltroResultado r = executar(
                Collections.singletonList(h),
                Collections.singletonList(f),
                null, null, null);

        assertEquals(1, r.historico.size());
        assertEquals(1, r.futuro.size());
    }

    @Test
    public void listaVazia_retornaVazioSemCrash() {
        FiltroResultado r = executar(
                Collections.emptyList(), Collections.emptyList(),
                null, null, null);
        assertTrue(r.historico.isEmpty());
        assertTrue(r.futuro.isEmpty());
    }

    // ── filtro por texto ──────────────────────────────────────────────────────

    @Test
    public void filtroPorDescricao_encontraCorrespondencia() {
        MovimentacaoModel m1 = despesaPaga("Mercado Extra", "c1", "Alimentação", 5000);
        MovimentacaoModel m2 = despesaPaga("Aluguel", "c2", "Moradia", 100000);

        FiltroResultado r = executar(
                Arrays.asList(m1, m2), Collections.emptyList(),
                "mercado", null, null);

        assertEquals(1, r.historico.size());
        assertEquals("Mercado Extra", r.historico.get(0).getDescricao());
    }

    @Test
    public void filtroPorDescricao_caseInsensitive() {
        MovimentacaoModel m = despesaPaga("NETFLIX", "c1", "Lazer", 4490);

        FiltroResultado r = executar(
                Collections.singletonList(m), Collections.emptyList(),
                "netflix", null, null);

        assertEquals(1, r.historico.size());
    }

    @Test
    public void filtroPorCategoriaNome_encontraCorrespondencia() {
        MovimentacaoModel m = despesaPaga("Supermercado", "c1", "Alimentação", 8000);

        FiltroResultado r = executar(
                Collections.singletonList(m), Collections.emptyList(),
                "aliment", null, null);

        assertEquals(1, r.historico.size());
    }

    @Test
    public void filtroPorValorNumerico_encontra() {
        // 15000 centavos = R$150,00 — busca por "150"
        MovimentacaoModel m = despesaPaga("Academia", "c1", "Saúde", 15000);

        FiltroResultado r = executar(
                Collections.singletonList(m), Collections.emptyList(),
                "150", null, null);

        assertEquals(1, r.historico.size());
    }

    @Test
    public void filtroSemCorrespondencia_retornaVazio() {
        MovimentacaoModel m = despesaPaga("Mercado", "c1", "Alimentação", 5000);

        FiltroResultado r = executar(
                Collections.singletonList(m), Collections.emptyList(),
                "xyzxyz_inexistente", null, null);

        assertTrue(r.historico.isEmpty());
    }

    // ── filtro por tipo ───────────────────────────────────────────────────────

    @Test
    public void filtroDespesa_ocultaReceitas() {
        MovimentacaoModel desp = despesaPaga("Mercado", "c1", "Alimentação", 5000);
        MovimentacaoModel rec  = receitaPaga("Salário", 300000);

        FiltroResultado r = executar(
                Arrays.asList(desp, rec), Collections.emptyList(),
                null, TipoCategoriaContas.DESPESA, null);

        assertEquals(1, r.historico.size());
        assertEquals(TipoCategoriaContas.DESPESA, r.historico.get(0).getTipoEnum());
    }

    @Test
    public void filtroReceita_ocultaDespesas() {
        MovimentacaoModel desp = despesaPaga("Mercado", "c1", "Alimentação", 5000);
        MovimentacaoModel rec  = receitaPaga("Salário", 300000);

        FiltroResultado r = executar(
                Arrays.asList(desp, rec), Collections.emptyList(),
                null, TipoCategoriaContas.RECEITA, null);

        assertEquals(1, r.historico.size());
        assertEquals(TipoCategoriaContas.RECEITA, r.historico.get(0).getTipoEnum());
    }

    // ── filtro por categoria ──────────────────────────────────────────────────

    @Test
    public void filtroCategoriaId_retornaSoAquelaCategoria() {
        MovimentacaoModel m1 = despesaPaga("Mercado",  "cat-alim",  "Alimentação", 5000);
        MovimentacaoModel m2 = despesaPaga("Farmácia", "cat-saude", "Saúde",       3000);

        FiltroResultado r = executar(
                Arrays.asList(m1, m2), Collections.emptyList(),
                null, null, "cat-alim");

        assertEquals(1, r.historico.size());
        assertEquals("cat-alim", r.historico.get(0).getCategoria_id());
    }

    // ── filtro por intervalo de valor ─────────────────────────────────────────

    @Test
    public void filtroIntervaloValor_retornaApenasItemDentroDoRange() {
        MovimentacaoModel barato = despesaPaga("Café",    "c1", "Alimentação",   500);
        MovimentacaoModel medio  = despesaPaga("Mercado", "c1", "Alimentação",  5000);
        MovimentacaoModel caro   = despesaPaga("Aluguel", "c2", "Moradia",    100000);

        FiltroResultado r = executarComValor(
                Arrays.asList(barato, medio, caro),
                Collections.emptyList(),
                1000, 50000); // R$10 a R$500

        assertEquals(1, r.historico.size());
        assertEquals("Mercado", r.historico.get(0).getDescricao());
    }

    @Test
    public void filtroValorSemLimiteMax_retornaTodosAcimaDoMinimo() {
        MovimentacaoModel barato = despesaPaga("Café", "c1", "Alimentação",   500);
        MovimentacaoModel caro   = despesaPaga("TV",   "c1", "Eletrônicos", 200000);

        FiltroResultado r = executarComValor(
                Arrays.asList(barato, caro),
                Collections.emptyList(),
                1000, -1); // mínimo R$10, sem máximo

        assertEquals(1, r.historico.size());
        assertEquals("TV", r.historico.get(0).getDescricao());
    }

    // ── saldo calculado ───────────────────────────────────────────────────────

    @Test
    public void saldoHistorico_receitaMinusDespesa() {
        MovimentacaoModel rec  = receitaPaga("Salário", 300000);  // +R$3000
        MovimentacaoModel desp = despesaPaga("Mercado", "c1", "Alimentação", 50000); // -R$500

        FiltroResultado r = executar(
                Arrays.asList(rec, desp), Collections.emptyList(),
                null, null, null);

        assertEquals(250000L, r.saldoHist); // 3000 - 500 = 2500 = 250000 centavos
    }

    @Test
    public void saldoZero_listaVazia() {
        FiltroResultado r = executar(
                Collections.emptyList(), Collections.emptyList(),
                null, null, null);
        assertEquals(0L, r.saldoHist);
    }

    // ── separação histórico x futuro ──────────────────────────────────────────

    @Test
    public void itemNaoPago_naoAparecenNoHistorico() {
        MovimentacaoModel futuro = despesaFutura("Aluguel", "c2", "Moradia", 100000);

        FiltroResultado r = executar(
                Collections.singletonList(futuro),
                Collections.singletonList(futuro),
                null, null, null);

        assertTrue(r.historico.isEmpty());
        assertEquals(1, r.futuro.size());
    }

    @Test
    public void itemPago_naoAparecenNoFuturo() {
        MovimentacaoModel pago = despesaPaga("Mercado", "c1", "Alimentação", 5000);

        FiltroResultado r = executar(
                Collections.singletonList(pago),
                Collections.singletonList(pago),
                null, null, null);

        assertTrue(r.futuro.isEmpty());
    }
}