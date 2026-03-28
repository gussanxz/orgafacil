package com.gussanxz.orgafacil.util_helper;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Encapsula toda a lógica de UI do painel de recorrência.
 * Usado por DespesasActivity e ReceitasActivity — basta chamar init() no onCreate.
 *
 * Atualizado com suporte a Progressive Disclosure: o sub-painel "Personalizado"
 * e seus sub-chips agora são lidos para compor o TipoRecorrencia final.
 */
public class RecorrenciaFormHelper {

    public interface OnRecorrenciaChangedListener {
        /** Chamado toda vez que qualquer campo de recorrência muda. */
        void onChange(boolean repetirAtivo, TipoRecorrencia tipo, int intervalo,
                      int quantidade, Date dataReferencia);
    }

    private final Context context;

    // Views
    private final MaterialSwitch      checkboxRepetir;
    private final View                painelRecorrencia;
    private final ChipGroup           chipGroup;
    private final Chip                chipSemanal, chipQuinzenal,
            chipMensal, chipPersonalizado, chipCadaXDias, chipCadaXMeses;

    // Novos campos do Progressive Disclosure
    private final View                painelPersonalizado;
    private final ChipGroup           chipGroupIntervaloPersonalizado;

    private final TextInputLayout     layoutIntervalo;
    private final TextInputEditText   editIntervalo;
    private final TextInputLayout     layoutQtd;
    private final TextInputEditText   editQtd;
    private final TextInputEditText   editDataReferencia;
    private final TextView            textPreview;

    // Estado inicial padrão (já que Parcelado foi removido da UI)
    private TipoRecorrencia tipoAtual = TipoRecorrencia.MENSAL;
    private Date            dataReferencia = null;

    private OnRecorrenciaChangedListener listener;

    public RecorrenciaFormHelper(View root, OnRecorrenciaChangedListener listener) {
        this.context  = root.getContext();
        this.listener = listener;

        checkboxRepetir                 = root.findViewById(R.id.checkboxRepetir);
        painelRecorrencia               = root.findViewById(R.id.painelRecorrencia);
        chipGroup                       = root.findViewById(R.id.chipGroupTipoRecorrencia);
        chipSemanal                     = root.findViewById(R.id.chipSemanal);
        chipQuinzenal                   = root.findViewById(R.id.chipQuinzenal);
        chipMensal                      = root.findViewById(R.id.chipMensal);
        chipPersonalizado               = root.findViewById(R.id.chipPersonalizado);

        painelPersonalizado             = root.findViewById(R.id.painelPersonalizado);
        chipGroupIntervaloPersonalizado = root.findViewById(R.id.chipGroupIntervaloPersonalizado);
        chipCadaXDias                   = root.findViewById(R.id.chipCadaXDias);
        chipCadaXMeses                  = root.findViewById(R.id.chipCadaXMeses);

        layoutIntervalo                 = root.findViewById(R.id.textInputIntervalo);
        editIntervalo                   = root.findViewById(R.id.editIntervalo);
        layoutQtd                       = root.findViewById(R.id.textInputMeses);
        editQtd                         = root.findViewById(R.id.editQtdMeses);
        editDataReferencia              = root.findViewById(R.id.editDataReferencia);
        textPreview                     = root.findViewById(R.id.textPreviewRecorrencia);

        configurarListeners();
    }

    // ── configuração ─────────────────────────────────────────────────────────

    private void configurarListeners() {

        // Switch mostra/esconde o painel principal
        checkboxRepetir.setOnCheckedChangeListener((btn, checked) -> {
            if (painelRecorrencia != null) {
                painelRecorrencia.setVisibility(checked ? View.VISIBLE : View.GONE);
            }
            atualizarPreview();
            notificar();
        });

        // Chips do grupo PRINCIPAL
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);

