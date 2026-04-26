package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.helper.CarrinhoManager;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrarVendasViewModel extends ViewModel {

    public static final String ID_TODOS_PRODUTOS = "todos_produtos";

    private final CatalogoRepository catalogoRepository = new CatalogoRepository();
    private final CategoriaCatalogoRepository categoriaRepository = new CategoriaCatalogoRepository();
    private final CarrinhoManager carrinho = new CarrinhoManager();

    private ListenerRegistration listenerCatalogo;
    private ListenerRegistration listenerCategorias;
    private ListenerRegistration listenerExibicaoCategorias;

    private Categoria categoriaAtiva = null;
    private String textoBusca = null;
    private String exibicaoCategoriasAtiva = CategoriaCatalogoRepository.EXIBICAO_ALFABETICA;
    private final List<ItemVendaModel> catalogoCompleto = new ArrayList<>();
    private final List<Categoria> categoriasBrutas = new ArrayList<>();
    private final List<String> ordemCategoriasAtiva = new ArrayList<>();
    private final Map<String, List<String>> ordemItensPorCategoria = new HashMap<>();

    private final MutableLiveData<List<ItemVendaModel>> _produtosFiltrados = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<ItemVendaModel>> produtosFiltrados = _produtosFiltrados;

    private final MutableLiveData<List<Categoria>> _categorias = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Categoria>> categorias = _categorias;

    private final MutableLiveData<List<ItemSacolaVendaModel>> _sacola = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<ItemSacolaVendaModel>> sacola = _sacola;

    private final MutableLiveData<Boolean> _modoCategorias = new MutableLiveData<>(true);
    public final LiveData<Boolean> modoCategorias = _modoCategorias;

    private final MutableLiveData<String> _erro = new MutableLiveData<>();
    public final LiveData<String> erro = _erro;

    public RegistrarVendasViewModel() {
        iniciarListeners();
    }

    private void iniciarListeners() {
        listenerCategorias = categoriaRepository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override public void onNovosDados(List<Categoria> lista) {
                categoriasBrutas.clear();
                categoriasBrutas.addAll(lista);
                publicarCategorias();
            }
            @Override public void onErro(String erro) { _erro.postValue(erro); }
        });

        listenerCatalogo = catalogoRepository.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override public void onNovosDados(List<CatalogoModel> lista) {
                catalogoCompleto.clear();
                catalogoCompleto.addAll(lista);
                publicarCategorias();
                publicarProdutosFiltrados();
            }
            @Override public void onErro(String erro) { _erro.postValue(erro); }
        });

        listenerExibicaoCategorias = categoriaRepository.ouvirOrganizacaoCatalogo(new CategoriaCatalogoRepository.OrganizacaoCallback() {
            @Override public void onNovosDados(String exibicaoAtiva,
                                               List<String> ordemCategoriaIds,
                                               Map<String, List<String>> novaOrdemItensPorCategoria) {
                exibicaoCategoriasAtiva = exibicaoAtiva;
                ordemCategoriasAtiva.clear();
                ordemCategoriasAtiva.addAll(ordemCategoriaIds);
                ordemItensPorCategoria.clear();
                ordemItensPorCategoria.putAll(novaOrdemItensPorCategoria);
                publicarCategorias();
                publicarProdutosFiltrados();
            }

            @Override public void onErro(String erro) { _erro.postValue(erro); }
        });
    }

    // ── Modo de exibição ──────────────────────────────────────────────

    public boolean isModoCategorias() {
        return Boolean.TRUE.equals(_modoCategorias.getValue());
    }

    public void entrarEmModoPS() {
        _modoCategorias.setValue(false);
        publicarProdutosFiltrados();
    }

    public void entrarEmModoPS(Categoria categoria) {
        categoriaAtiva = ID_TODOS_PRODUTOS.equals(categoria.getId()) ? null : categoria;
        entrarEmModoPS();
    }

    public void entrarEmModoCategorias() {
        categoriaAtiva = null;
        _modoCategorias.setValue(true);
    }

    // ── Filtros ───────────────────────────────────────────────────────

    public void filtrarPorTexto(String texto) {
        textoBusca = texto.isEmpty() ? null : texto;
        publicarProdutosFiltrados();
    }

    private void publicarProdutosFiltrados() {
        List<ItemVendaModel> filtrados = new ArrayList<>();
        for (ItemVendaModel item : catalogoCompleto) {
            CatalogoModel c = (CatalogoModel) item;
            if (!c.isStatusAtivo()) continue;
            if (!categoriaEstaAtiva(c.getCategoriaId())) continue;
            if (categoriaAtiva != null && !categoriaAtiva.getId().equals(c.getCategoriaId())) continue;
            if (textoBusca != null && !c.getNome().toLowerCase().contains(textoBusca.toLowerCase())) continue;
            filtrados.add(item);
        }
        _produtosFiltrados.setValue(ordenarItensParaExibicao(filtrados));
    }

    private List<ItemVendaModel> ordenarItensParaExibicao(List<ItemVendaModel> itens) {
        List<ItemVendaModel> ordenados = new ArrayList<>(itens);
        ordenados.sort((a, b) -> {
            CatalogoModel itemA = (CatalogoModel) a;
            CatalogoModel itemB = (CatalogoModel) b;

            int categoriaA = indiceCategoria(itemA.getCategoriaId());
            int categoriaB = indiceCategoria(itemB.getCategoriaId());
            if (categoriaA != categoriaB) return Integer.compare(categoriaA, categoriaB);

            int posA = indiceItem(itemA);
            int posB = indiceItem(itemB);
            if (posA != posB) return Integer.compare(posA, posB);

            return itemA.getNome().compareToIgnoreCase(itemB.getNome());
        });
        return ordenados;
    }

    private int indiceCategoria(String categoriaId) {
        int index = ordemCategoriasAtiva.indexOf(categoriaId);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private int indiceItem(CatalogoModel item) {
        List<String> ordem = ordemItensPorCategoria.get(item.getCategoriaId());
        if (ordem == null) return Integer.MAX_VALUE;
        int index = ordem.indexOf(item.getId());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private void publicarCategorias() {
        List<Categoria> lista = new ArrayList<>();
        lista.addAll(CategoriaCatalogoRepository.ordenarParaExibicao(
                filtrarCategoriasSemItens(categoriasBrutas),
                exibicaoCategoriasAtiva,
                ordemCategoriasAtiva));
        lista.add(criarTodosProdutos());
        _categorias.setValue(lista);
    }

    private Categoria criarTodosProdutos() {
        Categoria todos = new Categoria();
        todos.setId(ID_TODOS_PRODUTOS);
        todos.setNome("Todos os produtos");
        todos.setAtiva(true);
        return todos;
    }

    private List<Categoria> filtrarCategoriasSemItens(List<Categoria> categorias) {
        List<Categoria> comItens = new ArrayList<>();
        for (Categoria categoria : categorias) {
            if (!categoria.isAtiva()) continue;
            for (ItemVendaModel item : catalogoCompleto) {
                CatalogoModel c = (CatalogoModel) item;
                if (c.isStatusAtivo() && categoria.getId().equals(c.getCategoriaId())) {
                    comItens.add(categoria);
                    break;
                }
            }
        }
        return comItens;
    }

    private boolean categoriaEstaAtiva(String categoriaId) {
        for (Categoria categoria : categoriasBrutas) {
            if (categoria.getId() != null && categoria.getId().equals(categoriaId)) {
                return categoria.isAtiva();
            }
        }
        return true;
    }

    // ── Carrinho ──────────────────────────────────────────────────────

    public void adicionarItem(ItemVendaModel item) {
        carrinho.adicionar(item, 1);
        publicarSacola();
    }

    public void adicionarItem(ItemVendaModel item, int quantidade) {
        carrinho.adicionar(item, quantidade);
        publicarSacola();
    }

    public void incrementarItem(String chave) {
        carrinho.incrementar(chave);
        publicarSacola();
    }

    public void decrementarItem(String chave) {
        carrinho.decrementar(chave);
        publicarSacola();
    }

    public void removerItem(String chave) {
        carrinho.remover(chave);
        publicarSacola();
    }

    public void restaurarSacola(List<ItemSacolaVendaModel> itens) {
        carrinho.restaurar(itens);
        publicarSacola();
    }

    public void limparSacola() {
        carrinho.limpar();
        publicarSacola();
    }

    private void publicarSacola() {
        _sacola.setValue(carrinho.getItens());
    }

    // ── Getters snapshot (para passagem via Intent) ───────────────────

    public List<ItemSacolaVendaModel> getItensSacola() { return carrinho.getItens(); }
    public int getQuantidadeTotal() { return carrinho.getQuantidadeTotal(); }
    public double getValorTotal() { return carrinho.getValorTotal(); }
    public boolean isSacolaVazia() { return carrinho.isEmpty(); }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerCatalogo != null) listenerCatalogo.remove();
        if (listenerCategorias != null) listenerCategorias.remove();
        if (listenerExibicaoCategorias != null) listenerExibicaoCategorias.remove();
    }
}
