package com.gussanxz.orgafacil.funcionalidades.contas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;

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

    // Saldo ESTIMADO (Futuro - Soma das previsões)
    private final MutableLiveData<Long> _saldoFuturo = new MutableLiveData<>();
    public LiveData<Long> saldoFuturo = _saldoFuturo;

    // Avisa a tela se está buscando mais itens (scroll infinito) para mostrar o ProgressBar
    private final MutableLiveData<Boolean> _carregandoPaginacao = new MutableLiveData<>(false);
    public LiveData<Boolean> carregandoPaginacao = _carregandoPaginacao;

    // --- CACHE DE DADOS ---
    private List<MovimentacaoModel> cacheHistorico = new ArrayList<>();
    private List<MovimentacaoModel> cacheFuturo = new ArrayList<>();

    // Estado dos filtros atuais
    private String lastQuery = "";
    private Date lastInicio = null;
    private Date lastFim = null;

    // --- VARIÁVEIS DE CONTROLE DE PAGINAÇÃO (SCROLL INFINITO) ---
    // [NOVO]: Controle de Histórico
    private DocumentSnapshot ultimoDocumentoVisivelHistorico = null;
    private boolean isUltimaPaginaHistorico = false;

    // [NOVO]: Controle de Futuro
    private DocumentSnapshot ultimoDocumentoVisivelFuturo = null;
    private boolean isUltimaPaginaFuturo = false;

    // Variável global
    private boolean isCarregandoPagina = false;

    // --- MÉTODOS DE DADOS ---

    /**
     * Busca os dados iniciais.
     * Agora ele direciona para a query correta do Repositório dependendo se é Futuro ou Histórico.
     */
    public void fetchDados(MovimentacaoRepository repo, boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        Date agora = new Date();

        if (ehModoFuturo) {
            // ==========================================
            // MODO FUTURO: Inicia a paginação do zero
            // ==========================================
            ultimoDocumentoVisivelFuturo = null;
            isUltimaPaginaFuturo = false;
            isCarregandoPagina = true;

            repo.recuperarContasFuturasPaginado(agora, null, new MovimentacaoRepository.DadosPaginadosCallback() {
                @Override
                public void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDoc) {
                    cacheFuturo = new ArrayList<>(lista);
                    ultimoDocumentoVisivelFuturo = ultimoDoc;

                    // Verifica se já chegou no fim
                    if (lista.size() < 100) isUltimaPaginaFuturo = true;

                    isCarregandoPagina = false;
                    aplicarFiltros(lastQuery, lastInicio, lastFim);
                    if (callbackExterno != null) callbackExterno.onSucesso(lista);
                }

                @Override
                public void onErro(String erro) {
                    isCarregandoPagina = false;
                    if (callbackExterno != null) callbackExterno.onErro(erro);
                }
            });

        } else {
            // ==========================================
            // MODO HISTÓRICO: Inicia a paginação do zero
            // ==========================================
            ultimoDocumentoVisivelHistorico = null;
            isUltimaPaginaHistorico = false;
            isCarregandoPagina = true;

            repo.recuperarHistoricoPaginado(agora, null, new MovimentacaoRepository.DadosPaginadosCallback() {
                @Override
                public void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDoc) {
                    cacheHistorico = new ArrayList<>(lista);
                    ultimoDocumentoVisivelHistorico = ultimoDoc;

                    if (lista.size() < 100) isUltimaPaginaHistorico = true;

                    isCarregandoPagina = false;
                    aplicarFiltros(lastQuery, lastInicio, lastFim);
                    if (callbackExterno != null) callbackExterno.onSucesso(lista);
                }

                @Override
                public void onErro(String erro) {
                    isCarregandoPagina = false;
                    if (callbackExterno != null) callbackExterno.onErro(erro);
                }
            });
        }
    }

    /**
     * CARREGAR MAIS HISTÓRICO
     */
    public void carregarMaisHistorico(MovimentacaoRepository repo) {
        if (isCarregandoPagina || isUltimaPaginaHistorico) return;

        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true); // AVISA A TELA
        Date agora = new Date();

        repo.recuperarHistoricoPaginado(agora, ultimoDocumentoVisivelHistorico, new MovimentacaoRepository.DadosPaginadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> novaLista, DocumentSnapshot novoUltimoDoc) {
                if (novaLista.isEmpty()) {
                    isUltimaPaginaHistorico = true;
                } else {
                    cacheHistorico.addAll(novaLista);
                    ultimoDocumentoVisivelHistorico = novoUltimoDoc;
                    if (novaLista.size() < 100) isUltimaPaginaHistorico = true;
                }

                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
                aplicarFiltros(lastQuery, lastInicio, lastFim);
            }

            @Override
            public void onErro(String erro) {
                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
            }
        });
    }

    /**
     * [NOVO] CARREGAR MAIS CONTAS FUTURAS
     */
    public void carregarMaisFuturo(MovimentacaoRepository repo) {
        if (isCarregandoPagina || isUltimaPaginaFuturo) return;

        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true);
        Date agora = new Date();

        repo.recuperarContasFuturasPaginado(agora, ultimoDocumentoVisivelFuturo, new MovimentacaoRepository.DadosPaginadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> novaLista, DocumentSnapshot novoUltimoDoc) {
                if (novaLista.isEmpty()) {
                    isUltimaPaginaFuturo = true;
                } else {
                    cacheFuturo.addAll(novaLista);
                    ultimoDocumentoVisivelFuturo = novoUltimoDoc;
                    if (novaLista.size() < 100) isUltimaPaginaFuturo = true;
                }

                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
                aplicarFiltros(lastQuery, lastInicio, lastFim);
            }

            @Override
            public void onErro(String erro) {
                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
            }
        });
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
        calcularSaldoFuturo(resFuturo);
    }

    private List<MovimentacaoModel> filtrarListaGenerica(List<MovimentacaoModel> origem, String query, Date inicio, Date fim, boolean isModoFuturo) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (MovimentacaoModel m : origem) {
            if (m == null) continue;

            // Filtro de Status
            if (isModoFuturo && m.isPago()) continue;
            if (!isModoFuturo && !m.isPago()) continue;

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

    private void calcularSaldoFuturo(List<MovimentacaoModel> listaParaSaldo) {
        long saldoCentavos = 0;
        for (MovimentacaoModel m : listaParaSaldo) {
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                saldoCentavos += m.getValor();
            } else {
                saldoCentavos -= m.getValor();
            }
        }
        _saldoFuturo.setValue(saldoCentavos);
    }
}