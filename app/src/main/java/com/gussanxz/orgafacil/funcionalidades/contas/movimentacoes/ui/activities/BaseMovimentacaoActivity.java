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

import com.google.android.material.chip.ChipGroup;
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
import com.gussanxz.orgafacil.util_helper.DateHelper;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;
import com.gussanxz.orgafacil.util_helper.RecorrenciaFormHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BaseMovimentacaoActivity
 *
 * Centraliza toda a lógica compartilhada entre DespesasActivity, ReceitasActivity
 * e EditarMovimentacaoActivity.
 *
 * MUDANÇAS NESTA VERSÃO (para suportar EditarMovimentacaoActivity):
 *
 *   - Campos de UI elevados de private → protected para acesso direto pela subclasse Editar.
 *     (campoValor, campoData, campoHora, campoCategoria, campoDescricao, switchStatusPago,
 *      layoutRecorrencia, checkboxRepetir, btnExcluir, recorrenciaHelper)
 *
 *   - Campos de estado elevados de private → protected.
 *     (repository, isEdicao, ehContaFutura, itemEmEdicao, categoriaIdSelecionada,
 *      valorCentavosAtual, salvandoEmProgresso)
 *
 *   - Métodos de comportamento elevados de private → protected para que o Editar
 *     possa reutilizar sem reimplementar:
 *       · setupCurrencyMask()
 *       · aplicarRegraStatusPorData()
 *       · atualizarTextoStatus()
 *       · abrirSelecaoCategoria()
 *       · configurarLauncherCategoria()
 *       · abrirSelecionadorDeData()
 *       · abrirSelecionadorDeHora()
 *       · validarLimiteHoraAtual()
 *
 *   - Novo hook onModoEdicaoAlternado(boolean) para que EditarMovimentacaoActivity
 *     possa reagir à troca de modo (ex: habilitar/desabilitar campos, trocar ícone FAB)
 *     sem precisar reimplementar toda a lógica de alternância.
 *
 *   - getTipo() tem implementação padrão baseada em itemEmEdicao, então o Editar
 *     não precisa implementar esse método abstrato (que ele não pode responder
 *     de forma estática, já que o tipo vem do objeto carregado).
 *
 *   - Card de recorrência: adicionado suporte a textResumoRecorrencia e
 *     divisorRecorrencia — IDs novos no layout que exibem o estado resumido
 *     do bloco de repetição sem precisar expandi-lo.
 *
 *   - FIX: checkboxRepetir alterado de MaterialCheckBox → MaterialSwitch para
 *     ficar consistente com o XML atualizado. A lógica não muda pois ambos
 *     herdam de CompoundButton (isChecked / setChecked / setOnCheckedChangeListener).
 *     O import de MaterialCheckBox foi removido pois não é mais referenciado.
 */
public abstract class BaseMovimentacaoActivity extends AppCompatActivity {

    // ── Constantes ─────────────────────────────────────────────────────────────
    private static final long VALOR_MAXIMO_CENTAVOS = 999_999_999_99L;

    // ── Campos de UI — protected para acesso pelas subclasses ─────────────────
    protected TextInputEditText campoData;
    protected TextInputEditText campoDescricao;
    protected TextInputEditText campoHora;
    protected EditText campoValor;
    protected EditText campoCategoria;
    protected ImageButton btnExcluir;
    protected MaterialSwitch switchStatusPago;
    protected LinearLayout layoutRecorrencia;

    // FIX: era MaterialCheckBox — trocado para MaterialSwitch para corresponder
    // ao XML (id checkboxRepetir agora é um MaterialSwitch). A API pública usada
    // aqui (isChecked, setOnCheckedChangeListener) é idêntica em ambos os tipos.
    protected MaterialSwitch checkboxRepetir;

    protected RecorrenciaFormHelper recorrenciaHelper;

    // ── Novos campos do card de recorrência ────────────────────────────────────
    protected TextView textResumoRecorrencia;
    protected View divisorRecorrencia;
    private ChipGroup chipGroupTipoRecorrencia;
    private TextInputEditText editQtdMeses;

    // ── Estado — protected para acesso pelas subclasses ───────────────────────
    protected MovimentacaoRepository repository;
    protected boolean isEdicao = false;
    protected boolean ehContaFutura = false;
    protected MovimentacaoModel itemEmEdicao = null;
    protected String categoriaIdSelecionada;
    protected long valorCentavosAtual = 0;
    protected boolean salvandoEmProgresso = false;

    private ActivityResultLauncher<Intent> launcherCategoria;
    private String TAG;

    // ── Métodos abstratos que DespesasActivity e ReceitasActivity implementam ──

    protected abstract TipoCategoriaContas getTipo();
    protected abstract int getLayoutResId();
    protected abstract String getCategoriaDefault();
    protected abstract String getTituloExclusao();
    protected abstract String getMensagemExclusao();

    // ── Métodos que subclasses PODEM sobrescrever ──────────────────────────────

