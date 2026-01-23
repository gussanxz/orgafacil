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

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.Categoria;

import java.util.List;

/**
 * ADAPTER: AdapterCategoriaVendas
 *
 * RESPONSABILIDADE:
 * Atuar como intermediário entre a lista de dados (Categorias) e a interface do usuário (RecyclerView).
 * Faz parte da camada de UI (User Interface).
 *
 * O QUE ELA FAZ:
 * 1. Converte objetos do tipo 'Categoria' em itens visuais na lista de vendas.
 * 2. Gerencia a exibição dinâmica de ícones, nomes e status (Ativa/Inativa).
 * 3. Notifica a Activity/Fragment sobre ações do usuário (Editar/Excluir) via Interface Callback.
 * 4. Suporta filtragem de dados em tempo real através do metodo 'setListaFiltrada'.
 */

public class AdapterItemListaCategoriasVendas extends RecyclerView.Adapter<AdapterItemListaCategoriasVendas.MyViewHolder> {

    private List<Categoria> listaCategorias;
    private final Context context;
    private final OnCategoriaActionListener listener; // 1. Variável para ouvir os cliques

    // 2. Interface para comunicação com a Activity
    public interface OnCategoriaActionListener {
        void onEditarClick(Categoria categoria);
        void onExcluirClick(Categoria categoria);
    }

    public AdapterItemListaCategoriasVendas(List<Categoria> listaCategorias, Context context, OnCategoriaActionListener listener) {
        this.listaCategorias = listaCategorias;
        this.context = context;
        this.listener = listener;
    }

    public void setListaFiltrada(List<Categoria> listaFiltrada) {
        this.listaCategorias = listaFiltrada;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vendas_lista_categorias, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Categoria categoria = listaCategorias.get(position);

        // Configuração visual (Texto e Icones)
        holder.nome.setText(categoria.getNome());
        String desc = categoria.getDescricao() == null ? "" : categoria.getDescricao();
        holder.descricao.setText(desc);

        if (categoria.isAtiva()) {
            holder.status.setText("Ativa");
            holder.status.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.status.setText("Inativa");
            holder.status.setTextColor(Color.parseColor("#9E9E9E"));
        }

        int iconRes = getIconePorIndex(categoria.getIndexIcone());
        holder.icone.setImageResource(iconRes);

        // --- 4. NOVOS EVENTOS DE CLIQUE ---

        // A. Clicar no CARD INTEIRO -> Editar
        holder.itemView.setOnClickListener(v -> {
            listener.onEditarClick(categoria);
        });

        // B. Clicar na LIXEIRA -> Excluir
        holder.btnExcluirCategoria.setOnClickListener(v -> {
            listener.onExcluirClick(categoria);
        });
    }

    @Override
    public int getItemCount() {
        return listaCategorias == null ? 0 : listaCategorias.size();
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

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nome, descricao, status;
        ImageView icone;
        ImageButton btnExcluirCategoria;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textNomeCategoria);
            descricao = itemView.findViewById(R.id.textDescCategoria);
            status = itemView.findViewById(R.id.textStatus);
            icone = itemView.findViewById(R.id.imageIconeCategoria);
            btnExcluirCategoria = itemView.findViewById(R.id.btnExcluirCategoria);
        }
    }
}
