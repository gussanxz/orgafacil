package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterExibeListaMovimentacaoContas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ExibirItemListaMovimentacaoContas> itens;
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

    public AdapterExibeListaMovimentacaoContas(Context context, List<ExibirItemListaMovimentacaoContas> itens, OnItemActionListener listener) {
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
        if (viewType == ExibirItemListaMovimentacaoContas.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_contas_header_dia, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.adapter_contas_item_movimentacao, parent, false);
            return new MovimentoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExibirItemListaMovimentacaoContas item = itens.get(position);
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

        void bind(ExibirItemListaMovimentacaoContas item) {
            textDiaTitulo.setText(item.tituloDia);
            if (textSaldoDia != null) {
                // [PRECISÃO]: Converte centavos para double na exibição
                double saldoReal = item.saldoDia / 100.0;

                // Formata o texto com o símbolo da moeda (R$)
                textSaldoDia.setText("Saldo: " + currencyFormat.format(saldoReal));

                // --- POLIMENTO ESTÉTICO ---
                // Define a cor dinamicamente: Verde para positivo/zero, Vermelho para negativo
                if (item.saldoDia > 0) {
                    textSaldoDia.setTextColor(Color.parseColor("#008000")); // Verde escuro para melhor leitura
                } else if (item.saldoDia < 0) {
                    textSaldoDia.setTextColor(Color.parseColor("#B00020")); // Vermelho padrão Material Design
                } else {
                    textSaldoDia.setTextColor(Color.parseColor("#9E9E9E")); // Cinza se o saldo for exatamente zero
                }
            }
        }
    }

    class MovimentoViewHolder extends RecyclerView.ViewHolder {
        TextView textTitulo, textCategoria, textValor, textData, textHora;
        View viewIndicadorCor;
        ImageButton btnConfirmar; // Mapeado do novo componente no XML

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

            if (mov.getData_movimentacao() != null) {
                Date date = mov.getData_movimentacao().toDate();
                textData.setText(dateFormat.format(date));
                textHora.setText(hourFormat.format(date));
            }

            // [PRECISÃO]: Inteiro centavos para double exibição [cite: 2026-02-07]
            double valorReais = mov.getValor() / 100.0;

            // [CLEAN CODE]: Uso de Enum em vez de IDs mágicos para definição de cores
            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                textValor.setText("- " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#E53935"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#E53935"));
            } else {
                textValor.setText("+ " + currencyFormat.format(valorReais));
                textValor.setTextColor(Color.parseColor("#00D39E"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#00D39E"));
            }

            // [LOGICA STATUS]: Diferencia visualmente itens PAGOS de PENDENTES (FUTUROS)
            // Itens com pago=false mostram o botão de check para confirmação.
            if (!mov.isPago()) {
                itemView.setAlpha(0.7f); // Sutil transparência para indicar pendência
                if (btnConfirmar != null) {
                    btnConfirmar.setVisibility(View.VISIBLE);
                    btnConfirmar.setOnClickListener(v -> {
                        if (listener != null) listener.onCheckClick(mov);
                    });
                }
            } else {
                itemView.setAlpha(1.0f); // Totalmente opaco para movimentações confirmadas
                if (btnConfirmar != null) {
                    btnConfirmar.setVisibility(View.GONE);
                }
            }

            // Abre tela de edição ao clicar no item
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", mov);
                context.startActivity(intent);
            });

            // Clique longo para disparar o listener de ações adicionais
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(mov);
                return true;
            });
        }
    }
}