package com.gussanxz.orgafacil.activity.main.contas;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private Movimentacao movimentacaoAntiga; // Dados originais
    private String keyFirebase;
    private double valorAnterior;

    private FirebaseFirestore fs;
    private String uid;

    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recupera objeto vindo da ContasActivity
        movimentacaoAntiga = (Movimentacao) getIntent().getSerializableExtra("movimentacaoSelecionada");
        keyFirebase = getIntent().getStringExtra("keyFirebase");

        // Define layout baseado no tipo
        if (movimentacaoAntiga != null) {
            if ("d".equals(movimentacaoAntiga.getTipo())) {
                setContentView(R.layout.ac_main_contas_add_despesa);
            } else {
                setContentView(R.layout.ac_main_contas_add_provento);
            }
        } else {
            finish(); // Segurança
            return;
        }

        // Configurações Iniciais
        fs = ConfiguracaoFirestore.getFirestore();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        textViewHeader = findViewById(R.id.textViewHeader); // Verifique se existe no layout, ou remova
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);

        if (textViewHeader != null) {
            if ("d".equals(movimentacaoAntiga.getTipo())) {
                textViewHeader.setText("Editar Despesa");
            } else {
                textViewHeader.setText("Editar Receita");
            }
        }

        // Preenche os campos com os valores ANTIGOS
        valorAnterior = movimentacaoAntiga.getValor();

        editData.setText(movimentacaoAntiga.getData());
        editHora.setText(movimentacaoAntiga.getHora());
        editDescricao.setText(movimentacaoAntiga.getDescricao());
        editValor.setText(String.valueOf(valorAnterior));
        editCategoria.setText(movimentacaoAntiga.getCategoria());

        // Listeners
        editData.setFocusable(false);
        editData.setClickable(true);
        editData.setOnClickListener(v -> abrirDataPicker());

        editHora.setFocusable(false);
        editHora.setClickable(true);
        editHora.setOnClickListener(v -> abrirTimePicker());

        configurarLauncherCategoria();
    }

    public void retornarPrincipal(View view) {
        finish();
    }

    // Como o layout é reaproveitado, ambos os botões chamam métodos aqui.
    // Você pode ligar o "onClick" do botão Salvar no XML para um desses métodos.
    public void salvarDespesa(View view) {
        confirmarEdicao();
    }

    public void salvarProvento(View view) {
        confirmarEdicao();
    }

    // Método único para processar a edição
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

        // 1. Calcula a diferença para ajustar o saldo
        // Ex: Era 100, virou 120. Diferença = 20. (Soma 20 no total)
        // Ex: Era 100, virou 80. Diferença = -20. (Subtrai 20 no total)
        double diferencaValor = novoValor - valorAnterior;

        // 2. Prepara referências e Batch
        WriteBatch batch = fs.batch();

        String mesAnoAntigo = movimentacaoAntiga.getMesAno();
        if (mesAnoAntigo == null) mesAnoAntigo = DateCustom.mesAnoDataEscolhida(movimentacaoAntiga.getData());

        String mesAnoNovo = DateCustom.mesAnoDataEscolhida(novaData);

        // Referência do documento antigo
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
        dadosAtualizados.put("tipo", movimentacaoAntiga.getTipo()); // Tipo geralmente não muda
        dadosAtualizados.put("mesAno", mesAnoNovo);
        dadosAtualizados.put("key", keyFirebase);

        // --- LÓGICA DE MOVIMENTAÇÃO DE COLEÇÃO (MUDANÇA DE MÊS) ---
        if (!mesAnoNovo.equals(mesAnoAntigo)) {
            // Se mudou o mês, precisamos mover o documento para a nova coleção
            DocumentReference refNova = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("movimentacoes").document(mesAnoNovo)
                    .collection("itens").document(keyFirebase); // Mantém o mesmo ID

            batch.delete(refAntiga); // Deleta do mês antigo
            batch.set(refNova, dadosAtualizados); // Cria no mês novo
        } else {
            // Se é o mesmo mês, apenas atualiza
            batch.update(refAntiga, dadosAtualizados);
        }

        // 3. Atualiza o Saldo Total (Atomicamente)
        DocumentReference contaMainRef = fs.collection("users").document(uid)
                .collection("contas").document("main");

        String campoParaAtualizar = "d".equals(movimentacaoAntiga.getTipo()) ? "despesaTotal" : "proventosTotal";

        // Incrementa a diferença (pode ser positiva ou negativa)
        if (diferencaValor != 0) {
            batch.update(contaMainRef, campoParaAtualizar, FieldValue.increment(diferencaValor));
        }

        // 4. Executa
        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Avisa a ContasActivity para recarregar
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // --- Helpers de UI ---

    private void abrirDataPicker() {
        Calendar calendar = Calendar.getInstance();
        // Tenta parsear a data atual do campo para abrir o calendário nela
        try {
            String[] parts = editData.getText().toString().split("/");
            calendar.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
        } catch (Exception e) { /* fallback para hoje */ }

        new DatePickerDialog(this, (view, year, month, day) -> {
            String dataStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            editData.setText(dataStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void abrirTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String horaStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            editHora.setText(horaStr);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String cat = result.getData().getStringExtra("categoriaSelecionada");
                        editCategoria.setText(cat);
                    }
                });

        editCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(EditarMovimentacaoActivity.this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });
    }
}