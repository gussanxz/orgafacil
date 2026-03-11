package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import android.os.Handler;
import android.os.Looper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Motor de Filtragem Assíncrona.
 * Isola a lógica de threads (ExecutorService) e as regras de negócio de pesquisa,
 * tirando esse peso do ViewModel e respeitando o Princípio da Responsabilidade Única.
 */
public class ContasFiltroEngine {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService filtroExecutor = Executors.newSingleThreadExecutor();

    private Future<?> ultimaTarefaFiltro = null;
    private volatile long filtroGeracaoAtual = 0;

    public interface FiltroCallback {
        void onResultado(List<MovimentacaoModel> historico, List<MovimentacaoModel> futuro, long saldoHist, long saldoFut);
    }

    public void filtrarAsync(List<MovimentacaoModel> cacheHistorico,
                             List<MovimentacaoModel> cacheFuturo,
                             String query, Date inicio, Date fim,
                             TipoCategoriaContas filtroTipo,
                             FiltroCallback callback) {

        final String queryFinal = (query != null) ? query : "";
        final long minhaGeracao = ++filtroGeracaoAtual;

        if (ultimaTarefaFiltro != null && !ultimaTarefaFiltro.isDone()) {
            ultimaTarefaFiltro.cancel(false);
        }

        ultimaTarefaFiltro = filtroExecutor.submit(() -> {
            if (minhaGeracao != filtroGeracaoAtual) return;

            List<MovimentacaoModel> resHistorico = filtrarListaGenerica(
                    cacheHistorico, queryFinal, inicio, fim, false, filtroTipo);

            if (minhaGeracao != filtroGeracaoAtual) return;

            List<MovimentacaoModel> resFuturo = filtrarListaGenerica(
                    cacheFuturo, queryFinal, inicio, fim, true, filtroTipo);

            if (minhaGeracao != filtroGeracaoAtual) return;

            long saldoHist = calcularSaldo(resHistorico, false);
            long saldoFut  = calcularSaldo(resFuturo, true);

            mainHandler.post(() -> {
                if (minhaGeracao != filtroGeracaoAtual) return;
                callback.onResultado(resHistorico, resFuturo, saldoHist, saldoFut);
            });
        });
    }

    private List<MovimentacaoModel> filtrarListaGenerica(List<MovimentacaoModel> origem,
                                                         String query, Date inicioOrig, Date fimOrig,
                                                         boolean isModoFuturo, TipoCategoriaContas filtroTipo) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = query.toLowerCase(java.util.Locale.getDefault()).trim();

        // ── MELHORIA DE UX: Inversão inteligente ──
        // Se o usuário colocar Fim ANTES do Início, o motor inverte automaticamente
        Date inicio = (inicioOrig != null && fimOrig != null && inicioOrig.after(fimOrig)) ? fimOrig  : inicioOrig;
        Date fim    = (inicioOrig != null && fimOrig != null && inicioOrig.after(fimOrig)) ? inicioOrig : fimOrig;

        // Pré-calcula os limites uma única vez fora do loop (evita alocação de objeto por item)
        final Date inicioTruncado = (inicio != null) ? DateHelper.truncarParaInicioDia(inicio) : null;
        final Date fimMaximizado  = (fim    != null) ? DateHelper.maximizarParaFimDia(fim)      : null;

        for (MovimentacaoModel m : origem) {
            if (m == null) continue;
            if (isModoFuturo  &&  m.isPago()) continue;
            if (!isModoFuturo && !m.isPago()) continue;
            if (filtroTipo != null && m.getTipoEnum() != filtroTipo) continue;

            // Filtro de data seguro
            if (m.getData_movimentacao() != null) {
                Date dM = m.getData_movimentacao().toDate();

                if (inicioTruncado != null) {
                    // Trunca a movimentação para 00:00:00 e compara: inclui o dia inteiro do início
                    if (DateHelper.truncarParaInicioDia(dM).before(inicioTruncado)) continue;
                }
                if (fimMaximizado != null) {
                    // Usa o fim pré-calculado com 23:59:59.999: garante que o dia inteiro do fim seja incluído
                    if (dM.after(fimMaximizado)) continue;
                }
            }

            if (!q.isEmpty()) {
                String descricao = m.getDescricao() != null ? m.getDescricao().toLowerCase(java.util.Locale.getDefault()) : "";
                String categoria = m.getCategoria_nome() != null ? m.getCategoria_nome().toLowerCase(java.util.Locale.getDefault()) : "";
                if (!descricao.contains(q) && !categoria.contains(q)) continue;
            }

            filtrados.add(m);
        }
        return filtrados;
    }

    private long calcularSaldo(List<MovimentacaoModel> lista, boolean isModoFuturo) {
        long saldo = 0L;
        for (MovimentacaoModel m : lista) {
            if (!isModoFuturo && !m.isPago()) continue;
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) saldo += m.getValor();
            else saldo -= m.getValor();
        }
        return saldo;
    }

    public void encerrar() {
        filtroExecutor.shutdownNow();
    }
}