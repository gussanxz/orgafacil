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

    // --- DEPENDÊNCIAS INTERNAS ---
    private final MovimentacaoRepository repo;

    private final MutableLiveData<List<MovimentacaoModel>> _listaHistorico = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaHistorico = _listaHistorico;

    private final MutableLiveData<List<MovimentacaoModel>> _listaFutura = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaFutura = _listaFutura;

    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public LiveData<Long> saldoPeriodo = _saldoPeriodo;

    private final MutableLiveData<Long> _saldoFuturo = new MutableLiveData<>();
    public LiveData<Long> saldoFuturo = _saldoFuturo;

    private final MutableLiveData<Boolean> _carregandoPaginacao = new MutableLiveData<>(false);
    public LiveData<Boolean> carregandoPaginacao = _carregandoPaginacao;

    // Saldo calculado dinamicamente a partir da lista visível — NÃO vem do Firestore
    // Atualizado sempre que aplicarFiltros() é chamado, inclusive ao trocar de aba ou filtrar por tipo
    private final MutableLiveData<Long> _saldoListaAtual = new MutableLiveData<>(0L);
    public LiveData<Long> saldoListaAtual = _saldoListaAtual;

    private List<MovimentacaoModel> cacheHistorico = new ArrayList<>();
    private List<MovimentacaoModel> cacheFuturo = new ArrayList<>();

    private String lastQuery = "";
    private Date lastInicio = null;
    private Date lastFim = null;

    private DocumentSnapshot ultimoDocumentoVisivelHistorico = null;
    private boolean isUltimaPaginaHistorico = false;

    private DocumentSnapshot ultimoDocumentoVisivelFuturo = null;
    private boolean isUltimaPaginaFuturo = false;

    private boolean isCarregandoPagina = false;
    private TipoCategoriaContas lastFiltroTipo = null;

    public ContasViewModel() {
        this.repo = new MovimentacaoRepository();
    }

    // --- WRAPPERS DE AÇÃO PARA A VIEW ---
    public void excluir(MovimentacaoModel mov, MovimentacaoRepository.Callback callback) {
        repo.excluir(mov, callback);
    }

    public void confirmarMovimentacao(MovimentacaoModel mov, MovimentacaoRepository.Callback callback) {
        repo.confirmarMovimentacao(mov, callback);
    }

    public void zerarEstatisticasMensais(MovimentacaoRepository.Callback callback) {
        repo.zerarEstatisticasMensais(callback);
    }

    public void confirmarMovimentacaoEmMassa(MovimentacaoModel mov, MovimentacaoRepository.Callback callback) {
        repo.confirmarMovimentacaoEmMassa(mov, callback);
    }

    public void excluirEmLote(List<MovimentacaoModel> lista, MovimentacaoRepository.Callback callback) {
        repo.excluirEmLote(lista, callback);
    }

    // --- MÉTODOS DE BUSCA ---

    public void fetchDados(boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        Date agora = new Date();
        _carregandoPaginacao.setValue(true);

        if (ehModoFuturo) {
            ultimoDocumentoVisivelFuturo = null;
            isUltimaPaginaFuturo = false;
            isCarregandoPagina = true;

            repo.recuperarContasFuturasPaginado(agora, null, new MovimentacaoRepository.DadosPaginadosCallback() {
                @Override
                public void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDoc) {
                    cacheFuturo = new ArrayList<>(lista);
                    ultimoDocumentoVisivelFuturo = ultimoDoc;
                    if (lista.size() < 100) isUltimaPaginaFuturo = true;
                    isCarregandoPagina = false;
                    _carregandoPaginacao.setValue(false);
                    aplicarFiltros(lastQuery, lastInicio, lastFim);
                    if (callbackExterno != null) callbackExterno.onSucesso(lista);
                }

                @Override
                public void onErro(String erro) {
                    isCarregandoPagina = false;
                    _carregandoPaginacao.setValue(false);
                    if (callbackExterno != null) callbackExterno.onErro(erro);
                }
            });

        } else {
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
                    _carregandoPaginacao.setValue(false);
                    aplicarFiltros(lastQuery, lastInicio, lastFim);
                    if (callbackExterno != null) callbackExterno.onSucesso(lista);
                }

                @Override
                public void onErro(String erro) {
                    isCarregandoPagina = false;
                    _carregandoPaginacao.setValue(false);
                    if (callbackExterno != null) callbackExterno.onErro(erro);
                }
            });
        }
    }

    public void carregarMaisHistorico() {
        if (isCarregandoPagina || isUltimaPaginaHistorico) return;
        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true);
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

    public void carregarMaisFuturo() {
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

        List<MovimentacaoModel> resHistorico = filtrarListaGenerica(cacheHistorico, query, inicio, fim, false);
        _listaHistorico.setValue(resHistorico);

        List<MovimentacaoModel> resFuturo = filtrarListaGenerica(cacheFuturo, query, inicio, fim, true);
        _listaFutura.setValue(resFuturo);

        calcularSaldoHistorico(resHistorico);
        calcularSaldoFuturo(resFuturo);

        // [CORREÇÃO 1.5] Atualiza saldoListaAtual com base na aba atual
        // A aba 0 = pendentes (futuro), aba 1 = histórico — mas como o VM não sabe
        // qual aba está ativa, publica ambos e a Activity escolhe via abaAtiva
        calcularSaldoDaLista(resHistorico, resFuturo);
    }

    private List<MovimentacaoModel> filtrarListaGenerica(List<MovimentacaoModel> origem, String query, Date inicio, Date fim, boolean isModoFuturo) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (MovimentacaoModel m : origem) {
            if (m == null) continue;

            if (isModoFuturo && m.isPago()) continue;
            if (!isModoFuturo && !m.isPago()) continue;
            if (lastFiltroTipo != null && m.getTipoEnum() != lastFiltroTipo) continue;

            boolean noPeriodo = true;
            if (inicio != null && fim != null) {
                if (m.getData_movimentacao() != null) {
                    Date dM = m.getData_movimentacao().toDate();
                    noPeriodo = !dM.before(inicio) && !dM.after(fim);
                } else {
                    noPeriodo = false;
                }
            }

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

    /**
     * [CORREÇÃO 1.5] Calcula o saldoListaAtual armazenando ambos internamente.
     * A Activity notifica qual aba está ativa via notificarAbaAtiva() para
     * publicar o saldo correto no LiveData.
     */
    private long ultimoSaldoHistoricoCalculado = 0L;
    private long ultimoSaldoFuturoCalculado = 0L;
    private boolean abaAtualEhFuturo = false; // false = histórico (aba 1), true = futuro/pendentes (aba 0)

    private void calcularSaldoDaLista(List<MovimentacaoModel> historico, List<MovimentacaoModel> futuro) {
        // Recalcula ambos os lados
        long saldoHist = 0L;
        for (MovimentacaoModel m : historico) {
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) saldoHist += m.getValor();
            else saldoHist -= m.getValor();
        }
        ultimoSaldoHistoricoCalculado = saldoHist;

        long saldoFut = 0L;
        for (MovimentacaoModel m : futuro) {
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) saldoFut += m.getValor();
            else saldoFut -= m.getValor();
        }
        ultimoSaldoFuturoCalculado = saldoFut;

        // Publica o saldo da aba que está ativa no momento
        _saldoListaAtual.setValue(abaAtualEhFuturo ? ultimoSaldoFuturoCalculado : ultimoSaldoHistoricoCalculado);
    }

    /**
     * [CORREÇÃO 1.5] Chamado pela ResumoContasActivity ao trocar de aba no ViewPager.
     * Atualiza qual saldo deve ser exibido sem precisar refazer o fetch.
     *
     * @param ehFuturo true = aba "Contas Pendentes" (posição 0), false = aba "Últimas Movimentações" (posição 1)
     */
    public void notificarAbaAtiva(boolean ehFuturo) {
        abaAtualEhFuturo = ehFuturo;
        _saldoListaAtual.setValue(ehFuturo ? ultimoSaldoFuturoCalculado : ultimoSaldoHistoricoCalculado);
    }

    public void setFiltroTipo(TipoCategoriaContas tipo) {
        this.lastFiltroTipo = tipo;
        aplicarFiltros(lastQuery, lastInicio, lastFim);
    }
}