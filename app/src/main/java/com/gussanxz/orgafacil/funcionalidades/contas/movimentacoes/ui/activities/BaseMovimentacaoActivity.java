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
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui.SelecionarCategoriaContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia;
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

/**
 * BaseMovimentacaoActivity
 *
 * Classe abstrata que centraliza toda a lógica compartilhada entre
 * DespesasActivity e ReceitasActivity, eliminando ~80% de duplicação de código.
 *
 * Subclasses devem implementar:
 *  - getTipo()            → define se é RECEITA ou DESPESA
 *  - getLayoutResId()     → ID do layout XML a inflar
 *  - getCategoriaDefault()→ ID de categoria fallback (ex: "geral_despesa")
 *  - onExcluirConfirmado()→ lógica específica de confirmação de exclusão (título/mensagem)
 *
 * Subclasses podem sobrescrever:
 *  - onSalvarSucesso()    → comportamento após salvar (padrão: finish())
 *  - validarCamposExtra() → validações adicionais além das padrão
 */
public abstract class BaseMovimentacaoActivity extends AppCompatActivity {

    // ── Constantes ─────────────────────────────────────────────────────────────
    private static final long VALOR_MAXIMO_CENTAVOS = 999_999_999_99L; // R$ 9.999.999,99

    // ── Campos de UI (acessíveis às subclasses) ────────────────────────────────
    protected TextInputEditText campoData;
    protected TextInputEditText campoDescricao;
    protected TextInputEditText campoHora;
    protected EditText campoValor;
    protected EditText campoCategoria;
    protected ImageButton btnExcluir;
    protected MaterialSwitch switchStatusPago;
    protected LinearLayout layoutRecorrencia;
    protected MaterialCheckBox checkboxRepetir;
    protected RecorrenciaFormHelper recorrenciaHelper;

    // ── Estado ─────────────────────────────────────────────────────────────────
    protected MovimentacaoRepository repository;
    protected boolean isEdicao = false;
    protected boolean ehContaFutura = false;
    protected MovimentacaoModel itemEmEdicao = null;
    protected String categoriaIdSelecionada;
    protected long valorCentavosAtual = 0;
    protected boolean salvandoEmProgresso = false;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private String TAG;

    // ── Métodos abstratos que as subclasses DEVEM implementar ──────────────────

    /** Define o tipo da movimentação: RECEITA ou DESPESA */
    protected abstract TipoCategoriaContas getTipo();

    /** Retorna o ID do layout XML a ser inflado */
    protected abstract int getLayoutResId();

    /** Retorna o ID de categoria padrão caso nenhuma seja selecionada */
    protected abstract String getCategoriaDefault();

    /** Retorna o título do diálogo de exclusão */
    protected abstract String getTituloExclusao();

    /** Retorna a mensagem do diálogo de exclusão */
    protected abstract String getMensagemExclusao();

    // ── Métodos que subclasses PODEM sobrescrever ──────────────────────────────

    /**
     * Ponto de extensão para validações extras além das padrão.
     * Retorne null se válido, ou a mensagem de erro para exibir.
     */
    protected String validarCamposExtra() {
        return null; // sem validação extra por padrão
    }

