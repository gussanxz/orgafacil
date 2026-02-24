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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
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
    private TextInputLayout textInputMeses;
    private TextInputEditText editQtdMeses;

    // [NOVO] RadioGroup de Recorrência
    private RadioGroup radioGroupTipoRecorrencia;
    private RadioButton radioParcelado;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;
    private boolean isEdicao = false;
    private boolean ehContaFutura = false;
    private MovimentacaoModel itemEmEdicao = null;

    private String categoriaIdSelecionada;
    private long valorCentavosAtual = 0;
    private MaterialSwitch switchStatusPago;

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
        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);
        btnExcluir = findViewById(R.id.btnExcluir);
        switchStatusPago = findViewById(R.id.switchStatusPago);

        layoutRecorrencia = findViewById(R.id.layoutRecorrencia);
        checkboxRepetir = findViewById(R.id.checkboxRepetir);
        textInputMeses = findViewById(R.id.textInputMeses);
        editQtdMeses = findViewById(R.id.editQtdMeses);

        // [NOVO] Inicialização dos componentes
        radioGroupTipoRecorrencia = findViewById(R.id.radioGroupTipoRecorrencia);
        radioParcelado = findViewById(R.id.radioParcelado);

        campoCategoria.setFocusable(false);
        campoCategoria.setClickable(true);

        campoData.setFocusable(false);
        campoData.setClickable(true);
        if (campoHora != null) {
            campoHora.setFocusable(false);
            campoHora.setClickable(true);
        }
    }

    private void configurarListeners() {
        campoCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        campoData.setOnClickListener(v -> abrirSelecionadorDeData());
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, campoHora));

        checkboxRepetir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textInputMeses.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            // [NOVO] Mostra os botões de rádio junto com a caixinha
            radioGroupTipoRecorrencia.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }
        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((buttonView, isChecked) -> atualizarTextoStatus());
        }
    }

    private void abrirSelecionadorDeData() {
        Calendar c = Calendar.getInstance();
        try {
            Date dataAtual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(campoData.getText().toString());
            if(dataAtual != null) c.setTime(dataAtual);
        } catch (Exception ignored) {}

        new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void aplicarRegraStatusPorData(Date data) {
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
                        categoriaIdSelecionada = result.getData().getStringExtra("categoriaId");
                        campoCategoria.setText(nomeCat);
                    }
                });
    }

    private void setupCurrencyMask() {
        campoValor.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    campoValor.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            valorCentavosAtual = Long.parseLong(cleanString);
                            String formatted = MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0);
                            current = formatted;
                            campoValor.setText(formatted);
                            campoValor.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Erro ao processar máscara");
                        }
                    } else {
                        valorCentavosAtual = 0;
                        campoValor.setText("");
                    }
                    campoValor.addTextChangedListener(this);
                }
            }
        });
    }

    private void verificarModoEdicao() {
        if (getIntent().hasExtra("movimentacaoSelecionada")) {
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
            isEdicao = false;
            layoutRecorrencia.setVisibility(View.VISIBLE);
            if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);

            if (ehContaFutura) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, 1);
                Date dataFutura = c.getTime();

                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataFutura));
                campoHora.setText("08:00");
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
        if (!validarCamposDespesas()) return;

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
            String dataHoraStr = campoData.getText().toString() + " " + campoHora.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(dataHoraStr);
            if (date != null) {
                mov.setData_movimentacao(new Timestamp(date));
            }
        } catch (ParseException e) {
            mov.setData_movimentacao(Timestamp.now());
        }

        mov.setPago(switchStatusPago.isChecked());

        if (isEdicao) {
            mov.setValor(valorCentavosAtual);
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro) { mostrarErro(erro); }
            });
        } else {
            if (checkboxRepetir.isChecked()) {
                String strMeses = editQtdMeses.getText().toString();
                int qtdMeses = strMeses.isEmpty() ? 0 : Integer.parseInt(strMeses);

                if (qtdMeses > 1) {
                    // [NOVO] Lógica de Parcelar vs Repetir
                    if (radioParcelado.isChecked()) {
                        // Divide o valor pelas parcelas
                        long valorParcela = valorCentavosAtual / qtdMeses;
                        mov.setValor(valorParcela);
                    } else {
                        // Repete o valor total todos os meses
                        mov.setValor(valorCentavosAtual);
                    }

                    repository.salvarRecorrente(mov, qtdMeses, new MovimentacaoRepository.Callback() {
                        @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                        @Override public void onErro(String erro) { mostrarErro(erro); }
                    });
                    return;
                }
            }

            mov.setValor(valorCentavosAtual);
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
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
            @Override public void onErro(String erro) { mostrarErro(erro); }
        });
    }

    private void finalizarSucesso(String msg) {
        Toast.makeText(DespesasActivity.this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void mostrarErro(String erro) {
        Toast.makeText(DespesasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
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
        if (checkboxRepetir.isChecked() && editQtdMeses.getText().toString().isEmpty()) {
            editQtdMeses.setError("Informe a quantidade");
            return false;
        }
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

    private void atualizarTextoStatus() {
        boolean ok = switchStatusPago.isChecked();
        if (!switchStatusPago.isEnabled() && !ok) {
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
        }
    }
}