package com.gussanxz.orgafacil.funcionalidades.mercado.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.mercado.dados.model.ItemMercadoModel;
import com.gussanxz.orgafacil.funcionalidades.mercado.dados.repository.MercadoRepository;
import com.gussanxz.orgafacil.funcionalidades.mercado.ui.adapter.CategoriaDialogAdapter;
import com.gussanxz.orgafacil.funcionalidades.mercado.ui.adapter.ItemMercadoAdapter;
import com.gussanxz.orgafacil.funcionalidades.mercado.util_helper.PdfListaMercadoGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ResumoListaMercadoActivity extends AppCompatActivity
        implements ItemMercadoAdapter.OnItemInteractionListener {

    // ─── Views
    // IDs presentes no XML atual (doc 9):
    //   textTotalCarrinho, imgOlhoTotalCarrinho, textSaldoRestante,
    //   editOrcamentoMaximo (EditText puro), progressBarOrcamento,
    //   textPercentualOrcamento
    //
    // IDs que NÃO existem no XML atual (removidos do bind):
    //   textTotalLista, imgOlhoTotalLista  ← causavam o NPE / crash
    // ─────────────────────────────────────────────────────────────────────────
    private RecyclerView         recyclerViewItens;
    private LinearLayout         layoutEmptyState;
    private ProgressBar          progressBarOrcamento;
    private TextView             textEmptyStateTitulo;
    private TextView             textPercentualOrcamento;
    private TextView             textTotalCarrinho;
    private TextView             textSaldoRestante;
    private EditText             editOrcamentoMaximo;    // EditText puro no XML atual
    private ImageView            imgOlhoTotalCarrinho;
    private SearchView           searchViewItens;
    private ImageView            imgBtnCategoria;
    private ImageView            imgBtnLimparFiltro;
    private ImageView            imgBtnOrdenar;
    private ImageView            imgEditarOrcamento;
    private LinearLayout         layoutChipCategoriaAtiva;
    private Chip                 chipCategoriaAtiva;
    private FloatingActionButton fabMain;
    private LinearLayout         btnHistoricoTop;
    private ImageView            btnCompartilharTop;
    private ImageView            btnVoltar;
    private TextView             textContagemItens;


    // ─── Estado ───────────────────────────────────────────────────────────────
    private ItemMercadoAdapter           adapter;
    private final List<ItemMercadoModel> listaCompleta = new ArrayList<>();
    private final List<ItemMercadoModel> listaFiltrada = new ArrayList<>();

    private boolean totalCarrinhoVisivel    = true;
    private String  filtroBusca             = "";
    private String  filtroCategoria         = "";
    private int     ordemAtual              = 0;
    private int     orcamentoMaximoCentavos = 0;          // RN01 – centavos

    // Totais centralizados — calculados uma vez, reutilizados por saldo e exportar
    private int ultimoTotalListaCentavos    = 0;
    private int ultimoTotalCarrinhoCentavos = 0;

    private MercadoRepository repository;

    // ─── Ciclo de vida ────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_resumo_lista_mercado);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        repository = MercadoRepository.getInstance();
        bindViews();
        configurarRecyclerView();
        configurarBusca();
        configurarOrcamento();
        configurarBotoes();

        atualizarTotais();

        carregarItensDoFirebase();
    }

    // ─── Bind — somente IDs que existem no XML atual ──────────────────────────
    private void bindViews() {
        recyclerViewItens        = findViewById(R.id.recyclerViewItens);
        layoutEmptyState         = findViewById(R.id.layoutEmptyStateMercado);
        progressBarOrcamento     = findViewById(R.id.progressBarOrcamento);
        textEmptyStateTitulo     = findViewById(R.id.textEmptyStateTitulo);
        textPercentualOrcamento  = findViewById(R.id.textPercentualOrcamento);
        textTotalCarrinho        = findViewById(R.id.textTotalCarrinho);
        textSaldoRestante        = findViewById(R.id.textSaldoRestante);
        editOrcamentoMaximo      = findViewById(R.id.editOrcamentoMaximo);
        imgOlhoTotalCarrinho     = findViewById(R.id.imgOlhoTotalCarrinho);
        searchViewItens          = findViewById(R.id.searchViewItens);
        imgBtnCategoria          = findViewById(R.id.imgBtnCategoria);
        imgBtnLimparFiltro       = findViewById(R.id.imgBtnLimparFiltro);
        imgBtnOrdenar            = findViewById(R.id.imgBtnOrdenar);
        imgEditarOrcamento =      findViewById(R.id.imgEditarOrcamento);
        layoutChipCategoriaAtiva = findViewById(R.id.layoutChipCategoriaAtiva);
        chipCategoriaAtiva       = findViewById(R.id.chipCategoriaAtiva);
        fabMain                  = findViewById(R.id.fab_main);
        btnHistoricoTop          = findViewById(R.id.btnHistoricoTop);
        btnCompartilharTop       = findViewById(R.id.btnCompartilharTop);
        btnVoltar                = findViewById(R.id.btnVoltar);
        textContagemItens = findViewById(R.id.textContagemItens);
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────
    private void configurarRecyclerView() {
        adapter = new ItemMercadoAdapter(listaFiltrada, this);
        recyclerViewItens.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItens.setAdapter(adapter);
    }

    // ─── Busca ────────────────────────────────────────────────────────────────
    // ─── Busca ────────────────────────────────────────────────────────────────
    private void configurarBusca() {
        searchViewItens.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                // Fecha teclado ao apertar "Enter/Lupa" no teclado
                searchViewItens.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtroBusca = newText.trim().toLowerCase();
                aplicarFiltrosEOrdenacao();
                return true;
            }
        });

        // Intercepta o clique no botão 'X' (limpar) da SearchView
        ImageView closeBtn = searchViewItens.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                // Limpa o texto
                searchViewItens.setQuery("", false);
                // Remove o foco do campo
                searchViewItens.clearFocus();
                // Força o teclado a abaixar
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            });
        }
    }

    // ─── Orçamento (RF04) ─────────────────────────────────────────────────────
    private void configurarOrcamento() {
        // Bloqueia clique direto ou redireciona para o Dialog
        editOrcamentoMaximo.setOnClickListener(v -> abrirDialogOrcamento());
        imgEditarOrcamento.setOnClickListener(v -> abrirDialogOrcamento());

        // Garante que a tela inicie formatada caso já tenha valor
        editOrcamentoMaximo.setText(formatarMoeda(orcamentoMaximoCentavos));
    }

    // ─── Botões ───────────────────────────────────────────────────────────────
    private void configurarBotoes() {
        btnVoltar.setOnClickListener(v -> finish());
        btnHistoricoTop.setOnClickListener(v -> abrirHistorico());
        fabMain.setOnClickListener(v -> adicionarItemVazio());
        btnCompartilharTop.setOnClickListener(v -> exportarLista());

        imgOlhoTotalCarrinho.setOnClickListener(v -> {
            totalCarrinhoVisivel = !totalCarrinhoVisivel;
            atualizarTotais();
        });

        imgBtnCategoria.setOnClickListener(v -> abrirDialogCategorias());
        imgBtnOrdenar.setOnClickListener(v -> abrirMenuOrdenacao(v));
        imgBtnLimparFiltro.setOnClickListener(v -> limparFiltroCategoria());
        chipCategoriaAtiva.setOnCloseIconClickListener(v -> limparFiltroCategoria());
    }

    // ─── Carregar do Firebase (RN03) ──────────────────────────────────────────
    // ─── Carregar do Firebase (RN03) ──────────────────────────────────────────
    private void carregarItensDoFirebase() {
        // Mostra a tela de vazio, mas com o texto de carregamento
        textEmptyStateTitulo.setText("Carregando sua lista...");
        layoutEmptyState.setVisibility(View.VISIBLE);
        recyclerViewItens.setVisibility(View.GONE);

        repository.carregarItens(new MercadoRepository.CallbackLista() {
            @Override
            public void onSucesso(List<ItemMercadoModel> itens) {
                // Restaura o texto original para quando a lista realmente ficar vazia
                textEmptyStateTitulo.setText("Sua lista está vazia");

                listaCompleta.clear();
                listaCompleta.addAll(itens);
                aplicarFiltrosEOrdenacao();
            }

            @Override
            public void onErro(String mensagem) {
                // Restaura o texto e mostra o erro
                textEmptyStateTitulo.setText("Sua lista está vazia");

                Toast.makeText(ResumoListaMercadoActivity.this,
                        "Erro ao carregar lista: " + mensagem, Toast.LENGTH_LONG).show();
                aplicarFiltrosEOrdenacao();
            }
        });
    }

    // ─── Filtros + Ordenação (RF07) ───────────────────────────────────────────
    private void aplicarFiltrosEOrdenacao() {
        listaFiltrada.clear();

        for (ItemMercadoModel item : listaCompleta) {
            boolean passaBusca = filtroBusca.isEmpty()
                    || (item.getNome() != null
                    && item.getNome().toLowerCase().contains(filtroBusca));

            boolean passaCategoria = filtroCategoria.isEmpty()
                    || (item.getCategoria() != null
                    && item.getCategoria().equalsIgnoreCase(filtroCategoria));

            if (passaBusca && passaCategoria) listaFiltrada.add(item);
        }

        ordenarLista();
        adapter.notifyDataSetChanged();
        atualizarEmptyState();
        atualizarTotais();
    }

    private void ordenarLista() {
        switch (ordemAtual) {
            case 1:
                Collections.sort(listaFiltrada, (a, b) -> {
                    if (a.getNome() == null) return 1;
                    if (b.getNome() == null) return -1;
                    return a.getNome().compareToIgnoreCase(b.getNome());
                });
                break;
            case 2:
                Collections.sort(listaFiltrada, (a, b) -> {
                    int cmp = Integer.compare(b.getValorCentavos(), a.getValorCentavos());
                    if (cmp != 0) return cmp;
                    if (a.getNome() == null) return 1;
                    if (b.getNome() == null) return -1;
                    return a.getNome().compareToIgnoreCase(b.getNome());
                });
                break;
            case 3:
                Collections.sort(listaFiltrada,
                        (a, b) -> Boolean.compare(a.isNoCarrinho(), b.isNoCarrinho()));
                break;
            default:
                break;
        }
    }

    // ─── Totais (RF03, RN01) ──────────────────────────────────────────────────
    private void atualizarTotais() {

        ultimoTotalListaCentavos    = 0;
        ultimoTotalCarrinhoCentavos = 0;
        int qtdItensValidos = 0;
        int qtdItensNoCarrinho = 0;

        for (ItemMercadoModel item : listaCompleta) {
            if (item.getNome() == null || item.getNome().isEmpty()) continue;

            qtdItensValidos++;
            long sub = item.getSubtotalCentavos();

            if (sub > Integer.MAX_VALUE) continue; // CT04
            ultimoTotalListaCentavos += (int) sub;

            if (item.isNoCarrinho()) {
                ultimoTotalCarrinhoCentavos += (int) sub;
                qtdItensNoCarrinho++;
            }
        }

        // Atualiza a contagem dinâmica na tela
        if (textContagemItens != null) {
            textContagemItens.setText(qtdItensValidos + " itens • " + qtdItensNoCarrinho + " no carrinho");
        }

        if (totalCarrinhoVisivel) {
            textTotalCarrinho.setText(formatarMoeda(ultimoTotalCarrinhoCentavos));
            imgOlhoTotalCarrinho.setImageResource(R.drawable.ic_visibility_24);
        } else {
            textTotalCarrinho.setText("R$ •••••");
            imgOlhoTotalCarrinho.setImageResource(R.drawable.ic_visibility_off_24);
        }

        atualizarSaldoRestante();
        atualizarProgressoOrcamento(ultimoTotalCarrinhoCentavos);
    }

    private void atualizarSaldoRestante() {
        if (textSaldoRestante == null) return;
        if (orcamentoMaximoCentavos <= 0) {
            textSaldoRestante.setText("—");
            textSaldoRestante.setTextColor(Color.parseColor("#81C784"));
            return;
        }
        int saldo = orcamentoMaximoCentavos - ultimoTotalCarrinhoCentavos;
        if (saldo < 0) {
            // Mudança para a palavra "Passou"
            textSaldoRestante.setText("Passou " + formatarMoeda(Math.abs(saldo)));
            textSaldoRestante.setTextColor(Color.parseColor("#EF5350"));
        } else if (saldo == 0) {
            textSaldoRestante.setText("Restam " + formatarMoeda(0));
            textSaldoRestante.setTextColor(Color.parseColor("#FF7043"));
        } else {
            // Garante a palavra "Restam"
            textSaldoRestante.setText("Restam " + formatarMoeda(saldo));
            textSaldoRestante.setTextColor(Color.parseColor("#81C784"));
        }
    }

    private void atualizarProgressoOrcamento(int totalCarrinhoCentavos) {
        if (orcamentoMaximoCentavos <= 0) {
            progressBarOrcamento.setProgress(0);
            textPercentualOrcamento.setText("—");
            return;
        }
        int percentual = (int) ((long) totalCarrinhoCentavos * 100 / orcamentoMaximoCentavos);
        progressBarOrcamento.setProgress(Math.min(percentual, 100));
        textPercentualOrcamento.setText(percentual + "%");

        int cor;
        if (percentual >= 100)     cor = Color.parseColor("#E53935");
        else if (percentual >= 80) cor = Color.parseColor("#FF7043");
        else                       cor = Color.parseColor("#43A047");
        progressBarOrcamento.setProgressTintList(
                android.content.res.ColorStateList.valueOf(cor));
    }

    // ─── Empty State (CT09) ───────────────────────────────────────────────────
    private void atualizarEmptyState() {
        boolean temItemVisivel = false;
        for (ItemMercadoModel i : listaFiltrada) {
            if ((i.getNome() != null && !i.getNome().isEmpty()) || i.getFirestoreId() == null) {
                temItemVisivel = true;
                break;
            }
        }
        layoutEmptyState.setVisibility(temItemVisivel ? View.GONE    : View.VISIBLE);
        recyclerViewItens.setVisibility(temItemVisivel ? View.VISIBLE : View.GONE);
    }

    // ─── Dialog de Categorias (RF05) ─────────────────────────────────────────
    private void abrirDialogCategorias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_filtro_categorias_mercado, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RecyclerView rv = view.findViewById(R.id.recyclerCategoriasDialog);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<String> categorias = new ArrayList<>();
        categorias.add("Todos");
        categorias.add("Limpeza");   categorias.add("Açougue");
        categorias.add("Hortifruti"); categorias.add("Padaria");
        categorias.add("Mercearia"); categorias.add("Laticínios");
        categorias.add("Bebidas");   categorias.add("Higiene");

        rv.setAdapter(new CategoriaDialogAdapter(categorias, cat -> {
            if (cat.equals("Todos")) {
                limparFiltroCategoria();
            } else {
                filtroCategoria = cat;
                chipCategoriaAtiva.setText(cat);
                layoutChipCategoriaAtiva.setVisibility(View.VISIBLE);
                imgBtnLimparFiltro.setVisibility(View.VISIBLE);
                aplicarFiltrosEOrdenacao();
            }
            dialog.dismiss();
        }));

        View btnCancelar = view.findViewById(R.id.btnCancelarDialog);
        if (btnCancelar != null) btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ─── PopupMenu de Ordenação (RF07) ───────────────────────────────────────
    private void abrirMenuOrdenacao(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 0, 0, "Padrão (ordem de inserção)");
        popup.getMenu().add(0, 1, 1, "Ordem Alfabética");
        popup.getMenu().add(0, 2, 2, "Maior Valor Unitário");
        popup.getMenu().add(0, 3, 3, "Itens Pendentes Primeiro");
        popup.setOnMenuItemClickListener(item -> {
            ordemAtual = item.getItemId();
            aplicarFiltrosEOrdenacao();
            return true;
        });
        popup.show();
    }

    private void limparFiltroCategoria() {
        filtroCategoria = "";
        layoutChipCategoriaAtiva.setVisibility(View.GONE);
        imgBtnLimparFiltro.setVisibility(View.GONE);
        aplicarFiltrosEOrdenacao();
    }

    // ─── Adicionar item vazio inline (RF01) ───────────────────────────────────
    private void adicionarItemVazio() {
        ItemMercadoModel novoItem = new ItemMercadoModel("", "", 0, 1);
        listaCompleta.add(0, novoItem);

        filtroBusca = ""; filtroCategoria = "";
        searchViewItens.setQuery("", false);
        layoutChipCategoriaAtiva.setVisibility(View.GONE);
        imgBtnLimparFiltro.setVisibility(View.GONE);
        ordemAtual = 0;

        aplicarFiltrosEOrdenacao();
        recyclerViewItens.scrollToPosition(0);

        recyclerViewItens.post(() -> {
            RecyclerView.ViewHolder vh =
                    recyclerViewItens.findViewHolderForAdapterPosition(0);
            if (vh != null) {
                EditText nome = vh.itemView.findViewById(R.id.textNomeProduto);
                if (nome != null) nome.requestFocus();
            }
        });
    }

    // ─── Exportar (RF08) ─────────────────────────────────────────────────────
    private void exportarLista() {
        List<ItemMercadoModel> itensSalvos = new ArrayList<>();
        for (ItemMercadoModel item : listaCompleta) {
            if (item.getNome() != null && !item.getNome().trim().isEmpty())
                itensSalvos.add(item);
        }
        if (itensSalvos.isEmpty()) {
            Toast.makeText(this, "Nenhum item salvo para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File pdfFile = PdfListaMercadoGenerator.gerar(this, itensSalvos);
            Uri pdfUri   = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", pdfFile);

            StringBuilder resumo = new StringBuilder("🛒 *Lista de Mercado*\n\n");
            for (ItemMercadoModel item : itensSalvos) {
                resumo.append(item.isNoCarrinho() ? "✅ " : "⬜ ")
                        .append(item.getNome())
                        .append(" (").append(item.getQuantidade()).append("x) — ")
                        .append(formatarMoeda(item.getValorCentavos())).append("\n");
            }
            resumo.append("\n*Total: ").append(formatarMoeda(ultimoTotalListaCentavos)).append("*");

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.putExtra(Intent.EXTRA_TEXT, resumo.toString());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(intent, "Compartilhar lista"));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "Nenhum aplicativo encontrado.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirHistorico() {
        Toast.makeText(this, "Histórico em breve!", Toast.LENGTH_SHORT).show();
    }

    // ─── Callbacks do Adapter ─────────────────────────────────────────────────

    @Override
    public void onCarrinhoToggle(ItemMercadoModel item, boolean noCarrinho) {
        item.setNoCarrinho(noCarrinho);
        atualizarTotais();
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) return;
        repository.atualizarCarrinho(item, new MercadoRepository.Callback() {
            @Override public void onSucesso() {}
            @Override public void onErro(String m) {
                item.setNoCarrinho(!noCarrinho); atualizarTotais();
                Toast.makeText(ResumoListaMercadoActivity.this, "Erro: " + m, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onValorAlterado(ItemMercadoModel item, int novoValorCentavos) {
        if (novoValorCentavos <= 0) return;
        if ((long) novoValorCentavos * item.getQuantidade() > Integer.MAX_VALUE) {
            Toast.makeText(this, "Valor muito alto.", Toast.LENGTH_SHORT).show(); return;
        }
        int ant = item.getValorCentavos();
        item.setValorCentavos(novoValorCentavos);
        atualizarTotais();
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) return;
        repository.atualizarValorEQuantidade(item, new MercadoRepository.Callback() {
            @Override public void onSucesso() {}
            @Override public void onErro(String m) { item.setValorCentavos(ant); atualizarTotais(); }
        });
    }

    @Override
    public void onQuantidadeAlterada(ItemMercadoModel item, int novaQtd) {
        if (novaQtd <= 0) { Toast.makeText(this, "Mínimo 1.", Toast.LENGTH_SHORT).show(); return; }
        if ((long) item.getValorCentavos() * novaQtd > Integer.MAX_VALUE) {
            Toast.makeText(this, "Subtotal muito alto.", Toast.LENGTH_SHORT).show(); return;
        }
        int ant = item.getQuantidade();
        item.setQuantidade(novaQtd);
        atualizarTotais();
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) return;
        repository.atualizarValorEQuantidade(item, new MercadoRepository.Callback() {
            @Override public void onSucesso() {}
            @Override public void onErro(String m) {
                item.setQuantidade(ant); atualizarTotais(); adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onItemNovoPronto(ItemMercadoModel item) {
        if (item.getNome() == null || item.getNome().trim().isEmpty()) return;
        if (item.getFirestoreId() != null && !item.getFirestoreId().isEmpty()) return;
        repository.adicionarItem(item, new MercadoRepository.CallbackId() {
            @Override public void onSucesso(String id) { aplicarFiltrosEOrdenacao(); }
            @Override public void onErro(String m) {
                Toast.makeText(ResumoListaMercadoActivity.this, "Erro ao salvar: " + m, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onExcluirItem(ItemMercadoModel item) {
        int iC = listaCompleta.indexOf(item), iF = listaFiltrada.indexOf(item);
        listaCompleta.remove(item);
        if (iF >= 0) { listaFiltrada.remove(iF); adapter.notifyItemRemoved(iF); }
        atualizarEmptyState(); atualizarTotais();
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) return;
        repository.excluirItem(item, new MercadoRepository.Callback() {
            @Override public void onSucesso() {}
            @Override public void onErro(String m) {
                if (iC >= 0) { listaCompleta.add(iC, item); aplicarFiltrosEOrdenacao(); }
                Toast.makeText(ResumoListaMercadoActivity.this, "Erro ao excluir: " + m, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirDialogOrcamento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Definir Orçamento Máximo");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(40, 40, 40, 40);
        input.setTextSize(24f);

        // Se já tiver valor, seta para a máscara agir. Se não, começa zerado.
        input.setText(String.valueOf(orcamentoMaximoCentavos));

        input.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    input.removeTextChangedListener(this);

                    // Pega apenas os números digitados
                    String cleanString = s.toString().replaceAll("[^0-9]", "");

                    if (!cleanString.isEmpty()) {
                        long parsed = Long.parseLong(cleanString);
                        // Trava o limite em 9.999,99 (999999 centavos)
                        if (parsed > 999999) parsed = 999999;

                        String formatted = formatarMoeda((int) parsed);
                        current = formatted;
                        input.setText(formatted);
                        input.setSelection(formatted.length());
                    } else {
                        current = "";
                        input.setText("");
                    }

                    input.addTextChangedListener(this);
                }
            }
        });

        builder.setView(input);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String clean = input.getText().toString().replaceAll("[^0-9]", "");
            orcamentoMaximoCentavos = clean.isEmpty() ? 0 : Integer.parseInt(clean);
            editOrcamentoMaximo.setText(formatarMoeda(orcamentoMaximoCentavos));
            atualizarTotais();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // ─── Helper RN01 ─────────────────────────────────────────────────────────
    private String formatarMoeda(int centavos) {
        return String.format(Locale.US, "R$ %.2f", centavos / 100.0).replace(".", ",");
    }
}