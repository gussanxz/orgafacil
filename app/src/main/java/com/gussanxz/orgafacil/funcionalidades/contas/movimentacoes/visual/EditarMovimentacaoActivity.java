package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual;

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
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper; // Importado
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        movOriginal = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");
        repository = new MovimentacaoRepository();

        if (movOriginal == null) {
            finish();
            return;
        }

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

    private void preencherCampos() {
        // [CORRIGIDO] Usa o MoedaHelper para converter centavos em double para a tela
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

    public void salvarDespesa(View v) { confirmarEdicao(); }
    public void salvarProvento(View v) { confirmarEdicao(); }

    private void confirmarEdicao() {
        try {
            MovimentacaoModel movNova = new MovimentacaoModel();

            movNova.setId(movOriginal.getId());
            movNova.setTipo(movOriginal.getTipo());
            movNova.setData_criacao(movOriginal.getData_criacao());

            // [CORRIGIDO] Captura o double e converte para centavos (int) via MoedaHelper
            String valorStr = editValor.getText().toString().replace(",", ".");
            double valorDigitado = Double.parseDouble(valorStr);
            int novoValorCentavos = MoedaHelper.doubleParaCentavos(valorDigitado);
            movNova.setValor(novoValorCentavos);

            movNova.setDescricao(editDescricao.getText().toString());
            movNova.setCategoria_nome(editCategoria.getText().toString());
            movNova.setCategoria_id(novoCategoriaId);

            String dataHoraStr = editData.getText().toString();
            if (editHora != null) {
                dataHoraStr += " " + editHora.getText().toString();
            } else {
                dataHoraStr += " 00:00";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dataHoraStr);
            if (date != null) {
                movNova.setData_movimentacao(new Timestamp(date));
            }

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
                .setMessage("Deseja realmente excluir? O saldo será corrigido.")
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