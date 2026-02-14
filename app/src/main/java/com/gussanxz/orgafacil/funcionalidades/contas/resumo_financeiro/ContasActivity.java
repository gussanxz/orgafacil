package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.repository.ResumoFinanceiroRepository;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.HelperExibirDatasMovimentacao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ContasActivity Refatorada
 * Centraliza a exibição de movimentações e contas futuras através do ViewModel.
 * [ATUALIZADO]: Ajustado para consumir o ViewModel segregado (Histórico vs Futuro).
 */
public class ContasActivity extends AppCompatActivity {

    private final String TAG = "ContasActivity";
    private boolean ehAtalho = false; // Define se é modo Futuro (true) ou Histórico (false)
    private Bundle extrasAtalho = null;

    // UI Components
    private TextView textoSaudacao, textoSaldo, textSaldoAtual;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private ImageView imgLimparFiltroData;

    // Estado e Visualização
    private final List<ExibirItemListaMovimentacaoContas> itensAgrupados = new ArrayList<>();
    private AdapterExibeListaMovimentacaoContas adapterAgrupado;
    private Date dataInicialFiltro, dataFinalFiltro;

    // Motores de Dados (Repositories e ViewModel)
    private ContasViewModel viewModel;
    private MovimentacaoRepository movRepository;
    private ResumoFinanceiroRepository resumoRepository;
    private UsuarioRepository usuarioRepository;

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_lista_saldo);

        // Inicialização da ViewModel
        viewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("OrgaFácil");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        movRepository = new MovimentacaoRepository();
        resumoRepository = new ResumoFinanceiroRepository();
        usuarioRepository = new UsuarioRepository();

        // Lógica de Identificação de Modo (Histórico vs Futuras)
        extrasAtalho = (getIntent() != null) ? getIntent().getExtras() : null;
        ehAtalho = (extrasAtalho != null) && extrasAtalho.getBoolean("EH_ATALHO", false);

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();

        // Configura os observadores ANTES de carregar os dados
        setupObservers();

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == RESULT_OK) carregarDados(); }
        );

        if (ehAtalho) aplicarRegrasAtalho(extrasAtalho);
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    // --- CONEXÃO COM A VIEWMODEL (OBSERVERS) ---

    private void setupObservers() {
        // [CORREÇÃO]: Observador genérico para atualizar a UI
        Observer<List<MovimentacaoModel>> observerUI = lista -> {
            itensAgrupados.clear();
            // O Helper usa 'ehAtalho' (modo futuro) para decidir se ordena Crescente ou Decrescente
            itensAgrupados.addAll(HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehAtalho));
            adapterAgrupado.notifyDataSetChanged();
            atualizarLegendasFiltro(searchView.getQuery().toString());
        };

        // Decide qual LiveData observar com base no modo da Activity
        if (ehAtalho) {
            viewModel.listaFutura.observe(this, observerUI);
        } else {
            viewModel.listaHistorico.observe(this, observerUI);
        }

        viewModel.saldoPeriodo.observe(this, saldoCentavos -> {
            // [PRECISÃO] Converte centavos (long) para exibição apenas na UI
            double saldoExibicao = saldoCentavos / 100.0;
            textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoExibicao));
        });
    }

    // --- MÉTODOS DE DADOS ---

    private void carregarDados() {
        usuarioRepository.obterNomeUsuario(nome -> textoSaudacao.setText("Olá, " + nome + "!"));

        // Escuta o resumo geral apenas para o saldo total quando não houver filtros
        resumoRepository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
            @Override
            public void onUpdate(ResumoFinanceiroModel resumo) {
                // Só atualiza o saldo global se o usuário não estiver filtrando nada
                if (resumo != null && dataInicialFiltro == null && searchView.getQuery().length() == 0) {
                    int saldoCentavos = 0;
                    if (resumo.getBalanco() != null) {
                        saldoCentavos = resumo.getBalanco().getSaldoAtual();
                    }
                    double saldoDouble = saldoCentavos / 100.0;
                    textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoDouble));
                }
            }
            @Override public void onError(String erro) { Log.e(TAG, "Erro no resumo: " + erro); }
        });

        recuperarMovimentacoesDoBanco();
    }

    /**
     * Delega a busca para o ViewModel.
     * [CORREÇÃO]: Passa o modo 'ehAtalho' explicitamente para o fetchDados.
     */
    private void recuperarMovimentacoesDoBanco() {
        viewModel.fetchDados(movRepository, ehAtalho, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                // ViewModel processa a lista e notifica o Observer em setupObservers
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro ao atualizar dados: " + erro, Toast.LENGTH_SHORT).show();
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

        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            movRepository.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Toast.makeText(ContasActivity.this, "Lançamento excluído!", Toast.LENGTH_SHORT).show();
                    carregarDados(); // Recarrega para atualizar saldo global e lista
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
    }

    private void configurarRecyclerView() {
        adapterAgrupado = new AdapterExibeListaMovimentacaoContas(
                this,
                itensAgrupados,
                new AdapterExibeListaMovimentacaoContas.OnItemActionListener() {
                    @Override public void onDeleteClick(MovimentacaoModel m) { confirmarExclusao(m, -1); }
                    @Override public void onLongClick(MovimentacaoModel m) { abrirTelaEdicao(m); }
                    @Override public void onCheckClick(MovimentacaoModel m) { confirmarPagamentoOuRecebimento(m); }
                }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapterAgrupado);

        // Adiciona funcionalidade de Swipe (Arrastar para os lados)
        new ItemTouchHelper(new SwipeCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                // Verifica se a posição é um cabeçalho (não tem model) ou item
                if (itensAgrupados.get(pos).type == ExibirItemListaMovimentacaoContas.TYPE_MOVIMENTO) {
                    MovimentacaoModel m = itensAgrupados.get(pos).movimentacaoModel;
                    if (dir == ItemTouchHelper.LEFT) confirmarExclusao(m, pos);
                    else { adapterAgrupado.notifyItemChanged(pos); abrirTelaEdicao(m); }
                } else {
                    adapterAgrupado.notifyItemChanged(pos); // Ignora swipe em headers
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

        new AlertDialog.Builder(this)
                .setTitle("Confirmar " + acao)
                .setMessage("Você confirma que '" + mov.getDescricao() + "' foi concluído?")
                .setPositiveButton("Sim, confirmar", (dialog, which) -> {

                    movRepository.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            Toast.makeText(ContasActivity.this, "Lançamento confirmado!", Toast.LENGTH_SHORT).show();

                            // [CORREÇÃO]: Atualiza AMBOS os fluxos no ViewModel, pois o item muda de estado
                            viewModel.fetchDados(movRepository, true, null);  // Atualiza Futuros
                            viewModel.fetchDados(movRepository, false, null); // Atualiza Histórico
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Agora não", null)
                .show();
    }

    /**
     * Atualiza o rótulo do saldo com base no contexto (Modo ou Filtro).
     */
    private void atualizarLegendasFiltro(String query) {
        // [CORREÇÃO]: Usa a flag local 'ehAtalho' em vez de perguntar ao ViewModel
        if (ehAtalho) textSaldoAtual.setText("Saldo futuro estimado");
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
        startActivity(new Intent(this, ReceitasActivity.class));
    }

    public void adicionarDespesa(View v) {
        startActivity(new Intent(this, DespesasActivity.class));
    }

    private void aplicarRegrasAtalho(Bundle extras) {
        Log.i(TAG, "Exibindo tela em modo de Contas Futuras via atalho.");
    }
}