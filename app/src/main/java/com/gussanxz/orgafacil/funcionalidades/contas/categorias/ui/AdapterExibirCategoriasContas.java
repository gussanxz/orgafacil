package com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;

import java.util.List;

/**
 * AdapterExibirCategoriasContas
 * Responsável por exibir a lista de objetos ContasCategoriaModel.
 */
public class AdapterExibirCategoriasContas
        extends RecyclerView.Adapter<AdapterExibirCategoriasContas.ViewHolder> {

    private final List<ContasCategoriaModel> categorias;
    private final SelecionarCategoriaContasActivity activity;

    public AdapterExibirCategoriasContas(List<ContasCategoriaModel> categorias,
                                         SelecionarCategoriaContasActivity activity) {
        this.categorias = categorias;
        this.activity = activity;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textCategoria;
        android.widget.ImageView btnEditar;

        ViewHolder(View itemView) {
            super(itemView);
            textCategoria = itemView.findViewById(R.id.textCategoria);
            btnEditar = itemView.findViewById(R.id.btnEditarCategoria);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {

        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria_custom, parent, false);

        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {

        ContasCategoriaModel categoria = categorias.get(position);

        String nomeExibicao = "Sem Nome";
        if (categoria.getVisual() != null &&
                categoria.getVisual().getNome() != null) {
            nomeExibicao = categoria.getVisual().getNome();
        }

        holder.textCategoria.setText(nomeExibicao);

        // Clique normal → selecionar categoria
        String finalNome = nomeExibicao;
        holder.itemView.setOnClickListener(v -> {
            Intent resultado = new Intent();
            resultado.putExtra("categoriaSelecionada", finalNome);
            resultado.putExtra("categoriaId", categoria.getId());
            activity.setResult(Activity.RESULT_OK, resultado);
            activity.finish();
        });

        // LONG PRESS → excluir
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(activity)
                    .setTitle("Excluir Categoria")
                    .setMessage("Deseja excluir \"" + finalNome + "\"?")
                    .setPositiveButton("Excluir", (d, w) ->
                            activity.excluirCategoria(holder.getAdapterPosition(), categoria))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        });

        // BOTÃO LÁPIS → CHAMA O POPUP DE EDITAR CATEGORIA
        holder.btnEditar.setOnClickListener(v ->
                activity.mostrarDialogEditarCategoria(categoria));
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }
}