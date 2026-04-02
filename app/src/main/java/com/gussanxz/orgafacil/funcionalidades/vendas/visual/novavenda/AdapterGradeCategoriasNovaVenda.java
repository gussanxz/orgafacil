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
            holder.imgIcone.setImageResource(R.drawable.ic_grid_24);
        } else {
            holder.imgIcone.setImageResource(R.drawable.ic_label_24);
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome  = itemView.findViewById(R.id.txtNomeCategoriaGrid);
            imgIcone = itemView.findViewById(R.id.imgIconeCategoriaGrid);
        }
    }
}