package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    /** ID do caixa aberto passado por quem iniciou esta Activity. */
    public static final String EXTRA_CAIXA_ID = "caixaId";

    private RecyclerView rvCategorias;
    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private final List<Categoria> listaCategorias = new ArrayList<>();
    private List<Categoria> listaCategoriasBrutas = new ArrayList<>();

    private RecyclerView rvGradeProdutos;
    private AdapterFiltroPorPSNovaVenda adapterProdutos;
    private List<ItemVendaModel> listaCompletaProdutos = new ArrayList<>();
    private List<ItemVendaModel> listaFiltradaProdutos = new ArrayList<>();

    private EditText etBuscarProduto;

    private TextView txtSacolaQuantidade;
    private TextView txtSacolaTitulo;
    private TextView txtSacolaSubtotal;
    private LinearLayout layoutResumoSacola;
    private ImageButton btnHistoricoVendas;
    private ImageView imgIconeSacolaResumo;
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
    private AdapterGradeCategoriasNovaVenda adapterGridCategorias;
    private String vendaIdEdicao = null;
    /** ID do caixa aberto recebido de ResumoVendasActivity. */
    private String caixaId = null;

    // Extras de edição retroativa — preservados para repassar ao FechamentoVendaActivity
    private long   dataHoraOriginalEdicao       = 0L;
    private String formaPagamentoOriginalEdicao = null;
    private int    numeroVendaEdicao            = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.layout_nova_venda);

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
        configurarAcoesHeader();
        configurarResumoSacola();
        atualizarResumoSacola();
        restaurarSacolaSeEdicao();
    }

    private void inicializarComponentes() {
        rvCategorias    = findViewById(R.id.rvCategorias);
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
        etBuscarProduto = findViewById(R.id.etBuscarProduto);
        imgIconeSacolaResumo = findViewById(R.id.imgIconeSacolaResumo);

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
        btnHistoricoVendas  = findViewById(R.id.btnHistoricoVendas);
        btnAlternarModo     = findViewById(R.id.btnAlternarModoExibicao);
        rvGridCategorias    = findViewById(R.id.rvGridCategorias);
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
                listaCategoriasBrutas.clear();
                listaCategoriasBrutas.addAll(lista);
                atualizarListaCategorias();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this,
                        "Erro ao carregar categorias: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Categoria> filtrarCategoriasSemItens(List<Categoria> categorias) {
        List<Categoria> comItens = new ArrayList<>();
        for (Categoria categoria : categorias) {
            for (ItemVendaModel item : listaCompletaProdutos) {
                CatalogoModel c = (CatalogoModel) item;
                if (c.isStatusAtivo() && categoria.getId().equals(c.getCategoriaId())) {
                    comItens.add(categoria);
                    break;
                }
            }
        }
        return comItens;
    }

    private void configurarGridCategorias() {
        if (rvGridCategorias == null) return;
        rvGridCategorias.setLayoutManager(new GridLayoutManager(this, 3));
        rvGridCategorias.setNestedScrollingEnabled(false);
        adapterGridCategorias = new AdapterGradeCategoriasNovaVenda(
                listaCategorias,
                categoria -> {
                    if ("todos".equals(categoria.getId())) {
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
        btnAlternarModo.setImageResource(R.drawable.ic_grid_24);
        btnAlternarModo.setContentDescription("Ver categorias");
    }

    private void entrarEmModoCategorias() {
        modoCategorias = true;
        categoriaAtiva = null;
        rvGradeProdutos.setVisibility(View.GONE);
        rvGridCategorias.setVisibility(View.VISIBLE);
        btnAlternarModo.setImageResource(R.drawable.ic_list_24);
        btnAlternarModo.setContentDescription("Ver produtos e serviços");
    }

    private void aplicarModoCategorias() {
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

        adapterProdutos = new AdapterFiltroPorPSNovaVenda(listaFiltradaProdutos, new AdapterFiltroPorPSNovaVenda.OnItemClickListener() {
            @Override
            public void onItemClick(ItemVendaModel item) {
                adicionarItemNaSacola(item); // Toque normal: adiciona 1
            }

            @Override
            public void onLongClick(ItemVendaModel item) {
                abrirModalQuantidadeRapida(item); // Toque longo: abre pop-up
            }
        });

        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void carregarProdutosEServicos() {
        listenerCatalogo = catalogoRepository.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<CatalogoModel> lista) {
                listaCompletaProdutos.clear();
                listaCompletaProdutos.addAll(lista);
                atualizarListaCategorias();
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

    private void filtrarProdutosVisiveis() {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            CatalogoModel c = (CatalogoModel) item;
            if (!c.isStatusAtivo()) continue;
            if (categoriaAtiva != null && !"todos".equals(categoriaAtiva.getId())) {
                if (!categoriaAtiva.getId().equals(c.getCategoriaId())) continue;
            }
            listaFiltradaProdutos.add(item);
        }

        if (adapterProdutos != null) adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }

    private void filtrarProdutosPorCategoria(Categoria categoria) {
        if ("todos".equals(categoria.getId())) {
            categoriaAtiva = null;
        } else {
            categoriaAtiva = categoria;
        }
        entrarEmModoPS();
    }

    private void filtrarPorTexto(String texto) {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            CatalogoModel c = (CatalogoModel) item;
            if (!c.isStatusAtivo()) continue;
            if (categoriaAtiva != null && !categoriaAtiva.getId().equals(c.getCategoriaId())) continue;
            if (texto.isEmpty() || c.getNome().toLowerCase().contains(texto.toLowerCase())) {
                listaFiltradaProdutos.add(item);
            }
        }

        if (adapterProdutos != null) adapterProdutos.atualizarLista(listaFiltradaProdutos);
    }

    private void atualizarListaCategorias() {
        listaCategorias.clear();

        Categoria todos = new Categoria();
        todos.setId("todos");
        todos.setNome("Todos");
        todos.setAtiva(true);
        listaCategorias.add(todos);
        listaCategorias.addAll(filtrarCategoriasSemItens(listaCategoriasBrutas));

        if (adapterFiltro         != null) adapterFiltro.notifyDataSetChanged();
        if (adapterGridCategorias != null) adapterGridCategorias.notifyDataSetChanged();
    }

    // ── Sacola ────────────────────────────────────────────────────────

    private void adicionarItemNaSacola(ItemVendaModel item) {
        adicionarItemNaSacola(item, 1);
    }

    // Sobrecarga para aceitar múltiplas quantidades de uma vez
    private void adicionarItemNaSacola(ItemVendaModel item, int quantidadeDesejada) {
        String chave = ItemSacolaVendaModel.gerarChave(item);
        ItemSacolaVendaModel itemSacola = sacolaMap.get(chave);

        if (itemSacola == null) {
            ItemSacolaVendaModel novoItem = new ItemSacolaVendaModel(item);
            // Incrementa o restante (já começa com 1 no construtor)
            for (int i = 1; i < quantidadeDesejada; i++) {
                novoItem.incrementarQuantidade();
            }
            sacolaMap.put(chave, novoItem);
        } else {
            for (int i = 0; i < quantidadeDesejada; i++) {
                itemSacola.incrementarQuantidade();
            }
        }

        // Vibração tátil de confirmação (Virtual Key)
        if (rvGradeProdutos != null) {
            rvGradeProdutos.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }

        atualizarResumoSacola();

        if (quantidadeDesejada > 1) {
            Toast.makeText(this, quantidadeDesejada + " itens adicionados", Toast.LENGTH_SHORT).show();
        }
    }


    private void atualizarResumoSacola() {
        int    quantidadeTotal = getQuantidadeTotalSacola();
        double valorTotal      = getValorTotalSacola();

        if (txtSacolaQuantidade != null)
            txtSacolaQuantidade.setText(String.valueOf(quantidadeTotal));

        if (txtSacolaTitulo != null) {
            if (quantidadeTotal == 0) {
                txtSacolaTitulo.setText("Sacola vazia");
            } else if (quantidadeTotal == 1) {
                txtSacolaTitulo.setText("1 item na sacola");
            } else {
                txtSacolaTitulo.setText(quantidadeTotal + " itens na sacola");
            }
        }

        if (txtSacolaSubtotal != null) {
            if (quantidadeTotal == 0) {
                txtSacolaSubtotal.setText("Toque para ver os itens");
            } else {
                txtSacolaSubtotal.setText(formatadorMoeda.format(valorTotal));
            }
        }

        if (quantidadeTotal == 0) {
            // Estado Vazio: Ícone cinza e texto mais apagado
            if (imgIconeSacolaResumo != null) {
                imgIconeSacolaResumo.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"));
            }
            if (txtSacolaTitulo != null) {
                txtSacolaTitulo.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
        } else {
            // Estado com itens: Ícone e título com a cor primária do app
            if (imgIconeSacolaResumo != null) {
                imgIconeSacolaResumo.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
            }
            if (txtSacolaTitulo != null) {
                txtSacolaTitulo.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
            }
        }

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

    private void abrirResumoFechamentoVenda() {
        Intent intent = new Intent(this, FechamentoVendaActivity.class);
        intent.putExtra("itensSacola",     new ArrayList<>(sacolaMap.values()));
        intent.putExtra("quantidadeTotal", getQuantidadeTotalSacola());
        intent.putExtra("valorTotal",      getValorTotalSacola());
        if (caixaId != null)
            intent.putExtra(FechamentoVendaActivity.EXTRA_CAIXA_ID, caixaId);
        if (vendaIdEdicao != null) {
            intent.putExtra("vendaId", vendaIdEdicao);
            if (dataHoraOriginalEdicao > 0)
                intent.putExtra("dataHoraOriginal", dataHoraOriginalEdicao);
            if (formaPagamentoOriginalEdicao != null)
                intent.putExtra("formaPagamentoOriginal", formaPagamentoOriginalEdicao);
            if (numeroVendaEdicao > 0)
                intent.putExtra("numeroVenda", numeroVendaEdicao);
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
        BottomSheetDialog dialog = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sacola_nova_venda, null);
        dialog.setContentView(view);

        // Expande o bottom sheet imediatamente (comportamento de modal)
        com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                        (View) view.getParent());
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        RecyclerView rvItensSacola  = view.findViewById(R.id.rvItensSacola);
        TextView     txtQtdItens    = view.findViewById(R.id.txtQtdItensSacola);
        TextView     txtTotalBottom = view.findViewById(R.id.txtTotalSacolaBottom);
        TextView     txtEstadoVazio = view.findViewById(R.id.txtEstadoVazioSacola);
        ImageButton  btnFechar      = view.findViewById(R.id.btnFecharSacola);
        LinearLayout btnCobrarModal = view.findViewById(R.id.btnCobrar);
        TextView     txtCobrarTotal = view.findViewById(R.id.txtCobrarTotal);

        rvItensSacola.setLayoutManager(new LinearLayoutManager(this));

        final AdapterSacolaNovaVenda[] adapterRef = new AdapterSacolaNovaVenda[1];
        adapterRef[0] = new AdapterSacolaNovaVenda(
                getItensSacolaEmLista(),
                new AdapterSacolaNovaVenda.OnSacolaActionListener() {
                    @Override
                    public void onSomar(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        ItemSacolaVendaModel itemMap = sacolaMap.get(item.getChave());
                        if (itemMap != null) {
                            itemMap.incrementarQuantidade();
                            sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                                    txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                        }
                    }

                    @Override
                    public void onSubtrair(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        ItemSacolaVendaModel itemMap = sacolaMap.get(item.getChave());
                        if (itemMap != null) {
                            itemMap.decrementarQuantidade();
                            if (itemMap.getQuantidade() <= 0) sacolaMap.remove(item.getChave());
                            sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                                    txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                        }
                    }

                    @Override
                    public void onRemover(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
                        sacolaMap.remove(item.getChave());
                        sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                                txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                        if (sacolaMap.isEmpty()) dialog.dismiss();
                    }
                }
        );

        rvItensSacola.setAdapter(adapterRef[0]);

        if (btnFechar != null)
            btnFechar.setOnClickListener(v -> dialog.dismiss());

        // Botão COBRAR — exclusivo dentro do modal da sacola
        if (btnCobrarModal != null) {
            btnCobrarModal.setOnClickListener(v -> {
                if (sacolaMap.isEmpty()) {
                    Toast.makeText(this, "Adicione ao menos um item para continuar.", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                abrirResumoFechamentoVenda();
            });
        }

        sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
        dialog.show();
    }

    // ── Quick Add (Clique Longo) ──────────────────────────────────────────

    private void abrirModalQuantidadeRapida(ItemVendaModel item) {
        // Vibração específica de clique longo
        if (rvGradeProdutos != null) {
            rvGradeProdutos.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Adicionar quantidade");
        CatalogoModel catalogoItem = (CatalogoModel) item;
        builder.setMessage("Digite a quantidade desejada para:\n" + catalogoItem.getNome());

        // Criando o campo de texto dinamicamente com margens
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setGravity(android.view.Gravity.CENTER);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(48, 0, 48, 0); // Margem lateral para não colar nas bordas
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String valor = input.getText().toString();
            if (!valor.isEmpty()) {
                int qtd = Integer.parseInt(valor);
                if (qtd > 0) {
                    adicionarItemNaSacola(item, qtd); // Chamaremos a nova versão do método
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        // Mostra o teclado automaticamente
        android.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        dialog.show();
    }

    private void sincronizarBottomSheet(
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio) {

        List<ItemSacolaVendaModel> itens = getItensSacolaEmLista();
        adapter.atualizarLista(itens);

        int    quantidade = getQuantidadeTotalSacola();
        double total      = getValorTotalSacola();

        if (txtQtdItens != null)
            txtQtdItens.setText(quantidade + (quantidade == 1 ? " item" : " itens"));

        if (txtTotalBottom != null)
            txtTotalBottom.setText(formatadorMoeda.format(total));

        if (txtCobrarTotal != null)
            txtCobrarTotal.setText(formatadorMoeda.format(total));

        if (txtEstadoVazio != null) {
            boolean vazio = itens.isEmpty();
            txtEstadoVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
            rvItensSacola.setVisibility(vazio ? View.GONE : View.VISIBLE);
        }

        atualizarResumoSacola();
    }

    // ── Edição de venda ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void restaurarSacolaSeEdicao() {
        caixaId                      = getIntent().getStringExtra(EXTRA_CAIXA_ID);
        vendaIdEdicao                = getIntent().getStringExtra("vendaId");
        dataHoraOriginalEdicao       = getIntent().getLongExtra("dataHoraOriginal", 0L);
        formaPagamentoOriginalEdicao = getIntent().getStringExtra("formaPagamentoOriginal");
        numeroVendaEdicao            = getIntent().getIntExtra("numeroVenda", 0);

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