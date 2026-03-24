package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterResumoFechamentoVenda extends RecyclerView.Adapter<AdapterResumoFechamentoVenda.ResumoViewHolder> {

    private List<ItemSacolaVendaModel> listaItens;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterResumoFechamentoVenda(List<ItemSacolaVendaModel> listaItens) {
        this.listaItens = listaItens;
    }

    public void atualizarLista(List<ItemSacolaVendaModel> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResumoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resumo_fechamento_venda, parent, false);
        return new ResumoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResumoViewHolder holder, int position) {
        holder.bind(listaItens.get(position));
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    class ResumoViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtNomeItem;
        private final TextView txtTipoItem;
        private final TextView txtQuantidadeItem;
        private final TextView txtSubtotalItem;

        public ResumoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNomeItem = itemView.findViewById(R.id.txtNomeResumoItem);
            txtTipoItem = itemView.findViewById(R.id.txtTipoResumoItem);
            txtQuantidadeItem = itemView.findViewById(R.id.txtQuantidadeResumoItem);
            txtSubtotalItem = itemView.findViewById(R.id.txtSubtotalResumoItem);
        }

        void bind(ItemSacolaVendaModel item) {
            txtNomeItem.setText(item.getNome());
            txtTipoItem.setText(
                    item.getTipo() == ItemVendaModel.TIPO_PRODUTO ? "Produto" : "Serviço"
            );
            txtQuantidadeItem.setText("Qtd: " + item.getQuantidade());
            txtSubtotalItem.setText(formatadorMoeda.format(item.getSubtotal()));
        }
    }
}