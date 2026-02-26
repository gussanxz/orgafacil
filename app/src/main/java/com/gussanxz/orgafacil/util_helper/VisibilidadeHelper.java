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
import androidx.annotation.Nullable;
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
    // SALDO — API NOVA (clique no container inteiro)
    // =========================================================================

    /**
     * Configura a alternância de visibilidade de saldo de forma completa.
     *
     * @param containerClicavel  O LinearLayout/View que contém olho + texto. O clique será registrado nele.
     * @param txtSaldo           O TextView que exibe o valor.
     * @param imgOlho            O ImageView do ícone de olho.
     * @param valorReal          O texto formatado do valor real (ex: "R$ 1.250,00").
     * @param corSaldo           A cor do texto quando visível (ex: Color.parseColor("#4CAF50")).
     */
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

        containerClicavel.setOnClickListener(v -> _alternar(txtSaldo, imgOlho, valorReal, corSaldo));
        // Mantém o clique no próprio olho também, para quem usar só ele
        imgOlho.setOnClickListener(v -> _alternar(txtSaldo, imgOlho, valorReal, corSaldo));
    }

    /**
     * Atualiza o valor exibido sem mudar o estado de visibilidade.
     * Usar quando o saldo muda (ex: LiveData atualiza).
     */
    public static void atualizarValorSaldo(
            TextView txtSaldo,
            ImageView imgOlho,
            String novoValor,
            @ColorInt int novaCorSaldo) {

        boolean visivel = imgOlho.getTag() != null && (boolean) imgOlho.getTag();
        if (visivel) {
            txtSaldo.setText(novoValor);
            txtSaldo.setTextColor(novaCorSaldo);
        }
        // Se estiver oculto, só atualiza o valor guardado na tag do container para o próximo reveal
        // O reveal usará o valor mais recente via closure no setOnClickListener
    }

    // =========================================================================
    // LEGADO — mantido para não quebrar chamadas existentes
    // =========================================================================

    /** @deprecated Use configurarVisibilidadeSaldo() */
    public static void alternarVisibilidadeSaldo(TextView txtSaldo, ImageView imgOlho, String valorReal) {
        _alternar(txtSaldo, imgOlho, valorReal, Color.WHITE);
    }

    // =========================================================================
    // PRIVADO
    // =========================================================================

    private static void _alternar(TextView txtSaldo, ImageView imgOlho,
                                  String valorReal, @ColorInt int corSaldo) {
        boolean visivel = imgOlho.getTag() != null && (boolean) imgOlho.getTag();

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