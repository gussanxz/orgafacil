package com.gussanxz.orgafacil.activity.main.contas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.DatePickerHelper;
import com.gussanxz.orgafacil.model.TimePickerHelper;

import java.util.Map;

public class DespesasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private Movimentacao movimentacao;
    private FirebaseFirestore fs;
    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_despesa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fs = ConfiguracaoFirestore.getFirestore();

        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);

        campoData.setText(DatePickerHelper.setDataAtual());
        campoData.setFocusable(false);
        campoData.setClickable(true);
        campoData.setOnClickListener(v ->
                DatePickerHelper.showDatePickerDialog(DespesasActivity.this, campoData));

        campoHora.setText(TimePickerHelper.setHoraAtual());
        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v ->
                TimePickerHelper.showTimePickerDialog(DespesasActivity.this, campoHora));

        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });
        
        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(DespesasActivity.this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });

        // 🔹 NOVO: buscar no Firebase a última despesa e oferecer para reaproveitar
        recuperarUltimaDespesaDoFirebase();
    }

    public void retornarPrincipal(View view){
        finish(); // Apenas fecha a activity atual, voltando para a anterior (ContasActivity)
    }

    public void salvarDespesa(View view) {

        if (validarCamposDespesas()) {

            movimentacao = new Movimentacao();
            String data = campoData.getText().toString();
            Double valorRecuperado = Double.parseDouble(campoValor.getText().toString());
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            movimentacao.setValor( valorRecuperado );
            movimentacao.setCategoria(campoCategoria.getText().toString());
            movimentacao.setDescricao(campoDescricao.getText().toString());
            movimentacao.setData(data);
            movimentacao.setHora(campoHora.getText().toString());
            movimentacao.setTipo("d");

            movimentacao.salvar(uid, data);

            Toast.makeText(this, "Despesa adicionada!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public Boolean validarCamposDespesas() {

        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if ( !textoValor.isEmpty()) {
            if ( !textoData.isEmpty()) {
                if ( !textoCategoria.isEmpty()) {
                    if ( !textoDescricao.isEmpty()) {

                        return true;

                    } else {
                        Toast.makeText(DespesasActivity.this,
                                "Descrição não foi preenchida!", Toast.LENGTH_SHORT).show();
                        return false;

                    }
                } else {
                    Toast.makeText(DespesasActivity.this,
                            "Categoria não foi preenchida!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(DespesasActivity.this,
                        "Data não foi preenchida!", Toast.LENGTH_SHORT).show();
                return false;

            }
        } else {
            Toast.makeText(DespesasActivity.this,
                    "Valor não foi preenchido!", Toast.LENGTH_SHORT).show();
            return false;

        }
    }

    // =========================
    // 🔹 NOVO: BUSCAR ÚLTIMA DESPESA NO FIREBASE E MOSTRAR POPUP
    // =========================
    private void recuperarUltimaDespesaDoFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("_meta").document("ultimos")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Map<String, Object> ultima = (Map<String, Object>) doc.get("ultimaDespesa");
                    if (ultima == null) return;

                    Movimentacao m = new Movimentacao();
                    m.setCategoria((String) ultima.get("categoria"));
                    m.setDescricao((String) ultima.get("descricao"));

                    if (m.getCategoria() != null || m.getDescricao() != null) {
                        mostrarPopupAproveitarUltimaDespesa(m);
                    }
                })
                .addOnFailureListener(e -> {
                    // silencioso
                });
    }


    private java.util.Date parseDataHora(String dataStr, String horaStr) {
        try {
            if (dataStr == null || dataStr.isEmpty()) return null;
            if (horaStr == null || horaStr.isEmpty()) horaStr = "00:00";

            // Seu formato de data/hora: "dd/MM/yyyy" e "HH:mm"
            String texto = dataStr + " " + horaStr;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            return sdf.parse(texto);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarPopupAproveitarUltimaDespesa(Movimentacao ultima) {
        String categoria = ultima.getCategoria();
        String descricao = ultima.getDescricao();

        String categoriaLabel = TextUtils.isEmpty(categoria)
                ? "sem categoria"
                : categoria;

        String descricaoLabel = TextUtils.isEmpty(descricao)
                ? "sem descrição"
                : descricao;

        String mensagem = "Deseja aproveitar as informações da última despesa?\n\n"
                + "Categoria: " + categoriaLabel + "\n"
                + "Descrição do produto ou serviço: " + descricaoLabel
                + "\n\nOu prefere começar do zero?";

        new AlertDialog.Builder(this)
                .setTitle("Aproveitar último lançamento")
                .setMessage(mensagem)
                .setPositiveButton("Aproveitar", (dialog, which) -> {
                    if (!TextUtils.isEmpty(categoria)) {
                        campoCategoria.setText(categoria);
                    }
                    if (!TextUtils.isEmpty(descricao)) {
                        campoDescricao.setText(descricao);
                    }
                })
                .setNegativeButton("Começar do zero", null)
                .show();
    }
}