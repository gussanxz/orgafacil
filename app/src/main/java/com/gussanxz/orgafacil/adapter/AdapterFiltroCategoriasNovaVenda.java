package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.Categoria;

import java.util.List;
public class AdapterFiltroCategoriasNovaVenda extends RecyclerView.Adapter<AdapterFiltroCategoriasNovaVenda.ViewHolder> {

    private List<Categoria> listaCategorias;
    private Context context;
    private OnCategoriaSelectedListener listener;

    // Variável para saber qual item está clicado (começa no 0)
    private int posicaoSelecionada = 0;

    // Interface para avisar a Activity que mudou o filtro
    public interface OnCategoriaSelectedListener {
        void onCategoriaSelected(Categoria categoria, int position);
    }

    public AdapterFiltroCategoriasNovaVenda(List<Categoria> listaCategorias, Context context, OnCategoriaSelectedListener listener) {
        this.listaCategorias = listaCategorias;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Usei "holder.getAdapterPosition()" para garantir a posição correta no clique
        int pos = holder.getAdapterPosition();
        Categoria categoria = listaCategorias.get(pos);

        holder.txtNome.setText(categoria.getNome());

        // LÓGICA VISUAL DE SELEÇÃO
        if (pos == posicaoSelecionada) {
            // Estilo Selecionado (Fundo Escuro, Letra Branca - Igual imagem)
            holder.txtNome.setBackgroundTintList(context.getColorStateList(R.color.colorPrimary)); // Ou use Color.parseColor("#555555")
            holder.txtNome.setTextColor(Color.WHITE);
        } else {
            // Estilo Normal (Fundo Cinza Claro, Letra Cinza)
            holder.txtNome.setBackgroundTintList(context.getColorStateList(android.R.color.white)); // Ou cinza claro #EEEEEE
            holder.txtNome.setTextColor(Color.parseColor("#757575"));
        }

        // CLIQUE
        holder.itemView.setOnClickListener(v -> {
            // Atualiza a posição selecionada
            posicaoSelecionada = pos;

            // Avisa o adapter para redesenhar as cores
            notifyDataSetChanged();

            // Avisa a Activity para filtrar os produtos
            if (listener != null) {
                listener.onCategoriaSelected(categoria, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome;

        public ViewHolder(View itemView) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNomeCategoria);
        }
    }
}