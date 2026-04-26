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
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterItensCatalogo extends RecyclerView.Adapter<AdapterItensCatalogo.ViewHolder> {

    public interface OnItemActionListener {
        void onItemClick(CatalogoModel item);
        void onStatusChanged(CatalogoModel item, boolean ativo);
    }

    private final List<CatalogoModel> itens;
    private final OnItemActionListener listener;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterItensCatalogo(List<CatalogoModel> itens, OnItemActionListener listener) {
        this.itens = itens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalogo_produto_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatalogoModel item = itens.get(position);

        holder.txtNome.setText(item.getNome());
        holder.txtPreco.setText(formatadorMoeda.format(item.getPreco()));
        holder.overlayInativo.setVisibility(item.isStatusAtivo() ? View.GONE : View.VISIBLE);

        holder.switchStatus.setOnCheckedChangeListener(null);
        holder.switchStatus.setChecked(item.isStatusAtivo());

        if (item.temFoto()) {
            holder.imgIcone.clearColorFilter();
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.cardIcone.setCardBackgroundColor(Color.TRANSPARENT);
            Glide.with(holder.itemView.getContext())
                    .load(item.getUrlFoto())
                    .placeholder(R.drawable.ic_camera_alt_120)
                    .centerCrop()
                    .into(holder.imgIcone);
        } else if (item.isProduto()) {
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER);
            holder.imgIcone.setImageResource(getIconeProdutoPorIndex(item.getIconeIndex()));
            holder.imgIcone.setColorFilter(Color.parseColor("#EF6C00"));
            holder.cardIcone.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        } else {
            holder.imgIcone.setScaleType(ImageView.ScaleType.CENTER);
            holder.imgIcone.setImageResource(R.drawable.ic_paid_28);
            holder.imgIcone.setColorFilter(Color.parseColor("#1565C0"));
            holder.cardIcone.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onStatusChanged(item, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardIcone;
        final ImageView imgIcone;
        final TextView txtNome;
        final TextView txtPreco;
        final SwitchMaterial switchStatus;
        final View overlayInativo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIcone = itemView.findViewById(R.id.cardIconeCatalogoItem);
            imgIcone = itemView.findViewById(R.id.imgCatalogoItem);
            txtNome = itemView.findViewById(R.id.txtNomeCatalogoItem);
            txtPreco = itemView.findViewById(R.id.txtPrecoCatalogoItem);
            switchStatus = itemView.findViewById(R.id.switchStatusItem);
            overlayInativo = itemView.findViewById(R.id.overlayItemInativo);
        }
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
}
