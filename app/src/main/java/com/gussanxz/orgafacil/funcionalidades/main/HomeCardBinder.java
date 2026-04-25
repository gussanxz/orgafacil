package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.gussanxz.orgafacil.R;

/**
 * HomeCardBinder
 *
 * Responsabilidade única: vincular um {@link HomeMenuItem} a um {@link CardView}
 * já inflado no layout (ac_main_home.xml + item_card_home.xml).
 *
 * Boas práticas aplicadas:
 *  - Separação de responsabilidades: a Activity não conhece IDs internos
 *    dos cards — toda lógica de binding fica aqui.
 *  - Método estático puro (sem estado): fácil de testar e reutilizar.
 *  - Nenhuma string hard-coded: textos e cores via recursos (@string / @color).
 *  - Alpha reduzido para cards desabilitados no lugar de overlay preto
 *    (mais elegante e compatível com tema escuro).
 */
public final class HomeCardBinder {

    private HomeCardBinder() {}

    // ─────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────

    /**
     * Preenche o conteúdo visual de um card com os dados do item.
     *
     * @param card    O CardView raiz (filho direto do ConstraintLayout da home)
     * @param item    Os dados do item de menu
     * @param context Contexto para acesso a recursos
     */
    public static void bind(
            @NonNull CardView card,
            @NonNull HomeMenuItem item,
            @NonNull Context context) {

        // Localiza as views internas do <include layout="@layout/item_card_home">
        ImageView icone      = card.findViewById(R.id.iconeCard);
        TextView  titulo     = card.findViewById(R.id.tituloCard);
        View      dot        = card.findViewById(R.id.dotStatusCard);
        TextView  textoStatus = card.findViewById(R.id.textoStatusCard);

        if (icone == null || titulo == null || dot == null || textoStatus == null) return;

        // ── Ícone ────────────────────────────────────────────────
        icone.setImageResource(item.getIconeRes());

        // ── Título ───────────────────────────────────────────────
        titulo.setText(item.getTituloRes());

        // ── Status (dot + texto) ─────────────────────────────────
        int corStatus = resolverCorStatus(context, item.getStatus());
        String labelStatus = resolverLabelStatus(context, item.getStatus());

        dot.setBackgroundTintList(ColorStateList.valueOf(corStatus));
        textoStatus.setText(labelStatus);
        textoStatus.setTextColor(corStatus);

        // ── Estado habilitado / bloqueado ────────────────────────
        float alpha = item.isHabilitado() ? 1.0f : 0.45f;
        card.setAlpha(alpha);
        // O click listener é configurado pela Activity (via android:onClick no XML
        // para os habilitados, e via setOnClickListener para os bloqueados).
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────

    private static int resolverCorStatus(
            @NonNull Context context,
            @NonNull HomeMenuItem.StatusModulo status) {

        switch (status) {
            case EM_TESTE:
                return ContextCompat.getColor(context, R.color.cor_status_teste);
            case EM_DESENVOLVIMENTO:
                return ContextCompat.getColor(context, R.color.cor_status_dev);
            case EM_BREVE:
            default:
                return ContextCompat.getColor(context, R.color.cor_status_breve);
        }
    }

    private static String resolverLabelStatus(
            @NonNull Context context,
            @NonNull HomeMenuItem.StatusModulo status) {

        switch (status) {
            case EM_TESTE:
                return context.getString(R.string.status_em_teste);
            case EM_DESENVOLVIMENTO:
                return context.getString(R.string.status_em_desenvolvimento);
            case EM_BREVE:
            default:
                return context.getString(R.string.status_em_breve);
        }
    }
}