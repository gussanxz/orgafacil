package com.gussanxz.orgafacil.activity.main.contas;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.LoginActivity;
import com.gussanxz.orgafacil.activity.main.MainActivity;
import com.gussanxz.orgafacil.adapter.MovimentoItem;
import com.gussanxz.orgafacil.adapter.MovimentosAgrupadosAdapter;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.adapter.MovimentacoesGrouper;
import com.gussanxz.orgafacil.model.Movimentacao;

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

    private FirebaseAuth autenticacao  = ConfiguracaoFirestore.getFirebaseAutenticacao();
    private FirebaseFirestore fs;
    private String uid;

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
        setContentView(R.layout.ac_main_contas_lista_saldo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("OrgaFácil");

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            finish();
            return;
        }

        Log.i("DEBUG_FIREBASE", "UID atual: " + uid);
        fs = ConfiguracaoFirestore.getFirestore();

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
                        recuperarResumo(); // atualiza totais do usuário
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
        if (uid == null) return;

        movimentacoes.clear();

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes")
                .get()
                .addOnSuccessListener(mesesSnap -> {
                    if (mesesSnap.isEmpty()) {
                        aplicarFiltroTextoEAtualizarLista();
                        return;
                    }

                    final int totalMeses = mesesSnap.size();
                    final int[] done = {0};

                    for (QueryDocumentSnapshot mesDoc : mesesSnap) {
                        String mesAno = mesDoc.getId();

                        mesDoc.getReference()
                                .collection("itens")
                                .get()
                                .addOnSuccessListener(itensSnap -> {
                                    for (QueryDocumentSnapshot d : itensSnap) {
                                        Movimentacao m = new Movimentacao();
                                        m.setKey(d.getId());
                                        m.setMesAno(mesAno);
                                        m.setCategoria(d.getString("categoria"));
                                        m.setDescricao(d.getString("descricao"));
                                        m.setData(d.getString("data"));
                                        m.setHora(d.getString("hora"));
                                        m.setTipo(d.getString("tipo"));
                                        Double val = d.getDouble("valor");
                                        if (val != null) m.setValor(val);

                                        if (estaDentroDoIntervalo(m)) {
                                            movimentacoes.add(m);
                                        }
                                    }

                                    done[0]++;
                                    if (done[0] == totalMeses) {
                                        // saldo do intervalo (como você já fazia)
                                        if (dataInicialSelecionada != null && dataFinalSelecionada != null) {
                                            double totalProventos = 0.0;
                                            double totalDespesas = 0.0;


                                            for (Movimentacao m2 : movimentacoes) {
                                                if (m2 == null) continue;
                                                if ("r".equals(m2.getTipo())) totalProventos += m2.getValor();
                                                else if ("d".equals(m2.getTipo())) totalDespesas += m2.getValor();
                                            }

                                            double saldoIntervalo = totalProventos - totalDespesas;
                                            DecimalFormat decimalFormat = new DecimalFormat("0.##");
                                            String resultadoFormatado = decimalFormat.format(saldoIntervalo);

                                            textoSaldo.setText("R$ " + resultadoFormatado);
                                            textoSaldoLegenda.setText("Saldo entre " + dataInicialSelecionada + " e " + dataFinalSelecionada);
                                        } else {
                                            textoSaldoLegenda.setText("Saldo atual");
                                        }

                                        aplicarFiltroTextoEAtualizarLista();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    done[0]++;
                                    if (done[0] == totalMeses) {
                                        aplicarFiltroTextoEAtualizarLista();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar movimentações", Toast.LENGTH_SHORT).show()
                );
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

//            removerListenerMovimentacoesSeExistir();
            recuperarMovimentacoes();
            recuperarResumo();
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

            if (uid == null || movimentacao == null) return;

            String mesAno = movimentacao.getMesAno();
            if (mesAno == null || mesAno.trim().isEmpty()) {
                // fallback se ainda não tiver mesAno preenchido
                String data = movimentacao.getData();
                if (data != null && data.length() >= 10) {
                    mesAno = data.substring(3, 5) + data.substring(6);
                }
            }

            if (mesAno == null || mesAno.trim().isEmpty()) {
                Toast.makeText(this, "Não foi possível identificar o mês/ano da movimentação.", Toast.LENGTH_SHORT).show();
                adapterAgrupado.notifyDataSetChanged();
                return;
            }

            String movId = movimentacao.getKey();

            // 1) deletar doc da movimentação
            fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("movimentacoes").document(mesAno)
                    .collection("itens").document(movId)
                    .delete()
                    .addOnSuccessListener(unused -> {

                        // 2) decrementar total
                        double valor = movimentacao.getValor();
                        if ("r".equals(movimentacao.getTipo())) {
                            fs.collection("users").document(uid)
                                    .collection("contas").document("main")
                                    .update("proventosTotal", FieldValue.increment(-valor));
                        } else if ("d".equals(movimentacao.getTipo())) {
                            fs.collection("users").document(uid)
                                    .collection("contas").document("main")
                                    .update("despesaTotal", FieldValue.increment(-valor));
                        }

                        // 3) remove da lista base
                        for (int idx = 0; idx < movimentacoes.size(); idx++) {
                            Movimentacao mBase = movimentacoes.get(idx);
                            if (mBase != null && movId.equals(mBase.getKey())) {
                                movimentacoes.remove(idx);
                                break;
                            }
                        }

                        // 4) atualiza UI
                        recuperarResumo(); // recalc saldo atual (Proventos - Despesas)
                        aplicarFiltroTextoEAtualizarLista();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ContasActivity.this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                        adapterAgrupado.notifyDataSetChanged();
                    });
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

//    public void atualizarSaldo(){
//
//        if (movimentacao == null) return;
//
//        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);
//
//        if ("r".equals(movimentacao.getTipo())) {
//            proventosTotal = proventosTotal - movimentacao.getValor();
//            usuarioRef.child("proventosTotal").setValue(proventosTotal);
//        }
//        if ("d".equals(movimentacao.getTipo())) {
//            despesaTotal = despesaTotal - movimentacao.getValor();
//            usuarioRef.child("despesaTotal").setValue(despesaTotal);
//        }
//    }

    public void recuperarMovimentacoes() {
        if (uid == null) return;

        movimentacoes.clear();

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes")
                .get()
                .addOnSuccessListener(mesesSnap -> {
                    if (mesesSnap.isEmpty()) {
                        aplicarFiltroTextoEAtualizarLista();
                        return;
                    }

                    final int totalMeses = mesesSnap.size();
                    final int[] done = {0};

                    for (QueryDocumentSnapshot mesDoc : mesesSnap) {
                        String mesAno = mesDoc.getId();

                        mesDoc.getReference()
                                .collection("itens")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(itensSnap -> {
                                    for (QueryDocumentSnapshot d : itensSnap) {
                                        Movimentacao m = new Movimentacao();
                                        m.setKey(d.getId());
                                        m.setMesAno(mesAno);
                                        m.setCategoria(d.getString("categoria"));
                                        m.setDescricao(d.getString("descricao"));
                                        m.setData(d.getString("data"));
                                        m.setHora(d.getString("hora"));
                                        m.setTipo(d.getString("tipo"));
                                        Double val = d.getDouble("valor");
                                        if (val != null) m.setValor(val);

                                        movimentacoes.add(m);
                                    }

                                    done[0]++;
                                    if (done[0] == totalMeses) {
                                        aplicarFiltroTextoEAtualizarLista();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    done[0]++;
                                    if (done[0] == totalMeses) {
                                        aplicarFiltroTextoEAtualizarLista();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar movimentações", Toast.LENGTH_SHORT).show()
                );
    }

//    private void removerListenerMovimentacoesSeExistir() {
//        if (movimentacaoRef != null && valueEventListenerMovimentacoes != null) {
//            movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
//            valueEventListenerMovimentacoes = null;
//        }
//    }

    public void recuperarResumo() {
        if (uid == null) return;

        // 1) Nome no perfil: users/{uid}.perfil.nome
        fs.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String nome = null;
                    if (userDoc.exists() && userDoc.get("perfil") instanceof java.util.Map) {
                        java.util.Map perfil = (java.util.Map) userDoc.get("perfil");
                        Object n = perfil.get("nome");
                        if (n != null) nome = n.toString();
                    }
                    if (nome == null || nome.trim().isEmpty()) nome = "usuário";
                    textoSaudacao.setText("Ola, " + nome + "!");
                });

        // 2) Totais: users/{uid}/contas/main
        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    Double d = doc.getDouble("despesaTotal");
                    Double p = doc.getDouble("proventosTotal");

                    despesaTotal = (d != null) ? d : 0.0;
                    proventosTotal = (p != null) ? p : 0.0;

                    resumoUsuario = proventosTotal - despesaTotal;

                    DecimalFormat decimalFormat = new DecimalFormat("0.##");
                    String resultadoFormatado = decimalFormat.format(resumoUsuario);

                    textoSaldo.setText("R$ " + resultadoFormatado);
                    textoSaldoLegenda.setText("Saldo atual");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar resumo", Toast.LENGTH_SHORT).show()
                );
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
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
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

        if (dataInicialSelecionada != null && dataFinalSelecionada != null) {
            recuperarMovimentacoesComFiltro();
        } else {
            recuperarMovimentacoes();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", "Evento exibir dados removido");
//        if (usuarioRef != null && valueEventListenerUsuario != null) {
//            usuarioRef.removeEventListener(valueEventListenerUsuario);
//        }
//        if (movimentacaoRef != null && valueEventListenerMovimentacoes != null) {
//            movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
//        }
    }
}
