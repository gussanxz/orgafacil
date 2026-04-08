package com.gussanxz.orgafacil.funcionalidades.contas;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.repository.ContasCategoriaRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.CategoriaMovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasFiltroEngine;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // ── Dependências ───────────────────────────────────────────────────────────
    private final MovimentacaoRepository repo;
    private final ContasCategoriaRepository categoriaRepo;
    private final ContasFiltroEngine filtroEngine;
    private final UsuarioRepository usuarioRepo;

    // ── LiveData públicos ──────────────────────────────────────────────────────

    // Nome do usuário cacheado — buscado apenas uma vez por ciclo de vida do
    // ViewModel. Como o ViewModel sobrevive a rotações e navegação entre telas
    // filhas, o nome só é buscado novamente se o ViewModel for destruído
    // (ex: processo morto, logout). Isso elimina chamadas repetidas ao Firestore
    // toda vez que carregarDados() é chamado na Activity.
    private final MutableLiveData<String> _nomeUsuario = new MutableLiveData<>();
    public final LiveData<String> nomeUsuario = _nomeUsuario;
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
    private String             lastCategoriaId = null;

    // Versão String do filtroTipo para passar diretamente ao Firestore.
    // Mantemos os dois porque lastFiltroTipo é usado pela ContasFiltroEngine (local)
    // e lastFiltroTipoFirestore é usado nas queries paginadas do repositório.
    // Sempre sincronizados em setFiltroTipo() — nunca atualize um sem o outro.
    private String lastFiltroTipoFirestore = null;

    // Intervalo de valor em centavos para o RangeSlider. -1 = sem limite.
    private long lastValorMinCentavos = -1L;
    private long lastValorMaxCentavos = -1L;

    private DocumentSnapshot ultimoDocumentoVisivelHistorico = null;
    private boolean          isUltimaPaginaHistorico         = false;

    private DocumentSnapshot ultimoDocumentoVisivelFuturo = null;
    private boolean          isUltimaPaginaFuturo         = false;

    private boolean isCarregandoPagina = false;

    private long    ultimoSaldoHistoricoCalculado = 0L;
    private long    ultimoSaldoFuturoCalculado    = 0L;
    private boolean abaAtualEhFuturo              = false;
    private final MutableLiveData<Boolean> _isPrimeiroCarregamento = new MutableLiveData<>(true);
    public final LiveData<Boolean> isPrimeiroCarregamento = _isPrimeiroCarregamento;

    private final Handler debounceHandler = new Handler(android.os.Looper.getMainLooper());
    private Runnable filtroRunnable = null;
    private static final long DEBOUNCE_MS = 200L;


    // Timestamp da última vez que os dados locais foram sincronizados com o Firestore.
