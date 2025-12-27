package com.gussanxz.orgafacil.activity.contas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.SearchView;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.LoginActivity;
import com.gussanxz.orgafacil.activity.MainActivity;
import com.gussanxz.orgafacil.adapter.MovimentoItem;
import com.gussanxz.orgafacil.adapter.MovimentosAgrupadosAdapter;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.adapter.MovimentacoesGrouper;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.Usuario;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContasActivity extends AppCompatActivity {

    private TextView textoSaudacao, textoSaldo, textoSaldoLegenda;
    private Double despesaTotal = 0.0;
    private Double proventosTotal = 0.0;
    private Double resumoUsuario = 0.0;

    private FirebaseAuth autenticacao  = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private DatabaseReference usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private DatabaseReference movimentacaoRef;

    private ValueEventListener valueEventListenerUsuario;
    private ValueEventListener valueEventListenerMovimentacoes;

    private RecyclerView recyclerView;

    // Lista base de movimentações (vinda do Firebase, já considerando filtro de datas)
    private final List<Movimentacao> movimentacoes = new ArrayList<>();

    // Lista achatada para o adapter (headers + linhas)
    private final List<MovimentoItem> itensAgrupados = new ArrayList<>();
    private MovimentosAgrupadosAdapter adapterAgrupado;

    private Movimentacao movimentacao; // usado para atualizar saldo ao excluir
    private ActivityResultLauncher<Intent> launcher;

    private SearchView searchView;
    private EditText editDataInicial, editDataFinal;
    private String dataInicialSelecionada, dataFinalSelecionada;
    private ImageView imgLimparFiltroData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contas);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("OrgaFácil");

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "null";
        Log.i("DEBUG_FIREBASE", "UID atual: " + uid);

        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        textoSaldoLegenda = findViewById(R.id.textView16);
        recyclerView = findViewById(R.id.recyclesMovimentos);

        editDataInicial = findViewById(R.id.editDataInicial);
        editDataFinal = findViewById(R.id.editDataFinal);
        imgLimparFiltroData = findViewById(R.id.imgLimparFiltroData);

        editDataInicial.setOnClickListener(v -> abrirDatePicker(true));
        editDataFinal.setOnClickListener(v -> abrirDatePicker(false));

        limparFiltroData();
        swipe();

        searchView = findViewById(R.id.searchViewEventos);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtrarMovimentacoes(newText);
                return true;
            }
        });

        // Adapter agrupado (header por dia + linhas)
        adapterAgrupado = new MovimentosAgrupadosAdapter(this, itensAgrupados);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapterAgrupado);

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        atualizarSaldo(); // atualiza totais do usuário
                        if (dataInicialSelecionada != null || dataFinalSelecionada != null) {
                            recuperarMovimentacoesComFiltro();
                        } else {
                            recuperarMovimentacoes();
                        }
                        // reaplica filtro de texto (se houver)
                        aplicarFiltroTextoEAtualizarLista();
                    }
                }
        );
    }

    // ===============================
    // FILTRO POR TEXTO (SEARCHVIEW)
    // ===============================

    private void filtrarMovimentacoes(String texto) {
        // base = lista vinda do Firebase (já filtrada por data, se houver)
        List<Movimentacao> base = new ArrayList<>(movimentacoes);
        List<Movimentacao> filtrada = new ArrayList<>();

        if (texto == null || texto.trim().isEmpty()) {
            filtrada.addAll(base);
        } else {
            String query = texto.toLowerCase(Locale.getDefault());
            for (Movimentacao m : base) {
                if (m == null) continue;
                String cat = m.getCategoria() != null ? m.getCategoria().toLowerCase(Locale.getDefault()) : "";
                String desc = m.getDescricao() != null ? m.getDescricao().toLowerCase(Locale.getDefault()) : "";
                if (cat.contains(query) || desc.contains(query)) {
                    filtrada.add(m);
                }
            }
        }

        atualizarListaAgrupada(filtrada);
    }

    private void aplicarFiltroTextoEAtualizarLista() {
        String textoBusca = searchView != null ? searchView.getQuery().toString() : "";
        if (textoBusca == null || textoBusca.trim().isEmpty()) {
            atualizarListaAgrupada(movimentacoes);
        } else {
            filtrarMovimentacoes(textoBusca);
        }
    }

    private void atualizarListaAgrupada(List<Movimentacao> base) {
        itensAgrupados.clear();
        itensAgrupados.addAll(MovimentacoesGrouper.agruparPorDiaOrdenar(base));
        adapterAgrupado.notifyDataSetChanged();
    }

    // ===============================
    // FILTRO POR DATA (DATEPICKER)
    // ===============================

    private void abrirDatePicker(boolean isInicial) {
        Calendar calendar = Calendar.getInstance();
        int year  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day   = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dataSelecionada = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    if (isInicial) {
                        dataInicialSelecionada = dataSelecionada;
                        editDataInicial.setText(dataSelecionada);
                    } else {
                        dataFinalSelecionada = dataSelecionada;
                        editDataFinal.setText(dataSelecionada);
                    }
                    recuperarMovimentacoesComFiltro();
                },
                year, month, day
        );
        datePicker.show();
    }

    private void recuperarMovimentacoesComFiltro() {
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        movimentacaoRef = firebaseRef.child("movimentacao").child(idUsuario);

        valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movimentacoes.clear();

                for (DataSnapshot mesAnoSnap : snapshot.getChildren()) {
                    for (DataSnapshot movSnap : mesAnoSnap.getChildren()) {
                        Movimentacao mov = movSnap.getValue(Movimentacao.class);
                        if (mov == null) continue;
                        mov.setKey(movSnap.getKey());

                        if (estaDentroDoIntervalo(mov)) {
                            movimentacoes.add(mov);
                        }
                    }
                }

                // Recalcula o saldo do intervalo (se datas definidas)
                if (dataInicialSelecionada != null && dataFinalSelecionada != null) {
                    double totalReceitas = 0.0;
                    double totalDespesas = 0.0;

                    for (Movimentacao m : movimentacoes) {
                        if (m == null) continue;
                        if ("r".equals(m.getTipo())) {
                            totalReceitas += m.getValor();
                        } else if ("d".equals(m.getTipo())) {
                            totalDespesas += m.getValor();
                        }
                    }

                    double saldoIntervalo = totalReceitas - totalDespesas;
                    DecimalFormat decimalFormat = new DecimalFormat("0.##");
                    String resultadoFormatado = decimalFormat.format(saldoIntervalo);

                    textoSaldo.setText("R$ " + resultadoFormatado);
                    textoSaldoLegenda.setText("Saldo entre " + dataInicialSelecionada + " e " + dataFinalSelecionada);
                } else {
                    textoSaldoLegenda.setText("Saldo atual");
                }

                // Monta lista agrupada (respeitando filtro de texto, se houver)
                aplicarFiltroTextoEAtualizarLista();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean estaDentroDoIntervalo(Movimentacao mov) {
        if (dataInicialSelecionada == null || dataFinalSelecionada == null) return true;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dataMov = sdf.parse(mov.getData());
            Date dataInicial = sdf.parse(dataInicialSelecionada);
            Date dataFinal = sdf.parse(dataFinalSelecionada);

            return dataMov != null && !dataMov.before(dataInicial) && !dataMov.after(dataFinal);
        } catch (Exception e) {
            return true; // fallback seguro
        }
    }

    public void limparFiltroData() {
        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText("");
            editDataFinal.setText("");
            dataInicialSelecionada = null;
            dataFinalSelecionada = null;

            textoSaldoLegenda.setText("Saldo atual");

            recuperarMovimentacoes(); // reexibe tudo
        });
    }

    // ===============================
    // SWIPE: EDITAR / EXCLUIR
    // ===============================

    public void swipe(){

        ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= itensAgrupados.size()) return;

                MovimentoItem item = itensAgrupados.get(position);
                // Não desenha swipe em headers
                if (item.type == MovimentoItem.TYPE_HEADER) return;

                View itemView = viewHolder.itemView;
                Paint backgroundPaint = new Paint();
                Paint textPaint = new Paint();

                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(40f);
                textPaint.setAntiAlias(true);

                float textY = itemView.getTop() + itemView.getHeight() / 2f + 15;

                if (dX > 0) { // Direita - Editar
                    backgroundPaint.setColor(Color.parseColor("#4CAF50"));
                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                            (float) itemView.getBottom(), backgroundPaint);
                    c.drawText("Editar", itemView.getLeft() + 50, textY, textPaint);
                } else if (dX < 0) { // Esquerda - Excluir
                    backgroundPaint.setColor(Color.parseColor("#F44336"));
                    c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom(), backgroundPaint);
                    c.drawText("Excluir", itemView.getRight() - 200, textY, textPaint);
                }
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < itensAgrupados.size()) {
                    MovimentoItem item = itensAgrupados.get(position);
                    // Header não pode ser arrastado
                    if (item.type == MovimentoItem.TYPE_HEADER) {
                        return makeMovementFlags(0, 0);
                    }
                }
                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position < 0 || position >= itensAgrupados.size()) {
                    adapterAgrupado.notifyDataSetChanged();
                    return;
                }

                MovimentoItem item = itensAgrupados.get(position);
                if (item.type == MovimentoItem.TYPE_HEADER) {
                    // Não faz nada, só reseta visual
                    adapterAgrupado.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.START) {
                    excluirMovimentacao(position, item.movimentacao);
                } else if (direction == ItemTouchHelper.END) {
                    editarMovimentacao(position, item.movimentacao);
                }
            }
        };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }

    public void editarMovimentacao(int position, Movimentacao mov) {
        movimentacao = mov; // guarda referência se precisar

        Intent intent = new Intent(ContasActivity.this, EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", movimentacao);
        intent.putExtra("keyFirebase", movimentacao.getKey());
        launcher.launch(intent);

        // restaura item na lista (swipe visual)
        adapterAgrupado.notifyItemChanged(position);
    }

    public void excluirMovimentacao(int positionNaListaAgrupada, Movimentacao mov){
        movimentacao = mov; // usado em atualizarSaldo()

        String tipo = movimentacao.getTipo(); // "d" ou "r"
        String tipoTexto = tipo.equals("d") ? "despesa" : "provento";

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Excluir " + tipoTexto);
        alertDialog.setMessage("Você deseja excluir este " + tipoTexto + "?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", (dialogInterface, i) -> {

            String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();

            String data = movimentacao.getData(); // ex: 25/05/2025
            String mesAno = data.substring(3, 5) + data.substring(6); // "052025"

            movimentacaoRef = firebaseRef.child("movimentacao")
                    .child(idUsuario)
                    .child(mesAno);

            movimentacaoRef.child(movimentacao.getKey()).removeValue();

            // Remove da lista base de movimentações
            for (int idx = 0; idx < movimentacoes.size(); idx++) {
                Movimentacao mBase = movimentacoes.get(idx);
                if (mBase.getKey().equals(movimentacao.getKey())) {
                    movimentacoes.remove(idx);
                    break;
                }
            }

            atualizarSaldo();
            aplicarFiltroTextoEAtualizarLista();
        });

        alertDialog.setNegativeButton("Cancelar", (dialogInterface, i) -> {
            Toast.makeText(ContasActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
            adapterAgrupado.notifyDataSetChanged(); // restaura visual do item
        });

        alertDialog.show();
    }

    // ===============================
    // SALDO DO USUÁRIO
    // ===============================

    public void atualizarSaldo(){

        if (movimentacao == null) return;

        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        if ("r".equals(movimentacao.getTipo())) {
            proventosTotal = proventosTotal - movimentacao.getValor();
            usuarioRef.child("proventosTotal").setValue(proventosTotal);
        }
        if ("d".equals(movimentacao.getTipo())) {
            despesaTotal = despesaTotal - movimentacao.getValor();
            usuarioRef.child("despesaTotal").setValue(despesaTotal);
        }
    }

    public void recuperarMovimentacoes(){
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        movimentacaoRef = firebaseRef.child("movimentacao").child(idUsuario);

        valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movimentacoes.clear();

                for (DataSnapshot mesAnoSnap : snapshot.getChildren()) {
                    for (DataSnapshot movSnap : mesAnoSnap.getChildren()) {
                        Movimentacao mov = movSnap.getValue(Movimentacao.class);
                        if (mov == null) continue;
                        mov.setKey(movSnap.getKey());
                        movimentacoes.add(mov);
                    }
                }

                aplicarFiltroTextoEAtualizarLista();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

    }

    public void recuperarResumo(){
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        Log.i("onStart", "Evento exibir dados adicionado");
        valueEventListenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Usuario usuario = snapshot.getValue( Usuario.class );
                if (usuario == null) return;

                despesaTotal = usuario.getDespesaTotal();
                proventosTotal = usuario.getProventosTotal();
                resumoUsuario = proventosTotal - despesaTotal;

                DecimalFormat decimalFormat = new DecimalFormat("0.##");
                String resultadoFormatado = decimalFormat.format( resumoUsuario );

                textoSaudacao.setText("Ola, " + usuario.getNome() + "!");
                textoSaldo.setText( "R$ " + resultadoFormatado );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ===============================
    // MENU / NAVEGAÇÃO
    // ===============================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuSair){
            autenticacao.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void adicionarProventos(View view){
        startActivity(new Intent(this, ProventosActivity.class));
    }

    public void adicionarDespesa(View view){
        startActivity(new Intent(this, DespesasActivity.class));
    }

    public void deslogarUsuario(View view){
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signOut();
        startActivity(new Intent(this, LoginActivity.class));
    }

    // ===============================
    // CICLO DE VIDA
    // ===============================

    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacoesComFiltro();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", "Evento exibir dados removido");
        if (usuarioRef != null && valueEventListenerUsuario != null) {
            usuarioRef.removeEventListener(valueEventListenerUsuario);
        }
        if (movimentacaoRef != null && valueEventListenerMovimentacoes != null) {
            movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
        }
    }
}
