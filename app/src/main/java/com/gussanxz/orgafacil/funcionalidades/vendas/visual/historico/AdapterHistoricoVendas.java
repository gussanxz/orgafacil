package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

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

public class AdapterHistoricoVendas extends RecyclerView.Adapter<AdapterHistoricoVendas.HistoricoViewHolder> {

    private List<VendaModel> listaVendas;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat formatadorData = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    public AdapterHistoricoVendas(List<VendaModel> listaVendas) {
        this.listaVendas = listaVendas;
    }

    public void atualizarLista(List<VendaModel> novaLista) {
        this.listaVendas = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico_venda, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        holder.bind(listaVendas.get(position));
    }

    @Override
    public int getItemCount() {
        return listaVendas != null ? listaVendas.size() : 0;
    }

    class HistoricoViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtVendaId;
        private final TextView txtVendaData;
        private final TextView txtVendaPagamento;
        private final TextView txtVendaQuantidade;
        private final TextView txtVendaStatus;
        private final TextView txtVendaTotal;

        public HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtVendaId = itemView.findViewById(R.id.txtVendaId);
            txtVendaData = itemView.findViewById(R.id.txtVendaData);
            txtVendaPagamento = itemView.findViewById(R.id.txtVendaPagamento);
            txtVendaQuantidade = itemView.findViewById(R.id.txtVendaQuantidade);
            txtVendaStatus = itemView.findViewById(R.id.txtVendaStatus);
            txtVendaTotal = itemView.findViewById(R.id.txtVendaTotal);
        }

        void bind(VendaModel venda) {
            String idCurto = venda.getId() != null && venda.getId().length() > 8
                    ? venda.getId().substring(0, 8).toUpperCase(Locale.ROOT)
                    : (venda.getId() != null ? venda.getId().toUpperCase(Locale.ROOT) : "---");

            txtVendaId.setText("Venda #" + idCurto);
            txtVendaData.setText(formatarData(venda.getDataHoraMillis()));
            txtVendaPagamento.setText("Pagamento: " + valorOuPadrao(venda.getFormaPagamento(), "-"));
            txtVendaQuantidade.setText(
                    venda.getQuantidadeTotal() + (venda.getQuantidadeTotal() == 1 ? " item" : " itens")
            );
            txtVendaStatus.setText(valorOuPadrao(venda.getStatus(), "FINALIZADA"));
            txtVendaTotal.setText(formatadorMoeda.format(venda.getValorTotal()));
        }

        private String formatarData(long dataHoraMillis) {
            if (dataHoraMillis <= 0) return "-";
            return formatadorData.format(new Date(dataHoraMillis));
        }

        private String valorOuPadrao(String valor, String padrao) {
            return valor == null || valor.trim().isEmpty() ? padrao : valor;
        }
    }
}