package com.gussanxz.orgafacil.funcionalidades.contas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.repository.ContasCategoriaRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasFiltroEngine;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // ── Dependências ───────────────────────────────────────────────────────────
    private final MovimentacaoRepository repo;
    private final ContasCategoriaRepository categoriaRepo; // NOVO
    private final ContasFiltroEngine filtroEngine;

    // ── LiveData públicos ──────────────────────────────────────────────────────
    private final MutableLiveData<List<MovimentacaoModel>> _listaHistorico = new MutableLiveData<>();
    public final LiveData<List<MovimentacaoModel>> listaHistorico = _listaHistorico;

    private final MutableLiveData<List<MovimentacaoModel>> _listaFutura = new MutableLiveData<>();
    public final LiveData<List<MovimentacaoModel>> listaFutura = _listaFutura;

    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public final LiveData<Long> saldoPeriodo = _saldoPeriodo;

    private final MutableLiveData<Long> _saldoFuturo = new MutableLiveData<>();
    public final LiveData<Long> saldoFuturo = _saldoFuturo;

    private final MutableLiveData<Boolean> _carregandoPaginacao = new MutableLiveData<>(false);
    public final LiveData<Boolean> carregandoPaginacao = _carregandoPaginacao;

    private final MutableLiveData<Long> _saldoListaAtual = new MutableLiveData<>(0L);
    public final LiveData<Long> saldoListaAtual = _saldoListaAtual;

    // ── Estado interno ─────────────────────────────────────────────────────────
    private boolean dadosInvalidados = true;

    private List<MovimentacaoModel> cacheHistorico = new ArrayList<>();
    private List<MovimentacaoModel> cacheFuturo    = new ArrayList<>();

    private String             lastQuery      = "";
    private Date               lastInicio     = null;
    private Date               lastFim        = null;
    private TipoCategoriaContas lastFiltroTipo = null;
    private String             lastCategoriaId = null; // NOVO: Guarda o ID da categoria filtrada

    private DocumentSnapshot ultimoDocumentoVisivelHistorico = null;
    private boolean          isUltimaPaginaHistorico         = false;

    private DocumentSnapshot ultimoDocumentoVisivelFuturo = null;
    private boolean          isUltimaPaginaFuturo         = false;

    private boolean isCarregandoPagina = false;

    private long    ultimoSaldoHistoricoCalculado = 0L;
    private long    ultimoSaldoFuturoCalculado    = 0L;
    private boolean abaAtualEhFuturo              = false;

    // ── Construtor ─────────────────────────────────────────────────────────────

    public ContasViewModel() {
        this.repo = new MovimentacaoRepository();
        this.categoriaRepo = new ContasCategoriaRepository();
        this.filtroEngine = new ContasFiltroEngine();
    }

    // ── Controle de Cache ──────────────────────────────────────────────────────

    public boolean isDadosInvalidados() { return dadosInvalidados; }
    public void invalidarDados() { dadosInvalidados = true; }

    // ── Wrappers de ação (delegam ao repository) ───────────────────────────────

    public void excluir(MovimentacaoModel mov, MovimentacaoRepository.Callback cb) { repo.excluir(mov, cb); }
    public void confirmarMovimentacao(MovimentacaoModel mov, MovimentacaoRepository.Callback cb) { repo.confirmarMovimentacao(mov, cb); }
    public void confirmarMovimentacaoEmMassa(MovimentacaoModel mov, MovimentacaoRepository.Callback cb) { repo.confirmarMovimentacaoEmMassa(mov, cb); }
    public void excluirEmLote(List<MovimentacaoModel> lista, MovimentacaoRepository.Callback cb) { repo.excluirEmLote(lista, cb); }
    public void zerarEstatisticasMensais(MovimentacaoRepository.Callback cb) { repo.zerarEstatisticasMensais(cb); }

    // ── Busca de Categorias para o Filtro (BOAS PRÁTICAS MVVM) ─────────────────

    public interface CategoriasFiltroCallback {
        void onSucesso(List<ContasCategoriaModel> categorias);
        void onErro(String erro);
    }

    public void buscarCategoriasParaFiltro(CategoriasFiltroCallback callback) {
        // A responsabilidade de buscar no banco agora fica no ViewModel/Repository
        FirestoreSchema.contasCategoriasCol()
                .whereEqualTo("ativa", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ContasCategoriaModel> lista = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                            cat.setId(doc.getId());
                            lista.add(cat);
                        }
                        // Ordena alfabeticamente
                        lista.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
                    }
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── Busca e paginação ──────────────────────────────────────────────────────

    public void fetchDados(boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        _carregandoPaginacao.setValue(true);
        Date agora = new Date();

        if (ehModoFuturo) {
            ultimoDocumentoVisivelFuturo = null;
            isUltimaPaginaFuturo = false;
            isCarregandoPagina = true;

            repo.recuperarContasFuturasPaginado(agora, null, new MovimentacaoRepository.DadosPaginadosCallback() {
                @Override public void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDoc) {
                    cacheFuturo = new ArrayList<>(lista);
                    ultimoDocumentoVisivelFuturo = ultimoDoc;
                    if (lista.size() < 100) isUltimaPaginaFuturo = true;
                    finalizarCarregamento(true, lista, callbackExterno);
                }
                @Override public void onErro(String erro) { abortarCarregamento(callbackExterno, erro); }
            });
        } else {
            ultimoDocumentoVisivelHistorico = null;
            isUltimaPaginaHistorico = false;
            isCarregandoPagina = true;

            repo.recuperarHistoricoPaginado(agora, null, new MovimentacaoRepository.DadosPaginadosCallback() {
                @Override public void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDoc) {
                    cacheHistorico = new ArrayList<>(lista);
                    ultimoDocumentoVisivelHistorico = ultimoDoc;
                    if (lista.size() < 100) isUltimaPaginaHistorico = true;
                    finalizarCarregamento(false, lista, callbackExterno);
                }
                @Override public void onErro(String erro) { abortarCarregamento(callbackExterno, erro); }
            });
        }
    }

    private void finalizarCarregamento(boolean isFuturo, List<MovimentacaoModel> lista, MovimentacaoRepository.DadosCallback callbackExterno) {
        isCarregandoPagina = false;
        dadosInvalidados = false;
        _carregandoPaginacao.setValue(false);
        agendarFiltro();
        if (callbackExterno != null) callbackExterno.onSucesso(lista);
    }

    private void abortarCarregamento(MovimentacaoRepository.DadosCallback callbackExterno, String erro) {
        isCarregandoPagina = false;
        _carregandoPaginacao.setValue(false);
        if (callbackExterno != null) callbackExterno.onErro(erro);
    }

    public void carregarMaisHistorico() {
        if (isCarregandoPagina || isUltimaPaginaHistorico) return;
        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true);

        repo.recuperarHistoricoPaginado(new Date(), ultimoDocumentoVisivelHistorico, new MovimentacaoRepository.DadosPaginadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> novaLista, DocumentSnapshot novoUltimoDoc) {
                if (novaLista.isEmpty()) isUltimaPaginaHistorico = true;
                else {
                    cacheHistorico.addAll(novaLista);
                    ultimoDocumentoVisivelHistorico = novoUltimoDoc;
                    if (novaLista.size() < 100) isUltimaPaginaHistorico = true;
                }
                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
                agendarFiltro();
            }
            @Override public void onErro(String erro) { abortarCarregamento(null, erro); }
        });
    }

    public void carregarMaisFuturo() {
        if (isCarregandoPagina || isUltimaPaginaFuturo) return;
        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true);

        repo.recuperarContasFuturasPaginado(new Date(), ultimoDocumentoVisivelFuturo, new MovimentacaoRepository.DadosPaginadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> novaLista, DocumentSnapshot novoUltimoDoc) {
                if (novaLista.isEmpty()) isUltimaPaginaFuturo = true;
                else {
                    cacheFuturo.addAll(novaLista);
                    ultimoDocumentoVisivelFuturo = novoUltimoDoc;
                    if (novaLista.size() < 100) isUltimaPaginaFuturo = true;
                }
                isCarregandoPagina = false;
                _carregandoPaginacao.setValue(false);
                agendarFiltro();
            }
            @Override public void onErro(String erro) { abortarCarregamento(null, erro); }
        });
    }

    // ── Filtros — API pública e Integração com a Engine ──────────────────────

    // NOVO: Parâmetro categoriaId adicionado
    public void aplicarFiltros(String query, Date inicio, Date fim, String categoriaId) {
        this.lastQuery = query;
        this.lastInicio = inicio;
        this.lastFim = fim;
        this.lastCategoriaId = categoriaId;
        agendarFiltro();
    }

    public void setFiltroTipo(TipoCategoriaContas tipo) {
        this.lastFiltroTipo = tipo;
        agendarFiltro();
    }

    public void notificarAbaAtiva(boolean ehFuturo) {
        abaAtualEhFuturo = ehFuturo;
        _saldoListaAtual.setValue(ehFuturo ? ultimoSaldoFuturoCalculado : ultimoSaldoHistoricoCalculado);
    }

    private void agendarFiltro() {
        // O FiltroEngine agora recebe o lastCategoriaId!
        filtroEngine.filtrarAsync(
                new ArrayList<>(cacheHistorico),
                new ArrayList<>(cacheFuturo),
                lastQuery, lastInicio, lastFim, lastFiltroTipo, lastCategoriaId, // <- Novo parâmetro aqui

                (resHistorico, resFuturo, saldoHist, saldoFut) -> {
                    _listaHistorico.setValue(resHistorico);
                    _listaFutura.setValue(resFuturo);
                    _saldoPeriodo.setValue(saldoHist);
                    _saldoFuturo.setValue(saldoFut);

                    ultimoSaldoHistoricoCalculado = saldoHist;
                    ultimoSaldoFuturoCalculado = saldoFut;

                    _saldoListaAtual.setValue(abaAtualEhFuturo ? ultimoSaldoFuturoCalculado : ultimoSaldoHistoricoCalculado);
                }
        );
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        filtroEngine.encerrar(); // Limpa as threads quando o ViewModel for destruído
    }
}