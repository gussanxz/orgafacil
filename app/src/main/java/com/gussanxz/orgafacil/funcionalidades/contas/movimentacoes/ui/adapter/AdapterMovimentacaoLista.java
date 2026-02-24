package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterMovimentacaoLista extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AdapterItemListaMovimentacao> itens;
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onDeleteClick(MovimentacaoModel movimentacaoModel);
        void onLongClick(MovimentacaoModel movimentacaoModel);
        void onCheckClick(MovimentacaoModel movimentacaoModel);
    }

    public AdapterMovimentacaoLista(Context context, List<AdapterItemListaMovimentacao> itens, OnItemActionListener listener) {
        this.context = context;
        this.itens = itens;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return itens.get(position).type;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == AdapterItemListaMovimentacao.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.adapter_item_header_dia_e_saldo, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.adapter_item_movimentacao, parent, false);
            return new MovimentoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdapterItemListaMovimentacao item = itens.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof MovimentoViewHolder) {
            ((MovimentoViewHolder) holder).bind(item.movimentacaoModel);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textDiaTitulo, textSaldoDia;
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiaTitulo = itemView.findViewById(R.id.textDiaTitulo);
            textSaldoDia = itemView.findViewById(R.id.textSaldoDia);
        }

        void bind(AdapterItemListaMovimentacao item) {
            textDiaTitulo.setText(item.tituloDia);
            if (textSaldoDia != null) {
                double saldoReal = item.saldoDia / 100.0;
                textSaldoDia.setText("Saldo: " + currencyFormat.format(saldoReal));

                if (item.saldoDia > 0) {
                    textSaldoDia.setTextColor(Color.parseColor("#008000"));
                } else if (item.saldoDia < 0) {
                    textSaldoDia.setTextColor(Color.parseColor("#B00020"));
                } else {
                    textSaldoDia.setTextColor(Color.parseColor("#9E9E9E"));
                }
            }
        }
    }

    class MovimentoViewHolder extends RecyclerView.ViewHolder {
        TextView textTitulo, textCategoria, textValor, textData, textHora;
        View viewIndicadorCor;
        ImageButton btnConfirmar;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor = itemView.findViewById(R.id.textAdapterValor);
            textData = itemView.findViewById(R.id.textAdapterData);
            textHora = itemView.findViewById(R.id.textAdapterHora);
            viewIndicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmarPagamento);
        }

        void bind(MovimentacaoModel mov) {
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getCategoria_nome());
            textData.setTextColor(Color.parseColor("#888888"));

            if (mov.getData_movimentacao() != null) {
                Date date = mov.getData_movimentacao().toDate();
                textData.setText(dateFormat.format(date));
                textHora.setText(hourFormat.format(date));

                if (!mov.isPago()) {

                    if (btnConfirmar != null) {
                        btnConfirmar.setVisibility(View.VISIBLE);
                        btnConfirmar.setOnClickListener(v -> {
                            if (listener != null) listener.onCheckClick(mov);
                        });
                    }

                    if (mov.estaVencida()) {
                        textData.setTextColor(Color.parseColor("#E53935"));
                    } else if (mov.venceEmBreve()) {
                        textData.setTextColor(Color.parseColor("#FF9800"));
                    }
                } else {
                    if (btnConfirmar != null) btnConfirmar.setVisibility(View.GONE);

                    // --- [NOVO] EXIBIÇÃO DA AUDITORIA (VENCIMENTO ORIGINAL) ---
                    if (mov.getData_vencimento_original() != null) {
                        String strPago = dateFormat.format(date);
                        String strVenc = dateFormat.format(mov.getData_vencimento_original().toDate());

                        // Só exibe se as datas forem diferentes (para não ficar redundante)
                        if (!strPago.equals(strVenc)) {
                            // Pega apenas os 5 primeiros caracteres (ex: 28/02) para não quebrar o layout [cite: 2025-11-10]
                            textData.setText(strPago + " (Venc: " + strVenc.substring(0, 5) + ")");
                        }
                    }
                }
            }

            // Precisão em Int respeitada! [cite: 2026-02-07]
            double valorReais = mov.getValor() / 100.0;

            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                textValor.setText("- " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#E53935"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#E53935"));
            } else {
                textValor.setText("+ " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#00D39E"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#00D39E"));
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", mov);
                context.startActivity(intent);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(mov);
                return true;
            });
        }
    }
}