// Compara com ultimaAtualizacao do resumo para evitar buscas desnecessárias.
    private com.google.firebase.Timestamp ultimaAtualizacaoLocal = null;

    // ── Construtor ─────────────────────────────────────────────────────────────

    public ContasViewModel() {
        this.repo = new MovimentacaoRepository();
        this.categoriaRepo = new ContasCategoriaRepository();
        this.filtroEngine = new ContasFiltroEngine();
        this.usuarioRepo = new UsuarioRepository();
    }

    // ── Nome do usuário ────────────────────────────────────────────────────────

    /**
     * Busca o nome do usuário no Firestore apenas se ainda não estiver cacheado.
     * Chamadas subsequentes (ao voltar de telas filhas, invalidar cache etc.)
     * retornam imediatamente sem nenhuma operação de rede.
     * O LiveData nomeUsuario é observado pela Activity uma única vez em setupObservers().
     */
    public void carregarNomeUsuario() {
        if (_nomeUsuario.getValue() != null) return; // já cacheado — sem nova busca
        usuarioRepo.obterNomeUsuario(nome -> _nomeUsuario.setValue(nome));
    }

    // ── Controle de Cache ──────────────────────────────────────────────────────

    public boolean isDadosInvalidados() { return dadosInvalidados; }
    public void invalidarDados() { dadosInvalidados = true; }

    // ── Sync por timestamp ─────────────────────────────────────────────────────

    /** Retorna o timestamp do último fetch bem-sucedido. null = nunca buscou nesta sessão. */
    public com.google.firebase.Timestamp getUltimaAtualizacaoLocal() {
        return ultimaAtualizacaoLocal;
    }

    /**
     * Compara o timestamp do Firestore com o do último fetch local.
     * Retorna true se os dados locais já estão na versão mais recente — não precisa buscar.
     * Retorna false se nunca buscou, ou se o Firestore tem dados mais novos.
     */
    public boolean dadosEstaAtualizados(com.google.firebase.Timestamp tsFirestore) {
        if (ultimaAtualizacaoLocal == null || tsFirestore == null) return false;
        // "antes" = tsFirestore é mais novo que o local = dados desatualizados
        return !ultimaAtualizacaoLocal.toDate().before(tsFirestore.toDate());
    }

    public void marcarComoAtualizado(com.google.firebase.Timestamp ts) {
        this.ultimaAtualizacaoLocal = ts;
    }

    /**
     * Zera o cache e o timestamp local.
     * Use quando o usuário escolhe "baixar versão do servidor" no diálogo de conflito.
     */
    public void invalidarCacheCompleto() {
        cacheHistorico = new ArrayList<>();
        cacheFuturo = new ArrayList<>();
        ultimaAtualizacaoLocal = null;
        ultimoDocumentoVisivelHistorico = null;
        ultimoDocumentoVisivelFuturo = null;
        dadosInvalidados = true;
    }

    public boolean isUltimaPaginaHistorico() { return isUltimaPaginaHistorico; }
    public boolean isUltimaPaginaFuturo()    { return isUltimaPaginaFuturo; }

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
        // Agora sim, delegando para o repositório oficial que você indicou!
        categoriaRepo.listarTodasAtivas(new ContasCategoriaRepository.ListaCallback() {
            @Override
            public void onSucesso(List<ContasCategoriaModel> lista) {
                callback.onSucesso(lista);
            }

            @Override
            public void onErro(String erro) {
                callback.onErro(erro);
            }
        });
    }

    // ── Busca e paginação ──────────────────────────────────────────────────────

    public void fetchDados(boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        _carregandoPaginacao.setValue(true);
        iniciarBuscaPaginada(ehModoFuturo, callbackExterno);
    }

    // ── Reset + busca da primeira página (extrai a lógica duplicada do if/else) ─
    //
    // Ambos os ramos de fetchDados() faziam exatamente o mesmo:
    //   1. Zerar cursor e flag de última página do modo correspondente
    //   2. Marcar isCarregandoPagina = true
    //   3. Chamar o método de paginação do repositório (futuro ou histórico)
    //   4. No onSucesso: atualizar cache, cursor e flag, depois delegar a finalizarCarregamento()
    //   5. No onErro: delegar a abortarCarregamento()
    //
    // A única diferença entre os dois ramos eram os campos acessados (futuro vs histórico)
    // e o método do repositório chamado. Extrair para cá elimina a duplicação sem alterar
    // nenhuma lógica — fetchDados() continua com a mesma assinatura pública.
    private void iniciarBuscaPaginada(boolean isFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {
        Date agora = new Date();

        if (isFuturo) {
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

        _isPrimeiroCarregamento.setValue(false); // <--- NOVO: Avisa que o primeiro carregamento acabou!

        // Registra o momento em que os dados foram buscados.
        // Se ultimaAtualizacaoLocal ainda for null (primeiro fetch da sessão),
        // marca com now() como ponto de referência. Quando o resumo do Firestore
        // chegar com um timestamp mais novo, sabemos que houve mudança depois.
        if (ultimaAtualizacaoLocal == null) {
            ultimaAtualizacaoLocal = com.google.firebase.Timestamp.now();
        }

        agendarFiltro();
        if (callbackExterno != null) callbackExterno.onSucesso(lista);
    }

    private void abortarCarregamento(MovimentacaoRepository.DadosCallback callbackExterno, String erro) {
        isCarregandoPagina = false;
        _carregandoPaginacao.setValue(false);
        if (callbackExterno != null) callbackExterno.onErro(erro);
    }

    // ── Reset de cursores ──────────────────────────────────────────────────────
    //
    // Chamado sempre que um filtro que afeta a query do Firestore muda
    // (categoriaId ou filtroTipo). Zera os cursores de ambos os modos para
    // garantir que a próxima página seja buscada com a query correta.
    // O cache também é zerado — dados de uma query anterior não são válidos
    // para a nova query e seriam misturados erroneamente pelo agendarFiltro().
    private void resetarCursoresPaginacao() {
        ultimoDocumentoVisivelHistorico = null;
        isUltimaPaginaHistorico         = false;
        ultimoDocumentoVisivelFuturo    = null;
        isUltimaPaginaFuturo            = false;
        cacheHistorico                  = new ArrayList<>();
        cacheFuturo                     = new ArrayList<>();
    }

    public void carregarMaisHistorico() {
        if (isCarregandoPagina || isUltimaPaginaHistorico) return;
        isCarregandoPagina = true;
        _carregandoPaginacao.setValue(true);

        // Passa os filtros Firestore ativos para que o cursor seja consistente
        // com a query original. Sem isso, paginar com categoriaId ou filtroTipo
        // ativos retornaria docs de todas as categorias/tipos a partir do cursor.
        repo.recuperarHistoricoPaginado(new Date(), ultimoDocumentoVisivelHistorico,
                lastCategoriaId, lastFiltroTipoFirestore,
                new MovimentacaoRepository.DadosPaginadosCallback() {
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

        repo.recuperarContasFuturasPaginado(new Date(), ultimoDocumentoVisivelFuturo,
                lastCategoriaId, lastFiltroTipoFirestore,
                new MovimentacaoRepository.DadosPaginadosCallback() {
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
        // Detecta mudança nos filtros que o Firestore consegue aplicar na query.
        // Quando mudam, o cursor da paginação aponta para uma query diferente —
        // continuar paginando com ele retornaria documentos errados ou duplicados.
        // Solução: zerar os cursores e recarregar a primeira página com os novos filtros.
        //
        // Filtros que afetam o Firestore: categoriaId (whereEqualTo "categoria_id")
        // Filtros que NÃO afetam o Firestore: query (texto), inicio, fim — aplicados
        // localmente pela ContasFiltroEngine, portanto não invalidam o cursor.
        boolean categoriaIdMudou = !java.util.Objects.equals(this.lastCategoriaId, categoriaId);

        this.lastQuery      = query;
        this.lastInicio     = inicio;
        this.lastFim        = fim;
        this.lastCategoriaId = categoriaId;

        if (categoriaIdMudou) {
            resetarCursoresPaginacao();
            iniciarBuscaPaginada(abaAtualEhFuturo, null);
            return; // iniciarBuscaPaginada chama agendarFiltro() via finalizarCarregamento()
        }

        agendarFiltro();
    }

    public void setFiltroTipo(TipoCategoriaContas tipo) {
        // Mesmo raciocínio de aplicarFiltros(): filtroTipo vira whereEqualTo("tipo", ...)
        // no Firestore — se mudou, o cursor atual é inválido para a nova query.
        boolean tipoMudou = !java.util.Objects.equals(this.lastFiltroTipo, tipo);

        this.lastFiltroTipo = tipo;
        this.lastFiltroTipoFirestore = (tipo != null) ? tipo.name() : null;

        if (tipoMudou) {
            resetarCursoresPaginacao();
            iniciarBuscaPaginada(abaAtualEhFuturo, null);
            return;
        }

        agendarFiltro();
    }

    public void notificarAbaAtiva(boolean ehFuturo) {
        abaAtualEhFuturo = ehFuturo;
        _saldoListaAtual.setValue(ehFuturo ? ultimoSaldoFuturoCalculado : ultimoSaldoHistoricoCalculado);
    }

    /**
     * Define o intervalo de valor para o RangeSlider.
     * Valores em centavos. Passar -1 em ambos remove o filtro de valor.
     * Não invalida o cursor do Firestore — filtro aplicado apenas localmente.
     */
    public void setFiltroValor(long valorMinCentavos, long valorMaxCentavos) {
        this.lastValorMinCentavos = valorMinCentavos;
        this.lastValorMaxCentavos = valorMaxCentavos;
        agendarFiltro();
    }

    private void agendarFiltro() {
        if (filtroRunnable != null) {
            debounceHandler.removeCallbacks(filtroRunnable);
        }

        filtroRunnable = () -> filtroEngine.filtrarAsync(
                new ArrayList<>(cacheHistorico),
                new ArrayList<>(cacheFuturo),
                lastQuery, lastInicio, lastFim, lastFiltroTipo, lastCategoriaId,
                lastValorMinCentavos, lastValorMaxCentavos,

                (resHistorico, resFuturo, saldoHist, saldoFut) -> {
                    _listaHistorico.setValue(resHistorico);
                    _listaFutura.setValue(resFuturo);
                    _saldoPeriodo.setValue(saldoHist);
                    _saldoFuturo.setValue(saldoFut);

                    ultimoSaldoHistoricoCalculado = saldoHist;
                    ultimoSaldoFuturoCalculado = saldoFut;

                    _saldoListaAtual.setValue(
                            abaAtualEhFuturo
                                    ? ultimoSaldoFuturoCalculado
                                    : ultimoSaldoHistoricoCalculado);
                }
        );

        debounceHandler.postDelayed(filtroRunnable, DEBOUNCE_MS);
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();

        // Cancela qualquer disparo de filtro pendente no Handler antes de
        // encerrar a engine. Sem isso, filtroRunnable poderia executar após
        // onCleared() e tentar usar filtroEngine já encerrada.
        if (filtroRunnable != null) {
            debounceHandler.removeCallbacks(filtroRunnable);
            filtroRunnable = null;
        }

        filtroEngine.encerrar(); // existia antes — mantido intacto
    }


}