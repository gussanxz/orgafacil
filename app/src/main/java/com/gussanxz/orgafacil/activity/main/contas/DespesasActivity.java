package com.gussanxz.orgafacil.activity.main.contas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // IMPORTANTE
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.helper.DatePickerHelper;
import com.gussanxz.orgafacil.model.TimePickerHelper;
import com.gussanxz.orgafacil.repository.ContasRepository; // IMPORTANTE

import java.util.Map;

public class DespesasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private Button btnExcluir;
    private Movimentacao movimentacao;
    private FirebaseFirestore fs;
    private ActivityResultLauncher<Intent> launcherCategoria;
    private ContasRepository repository; // NOVO

    // Variáveis de controle de edição
    private boolean isEdicao = false;
    private Movimentacao itemEmEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_despesa);

        setupWindowInsets();

        fs = ConfiguracaoFirestore.getFirestore();
        repository = new ContasRepository(); // Inicializa repositório

        inicializarComponentes();
        configurarListeners();
        configurarLauncherCategoria();

        // Verifica se veio da tela de detalhes/lista para editar
        verificarModoEdicao();
    }

    private void inicializarComponentes() {
        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir); // Recupera o botão do XML
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

    // --- LÓGICA DE EDIÇÃO / EXCLUSÃO ---

    private void verificarModoEdicao() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("chave")) {
            isEdicao = true;
            itemEmEdicao = new Movimentacao();

            // Recupera dados passados pela Intent
            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo(extras.getString("tipo")); // "d" ou "r"
            // Se tiver hora na intent, recupera, senão deixa vazio
            if(extras.containsKey("hora")) itemEmEdicao.setHora(extras.getString("hora"));

            // Preenche a tela
            campoValor.setText(String.valueOf(itemEmEdicao.getValor()));
            campoCategoria.setText(itemEmEdicao.getCategoria());
            campoDescricao.setText(itemEmEdicao.getDescricao());
            campoData.setText(itemEmEdicao.getData());
            if(itemEmEdicao.getHora() != null) campoHora.setText(itemEmEdicao.getHora());

            // Mostra o botão de excluir
            btnExcluir.setVisibility(View.VISIBLE);

            // Muda título se quiser
            // ((TextView)findViewById(R.id.textViewHeader)).setText("Editar Despesa");

        } else {
            // Modo Novo Cadastro
            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());
            btnExcluir.setVisibility(View.GONE);

            // Só busca sugestão se for NOVO cadastro
            recuperarUltimaDespesaDoFirebase();
        }
    }

    public void excluirDespesa(View view) {
        if (!isEdicao || itemEmEdicao == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Excluir Despesa")
                .setMessage("Tem certeza que deseja apagar este lançamento?")
                .setPositiveButton("Sim, excluir", (dialog, which) -> {

                    // Usa o repositório para excluir
                    repository.excluirMovimentacao(itemEmEdicao, new ContasRepository.SimplesCallback() {
                        @Override
                        public void onSucesso() {
                            Toast.makeText(DespesasActivity.this, "Despesa removida!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(DespesasActivity.this, "Erro ao excluir: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- SALVAR (ALTERADO PARA SUPORTAR EDIÇÃO) ---

    public void salvarDespesa(View view) {
        if (!validarCamposDespesas()) return;

        // Se for edição, usamos o objeto existente, senão criamos novo
        movimentacao = isEdicao ? itemEmEdicao : new Movimentacao();

        String data = campoData.getText().toString();
        Double valorAtual = Double.parseDouble(campoValor.getText().toString());
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Se for edição e o valor mudou, precisamos lidar com a atualização do saldo.
        // O jeito mais simples e seguro (atomicamente) é excluir o antigo e salvar o novo.
        // Ou, se sua função 'salvar' já lida com update de saldo (increment/decrement), ok.
        // Assumindo aqui a lógica padrão de salvar novo:

        movimentacao.setValor(valorAtual);
        movimentacao.setCategoria(campoCategoria.getText().toString());
        movimentacao.setDescricao(campoDescricao.getText().toString());
        movimentacao.setData(data);
        movimentacao.setHora(campoHora.getText().toString());
        movimentacao.setTipo("d");

        // Se for edição, o ideal é remover o valor antigo do saldo total antes de salvar o novo
        // Para simplificar: salvar() sobrescreve o documento se tiver ID, mas o saldo total precisa de ajuste.
        // SOLUÇÃO ROBUSTA: No seu caso, sugiro APAGAR o antigo e CRIAR um novo se for edição de valor/data.

        if (isEdicao) {
            // 1. Exclui o antigo (ajusta saldo)
            // Nota: Para fazer isso perfeito, precisaria ser uma Transação (Batch).
            // Aqui faremos simplificado: Exclui -> Sucesso -> Salva Novo.
            repository.excluirMovimentacao(itemEmEdicao, new ContasRepository.SimplesCallback() {
                @Override
                public void onSucesso() {
                    // 2. Salva o novo (ajusta saldo novamente com valor novo)
                    // Resetamos a chave para gerar novo ID ou usamos a mesma se preferir manter histórico
                    // Melhor gerar novo ID se mudar o Mês (data).
                    movimentacao.setKey(null);
                    movimentacao.salvar(uid, data);
                    finalizarSalvar();
                }
                @Override
                public void onErro(String erro) {
                    Toast.makeText(DespesasActivity.this, "Erro ao atualizar: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Novo cadastro direto
            movimentacao.salvar(uid, data);
            finalizarSalvar();
        }
    }

    private void finalizarSalvar() {
        Toast.makeText(this, isEdicao ? "Despesa atualizada!" : "Despesa adicionada!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // --- MÉTODOS AUXILIARES (MANTIDOS) ---

    public Boolean validarCamposDespesas() {
        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if (textoValor.isEmpty()) {
            campoValor.setError("Preencha o valor"); return false;
        }
        if (textoData.isEmpty()) {
            Toast.makeText(this, "Preencha a data", Toast.LENGTH_SHORT).show(); return false;
        }
        if (textoCategoria.isEmpty()) {
            Toast.makeText(this, "Preencha a categoria", Toast.LENGTH_SHORT).show(); return false;
        }
        if (textoDescricao.isEmpty()) {
            campoDescricao.setError("Preencha a descrição"); return false;
        }
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

    // Código de Recuperar Ultima Despesa (Mantido igual)
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
                });
    }

    private void mostrarPopupAproveitarUltimaDespesa(Movimentacao ultima) {
        String categoria = ultima.getCategoria();
        String descricao = ultima.getDescricao();
        String msg = "Aproveitar última despesa?\nCategoria: " + (categoria!=null?categoria:"-") +
                "\nDescrição: " + (descricao!=null?descricao:"-");

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
}