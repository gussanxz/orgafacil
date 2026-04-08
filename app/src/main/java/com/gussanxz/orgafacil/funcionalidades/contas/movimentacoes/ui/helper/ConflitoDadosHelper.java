package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasViewModel;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ConflitoDadosHelper {

    public interface ResolucaoConflito {
        void usarDadosFirestore();   // descarta local, baixa do servidor
        void manterDadosLocais();    // sobe dados locais, ignora servidor
        void mesclarDados();         // adiciona os dois (soma as movimentações)
    }

    /**
     * Chame quando detectar que o tsFirestore é MAIS NOVO que ultimaAtualizacaoLocal
     * E o usuário já tinha dados carregados em tela (não é o primeiro acesso).
     */
    public static void exibirDialogoConflito(Context ctx,
                                             Timestamp tsLocal,
                                             Timestamp tsFirestore,
                                             ResolucaoConflito callback) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        String msgLocal    = tsLocal    != null ? fmt.format(tsLocal.toDate())    : "desconhecido";
        String msgFirestore = tsFirestore != null ? fmt.format(tsFirestore.toDate()) : "desconhecido";

        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Dados atualizados em outro dispositivo")
                .setMessage(
                        "Este dispositivo tem dados de " + msgLocal + ".\n" +
                                "Outro dispositivo atualizou em " + msgFirestore + ".\n\n" +
                                "O que deseja fazer?"
                )
                .setCancelable(false)
                .setPositiveButton("Usar mais recente", (d, w) -> callback.usarDadosFirestore())
                .setNegativeButton("Manter este dispositivo", (d, w) -> callback.manterDadosLocais())
                .setNeutralButton("Adicionar os dois", (d, w) -> callback.mesclarDados())
                .show();
    }
}