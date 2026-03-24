package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.ProdutoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.ServicoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ServicoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RegistrarVendasActivity extends AppCompatActivity {

    private RecyclerView rvCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private final List<Categoria> listaCategorias = new ArrayList<>();

    private RecyclerView rvGradeProdutos;
    private AdapterFiltroPorPSNovaVenda adapterProdutos;
    private final List<ItemVendaModel> listaCompletaProdutos = new ArrayList<>();
    private final List<ItemVendaModel> listaFiltradaProdutos = new ArrayList<>();

    private final List<ProdutoModel> cacheProdutos = new ArrayList<>();
    private final List<ServicoModel> cacheServicos = new ArrayList<>();

    private EditText etBuscarProduto;

    private ProdutoRepository produtoRepository;
    private ServicoRepository servicoRepository;
    private ListenerRegistration listenerProdutos;
    private ListenerRegistration listenerServicos;

    private String filtroAtual = "Todos";
    private String termoBuscaAtual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_nova_venda);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.novaVenda), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        produtoRepository = new ProdutoRepository();
        servicoRepository = new ServicoRepository();

        inicializarComponentes();
        configurarRvCategorias();
        configurarRvProdutos();
        configurarBusca();
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarCatalogoAtivo();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (listenerProdutos != null) {
            listenerProdutos.remove();
            listenerProdutos = null;
        }

        if (listenerServicos != null) {
            listenerServicos.remove();
            listenerServicos = null;
        }
    }

    private void inicializarComponentes() {
        rvCategorias = findViewById(R.id.rvCategorias);
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
        etBuscarProduto = findViewById(R.id.etBuscarProduto);
    }

    private void configurarRvCategorias() {
        rvCategorias.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        carregarCategoriasFixas();

        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(
                listaCategorias,
                this,
                (categoria, position) -> {
                    filtroAtual = categoria.getNome();
                    aplicarFiltros();
                }
        );

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void configurarRvProdutos() {
        GridLayoutManager gridManager = new GridLayoutManager(this, 3);
        rvGradeProdutos.setLayoutManager(gridManager);
        rvGradeProdutos.setNestedScrollingEnabled(false);

        adapterProdutos = new AdapterFiltroPorPSNovaVenda(
                listaFiltradaProdutos,
                item -> Toast.makeText(
                        RegistrarVendasActivity.this,
                        "Add: " + item.getNome(),
                        Toast.LENGTH_SHORT
                ).show()
        );

        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void configurarBusca() {
        if (etBuscarProduto == null) return;

        etBuscarProduto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // sem ação
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                termoBuscaAtual = s != null ? s.toString().trim() : "";
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // sem ação
            }
        });
    }

    private void carregarCategoriasFixas() {
        listaCategorias.clear();
        listaCategorias.add(criarCategoriaFiltro("todos", "Todos", 7));
        listaCategorias.add(criarCategoriaFiltro("produtos", "Produtos", 0));
        listaCategorias.add(criarCategoriaFiltro("servicos", "Serviços", 7));
    }

    private Categoria criarCategoriaFiltro(String id, String nome, int indexIcone) {
        Categoria categoria = new Categoria();
        categoria.setId(id != null ? id : UUID.randomUUID().toString());
        categoria.setNome(nome);
        categoria.setDescricao(nome);
        categoria.setIndexIcone(indexIcone);
        categoria.setAtiva(true);
        return categoria;
    }

    private void carregarCatalogoAtivo() {
        listenerProdutos = produtoRepository.listarTempoReal(new ProdutoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<ProdutoModel> lista) {
                cacheProdutos.clear();

                for (ProdutoModel produto : lista) {
                    if (produto != null && produto.isStatusAtivo()) {
                        cacheProdutos.add(produto);
                    }
                }

                atualizarCatalogoUnificado();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(
                        RegistrarVendasActivity.this,
                        "Erro ao carregar produtos: " + erro,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        listenerServicos = servicoRepository.listarTempoReal(new ServicoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<ServicoModel> lista) {
                cacheServicos.clear();

                for (ServicoModel servico : lista) {
                    if (servico != null && servico.isStatusAtivo()) {
                        cacheServicos.add(servico);
                    }
                }

                atualizarCatalogoUnificado();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(
                        RegistrarVendasActivity.this,
                        "Erro ao carregar serviços: " + erro,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void atualizarCatalogoUnificado() {
        listaCompletaProdutos.clear();
        listaCompletaProdutos.addAll(cacheProdutos);
        listaCompletaProdutos.addAll(cacheServicos);
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            if (!passaNoFiltroTipo(item)) {
                continue;
            }

            if (!passaNoFiltroBusca(item, termoBuscaAtual)) {
                continue;
            }

            listaFiltradaProdutos.add(item);
        }

        if (adapterProdutos != null) {
            adapterProdutos.atualizarLista(listaFiltradaProdutos);
        }
    }

    private boolean passaNoFiltroTipo(ItemVendaModel item) {
        if ("Produtos".equalsIgnoreCase(filtroAtual)) {
            return item.getTipo() == ItemVendaModel.TIPO_PRODUTO;
        }

        if ("Serviços".equalsIgnoreCase(filtroAtual)) {
            return item.getTipo() == ItemVendaModel.TIPO_SERVICO;
        }

        return true;
    }

    private boolean passaNoFiltroBusca(ItemVendaModel item, String termoBusca) {
        if (termoBusca == null || termoBusca.trim().isEmpty()) {
            return true;
        }

        String termoNormalizado = termoBusca.trim().toLowerCase(Locale.ROOT);
        String nome = item.getNome() != null ? item.getNome().toLowerCase(Locale.ROOT) : "";
        String descricao = item.getDescricao() != null ? item.getDescricao().toLowerCase(Locale.ROOT) : "";

        return nome.contains(termoNormalizado) || descricao.contains(termoNormalizado);
    }
}