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
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.dados.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.util.Map;

public class ReceitasActivity extends AppCompatActivity {

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
        setContentView(R.layout.ac_main_contas_add_receita);

        // 1. Segurança de Sessão
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

            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo("r"); // receita
            if (extras.containsKey("hora")) itemEmEdicao.setHora(extras.getString("hora"));

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
            recuperarSugestaoUltimaReceita();
        }
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

        repository.salvar(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Toast.makeText(ReceitasActivity.this, isEdicao ? "Receita atualizada!" : "Receita adicionada!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ReceitasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Receita")
                .setMessage("Deseja apagar este lançamento?")
                .setPositiveButton("Sim", (dialog, which) -> excluirReceita())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirReceita() {
        repository.excluir(itemEmEdicao.getKey()).addOnSuccessListener(unused -> {
            // Recalcula o ponteiro para 'Receita' (ultimaEntrada)
            repository.recalcularUltimoPonteiro("Receita", () -> {
                Toast.makeText(this, "Receita removida!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show());
    }

    private void recuperarSugestaoUltimaReceita() {
        repository.obterSugestaoUltimos().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Map<String, Object> ultima = (Map<String, Object>) doc.get("ultimaEntrada");
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
                .setMessage("Aproveitar última receita?\nCategoria: " + (cat != null ? cat : "Sem nome"))
                .setPositiveButton("Aproveitar", (dialog, which) -> {
                    if (cat != null) campoCategoria.setText(cat);
                    if (desc != null) campoDescricao.setText(desc);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    public Boolean validarCamposProventos() {
        if (campoValor.getText().toString().isEmpty()) return false;
        if (campoData.getText().toString().isEmpty()) return false;
        if (campoCategoria.getText().toString().isEmpty()) return false;
        return true;
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void retornarPrincipal(View view) { finish(); }
}