package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterItensComprovante extends RecyclerView.Adapter<AdapterItensComprovante.ViewHolder> {

    private final List<ItemVendaRegistradaModel> listaItens;
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterItensComprovante(List<ItemVendaRegistradaModel> listaItens) {
        this.listaItens = listaItens;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comprovante_venda, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(listaItens.get(position));
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtTipo, txtQtdUnitario, txtSubtotalItem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNome         = itemView.findViewById(R.id.txtNomeItemComprovante);
            txtTipo         = itemView.findViewById(R.id.txtTipoItemComprovante);
            txtQtdUnitario  = itemView.findViewById(R.id.txtQtdUnitarioComprovante);
            txtSubtotalItem = itemView.findViewById(R.id.txtSubtotalItemComprovante);
        }

        void bind(ItemVendaRegistradaModel item) {
            txtNome.setText(item.getNome());
            txtTipo.setText(item.getTipo() == ItemVendaModel.TIPO_PRODUTO ? "Produto" : "Serviço");
            txtQtdUnitario.setText(item.getQuantidade() + "x  " + fmt.format(item.getPrecoUnitario()));
            txtSubtotalItem.setText(fmt.format(item.getSubtotal()));
        }
    }
}