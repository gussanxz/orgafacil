package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private final String TAG = "EditarMovActivity";

    private EditText editData, editHora, editDescricao, editValor, editCategoria;
    private TextView textViewHeader;
    private ImageButton btnExcluir;

    private MovimentacaoModel movOriginal;
    private MovimentacaoRepository repository;
    private ActivityResultLauncher<Intent> launcherCategoria;

    private String novoCategoriaId;
    private androidx.appcompat.widget.SwitchCompat switchStatusPago;
    private long valorCentavosAtual = 0;

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
        setupCurrencyMask();
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
            String tituloPrincipal = isDespesa ? "Editar Despesa" : "Editar Receita";
            if (movOriginal.getTotal_parcelas() > 1) {
                tituloPrincipal += " (" + movOriginal.getParcela_atual() + "/" + movOriginal.getTotal_parcelas() + ")";
            }
            textViewHeader.setText(tituloPrincipal);
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

        View layoutRecorrencia = findViewById(R.id.layoutRecorrencia);
        if (layoutRecorrencia != null) {
            layoutRecorrencia.setVisibility(View.GONE);
        }
    }

    private void setupCurrencyMask() {
        editValor.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editValor.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            valorCentavosAtual = Long.parseLong(cleanString);
                            String formatted = MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0);
                            current = formatted;
                            editValor.setText(formatted);
                            editValor.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Erro ao processar máscara");
                        }
                    } else {
                        valorCentavosAtual = 0;
                        editValor.setText("");
                    }
                    editValor.addTextChangedListener(this);
                }
            }
        });
    }

    private void preencherCampos() {
        valorCentavosAtual = movOriginal.getValor();
        editValor.setText(MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0));

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
            editHora.setOnClickListener(v -> abrirSelecionadorDeHora());
        }

        editCategoria.setOnClickListener(v -> abrirSelecaoCategoria());

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }

        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((btn, checked) -> atualizarTextoStatus());
        }
    }

    private void abrirSelecionadorDeData() {
        Calendar c = Calendar.getInstance();
        try {
            Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(editData.getText().toString());
            if(dataAtual != null) c.setTime(dataAtual);
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);

            // Valida a hora logo após mudar a data
            validarLimiteHoraAtual();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // Regra de segurança anti-crash: Só aplica o bloqueio de data máxima
        // se a data ATUAL do campo não estiver no futuro.
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        }

        dialog.show();
    }

    private void abrirSelecionadorDeHora() {
        Calendar agora = Calendar.getInstance();
        int horaSet = agora.get(Calendar.HOUR_OF_DAY);
        int minutoSet = agora.get(Calendar.MINUTE);

        try {
            String[] partes = editHora.getText().toString().split(":");
            horaSet = Integer.parseInt(partes[0]);
            minutoSet = Integer.parseInt(partes[1]);
        } catch (Exception ignored) {}

        android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));

            // Valida assim que o usuário confirma a hora
            validarLimiteHoraAtual();
        }, horaSet, minutoSet, true);

        timePicker.show();
    }

    private void validarLimiteHoraAtual() {
        try {
            Date dataSelecionada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(editData.getText().toString());
            if (dataSelecionada == null) return;

            Calendar calSelecionada = Calendar.getInstance();
            calSelecionada.setTime(dataSelecionada);

            Calendar agora = Calendar.getInstance();

            // Só barra a hora se o switch estiver marcado como PAGO (finalizada)
            boolean isPago = switchStatusPago != null && switchStatusPago.isChecked();

            if (isPago && calSelecionada.get(Calendar.YEAR) == agora.get(Calendar.YEAR) &&
                    calSelecionada.get(Calendar.DAY_OF_YEAR) == agora.get(Calendar.DAY_OF_YEAR)) {

                String[] partesHora = editHora.getText().toString().split(":");
                int horaCampo = Integer.parseInt(partesHora[0]);
                int minCampo = Integer.parseInt(partesHora[1]);

                int horaAgora = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    Toast.makeText(this, "Horário ajustado: movimentações finalizadas não podem estar no futuro.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
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
        if (valorCentavosAtual <= 0) {
            editValor.setError("Preencha um valor válido");
            return;
        }

        try {
            MovimentacaoModel movNova = new MovimentacaoModel();
            movNova.setId(movOriginal.getId());
            movNova.setTipoEnum(movOriginal.getTipoEnum());
            movNova.setData_criacao(movOriginal.getData_criacao());

            movNova.setRecorrencia_id(movOriginal.getRecorrencia_id());
            movNova.setParcela_atual(movOriginal.getParcela_atual());
            movNova.setTotal_parcelas(movOriginal.getTotal_parcelas());

            movNova.setValor(valorCentavosAtual);
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

            // [NOVO] LÓGICA DO EFEITO GOOGLE AGENDA
            if (movOriginal.getTotal_parcelas() > 1 && movOriginal.getParcela_atual() < movOriginal.getTotal_parcelas()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Atualizar Série");
                builder.setMessage("Esta é uma conta com repetição. Deseja aplicar as alterações de valor e categoria apenas a esta parcela ou a todas as seguintes também?");

                builder.setPositiveButton("Esta e Seguintes", (dialog, which) -> {
                    enviarEdicaoAoBanco(movNova, true);
                });

                builder.setNegativeButton("Apenas Esta", (dialog, which) -> {
                    enviarEdicaoAoBanco(movNova, false);
                });

                builder.setNeutralButton("Cancelar", null);
                builder.show();

            } else {
                enviarEdicaoAoBanco(movNova, false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar", e);
            Toast.makeText(this, "Erro: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void enviarEdicaoAoBanco(MovimentacaoModel movNova, boolean todasSeguintes) {
        repository.editarMultiplos(movOriginal, movNova, todasSeguintes, new MovimentacaoRepository.Callback() {
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
    }

    private void confirmarExclusao() {
        if (movOriginal.getTotal_parcelas() > 1 && movOriginal.getParcela_atual() < movOriginal.getTotal_parcelas()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Excluir Lançamento Repetido");
            builder.setMessage("Como deseja tratar a exclusão desta conta?");

            // Opção 1: Cancela tudo dali pra frente
            builder.setPositiveButton("Esta e Seguintes", (dialog, which) -> {
                enviarExclusaoAoBanco(true);
            });

            // Opção 2: Deixa o "buraco" de isenção
            builder.setNegativeButton("Apenas Esta", (dialog, which) -> {
                Toast.makeText(this, "Apenas este mês será removido. As próximas estão mantidas.", Toast.LENGTH_LONG).show();
                enviarExclusaoAoBanco(false);
            });

            builder.setNeutralButton("Cancelar", null);
            builder.show();

        } else {
            // Conta única ou última parcela: só confirmar normalmente
            new AlertDialog.Builder(this)
                    .setTitle("Excluir Lançamento")
                    .setMessage("Deseja realmente excluir? O saldo será corrigido automaticamente.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Excluir", (dialog, which) -> enviarExclusaoAoBanco(false))
                    .show();
        }
    }

    private void enviarExclusaoAoBanco(boolean todasSeguintes) {
        repository.excluirMultiplos(movOriginal, todasSeguintes, new MovimentacaoRepository.Callback() {
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
    }

    public void retornarPrincipal(View view) {
        finish();
    }
}