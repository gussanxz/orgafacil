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
    private boolean acaoEmAndamento = false;

    private com.google.android.material.floatingactionbutton.FloatingActionButton fabSuperior, fabInferior;
    private boolean isModoEdicaoAtivo = false;

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

        aplicarModoInicial();

        novoCategoriaId = movOriginal.getCategoria_id();
    }

    private void aplicarModoInicial() {
        boolean direto = getIntent().getBooleanExtra("DIRETO_PRA_EDICAO", false);
        if (direto) {
            alternarModoEdicao(true);
        }
    }

    private void inicializarComponentes() {
        textViewHeader = findViewById(R.id.textViewHeader);
        editData       = findViewById(R.id.editData);
        editHora       = findViewById(R.id.editHora);
        editDescricao  = findViewById(R.id.editDescricao);
        editValor      = findViewById(R.id.editValor);
        editCategoria  = findViewById(R.id.editCategoria);
        btnExcluir     = findViewById(R.id.btnExcluir);
        switchStatusPago = findViewById(R.id.switchStatusPago);

        fabSuperior = findViewById(R.id.fabSuperiorSalvarCategoria);
        fabInferior = findViewById(R.id.fabInferiorSalvarCategoria);

        boolean isDespesa = movOriginal.getTipoEnum() == TipoCategoriaContas.DESPESA;

        if (textViewHeader != null) {
            String tituloPrincipal = isDespesa ? "Detalhes da Despesa" : "Detalhes da Receita";
            if (movOriginal.getTotal_parcelas() > 1) {
                tituloPrincipal += " (" + movOriginal.getParcela_atual()
                        + "/" + movOriginal.getTotal_parcelas() + ")";
            }
            textViewHeader.setText(tituloPrincipal);
        }

        // Banner "Ver série completa" — só aparece se for parcela de uma série
        if (movOriginal.getTotal_parcelas() > 1) {
            View bannerSerie = findViewById(R.id.bannerVerSerie);
            if (bannerSerie != null) {
                bannerSerie.setVisibility(View.VISIBLE);
                TextView txtBanner = bannerSerie.findViewById(R.id.textBannerSerie);
                if (txtBanner != null) {
                    txtBanner.setText("Parcela " + movOriginal.getParcela_atual()
                            + " de " + movOriginal.getTotal_parcelas()
                            + " — Ver série completa →");
                }
                bannerSerie.setOnClickListener(v -> abrirResumoDaSerie());
            }
        }

        if (editDescricao != null) {
            editDescricao.setHint(isDespesa ? "Descrição da despesa" : "Descrição da receita");
        }

        editCategoria.setFocusable(false);
        editData.setFocusable(false);
        if (editHora != null) editHora.setFocusable(false);

        View layoutRecorrencia = findViewById(R.id.layoutRecorrencia);
        if (layoutRecorrencia != null) layoutRecorrencia.setVisibility(View.GONE);

        // Começa no modo visualização
        alternarModoEdicao(false);
    }

    /**
     * Abre a tela de resumo da série de parcelamentos.
     * recorrencia_id identifica o grupo; movOriginal é passado para contexto.
     */
    private void abrirResumoDaSerie() {
        if (movOriginal.getRecorrencia_id() == null || movOriginal.getRecorrencia_id().isEmpty()) {
            Toast.makeText(this, "Esta parcela não tem série vinculada.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ResumoParcelasActivity.class);
        intent.putExtra("recorrencia_id", movOriginal.getRecorrencia_id());
        intent.putExtra("movimentacaoSelecionada", movOriginal);
        startActivity(intent);
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
        if (editHora != null) editHora.setOnClickListener(v -> abrirSelecionadorDeHora());
        editCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        if (btnExcluir != null) btnExcluir.setOnClickListener(v -> confirmarExclusao());
        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((btn, checked) -> atualizarTextoStatus());
        }
    }

    private void abrirSelecionadorDeData() {
        Calendar c = Calendar.getInstance();
        try {
            Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .parse(editData.getText().toString());
            if (dataAtual != null) c.setTime(dataAtual);
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            editData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);
            validarLimiteHoraAtual();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

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

        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            validarLimiteHoraAtual();
        }, horaSet, minutoSet, true).show();
    }

    private void validarLimiteHoraAtual() {
        try {
            Date dataSelecionada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .parse(editData.getText().toString());
            if (dataSelecionada == null) return;

            Calendar calSelecionada = Calendar.getInstance();
            calSelecionada.setTime(dataSelecionada);
            Calendar agora = Calendar.getInstance();

            boolean isPago = switchStatusPago != null && switchStatusPago.isChecked();

            if (isPago
                    && calSelecionada.get(Calendar.YEAR) == agora.get(Calendar.YEAR)
                    && calSelecionada.get(Calendar.DAY_OF_YEAR) == agora.get(Calendar.DAY_OF_YEAR)) {

                String[] partesHora = editHora.getText().toString().split(":");
                int horaCampo = Integer.parseInt(partesHora[0]);
                int minCampo  = Integer.parseInt(partesHora[1]);
                int horaAgora = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora  = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    Toast.makeText(this,
                            "Horário ajustado: movimentações finalizadas não podem estar no futuro.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
    }

    private void aplicarRegraStatusPorData(Date data) {
        if (switchStatusPago == null) return;

        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0); hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0); hoje.set(Calendar.MILLISECOND, 0);

        Calendar selecionada = Calendar.getInstance();
        selecionada.setTime(data);
        selecionada.set(Calendar.HOUR_OF_DAY, 0); selecionada.set(Calendar.MINUTE, 0);
        selecionada.set(Calendar.SECOND, 0); selecionada.set(Calendar.MILLISECOND, 0);

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
        if (movOriginal != null) {
            intent.putExtra("TIPO_CATEGORIA", movOriginal.getTipoEnum().getId());
        }
        launcherCategoria.launch(intent);
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        editCategoria.setText(result.getData().getStringExtra("categoriaSelecionada"));
                        novoCategoriaId = result.getData().getStringExtra("categoriaId");
                    }
                });
    }

    public void salvarDespesa(View v)   { processarAcaoFab(); }
    public void salvarProventos(View v) { processarAcaoFab(); }

    private void processarAcaoFab() {
        if (!isModoEdicaoAtivo) {
            alternarModoEdicao(true);
            Toast.makeText(this, "Modo de edição habilitado", Toast.LENGTH_SHORT).show();
            editValor.requestFocus();
        } else {
            confirmarEdicao();
        }
    }

    private void confirmarEdicao() {
        if (acaoEmAndamento) return;
        if (valorCentavosAtual <= 0) {
            editValor.setError("Preencha um valor válido");
            return;
        }
        acaoEmAndamento = true;

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

            Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(dataHoraStr);
            if (date != null) {
                movNova.setData_movimentacao(new Timestamp(date));
                boolean ok = switchStatusPago != null
                        ? switchStatusPago.isChecked()
                        : movOriginal.isPago();
                movNova.setPago(ok);
            }

            if (movOriginal.getTotal_parcelas() > 1
                    && movOriginal.getParcela_atual() < movOriginal.getTotal_parcelas()) {

                new AlertDialog.Builder(this)
                        .setTitle("Atualizar Série")
                        .setMessage("Deseja aplicar as alterações apenas a esta parcela ou a todas as seguintes também?")
                        .setPositiveButton("Esta e Seguintes", (d, w) -> enviarEdicaoAoBanco(movNova, true))
                        .setNegativeButton("Apenas Esta",      (d, w) -> enviarEdicaoAoBanco(movNova, false))
                        .setNeutralButton("Cancelar",          (d, w) -> acaoEmAndamento = false)
                        .show();
            } else {
                enviarEdicaoAoBanco(movNova, false);
            }

        } catch (Exception e) {
            acaoEmAndamento = false;
            Log.e(TAG, "Erro ao salvar", e);
            Toast.makeText(this, "Erro: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void enviarEdicaoAoBanco(MovimentacaoModel movNova, boolean todasSeguintes) {
        repository.editarMultiplos(movOriginal, movNova, todasSeguintes,
                new MovimentacaoRepository.Callback() {
                    @Override public void onSucesso(String msg) {
                        Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                    @Override public void onErro(String erro) {
                        acaoEmAndamento = false;
                        Toast.makeText(EditarMovimentacaoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmarExclusao() {
        if (movOriginal.getTotal_parcelas() > 1
                && movOriginal.getParcela_atual() < movOriginal.getTotal_parcelas()) {

            new AlertDialog.Builder(this)
                    .setTitle("Excluir Lançamento Repetido")
                    .setMessage("Como deseja tratar a exclusão desta conta?")
                    .setPositiveButton("Esta e Seguintes", (d, w) -> enviarExclusaoAoBanco(true))
                    .setNegativeButton("Apenas Esta", (d, w) -> {
                        Toast.makeText(this,
                                "Apenas este mês será removido. As próximas estão mantidas.",
                                Toast.LENGTH_LONG).show();
                        enviarExclusaoAoBanco(false);
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Excluir Lançamento")
                    .setMessage("Deseja realmente excluir? O saldo será corrigido automaticamente.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Excluir", (d, w) -> enviarExclusaoAoBanco(false))
                    .show();
        }
    }

    private void enviarExclusaoAoBanco(boolean todasSeguintes) {
        repository.excluirMultiplos(movOriginal, todasSeguintes,
                new MovimentacaoRepository.Callback() {
                    @Override public void onSucesso(String msg) {
                        Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                    @Override public void onErro(String erro) {
                        Toast.makeText(EditarMovimentacaoActivity.this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void alternarModoEdicao(boolean habilitar) {
        isModoEdicaoAtivo = habilitar;

        editValor.setEnabled(habilitar);
        editDescricao.setEnabled(habilitar);

        editCategoria.setEnabled(habilitar);
        editCategoria.setClickable(habilitar);
        editData.setEnabled(habilitar);
        editData.setClickable(habilitar);
        if (editHora != null) {
            editHora.setEnabled(habilitar);
            editHora.setClickable(habilitar);
        }

        if (btnExcluir != null) {
            btnExcluir.setVisibility(habilitar ? View.VISIBLE : View.GONE);
        }

        if (switchStatusPago != null) {
            if (habilitar) {
                try {
                    Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .parse(editData.getText().toString());
                    aplicarRegraStatusPorData(dataAtual);
                } catch (Exception e) {
                    switchStatusPago.setEnabled(true);
                }
            } else {
                switchStatusPago.setEnabled(false);
            }
        }

        if (fabSuperior != null) {
            fabSuperior.setImageResource(habilitar
                    ? R.drawable.ic_confirmar_branco_48
                    : R.drawable.ic_lapis_editar_24);
        }
        if (fabInferior != null) {
            fabInferior.setImageResource(habilitar
                    ? R.drawable.ic_confirmar_branco_24
                    : R.drawable.ic_lapis_editar_24);
        }
    }

    public void retornarPrincipal(View view) {
        if (acaoEmAndamento) return;
        acaoEmAndamento = true;
        finish();
    }
}