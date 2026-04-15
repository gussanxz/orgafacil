package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemVendaModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdapterSacolaNovaVenda extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    public interface OnSacolaActionListener {
        void onSomar(ItemSacolaVendaModel item);
        void onSubtrair(ItemSacolaVendaModel item);
        void onRemover(ItemSacolaVendaModel item);
    }

    // Lista plana mista: String = cabeçalho de categoria, ItemSacolaVendaModel = item
    private List<Object> listaFlat = new ArrayList<>();
    private final OnSacolaActionListener listener;
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterSacolaNovaVenda(List<ItemSacolaVendaModel> itens, OnSacolaActionListener listener) {
        this.listener = listener;
        listaFlat = agrupar(itens);
    }

    /** Recebe a lista plana de itens e reconstrói com cabeçalhos de categoria. */
    public void atualizarLista(List<ItemSacolaVendaModel> novaLista) {
        listaFlat = agrupar(novaLista);
        notifyDataSetChanged();
    }

    // ── Agrupamento ───────────────────────────────────────────────────────────

    private static List<Object> agrupar(List<ItemSacolaVendaModel> itens) {
        // Agrupa preservando a ordem de inserção (LinkedHashMap)
        LinkedHashMap<String, List<ItemSacolaVendaModel>> grupos = new LinkedHashMap<>();

        for (ItemSacolaVendaModel item : itens) {
            String cat = item.getCategoria();
            if (cat == null || cat.isEmpty()) cat = "Sem categoria";
            if (!grupos.containsKey(cat)) grupos.put(cat, new ArrayList<>());
            grupos.get(cat).add(item);
        }

        List<Object> flat = new ArrayList<>();
        for (Map.Entry<String, List<ItemSacolaVendaModel>> entry : grupos.entrySet()) {
            flat.add(entry.getKey());           // String → cabeçalho
            flat.addAll(entry.getValue());      // itens do grupo
        }
        return flat;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return listaFlat.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return listaFlat.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_sacola_header_categoria, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_sacola_nova_venda, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((String) listaFlat.get(position));
        } else {
            ((ItemVH) holder).bind((ItemSacolaVendaModel) listaFlat.get(position));
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView txtCategoria;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            txtCategoria = itemView.findViewById(R.id.txtHeaderCategoriaSacola);
        }

        void bind(String categoria) {
            txtCategoria.setText(categoria);
        }
    }

    class ItemVH extends RecyclerView.ViewHolder {
        final TextView     txtNome, txtTipo, txtPreco, txtQtd, txtSubtotal;
        final ImageButton  btnMenos, btnMais, btnRemover;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            txtNome     = itemView.findViewById(R.id.txtNomeItemSacola);
            txtTipo     = itemView.findViewById(R.id.txtTipoItemSacola);
            txtPreco    = itemView.findViewById(R.id.txtPrecoUnitarioSacola);
            txtQtd      = itemView.findViewById(R.id.txtQuantidadeSacola);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotalSacola);
            btnMenos    = itemView.findViewById(R.id.btnMenosQuantidade);
            btnMais     = itemView.findViewById(R.id.btnMaisQuantidade);
            btnRemover  = itemView.findViewById(R.id.btnRemoverItemSacola);
        }

        void bind(ItemSacolaVendaModel item) {
            txtNome.setText(item.getNome());
            txtTipo.setText(item.getTipo() == ItemVendaModel.TIPO_PRODUTO ? "Produto" : "Serviço");
            txtPreco.setText("Unitário: " + fmt.format(item.getPrecoUnitario() / 100.0));
            txtQtd.setText(String.valueOf(item.getQuantidade()));
            txtSubtotal.setText(fmt.format(item.getSubtotal() / 100.0));

            btnMais.setOnClickListener(v -> listener.onSomar(item));
            btnMenos.setOnClickListener(v -> listener.onSubtrair(item));
            btnRemover.setOnClickListener(v -> listener.onRemover(item));
        }
    }
}
