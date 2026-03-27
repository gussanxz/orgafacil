package com.gussanxz.orgafacil.funcionalidades.vendas.visual.gestaoerelatorios;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterVendasEmAberto extends RecyclerView.Adapter<AdapterVendasEmAberto.ViewHolder> {

    public interface OnVendaActionListener {
        void onCancelar(VendaModel venda);
    }

    private List<VendaModel> listaVendas;
    private final OnVendaActionListener listener;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat formatadorData = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    public AdapterVendasEmAberto(List<VendaModel> listaVendas, OnVendaActionListener listener) {
        this.listaVendas = listaVendas;
        this.listener = listener;
    }

    public void atualizarLista(List<VendaModel> novaLista) {
        this.listaVendas = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_venda_em_aberto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(listaVendas.get(position));
    }

    @Override
    public int getItemCount() {
        return listaVendas != null ? listaVendas.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtNumero;
        private final TextView txtDataAbertura;
        private final TextView txtQtdItens;
        private final TextView txtValor;
        private final TextView btnCancelar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNumero       = itemView.findViewById(R.id.txtNumeroVendaEmAberto);
            txtDataAbertura = itemView.findViewById(R.id.txtDataAbertura);
            txtQtdItens     = itemView.findViewById(R.id.txtQtdItensEmAberto);
            txtValor        = itemView.findViewById(R.id.txtValorEmAberto);
            btnCancelar     = itemView.findViewById(R.id.btnCancelarVendaAberta);
        }

        void bind(VendaModel venda) {
            String numero = venda.getNumeroVenda() > 0
                    ? String.format(Locale.ROOT, "%07d", venda.getNumeroVenda())
                    : "---";
            txtNumero.setText("Venda #" + numero);

            txtDataAbertura.setText("Aberta em " +
                    formatadorData.format(new Date(venda.getDataHoraAberturaMillis())));

            txtQtdItens.setText(venda.getQuantidadeTotal() +
                    (venda.getQuantidadeTotal() == 1 ? " item" : " itens"));

            txtValor.setText(formatadorMoeda.format(venda.getValorTotal()));

            btnCancelar.setOnClickListener(v -> {
                if (listener != null) listener.onCancelar(venda);
            });
        }
    }
}