package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

public class AdapterHistoricoVendas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_HEADER = 0;
    private static final int TIPO_VENDA  = 1;

    private List<Object> listaItens;

    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat formatadorHora = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));

    public interface OnVendaClickListener          { void onVendaClick(VendaModel venda); }
    public interface OnVendaEditarListener         { void onVendaEditar(VendaModel venda); }
    public interface OnVendaAlternarStatusListener { void onVendaAlternarStatus(VendaModel venda); }
    public interface OnResumoDiaListener           { void onResumoDia(HeaderDiaVenda header); }

    private final OnVendaClickListener          clickListener;
    private final OnVendaEditarListener         editarListener;
    private final OnVendaAlternarStatusListener alternarStatusListener;
    private final OnResumoDiaListener           resumoDiaListener;

    public AdapterHistoricoVendas(List<Object> listaItens,
                                  OnVendaClickListener clickListener,
                                  OnVendaEditarListener editarListener,
                                  OnVendaAlternarStatusListener alternarStatusListener,
                                  OnResumoDiaListener resumoDiaListener) {
        this.listaItens             = listaItens;
        this.clickListener          = clickListener;
        this.editarListener         = editarListener;
        this.alternarStatusListener = alternarStatusListener;
        this.resumoDiaListener      = resumoDiaListener;
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
            View v = inflater.inflate(R.layout.item_historico_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_historico_venda, parent, false);
            return new VendaViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderDiaVenda) listaItens.get(position));
        } else {
            ((VendaViewHolder) holder).bind((VendaModel) listaItens.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    // ── Header ViewHolder ──────────────────────────────────────────────────
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView    txtHeaderDia;
        private final ImageButton btnResumoDia;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtHeaderDia = itemView.findViewById(R.id.txtHeaderDia);
            btnResumoDia = itemView.findViewById(R.id.btnResumoDia);
        }

        void bind(HeaderDiaVenda header) {
            txtHeaderDia.setText(header.titulo);
            if (btnResumoDia != null) {
                btnResumoDia.setOnClickListener(v -> {
                    if (resumoDiaListener != null) resumoDiaListener.onResumoDia(header);
                });
            }
        }
    }

    // ── Venda ViewHolder ───────────────────────────────────────────────────
    class VendaViewHolder extends RecyclerView.ViewHolder {
        private final TextView    txtVendaId;
        private final TextView    txtVendaData;
        private final TextView    txtVendaPagamento;
        private final TextView    txtVendaQuantidade;
        private final TextView    txtVendaStatus;
        private final TextView    txtVendaTotal;
        private final ImageButton btnEditarVenda;
        private final ImageButton btnAlternarStatus;

        VendaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtVendaId         = itemView.findViewById(R.id.txtVendaId);
            txtVendaData       = itemView.findViewById(R.id.txtVendaData);
            txtVendaPagamento  = itemView.findViewById(R.id.txtVendaPagamento);
            txtVendaQuantidade = itemView.findViewById(R.id.txtVendaQuantidade);
            txtVendaStatus     = itemView.findViewById(R.id.txtVendaStatus);
            txtVendaTotal      = itemView.findViewById(R.id.txtVendaTotal);
            btnEditarVenda     = itemView.findViewById(R.id.btnEditarVenda);
            btnAlternarStatus  = itemView.findViewById(R.id.btnAlternarStatus);
        }

        void bind(VendaModel venda) {
            String numero = venda.getNumeroVenda() > 0
                    ? String.format(Locale.ROOT, "%07d", venda.getNumeroVenda())
                    : "---";
            txtVendaId.setText("Venda #" + numero);

            long dataExibir = venda.getDataHoraFechamentoMillis() > 0
                    ? venda.getDataHoraFechamentoMillis()
                    : venda.getDataHoraAberturaMillis();
            txtVendaData.setText(formatadorHora.format(new Date(dataExibir)));

            txtVendaPagamento.setText("Pagamento: " + (venda.getFormaPagamento() != null
                    ? venda.getFormaPagamento() : "-"));

            txtVendaQuantidade.setText(venda.getQuantidadeTotal()
                    + (venda.getQuantidadeTotal() == 1 ? " item" : " itens"));

            String status = venda.getStatus() != null
                    ? venda.getStatus() : VendaModel.STATUS_FINALIZADA;
            txtVendaStatus.setText(status);
            txtVendaStatus.setBackgroundResource(R.drawable.bg_status_ativo);
            switch (status) {
                case VendaModel.STATUS_FINALIZADA:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                    break;
                case VendaModel.STATUS_CANCELADA:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#C62828"));
                    break;
                default:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#E65100"));
                    break;
            }

            txtVendaTotal.setText(formatadorMoeda.format(venda.getValorTotal()));

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onVendaClick(venda);
            });

            if (btnEditarVenda != null) {
                btnEditarVenda.setOnClickListener(v -> {
                    if (editarListener != null) editarListener.onVendaEditar(venda);
                });
            }

            if (btnAlternarStatus != null) {
                boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
                btnAlternarStatus.setImageResource(finalizada
                        ? R.drawable.ic_delete_forever_24
                        : R.drawable.ic_restore_24);
                btnAlternarStatus.setColorFilter(itemView.getContext().getColor(
                        finalizada ? R.color.colorPrimaryDespesa : R.color.colorPrimaryProventos));
                btnAlternarStatus.setOnClickListener(v -> {
                    if (alternarStatusListener != null)
                        alternarStatusListener.onVendaAlternarStatus(venda);
                });
            }
        }
    }
}