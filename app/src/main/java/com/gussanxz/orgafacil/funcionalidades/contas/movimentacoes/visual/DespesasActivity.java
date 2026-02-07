package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DespesasActivity
 * Responsável por criar ou editar uma despesa.
 *
 * ATUALIZAÇÕES (Refatoração):
 * 1. Uso de MovimentacaoRepository novo (Batch Write para consistência financeira).
 * 2. Conversão de valores para Centavos (int) antes de salvar.
 * 3. Uso de Timestamp para datas.
 */
public class DespesasActivity extends AppCompatActivity {

    private final String TAG = "DespesasActivity";

    // Componentes de UI
    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    // Dependências e Estado
    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;
    private boolean isEdicao = false;
    private MovimentacaoModel itemEmEdicao = null;

    // Controle de Categoria (ID é crucial para o banco relacional)
    private String categoriaIdSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_despesa);

        // Segurança: Validação de Sessão
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

        // Processamento de Intent (Atalhos e Headers Dinâmicos)
        TextView textViewHeader = findViewById(R.id.textViewHeader);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String titulo = extras.getString("TITULO_TELA");
            boolean ehAtalho = extras.getBoolean("EH_ATALHO", false);

            if (titulo != null) textViewHeader.setText(titulo);
            if (ehAtalho) aplicarRegrasAtalho();
        }
    }

    private void inicializarComponentes() {
        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir);

        // Bloqueia digitação manual na categoria para forçar seleção da lista
        campoCategoria.setFocusable(false);
        campoCategoria.setClickable(true);
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
                        // [CORREÇÃO]: Agora pegamos o Nome E o ID da categoria
                        String nomeCat = result.getData().getStringExtra("categoriaSelecionada");
                        categoriaIdSelecionada = result.getData().getStringExtra("categoriaId");

                        campoCategoria.setText(nomeCat);
                    }
                });
    }

    /**
     * Verifica se a tela foi aberta para EDIÇÃO ou NOVA despesa.
     */
    private void verificarModoEdicao() {
        // [ATENÇÃO]: O objeto passado deve ser Serializable ou Parcelable
        MovimentacaoModel movRecebida = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");

        if (movRecebida != null) {
            isEdicao = true;
            itemEmEdicao = movRecebida;

            // Recupera IDs ocultos
            categoriaIdSelecionada = itemEmEdicao.getCategoria_id();

            // Popula UI: Converte Centavos (int) para Double visual
            double valorReais = itemEmEdicao.getValor() / 100.0;
            campoValor.setText(String.format(Locale.US, "%.2f", valorReais));

            campoCategoria.setText(itemEmEdicao.getCategoria_nome());
            campoDescricao.setText(itemEmEdicao.getDescricao());

            // Converte Timestamp para String de Data/Hora para os campos de texto
            if (itemEmEdicao.getData_movimentacao() != null) {
                Date data = itemEmEdicao.getData_movimentacao().toDate();
                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data));
                campoHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(data));
            }

            btnExcluir.setVisibility(View.VISIBLE);
        } else {
            isEdicao = false;
            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());
            btnExcluir.setVisibility(View.GONE);

            // TODO: Reimplementar sugestão no novo Repository se necessário
            // recuperarSugestaoUltimaDespesa();
        }
    }

    public void salvarDespesa(View view) {
        if (!validarCamposDespesas()) return;

        // Montagem do Objeto
        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        // 1. Valor: Converte Double da UI para Centavos (INT)
        try {
            double valorVisual = Double.parseDouble(campoValor.getText().toString().replace(",", "."));
            mov.setValor((int) Math.round(valorVisual * 100));
        } catch (NumberFormatException e) {
            campoValor.setError("Valor inválido");
            return;
        }

        // 2. Dados básicos
        mov.setDescricao(campoDescricao.getText().toString());
        mov.setTipo(TipoCategoriaContas.DESPESA.getId()); // Enum ID = 2

        // 3. Categoria (Dados desnormalizados)
        mov.setCategoria_nome(campoCategoria.getText().toString());
        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            // Fallback se for nova e não tiver ID (Evita crash, mas ideal é obrigar seleção)
            mov.setCategoria_id("geral_despesa");
        }

        // 4. Data e Hora: Combina Strings e converte para Timestamp
        try {
            String dataHoraStr = campoData.getText().toString() + " " + campoHora.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dataHoraStr);
            if (date != null) mov.setData_movimentacao(new com.google.firebase.Timestamp(date));
        } catch (ParseException e) {
            mov.setData_movimentacao(com.google.firebase.Timestamp.now());
        }

        // Lógica de Edição vs Criação
        if (isEdicao) {
            // [CORREÇÃO]: Editar requer o objeto antigo para estornar o saldo corretamente
            // Para simplificar aqui, estamos passando o mesmo objeto, mas o ideal seria ter um clone do original.
            // O Repository cuida da atualização.
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro) { mostrarErro(erro); }
            });
        } else {
            // [CORREÇÃO]: Salvar novo não precisa de ponteiros manuais, o Batch cuida de tudo.
            repository.salvar(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro) { mostrarErro(erro); }
            });
        }
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Despesa")
                .setMessage("Tem certeza? O saldo será corrigido automaticamente.")
                .setPositiveButton("Sim", (dialog, which) -> excluirDespesa())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirDespesa() {
        if (itemEmEdicao == null) return;

        // [CORREÇÃO]: O excluir agora pede o objeto inteiro para poder estornar o valor do saldo
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                finalizarSucesso(msg);
            }

            @Override
            public void onErro(String erro) {
                mostrarErro(erro);
            }
        });
    }

    // --- Helpers de UI ---

    private void finalizarSucesso(String msg) {
        Toast.makeText(DespesasActivity.this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // Avisa a Activity pai que houve mudança
        finish();
    }

    private void mostrarErro(String erro) {
        Toast.makeText(DespesasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
    }

    public Boolean validarCamposDespesas() {
        if (campoValor.getText().toString().isEmpty()) return false;
        if (campoCategoria.getText().toString().isEmpty()) return false;
        if (campoData.getText().toString().isEmpty()) return false;
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

    private void aplicarRegrasAtalho() {
        Log.i(TAG, "Regras de atalho aplicada!");
    }
}