package com.gussanxz.orgafacil.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;

// Classe abstrata: obriga quem usar a implementar o método onSwiped
public abstract class SwipeCallback extends ItemTouchHelper.SimpleCallback {

    private final Paint pFundoExcluir; // Vermelho
    private final Paint pFundoEditar;  // Verde
    private final Paint pTexto;
    private final Context context;

    public SwipeCallback(Context context) {
        // Habilita arrastar para ESQUERDA e DIREITA
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;

        // Configura Vermelho (Excluir)
        pFundoExcluir = new Paint();
        pFundoExcluir.setColor(Color.parseColor("#F44336"));

        // Configura Verde (Editar)
        pFundoEditar = new Paint();
        pFundoEditar.setColor(Color.parseColor("#4CAF50"));

        // Configura Texto (Branco)
        pTexto = new Paint();
        pTexto.setColor(Color.WHITE);
        pTexto.setTextSize(40f);
        pTexto.setAntiAlias(true);
        // pTexto.setTypeface(Typeface.DEFAULT_BOLD); // Se quiser negrito
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Não vamos mover itens de posição (apenas swipe)
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        float height = (float) itemView.getBottom() - (float) itemView.getTop();
        float textY = itemView.getTop() + height / 2f + 15; // Centraliza texto verticalmente

        if (dX > 0) {
            // ---> SWIPE PARA DIREITA (EDITAR - VERDE)

            // Desenha o fundo Verde
            c.drawRect(
                    (float) itemView.getLeft(),
                    (float) itemView.getTop(),
                    dX,
                    (float) itemView.getBottom(),
                    pFundoEditar
            );

            // Desenha o texto "Editar" na esquerda
            // (Um pouco afastado da margem esquerda)
            c.drawText("Editar", itemView.getLeft() + 50, textY, pTexto);

        } else if (dX < 0) {
            // <--- SWIPE PARA ESQUERDA (EXCLUIR - VERMELHO)

            // Desenha o fundo Vermelho
            c.drawRect(
                    (float) itemView.getRight() + dX,
                    (float) itemView.getTop(),
                    (float) itemView.getRight(),
                    (float) itemView.getBottom(),
                    pFundoExcluir
            );

            // Desenha o texto "Excluir" na direita
            // (Um pouco afastado da margem direita)
            c.drawText("Excluir", itemView.getRight() - 150, textY, pTexto);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}