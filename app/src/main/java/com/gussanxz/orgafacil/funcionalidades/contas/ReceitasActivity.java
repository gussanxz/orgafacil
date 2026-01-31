package com.gussanxz.orgafacil.funcionalidades.contas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.util.HashMap;
import java.util.Map;

public class ReceitasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    private ActivityResultLauncher<Intent> launcherCategoria;

    private boolean isEdicao = false;
    private MovimentacaoModel itemEmEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_receita);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir);

        campoData.setFocusable(false);
        campoData.setClickable(true);
        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, campoData));

        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, campoHora));

        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }

        verificarModoEdicao();
    }

    public void salvarProventos(View view) {
        if (!validarCamposProventos()) return;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        mov.setValor(Double.parseDouble(campoValor.getText().toString()));
        mov.setCategoria(campoCategoria.getText().toString());
        mov.setDescricao(campoDescricao.getText().toString());
        mov.setData(campoData.getText().toString());
        mov.setHora(campoHora.getText().toString());
        mov.setTipo("r");

        // regra: edição não recria (não zera key)
        String uid = FirestoreSchema.requireUid();
        mov.salvar(uid, mov.getData());

        Toast.makeText(this, isEdicao ? "Provento atualizado!" : "Provento adicionado!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Provento")
                .setMessage("Tem certeza que deseja apagar este lançamento?")
                .setPositiveButton("Sim, excluir", (dialog, which) -> excluirProvento())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirProvento() {
        String movId = itemEmEdicao.getKey();
        if (movId == null || movId.trim().isEmpty()) return;

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .delete()
                .addOnSuccessListener(unused -> {
                    recalcularUltimoPorTipo("Receita", () -> {
                        Toast.makeText(this, "Provento removido!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void verificarModoEdicao() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("chave")) {
            isEdicao = true;
            itemEmEdicao = new MovimentacaoModel();

            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo("r");
            if (extras.containsKey("hora")) itemEmEdicao.setHora(extras.getString("hora"));

            campoValor.setText(String.valueOf(itemEmEdicao.getValor()));
            campoCategoria.setText(itemEmEdicao.getCategoria());
            campoDescricao.setText(itemEmEdicao.getDescricao());
            campoData.setText(itemEmEdicao.getData());
            if (itemEmEdicao.getHora() != null) campoHora.setText(itemEmEdicao.getHora());

            btnExcluir.setVisibility(View.VISIBLE);
        } else {
            isEdicao = false;
            itemEmEdicao = null;

            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());
            btnExcluir.setVisibility(View.GONE);

            recuperarUltimoProventoDoFirebase();
        }
    }

    public void retornarPrincipal(View view) {
        finish();
    }

    public Boolean validarCamposProventos() {
        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if (textoValor.isEmpty()) { Toast.makeText(this, "Valor não foi preenchido!", Toast.LENGTH_SHORT).show(); return false; }
        if (textoData.isEmpty()) { Toast.makeText(this, "Data não foi preenchida!", Toast.LENGTH_SHORT).show(); return false; }
        if (textoCategoria.isEmpty()) { Toast.makeText(this, "Categoria não foi preenchida!", Toast.LENGTH_SHORT).show(); return false; }
        if (textoDescricao.isEmpty()) { Toast.makeText(this, "Descrição não foi preenchida!", Toast.LENGTH_SHORT).show(); return false; }

        return true;
    }

    // ===== Sugestão =====

    private void recuperarUltimoProventoDoFirebase() {
        FirestoreSchema.contasResumoUltimosDoc()
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Map<String, Object> ultimo = (Map<String, Object>) doc.get("ultimaEntrada");
                    if (ultimo == null) ultimo = (Map<String, Object>) doc.get("ultimoProvento"); // fallback legado
                    if (ultimo == null) return;

                    String cat = (String) ultimo.get("categoriaNomeSnapshot");
                    if (cat == null) cat = (String) ultimo.get("categoria");

                    String desc = (String) ultimo.get("descricao");
                    if (desc == null) desc = (String) ultimo.get("descricaoSnapshot");

                    MovimentacaoModel m = new MovimentacaoModel();
                    m.setCategoria(cat);
                    m.setDescricao(desc);

                    if (m.getCategoria() != null || m.getDescricao() != null) {
                        mostrarPopupAproveitarUltimoProvento(m);
                    }
                });
    }

    private void mostrarPopupAproveitarUltimoProvento(MovimentacaoModel ultimo) {
        String categoria = ultimo.getCategoria();
        String descricao = ultimo.getDescricao();

        String categoriaLabel = TextUtils.isEmpty(categoria) ? "sem categoria" : categoria;
        String descricaoLabel = TextUtils.isEmpty(descricao) ? "sem descrição" : descricao;

        String mensagem = "Deseja aproveitar as informações do último provento?\n\n"
                + "Categoria: " + categoriaLabel + "\n"
                + "Descrição: " + descricaoLabel;

        new AlertDialog.Builder(this)
                .setTitle("Sugestão")
                .setMessage(mensagem)
                .setPositiveButton("Aproveitar", (dialog, which) -> {
                    if (!TextUtils.isEmpty(categoria)) campoCategoria.setText(categoria);
                    if (!TextUtils.isEmpty(descricao)) campoDescricao.setText(descricao);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    // ===== Recalcular "ultimos" por createdAt =====

    private interface SimpleVoidCb { void done(); }

    private void recalcularUltimoPorTipo(String tipoNovo, SimpleVoidCb cb) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("tipo", tipoNovo)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        limparUltimoCampo(tipoNovo, cb);
                        return;
                    }

                    Map<String, Object> info = new HashMap<>();
                    info.put("movId", snap.getDocuments().get(0).getId());
                    info.put("createdAt", snap.getDocuments().get(0).get("createdAt"));
                    info.put("valorCent", snap.getDocuments().get(0).get("valorCent"));
                    info.put("categoriaId", snap.getDocuments().get(0).get("categoriaId"));
                    info.put("categoriaNomeSnapshot", snap.getDocuments().get(0).get("categoriaNome"));

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
}