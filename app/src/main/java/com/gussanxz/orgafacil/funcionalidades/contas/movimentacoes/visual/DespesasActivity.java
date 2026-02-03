package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual;

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
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.comum.visual.ui.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.util.Map;

public class DespesasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;

    private boolean isEdicao = false;
    private MovimentacaoModel itemEmEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_despesa);

        // Segurança de Sessão
        if (!FirebaseSession.isUserLogged()) {
            finish();
            return;
        }

        repository = new MovimentacaoRepository();
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
        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, campoData));
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
            itemEmEdicao = new MovimentacaoModel();

            // Preenchimento do model a partir dos extras
            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo("d");
            if (extras.containsKey("hora")) itemEmEdicao.setHora(extras.getString("hora"));

            // UI
            campoValor.setText(String.valueOf(itemEmEdicao.getValor()));
            campoCategoria.setText(itemEmEdicao.getCategoria());
            campoDescricao.setText(itemEmEdicao.getDescricao());
            campoData.setText(itemEmEdicao.getData());
            if (itemEmEdicao.getHora() != null) campoHora.setText(itemEmEdicao.getHora());
            btnExcluir.setVisibility(View.VISIBLE);
        } else {
            isEdicao = false;
            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());
            btnExcluir.setVisibility(View.GONE);
            recuperarSugestaoUltimaDespesa();
        }
    }

    public void salvarDespesa(View view) {
        if (!validarCamposDespesas()) return;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();
        mov.setValor(Double.parseDouble(campoValor.getText().toString()));
        mov.setCategoria(campoCategoria.getText().toString());
        mov.setDescricao(campoDescricao.getText().toString());
        mov.setData(campoData.getText().toString());
        mov.setHora(campoHora.getText().toString());
        mov.setTipo("d");

        // Chama o Repository: Sem UIDs ou lógica de Batch aqui!
        repository.salvar(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Toast.makeText(DespesasActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(DespesasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Despesa")
                .setMessage("Tem certeza?")
                .setPositiveButton("Sim", (dialog, which) -> excluirDespesa())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirDespesa() {
        repository.excluir(itemEmEdicao.getKey()).addOnSuccessListener(unused -> {
            // O segredo do sênior: Após excluir, pede para o repository recalcular o ponteiro
            repository.recalcularUltimoPonteiro("Despesa", () -> {
                Toast.makeText(this, "Despesa removida!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show());
    }

    private void recuperarSugestaoUltimaDespesa() {
        repository.obterSugestaoUltimos().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Map<String, Object> ultima = (Map<String, Object>) doc.get("ultimaSaida");
            if (ultima == null) return;

            String cat = (String) ultima.get("categoriaNomeSnapshot");
            String desc = (String) ultima.get("descricaoSnapshot");

            if (cat != null || desc != null) {
                mostrarPopupSugestao(cat, desc);
            }
        });
    }

    private void mostrarPopupSugestao(String cat, String desc) {
        new AlertDialog.Builder(this)
                .setTitle("Sugestão")
                .setMessage("Aproveitar última despesa?\nCat: " + cat)
                .setPositiveButton("Sim", (dialog, which) -> {
                    if (cat != null) campoCategoria.setText(cat);
                    if (desc != null) campoDescricao.setText(desc);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    public Boolean validarCamposDespesas() {
        if (campoValor.getText().toString().isEmpty()) return false;
        if (campoCategoria.getText().toString().isEmpty()) return false;
        return true; // Simplificado para o exemplo
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void retornarPrincipal(View view){ finish(); }
}