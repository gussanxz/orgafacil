package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.contas.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.model.Movimentacao;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MovimentosAgrupadosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MovimentoItem> itens;
    private final Context context;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // Interface para avisar a Activity que o usuário quer excluir
    private final OnItemDeleteListener deleteListener;

    public interface OnItemDeleteListener {
        void onDeleteClick(Movimentacao movimentacao);
    }

    // Construtor atualizado para receber o Listener
    public MovimentosAgrupadosAdapter(Context context, List<MovimentoItem> itens, OnItemDeleteListener deleteListener) {
        this.context = context;
        this.itens = itens;
        this.deleteListener = deleteListener;
    }

    @Override
    public int getItemViewType(int position) {
        return itens.get(position).type;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MovimentoItem.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_contas_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.adapter_contas_movimentacao, parent, false);
            return new MovimentoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MovimentoItem item = itens.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof MovimentoViewHolder) {
            ((MovimentoViewHolder) holder).bind(item.movimentacao);
        }
    }

    // --------- ViewHolder do Cabeçalho ---------
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textDiaTitulo, textSaldoDia;
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiaTitulo = itemView.findViewById(R.id.textDiaTitulo);
            textSaldoDia = itemView.findViewById(R.id.textSaldoDia);
        }

        void bind(MovimentoItem item) {
            textDiaTitulo.setText(item.tituloDia);
            if (textSaldoDia != null) {
                textSaldoDia.setText("Saldo: " + currencyFormat.format(item.saldoDia));
                int color = item.saldoDia >= 0 ? Color.parseColor("#008000") : Color.parseColor("#B00020");
                textSaldoDia.setTextColor(color);
            }
        }
    }

    // --------- ViewHolder do Item ---------
    class MovimentoViewHolder extends RecyclerView.ViewHolder {

        TextView textTitulo, textCategoria, textValor, textData, textHora;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor = itemView.findViewById(R.id.textAdapterValor);
            textData = itemView.findViewById(R.id.textAdapterData);
            textHora = itemView.findViewById(R.id.textAdapterHora);
        }

        void bind(Movimentacao mov) {
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getCategoria());
            textData.setText(mov.getData());
            textHora.setText(mov.getHora());

            double valor = mov.getValor();
            if ("d".equals(mov.getTipo())) {
                textValor.setText("- " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#E53935"));
            } else {
                textValor.setText("+ " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#00D39E"));
            }

            // 1. CLIQUE SIMPLES -> EDITAR
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", mov);
                intent.putExtra("keyFirebase", mov.getKey());
                context.startActivity(intent);
            });

            // 2. CLIQUE LONGO -> EXCLUIR
            itemView.setOnLongClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(mov);
                }
                return true; // Retorna true para consumir o evento e não acionar o clique simples depois
            });
        }
    }
}