package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ServicoModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterExibirPSGradeListaNovaVenda extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LISTA = 1;
    private static final int TYPE_GRADE = 2;

    private List<ItemVendaModel> listaItens;
    private boolean isGridMode = false;
    private final OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(ItemVendaModel item);
    }

    public AdapterExibirPSGradeListaNovaVenda(List<ItemVendaModel> listaItens, OnItemClickListener listener) {
        this.listaItens = listaItens;
        this.listener = listener;
    }

    public void setModoGrade(boolean ativarGrade) {
        this.isGridMode = ativarGrade;
        notifyDataSetChanged();
    }

    public void atualizarLista(List<ItemVendaModel> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? TYPE_GRADE : TYPE_LISTA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_GRADE) {
            View view = inflater.inflate(R.layout.item_venda_grade, parent, false);
            return new GradeViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_venda_lista, parent, false);
            return new ListaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemVendaModel item = listaItens.get(position);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

        if (holder instanceof GradeViewHolder) {
            ((GradeViewHolder) holder).bind(item);
        } else if (holder instanceof ListaViewHolder) {
            ((ListaViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    private void aplicarEstiloVisual(
            ItemVendaModel item,
            TextView txtTag,
            TextView txtStatus,
            ImageView imgIcone,
            CardView cardIcone,
            View rootView
    ) {
        boolean isProduto = item.getTipo() == ItemVendaModel.TIPO_PRODUTO;
        boolean isAtivo = isItemAtivo(item);

        int corDestaque;
        int corFundoElements = Color.WHITE;
        int backgroundRes;
        String textoTag;

        if (isProduto) {
            corDestaque = Color.parseColor("#EF6C00");
            textoTag = "PRODUTO";
            backgroundRes = R.drawable.bg_tag_produto;
        } else {
            corDestaque = Color.parseColor("#1565C0");
            textoTag = "SERVIÇO";
            backgroundRes = R.drawable.bg_tag_servico;
        }

        txtTag.setText(textoTag);
        txtTag.setTextColor(corDestaque);
        txtTag.setBackgroundResource(backgroundRes);

        if (txtTag.getBackground() != null) {
            androidx.core.graphics.drawable.DrawableCompat.setTint(
                    txtTag.getBackground(),
                    corFundoElements
            );
        }

        txtStatus.setText(isAtivo ? "ATIVO" : "INATIVO");
        txtStatus.setBackgroundResource(isAtivo ? R.drawable.bg_status_ativo : R.drawable.bg_status_inativo);

        imgIcone.setImageResource(getIconeDoItem(item));
        imgIcone.setColorFilter(corDestaque);

        cardIcone.setCardBackgroundColor(corFundoElements);

        rootView.setAlpha(isAtivo ? 1f : 0.55f);
    }

    private boolean isItemAtivo(ItemVendaModel item) {
        if (item instanceof ProdutoModel) {
            return ((ProdutoModel) item).isStatusAtivo();
        }
        if (item instanceof ServicoModel) {
            return ((ServicoModel) item).isStatusAtivo();
        }
        return true;
    }

    private int getIconeDoItem(ItemVendaModel item) {
        if (item instanceof ProdutoModel) {
            return getIconeProdutoPorIndex(((ProdutoModel) item).getIconeIndex());
        }

        return R.drawable.ic_paid_28;
    }

    private int getIconeProdutoPorIndex(int index) {
        switch (index) {
            case 0: return R.drawable.ic_categorias_mercado_24;
            case 1: return R.drawable.ic_categorias_roupas_24;
            case 2: return R.drawable.ic_categorias_comida_24;
            case 3: return R.drawable.ic_categorias_bebidas_24;
            case 4: return R.drawable.ic_categorias_eletronicos_24;
            case 5: return R.drawable.ic_categorias_spa_24;
            case 6: return R.drawable.ic_categorias_fitness_24;
            case 7: return R.drawable.ic_categorias_geral_24;
            case 8: return R.drawable.ic_categorias_ferramentas_24;
            case 9: return R.drawable.ic_categorias_papelaria_24;
            case 10: return R.drawable.ic_categorias_casa_24;
            case 11: return R.drawable.ic_categorias_brinquedos_24;
            default: return R.drawable.ic_categorias_geral_24;
        }
    }

    class GradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag, textStatusItem;
        ImageView imageIcone;
        CardView cardIcone;

        public GradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem);
            textPreco = itemView.findViewById(R.id.textPrecoItem);
            textTipoTag = itemView.findViewById(R.id.textTipoTag);
            textStatusItem = itemView.findViewById(R.id.textStatusItem);
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVendaModel item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());
            textPreco.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco()));

            // Usa o método centralizado
            aplicarEstiloVisual(
                    item,
                    textTipoTag,
                    textStatusItem,
                    imageIcone,
                    cardIcone,
                    itemView
            );
        }
    }

    // --- VIEWHOLDER: LISTA ---
    class ListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag, textStatusItem;
        ImageView imageIcone;
        CardView cardIcone;

        public ListaViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem);
            textPreco = itemView.findViewById(R.id.textPrecoItem);
            textTipoTag = itemView.findViewById(R.id.textTipoTag);
            textStatusItem = itemView.findViewById(R.id.textStatusItem);
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVendaModel item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());
            textPreco.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco()));

            // Usa o método centralizado
            aplicarEstiloVisual(
                    item,
                    textTipoTag,
                    textStatusItem,
                    imageIcone,
                    cardIcone,
                    itemView
            );
        }
    }
}