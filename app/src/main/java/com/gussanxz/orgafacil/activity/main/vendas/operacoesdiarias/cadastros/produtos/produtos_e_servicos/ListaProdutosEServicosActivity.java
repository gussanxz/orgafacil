package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtos_e_servicos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager; // IMPORTANTE
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup; // IMPORTANTE
import com.google.android.material.chip.ChipGroup;
import com.gussanxz.orgafacil.R;
// Ajuste os imports abaixo conforme seus pacotes
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtos_e_servicos.produtos.CadastroProdutoActivity;
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtos_e_servicos.servicos.CadastroServicoActivity;
import com.gussanxz.orgafacil.ui.vendas.nova_venda.AdapterExibirPSGradeListaNovaVenda;
import com.gussanxz.orgafacil.data.model.ItemVenda;
import com.gussanxz.orgafacil.data.model.Produto;
import com.gussanxz.orgafacil.data.model.Servico;
import com.gussanxz.orgafacil.data.repository.ProdutoRepository;
import com.gussanxz.orgafacil.data.repository.ServicoRepository;

import java.util.ArrayList;
import java.util.List;

public class ListaProdutosEServicosActivity extends AppCompatActivity {

    private RecyclerView recyclerProdutos;
    private ChipGroup chipGroupTipo;
    private MaterialButtonToggleGroup toggleVisualizacao; // VOLTOU!

    private AdapterExibirPSGradeListaNovaVenda adapter;

    // Listas
    private final List<ItemVenda> listaTotal = new ArrayList<>();
    private final List<ItemVenda> listaFiltrada = new ArrayList<>();

    // Caches
    private final List<ItemVenda> cacheProdutos = new ArrayList<>();
    private final List<ItemVenda> cacheServicos = new ArrayList<>();

    // Repositórios
    private ProdutoRepository produtoRepo;
    private ServicoRepository servicoRepo;
    private com.google.firebase.firestore.ListenerRegistration listenerProdutos;
    private com.google.firebase.firestore.ListenerRegistration listenerServicos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_produtos_e_servicos);

        setupWindowInsets();

        produtoRepo = new ProdutoRepository();
        servicoRepo = new ServicoRepository();

        // Inicializa Componentes
        recyclerProdutos = findViewById(R.id.recyclerProdutos);
        chipGroupTipo = findViewById(R.id.chipGroupTipo);
        toggleVisualizacao = findViewById(R.id.toggleVisualizacao); // Recupera do XML

        configurarRecyclerView();
        configurarFiltros();
        configurarAlternanciaVisualizacao(); // Configura o clique Lista vs Grade
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerProdutos != null) listenerProdutos.remove();
        if (listenerServicos != null) listenerServicos.remove();
    }

    private void carregarDados() {
        // 1. Produtos
        listenerProdutos = produtoRepo.listarTempoReal(new ProdutoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Produto> lista) {
                cacheProdutos.clear();
                cacheProdutos.addAll(lista);
                atualizarListaUnificada();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaProdutosEServicosActivity.this, "Erro Prod: " + erro, Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Serviços
        listenerServicos = servicoRepo.listarTempoReal(new ServicoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Servico> lista) {
                cacheServicos.clear();
                cacheServicos.addAll(lista);
                atualizarListaUnificada();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaProdutosEServicosActivity.this, "Erro Serv: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void atualizarListaUnificada() {
        listaTotal.clear();
        listaTotal.addAll(cacheProdutos);
        listaTotal.addAll(cacheServicos);
        aplicarFiltros();
    }

    private void configurarRecyclerView() {
        adapter = new AdapterExibirPSGradeListaNovaVenda(listaFiltrada, this::onItemClick);
        // Padrão inicial: Lista Vertical
        recyclerProdutos.setLayoutManager(new LinearLayoutManager(this));
        recyclerProdutos.setAdapter(adapter);
    }

    // --- NOVA LÓGICA: LISTA vs GRADE ---
    private void configurarAlternanciaVisualizacao() {
        toggleVisualizacao.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean modoGrade = (checkedId == R.id.btnVisualizacaoGrade);

                if (modoGrade) {
                    recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 2));
                } else {
                    recyclerProdutos.setLayoutManager(new LinearLayoutManager(this));
                }
                // Avisa o adapter para trocar o XML inflado (de Lista para Grade)
                if (adapter != null) {
                    adapter.setModoGrade(modoGrade);
                }
            }
        });
    }

    private void onItemClick(ItemVenda item) {
        Intent intent;
        if (item instanceof Produto) {
            intent = new Intent(this, CadastroProdutoActivity.class);
            Produto p = (Produto) item;
            intent.putExtra("id", p.getId());
            intent.putExtra("nome", p.getNome());
            intent.putExtra("categoria", p.getCategoria());
            intent.putExtra("preco", p.getPreco());
        } else {
            intent = new Intent(this, CadastroServicoActivity.class);
            Servico s = (Servico) item;
            intent.putExtra("id", s.getId());
            intent.putExtra("nome", s.getDescricao());
            intent.putExtra("categoria", s.getCategoria());
            intent.putExtra("preco", s.getValor());
        }
        startActivity(intent);
    }

    public void cadastrarProdutoOuServico(View view) {
        String[] opcoes = {"Novo Produto", "Novo Serviço"};
        new AlertDialog.Builder(this)
                .setTitle("O que deseja cadastrar?")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, CadastroProdutoActivity.class));
                    else startActivity(new Intent(this, CadastroServicoActivity.class));
                })
                .show();
    }

    private void configurarFiltros() {
        chipGroupTipo.setOnCheckedChangeListener((group, checkedId) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        int id = chipGroupTipo.getCheckedChipId();
        listaFiltrada.clear();

        for (ItemVenda item : listaTotal) {
            boolean isProd = item.getTipo() == ItemVenda.TIPO_PRODUTO;
            boolean isServ = item.getTipo() == ItemVenda.TIPO_SERVICO;

            if (id == R.id.chipProdutos && isProd) listaFiltrada.add(item);
            else if (id == R.id.chipServicos && isServ) listaFiltrada.add(item);
            else if (id == View.NO_ID || id == R.id.chipTodos) listaFiltrada.add(item);
        }
        adapter.notifyDataSetChanged();
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}