    /** Validações extras além das padrão. Retorne null se válido. */
    protected String validarCamposExtra() {
        return null;
    }

    /** Chamado após salvar/editar com sucesso. */
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

    /**
     * Hook chamado sempre que o modo de edição é alternado.
     * EditarMovimentacaoActivity sobrescreve para habilitar/desabilitar campos
     * e trocar o ícone dos FABs sem precisar reimplementar nada da Base.
     */
    protected void onModoEdicaoAlternado(boolean modoEdicaoAtivo) {
        // sem comportamento padrão — subclasses implementam se precisarem
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
        campoValor        = findViewById(R.id.editValor);
        campoData         = findViewById(R.id.editData);
        campoCategoria    = findViewById(R.id.editCategoria);
        campoDescricao    = findViewById(R.id.editDescricao);
        campoHora         = findViewById(R.id.editHora);
        btnExcluir        = findViewById(R.id.btnExcluir);
        switchStatusPago  = findViewById(R.id.switchStatusPago);
        layoutRecorrencia = findViewById(R.id.layoutRecorrencia);

        // FIX: o cast agora é para MaterialSwitch, que é o tipo real no XML
        checkboxRepetir   = findViewById(R.id.checkboxRepetir);

        // Novos campos do card de recorrência (podem ser null em layouts antigos)
        textResumoRecorrencia    = findViewById(R.id.textResumoRecorrencia);
        divisorRecorrencia       = findViewById(R.id.divisorRecorrencia);
        chipGroupTipoRecorrencia = findViewById(R.id.chipGroupTipoRecorrencia);
        editQtdMeses             = findViewById(R.id.editQtdMeses);

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

        configurarListenersRecorrencia();
    }

