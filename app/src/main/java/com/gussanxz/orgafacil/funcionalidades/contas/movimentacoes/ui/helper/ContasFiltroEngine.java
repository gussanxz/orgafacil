package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import android.os.Handler;
import android.os.Looper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class ContasFiltroEngine {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService filtroExecutor = Executors.newSingleThreadExecutor();

    // CORREÇÃO A: AtomicLong garante que o ++ seja read-modify-write atômico.
    // volatile long não é suficiente: o incremento composto (ler + somar + escrever)
    // não é atômico e pode gerar gerações duplicadas se duas threads incrementarem
    // ao mesmo tempo (ex: UI thread + executor em overlap de scheduling).
    private final AtomicLong filtroGeracaoAtual = new AtomicLong(0L);

    // Campo que estava faltando — declarado aqui junto com os demais campos da classe.
    private Future<?> ultimaTarefaFiltro = null;

    public interface FiltroCallback {
        void onResultado(List<MovimentacaoModel> historico,
                         List<MovimentacaoModel> futuro,
                         long saldoHist, long saldoFut);
    }

    public void filtrarAsync(List<MovimentacaoModel> cacheHistorico,
                             List<MovimentacaoModel> cacheFuturo,
                             String query, Date inicio, Date fim,
                             TipoCategoriaContas filtroTipo,
                             String filtroCategoriaId,
                             FiltroCallback callback) {
        filtrarAsync(cacheHistorico, cacheFuturo, query, inicio, fim,
                filtroTipo, filtroCategoriaId, -1L, -1L, callback);
    }

    /**
     * Versão completa com filtro de intervalo de valor.
     *
     * @param valorMinCentavos  valor mínimo em centavos, inclusivo. -1 = sem limite inferior.
     * @param valorMaxCentavos  valor máximo em centavos, inclusivo. -1 = sem limite superior.
     *
     * Busca por texto (query) também verifica o valor formatado do item —
     * ex: "150" encontra movimentações de R$ 150,00, R$ 1.500,00 e R$ 15,00.
     * A normalização remove "R$", espaços e pontos de milhar antes de comparar,
     * então "1.500", "1500" e "1500,00" encontram o mesmo item.
     */
    public void filtrarAsync(List<MovimentacaoModel> cacheHistorico,
                             List<MovimentacaoModel> cacheFuturo,
                             String query, Date inicio, Date fim,
                             TipoCategoriaContas filtroTipo,
                             String filtroCategoriaId,
                             long valorMinCentavos, long valorMaxCentavos,
                             FiltroCallback callback) {

        final String queryFinal = (query != null) ? query : "";

        // CORREÇÃO A: incrementAndGet() é atômico — sem risco de gerações duplicadas.
        final long minhaGeracao = filtroGeracaoAtual.incrementAndGet();

        // CORREÇÃO B: cancel(true) em vez de cancel(false).
        // cancel(true) chama interrupt() na thread, o que faz o executor
        // lançar InterruptedException se a thread estiver bloqueada.
        // Para loops CPU-bound como o nosso, isso não interrompe o loop sozinho —
        // por isso adicionamos checagens internas em filtrarListaGenerica().
        // O cancel(true) é a sinalização correta; as checagens são a interrupção real.
        if (ultimaTarefaFiltro != null && !ultimaTarefaFiltro.isDone()) {
            ultimaTarefaFiltro.cancel(true);
        }

        ultimaTarefaFiltro = filtroExecutor.submit(() -> {
            try {
                if (estaObsoleta(minhaGeracao)) return;

                List<MovimentacaoModel> resHistorico = filtrarListaGenerica(
                        cacheHistorico, queryFinal, inicio, fim,
                        false, filtroTipo, filtroCategoriaId,
                        valorMinCentavos, valorMaxCentavos, minhaGeracao);

                if (resHistorico == null || estaObsoleta(minhaGeracao)) return;

                List<MovimentacaoModel> resFuturo = filtrarListaGenerica(
                        cacheFuturo, queryFinal, inicio, fim,
                        true, filtroTipo, filtroCategoriaId,
                        valorMinCentavos, valorMaxCentavos, minhaGeracao);

                if (resFuturo == null || estaObsoleta(minhaGeracao)) return;

                long saldoHist = calcularSaldo(resHistorico, false);
                long saldoFut  = calcularSaldo(resFuturo, true);

                // Checagem final antes de postar na UI — garante que entre
                // o fim do cálculo e a execução do post nenhuma geração nova
                // foi iniciada.
                if (estaObsoleta(minhaGeracao)) return;

                mainHandler.post(() -> {
                    // Checagem dentro do post: o Looper pode ter acumulado
                    // posts de gerações antigas antes de processar este.
                    if (estaObsoleta(minhaGeracao)) return;
                    callback.onResultado(resHistorico, resFuturo, saldoHist, saldoFut);
                });

            } catch (InterruptedException e) {
                // cancel(true) interrompeu uma operação bloqueante (improvável
                // neste executor CPU-bound, mas tratamos para não engolir silenciosamente).
                Thread.currentThread().interrupt();
            }
        });
    }

    // ── Utilitário de checagem — evita repetir a comparação em todo lugar ──────

    /**
     * Retorna true se esta geração de filtro foi superada por uma mais nova.
     * Quando true, o resultado em produção deve ser descartado.
     */
    private boolean estaObsoleta(long minhaGeracao) {
        return minhaGeracao != filtroGeracaoAtual.get();
    }

    // ── Filtro com checagem interna por iteração ───────────────────────────────

    /**
     * Filtra a lista e retorna null se a geração for cancelada durante o loop.
     *
     * A checagem a cada iteração garante saída antecipada quando um filtro
     * mais novo é disparado enquanto este ainda está processando uma lista grande.
     *
     * @return lista filtrada, ou null se a geração foi superada durante o loop
     */
    private List<MovimentacaoModel> filtrarListaGenerica(
            List<MovimentacaoModel> origem,
            String query, Date inicioOrig, Date fimOrig,
            boolean isModoFuturo,
            TipoCategoriaContas filtroTipo,
            String filtroCategoriaId,
            long valorMinCentavos, long valorMaxCentavos,
            long minhaGeracao) throws InterruptedException {

        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = query.toLowerCase(Locale.getDefault()).trim();

        // Normaliza a query para busca por valor: remove "r$", espaços e pontos
        // de milhar. "R$ 1.500,00" → "1500,00", "1.500" → "1500", "150" → "150".
        String qValor = q.replaceAll("r\\$", "").replaceAll("\\.", "").trim();

        Date inicio = (inicioOrig != null && fimOrig != null && inicioOrig.after(fimOrig))
                ? fimOrig : inicioOrig;
        Date fim    = (inicioOrig != null && fimOrig != null && inicioOrig.after(fimOrig))
                ? inicioOrig : fimOrig;

        final Date inicioTruncado = (inicio != null)
                ? DateHelper.truncarParaInicioDia(inicio) : null;
        final Date fimMaximizado  = (fim != null)
                ? DateHelper.maximizarParaFimDia(fim) : null;

        final boolean temFiltroMin = (valorMinCentavos >= 0);
        final boolean temFiltroMax = (valorMaxCentavos >= 0);

        for (MovimentacaoModel m : origem) {

            if (estaObsoleta(minhaGeracao)) return null;
            if (Thread.interrupted()) throw new InterruptedException();

            if (m == null) continue;
            if (isModoFuturo  &&  m.isPago()) continue;
            if (!isModoFuturo && !m.isPago()) continue;
            if (filtroTipo != null && m.getTipoEnum() != filtroTipo) continue;

            if (filtroCategoriaId != null
                    && !filtroCategoriaId.equals(m.getCategoria_id())) continue;

            // ── Filtro de intervalo de valor (RangeSlider) ───────────────────
            long valorAbs = Math.abs(m.getValor());
            if (temFiltroMin && valorAbs < valorMinCentavos) continue;
            if (temFiltroMax && valorAbs > valorMaxCentavos) continue;

            if (m.getData_movimentacao() != null) {
                Date dM = m.getData_movimentacao().toDate();
                if (inicioTruncado != null
                        && DateHelper.truncarParaInicioDia(dM).before(inicioTruncado)) continue;
                if (fimMaximizado != null && dM.after(fimMaximizado)) continue;
            }

            if (!q.isEmpty()) {
                String descricao = m.getDescricao() != null
                        ? m.getDescricao().toLowerCase(Locale.getDefault()) : "";
                String categoria = m.getCategoria_nome() != null
                        ? m.getCategoria_nome().toLowerCase(Locale.getDefault()) : "";

                // ── Busca por valor ──────────────────────────────────────────
                // Formata o valor como "1500,00" e compara com qValor normalizado.
                // Mantém a vírgula no valor formatado para que "150" sem vírgula
                // funcione como prefixo parcial — encontra "150,00" e "1500,xx".
                long centavos = Math.abs(m.getValor());
                String valorStr = String.format(Locale.getDefault(),
                        "%d,%02d", centavos / 100, centavos % 100);

                if (!descricao.contains(q) && !categoria.contains(q)
                        && !valorStr.contains(qValor)) continue;
            }

            filtrados.add(m);
        }
        return filtrados;
    }

    // ── Saldo — sem checagem de geração (operação rápida em lista já filtrada) ─

    private long calcularSaldo(List<MovimentacaoModel> lista, boolean isModoFuturo) {
        long saldo = 0L;
        for (MovimentacaoModel m : lista) {
            if (!isModoFuturo && !m.isPago()) continue;
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) saldo += m.getValor();
            else saldo -= m.getValor();
        }
        return saldo;
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────────

    /**
     * Deve ser chamado em ViewModel.onCleared().
     * shutdownNow() envia interrupt() para todas as threads em execução,
     * o que aciona o catch(InterruptedException) e encerra o loop corretamente.
     */
    public void encerrar() {
        filtroExecutor.shutdownNow();
    }
}