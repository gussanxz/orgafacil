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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.RecorrenciaFormHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DespesasActivity extends AppCompatActivity {

    private final String TAG = "DespesasActivity";

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    // --- Componentes de Recorrência ---
    private LinearLayout layoutRecorrencia;
    private MaterialCheckBox checkboxRepetir;

    // [3.2] Helper que gerencia todos os tipos de recorrência
    private RecorrenciaFormHelper recorrenciaHelper;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;
    private boolean isEdicao = false;
    private boolean ehContaFutura = false;
    private MovimentacaoModel itemEmEdicao = null;

    private String categoriaIdSelecionada;
    private long valorCentavosAtual = 0;
    private MaterialSwitch switchStatusPago;

    private boolean salvandoEmProgresso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.tela_add_despesa);

        ehContaFutura = getIntent().getBooleanExtra("EH_CONTA_FUTURA", false);

        if (!FirebaseSession.isUserLogged()) {
            finish();
            return;
        }

        repository = new MovimentacaoRepository();

        setupWindowInsets();
        inicializarComponentes();
        configurarListeners();
        configurarLauncherCategoria();
        setupCurrencyMask();
        verificarModoEdicao();

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
        campoValor    = findViewById(R.id.editValor);
        campoData     = findViewById(R.id.editData);
        campoCategoria= findViewById(R.id.editCategoria);
        campoDescricao= findViewById(R.id.editDescricao);
        campoHora     = findViewById(R.id.editHora);
        btnExcluir    = findViewById(R.id.btnExcluir);
        switchStatusPago = findViewById(R.id.switchStatusPago);

        layoutRecorrencia = findViewById(R.id.layoutRecorrencia);
        checkboxRepetir   = findViewById(R.id.checkboxRepetir);

        campoCategoria.setFocusable(false);
        campoCategoria.setClickable(true);
        campoData.setFocusable(false);
        campoData.setClickable(true);
        if (campoHora != null) {
            campoHora.setFocusable(false);
            campoHora.setClickable(true);
        }

        // [3.2] Inicializa o helper passando a view raiz do layout
        recorrenciaHelper = new RecorrenciaFormHelper(
                findViewById(R.id.layoutRecorrencia), null);
    }

    private void configurarListeners() {
        campoCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        campoData.setOnClickListener(v -> abrirSelecionadorDeData());
        campoHora.setOnClickListener(v -> abrirSelecionadorDeHora());

        // O checkbox já é tratado internamente pelo RecorrenciaFormHelper.
        // Mantemos apenas a referência ao checkboxRepetir para ler .isChecked() em salvarDespesa().

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }
        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((btn, checked) -> atualizarTextoStatus());
        }
    }

    private void abrirSelecionadorDeData() {
        Calendar c = Calendar.getInstance();
        try {
            Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .parse(campoData.getText().toString());
            if (dataAtual != null) c.setTime(dataAtual);
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);
            validarLimiteHoraAtual();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        if (!ehContaFutura) {
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        }
        dialog.show();
    }

    private void abrirSelecionadorDeHora() {
        Calendar agora = Calendar.getInstance();
        int horaSet = agora.get(Calendar.HOUR_OF_DAY);
        int minutoSet = agora.get(Calendar.MINUTE);

        try {
            String horaAtual = campoHora.getText().toString().trim();
            if (!horaAtual.isEmpty() && horaAtual.contains(":")) {
                String[] partes = horaAtual.split(":");
                horaSet   = Integer.parseInt(partes[0]);
                minutoSet = Integer.parseInt(partes[1]);
            }
        } catch (Exception ignored) {}

        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            validarLimiteHoraAtual();
        }, horaSet, minutoSet, true).show();
    }

    private void validarLimiteHoraAtual() {
        if (ehContaFutura) return;
        try {
            Date dataSelecionada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .parse(campoData.getText().toString());
            if (dataSelecionada == null) return;

            Calendar calSelecionada = Calendar.getInstance();
            calSelecionada.setTime(dataSelecionada);
            Calendar agora = Calendar.getInstance();

            if (calSelecionada.get(Calendar.YEAR)        == agora.get(Calendar.YEAR) &&
                    calSelecionada.get(Calendar.DAY_OF_YEAR) == agora.get(Calendar.DAY_OF_YEAR)) {

                String horaTexto = campoHora.getText().toString().trim();
                if (horaTexto.isEmpty() || !horaTexto.contains(":")) return;

                String[] partes = horaTexto.split(":");
                int horaCampo = Integer.parseInt(partes[0]);
                int minCampo  = Integer.parseInt(partes[1]);
                int horaAgora = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora  = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    Toast.makeText(this, "Horário ajustado: não é possível usar horas no futuro.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
    }

    private void aplicarRegraStatusPorData(Date data) {
        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0); hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0);      hoje.set(Calendar.MILLISECOND, 0);

        Calendar selecionada = Calendar.getInstance();
        selecionada.setTime(data);
        selecionada.set(Calendar.HOUR_OF_DAY, 0); selecionada.set(Calendar.MINUTE, 0);
        selecionada.set(Calendar.SECOND, 0);       selecionada.set(Calendar.MILLISECOND, 0);

        if (selecionada.after(hoje)) {
            switchStatusPago.setChecked(false);
            switchStatusPago.setEnabled(false);
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setEnabled(true);
            atualizarTextoStatus();
        }
    }

    private void abrirSelecaoCategoria() {
        Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
        intent.putExtra("TIPO_CATEGORIA", TipoCategoriaContas.DESPESA.getId());
        launcherCategoria.launch(intent);
    }

    private void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        categoriaIdSelecionada = result.getData().getStringExtra("categoriaId");
                        campoCategoria.setText(result.getData().getStringExtra("categoriaSelecionada"));
                    }
                });
    }

    private void setupCurrencyMask() {
        campoValor.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                campoValor.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("[^\\d]", "");
                if (!clean.isEmpty()) {
                    try {
                        valorCentavosAtual = Long.parseLong(clean);
                        String fmt = MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0);
                        current = fmt;
                        campoValor.setText(fmt);
                        campoValor.setSelection(fmt.length());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Erro ao processar máscara");
                    }
                } else {
                    valorCentavosAtual = 0;
                    campoValor.setText("");
                }
                campoValor.addTextChangedListener(this);
            }
        });
    }

    private void verificarModoEdicao() {
        if (getIntent().hasExtra("movimentacaoSelecionada")) {
            // MODO EDIÇÃO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada", MovimentacaoModel.class);
            } else {
                itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada");
            }

            if (itemEmEdicao != null) {
                isEdicao = true;
                categoriaIdSelecionada = itemEmEdicao.getCategoria_id();
                valorCentavosAtual = itemEmEdicao.getValor();

                campoValor.setText(MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0));
                campoCategoria.setText(itemEmEdicao.getCategoria_nome());
                campoDescricao.setText(itemEmEdicao.getDescricao());

                layoutRecorrencia.setVisibility(View.GONE);

                if (itemEmEdicao.getData_movimentacao() != null) {
                    Date data = itemEmEdicao.getData_movimentacao().toDate();
                    campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data));
                    campoHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(data));
                    switchStatusPago.setChecked(itemEmEdicao.isPago());
                    aplicarRegraStatusPorData(data);
                }

                if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);
            }

        } else {
            // MODO CRIAÇÃO
            isEdicao = false;
            layoutRecorrencia.setVisibility(View.VISIBLE);
            if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);

            if (ehContaFutura) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, 1);
                Date dataFutura = c.getTime();
                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataFutura));
                campoHora.setText("");
                campoHora.setHint("HH:mm");
                aplicarRegraStatusPorData(dataFutura);
            } else {
                Date dataHoje = new Date();
                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataHoje));
                campoHora.setText(TimePickerHelper.setHoraAtual());
                switchStatusPago.setChecked(true);
                aplicarRegraStatusPorData(dataHoje);
            }
        }
    }

    public void salvarDespesa(View view) {
        if (salvandoEmProgresso) return;
        if (!validarCamposDespesas()) return;
        salvandoEmProgresso = true;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        mov.setDescricao(campoDescricao.getText().toString());
        mov.setTipoEnum(TipoCategoriaContas.DESPESA);
        mov.setCategoria_nome(campoCategoria.getText().toString());

        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            mov.setCategoria_id("geral_despesa");
        }

        try {
            String dataStr = campoData.getText().toString().trim();
            String horaStr = campoHora.getText().toString().trim();
            if (horaStr.isEmpty()) horaStr = "00:00";
            Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .parse(dataStr + " " + horaStr);
            if (date != null) mov.setData_movimentacao(new Timestamp(date));
        } catch (ParseException e) {
            mov.setData_movimentacao(Timestamp.now());
        }

        mov.setPago(switchStatusPago.isChecked());

        if (isEdicao) {
            mov.setValor(valorCentavosAtual);
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro)   { mostrarErro(erro); }
            });
            return;
        }

        // ── CRIAÇÃO ──────────────────────────────────────────────────────────
        if (recorrenciaHelper.isRepetirAtivo()) {

            // [3.2] Validação do helper (quantidade, intervalo, etc.)
            String erroRec = recorrenciaHelper.validar();
            if (erroRec != null) {
                salvandoEmProgresso = false;
                Toast.makeText(this, erroRec, Toast.LENGTH_SHORT).show();
                return;
            }

            int quantidade = recorrenciaHelper.getQuantidade();

            // Data de início: usa dataReferencia se informada, senão a do formulário
            if (recorrenciaHelper.getDataReferencia() != null) {
                mov.setData_movimentacao(new Timestamp(recorrenciaHelper.getDataReferencia()));
            }

            // Tipo e intervalo para o repository calcular as datas
            mov.setTipoRecorrenciaEnum(recorrenciaHelper.getTipo());
            mov.setRecorrencia_intervalo(recorrenciaHelper.getIntervalo());

            // PARCELADO divide o valor; os demais repetem o valor total
            if (recorrenciaHelper.getTipo() ==
                    com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia.PARCELADO) {
                mov.setValor(valorCentavosAtual / quantidade);
            } else {
                mov.setValor(valorCentavosAtual);
            }

            repository.salvarRecorrente(mov, quantidade, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro)   { mostrarErro(erro); }
            });

        } else {
            // Lançamento único
            mov.setValor(valorCentavosAtual);
            repository.salvar(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro)   { mostrarErro(erro); }
            });
        }
    }

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Despesa")
                .setMessage("Tem certeza? O saldo será corrigido automaticamente.")
                .setPositiveButton("Sim", (d, w) -> excluirDespesa())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirDespesa() {
        if (itemEmEdicao == null) return;
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
            @Override public void onErro(String erro)   { mostrarErro(erro); }
        });
    }

    private void finalizarSucesso(String msg) {
        salvandoEmProgresso = false;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void mostrarErro(String erro) {
        salvandoEmProgresso = false;
        Toast.makeText(this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
    }

    public Boolean validarCamposDespesas() {
        if (valorCentavosAtual <= 0) {
            campoValor.setError("Preencha um valor válido");
            return false;
        }
        if (campoCategoria.getText().toString().isEmpty()) {
            Toast.makeText(this, "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (campoData.getText().toString().isEmpty()) return false;
        // Validação de quantidade do painel é feita dentro de salvarDespesa() via helper
        return true;
    }

    public void retornarPrincipal(View view) {
        if (salvandoEmProgresso) return;
        salvandoEmProgresso = true;
        finish();
    }

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

    private void atualizarTextoStatus() {
        boolean ok = switchStatusPago.isChecked();
        if (!switchStatusPago.isEnabled() && !ok) {
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
        }
    }
}