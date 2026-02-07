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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual.SelecionarCategoriaContasActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EDITAR MOVIMENTAÇÃO ACTIVITY
 * * O que esta classe faz:
 * 1. Interface de Usuário: Permite que o usuário veja e altere dados de uma transação já existente.
 * 2. Conversão de Dados: Converte o 'Double' da tela para 'Int' (centavos) para o banco.
 * 3. Delegação: Não faz cálculos de saldo. Ela envia o "Antes" e o "Depois" para o MovimentacaoRepository.
 * 4. UX: Mantém a experiência de data/hora pickers e seleção de categorias.
 */
public class EditarMovimentacaoActivity extends AppCompatActivity {

    // Componentes de UI
    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir;

    // Estado da classe
    private MovimentacaoModel movOriginal; // Guarda o estado original para o estorno no Repository
    private MovimentacaoRepository repository; // O "Maestro" que executará as mudanças
    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Recupera o objeto enviado pela tela anterior (Extrato/Resumo)
        movOriginal = (MovimentacaoModel) getIntent().getSerializableExtra("movimentacaoSelecionada");
        repository = new MovimentacaoRepository();

        if (movOriginal == null) {
            finish();
            return;
        }

        // 2. Define o Layout: A mesma tela serve para Receita ou Despesa, mudando apenas o XML
        setContentView(movOriginal.getTipo() == TipoCategoriaContas.DESPESA.getId()
                ? R.layout.ac_main_contas_add_despesa
                : R.layout.ac_main_contas_add_receita);

        inicializarComponentes();
        preencherCampos();
        configurarListeners();
        configurarLauncherCategoria();
    }

    /**
     * Preenche os campos da tela com os dados atuais do objeto vindo do banco.
     */
    private void preencherCampos() {
        // REGRA DE OURO: Convertemos o Int (centavos) do banco para Double (exibição)
        double valorExibicao = movOriginal.getValor() / 100.0;
        editValor.setText(String.format(Locale.US, "%.2f", valorExibicao));

        editDescricao.setText(movOriginal.getDescricao());
        editCategoria.setText(movOriginal.getCategoria_nome());

        // Converte o Timestamp do Firebase para String amigável
        if (movOriginal.getData_movimentacao() != null) {
            Date date = movOriginal.getData_movimentacao().toDate();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date));
            editHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
        }
    }

    /**
     * Lógica disparada ao clicar em "Salvar".
     */
    private void confirmarEdicao() {
        try {
            // 1. Validação e conversão: Double da UI para Int Centavos
            String valorStr = editValor.getText().toString().replace(",", ".");
            int novoValorCentavos = (int) Math.round(Double.parseDouble(valorStr) * 100);

            // 2. Monta o novo objeto com as alterações do usuário
            MovimentacaoModel movNova = new MovimentacaoModel();
            movNova.setId(movOriginal.getId()); // Crucial: manter o mesmo ID do banco
            movNova.setTipo(movOriginal.getTipo());
            movNova.setValor(novoValorCentavos);
            movNova.setDescricao(editDescricao.getText().toString());
            movNova.setCategoria_id(movOriginal.getCategoria_id());
            movNova.setCategoria_nome(editCategoria.getText().toString());
            movNova.setData_movimentacao(movOriginal.getData_movimentacao()); // Mantém a data original ou atualiza se o picker foi usado

            // 3. Delega para o Repository: Ele vai estornar o 'movOriginal' e aplicar o 'movNova'
            repository.editar(movOriginal, movNova, new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Avisa a tela anterior que houve mudança
                    finish();
                }

                @Override
                public void onErro(String erro) {
                    Toast.makeText(EditarMovimentacaoActivity.this, "Erro ao editar: " + erro, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Erro nos valores informados", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Lógica de exclusão com confirmação.
     */
    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Deseja apagar este registro? O saldo será recalculado.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    // O Repository deleta o doc e estorna o valor do resumo geral
                    repository.excluir(movOriginal, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
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

    // --- Métodos de UI e Inicialização ---

    private void inicializarComponentes() {
        textViewHeader = findViewById(R.id.textViewHeader);
        editData = findViewById(R.id.editData);
        editHora = findViewById(R.id.editHora);
        editDescricao = findViewById(R.id.editDescricao);
        editValor = findViewById(R.id.editValor);
        editCategoria = findViewById(R.id.editCategoria);
        btnExcluir = findViewById(R.id.btnExcluir);
    }

    private void configurarListeners() {
        editData.setOnClickListener(v -> abrirDataPicker());
        if (btnExcluir != null) btnExcluir.setOnClickListener(v -> confirmarExclusao());
    }

    // Chamados pelos botões do XML (FABs)
    public void salvarDespesa(View v) { confirmarEdicao(); }
    public void salvarProvento(View v) { confirmarEdicao(); }

    private void abrirDataPicker() {
        // Lógica de DatePickerDialog... (Seu código original aqui)
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        editCategoria.setText(result.getData().getStringExtra("categoriaSelecionada"));
                    }
                });

        editCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            launcherCategoria.launch(intent);
        });
    }
}