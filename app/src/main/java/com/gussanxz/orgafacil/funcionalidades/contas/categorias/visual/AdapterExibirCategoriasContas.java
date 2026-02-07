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

    // CORREÇÃO: Alterado de List<String> para List<ContasCategoriaModel>
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
            // Usando o ID padrão do Android para listas simples por enquanto
            textCategoria = itemView.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla um layout de linha simples do Android
        View item = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // CORREÇÃO: Obtém o objeto completo da lista
        ContasCategoriaModel categoria = categorias.get(position);

        // Exibe o nome da categoria no TextView
        holder.textCategoria.setText(categoria.getNome());

        // Ao clicar, devolvemos os dados para a Activity anterior (ex: EditarMovimentacaoActivity)
        holder.itemView.setOnClickListener(v -> {
            Intent resultado = new Intent();

            // Passamos o Nome para exibição na UI e o ID para salvar no Banco
            resultado.putExtra("categoriaSelecionada", categoria.getNome());
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