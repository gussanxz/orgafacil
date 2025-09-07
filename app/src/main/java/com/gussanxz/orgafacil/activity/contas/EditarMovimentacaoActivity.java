package com.gussanxz.orgafacil.activity.contas;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.clans.fab.FloatingActionButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.SelecionarCategoriaActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.model.DatePickerHelper;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.gussanxz.orgafacil.activity.contas.PrincipalActivity;

import java.util.Calendar;

public class EditarMovimentacaoActivity extends AppCompatActivity {

    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private Movimentacao movimentacao;
    private boolean isModoEdicao = false;
    private String keyFirebase;
    private final DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private final FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private double valorAnterior;
    private String tipoAnterior;
    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isModoEdicao = true;
        super.onCreate(savedInstanceState);

        movimentacao = (Movimentacao) getIntent().getSerializableExtra("movimentacaoSelecionada");
        if (movimentacao != null) {
            if (movimentacao.getTipo().equals("d")) {
                setContentView(R.layout.activity_despesas);
            } else if (movimentacao.getTipo().equals("r")) {
                setContentView(R.layout.activity_proventos);
            }
        }

        textViewHeader = findViewById(R.id.textViewHeader);
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);

        keyFirebase = getIntent().getStringExtra("keyFirebase");

        if (movimentacao != null) {
            if (textViewHeader != null) {
                if (movimentacao.getTipo().equals("d")) {
                    textViewHeader.setText("Editar Despesa");
                } else if (movimentacao.getTipo().equals("r")) {
                    textViewHeader.setText("Editar Provento");
                }
            }

            valorAnterior = movimentacao.getValor();
            tipoAnterior = movimentacao.getTipo();

            // Preenche os campos
            editData.setText(movimentacao.getData());
            editHora.setText(movimentacao.getHora());
            editDescricao.setText(movimentacao.getDescricao());
            editValor.setText(String.valueOf(valorAnterior));
            editCategoria.setText(movimentacao.getCategoria());

            editData.setOnClickListener(v -> abrirDataPicker());
            editHora.setOnClickListener(v -> abrirTimePicker());

            abrirCategoras();

        }
    }

    public void retornarPrincipal(View view) {
        startActivity(new Intent(this, PrincipalActivity.class));
    }

    private void abrirDataPicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dataSelecionada = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    editData.setText(dataSelecionada);
                },
                year, month, day
        );
        datePicker.show();
    }

    private void abrirTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hora = calendar.get(Calendar.HOUR_OF_DAY);
        int minuto = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String horaSelecionada = String.format("%02d:%02d", hourOfDay, minute1);
            editHora.setText(horaSelecionada);
        }, hora, minuto, true).show();
    }

    public void abrirCategoras() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        editCategoria.setText(categoria);
                    }
                });

        editCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(EditarMovimentacaoActivity.this, SelecionarCategoriaActivity.class);
            launcherCategoria.launch(intent);
        });
    }

    public void salvarDespesa(View view) {
        if (isModoEdicao) {
            atualizarMovimentacaoExistente();
        } else {
            salvarNovaMovimentacao(); // opcional, se quiser aproveitar a mesma activity
        }
    }

    public void salvarProventos(View view) {
        if (isModoEdicao) {
            atualizarMovimentacaoExistente();
        } else {
            salvarNovaMovimentacao(); // opcional, se quiser aproveitar a mesma activity
        }
    }

    private void atualizarMovimentacaoExistente() {
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String mesAno = DatePickerHelper.mesAnoDataEscolhida(movimentacao.getData());
        ; // dd/MM/yyyy

        DatabaseReference ref = firebaseRef
                .child("movimentacao")
                .child(idUsuario)
                .child(mesAno)
                .child(keyFirebase);

        movimentacao.setData(editData.getText().toString());
        movimentacao.setHora(editHora.getText().toString());
        movimentacao.setDescricao(editDescricao.getText().toString());
        movimentacao.setCategoria(editCategoria.getText().toString());
        movimentacao.setValor(Double.parseDouble(editValor.getText().toString()));

        ref.setValue(movimentacao).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ajustarTotais(); // ✅ Corrige proventos/despesas
                setResult(RESULT_OK);
                finish();
            }
        });


        Toast.makeText(this, "Movimentação atualizada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void salvarNovaMovimentacao() {
        // opcional: se quiser permitir inserções nesta mesma tela
        Toast.makeText(this, "Função de nova movimentação não implementada aqui.", Toast.LENGTH_SHORT).show();
    }

    private void ajustarTotais() {
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        usuarioRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                double proventos = snapshot.child("proventosTotal").getValue(Double.class);
                double despesas = snapshot.child("despesaTotal").getValue(Double.class);

                // Subtrai o valor anterior
                if (tipoAnterior.equals("r")) {
                    proventos -= valorAnterior;
                } else if (tipoAnterior.equals("d")) {
                    despesas -= valorAnterior;
                }

                // Adiciona o novo valor
                if (movimentacao.getTipo().equals("r")) {
                    proventos += movimentacao.getValor();
                } else if (movimentacao.getTipo().equals("d")) {
                    despesas += movimentacao.getValor();
                }

                // Atualiza no Firebase
                usuarioRef.child("proventosTotal").setValue(proventos);
                usuarioRef.child("despesaTotal").setValue(despesas);
            }
        });
    }

}

