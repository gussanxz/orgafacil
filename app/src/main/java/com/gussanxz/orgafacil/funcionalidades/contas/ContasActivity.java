package com.gussanxz.orgafacil.funcionalidades.contas;

import static androidx.core.content.ContentProviderCompat.requireContext;

import static java.security.AccessController.getContext;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.repository.ResumoFinanceiroRepository;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.HelperExibirDatasMovimentacao;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ContasActivity Refatorada
 * Centraliza a exibição de movimentações e contas futuras através do ViewModel.
 * Agora com sistema de Virada de Mês e Scroll Infinito para os dois lados.
 */
public class ContasActivity extends AppCompatActivity {

    private final String TAG = "ContasActivity";
    private boolean ehAtalho = false; // Define se é modo Futuro (true) ou Histórico (false)
    private boolean isPrimeiroCarregamento = true; // [NOVO] Evita piscar a tela
    private Bundle extrasAtalho = null;
    private Long ultimoSaldoCarregado = null;

    // UI Components
    private TextView textoSaudacao, textoSaldo, textSaldoAtual;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private ImageView imgLimparFiltroData;
    private Button btnNovaDespesa, btnNovaReceita;
    private ProgressBar progressBarPaginacao;

    // Estado e Visualização
    private final List<AdapterItemListaMovimentacao> itensAgrupados = new ArrayList<>();
    private AdapterMovimentacaoLista adapterAgrupado;
    private Date dataInicialFiltro, dataFinalFiltro;

    // Motores de Dados (Repositories e ViewModel)
    private ContasViewModel viewModel;
    private ResumoFinanceiroRepository resumoRepository;
    private UsuarioRepository usuarioRepository;

    private ActivityResultLauncher<Intent> launcher;

