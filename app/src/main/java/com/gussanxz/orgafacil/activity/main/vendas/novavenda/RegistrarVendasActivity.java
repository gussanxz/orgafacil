package com.gussanxz.orgafacil.activity.main.vendas.novavenda;

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

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.adapter.AdapterFiltroCategoriasNovaVenda;
import com.gussanxz.orgafacil.adapter.AdapterProdutoServicoNovaVenda; // Import do novo Adapter
import com.gussanxz.orgafacil.model.Categoria;
import com.gussanxz.orgafacil.model.ItemVenda;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegistrarVendasActivity extends AppCompatActivity {

    // Componentes de Categorias
    private RecyclerView rvCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private List<Categoria> listaCategorias = new ArrayList<>();

    // Componentes de Produtos (NOVO)
    private RecyclerView rvGradeProdutos;
    private AdapterProdutoServicoNovaVenda adapterProdutos;
    private List<ItemVenda> listaCompletaProdutos = new ArrayList<>();
    private List<ItemVenda> listaFiltradaProdutos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_vendas_novavenda_registrarvendas);

        // Configuração de Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.novaVenda), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        // 1. Configura Categorias (Barra horizontal)
        configurarRvCategorias();

        // 2. Configura Produtos (Grade principal)
        configurarRvProdutos();
    }

    private void inicializarComponentes() {
        rvCategorias = findViewById(R.id.rvCategorias);
        // Certifique-se que o ID no XML é este mesmo
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
    }

    // --- MÉTODOS DE CATEGORIA ---
    private void configurarRvCategorias() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCategorias.setLayoutManager(layoutManager);

        carregarDadosFakes();

        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(listaCategorias, this, new AdapterFiltroCategoriasNovaVenda.OnCategoriaSelectedListener() {
            @Override
            public void onCategoriaSelected(Categoria categoria, int position) {
                // Ao clicar na categoria, filtra a grade de baixo
                filtrarProdutosPorCategoria(categoria.getNome());
            }
        });

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void carregarDadosFakes() {
        listaCategorias.clear();
        // Nomes ajustados para bater com a lógica de filtro
        listaCategorias.add(criarCategoriaExemplo("Todos", 0));
        listaCategorias.add(criarCategoriaExemplo("Produtos", 1));
        listaCategorias.add(criarCategoriaExemplo("Serviços", 7));
        listaCategorias.add(criarCategoriaExemplo("Bebidas", 3));
        listaCategorias.add(criarCategoriaExemplo("Lanches", 2));
    }

    private Categoria criarCategoriaExemplo(String nome, int indexIcone) {
        Categoria c = new Categoria();
        c.setId(UUID.randomUUID().toString());
        c.setNome(nome);
        c.setDescricao("Categoria: " + nome);
        c.setIndexIcone(indexIcone);
        c.setAtiva(true);
        return c;
    }

    // --- MÉTODOS DE PRODUTOS (NOVO) ---

    private void configurarRvProdutos() {
        // 1. Carregar dados de exemplo
        carregarDadosProdutosExemplo();

        // 2. Configurar Layout Manager (Grade 3 colunas)
        GridLayoutManager gridManager = new GridLayoutManager(this, 3);
        rvGradeProdutos.setLayoutManager(gridManager);
        // Importante: Desativar o scroll interno do RecyclerView para ele rolar junto com a tela inteira
        rvGradeProdutos.setNestedScrollingEnabled(false);

        // 3. Instanciar o novo Adapter
        adapterProdutos = new AdapterProdutoServicoNovaVenda(listaFiltradaProdutos, new AdapterProdutoServicoNovaVenda.OnItemClickListener() {
            @Override
            public void onItemClick(ItemVenda item) {
                // Ação ao clicar no produto (Adicionar ao carrinho futuramente)
                Toast.makeText(RegistrarVendasActivity.this, "Add: " + item.getNome(), Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Ligar Adapter
        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void carregarDadosProdutosExemplo() {
        listaCompletaProdutos = new ArrayList<>();

        // Usando o construtor da sua classe ItemVenda:
        // (int id, String nome, String descricao, double preco, int tipo)

        // Produtos
        listaCompletaProdutos.add(new ItemVenda(1, "Coca Cola 2L", "Gelada", 12.00, ItemVenda.TIPO_PRODUTO));
        listaCompletaProdutos.add(new ItemVenda(2, "Pastel", "Carne/Queijo", 8.50, ItemVenda.TIPO_PRODUTO));
        listaCompletaProdutos.add(new ItemVenda(3, "Água 500ml", "Sem gás", 3.00, ItemVenda.TIPO_PRODUTO));
        listaCompletaProdutos.add(new ItemVenda(4, "Coxinha", "Frango", 6.00, ItemVenda.TIPO_PRODUTO));

        // Serviços
        listaCompletaProdutos.add(new ItemVenda(5, "Mão de Obra", "Instalação", 150.00, ItemVenda.TIPO_SERVICO));
        listaCompletaProdutos.add(new ItemVenda(6, "Formatação", "PC/Note", 100.00, ItemVenda.TIPO_SERVICO));
        listaCompletaProdutos.add(new ItemVenda(7, "Visita", "Técnica", 50.00, ItemVenda.TIPO_SERVICO));

        // Inicializa a lista filtrada com TUDO
        listaFiltradaProdutos = new ArrayList<>(listaCompletaProdutos);
    }

    private void filtrarProdutosPorCategoria(String nomeCategoria) {
        listaFiltradaProdutos.clear();

        if (nomeCategoria.equalsIgnoreCase("Todos")) {
            listaFiltradaProdutos.addAll(listaCompletaProdutos);
        }
        else if (nomeCategoria.equalsIgnoreCase("Produtos")) {
            for (ItemVenda item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVenda.TIPO_PRODUTO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        }
        else if (nomeCategoria.equalsIgnoreCase("Serviços")) {
            for (ItemVenda item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVenda.TIPO_SERVICO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        }
        else {
            // Caso seja outra categoria (ex: Bebidas), por enquanto mostra tudo
            // (ou você implementa lógica de ID de categoria no futuro)
            Toast.makeText(this, "Filtro: " + nomeCategoria, Toast.LENGTH_SHORT).show();
            listaFiltradaProdutos.addAll(listaCompletaProdutos);
        }

        // Atualiza a grade
        adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }
}