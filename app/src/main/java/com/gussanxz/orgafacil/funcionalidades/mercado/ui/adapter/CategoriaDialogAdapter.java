package com.gussanxz.orgafacil.funcionalidades.mercado.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;

import java.util.List;

public class CategoriaDialogAdapter extends RecyclerView.Adapter<CategoriaDialogAdapter.ViewHolder> {

    public interface OnCategoriaClickListener {
        void onCategoriaClick(String categoria);
    }

    private final List<String> categorias;
    private final OnCategoriaClickListener listener;

    public CategoriaDialogAdapter(List<String> categorias, OnCategoriaClickListener listener) {
        this.categorias = categorias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String categoria = categorias.get(position);
        holder.textNome.setText(categoria);

        holder.itemView.setOnClickListener(v -> listener.onCategoriaClick(categoria));
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNome;

        ViewHolder(View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeCategoriaDialog);
        }
    }
}