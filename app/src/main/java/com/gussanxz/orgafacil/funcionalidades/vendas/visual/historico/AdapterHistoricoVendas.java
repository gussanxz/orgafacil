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

public class AdapterHistoricoVendas extends RecyclerView.Adapter<AdapterHistoricoVendas.HistoricoViewHolder> {

    private List<VendaModel> listaVendas;
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat formatadorData = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    private final OnVendaClickListener clickListener;
    private final OnVendaEditarListener editarListener;
    private final OnVendaAlternarStatusListener alternarStatusListener;

    public AdapterHistoricoVendas(List<VendaModel> listaVendas,
                                  OnVendaClickListener clickListener,
                                  OnVendaEditarListener editarListener,
                                  OnVendaAlternarStatusListener alternarStatusListener) {
        this.listaVendas = listaVendas;
        this.clickListener = clickListener;
        this.editarListener = editarListener;
        this.alternarStatusListener = alternarStatusListener;
    }



    public interface OnVendaClickListener {
        void onVendaClick(VendaModel venda);
    }
    public interface OnVendaEditarListener {
        void onVendaEditar(VendaModel venda);
    }

    public interface OnVendaAlternarStatusListener {
        void onVendaAlternarStatus(VendaModel venda);
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
        private final ImageButton btnEditarVenda;
        private final ImageButton btnAlternarStatus;

        public HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtVendaId = itemView.findViewById(R.id.txtVendaId);
            txtVendaData = itemView.findViewById(R.id.txtVendaData);
            txtVendaPagamento = itemView.findViewById(R.id.txtVendaPagamento);
            txtVendaQuantidade = itemView.findViewById(R.id.txtVendaQuantidade);
            txtVendaStatus = itemView.findViewById(R.id.txtVendaStatus);
            txtVendaTotal = itemView.findViewById(R.id.txtVendaTotal);
            btnEditarVenda   = itemView.findViewById(R.id.btnEditarVenda);
            btnAlternarStatus = itemView.findViewById(R.id.btnAlternarStatus);
        }

        void bind(VendaModel venda) {
            String numero = venda.getNumeroVenda() > 0
                    ? String.format(Locale.ROOT, "%07d", venda.getNumeroVenda())
                    : "---";
            txtVendaId.setText("Venda #" + numero);

            long dataExibir = venda.getDataHoraFechamentoMillis() > 0
                    ? venda.getDataHoraFechamentoMillis()
                    : venda.getDataHoraAberturaMillis();
            txtVendaData.setText(formatarData(dataExibir));
            txtVendaPagamento.setText("Pagamento: " + valorOuPadrao(venda.getFormaPagamento(), "-"));
            txtVendaQuantidade.setText(
                    venda.getQuantidadeTotal() + (venda.getQuantidadeTotal() == 1 ? " item" : " itens")
            );
            String status = valorOuPadrao(venda.getStatus(), VendaModel.STATUS_FINALIZADA);
            txtVendaStatus.setText(status);

            switch (status) {
                case VendaModel.STATUS_FINALIZADA:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // verde
                    txtVendaStatus.setBackgroundResource(R.drawable.bg_status_ativo);
                    break;
                case VendaModel.STATUS_CANCELADA:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#C62828")); // vermelho
                    txtVendaStatus.setBackgroundResource(R.drawable.bg_status_ativo);
                    break;
                default:
                    txtVendaStatus.setTextColor(android.graphics.Color.parseColor("#E65100")); // laranja
                    txtVendaStatus.setBackgroundResource(R.drawable.bg_status_ativo);
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
                // Finalizada → mostra X vermelho (cancelar)
                // Cancelada  → mostra ícone de restaurar verde
                btnAlternarStatus.setImageResource(
                        finalizada ? R.drawable.ic_delete_forever_24 : R.drawable.ic_restore_24
                );
                btnAlternarStatus.setColorFilter(
                        itemView.getContext().getColor(finalizada ? R.color.colorPrimaryDespesa
                                : R.color.colorPrimaryProventos)
                );
                btnAlternarStatus.setOnClickListener(v -> {
                    if (alternarStatusListener != null) alternarStatusListener.onVendaAlternarStatus(venda);
                });
            }


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