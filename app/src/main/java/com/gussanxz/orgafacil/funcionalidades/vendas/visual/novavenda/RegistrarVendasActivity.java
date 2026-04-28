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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HistoricoVendasActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegistrarVendasActivity extends AppCompatActivity {

    public static final String EXTRA_CAIXA_ID = "caixaId";

    private RegistrarVendasViewModel viewModel;

    private RecyclerView rvCategorias;
    private RecyclerView rvGradeProdutos;
    private RecyclerView rvGridCategorias;
    private EditText etBuscarProduto;
    private TextView txtSacolaQuantidade;
    private TextView txtSacolaTitulo;
    private TextView txtSacolaSubtotal;
    private LinearLayout layoutResumoSacola;
    private ImageButton btnHistoricoVendas;
    private ImageView imgIconeSacolaResumo;
    private ImageButton btnAlternarModo;

    private AdapterFiltroCategoriasNovaVenda adapterFiltro;
    private AdapterFiltroPorPSNovaVenda adapterProdutos;
    private AdapterGradeCategoriasNovaVenda adapterGridCategorias;

    private final List<Categoria> listaCategorias = new ArrayList<>();

    // Dados de navegação — não são estado, não vão pro ViewModel
    private String caixaId = null;
    private String nomeCaixa = null;
    private String vendaIdEdicao = null;
    private long dataHoraOriginalEdicao = 0L;
    private String formaPagamentoOriginalEdicao = null;
    private int numeroVendaEdicao = 0;

    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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

        viewModel = new ViewModelProvider(this).get(RegistrarVendasViewModel.class);

        vincularViews();
        configurarAdapters();
        configurarBotoes();
        configurarObservers();
        restaurarDadosIntent();
    }

    private void vincularViews() {
        rvCategorias        = findViewById(R.id.rvCategorias);
        rvGradeProdutos     = findViewById(R.id.rvGradeProdutos);
        rvGridCategorias    = findViewById(R.id.rvGridCategorias);
        etBuscarProduto     = findViewById(R.id.etBuscarProduto);
        imgIconeSacolaResumo = findViewById(R.id.imgIconeSacolaResumo);
        layoutResumoSacola  = findViewById(R.id.layoutResumoSacola);
        txtSacolaQuantidade = findViewById(R.id.txtSacolaQuantidade);
        txtSacolaTitulo     = findViewById(R.id.txtSacolaTitulo);
        txtSacolaSubtotal   = findViewById(R.id.txtSacolaSubtotal);
        btnHistoricoVendas  = findViewById(R.id.btnHistoricoVendas);
        btnAlternarModo     = findViewById(R.id.btnAlternarModoExibicao);
    }

    private void configurarAdapters() {
        // Filtro horizontal de categorias — compartilha referência de lista
        rvCategorias.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(listaCategorias, this,
                (categoria, position) -> viewModel.entrarEmModoPS(categoria));
        rvCategorias.setAdapter(adapterFiltro);

        // Grade de categorias
        if (rvGridCategorias != null) {
            rvGridCategorias.setLayoutManager(new GridLayoutManager(this, 3));
            rvGridCategorias.setNestedScrollingEnabled(false);
            adapterGridCategorias = new AdapterGradeCategoriasNovaVenda(
                    new ArrayList<>(),
                    categoria -> viewModel.entrarEmModoPS(categoria)
            );
            rvGridCategorias.setAdapter(adapterGridCategorias);
        }

        // Grade de produtos e serviços
        rvGradeProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        rvGradeProdutos.setNestedScrollingEnabled(false);
        adapterProdutos = new AdapterFiltroPorPSNovaVenda(new ArrayList<>(), new AdapterFiltroPorPSNovaVenda.OnItemClickListener() {
            @Override public void onItemClick(ItemVendaModel item) {
                if (rvGradeProdutos != null)
                    rvGradeProdutos.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                viewModel.adicionarItem(item);
            }
            @Override public void onLongClick(ItemVendaModel item) {
                abrirModalQuantidadeRapida(item);
            }
        });
        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void configurarBotoes() {
        if (etBuscarProduto != null) {
            etBuscarProduto.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.filtrarPorTexto(s.toString().trim());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnAlternarModo != null) {
            btnAlternarModo.setOnClickListener(v -> {
                if (viewModel.isModoCategorias()) viewModel.entrarEmModoPS();
                else viewModel.entrarEmModoCategorias();
            });
        }

        if (btnHistoricoVendas != null) {
            btnHistoricoVendas.setOnClickListener(v ->
                    startActivity(new Intent(this, HistoricoVendasActivity.class)));
        }

        if (layoutResumoSacola != null) {
            layoutResumoSacola.setOnClickListener(v -> {
                if (viewModel.isSacolaVazia()) {
                    Toast.makeText(this, "A sacola está vazia.", Toast.LENGTH_SHORT).show();
                    return;
                }
                abrirBottomSheetSacola();
            });
        }
    }

    private void configurarObservers() {
        viewModel.produtosFiltrados.observe(this, lista ->
                adapterProdutos.atualizarLista(lista));

        viewModel.categorias.observe(this, lista -> {
            listaCategorias.clear();
            listaCategorias.addAll(lista);
            adapterFiltro.selecionarTodosProdutosInicial();
            adapterFiltro.notifyDataSetChanged();
            if (adapterGridCategorias != null) adapterGridCategorias.atualizarLista(lista);
        });

        viewModel.sacola.observe(this, itens -> atualizarResumoSacolaUI());

        viewModel.modoCategorias.observe(this, modoCategoria -> {
            if (modoCategoria) {
                rvGradeProdutos.setVisibility(View.GONE);
                if (rvGridCategorias != null) rvGridCategorias.setVisibility(View.VISIBLE);
                if (btnAlternarModo != null) {
                    btnAlternarModo.setImageResource(R.drawable.ic_list_24);
                    btnAlternarModo.setContentDescription("Ver produtos e serviços");
                }
            } else {
                if (rvGridCategorias != null) rvGridCategorias.setVisibility(View.GONE);
                rvGradeProdutos.setVisibility(View.VISIBLE);
                if (btnAlternarModo != null) {
                    btnAlternarModo.setImageResource(R.drawable.ic_grid_24);
                    btnAlternarModo.setContentDescription("Ver categorias");
                }
            }
        });

        viewModel.erro.observe(this, erro -> {
            if (erro != null && !erro.isEmpty())
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
        });
    }

    // ── UI da sacola (header) ─────────────────────────────────────────

    private void atualizarResumoSacolaUI() {
        int quantidadeTotal = viewModel.getQuantidadeTotal();
        double valorTotal   = viewModel.getValorTotal();

        if (txtSacolaQuantidade != null)
            txtSacolaQuantidade.setText(String.valueOf(quantidadeTotal));

        if (txtSacolaTitulo != null) {
            if (quantidadeTotal == 0)        txtSacolaTitulo.setText("Sacola vazia");
            else if (quantidadeTotal == 1)   txtSacolaTitulo.setText("1 item na sacola");
            else                             txtSacolaTitulo.setText(quantidadeTotal + " itens na sacola");
        }

        if (txtSacolaSubtotal != null) {
            if (quantidadeTotal == 0) txtSacolaSubtotal.setText("Toque para ver os itens");
            else txtSacolaSubtotal.setText(formatadorMoeda.format(valorTotal));
        }

        if (quantidadeTotal == 0) {
            if (imgIconeSacolaResumo != null)
                imgIconeSacolaResumo.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"));
            if (txtSacolaTitulo != null)
                txtSacolaTitulo.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
        } else {
            if (imgIconeSacolaResumo != null)
                imgIconeSacolaResumo.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
            if (txtSacolaTitulo != null)
                txtSacolaTitulo.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    // ── Navegação para fechamento ─────────────────────────────────────

    private void abrirResumoFechamentoVenda() {
        Intent intent = new Intent(this, FechamentoVendaActivity.class);
        intent.putExtra("itensSacola",     new ArrayList<>(viewModel.getItensSacola()));
        intent.putExtra("quantidadeTotal", viewModel.getQuantidadeTotal());
        intent.putExtra("valorTotal",      viewModel.getValorTotal());
        if (caixaId != null)   intent.putExtra(FechamentoVendaActivity.EXTRA_CAIXA_ID, caixaId);
        if (nomeCaixa != null) intent.putExtra(FechamentoVendaActivity.EXTRA_NOME_CAIXA, nomeCaixa);
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

    // ── Bottom Sheet Sacola ───────────────────────────────────────────

    private void abrirBottomSheetSacola() {
        BottomSheetDialog dialog = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sacola_nova_venda, null);
        dialog.setContentView(view);

        com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from((View) view.getParent());
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
                viewModel.getItensSacola(),
                new AdapterSacolaNovaVenda.OnSacolaActionListener() {
                    @Override public void onSomar(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        viewModel.incrementarItem(item.getChave());
                        sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                                txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                    }
                    @Override public void onSubtrair(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        if (item.getQuantidade() <= 1) {
                            confirmarRemocaoAoZerarItem(item, adapterRef[0], rvItensSacola,
                                    txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog);
                        } else {
                            viewModel.decrementarItem(item.getChave());
                            sincronizarBottomSheet(adapterRef[0], rvItensSacola,
                                    txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                        }
                    }
                    @Override public void onRemover(ItemSacolaVendaModel item) {
                        rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
                        confirmarRemocaoItem(item, adapterRef[0], rvItensSacola,
                                txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog);
                    }
                }
        );

        rvItensSacola.setAdapter(adapterRef[0]);
        configurarSwipeSacola(adapterRef[0], rvItensSacola,
                txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog);

        if (btnFechar != null) btnFechar.setOnClickListener(v -> dialog.dismiss());

        if (btnCobrarModal != null) {
            btnCobrarModal.setOnClickListener(v -> {
                if (viewModel.isSacolaVazia()) {
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

    private void configurarSwipeSacola(
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio,
            BottomSheetDialog dialog) {

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
                return adapter.getItemSacola(viewHolder.getAdapterPosition()) == null
                        ? 0
                        : super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    sincronizarBottomSheet(adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
                    return;
                }

                ItemSacolaVendaModel item = adapter.getItemSacola(position);
                if (item == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                rvItensSacola.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
                confirmarRemocaoItem(item, adapter, rvItensSacola,
                        txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog);
            }
        });
        helper.attachToRecyclerView(rvItensSacola);
    }

    private void confirmarRemocaoAoZerarItem(
            ItemSacolaVendaModel item,
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio,
            BottomSheetDialog dialog) {

        new AlertDialog.Builder(this)
                .setTitle("Remover item da venda?")
                .setMessage("Ao reduzir \"" + item.getNome() + "\" de 1 para 0, o item ser\u00e1 removido da sacola.")
                .setNegativeButton("Cancelar", (d, w) ->
                        sincronizarBottomSheet(adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio))
                .setPositiveButton("Remover", (d, w) ->
                        removerItemDaSacola(item, adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog))
                .show();
    }

    private void confirmarRemocaoItem(
            ItemSacolaVendaModel item,
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio,
            BottomSheetDialog dialog) {

        String quantidade = item.getQuantidade() + (item.getQuantidade() == 1 ? " unidade" : " unidades");
        String valor = formatadorMoeda.format(item.getSubtotal());

        new AlertDialog.Builder(this)
                .setTitle("Excluir item da venda?")
                .setMessage("Ser\u00e3o exclu\u00eddos " + quantidade + " de \"" + item.getNome() + "\", no valor de " + valor + ".")
                .setNegativeButton("Cancelar", (d, w) ->
                        sincronizarBottomSheet(adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio))
                .setPositiveButton("Excluir", (d, w) ->
                        removerItemDaSacola(item, adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio, dialog))
                .show();
    }

    private void removerItemDaSacola(
            ItemSacolaVendaModel item,
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio,
            BottomSheetDialog dialog) {

        viewModel.removerItem(item.getChave());
        sincronizarBottomSheet(adapter, rvItensSacola, txtQtdItens, txtTotalBottom, txtCobrarTotal, txtEstadoVazio);
        if (viewModel.isSacolaVazia()) dialog.dismiss();
    }

    private void sincronizarBottomSheet(
            AdapterSacolaNovaVenda adapter,
            RecyclerView rvItensSacola,
            TextView txtQtdItens,
            TextView txtTotalBottom,
            TextView txtCobrarTotal,
            TextView txtEstadoVazio) {

        List<ItemSacolaVendaModel> itens = viewModel.getItensSacola();
        adapter.atualizarLista(itens);

        int    quantidade = viewModel.getQuantidadeTotal();
        double total      = viewModel.getValorTotal();

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

        atualizarResumoSacolaUI();
    }

    // ── Quick Add (Clique Longo) ──────────────────────────────────────

    private void abrirModalQuantidadeRapida(ItemVendaModel item) {
        if (rvGradeProdutos != null)
            rvGradeProdutos.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Adicionar quantidade");
        CatalogoModel catalogoItem = (CatalogoModel) item;
        builder.setMessage("Digite a quantidade desejada para:\n" + catalogoItem.getNome());

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setGravity(android.view.Gravity.CENTER);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(48, 0, 48, 0);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Adicionar", (dialogInterface, which) -> {
            String valor = input.getText().toString();
            if (!valor.isEmpty()) {
                int qtd = Integer.parseInt(valor);
                if (qtd > 0) {
                    viewModel.adicionarItem(item, qtd);
                    Toast.makeText(this, qtd + " itens adicionados", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialogInterface, which) -> dialogInterface.cancel());

        android.app.AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(d -> {
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        alertDialog.show();
    }

    // ── Intent / edição retroativa ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void restaurarDadosIntent() {
        caixaId                      = getIntent().getStringExtra(EXTRA_CAIXA_ID);
        nomeCaixa                    = getIntent().getStringExtra(FechamentoVendaActivity.EXTRA_NOME_CAIXA);
        vendaIdEdicao                = getIntent().getStringExtra("vendaId");
        dataHoraOriginalEdicao       = getIntent().getLongExtra("dataHoraOriginal", 0L);
        formaPagamentoOriginalEdicao = getIntent().getStringExtra("formaPagamentoOriginal");
        numeroVendaEdicao            = getIntent().getIntExtra("numeroVenda", 0);

        ArrayList<ItemSacolaVendaModel> itensRecebidos =
                (ArrayList<ItemSacolaVendaModel>) getIntent().getSerializableExtra("itensSacola");
        if (itensRecebidos != null && !itensRecebidos.isEmpty()) {
            viewModel.restaurarSacola(itensRecebidos);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ArrayList<ItemSacolaVendaModel> itens =
                (ArrayList<ItemSacolaVendaModel>) intent.getSerializableExtra("itensSacola");
        if (itens == null || itens.isEmpty()) {
            viewModel.limparSacola();
            vendaIdEdicao = null;
        } else {
            restaurarDadosIntent();
        }
    }

    @Override
    public void onBackPressed() {
        if (!viewModel.isModoCategorias()) {
            viewModel.entrarEmModoCategorias();
        } else {
            super.onBackPressed();
        }
    }
}