    /** Chamado após salvar/editar com sucesso. Padrão: exibe toast e encerra. */
    protected void onSalvarSucesso(String msg) {
        salvandoEmProgresso = false;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    /** Chamado quando ocorre erro ao salvar. */
    protected void onSalvarErro(String erro) {
        salvandoEmProgresso = false;
        Toast.makeText(this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(getLayoutResId());

        TAG = getClass().getSimpleName();

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
        aplicarExtrasIntent();
    }

    // ── Inicialização ──────────────────────────────────────────────────────────

    private void inicializarComponentes() {
        campoValor      = findViewById(R.id.editValor);
        campoData       = findViewById(R.id.editData);
        campoCategoria  = findViewById(R.id.editCategoria);
        campoDescricao  = findViewById(R.id.editDescricao);
        campoHora       = findViewById(R.id.editHora);
        btnExcluir      = findViewById(R.id.btnExcluir);
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

        recorrenciaHelper = new RecorrenciaFormHelper(
                findViewById(R.id.layoutRecorrencia), null);
    }

    private void aplicarExtrasIntent() {
        TextView textViewHeader = findViewById(R.id.textViewHeader);
        Bundle extras = getIntent().getExtras();
        if (extras != null && textViewHeader != null) {
            String titulo = extras.getString("TITULO_TELA");
            boolean ehAtalho = extras.getBoolean("EH_ATALHO", false);
            if (titulo != null) textViewHeader.setText(titulo);
            if (ehAtalho) Log.i(TAG, "Regras de atalho aplicadas.");
        }
    }

    // ── Listeners ──────────────────────────────────────────────────────────────

    private void configurarListeners() {
        campoCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        campoData.setOnClickListener(v -> abrirSelecionadorDeData());
        if (campoHora != null) {
            campoHora.setOnClickListener(v -> abrirSelecionadorDeHora());
        }

        if (btnExcluir != null) {
            btnExcluir.setOnClickListener(v -> {
                if (isEdicao && itemEmEdicao != null) confirmarExcluir();
            });
        }

        if (switchStatusPago != null) {
            switchStatusPago.setOnCheckedChangeListener((btn, checked) -> atualizarTextoStatus());
        }
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

    // ── Seletores de Data e Hora ───────────────────────────────────────────────

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

                String horaTexto = campoHora != null ? campoHora.getText().toString().trim() : "";
                if (horaTexto.isEmpty() || !horaTexto.contains(":")) return;

                String[] partes = horaTexto.split(":");
                int horaCampo = Integer.parseInt(partes[0]);
                int minCampo  = Integer.parseInt(partes[1]);
                int horaAgora = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora  = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    if (campoHora != null) {
                        campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    }
                    Toast.makeText(this, "Horário ajustado: não é possível usar horas no futuro.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Regras de Status ───────────────────────────────────────────────────────

    protected void aplicarRegraStatusPorData(Date data) {
        if (switchStatusPago == null) return;

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

    protected void atualizarTextoStatus() {
        if (switchStatusPago == null) return;
        boolean ok = switchStatusPago.isChecked();
        if (!switchStatusPago.isEnabled() && !ok) {
            switchStatusPago.setText("Status: Pendente (Futuro)");
        } else {
            switchStatusPago.setText(ok ? "Status: OK" : "Status: Pendente");
        }
    }

    // ── Seleção de Categoria ───────────────────────────────────────────────────

    private void abrirSelecaoCategoria() {
        Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
        intent.putExtra("TIPO_CATEGORIA", getTipo().getId());
        launcherCategoria.launch(intent);
    }

    // ── Máscara de Moeda ───────────────────────────────────────────────────────

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
                        long parsed = Long.parseLong(clean);
                        // Limite de segurança: evita overflow e valores absurdos
                        valorCentavosAtual = Math.min(parsed, VALOR_MAXIMO_CENTAVOS);
                        String fmt = MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0);
                        current = fmt;
                        campoValor.setText(fmt);
                        campoValor.setSelection(fmt.length());

                        if (parsed > VALOR_MAXIMO_CENTAVOS) {
                            Toast.makeText(BaseMovimentacaoActivity.this,
                                    "Valor máximo permitido: R$ 9.999.999,99",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Erro ao processar máscara de moeda");
                    }
                } else {
                    valorCentavosAtual = 0;
                    campoValor.setText("");
                }
                campoValor.addTextChangedListener(this);
            }
        });
    }

    // ── Modo de Edição vs Criação ──────────────────────────────────────────────

    private void verificarModoEdicao() {
        if (getIntent().hasExtra("movimentacaoSelecionada")) {
            carregarModoEdicao();
        } else {
            configurarModoCriacao();
        }
    }

