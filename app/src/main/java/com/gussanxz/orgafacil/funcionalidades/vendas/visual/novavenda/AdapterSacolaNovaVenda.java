package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterSacolaNovaVenda extends RecyclerView.Adapter<AdapterSacolaNovaVenda.SacolaViewHolder> {

    public interface OnSacolaActionListener {
        void onSomar(ItemSacolaVendaModel item);
        void onSubtrair(ItemSacolaVendaModel item);
        void onRemover(ItemSacolaVendaModel item);
    }

    private List<ItemSacolaVendaModel> listaItens;
    private final OnSacolaActionListener listener;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterSacolaNovaVenda(List<ItemSacolaVendaModel> listaItens, OnSacolaActionListener listener) {
        this.listaItens = listaItens;
        this.listener = listener;
    }

    public void atualizarLista(List<ItemSacolaVendaModel> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SacolaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sacola_nova_venda, parent, false);
        return new SacolaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SacolaViewHolder holder, int position) {
        holder.bind(listaItens.get(position));
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    class SacolaViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtNomeItem;
        private final TextView txtTipoItem;
        private final TextView txtPrecoUnitario;
        private final TextView txtQuantidade;
        private final TextView txtSubtotal;
        private final ImageButton btnMenos;
        private final ImageButton btnMais;
        private final ImageButton btnRemover;

        public SacolaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNomeItem = itemView.findViewById(R.id.txtNomeItemSacola);
            txtTipoItem = itemView.findViewById(R.id.txtTipoItemSacola);
            txtPrecoUnitario = itemView.findViewById(R.id.txtPrecoUnitarioSacola);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidadeSacola);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotalSacola);
            btnMenos = itemView.findViewById(R.id.btnMenosQuantidade);
            btnMais = itemView.findViewById(R.id.btnMaisQuantidade);
            btnRemover = itemView.findViewById(R.id.btnRemoverItemSacola);
        }

        void bind(ItemSacolaVendaModel item) {
            txtNomeItem.setText(item.getNome());
            txtTipoItem.setText(
                    item.getTipo() == ItemVendaModel.TIPO_PRODUTO ? "Produto" : "Serviço"
            );
            txtPrecoUnitario.setText("Unitário: " + formatadorMoeda.format(item.getPrecoUnitario()));
            txtQuantidade.setText(String.valueOf(item.getQuantidade()));
            txtSubtotal.setText(formatadorMoeda.format(item.getSubtotal()));

            btnMais.setOnClickListener(v -> listener.onSomar(item));
            btnMenos.setOnClickListener(v -> listener.onSubtrair(item));
            btnRemover.setOnClickListener(v -> listener.onRemover(item));
        }
    }
}