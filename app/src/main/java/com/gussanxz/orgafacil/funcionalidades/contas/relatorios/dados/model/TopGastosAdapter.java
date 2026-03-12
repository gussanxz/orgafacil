package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.util.ArrayList;
import java.util.List;

public class TopGastosAdapter extends RecyclerView.Adapter<TopGastosAdapter.ViewHolder> {

    private List<TopGastoDTO> lista = new ArrayList<>();

    public void atualizarLista(List<TopGastoDTO> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_gasto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopGastoDTO item = lista.get(position);

        holder.textPosicaoRanking.setText(String.valueOf(position + 1));
        holder.textNomeCategoria.setText(item.getNomeCategoria());
        holder.textPercentual.setText(item.getPercentual() + "%");

        // Conversão do dinheiro usando seu helper!
        holder.textValorGasto.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(item.getValorTotal())));

        // Animação/Exibição da barra de progresso
        holder.progressPesoGasto.setProgress(item.getPercentual());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textPosicaoRanking, textNomeCategoria, textPercentual, textValorGasto;
        ProgressBar progressPesoGasto;

        ViewHolder(View itemView) {
            super(itemView);
            textPosicaoRanking = itemView.findViewById(R.id.textPosicaoRanking);
            textNomeCategoria = itemView.findViewById(R.id.textNomeCategoria);
            textPercentual = itemView.findViewById(R.id.textPercentual);
            textValorGasto = itemView.findViewById(R.id.textValorGasto);
            progressPesoGasto = itemView.findViewById(R.id.progressPesoGasto);
        }
    }
}