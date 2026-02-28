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
import java.util.ArrayList;
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

        /**
         * Chamado quando o usuário desliza o HEADER do dia para a esquerda.
         * Recebe a data do dia (para exibição) e a lista de movimentações daquele dia.
         */
        void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movimentacoesDoDia);
    }

    public AdapterMovimentacaoLista(Context context, List<AdapterItemListaMovimentacao> itens, OnItemActionListener listener) {
        this.context = context;
        this.itens = itens;
        this.listener = listener;
    }

    public List<AdapterItemListaMovimentacao> getItens() {
        return itens;
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

    /**
     * Coleta todas as movimentações que pertencem ao mesmo grupo de data do header na posição informada.
     * Percorre os itens seguintes até encontrar outro header ou o fim da lista.
     */
    public List<MovimentacaoModel> getMovimentacoesDoDia(int headerPosition) {
        List<MovimentacaoModel> lista = new ArrayList<>();
        int next = headerPosition + 1;
        while (next < itens.size() && itens.get(next).type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
            lista.add(itens.get(next).movimentacaoModel);
            next++;
        }
        return lista;
    }

    // =========================================================================
    // HEADER VIEW HOLDER
    // =========================================================================

    class HeaderViewHolder extends RecyclerView.ViewHolder {
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

    // =========================================================================
    // MOVIMENTO VIEW HOLDER
    // =========================================================================

    class MovimentoViewHolder extends RecyclerView.ViewHolder {
        TextView textTitulo, textCategoria, textValor, textData, textHora, textSeparador;
        View viewIndicadorCor;
        ImageButton btnConfirmar;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo    = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor     = itemView.findViewById(R.id.textAdapterValor);
            textData      = itemView.findViewById(R.id.textAdapterData);
            textHora      = itemView.findViewById(R.id.textAdapterHora);
            textSeparador = itemView.findViewById(R.id.textAdapterSeparador);
            viewIndicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
            btnConfirmar  = itemView.findViewById(R.id.btnConfirmarPagamento);
        }

        void bind(MovimentacaoModel mov) {

            // — Título e categoria —
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getTotal_parcelas() > 1
                    ? "🔁 " + mov.getCategoria_nome()
                    : mov.getCategoria_nome());

            // — Valor com sinal e cor —
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

            // — Cores padrão de data/hora —
            textData.setTextColor(Color.parseColor("#888888"));
            textHora.setTextColor(Color.parseColor("#888888"));

            // — Data e hora —
            if (mov.getData_movimentacao() != null) {
                Date date = mov.getData_movimentacao().toDate();
                textData.setText(dateFormat.format(date));
                textHora.setText(hourFormat.format(date));

                if (!mov.isPago()) {
                    // PENDENTE: esconde hora e separador
                    textHora.setVisibility(View.GONE);
                    textSeparador.setVisibility(View.GONE);

                    // Botão de confirmação com animação de feedback
                    if (btnConfirmar != null) {
                        btnConfirmar.setVisibility(View.VISIBLE);
                        btnConfirmar.setOnClickListener(v ->
                                v.animate()
                                        .scaleX(0.8f).scaleY(0.8f).setDuration(80)
                                        .withEndAction(() ->
                                                v.animate().scaleX(1f).scaleY(1f).setDuration(80)
                                                        .withEndAction(() -> {
                                                            if (listener != null) listener.onCheckClick(mov);
                                                        }).start()
                                        ).start()
                        );
                    }

                    // Cor de urgência na data
                    aplicarCorData(textData, mov);

                } else {
                    // PAGO: mostra hora da transação
                    textHora.setVisibility(View.VISIBLE);
                    textSeparador.setVisibility(View.VISIBLE);

                    if (btnConfirmar != null) btnConfirmar.setVisibility(View.GONE);

                    // Se pago em data diferente do vencimento, mostra ambas
                    if (mov.getData_vencimento_original() != null) {
                        String strPago = dateFormat.format(date);
                        String strVenc = dateFormat.format(mov.getData_vencimento_original().toDate());
                        if (!strPago.equals(strVenc)) {
                            textData.setText(strPago + " (Venc: " + strVenc.substring(0, 5) + ")");
                        }
                    }
                }
            }

            // — Cliques no item —
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

        // — Cor de urgência da data (apenas para pendentes) —
        private void aplicarCorData(TextView txtData, MovimentacaoModel mov) {
            if (mov.isPago()) {
                txtData.setTextColor(Color.parseColor("#9E9E9E"));
                return;
            }
            if (mov.getData_movimentacao() == null) return;

            int diasRestantes = mov.diasParaVencimento();

            if (mov.estaVencida() || diasRestantes < 0) {
                txtData.setTextColor(Color.parseColor("#FFC107")); // amarelo — vencida
            } else if (diasRestantes <= 3) {
                txtData.setTextColor(Color.parseColor("#FF7043")); // laranja — vencendo
            } else {
                txtData.setTextColor(Color.parseColor("#1E88E5")); // azul — tranquilo
            }
        }
    }
}
// NOTE: Add this getter to AdapterMovimentacaoLista (inside the class, before the last closing brace):
//    public List<AdapterItemListaMovimentacao> getItens() { return itens; }