package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual;

import android.app.Activity;
import android.app.AlertDialog; // Cuidado: Use androidx.appcompat.app.AlertDialog preferencialmente
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
// [ATUALIZADO] Import correto (sem r_negocio)
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditarMovimentacaoActivity extends AppCompatActivity {

    // UI
    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir;

    // Estado
    private MovimentacaoModel movOriginal;
    private MovimentacaoRepository repository;
    private ActivityResultLauncher<Intent> launcherCategoria;

    // Controle de Categoria (ID é crucial)
    private String novoCategoriaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Recupera o objeto
        movOriginal = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");
        repository = new MovimentacaoRepository();

        if (movOriginal == null) {
            finish();
            return;
        }

        // 2. Define o Layout (Reusa os layouts de criação)
        if (movOriginal.getTipo() == TipoCategoriaContas.DESPESA.getId()) {
            setContentView(R.layout.ac_main_contas_add_despesa);
        } else {
            setContentView(R.layout.ac_main_contas_add_receita);
        }

        inicializarComponentes();
        preencherCampos();
        configurarListeners();
        configurarLauncherCategoria();

        // Inicializa o ID da categoria com o atual
        novoCategoriaId = movOriginal.getCategoria_id();
    }

    private void inicializarComponentes() {
        // IDs devem bater com o layout XML (ac_main_contas_add_despesa / receita)
        textViewHeader = findViewById(R.id.textViewHeader);
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora); // Certifique-se que existe no XML
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);
        btnExcluir = findViewById(R.id.btnExcluir);

        if (textViewHeader != null) textViewHeader.setText("Editar Lançamento");

        // Se o botão excluir existir no layout, deixa visível
        if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);

        // Bloqueia a edição manual da categoria
        editCategoria.setFocusable(false);
        editCategoria.setClickable(true);
    }

    private void preencherCampos() {
        // Converte Centavos -> Double
        double valorExibicao = movOriginal.getValor() / 100.0;
        editValor.setText(String.format(Locale.US, "%.2f", valorExibicao));

        editDescricao.setText(movOriginal.getDescricao());
        editCategoria.setText(movOriginal.getCategoria_nome());

        // Converte Timestamp -> String
        if (movOriginal.getData_movimentacao() != null) {
            Date date = movOriginal.getData_movimentacao().toDate();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date));
            if (editHora != null) {
                editHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
            }
        }
    }

    private void configurarListeners() {
        // Usa os Helpers de Data/Hora se disponíveis
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
                        novoCategoriaId = idCat; // Atualiza o ID para salvar
                    }
                });
    }

    // --- AÇÃO DE SALVAR ---

    // Vinculado ao botão "Salvar" no XML (onClick)
    public void salvarDespesa(View v) { confirmarEdicao(); }
    public void salvarProvento(View v) { confirmarEdicao(); }

    private void confirmarEdicao() {
        try {
            // 1. Cria objeto NOVO com os dados da tela
            MovimentacaoModel movNova = new MovimentacaoModel();

            // Mantém IDs e Metadados imutáveis
            movNova.setId(movOriginal.getId());
            movNova.setTipo(movOriginal.getTipo());
            movNova.setData_criacao(movOriginal.getData_criacao());

            // Atualiza Valor (Double -> Centavos)
            String valorStr = editValor.getText().toString().replace(",", ".");
            int novoValorCentavos = (int) Math.round(Double.parseDouble(valorStr) * 100);
            movNova.setValor(novoValorCentavos);

            // Atualiza Descrição
            movNova.setDescricao(editDescricao.getText().toString());

            // Atualiza Categoria
            movNova.setCategoria_nome(editCategoria.getText().toString());
            movNova.setCategoria_id(novoCategoriaId); // Usa o ID atualizado ou o original

            // Atualiza Data e Hora
            String dataHoraStr = editData.getText().toString();
            if (editHora != null) {
                dataHoraStr += " " + editHora.getText().toString();
            } else {
                dataHoraStr += " 00:00"; // Fallback
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dataHoraStr);
            if (date != null) {
                movNova.setData_movimentacao(new Timestamp(date));
            }

            // 2. Chama o Repository para fazer a troca (Estorno + Novo Lançamento)
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