                if (id == R.id.chipPersonalizado) {
                    // Revela o sub-painel
                    if (painelPersonalizado != null) painelPersonalizado.setVisibility(View.VISIBLE);
                    // Define o tipo atual baseado no que estiver marcado no sub-grupo
                    lerSubGrupoPersonalizado();
                } else {
                    // Esconde o sub-painel
                    if (painelPersonalizado != null) painelPersonalizado.setVisibility(View.GONE);

                    if      (id == R.id.chipSemanal)    tipoAtual = TipoRecorrencia.SEMANAL;
                    else if (id == R.id.chipQuinzenal)  tipoAtual = TipoRecorrencia.QUINZENAL;
                    else if (id == R.id.chipMensal)     tipoAtual = TipoRecorrencia.MENSAL;
                }

                atualizarCamposVisiveis();
                atualizarPreview();
                notificar();
            });
        }

        // Chips do SUB-GRUPO (só reage se o painel personalizado estiver ativo)
        if (chipGroupIntervaloPersonalizado != null) {
            chipGroupIntervaloPersonalizado.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (chipGroup != null && !chipGroup.getCheckedChipIds().isEmpty()) {
                    if (chipGroup.getCheckedChipIds().get(0) == R.id.chipPersonalizado) {
                        lerSubGrupoPersonalizado();
                        atualizarCamposVisiveis();
                        atualizarPreview();
                        notificar();
                    }
                }
            });
        }

        // Campos de texto — atualiza preview em tempo real
        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { atualizarPreview(); notificar(); }
        };
        if (editIntervalo != null) editIntervalo.addTextChangedListener(previewWatcher);
        if (editQtd != null) editQtd.addTextChangedListener(previewWatcher);

        // Data de referência
        if (editDataReferencia != null) {
            editDataReferencia.setOnClickListener(v -> abrirDatePickerReferencia());
        }
    }

    /** Lê qual chip está selecionado dentro do grupo "Personalizado" */
    private void lerSubGrupoPersonalizado() {
        if (chipGroupIntervaloPersonalizado == null) return;
        List<Integer> subIds = chipGroupIntervaloPersonalizado.getCheckedChipIds();
        if (!subIds.isEmpty()) {
            if (subIds.get(0) == R.id.chipCadaXDias) {
                tipoAtual = TipoRecorrencia.CADA_X_DIAS;
            } else {
                tipoAtual = TipoRecorrencia.CADA_X_MESES;
            }
        } else {
            tipoAtual = TipoRecorrencia.CADA_X_MESES; // Fallback seguro
        }
    }

    /** Atualiza visibilidade dos campos conforme o tipo selecionado. */
    private void atualizarCamposVisiveis() {
        if (layoutIntervalo == null || layoutQtd == null) return;

        String hintQtd;
        switch (tipoAtual) {
            case SEMANAL:      hintQtd = "Nº de semanas";           break;
            case QUINZENAL:    hintQtd = "Nº de quinzenas";         break;
            case MENSAL:       hintQtd = "Nº de meses";             break;
            case CADA_X_DIAS:  hintQtd = "Nº de repetições";        break;
            case CADA_X_MESES: hintQtd = "Nº de repetições";        break;
            default:           hintQtd = "Quantidade de repetições"; break;
        }
        layoutQtd.setHint(hintQtd);

        // Hint do intervalo
        if (tipoAtual == TipoRecorrencia.CADA_X_DIAS) {
            layoutIntervalo.setHint("A cada quantos dias?");
        } else if (tipoAtual == TipoRecorrencia.CADA_X_MESES) {
            layoutIntervalo.setHint("A cada quantos meses?");
        }
    }

    private void abrirDatePickerReferencia() {
        Calendar c = Calendar.getInstance();
        if (dataReferencia != null) c.setTime(dataReferencia);

        new DatePickerDialog(context, (view, year, month, day) -> {
            c.set(year, month, day);
            dataReferencia = c.getTime();
            if (editDataReferencia != null) {
                editDataReferencia.setText(
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataReferencia));
            }
            atualizarPreview();
            notificar();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Gera texto de preview amigável da configuração atual. */
    private void atualizarPreview() {
        if (checkboxRepetir == null || textPreview == null) return;

        if (!checkboxRepetir.isChecked()) {
            textPreview.setVisibility(View.GONE);
            return;
        }

        int qtd       = parseIntSafe(editQtd != null ? editQtd.getText() : null);
        int intervalo = parseIntSafe(editIntervalo != null ? editIntervalo.getText() : null);

        if (qtd <= 0) { textPreview.setVisibility(View.GONE); return; }

        StringBuilder sb = new StringBuilder();

        switch (tipoAtual) {
            case SEMANAL:
                sb.append("Repetir por ").append(qtd).append(" semana(s), a cada 7 dias");
                break;
            case QUINZENAL:
                sb.append("Repetir por ").append(qtd).append(" quinzena(s), a cada 15 dias");
                break;
            case MENSAL:
                sb.append("Repetir por ").append(qtd).append(" mês(es), todo mês no mesmo dia");
                break;
            case CADA_X_DIAS:
                if (intervalo > 0)
                    sb.append("Repetir ").append(qtd).append("x, a cada ").append(intervalo).append(" dias");
                else
                    sb.append("Informe o intervalo em dias");
                break;
            case CADA_X_MESES:
                if (intervalo > 0)
                    sb.append("Repetir ").append(qtd).append("x, a cada ").append(intervalo).append(" meses");
                else
                    sb.append("Informe o intervalo em meses");
                break;
            default:
                break;
        }

        if (dataReferencia != null) {
            sb.append("\nInício: ")
                    .append(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dataReferencia));
        }

        textPreview.setText(sb.toString());
        textPreview.setVisibility(View.VISIBLE);
    }

    private void notificar() {
        if (listener == null) return;
        listener.onChange(
                checkboxRepetir != null && checkboxRepetir.isChecked(),
                tipoAtual,
                parseIntSafe(editIntervalo != null ? editIntervalo.getText() : null),
                parseIntSafe(editQtd != null ? editQtd.getText() : null),
                dataReferencia
        );
    }

    // ── acesso público ───────────────────────────────────────────────────────

    public boolean isRepetirAtivo()    { return checkboxRepetir != null && checkboxRepetir.isChecked(); }
    public TipoRecorrencia getTipo()   { return tipoAtual; }
    public int getIntervalo()          { return parseIntSafe(editIntervalo != null ? editIntervalo.getText() : null); }
    public int getQuantidade()         { return parseIntSafe(editQtd != null ? editQtd.getText() : null); }
    public Date getDataReferencia()    { return dataReferencia; }

    /**
     * Retorna a data de início da série: dataReferencia se informada,
     * caso contrário a data passada como argumento (data do campo principal da activity).
     */
    public Timestamp resolverDataInicio(Date dataFormulario) {
        Date base = (dataReferencia != null) ? dataReferencia : dataFormulario;
        return new Timestamp(base);
    }

    /** Valida os campos obrigatórios e retorna mensagem de erro ou null se OK. */
    public String validar() {
        if (!isRepetirAtivo()) return null; // sem recorrência, sem validação aqui

        int qtd = getQuantidade();
        if (qtd < 2)   return "Informe ao menos 2 repetições.";
        if (qtd > 999) return "Máximo de 999 repetições.";

        if (tipoAtual == TipoRecorrencia.CADA_X_DIAS || tipoAtual == TipoRecorrencia.CADA_X_MESES) {
            int intervalo = getIntervalo();
            if (intervalo <= 0) return "Informe o intervalo (" + tipoAtual.getUnidade() + ").";
        }

        return null; // tudo ok
    }

    // ── utilitário ───────────────────────────────────────────────────────────

    private static int parseIntSafe(Editable e) {
        if (e == null || e.toString().trim().isEmpty()) return 0;
        try { return Integer.parseInt(e.toString().trim()); }
        catch (NumberFormatException ex) { return 0; }
    }
}