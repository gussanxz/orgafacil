package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.os.Bundle;
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
import java.util.UUID;

public class RegistrarVendasActivity extends AppCompatActivity {

    // Componentes de Categorias
    private RecyclerView rvCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private final List<Categoria> listaCategorias = new ArrayList<>();

    // Componentes de Produtos (NOVO)
    private RecyclerView rvGradeProdutos;
    private AdapterFiltroPorPSNovaVenda adapterProdutos;
    private final List<ItemVendaModel> listaCompletaProdutos = new ArrayList<>();
    private final List<ItemVendaModel> listaFiltradaProdutos = new ArrayList<>();

    private final List<ProdutoModel> cacheProdutos = new ArrayList<>();
    private final List<ServicoModel> cacheServicos = new ArrayList<>();

    private ProdutoRepository produtoRepository;
    private ServicoRepository servicoRepository;
    private ListenerRegistration listenerProdutos;
    private ListenerRegistration listenerServicos;

    private String filtroAtual = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_nova_venda);

        // Configuração de Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.novaVenda), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        produtoRepository = new ProdutoRepository();
        servicoRepository = new ServicoRepository();

        inicializarComponentes();

        // 1. Configura Categorias (Barra horizontal)
        configurarRvCategorias();

        // 2. Configura Produtos (Grade principal)
        configurarRvProdutos();
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
        // Certifique-se que o ID no XML é este mesmo
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
    }

    // --- MÉTODOS DE CATEGORIA ---
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
                    aplicarFiltroAtual();
                }
        );

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void configurarRvProdutos() {
        GridLayoutManager gridManager = new GridLayoutManager(this, 3);
        rvGradeProdutos.setLayoutManager(gridManager);
        // Importante: Desativar o scroll interno do RecyclerView para ele rolar junto com a tela inteira
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
        aplicarFiltroAtual();
    }

    private void aplicarFiltroAtual() {
        listaFiltradaProdutos.clear();

        if ("Produtos".equalsIgnoreCase(filtroAtual)) {
            for (ItemVendaModel item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVendaModel.TIPO_PRODUTO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        } else if ("Serviços".equalsIgnoreCase(filtroAtual)) {
            for (ItemVendaModel item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVendaModel.TIPO_SERVICO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        } else {
            listaFiltradaProdutos.addAll(listaCompletaProdutos);
        }

        if (adapterProdutos != null) {
            adapterProdutos.atualizarLista(listaFiltradaProdutos);
        }
    }
}