    // UI Components do Estado Vazio
    private View layoutEmptyStateContas;
    private TextView textEmptyStateContas;
    private Button btnEmptyStateCTA;
    ImageView imgEmptyStateContas;
    private com.google.android.material.chip.ChipGroup chipGroupFiltroTipo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_movimentacoes);

        // Inicialização da ViewModel
        viewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        resumoRepository = new ResumoFinanceiroRepository();
        usuarioRepository = new UsuarioRepository();

        // Lógica de Identificação de Modo (Histórico vs Futuras)
        extrasAtalho = (getIntent() != null) ? getIntent().getExtras() : null;
        ehAtalho = (extrasAtalho != null) && extrasAtalho.getBoolean("EH_ATALHO", false);

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();

        configurarChipsFiltro();

        setupObservers();

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == RESULT_OK) carregarDados(); }
        );

        if (ehAtalho) aplicarRegrasAtalho(extrasAtalho);

        // Verifica se o mês virou logo que a tela é criada
        verificarViradaDeMes();
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    // --- VIRADA DE MÊS ---

    private void verificarViradaDeMes() {
        SharedPreferences prefs = getSharedPreferences("OrgaFacilPrefs", Context.MODE_PRIVATE);
        int mesSalvo = prefs.getInt("mes_ultimo_acesso", -1);
        int mesAtual = Calendar.getInstance().get(Calendar.MONTH);

        if (mesSalvo != -1 && mesSalvo != mesAtual) {
            Log.i(TAG, "Virada de Mês detectada! Zerando estatísticas de " + mesSalvo + " para " + mesAtual);

            viewModel.zerarEstatisticasMensais(new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Log.i(TAG, msg);
                    prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
                }

                @Override
                public void onErro(String erro) {
                    Log.e(TAG, "Falha ao zerar mês: " + erro);
                }
            });
        } else if (mesSalvo == -1) {
            prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
        }
    }

    // --- CONEXÃO COM A VIEWMODEL (OBSERVERS) ---

    private void setupObservers() {
        // [CORREÇÃO] Fica de olho se a paginação está carregando para mostrar a bolinha e esconder as views no inicio
        viewModel.carregandoPaginacao.observe(this, isCarregando -> {
            if (isCarregando) {
                progressBarPaginacao.setVisibility(View.VISIBLE);

                // Se for o primeiro carregamento da tela, esconde a lista e o estado vazio
                if (isPrimeiroCarregamento) {
                    textoSaldo.setText("--");
                    textoSaldo.setTextColor(Color.WHITE);
                    textSaldoAtual.setText("Carregando saldo...");
                }
            } else {
                progressBarPaginacao.setVisibility(View.GONE);
                isPrimeiroCarregamento = false;
            }
        });

        // Observer para atualizar a LISTA
        Observer<List<MovimentacaoModel>> observerUI = lista -> {

            // Se ainda estiver na primeira vez e o loading não acabou, ignora
            if (isPrimeiroCarregamento && Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

            // [LÓGICA DE CONTROLE]: Se a lista está vazia, mostra o CTA. Se não, mostra o RecyclerView.
            if (lista == null || lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyStateContas.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmptyStateContas.setVisibility(View.GONE);
            }

            itensAgrupados.clear();
            if (lista != null) {
                itensAgrupados.addAll(HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehAtalho));
            }
            adapterAgrupado.notifyDataSetChanged();
            atualizarLegendasFiltro(searchView.getQuery().toString());
        };

        // Observer para atualizar o SALDO
        Observer<Long> observerSaldo = saldoCentavos -> {
            if (saldoCentavos == null) return;
            ultimoSaldoCarregado = saldoCentavos;

            double saldoDouble = saldoCentavos / 100.0;
            String valorFormatado = String.format(Locale.getDefault(), "R$ %.2f", saldoDouble);

            int corSaldo;
            if (saldoCentavos > 0)      corSaldo = Color.parseColor("#4CAF50");
            else if (saldoCentavos < 0) corSaldo = Color.parseColor("#F44336");
            else                         corSaldo = Color.WHITE;

            // Configura container clicável (olho + saldo)
            View containerSaldoContas = findViewById(R.id.containerSaldoContas);
            if (containerSaldoContas != null) {
                VisibilidadeHelper.configurarVisibilidadeSaldo(
                        containerSaldoContas, textoSaldo, findViewById(R.id.imgOlhoSaldoContas),
                        valorFormatado, corSaldo);
            }

            VisibilidadeHelper.atualizarValorSaldo(textoSaldo, findViewById(R.id.imgOlhoSaldoContas),
                    valorFormatado, corSaldo);

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

    // --- MÉTODOS DE DADOS ---

    private void carregarDados() {
        usuarioRepository.obterNomeUsuario(nome -> textoSaudacao.setText("Olá, " + nome + "!"));

        if (!ehAtalho && dataInicialFiltro == null && searchView.getQuery().length() == 0) {
            resumoRepository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
                @Override
                public void onUpdate(ResumoFinanceiroModel resumo) {
                    if (resumo != null && resumo.getBalanco() != null) {

                        if (isPrimeiroCarregamento || Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) {
                            return;
                        }

                        if (ultimoSaldoCarregado != null) {
                            return;
                        }

                        // CORREÇÃO: int alterado para long
                        long saldoCentavos = resumo.getBalanco().getSaldoAtual();
                        double saldoDouble = saldoCentavos / 100.0;
                        textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoDouble));
                    }
                }
                @Override public void onError(String erro) { Log.e(TAG, "Erro no resumo: " + erro); }
            });
        }

        recuperarMovimentacoesDoBanco();
    }

    private void recuperarMovimentacoesDoBanco() {
        viewModel.fetchDados(ehAtalho, new MovimentacaoRepository.DadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> lista) { /* UI via Observer */ }
            @Override public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- EXCLUSÃO ---

    private void confirmarExclusao(MovimentacaoModel mov, int positionParaRestaurar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmar_exclusao, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        textMensagem.setText("Você deseja realmente excluir '" + mov.getDescricao() + "'?");

        btnCancelar.setOnClickListener(v -> {
            dialog.dismiss();
            if (positionParaRestaurar != -1) adapterAgrupado.notifyItemChanged(positionParaRestaurar);
        });

        // CORREÇÃO: Utilizando o viewModel para excluir
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Toast.makeText(ContasActivity.this, "Lançamento excluído!", Toast.LENGTH_SHORT).show();
                    carregarDados();
                }
                @Override public void onErro(String erro) {
                    Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                    if (positionParaRestaurar != -1) adapterAgrupado.notifyItemChanged(positionParaRestaurar);
                }
            });
        });

        dialog.show();
    }

    // --- FILTROS ---

    private void aplicarFiltros() {
        viewModel.aplicarFiltros(
                searchView.getQuery().toString(),
                dataInicialFiltro,
                dataFinalFiltro
        );
    }

    // --- UI SETUP ---

    private void inicializarComponentes() {
        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        textSaldoAtual = findViewById(R.id.textSaldoAtual);
        recyclerView = findViewById(R.id.recyclesMovimentos);
        editDataInicial = findViewById(R.id.editDataInicial);
        editDataFinal = findViewById(R.id.editDataFinal);
        imgLimparFiltroData = findViewById(R.id.imgLimparFiltroData);
        searchView = findViewById(R.id.searchViewEventos);
        btnNovaDespesa = findViewById(R.id.btnNovaDespesa);
        btnNovaReceita = findViewById(R.id.btnNovaReceita);
        progressBarPaginacao = findViewById(R.id.progressBarPaginacao);

        layoutEmptyStateContas = findViewById(R.id.layoutEmptyStateContas);
        textEmptyStateContas = findViewById(R.id.textEmptyStateContas);
        btnEmptyStateCTA = findViewById(R.id.btnEmptyStateCTA);
        imgEmptyStateContas = findViewById(R.id.imgEmptyStateContas);

        chipGroupFiltroTipo = findViewById(R.id.chipGroupFiltroTipo);

        if (ehAtalho) { // TELA DE FUTUROS
            imgEmptyStateContas.setImageResource(R.drawable.ic_event_available_24);
            textEmptyStateContas.setText("Tudo em dia. Nenhuma conta pendente 🎉");
            btnEmptyStateCTA.setText("COMEÇAR MEU PLANEJAMENTO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26A69A")));
        } else { // TELA DE HISTÓRICO
            imgEmptyStateContas.setImageResource(R.drawable.ic_receipt_long_28);
            textEmptyStateContas.setText("Você ainda não registrou movimentações.");
            btnEmptyStateCTA.setText("ADICIONAR MINHA PRIMEIRA MOVIMENTAÇÃO");
            btnEmptyStateCTA.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF7043")));
        }

        btnEmptyStateCTA.setOnClickListener(v -> {
            abrirMenuEscolha();
            v.performHapticFeedback(
                    android.view.HapticFeedbackConstants.LONG_PRESS,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Força a vibração
            );
        });
    }

    private void configurarRecyclerView() {
        adapterAgrupado = new AdapterMovimentacaoLista(
                this,
                itensAgrupados,
                new AdapterMovimentacaoLista.OnItemActionListener() {
                    @Override public void onDeleteClick(MovimentacaoModel m) { confirmarExclusao(m, -1); }
                    @Override public void onLongClick(MovimentacaoModel m) { abrirTelaEdicao(m); }
                    @Override public void onCheckClick(MovimentacaoModel m) { confirmarPagamentoOuRecebimento(m); }
                }
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterAgrupado);

        // ESPIÃO DE PAGINAÇÃO: Escuta quando o usuário rola a lista para baixo
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // dy > 0 significa rolar para baixo
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        if (ehAtalho) {
                            viewModel.carregarMaisFuturo();
                        } else {
                            viewModel.carregarMaisHistorico();
                        }
                    }
                }
            }
        });

        new ItemTouchHelper(new SwipeCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                if (itensAgrupados.get(pos).type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
                    MovimentacaoModel m = itensAgrupados.get(pos).movimentacaoModel;
                    if (dir == ItemTouchHelper.LEFT) confirmarExclusao(m, pos);
                    else { adapterAgrupado.notifyItemChanged(pos); abrirTelaEdicao(m); }
                } else {
                    adapterAgrupado.notifyItemChanged(pos);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void abrirTelaEdicao(MovimentacaoModel m) {
        Intent intent = new Intent(this, EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", m);
        launcher.launch(intent);
    }

    private void configurarFiltros() {
        editDataInicial.setOnClickListener(v -> abrirDataPicker(true));
        editDataFinal.setOnClickListener(v -> abrirDataPicker(false));

        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText(""); editDataFinal.setText("");
            dataInicialFiltro = null; dataFinalFiltro = null;
            chipGroupFiltroTipo.check(R.id.chipTodos); // ← reseta para "Todos"
            viewModel.setFiltroTipo(null);
            carregarDados();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String n) { aplicarFiltros(); return true; }
        });
    }

    private void abrirDataPicker(boolean isInicio) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d, isInicio ? 0 : 23, isInicio ? 0 : 59);
            if (isInicio) dataInicialFiltro = c.getTime(); else dataFinalFiltro = c.getTime();

            String format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
            if (isInicio) editDataInicial.setText(format); else editDataFinal.setText(format);
            aplicarFiltros();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmarPagamentoOuRecebimento(MovimentacaoModel mov) {
        String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";

        // Se for parcela com sequência, perguntar sobre as demais
        if (mov.getTotal_parcelas() > 1 && mov.getParcela_atual() < mov.getTotal_parcelas()) {
            // MUDANÇA: Use "this" ou "ContasActivity.this" ao invés de requireContext()
            new AlertDialog.Builder(ContasActivity.this)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar apenas '" + mov.getDescricao() + "' ou também antecipar todas as parcelas seguintes?")
                    .setPositiveButton("Apenas esta", (d, w) -> executarConfirmacao(mov))
                    .setNegativeButton("Esta e seguintes", (d, w) -> executarConfirmacaoEmMassa(mov))
                    .setNeutralButton("Cancelar", null)
                    .show();
        } else {
            // Fluxo normal para movimentação avulsa
            // MUDANÇA: Use "this" ou "ContasActivity.this"
            new AlertDialog.Builder(ContasActivity.this)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                    .setPositiveButton("Confirmar", (dialog, which) -> executarConfirmacao(mov))
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void executarConfirmacao(MovimentacaoModel mov) {
        viewModel.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                // MUDANÇA: Substitua isAdded() e getContext()
                if (!isFinishing()) {
                    Toast.makeText(ContasActivity.this, "Concluído!", Toast.LENGTH_SHORT).show();
                    viewModel.fetchDados(true, null);
                    viewModel.fetchDados(false, null);
                }
            }
            @Override
            public void onErro(String erro) {
                // MUDANÇA: Substitua isAdded() e getContext()
                if (!isFinishing()) Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarConfirmacaoEmMassa(MovimentacaoModel movBase) {
        viewModel.confirmarMovimentacaoEmMassa(movBase, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                // MUDANÇA: Substitua isAdded() e getContext()
                if (!isFinishing()) {
                    Toast.makeText(ContasActivity.this, msg, Toast.LENGTH_SHORT).show();
                    viewModel.fetchDados(true, null);
                    viewModel.fetchDados(false, null);
                }
            }
            @Override
            public void onErro(String erro) {
                // MUDANÇA: Substitua isAdded() e getContext()
                if (!isFinishing()) Toast.makeText(ContasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirMenuEscolha() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.layout_bottom_escolha_despesa_receita, null);
        dialog.setContentView(view);

        // Clique na Receita
        view.findViewById(R.id.btnEscolhaReceita).setOnClickListener(v -> {
            dialog.dismiss();
            adicionarReceita(null); // Chama sua função já existente
        });

        // Clique na Despesa
        view.findViewById(R.id.btnEscolhaDespesa).setOnClickListener(v -> {
            dialog.dismiss();
            adicionarDespesa(null); // Chama sua função já existente
        });

        dialog.show();
    }

    private void atualizarLegendasFiltro(String query) {
        if (ehAtalho) textSaldoAtual.setText("Total pendente");
        else if (dataInicialFiltro != null) textSaldoAtual.setText("Saldo do período");
        else if (!query.isEmpty()) textSaldoAtual.setText("Saldo da pesquisa");
        else textSaldoAtual.setText("Saldo total atual");
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

        if (ehAtalho) {
            intent.putExtra("EH_CONTA_FUTURA", true);
            intent.putExtra("TITULO_TELA", "Agendar Receita");
            intent.putExtra("EH_ATALHO", true);
        }

        launcher.launch(intent);
    }

    public void adicionarDespesa(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);

        if (ehAtalho) {
            intent.putExtra("EH_CONTA_FUTURA", true);
            intent.putExtra("TITULO_TELA", "Agendar Despesa");
            intent.putExtra("EH_ATALHO", true);
        }

        launcher.launch(intent);
    }

    private void aplicarRegrasAtalho(Bundle extras) {
        Log.i(TAG, "Exibindo tela em modo de Contas Futuras via atalho.");

        if (btnNovaDespesa != null) btnNovaDespesa.setText("Agendar Despesa");
        if (btnNovaReceita != null) btnNovaReceita.setText("Agendar Receita");
    }

    private void atualizarTextoResumo() {

        if (ehAtalho) {

            long saldoCentavos = (viewModel.saldoFuturo.getValue() != null)
                    ? viewModel.saldoFuturo.getValue()
                    : 0;

            if (saldoCentavos < 0) {
                textSaldoAtual.setText("Total a pagar");
            } else if (saldoCentavos > 0) {
                textSaldoAtual.setText("Total a receber");
            } else {
                textSaldoAtual.setText("Nenhum valor pendente");
            }

        } else {
            textSaldoAtual.setText("Saldo atual");
        }
    }

    private void configurarChipsFiltro() {
        chipGroupFiltroTipo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chipTodos)) {
                viewModel.setFiltroTipo(null); // Todos
            } else if (checkedIds.contains(R.id.chipSoReceitas)) {
                viewModel.setFiltroTipo(TipoCategoriaContas.RECEITA);
            } else if (checkedIds.contains(R.id.chipSoDespesas)) {
                viewModel.setFiltroTipo(TipoCategoriaContas.DESPESA);
            }
        });
    }
}