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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.config.FirestoreSchema;
import com.gussanxz.orgafacil.data.model.Movimentacao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

/**
 * EditarMovimentacaoActivity (schema novo)
 *
 * O que faz:
 * - Edita uma movimentação EXISTENTE SEM recriar documento.
 *
 * Impacto:
 * - "Último lançamento" continua baseado em createdAt (criação),
 *   porque edição NÃO altera createdAt.
 * - Evita bug de ranking (edição virando "mais recente").
 */
public class EditarMovimentacaoActivity extends AppCompatActivity {

    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir;

    private Movimentacao movimentacaoAntiga;
    private String movId;          // ID do doc em contasMovimentacoes
    private double valorAnterior;  // valor antigo (double legado)

    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        movimentacaoAntiga = (Movimentacao) getIntent().getSerializableExtra("movimentacaoSelecionada");
        movId = getIntent().getStringExtra("keyFirebase"); // reutilizando seu nome antigo

        if (movimentacaoAntiga == null || movId == null || movId.trim().isEmpty()) {
            finish();
            return;
        }

        // Mantém suas telas separadas por tipo (UI)
        if ("d".equals(movimentacaoAntiga.getTipo())) {
            setContentView(R.layout.ac_main_contas_add_despesa);
        } else {
            setContentView(R.layout.ac_main_contas_add_receita);
        }

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

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }
    }

    public void retornarPrincipal(View view) {
        finish();
    }

    // XML chama esses nomes nos FABs (mantidos)
    public void salvarDespesa(View view) { confirmarEdicao(); }
    public void salvarProvento(View view) { confirmarEdicao(); }
    public void salvarProventos(View view) { confirmarEdicao(); }

    /**
     * Edição no schema novo:
     * - Atualiza o MESMO documento /contasMovimentacoes/{movId}
     * - NÃO mexe em createdAt (para não virar "último")
     * - Atualiza diaKey/mesKey conforme a data corrigida
     */
    private void confirmarEdicao() {
        String novaData = editData.getText().toString().trim();
        String novaDescricao = editDescricao.getText().toString().trim();
        String novaCategoria = editCategoria.getText().toString().trim();
        String novoValorStr = editValor.getText().toString().trim();
        String novaHora = editHora.getText().toString().trim();

        if (novaData.isEmpty() || novaDescricao.isEmpty() || novaCategoria.isEmpty() || novoValorStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double novoValor;
        try {
            novoValor = Double.parseDouble(novoValorStr);
        } catch (Exception e) {
            Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // recalcula keys
        Date dateObj = parseBrDate(novaData);
        String diaKey = FirestoreSchema.diaKey(dateObj);
        String mesKey = FirestoreSchema.mesKey(dateObj);

        // tipo novo (não muda na tela)
        String tipoNovo = "d".equals(movimentacaoAntiga.getTipo()) ? "Despesa" : "Receita";
        int valorCent = (int) Math.round(novoValor * 100.0);

        Map<String, Object> patch = new HashMap<>();
        patch.put("diaKey", diaKey);
        patch.put("mesKey", mesKey);
        patch.put("tipo", tipoNovo);
        patch.put("valorCent", valorCent);

        patch.put("data", novaData);
        patch.put("hora", novaHora);
        patch.put("descricao", novaDescricao);
        patch.put("categoriaNome", novaCategoria);

        // crucial: só updatedAt (não altera createdAt)
        patch.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ===== EXCLUSÃO =====

    private void confirmarExclusao() {
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

        textMensagem.setText("Você deseja realmente excluir '" + movimentacaoAntiga.getDescricao() + "'?");

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            excluirNoFirestore();
        });

        dialog.show();
    }

    /**
     * Exclui o doc e recalcula o "último" do mesmo tipo.
     * Impacto:
     * - mantém recomendação correta: se apagou o último, pega o anterior.
     * - se não houver anterior, limpa o campo no resumo.
     */
    private void excluirNoFirestore() {
        String tipoNovo = "d".equals(movimentacaoAntiga.getTipo()) ? "Despesa" : "Receita";

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .delete()
                .addOnSuccessListener(unused -> {
                    recalcularUltimoPorTipo(tipoNovo, () -> {
                        Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ===== Recalcular "ultimos" (por createdAt) =====

    private interface SimpleVoidCb { void done(); }

    private void recalcularUltimoPorTipo(String tipoNovo, SimpleVoidCb cb) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("tipo", tipoNovo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        limparUltimoCampo(tipoNovo, cb);
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);

                    Map<String, Object> info = new HashMap<>();
                    info.put("movId", doc.getId());
                    info.put("createdAt", doc.get("createdAt"));
                    info.put("valorCent", doc.get("valorCent"));
                    info.put("categoriaId", doc.get("categoriaId"));
                    info.put("categoriaNomeSnapshot", doc.get("categoriaNome"));

                    String campo = "Receita".equalsIgnoreCase(tipoNovo) ? "ultimaEntrada" : "ultimaSaida";

                    Map<String, Object> patch = new HashMap<>();
                    patch.put(campo, info);
                    patch.put("updatedAt", FieldValue.serverTimestamp());

                    FirestoreSchema.contasResumoUltimosDoc()
                            .set(patch, SetOptions.merge())
                            .addOnSuccessListener(v -> cb.done())
                            .addOnFailureListener(e -> cb.done());
                })
                .addOnFailureListener(e -> cb.done());
    }

    private void limparUltimoCampo(String tipoNovo, SimpleVoidCb cb) {
        String campo = "Receita".equalsIgnoreCase(tipoNovo) ? "ultimaEntrada" : "ultimaSaida";

        Map<String, Object> patch = new HashMap<>();
        patch.put(campo, null);
        patch.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSchema.contasResumoUltimosDoc()
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.done())
                .addOnFailureListener(e -> cb.done());
    }

    // ===== Pickers =====

    private void abrirDataPicker() {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = editData.getText().toString().split("/");
            c.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
        } catch (Exception ignored) {}

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
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });
    }

    private Date parseBrDate(String dataBr) {
        if (dataBr == null) return new Date();
        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dataBr);
        } catch (ParseException e) {
            return new Date();
        }
    }
}