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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasDialogHelper;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.repository.ResumoFinanceiroRepository;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.DateHelper;
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

public class ContasActivity extends AppCompatActivity {

    private final String TAG = "ContasActivity";
    private boolean ehAtalho = false;
    private boolean isPrimeiroCarregamento = true;
    private Bundle extrasAtalho = null;
    private Long ultimoSaldoCarregado = null;

    private TextView textoSaudacao, textoSaldo, textoTituloSaldo, textPeriodoSelecionado;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private ImageView imgLimparFiltroData, imgFiltroCalendario;
    private Button btnNovaDespesa, btnNovaReceita;
    private ProgressBar progressBarPaginacao;

    private final List<AdapterItemListaMovimentacao> itensAgrupados = new ArrayList<>();
    private AdapterMovimentacaoLista adapterAgrupado;
    private Date dataInicialFiltro, dataFinalFiltro;

    private ContasViewModel viewModel;
    private ResumoFinanceiroRepository resumoRepository;
    private UsuarioRepository usuarioRepository;

    private ActivityResultLauncher<Intent> launcher;

    private View layoutEmptyStateContas;
    private TextView textEmptyStateContas;
    private Button btnEmptyStateCTA;
    ImageView imgEmptyStateContas;
    private com.google.android.material.chip.ChipGroup chipGroupFiltroTipo;
    private ListenerRegistration listenerResumo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_movimentacoes);

        viewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        resumoRepository = new ResumoFinanceiroRepository();
        usuarioRepository = new UsuarioRepository();

        extrasAtalho = (getIntent() != null) ? getIntent().getExtras() : null;
        ehAtalho = (extrasAtalho != null) && extrasAtalho.getBoolean("EH_ATALHO", false);

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();
        configurarChipsFiltro();
        setupObservers();

        // O launcher apenas marca os dados como inválidos.
        // onStart() é o único responsável por disparar carregarDados() quando necessário.
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        viewModel.invalidarDados();
                        // Não chama carregarDados() aqui — onStart() será chamado
                        // logo após o retorno desta Activity e fará o fetch.
                    }
                }
        );

        if (ehAtalho) aplicarRegrasAtalho(extrasAtalho);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ponto único de disparo do carregamento de dados.
        // Cobre tanto o carregamento inicial (dadosInvalidados = true por padrão no ViewModel)
        // quanto recarregamentos após edição/exclusão/confirmação.
        if (viewModel.isDadosInvalidados()) carregarDados();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerResumo != null) {
            listenerResumo.remove();
            listenerResumo = null;
        }
    }

    private void setupObservers() {
        viewModel.carregandoPaginacao.observe(this, isCarregando -> {
            if (isCarregando) {
                progressBarPaginacao.setVisibility(View.VISIBLE);
                if (isPrimeiroCarregamento) {
                    textoSaldo.setText("--");
                    textoSaldo.setTextColor(Color.WHITE);
                    textoTituloSaldo.setText("Carregando saldo...");
                }
            } else {
                progressBarPaginacao.setVisibility(View.GONE);
                isPrimeiroCarregamento = false;
            }
        });

        Observer<List<MovimentacaoModel>> observerUI = lista -> {
            if (isPrimeiroCarregamento && Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

            if (lista == null || lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyStateContas.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmptyStateContas.setVisibility(View.GONE);
            }

            // Criamos uma lista zerada e processamos os itens
            List<AdapterItemListaMovimentacao> listaProcessada = new ArrayList<>();
            if (lista != null) {
                listaProcessada = HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehAtalho);
            }

            // Antes de submeter a lista, se ela estava vazia, preparamos a animação
            boolean listaEstavaVazia = adapterAgrupado.getCurrentList().isEmpty();
            if (listaEstavaVazia && !listaProcessada.isEmpty()) {
                recyclerView.scheduleLayoutAnimation();
            }

            adapterAgrupado.submitList(listaProcessada, () -> {
                // Se o campo de busca estiver vazio e não houver datas filtradas,
                // significa que o usuário limpou o filtro ou a lista carregou agora.
                if (searchView.getQuery().toString().isEmpty() && dataInicialFiltro == null) {
                    recyclerView.scrollToPosition(0);
                    // Ou use recyclerView.smoothScrollToPosition(0) para um efeito de deslize
                }
            });

            atualizarLegendasFiltro(searchView.getQuery().toString());
        };

        Observer<Long> observerSaldo = saldoCentavos -> {
            if (saldoCentavos == null) return;
            ultimoSaldoCarregado = saldoCentavos;

            double saldoDouble = saldoCentavos / 100.0;
            String valorFormatado = String.format(Locale.getDefault(), "R$ %.2f", saldoDouble);

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
        usuarioRepository.obterNomeUsuario(nome -> textoSaudacao.setText("Olá, " + nome + "!"));

        if (!ehAtalho && dataInicialFiltro == null && searchView.getQuery().length() == 0) {
            listenerResumo = resumoRepository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
                @Override
                public void onUpdate(ResumoFinanceiroModel resumo) {
                    if (resumo != null && resumo.getBalanco() != null) {
                        if (isPrimeiroCarregamento || Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue()) || ultimoSaldoCarregado != null) return;
                        long saldoCentavos = resumo.getBalanco().getSaldoAtual();
                        textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoCentavos / 100.0));
                    }
                }
                @Override public void onError(String erro) { Log.e(TAG, "Erro no resumo: " + erro); }
            });
        }
        recuperarMovimentacoesDoBanco();
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
                        // Invalida e deixa onStart() reagir quando a Activity voltar ao foco.
                        // Não há double-fetch pois onStart() só dispara carregarDados() uma vez.
                        viewModel.invalidarDados();
                        carregarDados();
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
                            viewModel.invalidarDados();
                            carregarDados();
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
        viewModel.aplicarFiltros(searchView.getQuery().toString(), dataInicialFiltro, dataFinalFiltro);
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

        if (ehAtalho) {
            imgEmptyStateContas.setImageResource(R.drawable.ic_event_available_24);
            textEmptyStateContas.setText("Tudo em dia. Nenhuma conta pendente 🎉");
            btnEmptyStateCTA.setText("COMEÇAR MEU PLANEJAMENTO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26A69A")));
        } else {
            imgEmptyStateContas.setImageResource(R.drawable.ic_receipt_long_28);
            textEmptyStateContas.setText("Você ainda não registrou movimentações.");
            btnEmptyStateCTA.setText("ADICIONAR MINHA PRIMEIRA MOVIMENTAÇÃO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF7043")));
        }

        btnEmptyStateCTA.setOnClickListener(v -> {
            abrirMenuEscolha();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        });

        // Adicione isso para que o filtro rode assim que a data for selecionada pelo Helper
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
        // 1. Removemos a passagem da lista 'itensAgrupados' no construtor
        adapterAgrupado = new AdapterMovimentacaoLista(this, new AdapterMovimentacaoLista.OnItemActionListener() {
            @Override public void onDeleteClick(MovimentacaoModel m) { confirmarExclusao(m, -1); }
            @Override public void onLongClick(MovimentacaoModel m) {
                new AlertDialog.Builder(ContasActivity.this).setTitle("Editar").setMessage("Você deseja editar '" + m.getDescricao() + "'?")
                        .setPositiveButton("Sim", (dialog, which) -> abrirTelaEdicao(m, true)).setNegativeButton("Cancelar", null).show();
            }
            @Override public void onCheckClick(MovimentacaoModel m) {
                // Dá a vibradinha tátil (Haptic Feedback)
                recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                confirmarPagamentoOuRecebimento(m);
            }
            @Override public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) { confirmarExclusaoDoDia(dataDia, movsDoDia); }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterAgrupado);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && (layoutManager.getChildCount() + layoutManager.findFirstVisibleItemPosition()) >= layoutManager.getItemCount()) {
                    if (ehAtalho) viewModel.carregarMaisFuturo(); else viewModel.carregarMaisHistorico();
                }
            }
        });

        new ItemTouchHelper(new SwipeCallback(this) {
            @Override
            protected void onHeaderSwipeDelete(String tituloDia, List<MovimentacaoModel> movimentos) {
                confirmarExclusaoDoDia(tituloDia, movimentos);
            }

            @Override
            protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction, int position) {
                // 2. Lemos o item deslizado diretamente da lista atual do adapter
                AdapterItemListaMovimentacao item = adapterAgrupado.getCurrentList().get(position);

                if (item.type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
                    MovimentacaoModel m = item.movimentacaoModel;
                    if (direction == ItemTouchHelper.LEFT) {
                        confirmarExclusao(m, position);
                    } else {
                        recyclerView.post(() -> adapterAgrupado.notifyItemChanged(position));

                        com.google.android.material.snackbar.Snackbar.make(recyclerView, "Abrindo edição de " + m.getDescricao() + "...", 600).show();
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
        // Agora o clique é no ícone do calendário (começando pela data inicial)
        imgFiltroCalendario.setOnClickListener(v -> abrirDataPicker(true));

        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText("");
            editDataFinal.setText("");
            dataInicialFiltro = null;
            dataFinalFiltro = null;

            // Esconde o texto do período novamente
            textPeriodoSelecionado.setVisibility(View.GONE);

            searchView.setQuery("", false);
            searchView.clearFocus();

            chipGroupFiltroTipo.check(R.id.chipTodos);
            viewModel.setFiltroTipo(null);

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

                // Abre automaticamente o calendário para escolher a data final
                Toast.makeText(this, "Selecione a data final", Toast.LENGTH_SHORT).show();
                abrirDataPicker(false);
            } else {
                cal.set(y, m, d, 23, 59, 59);
                cal.set(Calendar.MILLISECOND, 999);
                dataFinalFiltro = cal.getTime();
                editDataFinal.setText(DateHelper.formatarData(dataFinalFiltro));

                // Mostra o período selecionado bonitinho na tela
                if (dataInicialFiltro != null && dataFinalFiltro != null) {
                    textPeriodoSelecionado.setText("Período: " + DateHelper.formatarData(dataInicialFiltro) + " a " + DateHelper.formatarData(dataFinalFiltro));
                    textPeriodoSelecionado.setVisibility(View.VISIBLE);
                }

                aplicarFiltros();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        if (!ehAtalho) {
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        }

        picker.show();
    }

    private void executarConfirmacao(MovimentacaoModel mov) {
        viewModel.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, "Concluído!", Toast.LENGTH_SHORT).show();
                // invalidarDados() + fetchDados() via onStart() é o padrão para ações que
                // mudam o estado do Firestore. Não duplicamos o fetch chamando carregarDados()
                // aqui, pois a Activity permanece visível — onStart() não será re-chamado.
                // Portanto o reload explícito abaixo é necessário e correto (não é double-fetch).
                viewModel.invalidarDados();
                viewModel.fetchDados(true, null);
                viewModel.fetchDados(false, null);
            }
            @Override public void onErro(String erro) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarConfirmacaoEmMassa(MovimentacaoModel movBase) {
        viewModel.confirmarMovimentacaoEmMassa(movBase, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ContasActivity.this, msg, Toast.LENGTH_SHORT).show();
                // Mesmo padrão de executarConfirmacao: Activity continua visível,
                // reload duplo (historico + futuro) necessário e intencional aqui.
                viewModel.invalidarDados();
                viewModel.fetchDados(true, null);
                viewModel.fetchDados(false, null);
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
}