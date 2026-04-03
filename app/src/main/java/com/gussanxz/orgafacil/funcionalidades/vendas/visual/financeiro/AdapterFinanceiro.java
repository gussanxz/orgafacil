package com.gussanxz.orgafacil.funcionalidades.vendas.visual.financeiro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HeaderDiaVenda;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterFinanceiro extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_HEADER = 0;
    private static final int TIPO_VENDA  = 1;

    private List<Object> listaItens;

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat fmtHora = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));

    public AdapterFinanceiro(List<Object> listaItens) {
        this.listaItens = listaItens;
    }

    public void atualizarLista(List<Object> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return listaItens.get(position) instanceof HeaderDiaVenda ? TIPO_HEADER : TIPO_VENDA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_HEADER) {
            View v = inflater.inflate(R.layout.item_financeiro_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_financeiro_venda, parent, false);
            return new VendaViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder)
            ((HeaderViewHolder) holder).bind((HeaderDiaVenda) listaItens.get(position));
        else
            ((VendaViewHolder) holder).bind((VendaModel) listaItens.get(position));
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    // ── Header ViewHolder ──────────────────────────────────────────
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDia, txtTotalDia;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDia      = itemView.findViewById(R.id.txtFinHeaderDia);
            txtTotalDia = itemView.findViewById(R.id.txtFinHeaderTotal);
        }

        void bind(HeaderDiaVenda header) {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            txtDia.setText(header.titulo);
            txtTotalDia.setText(fmt.format(header.totalDia));
        }
    }

    // ── Venda ViewHolder ───────────────────────────────────────────
    class VendaViewHolder extends RecyclerView.ViewHolder {
        final TextView txtNumero, txtHora, txtPagamento, txtStatus, txtTotal;

        VendaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNumero    = itemView.findViewById(R.id.txtFinVendaNumero);
            txtHora      = itemView.findViewById(R.id.txtFinVendaHora);
            txtPagamento = itemView.findViewById(R.id.txtFinVendaPagamento);
            txtStatus    = itemView.findViewById(R.id.txtFinVendaStatus);
            txtTotal     = itemView.findViewById(R.id.txtFinVendaTotal);
        }

        void bind(VendaModel venda) {
            txtNumero.setText(venda.getNumeroVenda() > 0
                    ? String.format(Locale.ROOT, "Venda #%07d", venda.getNumeroVenda())
                    : "Venda #---");

            long ts = venda.getDataHoraFechamentoMillis() > 0
                    ? venda.getDataHoraFechamentoMillis()
                    : venda.getDataHoraAberturaMillis();
            txtHora.setText(fmtHora.format(new Date(ts)));

            txtPagamento.setText(venda.getFormaPagamento() != null
                    ? venda.getFormaPagamento() : "-");

            boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
            txtStatus.setText(venda.getStatus());
            txtStatus.setBackgroundResource(finalizada
                    ? R.drawable.bg_status_finalizada
                    : R.drawable.bg_status_cancelada);
            txtStatus.setTextColor(android.graphics.Color.parseColor(
                    finalizada ? "#2E7D32" : "#C62828"));

            txtTotal.setText(fmt.format(venda.getValorTotal()));
            txtTotal.setTextColor(android.graphics.Color.parseColor(
                    finalizada ? "#1B5E20" : "#9E9E9E"));
        }
    }
}