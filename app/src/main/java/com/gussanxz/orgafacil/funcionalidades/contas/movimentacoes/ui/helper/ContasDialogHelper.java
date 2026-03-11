package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import java.util.List;

/**
 * ContasDialogHelper
 *
 * Centraliza todos os dialogs de confirmação do módulo de Contas.
 * Tanto ContasActivity quanto ListaMovimentacoesFragment devem usar este helper —
 * nenhuma lógica de dialog deve existir diretamente nessas classes.
 *
 * MÉTODOS DISPONÍVEIS:
 *   confirmarExclusao()               — exclui uma movimentação (modo histórico)
 *   confirmarOuExcluirMovimento()     — confirma OU exclui dependendo do modo da tela
 *                                       (novo — para ListaMovimentacoesFragment)
 *   confirmarExclusaoDoDia()          — exclui todas as movimentações de um dia
 *   confirmarPagamentoOuRecebimento() — confirma pagamento/recebimento, com suporte a série
 *   abrirMenuEscolha()                — bottom sheet para escolher Receita ou Despesa
 */
public class ContasDialogHelper {

    // ── Callbacks ────────────────────────────────────────────────────────────

    public interface AcaoUnicaCallback {
        void onConfirmar();
        default void onCancelar() {}
    }

    public interface AcaoMultiplaCallback {
        void onApenasEsta();
        void onEstaESeguintes();
    }

    public interface EscolhaNovaMovimentacaoCallback {
        void onEscolherReceita();
        void onEscolherDespesa();
    }

    // ── Exclusão simples (histórico) ──────────────────────────────────────────

    /**
     * Exibe o dialog de confirmação para excluir uma movimentação.
     * Usado por: ContasActivity (modo histórico).
     */
    public static void confirmarExclusao(Context context, MovimentacaoModel mov, AcaoUnicaCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirmar_exclusao, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar   = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar    = view.findViewById(R.id.btnCancelarDialog);

        textMensagem.setText("Você deseja realmente excluir '" + mov.getDescricao() + "'?");

        btnCancelar.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onCancelar();
        });
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onConfirmar();
        });

        dialog.show();
    }

    // ── Confirmar OU Excluir dependendo do modo ───────────────────────────────

    /**
     * Exibe o dialog correto dependendo de se a tela está em modo futuro ou histórico.
     *
     * Modo futuro  → botão principal diz "Pagar" ou "Receber" (confirma a movimentação)
     * Modo histórico → botão principal diz "Excluir"
     *
     * Este método foi adicionado para cobrir o comportamento de ListaMovimentacoesFragment,
     * que precisa de lógica diferente em cada aba (Pendentes vs Histórico).
     *
     * @param ehModoFuturo true = aba "Contas a Vencer", false = aba "Histórico"
     * @param callback     onConfirmar() é chamado quando o botão principal é acionado
     */
    public static void confirmarOuExcluirMovimento(Context context,
                                                   MovimentacaoModel mov,
                                                   boolean ehModoFuturo,
                                                   AcaoUnicaCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirmar_exclusao, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar   = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar    = view.findViewById(R.id.btnCancelarDialog);

        if (ehModoFuturo) {
            String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "Pagar" : "Receber";
            textMensagem.setText(acao + " '" + mov.getDescricao() + "'?");
            btnConfirmar.setText(acao);
        } else {
            textMensagem.setText("Excluir '" + mov.getDescricao() + "'?");
            btnConfirmar.setText("Excluir");
        }

        btnCancelar.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onCancelar();
        });
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onConfirmar();
        });

        dialog.show();
    }

    // ── Exclusão de todos os lançamentos de um dia ────────────────────────────

    /**
     * Exibe o dialog de confirmação para excluir todas as movimentações de um dia (swipe no header).
     * Avisa o usuário quando há lançamentos recorrentes no grupo.
     * Usado por: ContasActivity e ListaMovimentacoesFragment.
     */
    public static void confirmarExclusaoDoDia(Context context,
                                              String tituloDia,
                                              List<MovimentacaoModel> movimentos,
                                              AcaoUnicaCallback callback) {
        if (movimentos == null || movimentos.isEmpty()) return;

        int total = movimentos.size();

        boolean temRecorrentes = false;
        for (MovimentacaoModel m : movimentos) {
            if (m.getTotal_parcelas() > 1) { temRecorrentes = true; break; }
        }

        String msgPrincipal = "Deseja excluir " + total
                + (total == 1 ? " movimentação" : " movimentações")
                + " de \"" + tituloDia + "\"?\n\nEsta ação não pode ser desfeita.";

        String msgFinal = temRecorrentes
                ? msgPrincipal + "\n\n⚠️ Atenção: há lançamentos recorrentes neste dia. "
                + "Apenas as parcelas deste dia serão removidas — as demais da série serão mantidas."
                : msgPrincipal;

        new AlertDialog.Builder(context)
                .setTitle("Excluir " + tituloDia)
                .setMessage(msgFinal)
                .setPositiveButton("Excluir tudo", (d, w) -> callback.onConfirmar())
                .setNegativeButton("Cancelar", (d, w) -> callback.onCancelar())
                .show();
    }

    // ── Confirmação de pagamento/recebimento ──────────────────────────────────

    /**
     * Exibe o dialog de confirmação de pagamento ou recebimento.
     * Para parcelas com sequência, oferece "Apenas esta" e "Esta e seguintes".
     * Para movimentações simples, oferece só "Confirmar".
     * Usado por: ContasActivity e ListaMovimentacoesFragment (via onCheckClick).
     */
    public static void confirmarPagamentoOuRecebimento(Context context,
                                                       MovimentacaoModel mov,
                                                       AcaoMultiplaCallback callback) {
        String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";

        if (mov.getTotal_parcelas() > 1 && mov.getParcela_atual() < mov.getTotal_parcelas()) {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar apenas '" + mov.getDescricao()
                            + "' ou também antecipar todas as parcelas seguintes?")
                    .setPositiveButton("Apenas esta",       (d, w) -> callback.onApenasEsta())
                    .setNeutralButton("Esta e seguintes",   (d, w) -> callback.onEstaESeguintes())
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                    .setPositiveButton("Confirmar", (d, w) -> callback.onApenasEsta())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    // ── Menu de nova movimentação ─────────────────────────────────────────────

    /**
     * Exibe o bottom sheet para o usuário escolher entre Receita e Despesa.
     * Usado por: ContasActivity.
     */
    public static void abrirMenuEscolha(Context context, EscolhaNovaMovimentacaoCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.layout_bottom_escolha_despesa_receita, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnEscolhaReceita).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onEscolherReceita();
        });
        view.findViewById(R.id.btnEscolhaDespesa).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onEscolherDespesa();
        });

        dialog.show();
    }
}