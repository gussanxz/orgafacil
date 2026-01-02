package com.gussanxz.orgafacil.helper;

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

    public static void alternarVisibilidadeSaldo(TextView txtSaldo, ImageView imgOlho, String valorReal) {

        boolean saldoEstaVisivel;

        if (imgOlho.getTag() == null) {
            saldoEstaVisivel = true;

        } else {
            saldoEstaVisivel = (boolean) imgOlho.getTag();
        }

        //Se o saldo esta visivel, torna invisivel
        if (saldoEstaVisivel) {
            txtSaldo.setText("R$ ****** "); //mascara o valor
            imgOlho.setImageResource(R.drawable.ic_visibility_off_24);
            imgOlho.setTag(false);
        } else {
            txtSaldo.setText(valorReal);
            imgOlho.setImageResource(R.drawable.ic_visibility_24);
            imgOlho.setTag(true);
        }
    }
}
