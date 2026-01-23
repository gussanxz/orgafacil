package com.gussanxz.orgafacil.activity.main.contas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.MainActivity;
import com.gussanxz.orgafacil.adapter.MovimentoItem;
import com.gussanxz.orgafacil.adapter.MovimentacoesGrouper;
import com.gussanxz.orgafacil.adapter.MovimentosAgrupadosAdapter;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.SwipeCallback;
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
    private ContasRepository repository;
    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_lista_saldo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("OrgaFácil");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }

        repository = new ContasRepository();

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

    // --- MÉTODOS DE DADOS ---

    private void carregarDados() {
        repository.recuperarResumo((saldo, nome) -> {
            textoSaudacao.setText("Olá, " + nome + "!");
        });

        repository.recuperarMovimentacoes(new ContasRepository.DadosCallback() {
            @Override
            public void onSucesso(List<Movimentacao> lista) {
                listaCompleta.clear();
                listaCompleta.addAll(lista);
                aplicarFiltros();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- EXCLUSÃO ---

    private void confirmarExclusao(Movimentacao mov) {
        confirmarExclusao(mov, -1);
    }

    private void confirmarExclusao(Movimentacao mov, int positionParaRestaurar) {
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

        // BOTÃO CANCELAR
        btnCancelar.setOnClickListener(v -> {
            dialog.dismiss();
            // Restaura visualmente o item (fade suave)
            if (positionParaRestaurar != -1) {
                adapterAgrupado.notifyItemChanged(positionParaRestaurar);
            }
        });

        // BOTÃO EXCLUIR
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            repository.excluirMovimentacao(mov, new ContasRepository.SimplesCallback() {
                @Override
                public void onSucesso() {
                    Toast.makeText(ContasActivity.this, "Excluído!", Toast.LENGTH_SHORT).show();
                    listaCompleta.removeIf(m -> m.getKey().equals(mov.getKey()));
                    aplicarFiltros();
                }

                @Override
                public void onErro(String erro) {
                    Toast.makeText(ContasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    // Se der erro, restaura o item
                    if (positionParaRestaurar != -1) {
                        adapterAgrupado.notifyItemChanged(positionParaRestaurar);
                    }
                }
            });
        });

        // SE CLICAR FORA (CANCELAR)
        dialog.setOnCancelListener(d -> {
            if (positionParaRestaurar != -1) {
                adapterAgrupado.notifyItemChanged(positionParaRestaurar);
            }
        });

        dialog.show();
    }

    // --- FILTROS E CÁLCULOS ---

    private void aplicarFiltros() {
        String texto = searchView.getQuery().toString();
        List<Movimentacao> temp = new ArrayList<>();

        double totalReceitasFiltradas = 0.0;
        double totalDespesasFiltradas = 0.0;

        for (Movimentacao m : listaCompleta) {
            if (!estaNoPeriodo(m)) continue;

            if (texto.isEmpty() ||
                    m.getDescricao().toLowerCase().contains(texto.toLowerCase()) ||
                    m.getCategoria().toLowerCase().contains(texto.toLowerCase())) {

                temp.add(m);

                if ("r".equals(m.getTipo())) {
                    totalReceitasFiltradas += m.getValor();
                } else {
                    totalDespesasFiltradas += m.getValor();
                }
            }
        }

        itensAgrupados.clear();
        itensAgrupados.addAll(MovimentacoesGrouper.agruparPorDiaOrdenar(temp));
        adapterAgrupado.notifyDataSetChanged();

        double saldoFiltrado = totalReceitasFiltradas - totalDespesasFiltradas;
        DecimalFormat df = new DecimalFormat("0.##");
        textoSaldo.setText("R$ " + df.format(saldoFiltrado));

        if (dataInicialSelecionada != null) {
            textoSaldoLegenda.setText("Saldo do período");
        } else if (!texto.isEmpty()) {
            textoSaldoLegenda.setText("Saldo da pesquisa");
        } else {
            textoSaldoLegenda.setText("Saldo total");
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

    // --- UI SETUP ---

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
        adapterAgrupado = new MovimentosAgrupadosAdapter(this, itensAgrupados, new MovimentosAgrupadosAdapter.OnItemActionListener() {
            @Override
            public void onDeleteClick(Movimentacao movimentacao) {
                confirmarExclusao(movimentacao);
            }

            @Override
            public void onLongClick(Movimentacao movimentacao) {
                confirmarExclusao(movimentacao);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapterAgrupado);

        // OBS: Removi o código que desligava as animações (SimpleItemAnimator).
        // Isso fará o retorno do "Cancelar Exclusão" ser mais suave (Fade in/out) em vez de seco.

        SwipeCallback swipeHelper = new SwipeCallback(this) {

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.5f; // Padrão (arrastar metade)
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= itensAgrupados.size()) return 0;

                // Trava cabeçalho
                if (itensAgrupados.get(position).type == MovimentoItem.TYPE_HEADER) {
                    return 0;
                }
                return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= itensAgrupados.size()) return;

                MovimentoItem item = itensAgrupados.get(position);
                if (item.type == MovimentoItem.TYPE_HEADER) {
                    adapterAgrupado.notifyItemChanged(position);
                    return;
                }

                Movimentacao mov = item.movimentacao;

                if (direction == ItemTouchHelper.LEFT) {
                    // EXCLUIR
                    confirmarExclusao(mov, position);

                } else if (direction == ItemTouchHelper.RIGHT) {
                    // EDITAR

                    // 1. Fecha o item IMEDIATAMENTE antes de sair
                    // Isso evita que ele fique "aberto" (verde) quando você voltar
                    adapterAgrupado.notifyItemChanged(position);

                    // 2. Abre a tela
                    abrirTelaEdicao(mov);
                }
            }
        };

        new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerView);
    }

    private void abrirTelaEdicao(Movimentacao movimentacao) {
        Intent intent = new Intent(this, com.gussanxz.orgafacil.activity.main.contas.EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", movimentacao);
        intent.putExtra("keyFirebase", movimentacao.getKey());
        intent.putExtra("valor", movimentacao.getValor());
        intent.putExtra("categoria", movimentacao.getCategoria());
        intent.putExtra("descricao", movimentacao.getDescricao());
        intent.putExtra("data", movimentacao.getData());
        intent.putExtra("tipo", movimentacao.getTipo());
        launcher.launch(intent);
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

    public void adicionarReceita(View v) { startActivity(new Intent(this, ReceitasActivity.class)); }
    public void adicionarDespesa(View v) { startActivity(new Intent(this, DespesasActivity.class)); }
}