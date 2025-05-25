package com.gussanxz.orgafacil.activity.contas;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.SearchView;
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
import com.gussanxz.orgafacil.helper.Base64Custom;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.Usuario;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrincipalActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView textoSaudacao, textoSaldo;
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitle("OrgaFácil");


        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.recyclesMovimentos);
        configuraCalendarView();
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
                        adapterMovimentacao.notifyDataSetChanged(); // força atualizar lista
                    }
                }
        );

    }

    private void filtrarMovimentacoes(String texto) {
        listaFiltrada.clear();

        if (texto.isEmpty()) {
            listaFiltrada.addAll(movimentacoes);
        } else {
            for (Movimentacao m : movimentacoes) {
                if (m.getCategoria().toLowerCase().contains(texto.toLowerCase()) ||
                        m.getDescricao().toLowerCase().contains(texto.toLowerCase())) {
                    listaFiltrada.add(m);
                }
            }
        }

        adapterMovimentacao.atualizarLista(listaFiltrada);
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
        if (tipoTexto == "despesa"){
            alertDialog.setMessage("Você deseja excluir esta despesa?");
        }else{
            alertDialog.setMessage("Você deseja excluir este provento?");
        }
        alertDialog.setCancelable(false);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int position = viewHolder.getAdapterPosition();
                movimentacao = movimentacoes.get( position );

                String emailUsuario = autenticacao.getCurrentUser().getEmail();
                String idUsuario = Base64Custom.codificarBase64( emailUsuario );
                movimentacaoRef = firebaseRef.child("movimentacao")
                        .child(idUsuario)
                        .child(mesAnoSelecionado);
                movimentacaoRef.child(movimentacao.getKey()).removeValue();
                adapterMovimentacao.notifyItemRemoved(position);
                atualizarSaldo();

            }
        });
        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(PrincipalActivity.this,
                        "Cancelado",
                        Toast.LENGTH_SHORT).show();
                adapterMovimentacao.notifyDataSetChanged();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

    }

    public void atualizarSaldo(){

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64( emailUsuario );
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
        String idUsuario = Base64Custom.codificarBase64( emailUsuario );
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
        String idUsuario = Base64Custom.codificarBase64( emailUsuario );
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

    public void configuraCalendarView(){
        CharSequence meses[] = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        calendarView.setTitleMonths( meses );

        CalendarDay dataAtual = calendarView.getCurrentDate();
        String mesSelecionado = String.format("%02d", (dataAtual.getMonth() + 1) );
        mesAnoSelecionado = String.valueOf( mesSelecionado + "" + dataAtual.getYear() );

        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                String mesSelecionado = String.format("%02d", (date.getMonth() + 1) );
                mesAnoSelecionado = String.valueOf( mesSelecionado + "" + date.getYear() );
                Log.i("MES", "mes: " + mesAnoSelecionado);

                movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
                recuperarMovimentacoes();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacoes();
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