package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * DespesasActivity
 * Agora utiliza o MovimentacaoModel unificado com suporte a Mapas (Dot Notation).
 * Define automaticamente o status 'pago' com base na data escolhida ou intenção inicial.
 */
public class DespesasActivity extends AppCompatActivity {

    private final String TAG = "DespesasActivity";

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;
    private boolean isEdicao = false;
    private boolean ehContaFutura = false;
    private MovimentacaoModel itemEmEdicao = null;

    private String categoriaIdSelecionada;
    private int valorCentavosAtual = 0;
    private MaterialSwitch switchStatusPago;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.tela_add_despesa);

        // Captura a intenção vinda do menu (Agendar vs Nova)
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

        campoCategoria.setFocusable(false);
        campoCategoria.setClickable(true);
        campoCategoria.setOnClickListener(v -> abrirSelecaoCategoria());

        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, campoData));
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, campoHora));
    }

    private void configurarListeners() {
        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }
        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((buttonView, isChecked) -> atualizarTextoStatus());
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
                            valorCentavosAtual = Integer.parseInt(cleanString);
                            double valorDouble = MoedaHelper.centavosParaDouble(valorCentavosAtual);
                            String formatted = MoedaHelper.formatarParaBRL(valorDouble);
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
        // 1. Lógica de Recuperação de Dados (Edição)
        if (getIntent().hasExtra("movimentacaoSelecionada")) {
            MovimentacaoModel movRecebida;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                movRecebida = getIntent().getParcelableExtra("movimentacaoSelecionada", MovimentacaoModel.class);
            } else {
                movRecebida = getIntent().getParcelableExtra("movimentacaoSelecionada");
            }

            if (movRecebida != null) {
                isEdicao = true;
                itemEmEdicao = movRecebida;
                categoriaIdSelecionada = itemEmEdicao.getCategoria_id();
                valorCentavosAtual = itemEmEdicao.getValor();

                campoValor.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(valorCentavosAtual)));
                campoCategoria.setText(itemEmEdicao.getCategoria_nome());
                campoDescricao.setText(itemEmEdicao.getDescricao());

                if (itemEmEdicao.getData_movimentacao() != null) {
                    Date data = itemEmEdicao.getData_movimentacao().toDate();
                    campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data));
                    campoHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(data));
                }
                if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);
            }
        } else {
            isEdicao = false;
            if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);
        }

        // 2. Configuração de "Smart Defaults" (Padrões Inteligentes) baseados na intenção inicial
        if (!isEdicao) {
            if (ehContaFutura) {
                // MODO AGENDAR: Sugere amanhã e Status Pendente
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, 1);
                campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime()));
                campoHora.setText("08:00");

                switchStatusPago.setChecked(false);
                switchStatusPago.setText("Status: Pendente");
            } else {
                // MODO NOVA MOVIMENTAÇÃO: Hoje e Status OK
                campoData.setText(DatePickerHelper.setDataAtual());
                campoHora.setText(TimePickerHelper.setHoraAtual());

                switchStatusPago.setChecked(true);
                switchStatusPago.setText("Status: OK");
            }
        } else {
            // MODO EDIÇÃO: Respeita o que já estava salvo no banco
            boolean pago = itemEmEdicao != null && itemEmEdicao.isPago();
            switchStatusPago.setChecked(pago);
            switchStatusPago.setText(pago ? "Status: OK" : "Status: Pendente");
        }

        // SEMPRE deixa o switch habilitado para o usuário ter a decisão final independente de como entrou
        switchStatusPago.setEnabled(true);
    }

    /**
     * SALVAR DESPESA
     * O status 'pago' é definido pela escolha final do usuário no switch.
     */
    public void salvarDespesa(View view) {
        if (!validarCamposDespesas()) return;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        // 1. Dados Financeiros (Sempre Centavos) [cite: 2026-02-07]
        mov.setValor(valorCentavosAtual);
        mov.setDescricao(campoDescricao.getText().toString());

        // Usa o Enum para salvar "DESPESA" como String no Firestore
        mov.setTipoEnum(TipoCategoriaContas.DESPESA);

        // 2. Categoria
        mov.setCategoria_nome(campoCategoria.getText().toString());
        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            mov.setCategoria_id("geral_despesa");
        }

        // 3. Data e Hora
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

        // ✅ STATUS FINAL: A vontade do usuário no switch prevalece
        mov.setPago(switchStatusPago != null && switchStatusPago.isChecked());

        // 4. Persistência
        if (isEdicao) {
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { finalizarSucesso(msg); }
                @Override public void onErro(String erro) { mostrarErro(erro); }
            });
        } else {
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

    private void aplicarStatusUI(boolean isEdicaoLocal, Boolean pagoDoItem, boolean dataFutura) {
        // Regra fixa: conta futura = PENDENTE (não pago) e não editável
        if (dataFutura) {
            switchStatusPago.setChecked(false);           // Pendente
            switchStatusPago.setEnabled(false);           // trava
            switchStatusPago.setText("Status: Pendente");
            return;
        }

        // Se for NOVO: sempre OK e travado
        if (!isEdicaoLocal) {
            switchStatusPago.setChecked(true);            // OK
            switchStatusPago.setEnabled(false);           // trava
            switchStatusPago.setText("Status: OK");
            return;
        }

        // Se for EDIÇÃO e não é futuro: usuário pode alterar
        boolean ok = (pagoDoItem != null) ? pagoDoItem : true;
        switchStatusPago.setChecked(ok);
        switchStatusPago.setEnabled(true);
        switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
    }

    private void atualizarTextoStatus() {
        boolean ok = switchStatusPago.isChecked();
        switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
    }

    private void aplicarStatusInicial() {
        if (switchStatusPago == null) return;

        if (!isEdicao) {
            boolean ok = !ehContaFutura;
            switchStatusPago.setChecked(ok);
            switchStatusPago.setEnabled(true);
            switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
            return;
        }

        // edição
        boolean ok = (itemEmEdicao != null) && itemEmEdicao.isPago();
        switchStatusPago.setChecked(ok);
        switchStatusPago.setEnabled(true);
        switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
    }
}