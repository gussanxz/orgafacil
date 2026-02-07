package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EditarMovimentacaoActivity
 * Gerencia a edição de lançamentos existentes.
 * [ATUALIZADO]: Sincronizado com o modelo unificado e mapas do Firestore.
 */
public class EditarMovimentacaoActivity extends AppCompatActivity {

    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir;

    private MovimentacaoModel movOriginal;
    private MovimentacaoRepository repository;
    private ActivityResultLauncher<Intent> launcherCategoria;

    private String novoCategoriaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recupera o objeto enviado para edição
        movOriginal = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");
        repository = new MovimentacaoRepository();

        if (movOriginal == null) {
            finish();
            return;
        }

        // Define o layout com base no tipo original (Receita ou Despesa)
        if (movOriginal.getTipo() == TipoCategoriaContas.DESPESA.getId()) {
            setContentView(R.layout.ac_main_contas_add_despesa);
        } else {
            setContentView(R.layout.ac_main_contas_add_receita);
        }

        inicializarComponentes();
        preencherCampos();
        configurarListeners();
        configurarLauncherCategoria();

        novoCategoriaId = movOriginal.getCategoria_id();
    }

    private void inicializarComponentes() {
        textViewHeader = findViewById(R.id.textViewHeader);
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);
        btnExcluir = findViewById(R.id.btnExcluir);

        if (textViewHeader != null) textViewHeader.setText("Editar Lançamento");
        if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);

        editCategoria.setFocusable(false);
        editCategoria.setClickable(true);
    }

    /**
     * Preenche a UI com os dados do modelo via métodos @Exclude (compatibilidade)
     */
    private void preencherCampos() {
        // [PRECISÃO]: Converte centavos para Double apenas para exibição
        double valorExibicao = MoedaHelper.centavosParaDouble(movOriginal.getValor());
        editValor.setText(String.format(Locale.US, "%.2f", valorExibicao));

        editDescricao.setText(movOriginal.getDescricao());
        editCategoria.setText(movOriginal.getCategoria_nome());

        if (movOriginal.getData_movimentacao() != null) {
            Date date = movOriginal.getData_movimentacao().toDate();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date));
            if (editHora != null) {
                editHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
            }
        }
    }

    private void configurarListeners() {
        editData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, editData));
        if (editHora != null) {
            editHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, editHora));
        }
        editCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }
    }

    private void abrirSelecaoCategoria() {
        Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
        launcherCategoria.launch(intent);
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String nomeCat = result.getData().getStringExtra("categoriaSelecionada");
                        String idCat = result.getData().getStringExtra("categoriaId");
                        editCategoria.setText(nomeCat);
                        novoCategoriaId = idCat;
                    }
                });
    }

    // Handlers para os botões do XML (tanto de receita quanto despesa)
    public void salvarDespesa(View v) { confirmarEdicao(); }
    public void salvarProvento(View v) { confirmarEdicao(); }

    /**
     * Confirma a edição e envia para o Repository
     */
    private void confirmarEdicao() {
        try {
            // Criamos uma nova instância baseada na original para preservar IDs e metadados
            MovimentacaoModel movNova = new MovimentacaoModel();

            movNova.setId(movOriginal.getId());
            movNova.setTipoEnum(movOriginal.getTipoEnum()); // Preserva o Tipo original como Enum
            movNova.setData_criacao(movOriginal.getData_criacao());

            // 1. Processamento Financeiro (Converte de volta para Centavos)
            String valorStr = editValor.getText().toString().replace(",", ".");
            double valorDigitado = Double.parseDouble(valorStr);
            movNova.setValor(MoedaHelper.doubleParaCentavos(valorDigitado));

            // 2. Dados de Texto e Categoria
            movNova.setDescricao(editDescricao.getText().toString());
            movNova.setCategoria_nome(editCategoria.getText().toString());
            movNova.setCategoria_id(novoCategoriaId);

            // 3. Data e Lógica de Status (Pago/Pendente)
            String dataHoraStr = editData.getText().toString();
            if (editHora != null && !editHora.getText().toString().isEmpty()) {
                dataHoraStr += " " + editHora.getText().toString();
            } else {
                dataHoraStr += " 00:00";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dataHoraStr);
            if (date != null) {
                movNova.setData_movimentacao(new Timestamp(date));

                // [ATUALIZAÇÃO]: Se o usuário mudar a data para o futuro na edição,
                // o status 'pago' deve mudar automaticamente.
                boolean ehFuturo = date.after(new Date());
                movNova.setPago(!ehFuturo);
            }

            // 4. Envio via Repository (Batch Update)
            repository.editar(movOriginal, movNova, new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onErro(String erro) {
                    Toast.makeText(EditarMovimentacaoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Verifique os dados informados", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarExclusao() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir Lançamento")
                .setMessage("Deseja realmente excluir? O saldo será corrigido automaticamente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    repository.excluir(movOriginal, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(EditarMovimentacaoActivity.this, erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).show();
    }
}