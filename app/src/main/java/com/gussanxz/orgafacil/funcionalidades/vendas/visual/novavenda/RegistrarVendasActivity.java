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
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.ProdutoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.ServicoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ServicoModel;

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
    private LinearLayout layoutResumoSacola;
    private ImageButton btnHistoricoVendas;
    private Categoria categoriaAtiva = null;

    private final Map<String, ItemSacolaVendaModel> sacolaMap = new LinkedHashMap<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final ProdutoRepository produtoRepository = new ProdutoRepository();
    private final ServicoRepository servicoRepository = new ServicoRepository();
    private ListenerRegistration listenerProdutos;
    private ListenerRegistration listenerServicos;
    private final CategoriaCatalogoRepository categoriaRepository = new CategoriaCatalogoRepository();
    private ListenerRegistration listenerCategorias;


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
        configurarAcoesHeader();
        configurarResumoSacola();
        atualizarResumoSacola();
    }

    private void inicializarComponentes() {
        rvCategorias = findViewById(R.id.rvCategorias);
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
        layoutResumoSacola = findViewById(R.id.layoutResumoSacola);
        txtSacolaQuantidade = findViewById(R.id.txtSacolaQuantidade);
        txtSacolaTitulo = findViewById(R.id.txtSacolaTitulo);
        txtSacolaSubtotal = findViewById(R.id.txtSacolaSubtotal);
        txtCobrarTotal = findViewById(R.id.txtCobrarTotal);
        btnCobrar = findViewById(R.id.btnCobrar);
        btnHistoricoVendas = findViewById(R.id.btnHistoricoVendas);
    }

    private void configurarRvCategorias() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCategorias.setLayoutManager(layoutManager);

        carregarCategorias();

        adapterFiltro = new AdapterFiltroCategoriasNovaVenda(listaCategorias, this,
                new AdapterFiltroCategoriasNovaVenda.OnCategoriaSelectedListener() {
                    @Override
                    public void onCategoriaSelected(Categoria categoria, int position) {
                        filtrarProdutosPorCategoria(categoria);
                    }
                });

        rvCategorias.setAdapter(adapterFiltro);
    }

    private void configurarRvProdutos() {
        carregarProdutosEServicos();

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

            abrirResumoFechamentoVenda();
        });
    }
    private void abrirResumoFechamentoVenda() {
        Intent intent = new Intent(this, FechamentoVendaActivity.class);
        intent.putExtra("itensSacola", new ArrayList<>(sacolaMap.values()));
        intent.putExtra("quantidadeTotal", getQuantidadeTotalSacola());
        intent.putExtra("valorTotal", getValorTotalSacola());
        startActivity(intent);
    }
    private void carregarCategorias() {
        listenerCategorias = categoriaRepository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategorias.clear();

                // "Todos" é sempre o primeiro item fixo
                Categoria todos = new Categoria();
                todos.setId("todos");
                todos.setNome("Todos");
                todos.setAtiva(true);
                listaCategorias.add(todos);

                listaCategorias.addAll(lista);

                if (adapterFiltro != null) {
                    adapterFiltro.notifyDataSetChanged();
                }
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this,
                        "Erro ao carregar categorias: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarProdutosEServicos() {
        listenerProdutos = produtoRepository.listarTempoReal(new ProdutoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<ProdutoModel> lista) {
                // Remove os produtos antigos e insere os novos
                listaCompletaProdutos.removeIf(i -> i.getTipo() == ItemVendaModel.TIPO_PRODUTO);
                listaCompletaProdutos.addAll(lista);
                aplicarFiltroAtual();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this, "Erro ao carregar produtos: " + erro, Toast.LENGTH_SHORT).show();
            }
        });

        listenerServicos = servicoRepository.listarTempoReal(new ServicoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<ServicoModel> lista) {
                listaCompletaProdutos.removeIf(i -> i.getTipo() == ItemVendaModel.TIPO_SERVICO);
                listaCompletaProdutos.addAll(lista);
                aplicarFiltroAtual();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(RegistrarVendasActivity.this, "Erro ao carregar serviços: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarFiltroAtual() {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            // Filtro 1: apenas ativos
            boolean ativo = (item instanceof ProdutoModel && ((ProdutoModel) item).isStatusAtivo())
                    || (item instanceof ServicoModel && ((ServicoModel) item).isStatusAtivo());
            if (!ativo) continue;

            // Filtro 2: categoria (null ou "todos" = sem filtro)
            if (categoriaAtiva != null && !"todos".equals(categoriaAtiva.getId())) {
                String catId = (item instanceof ProdutoModel)
                        ? ((ProdutoModel) item).getCategoriaId()
                        : (item instanceof ServicoModel)
                        ? ((ServicoModel) item).getCategoriaId()
                        : null;
                if (!categoriaAtiva.getId().equals(catId)) continue;
            }

            listaFiltradaProdutos.add(item);
        }

        if (adapterProdutos != null) {
            adapterProdutos.atualizarLista(listaFiltradaProdutos);
        }
    }

    private void filtrarProdutosPorCategoria(Categoria categoria) {
        categoriaAtiva = categoria;
        aplicarFiltroAtual();
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
        if (layoutResumoSacola != null) {
            boolean possuiItens = quantidadeTotal > 0;
            layoutResumoSacola.setAlpha(possuiItens ? 1f : 0.75f);
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

    private void configurarAcoesHeader() {
        if (btnHistoricoVendas != null) {
            btnHistoricoVendas.setOnClickListener(v -> {
                Intent intent = new Intent(RegistrarVendasActivity.this, com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HistoricoVendasActivity.class);
                startActivity(intent);
            });
        }
    }

    private void configurarResumoSacola() {
        if (layoutResumoSacola == null) return;

        layoutResumoSacola.setOnClickListener(v -> {
            if (sacolaMap.isEmpty()) {
                Toast.makeText(
                        RegistrarVendasActivity.this,
                        "A sacola está vazia.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            abrirBottomSheetSacola();
        });
    }
    private void abrirBottomSheetSacola() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sacola_nova_venda, null);
        dialog.setContentView(view);

        RecyclerView rvItensSacola = view.findViewById(R.id.rvItensSacola);
        TextView txtQtdItensSacola = view.findViewById(R.id.txtQtdItensSacola);
        TextView txtTotalSacolaBottom = view.findViewById(R.id.txtTotalSacolaBottom);
        TextView txtEstadoVazioSacola = view.findViewById(R.id.txtEstadoVazioSacola);
        ImageButton btnFecharSacola = view.findViewById(R.id.btnFecharSacola);

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
                            atualizarBottomSheetSacola(
                                    adapterSacolaRef[0],
                                    rvItensSacola,
                                    txtQtdItensSacola,
                                    txtTotalSacolaBottom,
                                    txtEstadoVazioSacola
                            );
                        }
                    }

                    @Override
                    public void onSubtrair(ItemSacolaVendaModel item) {
                        ItemSacolaVendaModel itemMap = sacolaMap.get(item.getChave());
                        if (itemMap != null) {
                            itemMap.decrementarQuantidade();

                            if (itemMap.getQuantidade() <= 0) {
                                sacolaMap.remove(item.getChave());
                            }

                            atualizarBottomSheetSacola(
                                    adapterSacolaRef[0],
                                    rvItensSacola,
                                    txtQtdItensSacola,
                                    txtTotalSacolaBottom,
                                    txtEstadoVazioSacola
                            );
                        }
                    }

                    @Override
                    public void onRemover(ItemSacolaVendaModel item) {
                        sacolaMap.remove(item.getChave());

                        atualizarBottomSheetSacola(
                                adapterSacolaRef[0],
                                rvItensSacola,
                                txtQtdItensSacola,
                                txtTotalSacolaBottom,
                                txtEstadoVazioSacola
                        );

                        if (sacolaMap.isEmpty()) {
                            dialog.dismiss();
                        }
                    }
                }
        );

        rvItensSacola.setAdapter(adapterSacolaRef[0]);

        if (btnFecharSacola != null) {
            btnFecharSacola.setOnClickListener(v -> dialog.dismiss());
        }

        atualizarBottomSheetSacola(
                adapterSacolaRef[0],
                rvItensSacola,
                txtQtdItensSacola,
                txtTotalSacolaBottom,
                txtEstadoVazioSacola
        );

        dialog.show();
    }

    private void atualizarBottomSheetSacola(
            AdapterSacolaNovaVenda adapterSacola,
            RecyclerView rvItensSacola,
            TextView txtQtdItensSacola,
            TextView txtTotalSacolaBottom,
            TextView txtEstadoVazioSacola
    ) {
        List<ItemSacolaVendaModel> itens = getItensSacolaEmLista();

        adapterSacola.atualizarLista(itens);

        if (txtQtdItensSacola != null) {
            int quantidade = getQuantidadeTotalSacola();
            txtQtdItensSacola.setText(
                    quantidade + (quantidade == 1 ? " item" : " itens")
            );
        }

        if (txtTotalSacolaBottom != null) {
            txtTotalSacolaBottom.setText(formatadorMoeda.format(getValorTotalSacola()));
        }

        if (txtEstadoVazioSacola != null) {
            boolean vazio = itens.isEmpty();
            txtEstadoVazioSacola.setVisibility(vazio ? View.VISIBLE : View.GONE);
            rvItensSacola.setVisibility(vazio ? View.GONE : View.VISIBLE);
        }

        atualizarResumoSacola();
    }

    private List<ItemSacolaVendaModel> getItensSacolaEmLista() {
        return new ArrayList<>(sacolaMap.values());
    }

    //Evitar memory leak
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerCategorias != null) listenerCategorias.remove();
        if (listenerProdutos != null) listenerProdutos.remove();
        if (listenerServicos != null) listenerServicos.remove();
    }
    private void filtrarPorTexto(String texto) {
        listaFiltradaProdutos.clear();

        for (ItemVendaModel item : listaCompletaProdutos) {
            boolean ativo = (item instanceof ProdutoModel && ((ProdutoModel) item).isStatusAtivo())
                    || (item instanceof ServicoModel && ((ServicoModel) item).isStatusAtivo());
            if (!ativo) continue;

            if (texto.isEmpty() || item.getNome().toLowerCase().contains(texto.toLowerCase())) {
                listaFiltradaProdutos.add(item);
            }
        }

        if (adapterProdutos != null) {
            adapterProdutos.atualizarLista(listaFiltradaProdutos);
        }
    }
}