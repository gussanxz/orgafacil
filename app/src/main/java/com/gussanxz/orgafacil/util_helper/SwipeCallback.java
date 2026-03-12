package com.gussanxz.orgafacil.util_helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import java.util.List;

/**
 * SwipeCallback — Atualizado.
 *
 * Mantém o visual original (texto "Excluir"/"Editar", vermelho/verde).
 *
 * Uso em telas SEM headers (ex: categorias):
 * Sobrescreva apenas onSwiped(viewHolder, direction) como antes — continua funcionando.
 *
 * Uso em telas COM headers (ex: ContasActivity, ListaMovimentacoesFragment):
 * Sobrescreva onHeaderSwipeDelete() e onMovimentoSwiped() para o comportamento completo.
 */
public abstract class SwipeCallback extends ItemTouchHelper.SimpleCallback {

    private final Paint pFundoExcluir;
    private final Paint pFundoEditar;
    private final Paint pFundoExcluirDia;
    private final Paint pTexto;
    private final Context context;

    public SwipeCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;

        pFundoExcluir = new Paint();
        pFundoExcluir.setColor(Color.parseColor("#F44336"));

        pFundoExcluirDia = new Paint();
        pFundoExcluirDia.setColor(Color.parseColor("#B71C1C"));

        pFundoEditar = new Paint();
        pFundoEditar.setColor(Color.parseColor("#4CAF50"));

        pTexto = new Paint();
        pTexto.setColor(Color.WHITE);
        pTexto.setTextSize(40f);
        pTexto.setAntiAlias(true);
    }

    // =========================================================================
    // CONTROLE DE DIREÇÕES POR TIPO DE ITEM
    // Só aplica restrição se o adapter for do tipo que tem headers.
    // Listas simples (categorias, etc.) não têm TYPE_HEADER — sem impacto.
    // =========================================================================

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == AdapterItemListaMovimentacao.TYPE_HEADER) {
            return ItemTouchHelper.LEFT; // header: só excluir dia
        }
        return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    // =========================================================================
    // DISPATCH — detecta se é header ou item normal
    // =========================================================================

    @Override
    public final void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) return;

        if (viewHolder.getItemViewType() == AdapterItemListaMovimentacao.TYPE_HEADER) {
            if (viewHolder.itemView.getParent() instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) viewHolder.itemView.getParent();
                if (rv.getAdapter() instanceof AdapterMovimentacaoLista) {
                    AdapterMovimentacaoLista adapter = (AdapterMovimentacaoLista) rv.getAdapter();
                    AdapterItemListaMovimentacao headerItem = adapter.getItens().get(pos);
                    String tituloDia = headerItem.tituloDia != null ? headerItem.tituloDia : headerItem.data;
                    List<MovimentacaoModel> movsDoDia = adapter.getMovimentacoesDoDia(pos);
                    adapter.notifyItemChanged(pos);
                    onHeaderSwipeDelete(tituloDia, movsDoDia);
                }
            }
            return;
        }

        // Item normal → chama o método unificado que as subclasses sobrescrevem
        onMovimentoSwiped(viewHolder, direction, pos);
    }

    // =========================================================================
    // MÉTODOS DE EXTENSÃO — NÃO abstratos.
    // Telas simples (sem headers) sobrescrevem apenas onMovimentoSwiped.
    // Telas com headers sobrescrevem os dois.
    // =========================================================================

    /**
     * Chamado quando o header de um dia é arrastado para a esquerda.
     * Implementação padrão vazia — sobrescreva nas telas que precisam.
     */
    protected void onHeaderSwipeDelete(String tituloDia, List<MovimentacaoModel> movimentos) {
        // padrão: não faz nada. Sobrescreva em ContasActivity e ListaMovimentacoesFragment.
    }

    /**
     * Chamado quando um item de movimentação é arrastado.
     *
     * Telas simples que sobrescreviam onSwiped() diretamente devem migrar para cá.
     * Assinatura compatível: recebe viewHolder, direction e position.
     */
    protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                     int direction,
                                     int position) {
        // padrão: não faz nada. Sobrescreva para tratar excluir/editar.
    }

    // =========================================================================
    // VISUAL DO FUNDO — mantém estilo original
    // =========================================================================

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;

        // Define as cores
        ColorDrawable background = new ColorDrawable();
        Drawable icon;
        int iconMargin = (itemView.getHeight() - 60) / 2; // Centraliza o ícone (supondo ícone de 60px)
        int iconTop = itemView.getTop() + (itemView.getHeight() - 60) / 2;
        int iconBottom = iconTop + 60;

        // Deslizando para a Direita (EDITAR)
        if (dX > 0) {
            background.setColor(Color.parseColor("#2196F3")); // Azul (pode mudar pra sua cor)
            background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
            background.draw(c);

            icon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_lapis_editar_24);
            if (icon != null) {
                int iconLeft = itemView.getLeft() + iconMargin;
                int iconRight = itemView.getLeft() + iconMargin + 60;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(c);
            }
        }
        // Deslizando para a Esquerda (EXCLUIR)
        else if (dX < 0) {
            background.setColor(Color.parseColor("#F44336")); // Vermelho
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            icon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_lixeira_excluir_24);
            if (icon != null) {
                int iconLeft = itemView.getRight() - iconMargin - 60;
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}