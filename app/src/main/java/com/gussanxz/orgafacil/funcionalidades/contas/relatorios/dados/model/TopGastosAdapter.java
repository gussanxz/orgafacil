package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import java.util.Locale;

public class TopGastosAdapter extends RecyclerView.Adapter<TopGastosAdapter.ViewHolder> {

    private List<TopGastoDTO> lista = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TopGastoDTO item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

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

        // Controle de formatação de Porcentagem para evitar exibição de 0,0%
        if (item.getPercentual() > 0 && item.getPercentual() < 0.1) {
            holder.textPercentual.setText(String.format(Locale.getDefault(), "<%.1f%%", 0.1));
        } else {
            holder.textPercentual.setText(String.format(Locale.getDefault(), "%.1f%%", item.getPercentual()));
        }

        // O dinheiro continua como long (inteiro em centavos) para máxima precisão
        holder.textValorGasto.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(item.getValorTotal())));

        // ProgressBar exige int, então arredondamos a exibição visual da barra
        holder.progressPesoGasto.setProgress((int) item.getPercentual());

        // Controle de Cores e Visibilidade da %
        if (item.isDespesa()) {
            holder.textValorGasto.setTextColor(Color.parseColor("#E53935")); // Vermelho
            holder.progressPesoGasto.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
            holder.textPercentual.setVisibility(View.VISIBLE); // Mostra % nas Despesas
        } else {
            holder.textValorGasto.setTextColor(Color.parseColor("#4CAF50")); // Verde
            holder.progressPesoGasto.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            holder.textPercentual.setVisibility(View.GONE); // Esconde % nas Receitas
        }

        // Aplica o clique na linha inteira
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
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