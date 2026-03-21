package com.gussanxz.orgafacil.funcionalidades.contas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasDialogHelper;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.repository.ResumoFinanceiroRepository;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.DateHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.HelperExibirDatasMovimentacao;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ContasActivity extends AppCompatActivity {

    private final String TAG = "ContasActivity";
    private boolean ehAtalho = false;
    private Bundle extrasAtalho = null;
    private Long ultimoSaldoCarregado = null;

    private TextView textoSaudacao, textoSaldo, textoTituloSaldo, textPeriodoSelecionado;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private ImageView imgLimparFiltroData, imgFiltroCalendario;
    private Button btnNovaDespesa, btnNovaReceita;
    private ProgressBar progressBarPaginacao;
    private com.google.android.material.slider.RangeSlider rangeSliderValor;
    private android.widget.LinearLayout layoutFiltroValor;
    private android.widget.TextView textLabelFiltroValor;

    private final List<AdapterItemListaMovimentacao> itensAgrupados = new ArrayList<>();
    private AdapterMovimentacaoLista adapterAgrupado;
    private Date dataInicialFiltro, dataFinalFiltro;

    private ContasViewModel viewModel;
    private ResumoFinanceiroRepository resumoRepository;

    private ActivityResultLauncher<Intent> launcher;

    private View layoutEmptyStateContas;
    private TextView textEmptyStateContas;
    private Button btnEmptyStateCTA;
    ImageView imgEmptyStateContas;
    private com.google.android.material.chip.ChipGroup chipGroupFiltroTipo;
    private ListenerRegistration listenerResumo;
    private ImageView imgFiltroCategoria;
    private String categoriaIdFiltro = null;

    // Guarda o nome da categoria selecionada para reconstruir o label
    // sem precisar resolver o ID a cada chamada. Declarado junto com
    // categoriaIdFiltro pois os dois sempre andam juntos.
    private String categoriaNomeFiltro = null;

    private static final int LIMIAR_PAGINACAO_PROATIVA = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_movimentacoes);

        viewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        resumoRepository = new ResumoFinanceiroRepository();

        extrasAtalho = (getIntent() != null) ? getIntent().getExtras() : null;
        ehAtalho = (extrasAtalho != null) && extrasAtalho.getBoolean("EH_ATALHO", false);

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();
        configurarChipsFiltro();
        setupObservers();
        viewModel.carregarNomeUsuario(); // busca uma vez; cache no ViewModel para chamadas futuras

        // ContasActivity.java — no registerForActivityResult
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        viewModel.invalidarDados();
                        carregarDados(); // ← ADICIONAR ESTA LINHA
                    }
                }
        );

        if (ehAtalho) aplicarRegrasAtalho(extrasAtalho);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // FIX: zera ultimoSaldoCarregado a cada onStart() para que o listener
        // do Firestore possa atualizar o saldo quando a Activity volta ao foco.
        // Sem isso o guard "ultimoSaldoCarregado != null" bloqueava o listener
        // permanentemente após o primeiro carregamento.
        ultimoSaldoCarregado = null;
        if (viewModel.isDadosInvalidados()) carregarDados();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removerListenerResumo();
    }

    private void removerListenerResumo() {
        if (listenerResumo != null) {
            listenerResumo.remove();
            listenerResumo = null;
        }
    }

    private void setupObservers() {
        // Nome do usuário: observado uma vez, atualiza a saudação quando o
        // ViewModel retornar o valor do cache ou do Firestore.
        viewModel.nomeUsuario.observe(this, nome -> {
            if (nome != null) textoSaudacao.setText("Olá, " + nome + "!");
        });

        viewModel.carregandoPaginacao.observe(this, isCarregando -> {
            if (isCarregando) {
                progressBarPaginacao.setVisibility(View.VISIBLE);

                if (Boolean.TRUE.equals(viewModel.isPrimeiroCarregamento.getValue())) {
                    textoSaldo.setText("--");
                    textoSaldo.setTextColor(Color.WHITE);
                    textoTituloSaldo.setText("Carregando saldo...");
                }
            } else {
                progressBarPaginacao.setVisibility(View.GONE);
            }
        });

        Observer<List<MovimentacaoModel>> observerUI = lista -> {
            if (Boolean.TRUE.equals(viewModel.isPrimeiroCarregamento.getValue())
                    && Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

            if (lista == null || lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyStateContas.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmptyStateContas.setVisibility(View.GONE);
            }

            List<AdapterItemListaMovimentacao> listaProcessada = new ArrayList<>();
            if (lista != null) {
                listaProcessada = HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehAtalho);
            }

            boolean listaEstavaVazia = adapterAgrupado.getCurrentList().isEmpty();
            if (listaEstavaVazia && !listaProcessada.isEmpty()) {
                recyclerView.scheduleLayoutAnimation();
            }

            final int totalMovimentacoesFiltradas = (lista != null) ? lista.size() : 0;
            final List<AdapterItemListaMovimentacao> listaFinal = listaProcessada;

            adapterAgrupado.submitList(listaFinal, () -> {
                if (searchView.getQuery().toString().isEmpty() && dataInicialFiltro == null) {
                    recyclerView.scrollToPosition(0);
                }
                verificarPaginacaoProativaAposSubmit(totalMovimentacoesFiltradas);
            });

            atualizarLegendasFiltro(searchView.getQuery().toString());
        };

        Observer<Long> observerSaldo = saldoCentavos -> {
            if (saldoCentavos == null) return;
            ultimoSaldoCarregado = saldoCentavos;

            double saldoDouble = saldoCentavos / 100.0;
            String valorFormatado = MoedaHelper.formatarCentavosParaBRL(saldoCentavos);

            int corSaldo = saldoCentavos > 0 ? Color.parseColor("#4CAF50") :
                    (saldoCentavos < 0 ? Color.parseColor("#F44336") : Color.WHITE);

            View containerSaldoContas = findViewById(R.id.containerSaldoContas);
            if (containerSaldoContas != null) {
                VisibilidadeHelper.configurarVisibilidadeSaldo(containerSaldoContas, textoSaldo, findViewById(R.id.imgOlhoSaldoContas), valorFormatado, corSaldo);
            }
            VisibilidadeHelper.atualizarValorSaldo(textoSaldo, findViewById(R.id.imgOlhoSaldoContas), valorFormatado, corSaldo);
            atualizarTextoResumo();
        };

        if (ehAtalho) {
            viewModel.listaFutura.observe(this, observerUI);
            viewModel.saldoFuturo.observe(this, observerSaldo);
        } else {
            viewModel.listaHistorico.observe(this, observerUI);
            viewModel.saldoPeriodo.observe(this, observerSaldo);
        }
    }

    private void carregarDados() {
        if (!ehAtalho && dataInicialFiltro == null && searchView.getQuery().length() == 0) {
            registrarListenerResumo();
        } else {
            removerListenerResumo();
        }

        recuperarMovimentacoesDoBanco();
    }

    private void registrarListenerResumo() {
        removerListenerResumo();

        listenerResumo = resumoRepository.escutarResumoGeral(
                new ResumoFinanceiroRepository.ResumoCallback() {
                    @Override
                    public void onUpdate(ResumoFinanceiroModel resumo) {
                        if (resumo != null && resumo.getBalanco() != null) {
                            if (Boolean.TRUE.equals(viewModel.isPrimeiroCarregamento.getValue())
                                    || Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())
                                    || ultimoSaldoCarregado != null) return;

                            long saldoCentavos = resumo.getBalanco().getSaldoAtual();
                            textoSaldo.setText(MoedaHelper.formatarCentavosParaBRL(saldoCentavos));
                        }
                    }

                    @Override
                    public void onError(String erro) {
                        Log.e(TAG, "Erro no resumo: " + erro);
                    }
                });
    }

    private void recuperarMovimentacoesDoBanco() {
        viewModel.fetchDados(ehAtalho, new MovimentacaoRepository.DadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> lista) {}
            @Override public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- DELEGAÇÃO DE DIALOGS PARA O HELPER ---

    private void confirmarExclusao(MovimentacaoModel mov, int positionParaRestaurar) {
        ContasDialogHelper.confirmarExclusao(this, mov, new ContasDialogHelper.AcaoUnicaCallback() {
            @Override
            public void onConfirmar() {
                viewModel.excluir(mov, new MovimentacaoRepository.Callback() {
                    @Override
                    public void onSucesso(String msg) {
                        Toast.makeText(ContasActivity.this, "Lançamento excluído!", Toast.LENGTH_SHORT).show();
                        recarregarDadosAposMutacao();
                    }
                    @Override public void onErro(String erro) {
                        Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        if (positionParaRestaurar != -1) adapterAgrupado.notifyItemChanged(positionParaRestaurar);
                    }
                });
            }
            @Override
            public void onCancelar() {
                if (positionParaRestaurar != -1) adapterAgrupado.notifyItemChanged(positionParaRestaurar);
            }
        });
    }

    private void confirmarPagamentoOuRecebimento(MovimentacaoModel mov) {
        ContasDialogHelper.confirmarPagamentoOuRecebimento(this, mov, new ContasDialogHelper.AcaoMultiplaCallback() {
            @Override public void onApenasEsta() { executarConfirmacao(mov); }
            @Override public void onEstaESeguintes() { executarConfirmacaoEmMassa(mov); }
        });
    }

    private void confirmarExclusaoDoDia(String tituloDia, List<MovimentacaoModel> movimentos) {
        ContasDialogHelper.confirmarExclusaoDoDia(this, tituloDia, movimentos, new ContasDialogHelper.AcaoUnicaCallback() {
            @Override
            public void onConfirmar() {
                viewModel.excluirEmLote(movimentos, new MovimentacaoRepository.Callback() {
                    @Override
                    public void onSucesso(String msg) {
                        if (!isFinishing()) {
                            Toast.makeText(ContasActivity.this, msg, Toast.LENGTH_SHORT).show();
                            recarregarDadosAposMutacao();
                        }
                    }
                    @Override
                    public void onErro(String erro) {
                        if (!isFinishing()) Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void abrirMenuEscolha() {
        ContasDialogHelper.abrirMenuEscolha(this, new ContasDialogHelper.EscolhaNovaMovimentacaoCallback() {
            @Override public void onEscolherReceita() { adicionarReceita(null); }
            @Override public void onEscolherDespesa() { adicionarDespesa(null); }
        });
    }

    // --- FILTROS ---

    private void aplicarFiltros() {
        viewModel.aplicarFiltros(searchView.getQuery().toString(), dataInicialFiltro, dataFinalFiltro, categoriaIdFiltro);
    }

    // Lê os thumbs do RangeSlider, converte para centavos e passa ao ViewModel.
    // Separa a atualização do label em atualizarLabelFiltroValor() para que o
    // reset do limpar possa forçar o label sem depender de fromUser=true.
    private void aplicarFiltroValorDoSlider() {
        if (rangeSliderValor == null) return;
        java.util.List<Float> values = rangeSliderValor.getValues();
        long minCentavos = Math.round(values.get(0) * 100);
        long maxCentavos = Math.round(values.get(1) * 100);

        boolean semFiltroMin = (minCentavos <= 0);
        boolean semFiltroMax = (maxCentavos >= Math.round(rangeSliderValor.getValueTo() * 100));

        viewModel.setFiltroValor(
                semFiltroMin ? -1L : minCentavos,
                semFiltroMax ? -1L : maxCentavos);

        atualizarLabelFiltroValor(
                semFiltroMin ? -1L : minCentavos,
                semFiltroMax ? -1L : maxCentavos);
    }

    // Atualiza apenas o textLabelFiltroValor.
    // Recebe centavos (-1 = sem limite) e compõe o texto para todos os cenários:
    //   -1 / -1        → "Valor: qualquer"
    //   X  / -1        → "Valor: a partir de R$ X"
    //   -1 / Y         → "Valor: até R$ Y"
    //   X  / Y         → "Valor: R$ X → R$ Y"
    private void atualizarLabelFiltroValor(long minCentavos, long maxCentavos) {
        if (textLabelFiltroValor == null) return;
        boolean semMin = (minCentavos < 0);
        boolean semMax = (maxCentavos < 0);
        if (semMin && semMax) {
            textLabelFiltroValor.setText("Valor: qualquer");
        } else if (!semMin && semMax) {
            textLabelFiltroValor.setText("Valor: a partir de "
                    + MoedaHelper.formatarCentavosParaBRL(minCentavos));
        } else if (semMin) {
            textLabelFiltroValor.setText("Valor: até "
                    + MoedaHelper.formatarCentavosParaBRL(maxCentavos));
        } else {
            textLabelFiltroValor.setText("Valor: "
                    + MoedaHelper.formatarCentavosParaBRL(minCentavos)
                    + " a "
                    + MoedaHelper.formatarCentavosParaBRL(maxCentavos));
        }
        // Sempre que o label de valor muda, reconstrói o textPeriodoSelecionado
        // para que o fragmento de valor fique sincronizado com data e categoria.
        atualizarVisualFiltroCategoria(obterNomeCategoriaAtiva());
    }

    // --- UI SETUP ---

    private void inicializarComponentes() {
        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        textoTituloSaldo = findViewById(R.id.textoTituloSaldo);
        recyclerView = findViewById(R.id.recyclesMovimentos);
        editDataInicial = findViewById(R.id.editDataInicial);
        editDataFinal = findViewById(R.id.editDataFinal);
        imgLimparFiltroData = findViewById(R.id.imgLimparFiltroData);
        imgFiltroCalendario = findViewById(R.id.imgFiltroCalendario);
        textPeriodoSelecionado = findViewById(R.id.textPeriodoSelecionado);
        searchView = findViewById(R.id.searchViewEventos);
        btnNovaDespesa = findViewById(R.id.btnNovaDespesa);
        btnNovaReceita = findViewById(R.id.btnNovaReceita);
        progressBarPaginacao = findViewById(R.id.progressBarPaginacao);

        layoutEmptyStateContas = findViewById(R.id.layoutEmptyStateContas);
        textEmptyStateContas = findViewById(R.id.textEmptyStateContas);
        btnEmptyStateCTA = findViewById(R.id.btnEmptyStateCTA);
        imgEmptyStateContas = findViewById(R.id.imgEmptyStateContas);
        chipGroupFiltroTipo = findViewById(R.id.chipGroupFiltroTipo);
        imgFiltroCategoria = findViewById(R.id.imgFiltroCategoria);
        layoutFiltroValor = findViewById(R.id.layoutFiltroValor);
        textLabelFiltroValor = findViewById(R.id.textLabelFiltroValor);
        rangeSliderValor = findViewById(R.id.rangeSliderValor);

        if (ehAtalho) {
            imgEmptyStateContas.setImageResource(R.drawable.ic_event_available_24);
            textEmptyStateContas.setText("Tudo em dia. Nenhuma conta pendente 🎉");
            // FIX bug 4: em modo atalho a mensagem é "tudo em dia" — não faz
            // sentido exibir o botão de adicionar movimentação nesse contexto.
            btnEmptyStateCTA.setVisibility(View.GONE);
            btnEmptyStateCTA.setText("COMEÇAR MEU PLANEJAMENTO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26A69A")));
        } else {
            imgEmptyStateContas.setImageResource(R.drawable.ic_receipt_long_28);
            textEmptyStateContas.setText("Você ainda não registrou movimentações.");
            btnEmptyStateCTA.setText("ADICIONAR MINHA PRIMEIRA MOVIMENTAÇÃO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF7043")));
            // No modo histórico o botão permanece VISIBLE (padrão do XML)
        }

        btnEmptyStateCTA.setOnClickListener(v -> {
            abrirMenuEscolha();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        });

        editDataInicial.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (!s.toString().isEmpty()) {
                    dataInicialFiltro = DateHelper.parsearData(s.toString());
                    aplicarFiltros();
                }
            }
        });

        editDataFinal.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (!s.toString().isEmpty()) {
                    dataFinalFiltro = DateHelper.parsearDataFim(s.toString());
                    aplicarFiltros();
                }
            }
        });
    }

    private void configurarRecyclerView() {
        adapterAgrupado = new AdapterMovimentacaoLista(this, new AdapterMovimentacaoLista.OnItemActionListener() {
            @Override public void onDeleteClick(MovimentacaoModel m) { confirmarExclusao(m, -1); }
            @Override public void onLongClick(MovimentacaoModel m) {
                new AlertDialog.Builder(ContasActivity.this)
                        .setTitle("Editar")
                        .setMessage("Você deseja editar '" + m.getDescricao() + "'?")
                        .setPositiveButton("Sim", (dialog, which) -> abrirTelaEdicao(m, true))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
            @Override public void onCheckClick(MovimentacaoModel m) {
                recyclerView.performHapticFeedback(
                        android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                confirmarPagamentoOuRecebimento(m);
            }
            // onHeaderSwipeDelete não é implementado aqui porque o adapter nunca
            // chama listener.onHeaderSwipeDelete() — o evento de swipe no header
            // é disparado exclusivamente pelo SwipeCallback.onSwiped() abaixo,
            // que invoca o seu próprio onHeaderSwipeDelete() sobrescrito.
            // A implementação duplicada que existia aqui era código morto.
            @Override public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) {}
            @Override public void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia) {
                exibirPopupResumoDia(tituloDia, movsDoDia);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterAgrupado);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;

                int totalItens    = layoutManager.getItemCount();
                int ultimoVisivel = layoutManager.findLastVisibleItemPosition();
                int itensVisiveis = layoutManager.getChildCount();

                boolean chegouAoFim = (itensVisiveis + ultimoVisivel) >= totalItens - 2;
                boolean listaFiltradaPequena = totalItens < LIMIAR_PAGINACAO_PROATIVA;

                boolean devePaginar = chegouAoFim || listaFiltradaPequena;
                if (!devePaginar) return;

                if (Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

                if (ehAtalho) {
                    if (!viewModel.isUltimaPaginaFuturo()) {
                        viewModel.carregarMaisFuturo();
                    }
                } else {
                    if (!viewModel.isUltimaPaginaHistorico()) {
                        viewModel.carregarMaisHistorico();
                    }
                }
            }
        });

        new ItemTouchHelper(new SwipeCallback(this) {
            @Override
            protected void onHeaderSwipeDelete(String tituloDia, List<MovimentacaoModel> movimentos) {
                confirmarExclusaoDoDia(tituloDia, movimentos);
            }

            @Override
            protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                             int direction, int position) {
                AdapterItemListaMovimentacao item = adapterAgrupado.getCurrentList().get(position);

                if (item.type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
                    MovimentacaoModel m = item.movimentacaoModel;
                    if (direction == ItemTouchHelper.LEFT) {
                        confirmarExclusao(m, position);
                    } else {
                        recyclerView.post(() -> adapterAgrupado.notifyItemChanged(position));
                        com.google.android.material.snackbar.Snackbar
                                .make(recyclerView, "Abrindo edição de " + m.getDescricao() + "...", 600)
                                .show();
                        recyclerView.postDelayed(() -> abrirTelaEdicao(m, true), 600);
                    }
                } else {
                    recyclerView.post(() -> adapterAgrupado.notifyItemChanged(position));
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void abrirTelaEdicao(MovimentacaoModel m, boolean diretoPraEdicao) {
        Intent intent = new Intent(this, EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", m);
        intent.putExtra("DIRETO_PRA_EDICAO", diretoPraEdicao);
        launcher.launch(intent);
    }

    private void configurarFiltros() {
        imgFiltroCalendario.setOnClickListener(v -> abrirDataPicker(true));
        imgFiltroCategoria.setOnClickListener(v -> abrirDialogFiltroCategoria());

        // ── Filtro de valor (RangeSlider) — sempre visível ───────────────────
        if (rangeSliderValor != null) {
            // Aplica o filtro imediatamente ao iniciar com o range completo
            aplicarFiltroValorDoSlider();
            rangeSliderValor.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) aplicarFiltroValorDoSlider();
            });
        }

        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText("");
            editDataFinal.setText("");
            dataInicialFiltro = null;
            dataFinalFiltro = null;
            categoriaIdFiltro = null;
            categoriaNomeFiltro = null;
            searchView.setQuery("", false);
            searchView.clearFocus();
            chipGroupFiltroTipo.check(R.id.chipTodos);
            viewModel.setFiltroTipo(null);
            // Reseta o RangeSlider ao range completo (painel permanece visível).
            // setValues() com fromUser=false não dispara o listener — por isso
            // chamamos atualizarLabelFiltroValor() explicitamente logo depois.
            if (rangeSliderValor != null) {
                rangeSliderValor.setValues(
                        rangeSliderValor.getValueFrom(),
                        rangeSliderValor.getValueTo());
            }
            viewModel.setFiltroValor(-1L, -1L);
            // Ordem importa: label de valor primeiro, depois o composto de período
            // (atualizarLabelFiltroValor chama atualizarVisualFiltroCategoria internamente).
            atualizarLabelFiltroValor(-1L, -1L);
            aplicarFiltros();
            Toast.makeText(this, "Filtros limpos", Toast.LENGTH_SHORT).show();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                searchView.clearFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String n) {
                aplicarFiltros();
                return true;
            }
        });
    }

    private void abrirDataPicker(boolean isInicio) {
        Date dataPreSelecionada = isInicio ? dataInicialFiltro : dataFinalFiltro;
        Calendar c = Calendar.getInstance();
        if (dataPreSelecionada != null) c.setTime(dataPreSelecionada);

        DatePickerDialog picker = new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar cal = Calendar.getInstance();
            if (isInicio) {
                cal.set(y, m, d, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dataInicialFiltro = cal.getTime();
                editDataInicial.setText(DateHelper.formatarData(dataInicialFiltro));
                Toast.makeText(this, "Selecione a data final", Toast.LENGTH_SHORT).show();
                abrirDataPicker(false);
            } else {
                cal.set(y, m, d, 23, 59, 59);
                cal.set(Calendar.MILLISECOND, 999);
                dataFinalFiltro = cal.getTime();
                editDataFinal.setText(DateHelper.formatarData(dataFinalFiltro));

                // Reconstrói o label com intervalo + categoria (se ativa).
                // Delega para atualizarVisualFiltroCategoria() que sabe compor
                // os dois filtros sem sobrescrever um com o outro.
                atualizarVisualFiltroCategoria(obterNomeCategoriaAtiva());

                aplicarFiltros();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        if (!ehAtalho) {
            // Data final nunca pode ultrapassar hoje.
            // Se estivermos no picker de data inicial e já houver uma data final
            // definida, limitamos o máximo à data final para evitar inversão.
            if (!isInicio && dataFinalFiltro != null) {
                picker.getDatePicker().setMaxDate(dataFinalFiltro.getTime());
            } else {
                picker.getDatePicker().setMaxDate(System.currentTimeMillis());
            }
        }

        // Picker de data final: bloqueia datas anteriores à data inicial já escolhida.
        // Picker de data inicial: bloqueia datas posteriores à data final já escolhida
        // (somente se ela existir), complementando a restrição de setMaxDate acima.
        if (!isInicio && dataInicialFiltro != null) {
            picker.getDatePicker().setMinDate(dataInicialFiltro.getTime());
        } else if (isInicio && dataFinalFiltro != null) {
            picker.getDatePicker().setMaxDate(dataFinalFiltro.getTime());
        }

        TextView titleView = new TextView(this);
        titleView.setText(isInicio ? "Data Inicial" : "Data Final");
        titleView.setPadding(64, 48, 64, 48);
        titleView.setTextSize(20f);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setBackgroundColor(Color.parseColor("#121212"));

        picker.setCustomTitle(titleView);
        picker.show();
    }

    private void executarConfirmacao(MovimentacaoModel mov) {
        viewModel.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, "Concluído!", Toast.LENGTH_SHORT).show();
                recarregarDadosAposMutacao();
            }
            @Override public void onErro(String erro) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarConfirmacaoEmMassa(MovimentacaoModel movBase) {
        viewModel.confirmarMovimentacaoEmMassa(movBase, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, msg, Toast.LENGTH_SHORT).show();
                recarregarDadosAposMutacao();
            }
            @Override public void onErro(String erro) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void atualizarLegendasFiltro(String query) {
        if (ehAtalho) textoTituloSaldo.setText("Total pendente");
        else if (dataInicialFiltro != null) textoTituloSaldo.setText("Saldo do período");
        else if (!query.isEmpty()) textoTituloSaldo.setText("Saldo da pesquisa");
        else textoTituloSaldo.setText("Balanço Projetado");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuSair) {
            ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void adicionarReceita(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        if (ehAtalho) { intent.putExtra("EH_CONTA_FUTURA", true); intent.putExtra("TITULO_TELA", "Agendar Receita"); intent.putExtra("EH_ATALHO", true); }
        else { intent.putExtra("TITULO_TELA", "Adicionar Receita"); }
        launcher.launch(intent);
    }

    public void adicionarDespesa(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        if (ehAtalho) { intent.putExtra("EH_CONTA_FUTURA", true); intent.putExtra("TITULO_TELA", "Agendar Despesa"); intent.putExtra("EH_ATALHO", true); }
        else { intent.putExtra("TITULO_TELA", "Adicionar Despesa"); }
        launcher.launch(intent);
    }

    private void aplicarRegrasAtalho(Bundle extras) {
        if (btnNovaDespesa != null) btnNovaDespesa.setText("Agendar Despesa");
        if (btnNovaReceita != null) btnNovaReceita.setText("Agendar Receita");
    }

    private void atualizarTextoResumo() {
        if (ehAtalho) {
            long saldoCentavos = (viewModel.saldoFuturo.getValue() != null) ? viewModel.saldoFuturo.getValue() : 0;
            if (saldoCentavos < 0) textoTituloSaldo.setText("Total a pagar");
            else if (saldoCentavos > 0) textoTituloSaldo.setText("Total a receber");
            else textoTituloSaldo.setText("Nenhum valor pendente");
        } else {
            textoTituloSaldo.setText("Balanço Projetado");
        }
    }

    private void configurarChipsFiltro() {
        chipGroupFiltroTipo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chipTodos)) viewModel.setFiltroTipo(null);
            else if (checkedIds.contains(R.id.chipSoReceitas)) viewModel.setFiltroTipo(TipoCategoriaContas.RECEITA);
            else if (checkedIds.contains(R.id.chipSoDespesas)) viewModel.setFiltroTipo(TipoCategoriaContas.DESPESA);
        });
    }

    private void exibirPopupResumoDia(String tituloDia, List<MovimentacaoModel> movsDoDia) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_resumo_dia);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView txtTitulo       = dialog.findViewById(R.id.textTituloDialogResumo);
        TextView txtQtdReceitas  = dialog.findViewById(R.id.textQtdReceitas);
        TextView txtValorReceitas= dialog.findViewById(R.id.textValorReceitas);
        TextView txtQtdDespesas  = dialog.findViewById(R.id.textQtdDespesas);
        TextView txtValorDespesas= dialog.findViewById(R.id.textValorDespesas);
        TextView txtSaldoFinal   = dialog.findViewById(R.id.textSaldoDialog);
        com.google.android.material.button.MaterialButton btnVoltar = dialog.findViewById(R.id.btnVoltarResumo);

        txtTitulo.setText("Resumo: " + tituloDia);

        long totalReceitas = 0, totalDespesas = 0;
        int qtdReceitas = 0, qtdDespesas = 0;

        for (MovimentacaoModel mov : movsDoDia) {
            if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                totalReceitas += mov.getValor(); qtdReceitas++;
            } else {
                totalDespesas += mov.getValor(); qtdDespesas++;
            }
        }

        long saldoFinalLong = totalReceitas - totalDespesas;

        txtQtdReceitas.setText(qtdReceitas + (qtdReceitas == 1 ? " Receita" : " Receitas"));
        txtValorReceitas.setText("+ " + MoedaHelper.formatarCentavosParaBRL(totalReceitas));
        txtQtdDespesas.setText(qtdDespesas + (qtdDespesas == 1 ? " Despesa" : " Despesas"));
        txtValorDespesas.setText("- " + MoedaHelper.formatarCentavosParaBRL(totalDespesas));
        txtSaldoFinal.setText(MoedaHelper.formatarCentavosParaBRL(saldoFinalLong));

        if (saldoFinalLong > 0)      txtSaldoFinal.setTextColor(Color.parseColor("#008000"));
        else if (saldoFinalLong < 0) txtSaldoFinal.setTextColor(Color.parseColor("#E53935"));
        else                         txtSaldoFinal.setTextColor(Color.parseColor("#757575"));

        btnVoltar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void abrirDialogFiltroCategoria() {
        viewModel.buscarCategoriasParaFiltro(new ContasViewModel.CategoriasFiltroCallback() {

            @Override
            public void onSucesso(List<ContasCategoriaModel> listaCategorias) {
                if (listaCategorias.isEmpty()) {
                    Toast.makeText(ContasActivity.this,
                            "Nenhuma categoria encontrada.", Toast.LENGTH_SHORT).show();
                    return;
                }

                android.app.Dialog dialog = new android.app.Dialog(ContasActivity.this);
                dialog.setContentView(R.layout.dialog_selecionar_categoria);

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.getWindow().setLayout(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                RecyclerView recycler = dialog.findViewById(R.id.recyclerCategoriasDialog);
                recycler.setLayoutManager(new LinearLayoutManager(ContasActivity.this));

                DialogCategoriaAdapter adapter = new DialogCategoriaAdapter(cat -> {
                    categoriaIdFiltro = cat.getId();
                    // Guarda o nome para que atualizarVisualFiltroCategoria() e
                    // obterNomeCategoriaAtiva() possam reconstruir o label sem
                    // nova query — o nome já está disponível aqui no clique.
                    categoriaNomeFiltro = cat.getNome();
                    // Atualiza ícone (tint laranja) e label com o nome da categoria.
                    atualizarVisualFiltroCategoria(cat.getNome());
                    Toast.makeText(ContasActivity.this,
                            "Filtrando: " + cat.getNome(), Toast.LENGTH_SHORT).show();
                    aplicarFiltros();
                    dialog.dismiss();
                });

                recycler.setAdapter(adapter);
                adapter.submitList(listaCategorias);

                dialog.findViewById(R.id.btnCancelarDialog)
                        .setOnClickListener(v -> dialog.dismiss());

                dialog.show();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recarregarDadosAposMutacao() {
        viewModel.invalidarDados();
        viewModel.fetchDados(ehAtalho, null);
    }

    private void verificarPaginacaoProativaAposSubmit(int totalFiltrado) {
        // 1. Trava de Rede: Se já estiver carregando, aborta imediatamente para evitar loop de concorrência.
        if (Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

        // 2. Trava de Exaustão (Banco de Dados): Se a nuvem avisou que acabou, não importa o tamanho da lista.
        if (ehAtalho && viewModel.isUltimaPaginaFuturo()) return;
        if (!ehAtalho && viewModel.isUltimaPaginaHistorico()) return;

        // 3. Trava de Limiar da UI (Sua proteção original mantida):
        // Se já temos itens suficientes na tela para dar scroll, não precisamos ser proativos agora.
        if (totalFiltrado >= LIMIAR_PAGINACAO_PROATIVA) return;

        // 4. Execução Limpa: Se sobreviveu a todas as travas, é 100% seguro buscar mais.
        if (ehAtalho) {
            viewModel.carregarMaisFuturo();
        } else {
            viewModel.carregarMaisHistorico();
        }
    }

    // ── Auxiliar: nome da categoria ativa para reconstrução do label ──────────
    //
    // Retorna categoriaNomeFiltro se há filtro de categoria ativo, null caso
    // contrário. Usado por abrirDataPicker() para compor o label sem fazer
    // nova query ao Firestore — o nome já foi salvo no momento do clique.
    private String obterNomeCategoriaAtiva() {
        return (categoriaIdFiltro != null) ? categoriaNomeFiltro : null;
    }

    // ── Visual do filtro de categoria ────────────────────────────────────────
    //
    // Sincroniza três elementos visuais: tint do ícone, visibilidade e texto
    // do label textPeriodoSelecionado.
    //
    // Regras:
    //   - Filtro ativo   → ícone com tint laranja + label visível
    //   - Filtro inativo → ícone com tint neutro explícito + label some se
    //                      não há filtro de data ativo também
    //
    // O label é sempre reconstruído do zero — nunca lê getText() para
    // concatenar. Isso evita acumulação de texto em chamadas consecutivas
    // e garante que o conteúdo reflete exatamente o estado atual.
    //
    // FIX bug 1: setImageTintList(null) não restaura a cor original — deixa
    // o drawable sem tint e ele renderiza cinza. Usamos uma cor neutra
    // explícita via ContextCompat para restaurar corretamente.
    //
    // FIX bug 2: label reconstruído do zero, sem prefixos desnecessários.
    //   Formato: "dd/MM/yyyy → dd/MM/yyyy  •  NomeCategoria"
    //   Cada parte só aparece se existir — sem "Período:" ou outros prefixos.
    //
    // FIX bug 3: label fica GONE quando não há conteúdo real, evitando que
    // o RecyclerView fique deslocado para baixo por um label vazio/visível.
    private void atualizarVisualFiltroCategoria(String nomeCategoria) {
        if (imgFiltroCategoria == null) return;

        if (nomeCategoria != null && !nomeCategoria.isEmpty()) {
            imgFiltroCategoria.setImageTintList(
                    ColorStateList.valueOf(Color.parseColor("#FF7043")));
        } else {
            int corNeutra = androidx.core.content.ContextCompat.getColor(
                    this, android.R.color.darker_gray);
            imgFiltroCategoria.setImageTintList(
                    ColorStateList.valueOf(corNeutra));
        }

        if (textPeriodoSelecionado == null) return;

        boolean temData      = (dataInicialFiltro != null && dataFinalFiltro != null);
        boolean temCategoria = (nomeCategoria != null && !nomeCategoria.isEmpty());

        // O valor já é exibido no textLabelFiltroValor (grudado no slider),
        // não precisa ser duplicado aqui no textPeriodoSelecionado.
        if (!temData && !temCategoria) {
            textPeriodoSelecionado.setVisibility(View.GONE);
            textPeriodoSelecionado.setText("");
            return;
        }

        // Monta o texto composto com as partes ativas, separadas por "•".
        // Usa " a " em vez de "→" para garantir renderização correta em todos os dispositivos.
        StringBuilder sb = new StringBuilder();

        if (temData) {
            sb.append(DateHelper.formatarData(dataInicialFiltro))
                    .append(" a ")
                    .append(DateHelper.formatarData(dataFinalFiltro));
        }
        if (temCategoria) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(nomeCategoria);
        }

        textPeriodoSelecionado.setText(sb.toString());
        textPeriodoSelecionado.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // CLASSE INTERNA ESTÁTICA — DialogCategoriaAdapter
    //
    // BUG 6 CORRIGIDO:
    //   ANTES: adapter anônimo capturava ContasActivity.this e listaCategorias
    //          implicitamente por closure, impedindo GC durante o dialog.
    //          ViewHolder genérico chamava findViewById() a cada bind.
    //          Sem DiffUtil — qualquer mudança rebindava toda a lista.
    //
    //   DEPOIS:
    //   - static = sem referência implícita ao outer class (ContasActivity)
    //   - ListAdapter com DiffUtil = rebind cirúrgico apenas do que mudou
    //   - CategoriaViewHolder tipado = textNome cacheado, sem findViewById() no bind
    //   - OnCategoriaClickListener = callback via interface, sem capturar this
    // =========================================================================

    private static final class DialogCategoriaAdapter
            extends ListAdapter<ContasCategoriaModel, DialogCategoriaAdapter.CategoriaViewHolder> {

        interface OnCategoriaClickListener {
            void onCategoriaClick(ContasCategoriaModel categoria);
        }

        private static final DiffUtil.ItemCallback<ContasCategoriaModel> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<ContasCategoriaModel>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull ContasCategoriaModel oldItem,
                                                   @NonNull ContasCategoriaModel newItem) {
                        return Objects.equals(oldItem.getId(), newItem.getId());
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull ContasCategoriaModel oldItem,
                                                      @NonNull ContasCategoriaModel newItem) {
                        return Objects.equals(oldItem.getNome(), newItem.getNome());
                    }
                };

        @NonNull
        private final OnCategoriaClickListener clickListener;

        DialogCategoriaAdapter(@NonNull OnCategoriaClickListener clickListener) {
            super(DIFF_CALLBACK);
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public CategoriaViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.adapter_item_dialog_categoria, parent, false);
            return new CategoriaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position) {
            holder.bind(getItem(position), clickListener);
        }

        static final class CategoriaViewHolder extends RecyclerView.ViewHolder {

            private final TextView textNome;

            CategoriaViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                textNome = itemView.findViewById(R.id.textNomeCategoria);
            }

            void bind(ContasCategoriaModel categoria, OnCategoriaClickListener listener) {
                textNome.setText(categoria.getNome());
                itemView.setOnClickListener(v -> listener.onCategoriaClick(categoria));
            }
        }
    }
}