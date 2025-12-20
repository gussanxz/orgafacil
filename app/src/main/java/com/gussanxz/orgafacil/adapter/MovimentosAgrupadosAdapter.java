package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.Movimentacao;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MovimentosAgrupadosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MovimentoItem> itens;
    private final Context context;
    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public MovimentosAgrupadosAdapter(Context context, List<MovimentoItem> itens) {
        this.context = context;
        this.itens = itens;
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
            View v = inflater.inflate(R.layout.item_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.adapter_movimentacao, parent, false);
            // use aqui o mesmo layout que você já usa hoje para uma linha de movimentação
            return new MovimentoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MovimentoItem item = itens.get(position);
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.bind(item);
        } else if (holder instanceof MovimentoViewHolder) {
            MovimentoViewHolder mvh = (MovimentoViewHolder) holder;
            mvh.bind(item.movimentacao);
        }
    }

    // --------- ViewHolder do header

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textDiaTitulo, textSaldoDia;
        NumberFormat currencyFormat =
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiaTitulo = itemView.findViewById(R.id.textDiaTitulo);
            textSaldoDia = itemView.findViewById(R.id.textSaldoDia);
        }

        void bind(MovimentoItem item) {
            textDiaTitulo.setText(item.tituloDia);

            String saldoStr = "Saldo do dia: " + currencyFormat.format(item.saldoDia);
            textSaldoDia.setText(saldoStr);

            int color = item.saldoDia >= 0 ? Color.parseColor("#008000") : Color.parseColor("#B00020");
            textSaldoDia.setTextColor(color);
        }
    }

    // --------- ViewHolder da linha de movimentação

    class MovimentoViewHolder extends RecyclerView.ViewHolder {

        TextView textDescricao, textCategoria, textValor, textDataHora;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            // ajuste os IDs para os do seu layout atual
            textDescricao = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor = itemView.findViewById(R.id.textAdapterValor);
            textDataHora = itemView.findViewById(R.id.textAdapterHora);
        }

        void bind(Movimentacao mov) {
            textDescricao.setText(mov.getDescricao());
            textCategoria.setText(mov.getCategoria());

            double valor = mov.getValor();
            if ("d".equals(mov.getTipo())) {
                textValor.setText("- " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#B00020")); // vermelho
            } else {
                textValor.setText("+ " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#008000")); // verde
            }

            String dataHora = mov.getData() + " " + mov.getHora();
            textDataHora.setText(dataHora);
        }
    }
}
