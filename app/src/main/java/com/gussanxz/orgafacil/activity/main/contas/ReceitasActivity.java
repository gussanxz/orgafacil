package com.gussanxz.orgafacil.activity.main.contas;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.DatePickerHelper;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.TimePickerHelper;
import com.gussanxz.orgafacil.repository.ContasRepository; // IMPORTANTE

import java.util.Map;

public class ReceitasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir; // NOVO BOTÃO

    private Movimentacao movimentacao;
    private FirebaseFirestore fs;
    private ContasRepository repository; // NOVO REPOSITÓRIO

    private ActivityResultLauncher<Intent> launcherCategoria;

    // Variáveis de controle de edição
    private boolean isEdicao = false;
    private Movimentacao itemEmEdicao = null;

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

        fs = ConfiguracaoFirestore.getFirestore();
        repository = new ContasRepository(); // Inicializa repositório

        // Inicializar componentes
        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir); // Recupera botão do XML

        // Configurar Listeners
        campoData.setFocusable(false);
        campoData.setClickable(true);
        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(ReceitasActivity.this, campoData));

        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(ReceitasActivity.this, campoHora));

        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(ReceitasActivity.this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });

        // Verifica se é edição (Preenche campos e mostra botão excluir) ou novo cadastro
        verificarModoEdicao();
    }

    // --- MÉTODOS DE LÓGICA (SALVAR/EXCLUIR) ---

    public void salvarProventos(View view) {
        if (!validarCamposProventos()) return;

        // Se for edição, usamos o objeto existente, senão criamos novo
        movimentacao = isEdicao ? itemEmEdicao : new Movimentacao();

        String data = campoData.getText().toString();
        Double valorAtual = Double.parseDouble(campoValor.getText().toString());
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        movimentacao.setValor(valorAtual);
        movimentacao.setCategoria(campoCategoria.getText().toString());
        movimentacao.setDescricao(campoDescricao.getText().toString());
        movimentacao.setData(data);
        movimentacao.setHora(campoHora.getText().toString());
        movimentacao.setTipo("r"); // TIPO RECEITA ("r")

        if (isEdicao) {
            // Se for edição: Exclui o antigo (para ajustar saldo) e cria novo
            repository.excluirMovimentacao(itemEmEdicao, new ContasRepository.SimplesCallback() {
                @Override
                public void onSucesso() {
                    movimentacao.setKey(null); // Reseta chave para criar novo ID
                    movimentacao.salvar(uid, data);
                    finalizarSalvar();
                }
                @Override
                public void onErro(String erro) {
                    Toast.makeText(ReceitasActivity.this, "Erro ao atualizar: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Novo cadastro
            movimentacao.salvar(uid, data);
            finalizarSalvar();
        }
    }

    private void finalizarSalvar() {
        Toast.makeText(this, isEdicao ? "Provento atualizado!" : "Provento adicionado!", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void excluirProvento(View view) {
        if (!isEdicao || itemEmEdicao == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Excluir Provento")
                .setMessage("Tem certeza que deseja apagar este lançamento?")
                .setPositiveButton("Sim, excluir", (dialog, which) -> {

                    repository.excluirMovimentacao(itemEmEdicao, new ContasRepository.SimplesCallback() {
                        @Override
                        public void onSucesso() {
                            Toast.makeText(ReceitasActivity.this, "Provento removido!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ReceitasActivity.this, "Erro ao excluir: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- CONTROLE DE TELA (EDIÇÃO VS NOVO) ---

    private void verificarModoEdicao() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("chave")) {
            isEdicao = true;
            itemEmEdicao = new Movimentacao();

            // Recupera dados
            itemEmEdicao.setKey(extras.getString("chave"));
            itemEmEdicao.setValor(extras.getDouble("valor"));
            itemEmEdicao.setCategoria(extras.getString("categoria"));
            itemEmEdicao.setDescricao(extras.getString("descricao"));
            itemEmEdicao.setData(extras.getString("data"));
            itemEmEdicao.setTipo("r"); // Garante que é Receita
            if(extras.containsKey("hora")) itemEmEdicao.setHora(extras.getString("hora"));

            // Preenche campos
            campoValor.setText(String.valueOf(itemEmEdicao.getValor()));
            campoCategoria.setText(itemEmEdicao.getCategoria());
            campoDescricao.setText(itemEmEdicao.getDescricao());
            campoData.setText(itemEmEdicao.getData());
            if(itemEmEdicao.getHora() != null) campoHora.setText(itemEmEdicao.getHora());

            // MOSTRA botão Excluir
            btnExcluir.setVisibility(View.VISIBLE);

        } else {
            // Novo cadastro
            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());

            // ESCONDE botão Excluir
            btnExcluir.setVisibility(View.GONE);

            // Só busca sugestão se for novo
            recuperarUltimoProventoDoFirebase();
        }
    }

    // --- MÉTODOS AUXILIARES ---

    public void retornarPrincipal(View view) {
        finish();
    }

    public Boolean validarCamposProventos() {
        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if (!textoValor.isEmpty()) {
            if (!textoData.isEmpty()) {
                if (!textoCategoria.isEmpty()) {
                    if (!textoDescricao.isEmpty()) {
                        return true;
                    } else {
                        Toast.makeText(this, "Descrição não foi preenchida!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    Toast.makeText(this, "Categoria não foi preenchida!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "Data não foi preenchida!", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(this, "Valor não foi preenchido!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // --- RECUPERAÇÃO DE SUGESTÃO ---

    private void recuperarUltimoProventoDoFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("_meta").document("ultimos")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Map<String, Object> ultimo = (Map<String, Object>) doc.get("ultimoProvento");
                    if (ultimo == null) return;

                    Movimentacao m = new Movimentacao();
                    m.setCategoria((String) ultimo.get("categoria"));
                    m.setDescricao((String) ultimo.get("descricao"));

                    if (m.getCategoria() != null || m.getDescricao() != null) {
                        mostrarPopupAproveitarUltimoProvento(m);
                    }
                });
    }

    private void mostrarPopupAproveitarUltimoProvento(Movimentacao ultimo) {
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
}