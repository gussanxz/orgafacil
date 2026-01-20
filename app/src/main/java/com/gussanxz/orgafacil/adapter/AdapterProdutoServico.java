package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.ItemVenda;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterProdutoServico extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LISTA = 1;
    private static final int TYPE_GRADE = 2;

    private List<ItemVenda> listaItens;
    private boolean isGridMode = false;
    private final OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(ItemVenda item);
    }

    public AdapterProdutoServico(List<ItemVenda> listaItens, OnItemClickListener listener) {
        this.listaItens = listaItens;
        this.listener = listener;
    }

    public void setModoGrade(boolean ativarGrade) {
        this.isGridMode = ativarGrade;
        notifyDataSetChanged();
    }

    public void atualizarLista(List<ItemVenda> novaLista) {
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
        ItemVenda item = listaItens.get(position);
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

    // --- VIEWHOLDER PARA MODO GRADE (CARD QUADRADO) ---
    class GradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag;
        ImageView imageIcone;
        CardView cardIcone;

        public GradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem); // Categoria
            textPreco = itemView.findViewById(R.id.textPrecoItem); // Onde era status
            textTipoTag = itemView.findViewById(R.id.textTipoTag); // Onde era o botão excluir
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVenda item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());
            String precoStr = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco());
            textPreco.setText(precoStr);

            // Lógica para aplicar o Background e Cores
            if (item.getTipo() == ItemVenda.TIPO_PRODUTO) {
                // Configuração PRODUTO (Azul)
                textTipoTag.setText("PRODUTO");
                textTipoTag.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                textTipoTag.setBackgroundResource(R.drawable.bg_tag_produto); // <--- AQUI APLICA O BG AZUL

                imageIcone.setImageResource(R.drawable.ic_categorias_mercado_24);
                imageIcone.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                cardIcone.setCardBackgroundColor(Color.parseColor("#E3F2FD"));

            } else {
                // Configuração SERVIÇO (Laranja)
                textTipoTag.setText("SERVIÇO");
                textTipoTag.setTextColor(ContextCompat.getColor(context, R.color.colorAccentProventos));
                textTipoTag.setBackgroundResource(R.drawable.bg_tag_servico); // <--- AQUI APLICA O BG LARANJA

                imageIcone.setImageResource(R.drawable.ic_categorias_ferramentas_24);
                imageIcone.setColorFilter(ContextCompat.getColor(context, R.color.colorAccentProventos));
                cardIcone.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            }
        }
    }

    // --- VIEWHOLDER PARA MODO LISTA (CARD HORIZONTAL) ---
    class ListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag;
        ImageView imageIcone;
        CardView cardIcone;

        public ListaViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem);
            textPreco = itemView.findViewById(R.id.textPrecoItem);
            textTipoTag = itemView.findViewById(R.id.textTipoTag);
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVenda item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());

            String precoStr = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco());
            textPreco.setText(precoStr);

            if (item.getTipo() == ItemVenda.TIPO_PRODUTO) {
                textTipoTag.setText("PRODUTO");
                textTipoTag.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                imageIcone.setImageResource(R.drawable.ic_categorias_mercado_24);
                imageIcone.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                cardIcone.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                textTipoTag.setText("SERVIÇO");
                textTipoTag.setTextColor(ContextCompat.getColor(context, R.color.colorAccentProventos));
                imageIcone.setImageResource(R.drawable.ic_categorias_ferramentas_24);
                imageIcone.setColorFilter(ContextCompat.getColor(context, R.color.colorAccentProventos));
                cardIcone.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            }
        }
    }
}