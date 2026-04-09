package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

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

    public static class TopItemVenda {
        public int posicao;
        public String nome;
        public int quantidade;
        public double valorTotal;
        public int percentual;

        public TopItemVenda(int posicao, String nome, int quantidade, double valorTotal, int percentual) {
            this.posicao = posicao; this.nome = nome;
            this.quantidade = quantidade; this.valorTotal = valorTotal; this.percentual = percentual;
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
        h.txtPosicao.setText(String.valueOf(item.posicao));
        h.txtNome.setText(item.nome);
        h.txtQtd.setText(item.quantidade + "x");
        h.txtValor.setText(fmt.format(item.valorTotal));
        h.txtPercentual.setText(item.percentual + "%");
        h.progressBar.setProgress(item.percentual);
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtPosicao, txtNome, txtQtd, txtValor, txtPercentual;
        ProgressBar progressBar;
        VH(@NonNull View itemView) {
            super(itemView);
            txtPosicao   = itemView.findViewById(R.id.txtTopVendaPosicao);
            txtNome      = itemView.findViewById(R.id.txtTopVendaNome);
            txtQtd       = itemView.findViewById(R.id.txtTopVendaQtd);
            txtValor     = itemView.findViewById(R.id.txtTopVendaValor);
            txtPercentual = itemView.findViewById(R.id.txtTopVendaPercentual);
            progressBar  = itemView.findViewById(R.id.progressTopVenda);
        }
    }
}