package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

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

public class TodosProdutosVendasAdapter
        extends RecyclerView.Adapter<TodosProdutosVendasAdapter.VH> {

    public static class ProdutoItem {
        public final String nome;
        public final int    quantidade;
        public final double valorTotal;

        public ProdutoItem(String nome, int quantidade, double valorTotal) {
            this.nome       = nome;
            this.quantidade = quantidade;
            this.valorTotal = valorTotal;
        }
    }

    private final List<ProdutoItem> lista = new ArrayList<>();
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public void atualizar(List<ProdutoItem> novaLista) {
        lista.clear();
        if (novaLista != null) lista.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produto_venda_lista, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProdutoItem item = lista.get(position);
        holder.nome.setText(item.nome);
        holder.qtd.setText(item.quantidade + "x");
        holder.valor.setText(fmt.format(item.valorTotal));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView nome, qtd, valor;
        VH(@NonNull View itemView) {
            super(itemView);
            nome  = itemView.findViewById(R.id.txtProdListaNome);
            qtd   = itemView.findViewById(R.id.txtProdListaQtd);
            valor = itemView.findViewById(R.id.txtProdListaValor);
        }
    }
}
