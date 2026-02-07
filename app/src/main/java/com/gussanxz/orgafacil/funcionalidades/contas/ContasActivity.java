package com.gussanxz.orgafacil.funcionalidades.contas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.repository.ResumoFinanceiroRepository;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.HelperExibirDatasMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual.ContasViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ContasActivity (MVVM)
 * - Exibe o saldo consolidado e extrato agrupado por data.
 * - REGRA DE OURO: Dinheiro no código é int (centavos), dividido por 100 apenas na UI [cite: 2026-02-07].
 */
public class ContasActivity extends AppCompatActivity {

    private final String TAG = "ContasActivity";
    private boolean ehAtalho = false;
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

        // Inicialização da ViewModel (Cérebro da tela)
        viewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        // Configuração da Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("OrgaFácil");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        // Inicialização dos Repositories
        movRepository = new MovimentacaoRepository();
        resumoRepository = new ResumoFinanceiroRepository();
        usuarioRepository = new UsuarioRepository();

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();
        setupObservers(); // Conecta a UI aos dados vivos da ViewModel

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == RESULT_OK) carregarDados(); }
        );

        extrasAtalho = (getIntent() != null) ? getIntent().getExtras() : null;
        ehAtalho = (extrasAtalho != null) && extrasAtalho.getBoolean("EH_ATALHO", false);
        if (ehAtalho) aplicarRegrasAtalho(extrasAtalho);
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    // --- CONEXÃO COM A VIEWMODEL (OBSERVERS) ---

    private void setupObservers() {
        // Observa mudanças na lista filtrada para atualizar o RecyclerView
        viewModel.listaFiltrada.observe(this, lista -> {
            itensAgrupados.clear();
            itensAgrupados.addAll(HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista));
            adapterAgrupado.notifyDataSetChanged();
            atualizarLegendasFiltro(searchView.getQuery().toString());
        });

        // Observa o saldo calculado do período/filtro (em centavos)
        viewModel.saldoPeriodo.observe(this, saldoCentavos -> {
            // Conversão de centavos (int) para double apenas para exibição [cite: 2026-02-07]
            double saldoExibicao = saldoCentavos / 100.0;
            textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoExibicao));
        });
    }

    // --- MÉTODOS DE DADOS ---

    private void carregarDados() {
        // 1. Identidade: Busca apenas o nome para a saudação
        usuarioRepository.obterNomeUsuario(nome -> textoSaudacao.setText("Olá, " + nome + "!"));

        // 2. Dashboard: Escuta o documento de resumo em tempo real.
        // CORREÇÃO: Usamos Classe Anônima (new Callback) em vez de Lambda para evitar erro de "Multiple abstract methods".
        resumoRepository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
            @Override
            public void onUpdate(ResumoFinanceiroModel resumo) {
                if (resumo != null) {
                    // Só atualiza o saldo principal se não houver filtros de pesquisa/data ativos
                    if (dataInicialFiltro == null && searchView.getQuery().length() == 0) {
                        // REGRA DE OURO: O saldo é INT (centavos). Convertemos para exibição [cite: 2026-02-07]
                        double saldoDouble = resumo.getSaldo_atual() / 100.0;
                        textoSaldo.setText(String.format(Locale.getDefault(), "R$ %.2f", saldoDouble));
                    }
                }
            }

            @Override
            public void onError(String erro) {

            }
            // Se sua interface tiver onErro, você pode implementar aqui ou deixar vazio se for default
        });

        recuperarMovimentacoesDoBanco();
    }

    private void recuperarMovimentacoesDoBanco() {
        // CORREÇÃO: Implementação da busca real no banco de dados
        movRepository.recuperarMovimentacoes(new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                // Passa a lista bruta para a ViewModel, que aplicará os filtros
                viewModel.carregarLista(lista);
                // aplicarFiltros() é chamado internamente pela ViewModel ao carregar a lista
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro ao atualizar extrato.", Toast.LENGTH_SHORT).show();
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
            // Repository gerencia a exclusão e o estorno automático do saldo [cite: 2026-02-07]
            movRepository.excluir(mov, new MovimentacaoRepository.Callback() {
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
        // Delega a lógica de filtro para a ViewModel, mantendo a Activity limpa
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
                }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapterAgrupado);

        new ItemTouchHelper(new SwipeCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                MovimentacaoModel m = itensAgrupados.get(pos).movimentacaoModel;
                if (dir == ItemTouchHelper.LEFT) confirmarExclusao(m, pos);
                else { adapterAgrupado.notifyItemChanged(pos); abrirTelaEdicao(m); }
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

    private void atualizarLegendasFiltro(String query) {
        if (ehAtalho) textSaldoAtual.setText("Saldo futuro");
        else if (dataInicialFiltro != null) textSaldoAtual.setText("Saldo do período");
        else if (!query.isEmpty()) textSaldoAtual.setText("Saldo da pesquisa");
        else textSaldoAtual.setText("Saldo total");
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
        Log.i(TAG, "Regras de atalho aplicada!");
    }
}