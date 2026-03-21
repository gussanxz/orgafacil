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
import androidx.core.content.ContextCompat;
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
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onDeleteClick(MovimentacaoModel movimentacaoModel);
        void onLongClick(MovimentacaoModel movimentacaoModel);
        void onCheckClick(MovimentacaoModel movimentacaoModel);
        void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia);

        // NOVO: Clique simples no header
        void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia);
    }

    // Quando true, itens pagos são exibidos em cinza (estilo "concluído").
    // Usado pela ResumoParcelasActivity para destacar parcelas já quitadas.
    // Na ContasActivity e demais telas permanece false — sem alteração visual.
    private final boolean modoParcelasResumidas;

    public AdapterMovimentacaoLista(Context context, OnItemActionListener listener) {
        this(context, listener, false);
    }

    public AdapterMovimentacaoLista(Context context, OnItemActionListener listener, boolean modoParcelasResumidas) {
        super(new MovimentacaoDiffCallback());
        this.context               = context;
        this.listener              = listener;
        this.modoParcelasResumidas = modoParcelasResumidas;
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
        // Guard contra NO_POSITION (-1): pode ocorrer quando getAdapterPosition()
        // é chamado durante animações de remoção/inserção do RecyclerView.
        // Sem esse guard, o loop começaria em índice 0 e retornaria os movimentos
        // do primeiro dia da lista — comportamento silenciosamente incorreto.
        if (headerPosition == RecyclerView.NO_POSITION) {
            return new ArrayList<>();
        }

        List<MovimentacaoModel> lista = new ArrayList<>();
        int next = headerPosition + 1;
        int total = getCurrentList().size();

        // A invariante da lista garante que após um TYPE_HEADER vêm apenas
        // TYPE_MOVIMENTO do mesmo dia, até o próximo TYPE_HEADER ou o fim.
        // O break encerra o loop assim que encontra o próximo header —
        // evitando percorrer o restante da lista (O(n) → O(k), onde k é
        // o número de movimentos do dia, tipicamente 1–10).
        while (next < total) {
            AdapterItemListaMovimentacao item = getItem(next);
            if (item.type == AdapterItemListaMovimentacao.TYPE_HEADER) {
                break; // próximo grupo — para imediatamente
            }
            lista.add(item.movimentacaoModel);
            next++;
        }

        return lista;
    }

    // =========================================================================
    // HEADER VIEW HOLDER
    // =========================================================================

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textDiaTitulo;
        View layoutHeaderClicavel; // Novo

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiaTitulo = itemView.findViewById(R.id.textDiaTitulo);
            layoutHeaderClicavel = itemView.findViewById(R.id.layoutHeaderClicavel); // Novo
        }

        void bind(AdapterItemListaMovimentacao item) {
            textDiaTitulo.setText(item.tituloDia);

            // Adiciona o evento de clique no Header
            layoutHeaderClicavel.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // Aproveitamos a função que já existe para pegar as contas do dia
                    List<MovimentacaoModel> movsDoDia = getMovimentacoesDoDia(position);
                    listener.onHeaderClick(item.tituloDia, movsDoDia);
                }
            });
        }
    }

    // =========================================================================
    // MOVIMENTO VIEW HOLDER
    // =========================================================================

    class MovimentoViewHolder extends RecyclerView.ViewHolder {

        TextView textTitulo, textCategoria, textValor, textData, textHora, textSeparador;
        View viewIndicadorCor;
        ImageButton btnConfirmar;
        androidx.cardview.widget.CardView cardIconeConta; // Mapeado para estilizar
        android.widget.ImageView imageIconeConta; // Mapeado para estilizar

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
            cardIconeConta   = itemView.findViewById(R.id.cardIconeConta); // Novo
            imageIconeConta  = itemView.findViewById(R.id.imageIconeConta); // Novo
        }

        void bind(MovimentacaoModel mov) {
            // Restaura cores ativas padrão (Garante que a reciclagem não aplique cinza onde não deve)
            textTitulo.setTextColor(ContextCompat.getColor(context, R.color.cor_texto));
            textCategoria.setTextColor(Color.parseColor("#757575"));
            textSeparador.setTextColor(Color.parseColor("#BDBDBD"));
            if (cardIconeConta != null) cardIconeConta.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
            if (imageIconeConta != null) imageIconeConta.setColorFilter(Color.parseColor("#009688"));

            // ── Título e categoria ────────────────────────────────────────────
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getTotal_parcelas() > 1
                    ? "🔁 " + mov.getCategoria_nome()
                    : mov.getCategoria_nome());

            // ── Valor com sinal e cor ─────────────────────────────────────────
            String valorFormatado = com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarCentavosParaBRL(mov.getValor());

            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                // Usamos o replace só para tirar o "R$" nativo e colocar o nosso "- R$" visual
                textValor.setText("- " + valorFormatado);
                textValor.setTextColor(Color.parseColor("#E53935"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#E53935"));
            } else {
                textValor.setText("+ " + valorFormatado);
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

                    // ITEM 6: Adiciona o "Vence em" para pendentes
                    textData.setText("Vence em " + dateFormat.format(date));

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

                    // Estilo "Cinza/Inativo" para itens pagos — aplicado apenas quando
                    // modoParcelasResumidas=true (ResumoParcelasActivity).
                    // Em ContasActivity e demais telas, pagos mantêm cores normais.
                    if (modoParcelasResumidas) {
                        textTitulo.setTextColor(Color.parseColor("#9E9E9E"));
                        textCategoria.setTextColor(Color.parseColor("#BDBDBD"));
                        viewIndicadorCor.setBackgroundColor(Color.parseColor("#9E9E9E"));
                        textValor.setTextColor(Color.parseColor("#9E9E9E"));
                        textData.setTextColor(Color.parseColor("#9E9E9E"));
                        textHora.setTextColor(Color.parseColor("#9E9E9E"));
                        textSeparador.setTextColor(Color.parseColor("#9E9E9E"));
                        if (cardIconeConta != null) cardIconeConta.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                        if (imageIconeConta != null) imageIconeConta.setColorFilter(Color.parseColor("#9E9E9E"));
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