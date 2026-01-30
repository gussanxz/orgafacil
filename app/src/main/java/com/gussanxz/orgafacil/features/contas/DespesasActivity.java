package com.gussanxz.orgafacil.features.contas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.gussanxz.orgafacil.data.config.FirestoreSchema;
import com.gussanxz.orgafacil.data.model.Movimentacao;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.data.model.TimePickerHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * DespesasActivity (schema novo)
 *
 * Regras:
 * - NOVO: cria doc novo (key null) => createdAt serverTimestamp => atualiza ultimos
 * - EDIÇÃO: mantém o mesmo movId (key) => NÃO mexe em createdAt (correção)
 */
public class DespesasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    private ActivityResultLauncher<Intent> launcherCategoria;

    private boolean isEdicao = false;
    private Movimentacao itemEmEdicao = null; // contém key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_despesa);
        setupWindowInsets();

        inicializarComponentes();
        configurarListeners();
        configurarLauncherCategoria();
        verificarModoEdicao();
    }

    private void inicializarComponentes() {
        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir);
    }

    private void configurarListeners() {
        campoData.setFocusable(false);
        campoData.setClickable(true);
        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, campoData));

        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, campoHora));

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });
    }

    private void verificarModoEdicao() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("chave")) {
            isEdicao = true;
            itemEmEdicao = new Movimentacao();

            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo("d");
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

            // sugestão barata: 1 doc (Resumos/ultimos)
            recuperarUltimaDespesaDoFirebase();
        }
    }

    public void salvarDespesa(View view) {
        if (!validarCamposDespesas()) return;

        Movimentacao mov = isEdicao ? itemEmEdicao : new Movimentacao();

        mov.setValor(Double.parseDouble(campoValor.getText().toString()));
        mov.setCategoria(campoCategoria.getText().toString());
        mov.setDescricao(campoDescricao.getText().toString());
        mov.setData(campoData.getText().toString());
        mov.setHora(campoHora.getText().toString());
        mov.setTipo("d"); // despesa

        // Aqui está o ponto MAIS IMPORTANTE:
        // - se for edição, NÃO zera key => não recria => não vira "último"
        // - se for novo, key está null => cria doc novo => atualiza ultimos
        String uid = FirestoreSchema.requireUid();
        mov.salvar(uid, mov.getData());

        Toast.makeText(this, isEdicao ? "Despesa atualizada!" : "Despesa adicionada!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Despesa")
                .setMessage("Tem certeza que deseja apagar este lançamento?")
                .setPositiveButton("Sim, excluir", (dialog, which) -> excluirDespesa())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirDespesa() {
        String movId = itemEmEdicao.getKey();
        if (movId == null || movId.trim().isEmpty()) return;

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .delete()
                .addOnSuccessListener(unused -> {
                    recalcularUltimoPorTipo("Despesa", () -> {
                        Toast.makeText(this, "Despesa removida!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ===== Sugestão: Resumos/ultimos =====

    private void recuperarUltimaDespesaDoFirebase() {
        FirestoreSchema.contasResumoUltimosDoc()
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // novo schema
                    Map<String, Object> ultima = (Map<String, Object>) doc.get("ultimaSaida");

                    // fallback legado (se ainda existir no banco antigo)
                    if (ultima == null) ultima = (Map<String, Object>) doc.get("ultimaDespesa");

                    if (ultima == null) return;

                    String cat = (String) ultima.get("categoriaNomeSnapshot");
                    if (cat == null) cat = (String) ultima.get("categoria");

                    String desc = (String) ultima.get("descricao"); // pode não existir no snapshot novo
                    if (desc == null) desc = (String) ultima.get("descricaoSnapshot"); // caso você crie depois

                    Movimentacao m = new Movimentacao();
                    m.setCategoria(cat);
                    m.setDescricao(desc);

                    if (m.getCategoria() != null || m.getDescricao() != null) {
                        mostrarPopupAproveitarUltimaDespesa(m);
                    }
                });
    }

    private void mostrarPopupAproveitarUltimaDespesa(Movimentacao ultima) {
        String categoria = ultima.getCategoria();
        String descricao = ultima.getDescricao();

        String msg = "Aproveitar última despesa?\nCategoria: " + (categoria != null ? categoria : "-") +
                "\nDescrição: " + (descricao != null ? descricao : "-");

        new AlertDialog.Builder(this)
                .setTitle("Sugestão")
                .setMessage(msg)
                .setPositiveButton("Sim", (dialog, which) -> {
                    if (categoria != null) campoCategoria.setText(categoria);
                    if (descricao != null) campoDescricao.setText(descricao);
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

    // ===== Validação/infra =====

    public Boolean validarCamposDespesas() {
        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if (textoValor.isEmpty()) { campoValor.setError("Preencha o valor"); return false; }
        if (textoData.isEmpty()) { Toast.makeText(this, "Preencha a data", Toast.LENGTH_SHORT).show(); return false; }
        if (textoCategoria.isEmpty()) { Toast.makeText(this, "Preencha a categoria", Toast.LENGTH_SHORT).show(); return false; }
        if (textoDescricao.isEmpty()) { campoDescricao.setError("Preencha a descrição"); return false; }

        return true;
    }

    public void retornarPrincipal(View view){ finish(); }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}