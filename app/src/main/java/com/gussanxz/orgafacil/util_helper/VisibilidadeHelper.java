package com.gussanxz.orgafacil.util_helper;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.gussanxz.orgafacil.R;

public class VisibilidadeHelper {

    @SuppressLint("ClickableViewAccessibility")
    public static void ativarAlternanciaSenha(EditText campoSenha) {
        final boolean[] senhaVisivel = {false};

        Drawable olhoAberto = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_24);
        Drawable olhoFechado = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_off_24);

        // DICA: Buscamos o ícone da esquerda que já está no XML ou o padrão
        Drawable cadeado = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_cadeado_cinza_24);

        // Inicializa o campo
        campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoFechado, null);

        campoSenha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                final int DRAWABLE_RIGHT = 2;

                // PEGA O DRAWABLE ATUAL (Evita NullPointer se ele mudar dinamicamente)
                Drawable drawableDireita = campoSenha.getCompoundDrawables()[DRAWABLE_RIGHT];

                // CORREÇÃO: Verificamos se o drawable não é nulo antes de medir o clique
                if (drawableDireita != null && event.getRawX() >= (campoSenha.getRight() - drawableDireita.getBounds().width())) {

                    v.performClick();

                    // Pegamos o ícone da esquerda atual para não perdê-lo (caso a CadastroActivity tenha mudado ele)
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

    public static void alternarVisibilidadeSaldo(TextView txtSaldo, ImageView imgOlho, String valorReal) {
        boolean saldoEstaVisivel;
        if (imgOlho.getTag() == null) {
            saldoEstaVisivel = true;
        } else {
            saldoEstaVisivel = (boolean) imgOlho.getTag();
        }

        if (saldoEstaVisivel) {
            txtSaldo.setText("R$ **** ");
            imgOlho.setImageResource(R.drawable.ic_visibility_off_24);
            imgOlho.setTag(false);
        } else {
            txtSaldo.setText(valorReal);
            imgOlho.setImageResource(R.drawable.ic_visibility_24);
            imgOlho.setTag(true);
        }
    }
}