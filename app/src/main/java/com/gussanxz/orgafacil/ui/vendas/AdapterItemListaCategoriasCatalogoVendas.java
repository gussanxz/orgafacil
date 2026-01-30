package com.gussanxz.orgafacil.ui.vendas;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.Categoria;

import java.util.List;

public class AdapterItemListaCategoriasCatalogoVendas extends RecyclerView.Adapter<AdapterItemListaCategoriasCatalogoVendas.MyViewHolder> {

    private List<Categoria> lista;
    private final Context context;
    private final OnCategoriaActionListener listener;

    public interface OnCategoriaActionListener {
        void onEditarClick(Categoria categoria);
        void onExcluirClick(Categoria categoria);
    }

    public AdapterItemListaCategoriasCatalogoVendas(List<Categoria> lista, Context context, OnCategoriaActionListener listener) {
        this.lista = lista;
        this.context = context;
        this.listener = listener;
    }

    public void setListaFiltrada(List<Categoria> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    // Método alias para compatibilidade
    public void atualizarLista(List<Categoria> novaLista) {
        setListaFiltrada(novaLista);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vendas_lista_categorias, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Categoria categoria = lista.get(position);

        holder.textNomeCategoria.setText(categoria.getNome());
        holder.textDescCategoria.setText(categoria.getDescricao() != null ? categoria.getDescricao() : "");

        // --- LÓGICA DE VISUAL: FOTO vs ÍCONE ---
        if (categoria.getUrlImagem() != null && !categoria.getUrlImagem().isEmpty()) {
            // MODO FOTO
            holder.imgIconeCategoria.setColorFilter(null);
            holder.imgIconeCategoria.setPadding(0, 0, 0, 0);

            Glide.with(context)
                    .load(categoria.getUrlImagem())
                    .apply(new RequestOptions().transform(new CenterCrop()))
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.imgIconeCategoria);

            holder.cardIconeCategoria.setStrokeColor(Color.parseColor("#2196F3")); // Azul
            holder.cardIconeCategoria.setStrokeWidth(3);

        } else {
            // MODO ÍCONE
            holder.imgIconeCategoria.setImageResource(getIconePorIndex(categoria.getIndexIcone()));
            holder.imgIconeCategoria.setColorFilter(Color.parseColor("#9E9E9E"));

            int padding = dpToPx(8);
            holder.imgIconeCategoria.setPadding(padding, padding, padding, padding);
            holder.imgIconeCategoria.setScaleType(ImageView.ScaleType.CENTER_CROP);

            holder.cardIconeCategoria.setStrokeColor(Color.parseColor("#E0E0E0")); // Cinza
            holder.cardIconeCategoria.setStrokeWidth(2);
        }

        // --- STATUS VISUAL ---
        if (categoria.isAtiva()) {
            holder.textStatus.setText("Ativa");
            holder.textStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.textStatus.setText("Inativa");
            holder.textStatus.setTextColor(Color.parseColor("#F44336")); // Vermelho
            holder.itemView.setAlpha(0.7f);
        }

        // --- CLIQUES ---
        holder.itemView.setOnClickListener(v -> listener.onEditarClick(categoria));

        if (holder.btnExcluirCategoria != null) {
            holder.btnExcluirCategoria.setOnClickListener(v -> listener.onExcluirClick(categoria));
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textNomeCategoria, textDescCategoria, textStatus;
        ImageView imgIconeCategoria;
        MaterialCardView cardIconeCategoria;
        ImageButton btnExcluirCategoria;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textNomeCategoria = itemView.findViewById(R.id.textNomeCategoria);
            textDescCategoria = itemView.findViewById(R.id.textDescCategoria);
            textStatus = itemView.findViewById(R.id.textStatus);
            imgIconeCategoria = itemView.findViewById(R.id.imgIconeCategoria);
            cardIconeCategoria = itemView.findViewById(R.id.cardIconeCategoria);
            btnExcluirCategoria = itemView.findViewById(R.id.btnExcluirCategoria);
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int getIconePorIndex(int index) {
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
}