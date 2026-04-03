package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import com.bumptech.glide.Glide;

import java.util.List;

public class AdapterGradeCategoriasNovaVenda
        extends RecyclerView.Adapter<AdapterGradeCategoriasNovaVenda.ViewHolder> {

    public interface OnCategoriaClickListener {
        void onCategoriaClick(Categoria categoria);
    }

    private List<Categoria> lista;
    private final OnCategoriaClickListener listener;

    public AdapterGradeCategoriasNovaVenda(List<Categoria> lista,
                                           OnCategoriaClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    public void atualizarLista(List<Categoria> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nova_venda_categoria_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria categoria = lista.get(position);
        holder.txtNome.setText(categoria.getNome());

        // Ícone "Todos" usa ícone de grade; demais categorias usam label
        if ("todos".equals(categoria.getId())) {
            holder.imgIcone.setPadding(32, 32, 32, 32);
            holder.imgIcone.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
            holder.imgIcone.setColorFilter(android.graphics.Color.parseColor("#616161"));
            holder.imgIcone.setImageResource(R.drawable.ic_grid_24);
            if (holder.cardRaiz != null) holder.cardRaiz.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#F5F5F5"));
        } else if (categoria.getUrlImagem() != null && !categoria.getUrlImagem().isEmpty()) {
            holder.imgIcone.setPadding(0, 0, 0, 0);
            holder.imgIcone.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            holder.imgIcone.clearColorFilter();
            androidx.core.widget.ImageViewCompat.setImageTintList(holder.imgIcone, null);
            if (holder.cardRaiz != null) holder.cardRaiz.setCardBackgroundColor(
                    android.graphics.Color.TRANSPARENT);
            Glide.with(holder.itemView.getContext())
                    .load(categoria.getUrlImagem())
                    .placeholder(R.drawable.ic_label_24)
                    .into(holder.imgIcone); // sem circleCrop — agora é retangular
        } else {
            holder.imgIcone.setPadding(32, 32, 32, 32);
            holder.imgIcone.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
            holder.imgIcone.setColorFilter(android.graphics.Color.parseColor("#9E9E9E"));
            holder.imgIcone.setImageResource(R.drawable.ic_label_24);
            if (holder.cardRaiz != null) holder.cardRaiz.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#F5F5F5"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoriaClick(categoria);
        });
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  txtNome;
        ImageView imgIcone;
        com.google.android.material.card.MaterialCardView cardRaiz;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome  = itemView.findViewById(R.id.txtNomeCategoriaGrid);
            imgIcone = itemView.findViewById(R.id.imgIconeCategoriaGrid);
            cardRaiz = itemView.findViewById(R.id.cardCategoriaGrid);
        }
    }
}