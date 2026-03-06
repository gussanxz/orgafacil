package com.gussanxz.orgafacil.util_helper;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.gussanxz.orgafacil.R;

public class VisibilidadeHelper {

    private static final String TAG_VISIVEL = "saldo_visivel";
    private static final String TEXTO_CENSURADO = "R$ ••••••";

    // =========================================================================
    // SENHA (mantido igual)
    // =========================================================================

    @SuppressLint("ClickableViewAccessibility")
    public static void ativarAlternanciaSenha(EditText campoSenha) {
        final boolean[] senhaVisivel = {false};

        Drawable olhoAberto  = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_24);
        Drawable olhoFechado = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_off_24);
        Drawable cadeado     = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_cadeado_cinza_24);

        campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoFechado, null);

        campoSenha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableDireita = campoSenha.getCompoundDrawables()[2];
                if (drawableDireita != null &&
                        event.getRawX() >= (campoSenha.getRight() - drawableDireita.getBounds().width())) {
                    v.performClick();
                    Drawable iconeEsquerdaAtual = campoSenha.getCompoundDrawables()[0];
                    if (senhaVisivel[0]) {
                        campoSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        campoSenha.setCompoundDrawablesWithIntrinsicBounds(iconeEsquerdaAtual, null, olhoFechado, null);
                    } else {
                        campoSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        campoSenha.setCompoundDrawablesWithIntrinsicBounds(iconeEsquerdaAtual, null, olhoAberto, null);
                    }
                    senhaVisivel[0] = !senhaVisivel[0];
                    campoSenha.setSelection(campoSenha.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    // =========================================================================
    // SALDO — API CORRIGIDA
    // =========================================================================

    public static void configurarVisibilidadeSaldo(
            View containerClicavel,
            TextView txtSaldo,
            ImageView imgOlho,
            String valorReal,
            @ColorInt int corSaldo) {

        // Garante que o estado inicial esteja correto (visível por padrão)
        if (imgOlho.getTag() == null) {
            imgOlho.setTag(true); // true = visível
        }

        // Armazena o valor real atualizado em um Object[] na Tag do TextView
        txtSaldo.setTag(new Object[]{valorReal, corSaldo});

        // O listener agora busca os dados na hora do clique (sempre atualizados)
        View.OnClickListener listener = v -> _alternar(txtSaldo, imgOlho);

        containerClicavel.setOnClickListener(listener);
        imgOlho.setOnClickListener(listener);
    }

    public static void atualizarValorSaldo(
            TextView txtSaldo,
            ImageView imgOlho,
            String novoValor,
            @ColorInt int novaCorSaldo) {

        // Atualiza a "memória" da view para o clique futuro
        txtSaldo.setTag(new Object[]{novoValor, novaCorSaldo});

        boolean visivel = imgOlho.getTag() != null && (boolean) imgOlho.getTag();
        if (visivel) {
            txtSaldo.setText(novoValor);
            txtSaldo.setTextColor(novaCorSaldo);
        }
    }

    // =========================================================================
    // LEGADO — mantido para não quebrar chamadas existentes
    // =========================================================================

    /** @deprecated Use configurarVisibilidadeSaldo() */
    @Deprecated
    public static void alternarVisibilidadeSaldo(TextView txtSaldo, ImageView imgOlho, String valorReal) {
        txtSaldo.setTag(new Object[]{valorReal, Color.WHITE});
        _alternar(txtSaldo, imgOlho);
    }

    // =========================================================================
    // PRIVADO
    // =========================================================================

    private static void _alternar(TextView txtSaldo, ImageView imgOlho) {
        boolean visivel = imgOlho.getTag() != null && (boolean) imgOlho.getTag();

        // Recupera a string mais recente guardada na view
        Object[] dados = (Object[]) txtSaldo.getTag();
        String valorReal = dados != null ? (String) dados[0] : "";
        int corSaldo = dados != null ? (int) dados[1] : Color.WHITE;

        if (visivel) {
            // Ocultar
            txtSaldo.setText(TEXTO_CENSURADO);
            txtSaldo.setTextColor(Color.WHITE);
            imgOlho.setImageResource(R.drawable.ic_visibility_off_24);
            imgOlho.setTag(false);
        } else {
            // Revelar
            txtSaldo.setText(valorReal);
            txtSaldo.setTextColor(corSaldo);
            imgOlho.setImageResource(R.drawable.ic_visibility_24);
            imgOlho.setTag(true);
        }
    }
}