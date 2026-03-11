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
import com.gussanxz.orgafacil.util_helper.DateHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import java.util.Objects;

public class AdapterMovimentacaoLista extends ListAdapter<AdapterItemListaMovimentacao, RecyclerView.ViewHolder> {

    // A lista local 'itens' foi removida, o ListAdapter gerencia o estado da lista em background para nós.
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DateHelper.FORMATO_EXIBICAO, new Locale("pt", "BR"));
    private final SimpleDateFormat hourFormat = new SimpleDateFormat(DateHelper.FORMATO_HORA, new Locale("pt", "BR"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onDeleteClick(MovimentacaoModel movimentacaoModel);
        void onLongClick(MovimentacaoModel movimentacaoModel);
        void onCheckClick(MovimentacaoModel movimentacaoModel);

        /**
         * Chamado quando o usuário desliza o HEADER do dia para a esquerda.
         *
         * @param dataDia    string da data do grupo ("dd/MM/yyyy")
         * @param movsDoDia  lista de movimentações daquele dia
         */
        void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia);
    }

    public AdapterMovimentacaoLista(Context context, OnItemActionListener listener) {
        super(new MovimentacaoDiffCallback()); // 2. Passamos o comparador no construtor
        this.context  = context;
        this.listener = listener;
    }

    public List<AdapterItemListaMovimentacao> getItens() {
        return getCurrentList();
    }

    @Override
    public int getItemViewType(int position) {
        // Usa getItem() nativo do ListAdapter
        return getItem(position).type;
    }

    public static class MovimentacaoDiffCallback extends DiffUtil.ItemCallback<AdapterItemListaMovimentacao> {
        @Override
        public boolean areItemsTheSame(@NonNull AdapterItemListaMovimentacao oldItem, @NonNull AdapterItemListaMovimentacao newItem) {
            if (oldItem.type != newItem.type) return false;
            if (oldItem.type == AdapterItemListaMovimentacao.TYPE_HEADER) {
                return Objects.equals(oldItem.data, newItem.data);
            } else {
                return oldItem.movimentacaoModel.getId().equals(newItem.movimentacaoModel.getId());
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdapterItemListaMovimentacao oldItem, @NonNull AdapterItemListaMovimentacao newItem) {
            if (oldItem.type == AdapterItemListaMovimentacao.TYPE_HEADER) {
                // Aqui comparamos com segurança o saldo usando o formato inteiro/long, garantindo exatidão
                return oldItem.saldoDia == newItem.saldoDia &&
                        Objects.equals(oldItem.tituloDia, newItem.tituloDia);
            } else {
                MovimentacaoModel m1 = oldItem.movimentacaoModel;
                MovimentacaoModel m2 = newItem.movimentacaoModel;

                // Compara as propriedades de UI. O dinheiro é validado estritamente como número inteiro.
                return m1.getValor() == m2.getValor() &&
                        m1.isPago() == m2.isPago() &&
                        Objects.equals(m1.getDescricao(), m2.getDescricao()) &&
                        Objects.equals(m1.getCategoria_nome(), m2.getCategoria_nome());
            }
        }
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
        // Usa getItem() nativo do ListAdapter
        AdapterItemListaMovimentacao item = getItem(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof MovimentoViewHolder) {
            ((MovimentoViewHolder) holder).bind(item.movimentacaoModel);
        }
    }

    public List<MovimentacaoModel> getMovimentacoesDoDia(int headerPosition) {
        List<MovimentacaoModel> lista = new ArrayList<>();
        int next = headerPosition + 1;
        while (next < getCurrentList().size()
                && getItem(next).type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
            lista.add(getItem(next).movimentacaoModel);
            next++;
        }
        return lista;
    }

    // =========================================================================
    // HEADER VIEW HOLDER
    // =========================================================================

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textDiaTitulo;
        TextView textSaldoDia;
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiaTitulo = itemView.findViewById(R.id.textDiaTitulo);
            textSaldoDia  = itemView.findViewById(R.id.textSaldoDia);
        }

        void bind(AdapterItemListaMovimentacao item) {
            textDiaTitulo.setText(item.tituloDia);

            if (textSaldoDia == null) return;

            // CORREÇÃO: item.saldoDia é long — divisão por 100.0 produz double correto.
            // Antes havia um cast (int) no Helper que truncava valores > R$ 21.474,83,
            // causando saldo negativo ou zero silenciosamente na UI.
            double saldoReais = item.saldoDia / 100.0;
            textSaldoDia.setText("Saldo: " + fmt.format(saldoReais));

            // Cor do saldo: verde positivo, vermelho negativo, cinza neutro
            if (item.saldoDia > 0) {
                textSaldoDia.setTextColor(Color.parseColor("#008000"));
            } else if (item.saldoDia < 0) {
                textSaldoDia.setTextColor(Color.parseColor("#B00020"));
            } else {
                textSaldoDia.setTextColor(Color.parseColor("#9E9E9E"));
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
            textTitulo       = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria    = itemView.findViewById(R.id.textAdapterCategoria);
            textValor        = itemView.findViewById(R.id.textAdapterValor);
            textData         = itemView.findViewById(R.id.textAdapterData);
            textHora         = itemView.findViewById(R.id.textAdapterHora);
            textSeparador    = itemView.findViewById(R.id.textAdapterSeparador);
            viewIndicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
            btnConfirmar     = itemView.findViewById(R.id.btnConfirmarPagamento);
        }

        void bind(MovimentacaoModel mov) {

            // ── Título e categoria ────────────────────────────────────────────
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getTotal_parcelas() > 1
                    ? "🔁 " + mov.getCategoria_nome()
                    : mov.getCategoria_nome());

            // ── Valor com sinal e cor ─────────────────────────────────────────
            // getValor() é long → divisão por 100.0 sempre em double, sem truncamento
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

            // ── Cores padrão de data/hora ─────────────────────────────────────
            textData.setTextColor(Color.parseColor("#888888"));
            textHora.setTextColor(Color.parseColor("#888888"));

            // ── Data e hora ───────────────────────────────────────────────────
            if (mov.getData_movimentacao() != null) {
                Date date = mov.getData_movimentacao().toDate();
                textData.setText(dateFormat.format(date));
                textHora.setText(hourFormat.format(date));

                if (!mov.isPago()) {
                    // PENDENTE: esconde hora e separador
                    textHora.setVisibility(View.GONE);
                    textSeparador.setVisibility(View.GONE);

                    // Botão de confirmação com animação de feedback tátil
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

                    aplicarCorData(textData, mov);

                } else {
                    // PAGO: mostra hora da transação
                    textHora.setVisibility(View.VISIBLE);
                    textSeparador.setVisibility(View.VISIBLE);
                    if (btnConfirmar != null) btnConfirmar.setVisibility(View.GONE);

                    // Se pago em data diferente do vencimento original, exibe ambas
                    if (mov.getData_vencimento_original() != null) {
                        String strPago = dateFormat.format(date);
                        String strVenc = dateFormat.format(mov.getData_vencimento_original().toDate());
                        if (!strPago.equals(strVenc)) {
                            textData.setText(strPago + " (Venc: " + strVenc.substring(0, 5) + ")");
                        }
                    }
                }
            }

            // ── Cliques no item ───────────────────────────────────────────────
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

        // ── Cor de urgência da data (apenas para pendentes) ───────────────────
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