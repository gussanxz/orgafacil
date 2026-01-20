package com.gussanxz.orgafacil.activity.main.contas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.LoginActivity;
import com.gussanxz.orgafacil.activity.main.MainActivity;
import com.gussanxz.orgafacil.adapter.MovimentoItem;
import com.gussanxz.orgafacil.adapter.MovimentacoesGrouper;
import com.gussanxz.orgafacil.adapter.MovimentosAgrupadosAdapter;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.repository.ContasRepository;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContasActivity extends AppCompatActivity {

    // UI
    private TextView textoSaudacao, textoSaldo, textoSaldoLegenda;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private ImageView imgLimparFiltroData;

    // Dados
    private final List<Movimentacao> listaCompleta = new ArrayList<>();
    private final List<MovimentoItem> itensAgrupados = new ArrayList<>();
    private MovimentosAgrupadosAdapter adapterAgrupado;
    private String dataInicialSelecionada, dataFinalSelecionada;

    // Dependências
    private ContasRepository repository; // <--- A MÁGICA
    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_lista_saldo);

        // Configurações iniciais
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("OrgaFácil");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        repository = new ContasRepository(); // Instancia o repositório

        inicializarComponentes();
        configurarRecyclerView();
        configurarFiltros();

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) carregarDados();
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarDados();
    }

    // --- MÉTODOS PRINCIPAIS QUE FICARAM MUITO MENORES ---

    private void carregarDados() {
        // 1. Busca Apenas o Nome (O saldo será sobrescrito pelo filtro depois)
        repository.recuperarResumo((saldo, nome) -> {
            textoSaudacao.setText("Olá, " + nome + "!");
            // Não precisamos mais setar o textoSaldo aqui,
            // pois o "recuperarMovimentacoes" abaixo vai chamar o "aplicarFiltros"
            // que vai calcular e exibir o saldo correto.
        });

        // 2. Busca Movimentações
        repository.recuperarMovimentacoes(new ContasRepository.DadosCallback() {
            @Override
            public void onSucesso(List<Movimentacao> lista) {
                listaCompleta.clear();
                listaCompleta.addAll(lista);

                // Isso aqui vai rodar o código acima e exibir o saldo certo
                aplicarFiltros();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarExclusao(Movimentacao mov) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir")
                .setMessage("Deseja excluir: " + mov.getDescricao() + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> {

                    // Chama o repositório para excluir
                    repository.excluirMovimentacao(mov, new ContasRepository.SimplesCallback() {
                        @Override
                        public void onSucesso() {
                            Toast.makeText(ContasActivity.this, "Excluído!", Toast.LENGTH_SHORT).show();
                            // Remove da lista localmente para ser rápido
                            listaCompleta.removeIf(m -> m.getKey().equals(mov.getKey()));
                            carregarDados(); // Recarrega saldo
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ContasActivity.this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- LÓGICA DE FILTROS (MANTIDA NA ACTIVITY POIS É VISUAL) ---

    private void aplicarFiltros() {
        String texto = searchView.getQuery().toString();
        List<Movimentacao> temp = new ArrayList<>();

        // Variáveis para recalcular o saldo da tela
        double totalReceitasFiltradas = 0.0;
        double totalDespesasFiltradas = 0.0;

        for (Movimentacao m : listaCompleta) {
            // 1. Aplica Filtro de Data
            if (!estaNoPeriodo(m)) continue;

            // 2. Aplica Filtro de Texto (Busca)
            if (texto.isEmpty() ||
                    m.getDescricao().toLowerCase().contains(texto.toLowerCase()) ||
                    m.getCategoria().toLowerCase().contains(texto.toLowerCase())) {

                // Se passou nos filtros, adiciona na lista visual
                temp.add(m);

                // --- A MÁGICA: SOMA O VALOR AQUI ---
                if ("r".equals(m.getTipo())) {
                    totalReceitasFiltradas += m.getValor();
                } else {
                    totalDespesasFiltradas += m.getValor();
                }
            }
        }

        // 3. Atualiza o Adapter (Lista visual)
        itensAgrupados.clear();
        itensAgrupados.addAll(MovimentacoesGrouper.agruparPorDiaOrdenar(temp));
        adapterAgrupado.notifyDataSetChanged();

        // 4. Atualiza o TEXTO DO SALDO com o valor recalculado
        double saldoFiltrado = totalReceitasFiltradas - totalDespesasFiltradas;
        DecimalFormat df = new DecimalFormat("0.##");
        textoSaldo.setText("R$ " + df.format(saldoFiltrado));

        // 5. Atualiza a legenda para o usuário saber o que está vendo
        if (dataInicialSelecionada != null) {
            textoSaldoLegenda.setText("Saldo do período");
        } else if (!texto.isEmpty()) {
            textoSaldoLegenda.setText("Saldo da pesquisa");
        } else {
            textoSaldoLegenda.setText("Saldo total"); // Sem filtros
        }
    }

    private boolean estaNoPeriodo(Movimentacao m) {
        if (dataInicialSelecionada == null || dataFinalSelecionada == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dM = sdf.parse(m.getData());
            Date dI = sdf.parse(dataInicialSelecionada);
            Date dF = sdf.parse(dataFinalSelecionada);
            return dM != null && !dM.before(dI) && !dM.after(dF);
        } catch (Exception e) { return true; }
    }

    // --- CONFIGURAÇÕES DE UI ---

    private void inicializarComponentes() {
        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        textoSaldoLegenda = findViewById(R.id.textView16);
        recyclerView = findViewById(R.id.recyclesMovimentos);
        editDataInicial = findViewById(R.id.editDataInicial);
        editDataFinal = findViewById(R.id.editDataFinal);
        imgLimparFiltroData = findViewById(R.id.imgLimparFiltroData);
        searchView = findViewById(R.id.searchViewEventos);
    }

    private void configurarRecyclerView() {
        adapterAgrupado = new MovimentosAgrupadosAdapter(this, itensAgrupados, this::confirmarExclusao);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapterAgrupado);
    }

    private void configurarFiltros() {
        editDataInicial.setOnClickListener(v -> abrirDataPicker(true));
        editDataFinal.setOnClickListener(v -> abrirDataPicker(false));

        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText("");
            editDataFinal.setText("");
            dataInicialSelecionada = null;
            dataFinalSelecionada = null;
            carregarDados();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                aplicarFiltros();
                return true;
            }
        });
    }

    private void abrirDataPicker(boolean isInicio) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String data = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
            if (isInicio) { dataInicialSelecionada = data; editDataInicial.setText(data); }
            else { dataFinalSelecionada = data; editDataFinal.setText(data); }
            aplicarFiltros();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // --- MENU ---
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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void adicionarProventos(View v) { startActivity(new Intent(this, ProventosActivity.class)); }
    public void adicionarDespesa(View v) { startActivity(new Intent(this, DespesasActivity.class)); }
}