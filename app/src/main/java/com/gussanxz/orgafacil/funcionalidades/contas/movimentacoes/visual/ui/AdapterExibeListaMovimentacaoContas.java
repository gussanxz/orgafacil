package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

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
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterExibeListaMovimentacaoContas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ExibirItemListaMovimentacaoContas> itens;
    private final Context context;
    // Formatador de Data (Novo!)
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onDeleteClick(MovimentacaoModel movimentacaoModel);
        void onLongClick(MovimentacaoModel movimentacaoModel);
    }

    public AdapterExibeListaMovimentacaoContas(Context context, List<ExibirItemListaMovimentacaoContas> itens, OnItemActionListener listener) {
        this.context = context;
        this.itens = itens;
        this.listener = listener;
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
        if (viewType == ExibirItemListaMovimentacaoContas.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_contas_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.adapter_contas_item_movimentacao, parent, false);
            return new MovimentoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExibirItemListaMovimentacaoContas item = itens.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof MovimentoViewHolder) {
            ((MovimentoViewHolder) holder).bind(item.movimentacaoModel);
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

        void bind(ExibirItemListaMovimentacaoContas item) {
            textDiaTitulo.setText(item.tituloDia);
            if (textSaldoDia != null) {
                // CORREÇÃO: Converter centavos (long) para double
                double saldoReal = item.saldoDia / 100.0;
                textSaldoDia.setText("Saldo: " + currencyFormat.format(saldoReal));

                int color = item.saldoDia >= 0 ? Color.parseColor("#008000") : Color.parseColor("#B00020");
                textSaldoDia.setTextColor(color);
            }
        }
    }

    // --------- ViewHolder do Item ---------
    class MovimentoViewHolder extends RecyclerView.ViewHolder {

        TextView textTitulo, textCategoria, textValor, textData, textHora;
        View viewIndicadorCor;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor = itemView.findViewById(R.id.textAdapterValor);
            textData = itemView.findViewById(R.id.textAdapterData);
            textHora = itemView.findViewById(R.id.textAdapterHora);
            viewIndicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
        }

        void bind(MovimentacaoModel mov) {
            textTitulo.setText(mov.getDescricao());

            // CORREÇÃO: Usar o getter correto do novo Model
            textCategoria.setText(mov.getCategoria_nome());

            // CORREÇÃO: Formatar Data e Hora a partir do Timestamp
            if (mov.getData_movimentacao() != null) {
                Date date = mov.getData_movimentacao().toDate();
                textData.setText(dateFormat.format(date));
                textHora.setText(hourFormat.format(date));
            } else {
                textData.setText("--/--");
                textHora.setText("--:--");
            }

            // CORREÇÃO: Converter INT (centavos) para DOUBLE (reais) [cite: 2026-02-07]
            double valorReais = mov.getValor() / 100.0;

            // CORREÇÃO: Comparar INT com INT usando o Enum
            if (mov.getTipo() == TipoCategoriaContas.DESPESA.getId()) {
                // DESPESA
                textValor.setText("- " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#E53935"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#E53935"));
            } else {
                // RECEITA
                textValor.setText("+ " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#00D39E"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#00D39E"));
            }

            // Cliques
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", mov);
                // intent.putExtra("keyFirebase", mov.getKey()); // Se o model já tem getKey, ok. Senão remover.
                context.startActivity(intent);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(mov);
                return true;
            });
        }
    }
}