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
import java.util.List;

public class RegistrarVendasViewModel extends ViewModel {

    private final CatalogoRepository catalogoRepository = new CatalogoRepository();
    private final CategoriaCatalogoRepository categoriaRepository = new CategoriaCatalogoRepository();
    private final CarrinhoManager carrinho = new CarrinhoManager();

    private ListenerRegistration listenerCatalogo;
    private ListenerRegistration listenerCategorias;

    private Categoria categoriaAtiva = null;
    private String textoBusca = null;
    private final List<ItemVendaModel> catalogoCompleto = new ArrayList<>();
    private final List<Categoria> categoriasBrutas = new ArrayList<>();

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
        categoriaAtiva = "todos".equals(categoria.getId()) ? null : categoria;
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
            if (categoriaAtiva != null && !categoriaAtiva.getId().equals(c.getCategoriaId())) continue;
            if (textoBusca != null && !c.getNome().toLowerCase().contains(textoBusca.toLowerCase())) continue;
            filtrados.add(item);
        }
        _produtosFiltrados.setValue(filtrados);
    }

    private void publicarCategorias() {
        List<Categoria> lista = new ArrayList<>();
        Categoria todos = new Categoria();
        todos.setId("todos");
        todos.setNome("Todos");
        todos.setAtiva(true);
        lista.add(todos);
        lista.addAll(filtrarCategoriasSemItens(categoriasBrutas));
        _categorias.setValue(lista);
    }

    private List<Categoria> filtrarCategoriasSemItens(List<Categoria> categorias) {
        List<Categoria> comItens = new ArrayList<>();
        for (Categoria categoria : categorias) {
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
    }
}
