package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.util_helper.DatePickerHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.TimePickerHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ReceitasActivity
 * Agora utiliza o MovimentacaoModel unificado.
 * Identifica automaticamente se é uma Receita Atual ou um Agendamento Futuro.
 */
public class ReceitasActivity extends AppCompatActivity {

    private final String TAG = "ReceitasActivity";

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private ImageButton btnExcluir;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private MovimentacaoRepository repository;
    private boolean isEdicao = false;
    private boolean ehContaFutura = false;
    private MovimentacaoModel itemEmEdicao = null;

    private String categoriaIdSelecionada;
    private MaterialSwitch switchStatusPago;

    // [PRECISÃO]: O valor é controlado em centavos (int) para evitar erros matemáticos [cite: 2026-02-07]
    private int valorCentavosAtual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_contas_add_receita);
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
    }

    private void configurarListeners() {
        campoData.setOnClickListener(v -> DatePickerHelper.showDatePickerDialog(this, campoData));
        campoHora.setOnClickListener(v -> TimePickerHelper.showTimePickerDialog(this, campoHora));

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
            // Passa o tipo para filtrar apenas categorias de RECEITA
            intent.putExtra("TIPO_CATEGORIA", TipoCategoriaContas.RECEITA.getId());
            launcherCategoria.launch(intent);
        });

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }

        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((buttonView, isChecked) -> {
                switchStatusPago.setText(isChecked ? "Status: OK" : "Status: Pendente");
            });
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
                            valorCentavosAtual = Integer.parseInt(cleanString);
                            double valorDouble = MoedaHelper.centavosParaDouble(valorCentavosAtual);
                            String formatted = MoedaHelper.formatarParaBRL(valorDouble);
                            current = formatted;
                            campoValor.setText(formatted);
                            campoValor.setSelection(formatted.length());
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

                double valorReais = MoedaHelper.centavosParaDouble(valorCentavosAtual);
                campoValor.setText(MoedaHelper.formatarParaBRL(valorReais));
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
            campoData.setText(DatePickerHelper.setDataAtual());
            campoHora.setText(TimePickerHelper.setHoraAtual());
            if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);
        }
        // 1) Se veio do botão "Conta Futura" do Resumo: SEMPRE pendente e travado
        if (ehContaFutura) {
            switchStatusPago.setChecked(false);
            switchStatusPago.setEnabled(false);
            switchStatusPago.setText("Status: Pendente");
            return;
        }

// 2) Se NÃO é edição (novo lançamento normal): sempre OK e travado
        if (!isEdicao) {
            switchStatusPago.setChecked(true);
            switchStatusPago.setEnabled(false);
            switchStatusPago.setText("Status: OK");
            return;
        }

// 3) Se é edição: usuário pode alternar conforme valor salvo
        switchStatusPago.setEnabled(true);
        switchStatusPago.setChecked(itemEmEdicao != null && itemEmEdicao.isPago());
        switchStatusPago.setText(switchStatusPago.isChecked() ? "Status: OK" : "Status: Pendente");
    }

    /**
     * SALVAR PROVENTOS
     * [ATUALIZADO]: Define status 'pago' automaticamente e usa Enum para legibilidade no Firestore.
     */
    public void salvarProventos(View view) {
        if (!validarCamposProventos()) return;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        // 1. Valores (Sempre em Centavos)
        mov.setValor(valorCentavosAtual);
        mov.setDescricao(campoDescricao.getText().toString());

        // [ATUALIZADO]: Salva como String "RECEITA" no banco
        mov.setTipoEnum(TipoCategoriaContas.RECEITA);

        // 2. Categoria
        mov.setCategoria_nome(campoCategoria.getText().toString());
        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            mov.setCategoria_id("geral_receita");
        }

        // 3. Data e Status Inteligente
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

        // ✅ STATUS (regra do sinal EH_CONTA_FUTURA + edição)
        if (ehContaFutura) {
            mov.setPago(false); // Pendente sempre
        } else if (!isEdicao) {
            mov.setPago(true);  // Nova movimentação normal = OK
        } else {
            mov.setPago(switchStatusPago != null && switchStatusPago.isChecked()); // Edição decide
        }

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
        Toast.makeText(ReceitasActivity.this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void mostrarErro(String erro) {
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
        return true;
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void retornarPrincipal(View view) { finish(); }

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
}