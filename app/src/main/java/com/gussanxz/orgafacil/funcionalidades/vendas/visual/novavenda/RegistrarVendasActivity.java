package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private LinearLayout layoutResumoSacola;
    private ImageButton btnHistoricoVendas;
    private Categoria categoriaAtiva = null;

    private final Map<String, ItemSacolaVendaModel> sacolaMap = new LinkedHashMap<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ── Repositório unificado ─────────────────────────────────────────
    private final CatalogoRepository catalogoRepository = new CatalogoRepository();
    private ListenerRegistration listenerCatalogo;

    private final CategoriaCatalogoRepository categoriaRepository = new CategoriaCatalogoRepository();
    private ListenerRegistration listenerCategorias;
    private boolean modoCategorias = true;
    private ImageButton btnAlternarModo;
    private RecyclerView rvGridCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterGridCategorias;
    private String vendaIdEdicao = null;

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
        configurarGridCategorias();
        configurarBotaoAlternarModo();
        aplicarModoCategorias();
        configurarRvProdutos();
        configurarBotaoCobrar();
        configurarAcoesHeader();
        configurarResumoSacola();
        atualizarResumoSacola();
        restaurarSacolaSeEdicao();
    }

    private void inicializarComponentes() {
        rvCategorias    = findViewById(R.id.rvCategorias);
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
        etBuscarProduto = findViewById(R.id.etBuscarProduto);

        if (etBuscarProduto != null) {
            etBuscarProduto.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarPorTexto(s.toString().trim());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        layoutResumoSacola  = findViewById(R.id.layoutResumoSacola);
        txtSacolaQuantidade = findViewById(R.id.txtSacolaQuantidade);
        txtSacolaTitulo     = findViewById(R.id.txtSacolaTitulo);
        txtSacolaSubtotal   = findViewById(R.id.txtSacolaSubtotal);
        txtCobrarTotal      = findViewById(R.id.txtCobrarTotal);
        btnCobrar           = findViewById(R.id.btnCobrar);
        btnHistoricoVendas  = findViewById(R.id.btnHistoricoVendas);
        btnAlternarModo   = findViewById(R.id.btnAlternarModoExibicao);
        rvGridCategorias  = findViewById(R.id.rvGridCategorias);
    }

    // ── Categorias ────────────────────────────────────────────────────

    private void configurarRvCategorias() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCategorias.setLayoutManager(layoutManager);

        carregarCategorias();

        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(listaCategorias, this,
                (categoria, position) -> filtrarProdutosPorCategoria(categoria));

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void carregarCategorias() {
        listenerCategorias = categoriaRepository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategorias.clear();

                Categoria todos = new Categoria();
                todos.setId("todos");
                todos.setNome("Todos");
                todos.setAtiva(true);
                listaCategorias.add(todos);
                listaCategorias.addAll(lista);

                if (adapterFiltro != null) adapterFiltro.notifyDataSetChanged();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this,
                        "Erro ao carregar categorias: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarGridCategorias() {
        if (rvGridCategorias == null) return;
        rvGridCategorias.setLayoutManager(new GridLayoutManager(this, 3));
        rvGridCategorias.setNestedScrollingEnabled(false);
        // Reutiliza a mesma lista de categorias já carregada
        adapterGridCategorias = new AdapterFiltroCategoriasNovaVenda(
                listaCategorias, this,
                (categoria, position) -> {
                    if ("todos".equals(categoria.getId())) {
                        // "Todos" no grid de categorias vai para modo PS sem filtro
                        categoriaAtiva = null;
                    } else {
                        categoriaAtiva = categoria;
                    }
                    entrarEmModoPS();
                }
        );
        rvGridCategorias.setAdapter(adapterGridCategorias);
    }

    private void configurarBotaoAlternarModo() {
        if (btnAlternarModo == null) return;
        btnAlternarModo.setOnClickListener(v -> {
            if (modoCategorias) entrarEmModoPS();
            else entrarEmModoCategorias();
        });
    }

    private void entrarEmModoPS() {
        modoCategorias = false;
        filtrarProdutosVisiveis();
        rvGridCategorias.setVisibility(View.GONE);
        rvGradeProdutos.setVisibility(View.VISIBLE);
        // Ícone: quando está em PS, mostra ícone de categorias para poder voltar
        btnAlternarModo.setImageResource(R.drawable.ic_grid_24); // grid de categorias
        btnAlternarModo.setContentDescription("Ver categorias");
    }

    private void entrarEmModoCategorias() {
        modoCategorias = true;
        categoriaAtiva = null;
        rvGradeProdutos.setVisibility(View.GONE);
        rvGridCategorias.setVisibility(View.VISIBLE);
        // Ícone: quando está em categorias, mostra ícone de lista de itens
        btnAlternarModo.setImageResource(R.drawable.ic_list_24);
        btnAlternarModo.setContentDescription("Ver produtos e serviços");
    }

    private void aplicarModoCategorias() {
        // Estado inicial — categorias visíveis, grade de produtos oculta
        rvGradeProdutos.setVisibility(View.GONE);
        rvGridCategorias.setVisibility(View.VISIBLE);
        btnAlternarModo.setImageResource(R.drawable.ic_list_24);
    }

    // ── Catálogo (produtos + serviços unificados) ─────────────────────

    private void configurarRvProdutos() {
        carregarProdutosEServicos();

        GridLayoutManager gridManager = new GridLayoutManager(this, 3);
        rvGradeProdutos.setLayoutManager(gridManager);
        rvGradeProdutos.setNestedScrollingEnabled(false);

        adapterProdutos = new AdapterFiltroPorPSNovaVenda(listaFiltradaProdutos,
                item -> adicionarItemNaSacola(item));

        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void carregarProdutosEServicos() {
        listenerCatalogo = catalogoRepository.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<CatalogoModel> lista) {
                listaCompletaProdutos.clear();
                listaCompletaProdutos.addAll(lista);
                filtrarProdutosVisiveis();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this,
                        "Erro ao carregar catálogo: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Filtros ───────────────────────────────────────────────────────

    /** Filtra por categoria ativa + status ativo. Renomeado de aplicarFiltroAtual(). */
    private void filtrarProdutosVisiveis() {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            CatalogoModel c = (CatalogoModel) item;

            // Apenas ativos
            if (!c.isStatusAtivo()) continue;

            // Filtro de categoria
            if (categoriaAtiva != null && !"todos".equals(categoriaAtiva.getId())) {
                if (!categoriaAtiva.getId().equals(c.getCategoriaId())) continue;
            }

            listaFiltradaProdutos.add(item);
        }

        if (adapterProdutos != null) adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }

    private void filtrarProdutosPorCategoria(Categoria categoria) {
        categoriaAtiva = categoria;
        filtrarProdutosVisiveis();
    }

    private void filtrarPorTexto(String texto) {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            CatalogoModel c = (CatalogoModel) item;
            if (!c.isStatusAtivo()) continue;
            if (texto.isEmpty() || c.getNome().toLowerCase().contains(texto.toLowerCase())) {
                listaFiltradaProdutos.add(item);
            }
        }

        if (adapterProdutos != null) adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }

    // ── Sacola ────────────────────────────────────────────────────────

    private void adicionarItemNaSacola(ItemVendaModel item) {
        String chave = ItemSacolaVendaModel.gerarChave(item);
        ItemSacolaVendaModel itemSacola = sacolaMap.get(chave);

        if (itemSacola == null) {
            sacolaMap.put(chave, new ItemSacolaVendaModel(item));
        } else {
            itemSacola.incrementarQuantidade();
        }

        atualizarResumoSacola();
        Toast.makeText(this, item.getNome() + " adicionado à sacola", Toast.LENGTH_SHORT).show();
    }

    private void atualizarResumoSacola() {
        int    quantidadeTotal = getQuantidadeTotalSacola();
        double valorTotal      = getValorTotalSacola();

        if (txtSacolaQuantidade != null)
            txtSacolaQuantidade.setText(String.valueOf(quantidadeTotal));

        if (txtSacolaTitulo != null)
            txtSacolaTitulo.setText(quantidadeTotal == 0
                    ? "Sacola vazia"
                    : quantidadeTotal + (quantidadeTotal == 1 ? " item na sacola" : " itens na sacola"));

        if (txtSacolaSubtotal != null)
            txtSacolaSubtotal.setText(formatadorMoeda.format(valorTotal));

        if (txtCobrarTotal != null)
            txtCobrarTotal.setText(formatadorMoeda.format(valorTotal));

        if (btnCobrar != null) {
            boolean habilitado = quantidadeTotal > 0;
            btnCobrar.setEnabled(habilitado);
            btnCobrar.setAlpha(habilitado ? 1f : 0.5f);
        }

        if (layoutResumoSacola != null)
            layoutResumoSacola.setAlpha(quantidadeTotal > 0 ? 1f : 0.75f);
    }

    private int getQuantidadeTotalSacola() {
        int total = 0;
        for (ItemSacolaVendaModel item : sacolaMap.values()) total += item.getQuantidade();
        return total;
    }

    private double getValorTotalSacola() {
        double total = 0.0;
        for (ItemSacolaVendaModel item : sacolaMap.values()) total += item.getSubtotal();
        return total;
    }

    private List<ItemSacolaVendaModel> getItensSacolaEmLista() {
        return new ArrayList<>(sacolaMap.values());
    }

    // ── Cobrar / Fechamento ───────────────────────────────────────────

    private void configurarBotaoCobrar() {
        if (btnCobrar == null) return;
        btnCobrar.setOnClickListener(v -> {
            if (sacolaMap.isEmpty()) {
                Toast.makeText(this, "Adicione ao menos um item para continuar.", Toast.LENGTH_SHORT).show();
                return;
            }
            abrirResumoFechamentoVenda();
        });
    }

    private void abrirResumoFechamentoVenda() {
        Intent intent = new Intent(this, FechamentoVendaActivity.class);
        intent.putExtra("itensSacola",      new ArrayList<>(sacolaMap.values()));
        intent.putExtra("quantidadeTotal",  getQuantidadeTotalSacola());
        intent.putExtra("valorTotal",       getValorTotalSacola());
        if (vendaIdEdicao != null) {
            intent.putExtra("vendaId", vendaIdEdicao);
            // Repassa os dados de contexto da edição para o FechamentoVendaActivity
            // ativar o seletor de data retroativa
            if (dataHoraOriginalEdicao > 0)
                intent.putExtra("dataHoraOriginal", dataHoraOriginalEdicao);
            if (formaPagamentoOriginalEdicao != null)
                intent.putExtra("formaPagamentoOriginal", formaPagamentoOriginalEdicao);
        }
        startActivity(intent);
    }

    // ── Header ────────────────────────────────────────────────────────

    private void configurarAcoesHeader() {
        if (btnHistoricoVendas != null) {
            btnHistoricoVendas.setOnClickListener(v ->
                    startActivity(new Intent(this,
                            com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HistoricoVendasActivity.class)));
        }
    }

    // ── Bottom Sheet Sacola ───────────────────────────────────────────

    private void configurarResumoSacola() {
        if (layoutResumoSacola == null) return;
        layoutResumoSacola.setOnClickListener(v -> {
            if (sacolaMap.isEmpty()) {
                Toast.makeText(this, "A sacola está vazia.", Toast.LENGTH_SHORT).show();
                return;
            }
            abrirBottomSheetSacola();
        });
    }

    private void abrirBottomSheetSacola() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sacola_nova_venda, null);
        dialog.setContentView(view);

        RecyclerView rvItensSacola       = view.findViewById(R.id.rvItensSacola);
        TextView txtQtdItensSacola       = view.findViewById(R.id.txtQtdItensSacola);
        TextView txtTotalSacolaBottom    = view.findViewById(R.id.txtTotalSacolaBottom);
        TextView txtEstadoVazioSacola    = view.findViewById(R.id.txtEstadoVazioSacola);
        ImageButton btnFecharSacola      = view.findViewById(R.id.btnFecharSacola);

        rvItensSacola.setLayoutManager(new LinearLayoutManager(this));

        final AdapterSacolaNovaVenda[] adapterSacolaRef = new AdapterSacolaNovaVenda[1];
        adapterSacolaRef[0] = new AdapterSacolaNovaVenda(
                getItensSacolaEmLista(),
                new AdapterSacolaNovaVenda.OnSacolaActionListener() {
                    @Override
                    public void onSomar(ItemSacolaVendaModel item) {
                        ItemSacolaVendaModel itemMap = sacolaMap.get(item.getChave());
                        if (itemMap != null) {
                            itemMap.incrementarQuantidade();
                            atualizarBottomSheetSacola(adapterSacolaRef[0], rvItensSacola,
                                    txtQtdItensSacola, txtTotalSacolaBottom, txtEstadoVazioSacola);
                        }
                    }

                    @Override
                    public void onSubtrair(ItemSacolaVendaModel item) {
                        ItemSacolaVendaModel itemMap = sacolaMap.get(item.getChave());
                        if (itemMap != null) {
                            itemMap.decrementarQuantidade();
                            if (itemMap.getQuantidade() <= 0) sacolaMap.remove(item.getChave());
                            atualizarBottomSheetSacola(adapterSacolaRef[0], rvItensSacola,
                                    txtQtdItensSacola, txtTotalSacolaBottom, txtEstadoVazioSacola);
                        }
                    }

                    @Override
                    public void onRemover(ItemSacolaVendaModel item) {
                        sacolaMap.remove(item.getChave());
                        atualizarBottomSheetSacola(adapterSacolaRef[0], rvItensSacola,
                                txtQtdItensSacola, txtTotalSacolaBottom, txtEstadoVazioSacola);
                        if (sacolaMap.isEmpty()) dialog.dismiss();
                    }
                }
        );

        rvItensSacola.setAdapter(adapterSacolaRef[0]);
        if (btnFecharSacola != null) btnFecharSacola.setOnClickListener(v -> dialog.dismiss());
        atualizarBottomSheetSacola(adapterSacolaRef[0], rvItensSacola,
                txtQtdItensSacola, txtTotalSacolaBottom, txtEstadoVazioSacola);
        dialog.show();
    }

    private void atualizarBottomSheetSacola(
            AdapterSacolaNovaVenda adapterSacola,
            RecyclerView rvItensSacola,
            TextView txtQtdItensSacola,
            TextView txtTotalSacolaBottom,
            TextView txtEstadoVazioSacola) {

        List<ItemSacolaVendaModel> itens = getItensSacolaEmLista();
        adapterSacola.atualizarLista(itens);

        if (txtQtdItensSacola != null) {
            int quantidade = getQuantidadeTotalSacola();
            txtQtdItensSacola.setText(quantidade + (quantidade == 1 ? " item" : " itens"));
        }

        if (txtTotalSacolaBottom != null)
            txtTotalSacolaBottom.setText(formatadorMoeda.format(getValorTotalSacola()));

        if (txtEstadoVazioSacola != null) {
            boolean vazio = itens.isEmpty();
            txtEstadoVazioSacola.setVisibility(vazio ? View.VISIBLE : View.GONE);
            rvItensSacola.setVisibility(vazio ? View.GONE : View.VISIBLE);
        }

        atualizarResumoSacola();
    }

    // ── Edição de venda ───────────────────────────────────────────────

    // Extras de edição retroativa — preservados para repassar ao FechamentoVendaActivity
    private long   dataHoraOriginalEdicao        = 0L;
    private String formaPagamentoOriginalEdicao  = null;

    @SuppressWarnings("unchecked")
    private void restaurarSacolaSeEdicao() {
        vendaIdEdicao               = getIntent().getStringExtra("vendaId");
        dataHoraOriginalEdicao      = getIntent().getLongExtra("dataHoraOriginal", 0L);
        formaPagamentoOriginalEdicao= getIntent().getStringExtra("formaPagamentoOriginal");

        ArrayList<ItemSacolaVendaModel> itensRecebidos =
                (ArrayList<ItemSacolaVendaModel>) getIntent().getSerializableExtra("itensSacola");

        if (itensRecebidos != null && !itensRecebidos.isEmpty()) {
            sacolaMap.clear();
            for (ItemSacolaVendaModel item : itensRecebidos) sacolaMap.put(item.getChave(), item);
            atualizarResumoSacola();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getSerializableExtra("itensSacola") == null) {
            sacolaMap.clear();
            vendaIdEdicao = null;
            atualizarResumoSacola();
        } else {
            restaurarSacolaSeEdicao();
        }
    }

    @Override
    public void onBackPressed() {
        if (!modoCategorias) {
            entrarEmModoCategorias();
        } else {
            super.onBackPressed();
        }
    }

    // ── Ciclo de vida / memory leak ───────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerCategorias != null) listenerCategorias.remove();
        if (listenerCatalogo   != null) listenerCatalogo.remove();
    }
}