    /**
     * Configura os listeners do card de recorrência.
     *
     * checkboxRepetir (agora MaterialSwitch): mostra/oculta o painel e
     * atualiza o subtítulo do card.
     * chipGroupTipoRecorrencia: atualiza o subtítulo ao mudar o tipo.
     * editQtdMeses: atualiza o subtítulo ao digitar a quantidade.
     *
     * Todos os IDs são opcionais — layouts que não têm o card simplificado
     * continuam funcionando normalmente (os campos serão null).
     */
    private void configurarListenersRecorrencia() {
        if (checkboxRepetir == null) return;

        checkboxRepetir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            View painelRecorrencia = findViewById(R.id.painelRecorrencia);
            if (painelRecorrencia != null) {
                painelRecorrencia.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            if (divisorRecorrencia != null) {
                divisorRecorrencia.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            if (textResumoRecorrencia != null) {
                textResumoRecorrencia.setText(
                        isChecked ? "Ativado — configure abaixo" : "Desativado"
                );
            }
        });

        if (chipGroupTipoRecorrencia != null) {
            chipGroupTipoRecorrencia.setOnCheckedStateChangeListener(
                    (group, checkedIds) -> atualizarResumoRecorrencia()
            );
        }

        if (editQtdMeses != null) {
            editQtdMeses.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    atualizarResumoRecorrencia();
                }
            });
        }
    }

    /**
     * Atualiza o subtítulo do card de recorrência com o tipo e quantidade atuais.
     * Exemplo: "Parcelado · 6x", "Mensal · 12x", "Semanal".
     */
    protected void atualizarResumoRecorrencia() {
        if (textResumoRecorrencia == null) return;
        if (checkboxRepetir == null || !checkboxRepetir.isChecked()) return;
        if (chipGroupTipoRecorrencia == null) return;

        String tipo;
        List<Integer> ids = chipGroupTipoRecorrencia.getCheckedChipIds();
        if (ids.isEmpty()) {
            tipo = "—";
        } else {
            int chipId = ids.get(0);
            if      (chipId == R.id.chipParcelado)  tipo = "Parcelado";
            else if (chipId == R.id.chipSemanal)    tipo = "Semanal";
            else if (chipId == R.id.chipQuinzenal)  tipo = "Quinzenal";
            else if (chipId == R.id.chipMensal)     tipo = "Mensal";
            else if (chipId == R.id.chipCadaXDias)  tipo = "A cada X dias";
            else if (chipId == R.id.chipCadaXMeses) tipo = "A cada X meses";
            else                                    tipo = "—";
        }

        String qtdTexto = (editQtdMeses != null && editQtdMeses.getText() != null)
                ? editQtdMeses.getText().toString().trim()
                : "";

        String resumo = qtdTexto.isEmpty() ? tipo : tipo + " · " + qtdTexto + "x";
        textResumoRecorrencia.setText(resumo);
    }

    // ── Launcher de categoria — protected para o Editar reutilizar ────────────

    protected void configurarLauncherCategoria() {
        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        categoriaIdSelecionada = result.getData().getStringExtra("categoriaId");
                        campoCategoria.setText(result.getData().getStringExtra("categoriaSelecionada"));
                    }
                });
    }

    // ── Seletores de Data e Hora — protected ──────────────────────────────────

    protected void abrirSelecionadorDeData() {
        DateHelper.exibirSeletorData(this, campoData);
    }

    protected void abrirSelecionadorDeHora() {
        Calendar agora = Calendar.getInstance();
        int horaSet   = agora.get(Calendar.HOUR_OF_DAY);
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

    protected void validarLimiteHoraAtual() {
        if (ehContaFutura) return;
        try {
            Date dataSelecionada = DateHelper.parsearData(campoData.getText().toString());
            if (dataSelecionada == null) return;

            Calendar calSelecionada = Calendar.getInstance();
            calSelecionada.setTime(dataSelecionada);
            Calendar agora = Calendar.getInstance();

            if (calSelecionada.get(Calendar.YEAR)        == agora.get(Calendar.YEAR) &&
                    calSelecionada.get(Calendar.DAY_OF_YEAR) == agora.get(Calendar.DAY_OF_YEAR)) {

                String horaTexto = campoHora != null ? campoHora.getText().toString().trim() : "";
                if (horaTexto.isEmpty() || !horaTexto.contains(":")) return;

                String[] partes  = horaTexto.split(":");
                int horaCampo    = Integer.parseInt(partes[0]);
                int minCampo     = Integer.parseInt(partes[1]);
                int horaAgora    = agora.get(Calendar.HOUR_OF_DAY);
                int minAgora     = agora.get(Calendar.MINUTE);

                if (horaCampo > horaAgora || (horaCampo == horaAgora && minCampo > minAgora)) {
                    if (campoHora != null) {
                        campoHora.setText(String.format(Locale.getDefault(), "%02d:%02d", horaAgora, minAgora));
                    }
                    Toast.makeText(this,
                            "Horário ajustado: não é possível usar horas no futuro.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Regras de Status ── protected ─────────────────────────────────────────

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

    // ── Seleção de Categoria — protected ──────────────────────────────────────

    protected void abrirSelecaoCategoria() {
        Intent intent = new Intent(this, SelecionarCategoriaContasActivity.class);
        intent.putExtra("TIPO_CATEGORIA", getTipo().getId());
        launcherCategoria.launch(intent);
    }

    // ── Máscara de Moeda — protected ──────────────────────────────────────────

    protected void setupCurrencyMask() {
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
        // Só lê do Intent se a subclasse ainda não preencheu itemEmEdicao.
        // EditarMovimentacaoActivity faz isso em getLayoutResId() para garantir
        // leitura única do Parcelable (ver BUG 4 no javadoc da subclasse).
        if (itemEmEdicao == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                itemEmEdicao = getIntent().getParcelableExtra(
                        "movimentacaoSelecionada", MovimentacaoModel.class);
            } else {
                itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada");
            }
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
            campoData.setText(DateHelper.formatarData(data));
            if (campoHora != null) {
                campoHora.setText(DateHelper.formatarHora(data));
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

        if (textResumoRecorrencia != null) {
            textResumoRecorrencia.setText("Desativado");
        }

        if (ehContaFutura) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 1);
            Date dataFutura = c.getTime();
            campoData.setText(DateHelper.formatarData(dataFutura));
            if (campoHora != null) {
                campoHora.setText("");
                campoHora.setHint("HH:mm");
            }
            aplicarRegraStatusPorData(dataFutura);
        } else {
            campoData.setText(DateHelper.dataAtual());
            if (campoHora != null) campoHora.setText(DateHelper.horaAtual());
            if (switchStatusPago != null) switchStatusPago.setChecked(true);
            aplicarRegraStatusPorData(new Date());
        }
    }

    // ── Salvar ─────────────────────────────────────────────────────────────────

    protected final void salvarMovimentacao() {
        if (salvandoEmProgresso) return;
        if (!validarCamposPadrao()) return;

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

        String dataStr = campoData.getText().toString().trim();
        String horaStr = (campoHora != null) ? campoHora.getText().toString().trim() : "00:00";

        Date date = DateHelper.parsearDataHora(dataStr, horaStr);
        mov.setData_movimentacao(new Timestamp(date));
        mov.setPago(switchStatusPago != null && switchStatusPago.isChecked());

        if (isEdicao) {
            mov.setValor(valorCentavosAtual);
            repository.editar(itemEmEdicao, mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
                @Override public void onErro(String erro)   { onSalvarErro(erro); }
            });
            return;
        }

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
            mov.setValor(valorCentavosAtual);
            repository.salvar(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { onSalvarSucesso(msg); }
                @Override public void onErro(String erro)   { onSalvarErro(erro); }
            });
        }
    }

    // ── Validação ──────────────────────────────────────────────────────────────

    private boolean validarCamposPadrao() {
        if (valorCentavosAtual <= 0) {
            campoValor.setError("Preencha um valor válido");
            campoValor.requestFocus();
            return false;
        }
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
            return;
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