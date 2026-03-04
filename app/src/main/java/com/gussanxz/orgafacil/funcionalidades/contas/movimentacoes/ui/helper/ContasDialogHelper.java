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

public class ContasDialogHelper {

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

    public static void confirmarExclusao(Context context, MovimentacaoModel mov, AcaoUnicaCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirmar_exclusao, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

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

    public static void confirmarExclusaoDoDia(Context context, String tituloDia, List<MovimentacaoModel> movimentos, AcaoUnicaCallback callback) {
        if (movimentos == null || movimentos.isEmpty()) return;

        int total = movimentos.size();
        boolean temRecorrentes = false;
        for (MovimentacaoModel m : movimentos) {
            if (m.getTotal_parcelas() > 1) { temRecorrentes = true; break; }
        }

        String msgPrincipal = "Deseja excluir " + total + (total == 1 ? " movimentação" : " movimentações") +
                " de \"" + tituloDia + "\"?\n\nEsta ação não pode ser desfeita.";

        String msgFinal = temRecorrentes
                ? msgPrincipal + "\n\n⚠️ Atenção: há lançamentos recorrentes neste dia. Apenas as parcelas deste dia serão removidas."
                : msgPrincipal;

        new AlertDialog.Builder(context)
                .setTitle("Excluir " + tituloDia)
                .setMessage(msgFinal)
                .setPositiveButton("Excluir tudo", (dialog, which) -> callback.onConfirmar())
                .setNegativeButton("Cancelar", (dialog, which) -> callback.onCancelar())
                .show();
    }

    public static void confirmarPagamentoOuRecebimento(Context context, MovimentacaoModel mov, AcaoMultiplaCallback callback) {
        String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";

        if (mov.getTotal_parcelas() > 1 && mov.getParcela_atual() < mov.getTotal_parcelas()) {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Como deseja confirmar a movimentação '" + mov.getDescricao() + "'?")
                    .setPositiveButton("Apenas esta", (d, w) -> callback.onApenasEsta())
                    .setNeutralButton("Esta e seguintes", (d, w) -> callback.onEstaESeguintes())
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                    .setPositiveButton("Confirmar", (dialog, which) -> callback.onApenasEsta())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    public static void abrirMenuEscolha(Context context, EscolhaNovaMovimentacaoCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_escolha_despesa_receita, null);
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