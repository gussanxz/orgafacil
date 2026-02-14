package com.gussanxz.orgafacil.funcionalidades.contas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // --- ENCAPSULAMENTO ---

    // LiveData exclusivo para a aba HISTÓRICO
    private final MutableLiveData<List<MovimentacaoModel>> _listaHistorico = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaHistorico = _listaHistorico;

    // LiveData exclusivo para a aba FUTURO
    private final MutableLiveData<List<MovimentacaoModel>> _listaFutura = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaFutura = _listaFutura;

    // Saldo REAL (Histórico - Apenas o que foi pago)
    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public LiveData<Long> saldoPeriodo = _saldoPeriodo;

    // [NOVO] Saldo ESTIMADO (Futuro - Soma das previsões)
    private final MutableLiveData<Long> _saldoFuturo = new MutableLiveData<>();
    public LiveData<Long> saldoFuturo = _saldoFuturo;

    // --- CACHE DE DADOS ---
    private List<MovimentacaoModel> cacheHistorico = new ArrayList<>();
    private List<MovimentacaoModel> cacheFuturo = new ArrayList<>();

    // Estado dos filtros atuais
    private String lastQuery = "";
    private Date lastInicio = null;
    private Date lastFim = null;

    // --- MÉTODOS DE DADOS ---

    public void fetchDados(MovimentacaoRepository repo, boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        Date agora = new Date();

        MovimentacaoRepository.DadosCallback internalCallback = new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                if (ehModoFuturo) {
                    cacheFuturo = new ArrayList<>(lista);
                } else {
                    cacheHistorico = new ArrayList<>(lista);
                }

                // Reaplica os filtros para atualizar a UI e CALCULAR OS SALDOS
                aplicarFiltros(lastQuery, lastInicio, lastFim);

                if (callbackExterno != null) callbackExterno.onSucesso(lista);
            }

            @Override
            public void onErro(String erro) {
                if (callbackExterno != null) callbackExterno.onErro(erro);
            }
        };

        if (ehModoFuturo) {
            repo.recuperarContasFuturas(agora, internalCallback);
        } else {
            repo.recuperarHistorico(agora, internalCallback);
        }
    }

    public void aplicarFiltros(String query, Date inicio, Date fim) {
        this.lastQuery = query;
        this.lastInicio = inicio;
        this.lastFim = fim;

        // --- Processa Lista de Histórico ---
        List<MovimentacaoModel> resHistorico = filtrarListaGenerica(cacheHistorico, query, inicio, fim, false);
        _listaHistorico.setValue(resHistorico);

        // --- Processa Lista Futura ---
        List<MovimentacaoModel> resFuturo = filtrarListaGenerica(cacheFuturo, query, inicio, fim, true);
        _listaFutura.setValue(resFuturo);

        // --- Calcula Saldos ---
        calcularSaldoHistorico(resHistorico);
        calcularSaldoFuturo(resFuturo); // [NOVO] Calcula o total da lista futura
    }

    private List<MovimentacaoModel> filtrarListaGenerica(List<MovimentacaoModel> origem, String query, Date inicio, Date fim, boolean isModoFuturo) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (MovimentacaoModel m : origem) {
            if (m == null) continue;

            // Filtro de Status
            if (isModoFuturo && m.isPago()) continue;

            // Filtro de Data
            boolean noPeriodo = true;
            if (inicio != null && fim != null) {
                if (m.getData_movimentacao() != null) {
                    Date dM = m.getData_movimentacao().toDate();
                    noPeriodo = !dM.before(inicio) && !dM.after(fim);
                } else {
                    noPeriodo = false;
                }
            }

            // Filtro de Texto
            if (noPeriodo) {
                String descricao = (m.getDescricao() != null) ? m.getDescricao().toLowerCase() : "";
                String categoria = (m.getCategoria_nome() != null) ? m.getCategoria_nome().toLowerCase() : "";

                if (q.isEmpty() || descricao.contains(q) || categoria.contains(q)) {
                    filtrados.add(m);
                }
            }
        }
        return filtrados;
    }

    /**
     * Calcula o saldo do HISTÓRICO (Apenas PAGOS).
     */
    private void calcularSaldoHistorico(List<MovimentacaoModel> listaParaSaldo) {
        long saldoCentavos = 0;
        for (MovimentacaoModel m : listaParaSaldo) {
            if (m.isPago()) {
                if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                    saldoCentavos += m.getValor();
                } else {
                    saldoCentavos -= m.getValor();
                }
            }
        }
        _saldoPeriodo.setValue(saldoCentavos);
    }

    /**
     * [NOVO] Calcula o saldo do FUTURO (Soma tudo o que está listado, pois é previsão).
     */
    private void calcularSaldoFuturo(List<MovimentacaoModel> listaParaSaldo) {
        long saldoCentavos = 0;
        for (MovimentacaoModel m : listaParaSaldo) {
            // Na lista futura, somamos tudo para dar a previsão de fluxo de caixa
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                saldoCentavos += m.getValor();
            } else {
                saldoCentavos -= m.getValor();
            }
        }
        _saldoFuturo.setValue(saldoCentavos);
    }
}