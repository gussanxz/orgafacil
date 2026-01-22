package com.gussanxz.orgafacil.adapter.contas;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterCategoriaContas extends RecyclerView.Adapter<AdapterCategoriaContas.ViewHolder> {

    private final List<String> categorias;
    private final Context context;

    public AdapterCategoriaContas(List<String> categorias, Context context) {
        this.categorias = categorias;
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textCategoria;

        ViewHolder(View itemView) {
            super(itemView);
            textCategoria = itemView.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String categoria = categorias.get(position);
        holder.textCategoria.setText(categoria);

        holder.itemView.setOnClickListener(v -> {
            Intent resultado = new Intent();
            resultado.putExtra("categoriaSelecionada", categoria);
            ((Activity) context).setResult(Activity.RESULT_OK, resultado);
            ((Activity) context).finish();
        });
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }
}
