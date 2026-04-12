package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // Cores de rank: ouro, prata, bronze, cinza claro (4º e 5º)
    private static final int[] CORES_RANK = {
            Color.parseColor("#FFC107"),  // 1º ouro
            Color.parseColor("#9E9E9E"),  // 2º prata
            Color.parseColor("#A1887F"),  // 3º bronze
            Color.parseColor("#BDBDBD"),  // 4º
            Color.parseColor("#BDBDBD"),  // 5º
    };

    public static class TopItemVenda {
        public int posicao;
        public String nome;
        public int quantidade;
        public double valorTotal;

        public TopItemVenda(int posicao, String nome, int quantidade, double valorTotal, int percentual) {
            this.posicao = posicao; this.nome = nome;
            this.quantidade = quantidade; this.valorTotal = valorTotal;
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
        String prefixo = "#" + item.posicao + "  ";
        SpannableString ss = new SpannableString(prefixo + item.nome);
        ss.setSpan(new ForegroundColorSpan(corRank), 0, prefixo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, prefixo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new RelativeSizeSpan(0.85f), 0, prefixo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        h.txtNome.setText(ss);

        h.txtQtd.setText(item.quantidade + "x");
        h.txtValor.setText(fmt.format(item.valorTotal));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtNome, txtQtd, txtValor;
        VH(@NonNull View itemView) {
            super(itemView);
            txtNome  = itemView.findViewById(R.id.txtTopVendaNome);
            txtQtd   = itemView.findViewById(R.id.txtTopVendaQtd);
            txtValor = itemView.findViewById(R.id.txtTopVendaValor);
        }
    }
}