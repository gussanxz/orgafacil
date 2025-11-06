package com.gussanxz.orgafacil.activity.contas;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
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
import android.graphics.RectF;

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
import com.gussanxz.orgafacil.activity.vendas.CadastroClienteActivity;
import com.gussanxz.orgafacil.activity.LoginActivity;
import com.gussanxz.orgafacil.activity.MainActivity;
import com.gussanxz.orgafacil.activity.vendas.VendasActivity;
import com.gussanxz.orgafacil.adapter.AdapterMovimentacao;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.Usuario;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrincipalActivity extends AppCompatActivity {

//    private MaterialCalendarView calendarView;
    private TextView textoSaudacao, textoSaldo, textoSaldoLegenda;
    private Double despesaTotal = 0.0;
    private Double proventosTotal = 0.0;
    private Double resumoUsuario = 0.0;
    private FirebaseAuth autenticacao  = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private DatabaseReference usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private ValueEventListener valueEventListenerUsuario;
    private ValueEventListener valueEventListenerMovimentacoes;
    private RecyclerView recyclerView;
    private AdapterMovimentacao adapterMovimentacao;
    private List<Movimentacao> movimentacoes = new ArrayList<>();
    private Movimentacao movimentacao;
    private DatabaseReference movimentacaoRef;
    private String mesAnoSelecionado;
    private ActivityResultLauncher<Intent> launcher;
    private SearchView searchView;
    private List<Movimentacao> listaFiltrada = new ArrayList<>();
    private EditText editDataInicial, editDataFinal;
    private String dataInicialSelecionada, dataFinalSelecionada;
    private ImageView imgLimparFiltroData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "null";

        Log.i("DEBUG_FIREBASE", "UID atual: " + uid);


        toolbar.setTitle("OrgaFácil");


        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        textoSaldoLegenda = findViewById(R.id.textView16);
//        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.recyclesMovimentos);
//        configuraCalendarView();
        editDataInicial = findViewById(R.id.editDataInicial);
        editDataFinal = findViewById(R.id.editDataFinal);

        editDataInicial.setOnClickListener(v -> abrirDatePicker(true));
        editDataFinal.setOnClickListener(v -> abrirDatePicker(false));
        limparFiltroData();

        swipe();

        searchView = findViewById(R.id.searchViewEventos);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filtrarMovimentacoes(newText);
                return true;
            }
        });


        //Configurar adapter recyclerview
        adapterMovimentacao = new AdapterMovimentacao(movimentacoes, this);

        //Configurar RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapterMovimentacao);

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        atualizarSaldo(); // ✅ atualiza saldo ao voltar
                        if (dataInicialSelecionada != null || dataFinalSelecionada != null) {
                            recuperarMovimentacoesComFiltro(); // ✅ se filtro por data estiver ativo
                        } else {
                            recuperarMovimentacoes(); // ✅ se nenhum filtro de data estiver definido
                        }

                        // reexecutar filtro por texto (se houver texto na searchView)
                        String textoBusca = searchView.getQuery().toString();
                        if (!textoBusca.isEmpty()) {
                            filtrarMovimentacoes(textoBusca);
                        }
                    }
                }
        );

    }

    private void filtrarMovimentacoes(String texto) {
        listaFiltrada.clear();

        // Qual base usar: toda a lista ou lista filtrada por data
        List<Movimentacao> base = (dataInicialSelecionada != null || dataFinalSelecionada != null)
                ? movimentacoes // já filtradas por data
                : movimentacoes; // se quiser usar lista original completa separadamente, crie uma cópia inicial.

        if (texto.isEmpty()) {
            listaFiltrada.addAll(base);
        } else {
            for (Movimentacao m : base) {
                if (m.getCategoria().toLowerCase().contains(texto.toLowerCase()) ||
                        m.getDescricao().toLowerCase().contains(texto.toLowerCase())) {
                    listaFiltrada.add(m);
                }
            }
        }

        adapterMovimentacao.atualizarLista(listaFiltrada);
    }


    private void abrirDatePicker(boolean isInicial) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    if (isInicial) {
                        dataInicialSelecionada = dataSelecionada;
                        editDataInicial.setText(dataSelecionada);
                    } else {
                        dataFinalSelecionada = dataSelecionada;
                        editDataFinal.setText(dataSelecionada);
                    }

                    // Reaplica filtro após cada seleção
                    recuperarMovimentacoesComFiltro();
                },
                year, month, day
        );
        datePicker.show();
    }

    private void recuperarMovimentacoesComFiltro() {
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        movimentacaoRef = firebaseRef.child("movimentacao").child(idUsuario);

        valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movimentacoes.clear();

                for (DataSnapshot mesAnoSnap : snapshot.getChildren()) {
                    for (DataSnapshot movSnap : mesAnoSnap.getChildren()) {
                        Movimentacao mov = movSnap.getValue(Movimentacao.class);
                        mov.setKey(movSnap.getKey());

                        if (mov != null && estaDentroDoIntervalo(mov)) {
                            movimentacoes.add(mov);
                        }
                    }
                }

                // Ordenar por data/hora
                movimentacoes.sort((m1, m2) -> {
                    try {
                        String dataHora1 = m1.getData() + " " + m1.getHora();
                        String dataHora2 = m2.getData() + " " + m2.getHora();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date dt1 = sdf.parse(dataHora1);
                        Date dt2 = sdf.parse(dataHora2);
                        return dt2.compareTo(dt1);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                adapterMovimentacao.notifyDataSetChanged();

                // === Recalcular saldo do intervalo e atualizar legenda ===
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

                    java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("0.##");
                    String resultadoFormatado = decimalFormat.format(saldoIntervalo);

                    // atualiza o valor
                    textoSaldo.setText("R$ " + resultadoFormatado);

                    // atualiza a legenda “Saldo entre X e Y”
                    textoSaldoLegenda.setText("Saldo entre " + dataInicialSelecionada + " e " + dataFinalSelecionada);
                } else {
                    // Se o intervalo ainda não está completo, mantém “Saldo atual”
                    textoSaldoLegenda.setText("Saldo atual");
                }
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
            return true; // fallback: inclui por segurança
        }
    }

    public void limparFiltroData() {
        imgLimparFiltroData = findViewById(R.id.imgLimparFiltroData);

        imgLimparFiltroData.setOnClickListener(v -> {
            editDataInicial.setText("");
            editDataFinal.setText("");
            dataInicialSelecionada = null;
            dataFinalSelecionada = null;

            textoSaldoLegenda.setText("Saldo atual");

            recuperarMovimentacoes(); // mostra todos os eventos
        });

    }


    public void swipe(){

        ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {


            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                Paint backgroundPaint = new Paint();
                Paint textPaint = new Paint();

                // Configura pintura do texto
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(40f);
                textPaint.setAntiAlias(true);


                float textY = viewHolder.itemView.getTop() + viewHolder.itemView.getHeight() / 2f + 15;

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
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Log.i("swipe", "item foi arrastado");
                if (direction == ItemTouchHelper.START) {
                    excluirMovimentacao(viewHolder);
                } else if (direction == ItemTouchHelper.END) {
                    editarMovimentacao(viewHolder);
                }

            }
        };

        new ItemTouchHelper( itemTouch ).attachToRecyclerView( recyclerView );

    }

    public void editarMovimentacao(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        Movimentacao movimentacao = movimentacoes.get(position);

        Intent intent = new Intent(PrincipalActivity.this, EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", movimentacao);
        intent.putExtra("keyFirebase", movimentacao.getKey());
        launcher.launch(intent);
        adapterMovimentacao.notifyDataSetChanged(); // para restaurar o item na lista
    }


    public void excluirMovimentacao(RecyclerView.ViewHolder viewHolder){
        int position = viewHolder.getAdapterPosition();
        movimentacao = movimentacoes.get(position);

        String tipo = movimentacao.getTipo(); // "d" ou "r"
        String tipoTexto = tipo.equals("d") ? "despesa" : "provento";

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Excluir " + tipoTexto);
        alertDialog.setMessage("Você deseja excluir este " + tipoTexto + "?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", (dialogInterface, i) -> {
            String emailUsuario = autenticacao.getCurrentUser().getEmail();
            String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // ✅ Usa a data da movimentação para calcular o mesAno
            String data = movimentacao.getData(); // ex: 25/05/2025
            String mesAno = data.substring(3, 5) + data.substring(6); // "052025"

            movimentacaoRef = firebaseRef.child("movimentacao")
                    .child(idUsuario)
                    .child(mesAno);

            movimentacaoRef.child(movimentacao.getKey()).removeValue();
            movimentacoes.remove(position);
            adapterMovimentacao.notifyItemRemoved(position);
            atualizarSaldo();
        });

        alertDialog.setNegativeButton("Cancelar", (dialogInterface, i) -> {
            Toast.makeText(PrincipalActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
            adapterMovimentacao.notifyDataSetChanged(); // restaura visual do item
        });

        alertDialog.show();
    }


    public void atualizarSaldo(){

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        if (movimentacao.getTipo().equals("r")){
            proventosTotal = proventosTotal - movimentacao.getValor();
            usuarioRef.child("proventosTotal").setValue(proventosTotal);
        }
        if (movimentacao.getTipo().equals("d")){
            despesaTotal = despesaTotal - movimentacao.getValor();
            usuarioRef.child("despesaTotal").setValue(despesaTotal);
        }
    }

    public void recuperarMovimentacoes(){
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        movimentacaoRef = firebaseRef.child("movimentacao")
                .child(idUsuario);

        valueEventListenerMovimentacoes  =  movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movimentacoes.clear();

                for (DataSnapshot mesAnoSnap : snapshot.getChildren()) {
                    for (DataSnapshot movSnap : mesAnoSnap.getChildren()) {
                        Movimentacao mov = movSnap.getValue(Movimentacao.class);
                        mov.setKey(movSnap.getKey());
                        movimentacoes.add(mov);
                    }
                }
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//                movimentacoes.clear();
//
//                for (DataSnapshot dados: snapshot.getChildren() ){
//                    for (DataSnapshot movSnap : mesAno)
//                    Movimentacao movimentacao = dados.getValue( Movimentacao.class );
//                    movimentacao.setKey(dados.getKey());
//
//                    movimentacoes.add( movimentacao );
//
//                }

                movimentacoes.sort((m1, m2) -> {
                    try {
                        String dataHora1 = m1.getData() + " " + m1.getHora(); // "01/05/2025 14:30"
                        String dataHora2 = m2.getData() + " " + m2.getHora();

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date dt1 = sdf.parse(dataHora1);
                        Date dt2 = sdf.parse(dataHora2);

                        return dt2.compareTo(dt1); // ordem decrescente (mais novo primeiro)

                    } catch (Exception e) {
                        return 0; // fallback se algo der errado
                    }
                });

                adapterMovimentacao.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void recuperarResumo(){
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        Log.i("onStart", "Evento exibir dados adicionado");
        valueEventListenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Usuario usuario = snapshot.getValue( Usuario.class );

                despesaTotal = usuario.getDespesaTotal();
                proventosTotal = usuario.getProventosTotal();
                resumoUsuario = proventosTotal - despesaTotal;

                DecimalFormat decimalFormat = new DecimalFormat("0.##");
                String resultadoFormatado = decimalFormat.format( resumoUsuario );

                textoSaudacao.setText("Ola, " + usuario.getNome() + "!");
                textoSaldo.setText( "R$ " + resultadoFormatado );

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

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

//    public void configuraCalendarView(){
//        CharSequence meses[] = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
//        calendarView.setTitleMonths( meses );
//
//        CalendarDay dataAtual = calendarView.getCurrentDate();
//        String mesSelecionado = String.format("%02d", (dataAtual.getMonth() + 1) );
//        mesAnoSelecionado = String.valueOf( mesSelecionado + "" + dataAtual.getYear() );
//
//        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
//            @Override
//            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
//                String mesSelecionado = String.format("%02d", (date.getMonth() + 1) );
//                mesAnoSelecionado = String.valueOf( mesSelecionado + "" + date.getYear() );
//                Log.i("MES", "mes: " + mesAnoSelecionado);
//
//                movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
//                recuperarMovimentacoes();
//            }
//        });
//    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacoesComFiltro();
    }


    ;
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", "Evento exibir dados removido");
        usuarioRef.removeEventListener( valueEventListenerUsuario );
        movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
    }
}