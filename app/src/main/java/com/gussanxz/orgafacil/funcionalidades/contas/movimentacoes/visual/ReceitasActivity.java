package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ReceitasActivity
 * Responsável por criar ou editar uma receita (entrada financeira).
 *
 * ATUALIZAÇÕES (Refatoração):
 * 1. Uso de MovimentacaoRepository novo (Batch Write).
 * 2. Conversão de valores para Centavos (int) antes de salvar.
 * 3. Uso de Timestamp para datas.
 */
public class ReceitasActivity extends AppCompatActivity {

    private final String TAG = "ReceitasActivity";

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

    // Formatação de Moeda em Tempo Real
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private double valorAtualDigitado = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_receita);

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

        // Setup inicial da máscara monetária
        setupCurrencyMask();

        // Processamento de Intent (Atalhos e Headers Dinâmicos)
        TextView textViewHeader = findViewById(R.id.textViewHeader);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String titulo = extras.getString("TITULO_TELA");
            boolean ehAtalho = extras.getBoolean("EH_ATALHO", false);

            if (titulo != null) textViewHeader.setText(titulo);
            if (ehAtalho) aplicarRegrasAtalho();
        }

        // Verifica se é Edição APÓS processar os extras visuais
        verificarModoEdicao();
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
                        // [CORREÇÃO]: Captura Nome E ID da categoria
                        String nomeCat = result.getData().getStringExtra("categoriaSelecionada");
                        categoriaIdSelecionada = result.getData().getStringExtra("categoriaId");

                        campoCategoria.setText(nomeCat);
                    }
                });
    }

    /**
     * Configura a máscara de dinheiro (R$) no campo de valor.
     */
    private void setupCurrencyMask() {
        campoValor.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    campoValor.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[R$,.\\s]", "");
                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);
                        // Armazena o valor real (ex: 1050 para R$ 10,50)
                        valorAtualDigitado = parsed / 100;
                        String formatted = currencyFormat.format(valorAtualDigitado);
                        current = formatted;
                        campoValor.setText(formatted);
                        campoValor.setSelection(formatted.length());
                    } else {
                        valorAtualDigitado = 0.0;
                        campoValor.setText("");
                    }
                    campoValor.addTextChangedListener(this);
                }
            }
        });
    }

    private void verificarModoEdicao() {
        // [ATENÇÃO]: O objeto passado deve ser Serializable ou Parcelable
        MovimentacaoModel movRecebida = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");

        if (movRecebida != null) {
            isEdicao = true;
            itemEmEdicao = movRecebida;

            // Recupera IDs ocultos
            categoriaIdSelecionada = itemEmEdicao.getCategoria_id();

            // Popula UI: Converte Centavos (int) para Double visual e aplica formatação
            double valorReais = itemEmEdicao.getValor() / 100.0;
            // O TextWatcher vai formatar automaticamente, mas precisamos setar o valor base
            valorAtualDigitado = valorReais;
            campoValor.setText(currencyFormat.format(valorReais));

            campoCategoria.setText(itemEmEdicao.getCategoria_nome());
            campoDescricao.setText(itemEmEdicao.getDescricao());

            // Converte Timestamp para String de Data/Hora
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

            // Sugestão de última receita removida pois o novo Repository foca em Batch Writes atômicos
        }
    }

    public void salvarProventos(View view) {
        if (!validarCamposProventos()) return;

        // Montagem do Objeto
        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        // 1. Valor: Usa o valor digitado (Double) e converte para Centavos (INT)
        mov.setValor((int) Math.round(valorAtualDigitado * 100));

        // 2. Dados básicos
        mov.setDescricao(campoDescricao.getText().toString());
        mov.setTipo(TipoCategoriaContas.RECEITA.getId()); // Enum ID = 1 (Receita)

        // 3. Categoria (Dados desnormalizados)
        mov.setCategoria_nome(campoCategoria.getText().toString());
        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            // Fallback para evitar crash se não selecionar categoria
            mov.setCategoria_id("geral_receita");
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
                .setTitle("Excluir Receita")
                .setMessage("Deseja apagar este lançamento? O saldo será corrigido.")
                .setPositiveButton("Sim", (dialog, which) -> excluirReceita())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirReceita() {
        if (itemEmEdicao == null) return;

        // [CORREÇÃO]: Excluir pelo objeto para permitir estorno de saldo
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) { finalizarSucesso(msg); }
            @Override
            public void onErro(String erro) { mostrarErro(erro); }
        });
    }

    // --- Helpers de UI ---

    private void finalizarSucesso(String msg) {
        Toast.makeText(ReceitasActivity.this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // Avisa a Activity pai que houve mudança
        finish();
    }

    private void mostrarErro(String erro) {
        Toast.makeText(ReceitasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
    }

    public Boolean validarCamposProventos() {
        if (valorAtualDigitado <= 0) {
            campoValor.setError("Valor inválido");
            return false;
        }
        if (campoData.getText().toString().isEmpty()) return false;
        if (campoCategoria.getText().toString().isEmpty()) {
            Toast.makeText(this, "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            return false;
        }
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

    private void aplicarRegrasAtalho() {
        Log.i(TAG, "Regras de atalho aplicada!");
    }
}