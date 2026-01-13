package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.Categoria;

import java.util.List;

public class AdapterCategoria extends RecyclerView.Adapter<AdapterCategoria.MyViewHolder> {

    private List<Categoria> listaCategorias;
    private Context context;

    public AdapterCategoria(List<Categoria> listaCategorias, Context context) {
        this.listaCategorias = listaCategorias;
        this.context = context;
    }

    // Método para atualizar a lista quando filtrar
    public void setListaFiltrada(List<Categoria> listaFiltrada) {
        this.listaCategorias = listaFiltrada;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Categoria categoria = listaCategorias.get(position);

        holder.nome.setText(categoria.getNome());
        holder.descricao.setText(categoria.getDescricao());

        // Lógica de Status (Verde ou Vermelho)
        if (categoria.isAtiva()) {
            holder.status.setText("Ativa");
            holder.status.setTextColor(Color.parseColor("#4CAF50")); // Verde
        } else {
            holder.status.setText("Inativa");
            holder.status.setTextColor(Color.parseColor("#F44336")); // Vermelho
        }

        // Lógica do Ícone (Você precisa mapear seus ícones aqui)
        int iconRes = getIconePorIndex(categoria.getIndexIcone());
        holder.icone.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return listaCategorias.size();
    }

    // --- Mapeamento dos Ícones ---
    // --- Mapeamento dos Ícones ---
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

            // Se der algum erro, mostra o ícone Geral
            default: return R.drawable.ic_categorias_geral_24;
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nome, descricao, status;
        ImageView icone;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textNomeCategoria);
            descricao = itemView.findViewById(R.id.textDescCategoria);
            status = itemView.findViewById(R.id.textStatus);
            icone = itemView.findViewById(R.id.imageIconeCategoria);
        }
    }
}