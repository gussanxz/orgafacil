package com.gussanxz.orgafacil.activity.main.contas;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // Importante: ImageButton
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.DateCustom;
import com.gussanxz.orgafacil.model.Movimentacao;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditarMovimentacaoActivity extends AppCompatActivity {

    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir; // Referência para o botão de lixeira

    private Movimentacao movimentacaoAntiga;
    private String keyFirebase;
    private double valorAnterior;

    private FirebaseFirestore fs;
    private String uid;

    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recupera dados
        movimentacaoAntiga = (Movimentacao) getIntent().getSerializableExtra("movimentacaoSelecionada");
        keyFirebase = getIntent().getStringExtra("keyFirebase");

        if (movimentacaoAntiga == null) {
            finish();
            return;
        }

        // Define layout
        if ("d".equals(movimentacaoAntiga.getTipo())) {
            setContentView(R.layout.ac_main_contas_add_despesa);
        } else {
            setContentView(R.layout.ac_main_contas_add_receita);
        }

        fs = ConfiguracaoFirestore.getFirestore();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        inicializarComponentes();
        preencherDados();
        configurarListeners();
        configurarLauncherCategoria();
    }

    private void inicializarComponentes() {
        textViewHeader = findViewById(R.id.textViewHeader);
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);

        // VINCULA O BOTÃO PELO ID (Muito mais seguro que onClick no XML)
        btnExcluir = findViewById(R.id.btnExcluir);

        if (textViewHeader != null) {
            textViewHeader.setText("d".equals(movimentacaoAntiga.getTipo()) ? "Editar Despesa" : "Editar Receita");
        }
    }

    private void preencherDados() {
        valorAnterior = movimentacaoAntiga.getValor();
        editData.setText(movimentacaoAntiga.getData());
        editHora.setText(movimentacaoAntiga.getHora());
        editDescricao.setText(movimentacaoAntiga.getDescricao());
        editValor.setText(String.valueOf(valorAnterior));
        editCategoria.setText(movimentacaoAntiga.getCategoria());
    }

    private void configurarListeners() {
        editData.setFocusable(false);
        editData.setClickable(true);
        editData.setOnClickListener(v -> abrirDataPicker());

        editHora.setFocusable(false);
        editHora.setClickable(true);
        editHora.setOnClickListener(v -> abrirTimePicker());

        // CONFIGURA O CLIQUE DO BOTÃO EXCLUIR NO JAVA
        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }
    }

    public void retornarPrincipal(View view) {
        finish();
    }

    // --- Métodos para Salvar (Chamados pelo XML dos FABs) ---
    public void salvarDespesa(View view) { confirmarEdicao(); }
    public void salvarProvento(View view) { confirmarEdicao(); }
    public void salvarProventos(View view) { confirmarEdicao(); }

    private void confirmarEdicao() {
        String novaData = editData.getText().toString();
        String novaDescricao = editDescricao.getText().toString();
        String novaCategoria = editCategoria.getText().toString();
        String novoValorStr = editValor.getText().toString();
        String novaHora = editHora.getText().toString();

        if (novaData.isEmpty() || novaDescricao.isEmpty() || novaCategoria.isEmpty() || novoValorStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double novoValor = Double.parseDouble(novoValorStr);
        double diferencaValor = novoValor - valorAnterior;

        WriteBatch batch = fs.batch();

        String mesAnoAntigo = movimentacaoAntiga.getMesAno();
        if (mesAnoAntigo == null) mesAnoAntigo = DateCustom.mesAnoDataEscolhida(movimentacaoAntiga.getData());
        String mesAnoNovo = DateCustom.mesAnoDataEscolhida(novaData);

        DocumentReference refAntiga = fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes").document(mesAnoAntigo)
                .collection("itens").document(keyFirebase);

        Map<String, Object> dadosAtualizados = new HashMap<>();
        dadosAtualizados.put("data", novaData);
        dadosAtualizados.put("hora", novaHora);
        dadosAtualizados.put("descricao", novaDescricao);
        dadosAtualizados.put("categoria", novaCategoria);
        dadosAtualizados.put("valor", novoValor);
        dadosAtualizados.put("tipo", movimentacaoAntiga.getTipo());
        dadosAtualizados.put("mesAno", mesAnoNovo);
        dadosAtualizados.put("key", keyFirebase);

        if (!mesAnoNovo.equals(mesAnoAntigo)) {
            DocumentReference refPastaMesNovo = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("movimentacoes").document(mesAnoNovo);

            Map<String, Object> dadosMes = new HashMap<>();
            dadosMes.put("mesAno", mesAnoNovo);
            batch.set(refPastaMesNovo, dadosMes, SetOptions.merge());

            DocumentReference refNova = refPastaMesNovo.collection("itens").document(keyFirebase);
            batch.delete(refAntiga);
            batch.set(refNova, dadosAtualizados);
        } else {
            batch.update(refAntiga, dadosAtualizados);
        }

        DocumentReference contaMainRef = fs.collection("users").document(uid)
                .collection("contas").document("main");
        String campoParaAtualizar = "d".equals(movimentacaoAntiga.getTipo()) ? "despesaTotal" : "proventosTotal";

        if (diferencaValor != 0) {
            batch.update(contaMainRef, campoParaAtualizar, FieldValue.increment(diferencaValor));
        }

        batch.commit().addOnSuccessListener(unused -> {
            Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // --- DIALOG CUSTOMIZADO ---
    private void confirmarExclusao() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Verifique se o nome do arquivo XML do dialog está correto aqui:
        // Se o arquivo for "dialog_exclusao.xml", troque abaixo.
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmar_exclusao, null);

        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        textMensagem.setText("Você deseja realmente excluir '" + movimentacaoAntiga.getDescricao() + "'?");

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            excluirNoFirestore();
        });

        dialog.show();
    }

    private void excluirNoFirestore() {
        WriteBatch batch = fs.batch();

        String mesAno = movimentacaoAntiga.getMesAno();
        if (mesAno == null) mesAno = DateCustom.mesAnoDataEscolhida(movimentacaoAntiga.getData());

        DocumentReference refItem = fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes").document(mesAno)
                .collection("itens").document(keyFirebase);

        DocumentReference contaMainRef = fs.collection("users").document(uid)
                .collection("contas").document("main");

        String campoTotal = "d".equals(movimentacaoAntiga.getTipo()) ? "despesaTotal" : "proventosTotal";

        batch.delete(refItem);
        batch.update(contaMainRef, campoTotal, FieldValue.increment(-valorAnterior));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // --- Pickers & Categoria ---
    private void abrirDataPicker() {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = editData.getText().toString().split("/");
            c.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
        } catch (Exception e) { /* fallback */ }

        new DatePickerDialog(this, (v, y, m, d) ->
                editData.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void abrirTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) ->
                editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        editCategoria.setText(result.getData().getStringExtra("categoriaSelecionada"));
                    }
                });

        editCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(EditarMovimentacaoActivity.this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });
    }
}