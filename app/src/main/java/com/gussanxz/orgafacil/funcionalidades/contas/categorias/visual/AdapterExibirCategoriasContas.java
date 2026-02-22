package com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;

import java.util.List;

/**
 * AdapterExibirCategoriasContas
 * Responsável por exibir a lista de objetos ContasCategoriaModel.
 */
public class AdapterExibirCategoriasContas extends RecyclerView.Adapter<AdapterExibirCategoriasContas.ViewHolder> {

    private final List<ContasCategoriaModel> categorias;
    private final Context context;

    public AdapterExibirCategoriasContas(List<ContasCategoriaModel> categorias, Context context) {
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
        ContasCategoriaModel categoria = categorias.get(position);

        // [ATUALIZADO]: Acessamos o grupo VISUAL para pegar o nome
        // O helper getNome() funcionaria, mas ser explícito é melhor para manutenção
        String nomeExibicao = "Sem Nome";
        if (categoria.getVisual() != null) {
            nomeExibicao = categoria.getVisual().getNome();
        }

        holder.textCategoria.setText(nomeExibicao);

        // Configura o clique
        String finalNomeExibicao = nomeExibicao;
        holder.itemView.setOnClickListener(v -> {
            Intent resultado = new Intent();

            // Retorna o Nome (do Visual) e o ID (da Raiz)
            resultado.putExtra("categoriaSelecionada", finalNomeExibicao);
            resultado.putExtra("categoriaId", categoria.getId());

            if (context instanceof Activity) {
                ((Activity) context).setResult(Activity.RESULT_OK, resultado);
                ((Activity) context).finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }
}