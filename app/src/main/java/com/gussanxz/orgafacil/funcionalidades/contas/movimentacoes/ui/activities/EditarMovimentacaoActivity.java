package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    private androidx.appcompat.widget.SwitchCompat switchStatusPago;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            movOriginal = getIntent().getParcelableExtra("movimentacaoSelecionada", MovimentacaoModel.class);
        } else {
            movOriginal = getIntent().getParcelableExtra("movimentacaoSelecionada");
        }

        repository = new MovimentacaoRepository();

        if (movOriginal == null) {
            finish();
            return;
        }

        if (movOriginal.getTipoEnum() == TipoCategoriaContas.DESPESA) {
            setContentView(R.layout.tela_add_despesa);
        } else {
            setContentView(R.layout.tela_add_receita);
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
        switchStatusPago = findViewById(R.id.switchStatusPago);

        boolean isDespesa = movOriginal.getTipoEnum() == TipoCategoriaContas.DESPESA;

        if (textViewHeader != null) {
            textViewHeader.setText(isDespesa ? "Editar Despesa" : "Editar Receita");
        }

        if (editDescricao != null) {
            editDescricao.setHint(isDespesa ? "Descrição da despesa" : "Descrição da receita");
        }

        if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);

        editCategoria.setFocusable(false);
        editCategoria.setClickable(true);

        editData.setFocusable(false);
        editData.setClickable(true);
        if (editHora != null) {
            editHora.setFocusable(false);
            editHora.setClickable(true);
        }
    }

    private void preencherCampos() {
        double valorExibicao = movOriginal.getValor() / 100.0;
        editValor.setText(String.format(Locale.US, "%.2f", valorExibicao));

        editDescricao.setText(movOriginal.getDescricao());
        editCategoria.setText(movOriginal.getCategoria_nome());

        if (movOriginal.getData_movimentacao() != null) {
            Date date = movOriginal.getData_movimentacao().toDate();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date));
            if (editHora != null) {
                editHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
            }

            if (switchStatusPago != null) {
                switchStatusPago.setChecked(movOriginal.isPago());
                aplicarRegraStatusPorData(date);
            }
        }
    }

    private void configurarListeners() {
        editData.setOnClickListener(v -> abrirSelecionadorDeData());
        if (editHora != null) {
            editHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, editHora));
        }
        editCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }
        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((btn, checked) -> atualizarTextoStatus());
        }
    }

    // --- NOVA REGRA DE NEGÓCIO DE DATAS ---
    private void abrirSelecionadorDeData() {
        Calendar c = Calendar.getInstance();
        try {
            Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(editData.getText().toString());
            if(dataAtual != null) c.setTime(dataAtual);
        } catch (Exception ignored) {}

        new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void aplicarRegraStatusPorData(Date data) {
        if (switchStatusPago == null) return;

        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0); hoje.set(Calendar.MINUTE, 0); hoje.set(Calendar.SECOND, 0); hoje.set(Calendar.MILLISECOND, 0);

        Calendar selecionada = Calendar.getInstance();
        selecionada.setTime(data);
        selecionada.set(Calendar.HOUR_OF_DAY, 0); selecionada.set(Calendar.MINUTE, 0); selecionada.set(Calendar.SECOND, 0); selecionada.set(Calendar.MILLISECOND, 0);

        if (selecionada.after(hoje)) {
            switchStatusPago.setChecked(false);
            switchStatusPago.setEnabled(false);
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setEnabled(true);
            atualizarTextoStatus();
        }
    }

    private void atualizarTextoStatus() {
        boolean ok = switchStatusPago.isChecked();
        if (!switchStatusPago.isEnabled() && !ok) {
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
        }
    }
    // ----------------------------------------

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
    public void salvarProventos(View v) { confirmarEdicao(); }

    private void confirmarEdicao() {
        try {
            MovimentacaoModel movNova = new MovimentacaoModel();
            movNova.setId(movOriginal.getId());
            movNova.setTipoEnum(movOriginal.getTipoEnum());
            movNova.setData_criacao(movOriginal.getData_criacao());

            String valorStr = editValor.getText().toString().replace(",", ".");
            double valorDigitado = Double.parseDouble(valorStr);
            movNova.setValor((long)MoedaHelper.doubleParaCentavos(valorDigitado));

            movNova.setDescricao(editDescricao.getText().toString());
            movNova.setCategoria_nome(editCategoria.getText().toString());
            movNova.setCategoria_id(novoCategoriaId);

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
                boolean ok = (switchStatusPago != null)
                        ? switchStatusPago.isChecked()
                        : (movOriginal != null && movOriginal.isPago());
                movNova.setPago(ok);
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
            Log.e("ReceitasActivity", "Erro ao salvar", e);
            Toast.makeText(this, "Erro: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
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

    public void retornarPrincipal(View view) {
        finish();
    }
}