    private void carregarModoEdicao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada", MovimentacaoModel.class);
        } else {
            itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada");
        }

        if (itemEmEdicao == null) return;

        isEdicao = true;
        categoriaIdSelecionada = itemEmEdicao.getCategoria_id();
        valorCentavosAtual = itemEmEdicao.getValor();

        campoValor.setText(MoedaHelper.formatarParaBRL(valorCentavosAtual / 100.0));
        campoCategoria.setText(itemEmEdicao.getCategoria_nome());
        campoDescricao.setText(itemEmEdicao.getDescricao());

        if (layoutRecorrencia != null) layoutRecorrencia.setVisibility(View.GONE);

        if (itemEmEdicao.getData_movimentacao() != null) {
            Date data = itemEmEdicao.getData_movimentacao().toDate();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data));
            if (campoHora != null) {
                campoHora.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(data));
            }
            if (switchStatusPago != null) switchStatusPago.setChecked(itemEmEdicao.isPago());
            aplicarRegraStatusPorData(data);
        }

        if (btnExcluir != null) btnExcluir.setVisibility(View.VISIBLE);
    }

    private void configurarModoCriacao() {
        isEdicao = false;
        if (layoutRecorrencia != null) layoutRecorrencia.setVisibility(View.VISIBLE);
        if (btnExcluir != null) btnExcluir.setVisibility(View.GONE);

        if (ehContaFutura) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 1);
            Date dataFutura = c.getTime();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataFutura));
            if (campoHora != null) {
                campoHora.setText("");
                campoHora.setHint("HH:mm");
            }
            aplicarRegraStatusPorData(dataFutura);
        } else {
            Date dataHoje = new Date();
            campoData.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataHoje));
            if (campoHora != null) campoHora.setText(TimePickerHelper.setHoraAtual());
            if (switchStatusPago != null) switchStatusPago.setChecked(true);
            aplicarRegraStatusPorData(dataHoje);
        }
    }

    // ── Salvar (lógica unificada para Receita e Despesa) ───────────────────────

    /**
     * Método central de salvamento. Chame-o nos botões de ação das subclasses:
     *   public void salvarDespesa(View view) { salvarMovimentacao(); }
     *   public void salvarProventos(View view) { salvarMovimentacao(); }
     */
    protected final void salvarMovimentacao() {
        if (salvandoEmProgresso) return;
        if (!validarCamposPadrao()) return;

        // Validação extra definida pela subclasse
        String erroExtra = validarCamposExtra();
        if (erroExtra != null) {
            Toast.makeText(this, erroExtra, Toast.LENGTH_SHORT).show();
            return;
        }

        salvandoEmProgresso = true;

        MovimentacaoModel mov = isEdicao ? itemEmEdicao : new MovimentacaoModel();

        mov.setDescricao(campoDescricao.getText().toString().trim());
        mov.setTipoEnum(getTipo());
        mov.setCategoria_nome(campoCategoria.getText().toString());

        if (categoriaIdSelecionada != null) {
            mov.setCategoria_id(categoriaIdSelecionada);
        } else if (!isEdicao) {
            mov.setCategoria_id(getCategoriaDefault());
        }

        // Parseia data + hora
        try {
            String dataStr = campoData.getText().toString().trim();
            String horaStr = (campoHora != null) ? campoHora.getText().toString().trim() : "";
            if (horaStr.isEmpty()) horaStr = "00:00";
            Date date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .parse(dataStr + " " + horaStr);
            if (date != null) mov.setData_movimentacao(new Timestamp(date));
        } catch (ParseException e) {
            mov.setData_movimentacao(Timestamp.now());
        }

        mov.setPago(switchStatusPago != null && switchStatusPago.isChecked());

        if (isEdicao) {
            mov.setValor(valorCentavosAtual);
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
                @Override public void onErro(String erro)   { onSalvarErro(erro); }
            });
            return;
        }

        // ── CRIAÇÃO ──────────────────────────────────────────────────────────────
        if (recorrenciaHelper != null && recorrenciaHelper.isRepetirAtivo()) {

            String erroRec = recorrenciaHelper.validar();
            if (erroRec != null) {
                salvandoEmProgresso = false;
                Toast.makeText(this, erroRec, Toast.LENGTH_SHORT).show();
                return;
            }

            int quantidade = recorrenciaHelper.getQuantidade();

            if (recorrenciaHelper.getDataReferencia() != null) {
                mov.setData_movimentacao(new Timestamp(recorrenciaHelper.getDataReferencia()));
            }

            mov.setTipoRecorrenciaEnum(recorrenciaHelper.getTipo());
            mov.setRecorrencia_intervalo(recorrenciaHelper.getIntervalo());

            // PARCELADO divide o valor; os demais repetem o valor total
            if (recorrenciaHelper.getTipo() == TipoRecorrencia.PARCELADO) {
                mov.setValor(valorCentavosAtual / quantidade);
            } else {
                mov.setValor(valorCentavosAtual);
            }

            repository.salvarRecorrente(mov, quantidade, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
                @Override public void onErro(String erro)   { onSalvarErro(erro); }
            });

        } else {
            // Lançamento único
            mov.setValor(valorCentavosAtual);
            repository.salvar(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
                @Override public void onErro(String erro)   { onSalvarErro(erro); }
            });
        }
    }

    // ── Validação padrão ───────────────────────────────────────────────────────

    private boolean validarCamposPadrao() {
        if (valorCentavosAtual <= 0) {
            campoValor.setError("Preencha um valor válido");
            campoValor.requestFocus();
            return false;
        }
        // 👇 NOVA BARREIRA: Bloqueia salvamento de valores astronômicos (Proteção de UI/Overflow)
        if (valorCentavosAtual > VALOR_MAXIMO_CENTAVOS) {
            campoValor.setError("O valor máximo permitido é R$ 9.999.999,99");
            campoValor.requestFocus();
            Toast.makeText(this, "Reduza o valor para salvar", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (campoCategoria.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (campoData.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Informe uma data", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ── Exclusão ───────────────────────────────────────────────────────────────

    private void confirmarExcluir() {
        new AlertDialog.Builder(this)
                .setTitle(getTituloExclusao())
                .setMessage(getMensagemExclusao())
                .setPositiveButton("Confirmar", (d, w) -> excluirMovimentacao())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirMovimentacao() {
        if (itemEmEdicao == null) return;
        repository.excluir(itemEmEdicao, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
            @Override public void onErro(String erro)   { onSalvarErro(erro); }
        });
    }

    // ── Navegação ──────────────────────────────────────────────────────────────

    public void retornarPrincipal(View view) {
        if (salvandoEmProgresso) {
            new AlertDialog.Builder(this)
                    .setTitle("Operação em andamento")
                    .setMessage("Um salvamento está em progresso. Deseja sair mesmo assim?")
                    .setPositiveButton("Sair", (d, w) -> finish())
                    .setNegativeButton("Aguardar", null)
                    .show();
            // 👇 Remova o 'return;' daqui se ele estiver bloqueando a execução do diálogo!
            return; // Mantemos o return para a função não chamar o finish() da linha de baixo
        }
        finish();
    }

    // ── Window Insets ──────────────────────────────────────────────────────────

    private void setupWindowInsets() {
        View rootView = findViewById(R.id.main);
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}