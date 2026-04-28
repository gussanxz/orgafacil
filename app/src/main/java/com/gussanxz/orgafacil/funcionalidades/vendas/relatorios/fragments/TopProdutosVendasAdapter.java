package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopProdutosVendasAdapter extends RecyclerView.Adapter<TopProdutosVendasAdapter.VH> {

    private List<TopItemVenda> lista = new ArrayList<>();
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private static final int[] CORES_RANK = {
            Color.parseColor("#D9A520"),
            Color.parseColor("#8B96A8"),
            Color.parseColor("#B8753A"),
            Color.parseColor("#6B7890"),
            Color.parseColor("#6B7890"),
    };

    public static class TopItemVenda {
        public int posicao;
        public String nome;
        public int quantidade;
        public double valorTotal;
        public int percentual;

        public TopItemVenda(int posicao, String nome, int quantidade, double valorTotal, int percentual) {
            this.posicao = posicao;
            this.nome = nome;
            this.quantidade = quantidade;
            this.valorTotal = valorTotal;
            this.percentual = percentual;
        }
    }

    public void atualizar(List<TopItemVenda> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_produto_venda, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TopItemVenda item = lista.get(position);
        int corRank = position < CORES_RANK.length ? CORES_RANK[position] : CORES_RANK[CORES_RANK.length - 1];

        h.txtRank.setText("#" + item.posicao);
        h.txtRank.setTextColor(corRank);
        h.txtRank.setBackgroundResource(backgroundRank(position));
        h.txtNome.setText(item.nome);
        h.txtValor.setText(fmt.format(item.valorTotal));
        h.txtQtd.setText(item.quantidade + "x vendidos");
        h.progressValor.setProgress(Math.max(4, Math.min(100, item.percentual)));
    }

    private int backgroundRank(int position) {
        if (position == 0) return R.drawable.bg_rank_gold;
        if (position == 1) return R.drawable.bg_rank_silver;
        if (position == 2) return R.drawable.bg_rank_bronze;
        return R.drawable.bg_rank_default;
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtRank, txtNome, txtQtd, txtValor;
        ProgressBar progressValor;

        VH(@NonNull View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txtTopVendaRank);
            txtNome = itemView.findViewById(R.id.txtTopVendaNome);
            txtQtd = itemView.findViewById(R.id.txtTopVendaQtd);
            txtValor = itemView.findViewById(R.id.txtTopVendaValor);
            progressValor = itemView.findViewById(R.id.progressTopVendaValor);
        }
    }
}
