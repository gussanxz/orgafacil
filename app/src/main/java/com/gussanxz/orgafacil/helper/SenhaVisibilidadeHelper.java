package com.gussanxz.orgafacil.helper;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.gussanxz.orgafacil.R;

public class SenhaVisibilidadeHelper {


    @SuppressLint("ClickableViewAccessibility")
    public static void ativarAlternancia(EditText campoSenha) {
        final boolean[] senhaVisivel = {false};

        Drawable olhoAberto = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_24);
        Drawable olhoFechado = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_visibility_off_24);
        Drawable cadeado = ContextCompat.getDrawable(campoSenha.getContext(), R.drawable.ic_cadeado_cinza_24);

        campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoFechado, null);

        campoSenha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                final int DRAWABLE_RIGHT = 2;

                if (event.getRawX() >= (campoSenha.getRight() - campoSenha.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    // Executa como se fosse um clique, útil para TalkBack e acessibilidade
                    v.performClick();

                    // Alterna visibilidade
                    if (senhaVisivel[0]) {
                        campoSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoFechado, null);
                    } else {
                        campoSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoAberto, null);
                    }

                    senhaVisivel[0] = !senhaVisivel[0];
                    campoSenha.setSelection(campoSenha.getText().length());

                    return true;
                }
            }
            return false;
        });

    }
}
