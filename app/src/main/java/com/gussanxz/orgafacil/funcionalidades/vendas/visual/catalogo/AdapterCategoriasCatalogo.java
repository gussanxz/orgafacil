package com.gussanxz.orgafacil.funcionalidades.vendas.visual.catalogo;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.List;

public class AdapterCategoriasCatalogo extends RecyclerView.Adapter<AdapterCategoriasCatalogo.ViewHolder> {

    public interface OnCategoriaActionListener {
        void onCategoriaClick(Categoria categoria);
        void onStatusChanged(Categoria categoria, boolean ativa);
    }

    private final List<Categoria> categorias;
    private final OnCategoriaActionListener listener;

    public AdapterCategoriasCatalogo(List<Categoria> categorias,
                                     OnCategoriaActionListener listener) {
        this.categorias = categorias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalogo_categoria_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria categoria = categorias.get(position);
        boolean todosProdutos = CatalogoActivity.ID_TODOS_PRODUTOS.equals(categoria.getId());

        holder.txtNome.setText(categoria.getNome());
        holder.switchStatus.setOnCheckedChangeListener(null);
        holder.switchStatus.setVisibility(todosProdutos ? View.GONE : View.VISIBLE);
        holder.switchStatus.setChecked(categoria.isAtiva());
        holder.overlayInativa.setVisibility(!todosProdutos && !categoria.isAtiva() ? View.VISIBLE : View.GONE);

        if (todosProdutos) {
            Glide.with(holder.itemView.getContext()).clear(holder.imgIcone);
            holder.imgIcone.setImageDrawable(null);
            holder.imgIcone.setPadding(32, 32, 32, 32);
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgIcone.setImageResource(R.drawable.ic_grid_24);
            holder.imgIcone.setColorFilter(Color.parseColor("#616161"));
            holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        } else if (categoria.getUrlImagem() != null && !categoria.getUrlImagem().isEmpty()) {
            Glide.with(holder.itemView.getContext()).clear(holder.imgIcone);
            holder.imgIcone.setPadding(0, 0, 0, 0);
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imgIcone.clearColorFilter();
            holder.card.setCardBackgroundColor(Color.TRANSPARENT);
            Glide.with(holder.itemView.getContext())
                    .load(categoria.getUrlImagem())
                    .placeholder(R.drawable.ic_label_24)
                    .centerCrop()
                    .into(holder.imgIcone);
        } else {
            int corIcone = corIconeCategoria(categoria);
            Glide.with(holder.itemView.getContext()).clear(holder.imgIcone);
            holder.imgIcone.setImageDrawable(null);
            holder.imgIcone.setPadding(32, 32, 32, 32);
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgIcone.setImageResource(getIconePorIndex(categoria.getIndexIcone()));
            holder.imgIcone.setColorFilter(corIcone);
            holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoriaClick(categoria);
        });

        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onStatusChanged(categoria, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    private int corIconeCategoria(Categoria categoria) {
        String cor = categoria.getCorIcone();
        if (cor == null || cor.trim().isEmpty()) return Color.parseColor("#9E9E9E");
        try {
            return Color.parseColor(cor);
        } catch (IllegalArgumentException e) {
            return Color.parseColor("#9E9E9E");
        }
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imgIcone;
        final TextView txtNome;
        final SwitchMaterial switchStatus;
        final View overlayInativa;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardCategoriaCatalogo);
            imgIcone = itemView.findViewById(R.id.imgIconeCategoriaCatalogo);
            txtNome = itemView.findViewById(R.id.txtNomeCategoriaCatalogo);
            switchStatus = itemView.findViewById(R.id.switchStatusCategoria);
            overlayInativa = itemView.findViewById(R.id.overlayCategoriaInativa);
        }
    }
}
