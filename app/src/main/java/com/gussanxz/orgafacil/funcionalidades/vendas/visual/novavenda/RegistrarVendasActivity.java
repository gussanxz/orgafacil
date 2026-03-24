package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RegistrarVendasActivity extends AppCompatActivity {

    private RecyclerView rvCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private final List<Categoria> listaCategorias = new ArrayList<>();

    private RecyclerView rvGradeProdutos;
    private AdapterFiltroPorPSNovaVenda adapterProdutos;
    private List<ItemVendaModel> listaCompletaProdutos = new ArrayList<>();
    private List<ItemVendaModel> listaFiltradaProdutos = new ArrayList<>();

    private EditText etBuscarProduto;

    private TextView txtSacolaQuantidade;
    private TextView txtSacolaTitulo;
    private TextView txtSacolaSubtotal;
    private TextView txtCobrarTotal;
    private LinearLayout btnCobrar;

    private final Map<String, ItemSacolaVendaModel> sacolaMap = new LinkedHashMap<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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

        inicializarComponentes();
        configurarRvCategorias();
        configurarRvProdutos();
        configurarBotaoCobrar();
        atualizarResumoSacola();
    }

    private void inicializarComponentes() {
        rvCategorias = findViewById(R.id.rvCategorias);
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
        etBuscarProduto = findViewById(R.id.etBuscarProduto);

        txtSacolaQuantidade = findViewById(R.id.txtSacolaQuantidade);
        txtSacolaTitulo = findViewById(R.id.txtSacolaTitulo);
        txtSacolaSubtotal = findViewById(R.id.txtSacolaSubtotal);
        txtCobrarTotal = findViewById(R.id.txtCobrarTotal);
        btnCobrar = findViewById(R.id.btnCobrar);
    }

    private void configurarRvCategorias() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCategorias.setLayoutManager(layoutManager);

        carregarDadosFakes();

        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(listaCategorias, this,
                new AdapterFiltroCategoriasNovaVenda.OnCategoriaSelectedListener() {
                    @Override
                    public void onCategoriaSelected(Categoria categoria, int position) {
                        filtrarProdutosPorCategoria(categoria.getNome());
                    }
                });

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void configurarRvProdutos() {
        carregarDadosProdutosExemplo();

        GridLayoutManager gridManager = new GridLayoutManager(this, 3);
        rvGradeProdutos.setLayoutManager(gridManager);
        rvGradeProdutos.setNestedScrollingEnabled(false);

        adapterProdutos = new AdapterFiltroPorPSNovaVenda(listaFiltradaProdutos,
                new AdapterFiltroPorPSNovaVenda.OnItemClickListener() {
                    @Override
                    public void onItemClick(ItemVendaModel item) {
                        adicionarItemNaSacola(item);
                    }
                });

        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void configurarBotaoCobrar() {
        if (btnCobrar == null) return;

        btnCobrar.setOnClickListener(v -> {
            if (sacolaMap.isEmpty()) {
                Toast.makeText(
                        RegistrarVendasActivity.this,
                        "Adicione ao menos um item para continuar.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Toast.makeText(
                    RegistrarVendasActivity.this,
                    "Próximo passo: fluxo de cobrança.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void carregarDadosFakes() {
        listaCategorias.clear();
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

    private void carregarDadosProdutosExemplo() {
        listaCompletaProdutos = new ArrayList<>();
        listaFiltradaProdutos = new ArrayList<>(listaCompletaProdutos);
    }

    private void filtrarProdutosPorCategoria(String nomeCategoria) {
        listaFiltradaProdutos.clear();

        if (nomeCategoria.equalsIgnoreCase("Todos")) {
            listaFiltradaProdutos.addAll(listaCompletaProdutos);
        } else if (nomeCategoria.equalsIgnoreCase("Produtos")) {
            for (ItemVendaModel item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVendaModel.TIPO_PRODUTO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        } else if (nomeCategoria.equalsIgnoreCase("Serviços")) {
            for (ItemVendaModel item : listaCompletaProdutos) {
                if (item.getTipo() == ItemVendaModel.TIPO_SERVICO) {
                    listaFiltradaProdutos.add(item);
                }
            }
        } else {
            Toast.makeText(this, "Filtro: " + nomeCategoria, Toast.LENGTH_SHORT).show();
            listaFiltradaProdutos.addAll(listaCompletaProdutos);
        }

        adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }

    private void adicionarItemNaSacola(ItemVendaModel item) {
        String chave = ItemSacolaVendaModel.gerarChave(item);
        ItemSacolaVendaModel itemSacola = sacolaMap.get(chave);

        if (itemSacola == null) {
            sacolaMap.put(chave, new ItemSacolaVendaModel(item));
        } else {
            itemSacola.incrementarQuantidade();
        }

        atualizarResumoSacola();

        Toast.makeText(
                this,
                item.getNome() + " adicionado à sacola",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void atualizarResumoSacola() {
        int quantidadeTotal = getQuantidadeTotalSacola();
        double valorTotal = getValorTotalSacola();

        if (txtSacolaQuantidade != null) {
            txtSacolaQuantidade.setText(String.valueOf(quantidadeTotal));
        }

        if (txtSacolaTitulo != null) {
            txtSacolaTitulo.setText(
                    quantidadeTotal == 0
                            ? "Sacola vazia"
                            : quantidadeTotal + (quantidadeTotal == 1 ? " item na sacola" : " itens na sacola")
            );
        }

        if (txtSacolaSubtotal != null) {
            txtSacolaSubtotal.setText(formatadorMoeda.format(valorTotal));
        }

        if (txtCobrarTotal != null) {
            txtCobrarTotal.setText(formatadorMoeda.format(valorTotal));
        }

        if (btnCobrar != null) {
            boolean habilitado = quantidadeTotal > 0;
            btnCobrar.setEnabled(habilitado);
            btnCobrar.setAlpha(habilitado ? 1f : 0.5f);
        }
    }

    private int getQuantidadeTotalSacola() {
        int total = 0;

        for (ItemSacolaVendaModel item : sacolaMap.values()) {
            total += item.getQuantidade();
        }

        return total;
    }

    private double getValorTotalSacola() {
        double total = 0.0;

        for (ItemSacolaVendaModel item : sacolaMap.values()) {
            total += item.getSubtotal();
        }

        return total;
    }
}