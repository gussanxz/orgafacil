package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtoseservicos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup; // IMPORTANTE
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.adapter.AdapterProdutoServico;
import com.gussanxz.orgafacil.model.ItemVenda;

import java.util.ArrayList;
import java.util.List;

public class ListaProdutosEServicosActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup toggleVisualizacao;
    private ChipGroup chipGroupTipo; // Variável para os filtros
    private RecyclerView recyclerProdutos;
    private AdapterProdutoServico adapter;

    // Precisamos de DUAS listas:
    private List<ItemVenda> listaCompleta; // Guarda todos os dados originais (banco de dados)
    private List<ItemVenda> listaFiltrada; // Guarda o que está sendo exibido agora

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_lista_produtos_e_servicos);

        setupWindowInsets();
        inicializarComponentes();
        carregarDadosExemplo();
        configurarRecyclerView();

        // Configurações de clique
        configurarListenerToggle();
        configurarListenerFiltros(); // NOVO: Listener dos Chips
    }

    private void inicializarComponentes() {
        toggleVisualizacao = findViewById(R.id.toggleVisualizacao);
        recyclerProdutos = findViewById(R.id.recyclerProdutos);
        chipGroupTipo = findViewById(R.id.chipGroupTipo); // ID do seu XML
    }

    private void carregarDadosExemplo() {
        listaCompleta = new ArrayList<>();
        listaCompleta.add(new ItemVenda(1, "Coca Cola 2L", "Refrigerante Gelado", 12.00, ItemVenda.TIPO_PRODUTO));
        listaCompleta.add(new ItemVenda(2, "Mão de Obra", "Instalação Elétrica", 150.00, ItemVenda.TIPO_SERVICO));
        listaCompleta.add(new ItemVenda(3, "Pastel de Carne", "Salgado frito na hora", 8.50, ItemVenda.TIPO_PRODUTO));
        listaCompleta.add(new ItemVenda(4, "Formatação PC", "Windows 11 + Backup", 100.00, ItemVenda.TIPO_SERVICO));
        listaCompleta.add(new ItemVenda(5, "Água 500ml", "Sem gás", 3.00, ItemVenda.TIPO_PRODUTO));

        // Inicialmente, a lista filtrada é igual à completa (mostra tudo)
        listaFiltrada = new ArrayList<>(listaCompleta);
    }

    private void configurarRecyclerView() {
        adapter = new AdapterProdutoServico(listaFiltrada);
        recyclerProdutos.setAdapter(adapter);
        recyclerProdutos.setLayoutManager(new LinearLayoutManager(this));
    }

    // --- LÓGICA DE FILTRO DOS CHIPS ---
    private void configurarListenerFiltros() {
        chipGroupTipo.setOnCheckedChangeListener((group, checkedId) -> {
            listaFiltrada.clear(); // Limpa a lista atual de exibição

            if (checkedId == R.id.chipProdutos) {
                // Filtra só Produtos
                for (ItemVenda item : listaCompleta) {
                    if (item.getTipo() == ItemVenda.TIPO_PRODUTO) {
                        listaFiltrada.add(item);
                    }
                }
            } else if (checkedId == R.id.chipServicos) {
                // Filtra só Serviços
                for (ItemVenda item : listaCompleta) {
                    if (item.getTipo() == ItemVenda.TIPO_SERVICO) {
                        listaFiltrada.add(item);
                    }
                }
            } else {
                // Se for "Todos" ou nenhum selecionado (fallback), mostra tudo
                listaFiltrada.addAll(listaCompleta);
            }

            // Avisa o adapter que os dados mudaram
            adapter.atualizarLista(listaFiltrada);
        });
    }

    private void configurarListenerToggle() {
        toggleVisualizacao.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnVisualizacaoGrade) {
                    recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 2));
                    adapter.setModoGrade(true);
                } else if (checkedId == R.id.btnVisualizacaoLista) {
                    recyclerProdutos.setLayoutManager(new LinearLayoutManager(this));
                    adapter.setModoGrade(false);
                }
            }
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Métodos de navegação...
    public void cadastrarProdutoOuServico (View view) {
        startActivity(new Intent(this, CadastroProdutosServicosActivity.class));
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}