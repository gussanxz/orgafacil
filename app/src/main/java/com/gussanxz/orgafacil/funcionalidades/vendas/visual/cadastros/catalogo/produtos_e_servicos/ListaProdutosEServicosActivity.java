package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.AdapterExibirPSGradeListaNovaVenda;

import java.util.ArrayList;
import java.util.List;

public class ListaProdutosEServicosActivity extends AppCompatActivity {

    private RecyclerView recyclerProdutos;
    private ChipGroup chipGroupTipo;
    private MaterialButtonToggleGroup toggleVisualizacao;

    private AdapterExibirPSGradeListaNovaVenda adapter;

    // Listas
    private final List<ItemVendaModel> listaTotal    = new ArrayList<>();
    private final List<ItemVendaModel> listaFiltrada = new ArrayList<>();

    // Repositório unificado
    private CatalogoRepository catalogoRepo;
    private ListenerRegistration listenerCatalogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_produtos_e_servicos);

        setupWindowInsets();

        catalogoRepo = new CatalogoRepository();

        recyclerProdutos   = findViewById(R.id.recyclerProdutos);
        chipGroupTipo      = findViewById(R.id.chipGroupTipo);
        toggleVisualizacao = findViewById(R.id.toggleVisualizacao);

        configurarRecyclerView();
        configurarFiltros();
        configurarAlternanciaVisualizacao();
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerCatalogo != null) listenerCatalogo.remove();
    }

    // ── Carregamento ──────────────────────────────────────────────────

    private void carregarDados() {
        listenerCatalogo = catalogoRepo.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<CatalogoModel> lista) {
                listaTotal.clear();
                listaTotal.addAll(lista);
                aplicarFiltros();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaProdutosEServicosActivity.this,
                        "Erro ao carregar catálogo: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── RecyclerView ──────────────────────────────────────────────────

    private void configurarRecyclerView() {
        adapter = new AdapterExibirPSGradeListaNovaVenda(listaFiltrada, this::onItemClick);
        recyclerProdutos.setLayoutManager(new LinearLayoutManager(this));
        recyclerProdutos.setAdapter(adapter);
    }

    private void configurarAlternanciaVisualizacao() {
        toggleVisualizacao.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean modoGrade = (checkedId == R.id.btnVisualizacaoGrade);
                recyclerProdutos.setLayoutManager(
                        modoGrade
                                ? new GridLayoutManager(this, 3)
                                : new LinearLayoutManager(this));
                if (adapter != null) adapter.setModoGrade(modoGrade);
            }
        });
    }

    // ── Clique no item → abre CadastroCatalogoActivity ───────────────

    private void onItemClick(ItemVendaModel item) {
        if (!(item instanceof CatalogoModel)) {
            Toast.makeText(this, "Item inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        CatalogoModel c = (CatalogoModel) item;
        Intent intent = new Intent(this, CadastroCatalogoActivity.class);
        intent.putExtra("id",          c.getId());
        intent.putExtra("nome",        c.getNome());
        intent.putExtra("tipo",        c.getTipoStr());
        intent.putExtra("preco",       c.getPreco());
        intent.putExtra("categoriaId", c.getCategoriaId());
        intent.putExtra("categoria",   c.getCategoria());
        intent.putExtra("descricao",   c.getDescricao());
        intent.putExtra("statusAtivo", c.isStatusAtivo());
        intent.putExtra("iconeIndex",  c.getIconeIndex());
        intent.putExtra("urlFoto", c.getUrlFoto());
        startActivity(intent);
    }

    // ── FAB / botão de cadastro ───────────────────────────────────────

    /**
     * Chamado pelo XML via android:onClick="cadastrarProdutoOuServico".
     * Exibe um diálogo para o usuário escolher o tipo antes de abrir o cadastro.
     */
    public void cadastrarProdutoOuServico(View view) {
        startActivity(new Intent(this, CadastroCatalogoActivity.class));
    }

    // ── Filtros por chip (Todos / Produtos / Serviços) ────────────────

    private void configurarFiltros() {
        chipGroupTipo.setOnCheckedChangeListener((group, checkedId) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        int id = chipGroupTipo.getCheckedChipId();
        listaFiltrada.clear();

        for (ItemVendaModel item : listaTotal) {
            boolean isProd = item.getTipo() == ItemVendaModel.TIPO_PRODUTO;
            boolean isServ = item.getTipo() == ItemVendaModel.TIPO_SERVICO;

            if      (id == R.id.chipProdutos && isProd)               listaFiltrada.add(item);
            else if (id == R.id.chipServicos && isServ)               listaFiltrada.add(item);
            else if (id == View.NO_ID        || id == R.id.chipTodos) listaFiltrada.add(item);
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ── Utilitários ───────────────────────────────────────────────────

    public void retornarParaVendasCadastros(View view) {
        finish();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }
}