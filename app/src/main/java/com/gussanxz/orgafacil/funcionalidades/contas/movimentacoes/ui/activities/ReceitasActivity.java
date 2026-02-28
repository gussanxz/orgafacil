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

public class ReceitasActivity extends AppCompatActivity {

    private final String TAG = "ReceitasActivity";

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
    private MaterialSwitch switchStatusPago;
    private long valorCentavosAtual = 0;

    //flag pra nao permitir multiplos click no botao
    private boolean salvandoEmProgresso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.tela_add_receita);

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
        campoData.setOnClickListener(v -> abrirSelecionadorDeData());
        campoHora.setOnClickListener(v -> abrirSelecionadorDeHora());

        checkboxRepetir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textInputMeses.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            radioGroupTipoRecorrencia.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            intent.putExtra("TIPO_CATEGORIA", TipoCategoriaContas.RECEITA.getId());
            launcherCategoria.launch(intent);
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

        DatePickerDialog dialog = new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            Date dataEscolhida = c.getTime();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataEscolhida));
            aplicarRegraStatusPorData(dataEscolhida);

            // Se a data mudou para "Hoje", precisamos conferir se a hora que já estava lá
            // não acabou ficando no futuro.
            validarLimiteHoraAtual();

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // Trava a seleção de datas futuras caso NÃO seja a tela de Contas Futuras
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
                horaSet = Integer.parseInt(partes[0]);
                minutoSet = Integer.parseInt(partes[1]);
            }
            // se vazio, mantém horaSet/minutoSet = hora atual do sistema (já definido acima)
        } catch (Exception ignored) {}

        android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            validarLimiteHoraAtual(); // Valida assim que o usuário aperta OK
        }, horaSet, minutoSet, true);

        timePicker.show();
    }

    private void validarLimiteHoraAtual() {
        if (ehContaFutura) return; // Contas pendentes/futuras podem ter qualquer hora

        try {
            Date dataSelecionada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(campoData.getText().toString());
            if (dataSelecionada == null) return;

            Calendar calSelecionada = Calendar.getInstance();
            calSelecionada.setTime(dataSelecionada);

            Calendar agora = Calendar.getInstance();

            if (calSelecionada.get(Calendar.YEAR) == agora.get(Calendar.YEAR) &&
                    calSelecionada.get(Calendar.DAY_OF_YEAR) == agora.get(Calendar.DAY_OF_YEAR)) {

                // ✅ CORREÇÃO: guarda antes do split — campo pode estar vazio em agendamentos
                String horaTexto = campoHora.getText().toString().trim();
                if (horaTexto.isEmpty() || !horaTexto.contains(":")) return;

                String[] partesHora = horaTexto.split(":");
                int horaCampo = Integer.parseInt(partesHora[0]);
                int minCampo  = Integer.parseInt(partesHora[1]);

                int horaAgora = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora  = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    Toast.makeText(this, "Horário reajustado: não é possível usar horas no futuro.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
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
                            campoValor.setText(MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0));
                            current = campoValor.getText().toString();
                            campoValor.setSelection(current.length());
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Erro ao formatar valor");
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
            // --- MODO EDIÇÃO ---
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
            // --- MODO CRIAÇÃO ---
            isEdicao = false;
            layoutRecorrencia.setVisibility(View.VISIBLE);
            if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);

            if (ehContaFutura) {
                // Agendamento: pré-preenche com amanhã como sugestão,
                // mas deixa hora em BRANCO para o usuário escolher livremente.
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, 1);
                Date dataFutura = c.getTime();

                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataFutura));
                campoHora.setText(""); // livre — sem horário fixo
                campoHora.setHint("HH:mm"); // indica o formato esperado

                aplicarRegraStatusPorData(dataFutura); // define como Pendente

            } else {
                // Lançamento normal: preenche com hoje + hora atual
                Date dataHoje = new Date();
                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataHoje));
                campoHora.setText(TimePickerHelper.setHoraAtual());

                switchStatusPago.setChecked(true);
                aplicarRegraStatusPorData(dataHoje);
            }
        }
    }

    public void salvarProventos(View view) {

        if (salvandoEmProgresso) return;
        if (!validarCamposProventos()) return;
        salvandoEmProgresso = true;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        mov.setDescricao(campoDescricao.getText().toString());
        mov.setTipoEnum(TipoCategoriaContas.RECEITA);

        mov.setCategoria_nome(campoCategoria.getText().toString());
        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            mov.setCategoria_id("geral_receita");
        }

        try {
            String dataStr = campoData.getText().toString().trim();
            String horaStr = campoHora.getText().toString().trim();

            // Se o campo hora estiver vazio (agendamento sem hora definida),
            // usa meia-noite como padrão — deixa claro que é o início do dia
            if (horaStr.isEmpty()) {
                horaStr = "00:00";
            }

            String dataHoraStr = dataStr + " " + horaStr;
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
                    if (radioParcelado.isChecked()) {
                        long valorParcela = valorCentavosAtual / qtdMeses;
                        mov.setValor(valorParcela);
                    } else {
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
                .setTitle("Excluir Receita")
                .setMessage("Deseja apagar este lançamento? O saldo será corrigido.")
                .setPositiveButton("Sim", (dialog, which) -> excluirReceita())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirReceita() {
        if (itemEmEdicao == null) return;
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
            @Override public void onErro(String erro) { mostrarErro(erro); }
        });
    }

    private void finalizarSucesso(String msg) {
        salvandoEmProgresso = false;
        Toast.makeText(ReceitasActivity.this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void mostrarErro(String erro) {
        salvandoEmProgresso = false;
        Toast.makeText(ReceitasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
    }

    public Boolean validarCamposProventos() {
        if (valorCentavosAtual <= 0) {
            campoValor.setError("Valor inválido");
            return false;
        }
        if (campoData.getText().toString().isEmpty()) return false;
        if (campoCategoria.getText().toString().isEmpty()) {
            Toast.makeText(this, "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkboxRepetir.isChecked() && editQtdMeses.getText().toString().isEmpty()) {
            editQtdMeses.setError("Informe a quantidade");
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

    public void retornarPrincipal(View view) {
        if (salvandoEmProgresso) return;
        salvandoEmProgresso = true;
        finish();
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