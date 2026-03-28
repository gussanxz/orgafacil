package com.gussanxz.orgafacil.funcionalidades.mercado;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.gussanxz.orgafacil.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResumoListaMercadoActivity extends AppCompatActivity
        implements ItemMercadoAdapter.OnItemInteractionListener {

    // ─── Views ────────────────────────────────────────────────────────────────
    private RecyclerView recyclerViewItens;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBarOrcamento;
    private TextView textPercentualOrcamento;
    private TextView textTotalLista;
    private TextView textTotalCarrinho;
    private TextInputEditText editOrcamentoMaximo;
    private ImageView imgOlhoTotalLista;
    private ImageView imgOlhoTotalCarrinho;
    private SearchView searchViewItens;
    private ImageView imgBtnCategoria;
    private ImageView imgBtnLimparFiltro;
    private ImageView imgBtnOrdenar;
    private LinearLayout layoutChipCategoriaAtiva;
    private Chip chipCategoriaAtiva;
    private FloatingActionButton fabMain;
    private LinearLayout btnHistoricoTop;
    private ImageView btnVoltar;

    // ─── Estado ───────────────────────────────────────────────────────────────
    private ItemMercadoAdapter adapter;
    private List<ItemMercado> listaCompleta = new ArrayList<>();   // fonte de verdade
    private List<ItemMercado> listaFiltrada = new ArrayList<>();   // exibida

    // Visibilidade dos saldos
    private boolean totalListaVisivel    = true;
    private boolean totalCarrinhoVisivel = true;

    // Filtros ativos
    private String filtroBusca     = "";
    private String filtroCategoria = "";  // "" = todos

    // Ordenação: 0=padrão, 1=alfabética, 2=maior valor, 3=pendentes
    private int ordemAtual = 0;

    // RN01 – orçamento em centavos (int)
    private int orcamentoMaximoCentavos = 0;

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

        bindViews();
        configurarRecyclerView();
        configurarBusca();
        configurarOrcamento();
        configurarBotoes();
        carregarItensDoDb();
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────
    private void bindViews() {
        recyclerViewItens        = findViewById(R.id.recyclerViewItens);
        layoutEmptyState         = findViewById(R.id.layoutEmptyStateMercado);
        progressBarOrcamento     = findViewById(R.id.progressBarOrcamento);
        textPercentualOrcamento  = findViewById(R.id.textPercentualOrcamento);
        textTotalLista           = findViewById(R.id.textTotalLista);
        textTotalCarrinho        = findViewById(R.id.textTotalCarrinho);
        editOrcamentoMaximo      = findViewById(R.id.editOrcamentoMaximo);
        imgOlhoTotalLista        = findViewById(R.id.imgOlhoTotalLista);
        imgOlhoTotalCarrinho     = findViewById(R.id.imgOlhoTotalCarrinho);
        searchViewItens          = findViewById(R.id.searchViewItens);
        imgBtnCategoria          = findViewById(R.id.imgBtnCategoria);
        imgBtnLimparFiltro       = findViewById(R.id.imgBtnLimparFiltro);
        imgBtnOrdenar            = findViewById(R.id.imgBtnOrdenar);
        layoutChipCategoriaAtiva = findViewById(R.id.layoutChipCategoriaAtiva);
        chipCategoriaAtiva       = findViewById(R.id.chipCategoriaAtiva);
        fabMain                  = findViewById(R.id.fab_main);
        btnHistoricoTop          = findViewById(R.id.btnHistoricoTop);
        btnVoltar                = findViewById(R.id.btnVoltar);
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────
    private void configurarRecyclerView() {
        adapter = new ItemMercadoAdapter(listaFiltrada, this);
        recyclerViewItens.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItens.setAdapter(adapter);
    }

    // ─── Busca em tempo real ──────────────────────────────────────────────────
    private void configurarBusca() {
        searchViewItens.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                // RF07 – filtro por nome
                filtroBusca = newText.trim().toLowerCase();
                aplicarFiltrosEOrdenacao();
                return true;
            }
        });
    }

    // ─── Orçamento (RF04) ─────────────────────────────────────────────────────
    private void configurarOrcamento() {
        editOrcamentoMaximo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                String raw = s.toString().replace(",", ".");
                try {
                    double valor = Double.parseDouble(raw);
                    // RN01 – converte para centavos
                    orcamentoMaximoCentavos = (int) Math.round(valor * 100);
                } catch (NumberFormatException e) {
                    orcamentoMaximoCentavos = 0;
                }
                atualizarTotais();
            }
        });
    }

    // ─── Botões de ação ───────────────────────────────────────────────────────
    private void configurarBotoes() {

        // Voltar
        btnVoltar.setOnClickListener(v -> finish());

        // Histórico
        btnHistoricoTop.setOnClickListener(v -> abrirHistorico());

        // FAB → exportar (RF08)
        fabMain.setOnClickListener(v -> exportarLista());

        // Olho – Total da Lista
        imgOlhoTotalLista.setOnClickListener(v -> {
            totalListaVisivel = !totalListaVisivel;
            atualizarTotais();
        });

        // Olho – Total no Carrinho
        imgOlhoTotalCarrinho.setOnClickListener(v -> {
            totalCarrinhoVisivel = !totalCarrinhoVisivel;
            atualizarTotais();
        });

        // Funil → BottomSheet de categorias (RF05)
        imgBtnCategoria.setOnClickListener(v -> abrirBottomSheetCategorias());

        // Setas → PopupMenu de ordenação (RF07)
        imgBtnOrdenar.setOnClickListener(v -> abrirMenuOrdenacao(v));

        // Chip ativo → limpar filtro de categoria
        chipCategoriaAtiva.setOnCloseIconClickListener(v -> {
            filtroCategoria = "";
            layoutChipCategoriaAtiva.setVisibility(View.GONE);
            aplicarFiltrosEOrdenacao();
        });

        // Funil → Abre o NOVO Dialog de categorias (RF05)
        imgBtnCategoria.setOnClickListener(v -> abrirDialogCategorias());

        // Botão Limpar Filtro (Novo X Vermelho ao lado do funil)
        imgBtnLimparFiltro.setOnClickListener(v -> limparFiltroCategoria());

        // Chip ativo → limpar filtro de categoria (mantido para compatibilidade, caso use)
        chipCategoriaAtiva.setOnCloseIconClickListener(v -> limparFiltroCategoria());
    }

    // ─── Carregar dados (RN03 – Room/SQLite local) ────────────────────────────
    private void carregarItensDoDb() {
        // TODO: substituir por chamada ao DAO do Room
        // Exemplo de dados mockados para desenvolvimento:
        listaCompleta.clear();
        // listaCompleta.addAll(mercadoDao.getTodosItens());
        aplicarFiltrosEOrdenacao();
    }

    // ─── Filtros + Ordenação (RF07) ───────────────────────────────────────────
    private void aplicarFiltrosEOrdenacao() {
        listaFiltrada.clear();

        for (ItemMercado item : listaCompleta) {
            // Filtro por nome (busca – CT11: case insensitive)
            boolean passaBusca = filtroBusca.isEmpty()
                    || item.getNome().toLowerCase().contains(filtroBusca);

            // Filtro por categoria (RF05)
            boolean passaCategoria = filtroCategoria.isEmpty()
                    || item.getCategoria().equalsIgnoreCase(filtroCategoria);

            if (passaBusca && passaCategoria) {
                listaFiltrada.add(item);
            }
        }

        // CT09 – lista vazia não lança NPE
        ordenarLista();
        adapter.notifyDataSetChanged();
        atualizarEmptyState();
        atualizarTotais();
    }

    private void ordenarLista() {
        switch (ordemAtual) {
            case 1: // Alfabética
                Collections.sort(listaFiltrada,
                        (a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
                break;

            case 2: // Maior valor unitário (CT07, CT08 – desempate alfabético)
                Collections.sort(listaFiltrada, (a, b) -> {
                    int cmp = Integer.compare(b.getValorCentavos(), a.getValorCentavos());
                    if (cmp != 0) return cmp;
                    return a.getNome().compareToIgnoreCase(b.getNome()); // desempate
                });
                break;

            case 3: // Itens pendentes (não marcados) primeiro
                Collections.sort(listaFiltrada,
                        (a, b) -> Boolean.compare(a.isNoCarrinho(), b.isNoCarrinho()));
                break;

            default: // ordem de inserção
                break;
        }
    }

    // ─── Totais (RF03, RN01) ──────────────────────────────────────────────────
    private void atualizarTotais() {
        // RN01 – todo cálculo em centavos (int)
        int totalListaCentavos    = 0;
        int totalCarrinhoCentavos = 0;

        for (ItemMercado item : listaCompleta) {
            // CT04 – proteção contra overflow: usa long na multiplicação
            long subtotal = (long) item.getValorCentavos() * item.getQuantidade();
            if (subtotal > Integer.MAX_VALUE) {
                Toast.makeText(this, "Valor muito alto para um item!", Toast.LENGTH_SHORT).show();
                continue;
            }
            totalListaCentavos += (int) subtotal;
            if (item.isNoCarrinho()) {
                totalCarrinhoCentavos += (int) subtotal;
            }
        }

        // Exibição: divide por 100 somente na camada de apresentação (RN01)
        if (totalListaVisivel) {
            textTotalLista.setText(formatarMoeda(totalListaCentavos));
            imgOlhoTotalLista.setImageResource(R.drawable.ic_visibility_24);
        } else {
            textTotalLista.setText("R$ •••••");
            imgOlhoTotalLista.setImageResource(R.drawable.ic_visibility_off_24);
        }

        if (totalCarrinhoVisivel) {
            textTotalCarrinho.setText(formatarMoeda(totalCarrinhoCentavos));
            imgOlhoTotalCarrinho.setImageResource(R.drawable.ic_visibility_24);
        } else {
            textTotalCarrinho.setText("R$ •••••");
            imgOlhoTotalCarrinho.setImageResource(R.drawable.ic_visibility_off_24);
        }

        // RF04 – barra de progresso do orçamento (RN02 – não bloqueia)
        atualizarProgressoOrcamento(totalCarrinhoCentavos);
    }

    private void atualizarProgressoOrcamento(int totalCarrinhoCentavos) {
        if (orcamentoMaximoCentavos <= 0) {
            progressBarOrcamento.setProgress(0);
            textPercentualOrcamento.setText("—");
            return;
        }

        // Calcula percentual (pode ultrapassar 100 – RN02)
        int percentual = (int) ((long) totalCarrinhoCentavos * 100 / orcamentoMaximoCentavos);
        int progressoClamped = Math.min(percentual, 100);

        progressBarOrcamento.setProgress(progressoClamped);
        textPercentualOrcamento.setText(percentual + "%");

        // Muda cor da barra conforme proximidade do limite (RF04)
        if (percentual >= 100) {
            // Ultrapassou – vermelho (RN02: não bloqueia, só avisa visualmente)
            progressBarOrcamento.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935")));
        } else if (percentual >= 80) {
            // Atenção – laranja
            progressBarOrcamento.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FF7043")));
        } else {
            // Normal – verde
            progressBarOrcamento.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#43A047")));
        }
    }

    // ─── Empty State (CT09) ───────────────────────────────────────────────────
    private void atualizarEmptyState() {
        boolean vazia = listaFiltrada.isEmpty();
        layoutEmptyState.setVisibility(vazia ? View.VISIBLE : View.GONE);
        recyclerViewItens.setVisibility(vazia ? View.GONE : View.VISIBLE);
    }

    // ─── BottomSheet de Categorias (RF05) ────────────────────────────────────
    // ─── BottomSheet de Categorias (RF05) ────────────────────────────────────
    private void abrirBottomSheetCategorias() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categorias, null);
        dialog.setContentView(sheetView);

        ChipGroup chipGroupCategorias = sheetView.findViewById(R.id.chipGroupCategorias);

        // Lista de categorias disponíveis
        String[] categorias = {"Limpeza", "Açougue", "Hortifruti", "Padaria",
                "Mercearia", "Laticínios", "Bebidas", "Higiene"};

        for (String categoria : categorias) {
            Chip chip = new Chip(this);
            chip.setText(categoria);
            chip.setCheckable(true);
            chip.setChecked(categoria.equalsIgnoreCase(filtroCategoria));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    filtroCategoria = categoria;
                    chipCategoriaAtiva.setText(categoria);
                    layoutChipCategoriaAtiva.setVisibility(View.VISIBLE);
                    aplicarFiltrosEOrdenacao();
                    dialog.dismiss();
                }
            });
            chipGroupCategorias.addView(chip);
        }

        // CORREÇÃO AQUI: Cast alterado de TextView para ImageView (botão de limpar com o 'X')
        ImageView btnLimpar = sheetView.findViewById(R.id.btnLimparFiltroCategoria);
        if (btnLimpar != null) {
            btnLimpar.setOnClickListener(v -> {
                filtroCategoria = ""; // Reseta o filtro para mostrar todos
                layoutChipCategoriaAtiva.setVisibility(View.GONE);
                aplicarFiltrosEOrdenacao();
                dialog.dismiss();
            });
        }

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

    // ─── Exportar PDF + WhatsApp (RF08) ───────────────────────────────────────
    private void exportarLista() {
        try {
            // Gera o arquivo PDF com os itens da lista
            File pdfFile = PdfListaMercadoGenerator.gerar(this, listaCompleta);

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            // Resumo em texto para o corpo da mensagem (valores /100 para leitura humana)
            StringBuilder resumo = new StringBuilder("🛒 *Lista de Mercado*\n\n");
            for (ItemMercado item : listaCompleta) {
                resumo.append("• ")
                        .append(item.getNome())
                        .append(" (")
                        .append(item.getQuantidade())
                        .append("x) — ")
                        .append(formatarMoeda(item.getValorCentavos()))
                        .append("\n");
            }

            int totalCentavos = calcularTotalLista();
            resumo.append("\n*Total: ").append(formatarMoeda(totalCentavos)).append("*");

            // Intent de compartilhamento nativo do Android
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, resumo.toString());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // CT15 – captura ActivityNotFoundException (WhatsApp não instalado)
            try {
                startActivity(Intent.createChooser(shareIntent, "Compartilhar lista"));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this,
                        "Nenhum aplicativo encontrado para compartilhar.",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao gerar PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Histórico ────────────────────────────────────────────────────────────
    private void abrirHistorico() {
        // TODO: navegar para HistoricoListaMercadoActivity
        Toast.makeText(this, "Histórico em breve!", Toast.LENGTH_SHORT).show();
    }

    // ─── Callbacks do Adapter ─────────────────────────────────────────────────

    /**
     * RF02 – checkbox "No Carrinho" marcado/desmarcado
     */
    @Override
    public void onCarrinhoToggle(ItemMercado item, boolean noCarrinho) {
        item.setNoCarrinho(noCarrinho);
        // Persiste no banco (RN03)
        // mercadoDao.atualizarCarrinho(item.getId(), noCarrinho);
        atualizarTotais();
    }

    /**
     * RF01 – edição inline do valor unitário
     * RN01 – recebe valor em centavos
     * CT04 – verificação de overflow
     * CT05 – validação de valor nulo/zerado
     */
    @Override
    public void onValorAlterado(ItemMercado item, int novoValorCentavos) {
        // CT05 – valor não pode ser zero
        if (novoValorCentavos <= 0) {
            Toast.makeText(this, "O valor deve ser maior que zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        // CT04 – overflow: verifica se qty * valor cabe em int
        long subtotal = (long) novoValorCentavos * item.getQuantidade();
        if (subtotal > Integer.MAX_VALUE) {
            Toast.makeText(this, "Valor inválido: número muito alto.", Toast.LENGTH_SHORT).show();
            return;
        }

        item.setValorCentavos(novoValorCentavos);
        // Persiste e atualiza memória de preços (RF06)
        // mercadoDao.atualizarItem(item);
        // mercadoDao.salvarMemoriaPreco(item.getNome(), novoValorCentavos);
        atualizarTotais();
    }

    /**
     * RF01 – alteração de quantidade via stepper
     * CT04 – verificação de overflow
     * CT05 – quantidade não pode ser zero
     */
    @Override
    public void onQuantidadeAlterada(ItemMercado item, int novaQtd) {
        // CT05 – quantidade mínima 1
        if (novaQtd <= 0) {
            Toast.makeText(this, "A quantidade deve ser pelo menos 1.", Toast.LENGTH_SHORT).show();
            return;
        }

        // CT04 – overflow
        long subtotal = (long) item.getValorCentavos() * novaQtd;
        if (subtotal > Integer.MAX_VALUE) {
            Toast.makeText(this, "Quantidade inválida: subtotal muito alto.", Toast.LENGTH_SHORT).show();
            return;
        }

        item.setQuantidade(novaQtd);
        // mercadoDao.atualizarItem(item);
        adapter.notifyDataSetChanged();
        atualizarTotais();
    }

    /**
     * RF01 – exclusão de item
     */
    @Override
    public void onExcluirItem(ItemMercado item) {
        listaCompleta.remove(item);
        // mercadoDao.deletarItem(item.getId());
        aplicarFiltrosEOrdenacao();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * RN01 – formatação monetária SOMENTE na camada de apresentação.
     * Divide centavos por 100 aqui, nunca antes.
     */
    private String formatarMoeda(int centavos) {
        double valor = centavos / 100.0;
        return String.format("R$ %,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private int calcularTotalLista() {
        int total = 0;
        for (ItemMercado item : listaCompleta) {
            long sub = (long) item.getValorCentavos() * item.getQuantidade();
            if (sub <= Integer.MAX_VALUE) total += (int) sub;
        }
        return total;
    }

    // ─── Adicionar item (chamado pelo FAB ou Empty State) ─────────────────────
    public void abrirDialogNovoItem() {
        // RF06 – ao digitar nome, buscar memória de preços:
        // int precoMemoria = mercadoDao.buscarUltimoPreco(nomeProduto); // CT10, CT11, CT12
        // TODO: implementar DialogFragment ou Activity de inserção
    }

    private void limparFiltroCategoria() {
        filtroCategoria = "";
        layoutChipCategoriaAtiva.setVisibility(View.GONE);
        imgBtnLimparFiltro.setVisibility(View.GONE); // Esconde o botão de limpar
        aplicarFiltrosEOrdenacao();
    }

    // ─── Dialog Customizado de Categorias (RF05) ────────────────────────────────────
    private void abrirDialogCategorias() {
        // Usa o builder do AlertDialog para criar um dialog customizado
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        // Infla o XML do ConstraintLayout que você me enviou
        View view = getLayoutInflater().inflate(R.layout.dialog_filtro_categorias_mercado, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        // Deixa o fundo do Dialog transparente para o CardView arredondado aparecer direito
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Configura a RecyclerView dentro do Dialog
        RecyclerView recyclerCategorias = view.findViewById(R.id.recyclerCategoriasDialog);
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));

        // Lista de categorias mockadas (em breve virão do banco)
        List<String> categorias = new ArrayList<>();
        categorias.add("Limpeza");
        categorias.add("Açougue");
        categorias.add("Hortifruti");
        categorias.add("Padaria");
        categorias.add("Mercearia");
        categorias.add("Laticínios");
        categorias.add("Bebidas");
        categorias.add("Higiene");

        // Configura o Adapter que criamos na Parte 1
        CategoriaDialogAdapter adapter = new CategoriaDialogAdapter(categorias, categoriaSelecionada -> {
            // Quando o usuário clicar em uma categoria:
            filtroCategoria = categoriaSelecionada;

            // Atualiza a UI para mostrar que tem filtro ativo
            chipCategoriaAtiva.setText(categoriaSelecionada);
            layoutChipCategoriaAtiva.setVisibility(View.VISIBLE);
            imgBtnLimparFiltro.setVisibility(View.VISIBLE); // Mostra o botão X vermelho

            aplicarFiltrosEOrdenacao();
            dialog.dismiss(); // Fecha o dialog
        });

        recyclerCategorias.setAdapter(adapter);

        // Configura o botão de Cancelar do Dialog
        View btnCancelar = view.findViewById(R.id.btnCancelarDialog);
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}