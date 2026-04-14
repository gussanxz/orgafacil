package com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CaixaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterHistoricoCaixas extends RecyclerView.Adapter<AdapterHistoricoCaixas.ViewHolder> {

    private List<CaixaModel> lista;

    private final SimpleDateFormat fmtData  = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final SimpleDateFormat fmtHora  = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));
    private final NumberFormat     fmtMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterHistoricoCaixas(List<CaixaModel> lista) {
        this.lista = lista;
    }

    public void atualizar(List<CaixaModel> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico_caixa, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CaixaModel c = lista.get(position);

        boolean legado = c.isLegado();
        boolean aberto = c.isAberto();

        // Caixa legado recebe tratamento especial
        if (legado) {
            h.txtCaixaData.setText("Vendas anteriores");
            h.txtCaixaStatusBadge.setVisibility(View.GONE);
            h.txtCaixaHorarios.setText("Registradas antes do sistema de caixa");
            h.txtCaixaObservacao.setVisibility(View.VISIBLE);
            h.txtCaixaObservacao.setText("Vendas sem caixa associado");
            h.txtLancamentoTardio.setVisibility(View.GONE);

            int    qtd   = c.getQtdVendasFechamento();
            double total = c.getValorTotalFechamento();
            if (qtd > 0) {
                h.txtCaixaQtdVendas.setText(qtd + (qtd == 1 ? " venda" : " vendas"));
                h.txtCaixaTotal.setText(fmtMoeda.format(total));
            } else {
                h.txtCaixaQtdVendas.setText("—");
                h.txtCaixaTotal.setText("—");
            }
            return;
        }

        h.txtCaixaStatusBadge.setVisibility(View.VISIBLE);

        // Data (abertura)
        String dataAbertura = c.getAbertoEmMillis() > 0
                ? fmtData.format(new Date(c.getAbertoEmMillis()))
                : "—";
        h.txtCaixaData.setText(dataAbertura);

        // Badge de status
        h.txtCaixaStatusBadge.setText(aberto ? "ABERTO" : "FECHADO");
        if (aberto) {
            h.txtCaixaStatusBadge.setBackgroundResource(R.drawable.bg_status_ativo);
            h.txtCaixaStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.txtCaixaStatusBadge.setBackgroundResource(R.drawable.bg_status_finalizada);
            h.txtCaixaStatusBadge.setTextColor(Color.parseColor("#1565C0"));
        }

        // Horários
        String horaAbertura   = c.getAbertoEmMillis()  > 0 ? fmtHora.format(new Date(c.getAbertoEmMillis()))  : "—";
        String horaFechamento = c.getFechadoEmMillis() > 0 ? fmtHora.format(new Date(c.getFechadoEmMillis())) : "—";
        if (aberto) {
            h.txtCaixaHorarios.setText("Abertura: " + horaAbertura + "  →  Em aberto");
        } else {
            h.txtCaixaHorarios.setText("Abertura: " + horaAbertura + "  →  Fechamento: " + horaFechamento);
        }

        // Observação
        String obs = c.getObservacao();
        if (obs != null && !obs.isEmpty()) {
            h.txtCaixaObservacao.setVisibility(View.VISIBLE);
            h.txtCaixaObservacao.setText(obs);
        } else {
            h.txtCaixaObservacao.setVisibility(View.GONE);
        }

        // Lançamento tardio
        h.txtLancamentoTardio.setVisibility(c.isPermiteLancamentoTardio() ? View.VISIBLE : View.GONE);

        // Totais
        if (aberto) {
            h.txtCaixaQtdVendas.setText("Em andamento");
            h.txtCaixaTotal.setText("—");
        } else {
            int qtd = c.getQtdVendasFechamento();
            h.txtCaixaQtdVendas.setText(qtd + (qtd == 1 ? " venda" : " vendas"));
            h.txtCaixaTotal.setText(fmtMoeda.format(c.getValorTotalFechamento()));
        }
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCaixaData;
        TextView txtCaixaStatusBadge;
        TextView txtCaixaHorarios;
        TextView txtCaixaObservacao;
        TextView txtLancamentoTardio;
        TextView txtCaixaQtdVendas;
        TextView txtCaixaTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCaixaData          = itemView.findViewById(R.id.txtCaixaData);
            txtCaixaStatusBadge   = itemView.findViewById(R.id.txtCaixaStatusBadge);
            txtCaixaHorarios      = itemView.findViewById(R.id.txtCaixaHorarios);
            txtCaixaObservacao    = itemView.findViewById(R.id.txtCaixaObservacao);
            txtLancamentoTardio   = itemView.findViewById(R.id.txtLancamentoTardio);
            txtCaixaQtdVendas     = itemView.findViewById(R.id.txtCaixaQtdVendas);
            txtCaixaTotal         = itemView.findViewById(R.id.txtCaixaTotal);
        }
    }
}
