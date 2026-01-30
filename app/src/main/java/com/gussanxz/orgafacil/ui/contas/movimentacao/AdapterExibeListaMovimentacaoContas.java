package com.gussanxz.orgafacil.ui.contas.movimentacao;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.features.contas.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.data.model.Movimentacao;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * ADAPTER: AdapterExibeListaMovimentacaoContas
 *
 * RESPONSABILIDADE: Gerenciar a exibição da timeline financeira, agrupando receitas e despesas por data.
 * Localizado em: ui.contas (Organização por Funcionalidade).
 *
 * O QUE ELA FAZ:
 * 1. Multi-Layout (Timeline): Alterna entre cabeçalhos de data (Header) e itens de conta (Movimentação).
 * 2. Cálculo Visual de Saldo: Exibe o saldo do dia no cabeçalho com cores dinâmicas (Verde para positivo, Vermelho para negativo).
 * 3. Diferenciação de Fluxo: Formata valores de Receita (+) e Despesa (-) com cores específicas.
 * 4. Gestão de Eventos:
 * - Clique Curto: Direciona para a tela de edição.
 * - Clique Longo: Aciona a interface para exclusão do registro.
 * 5. Formatação: Converte valores para a moeda brasileira (R$) e exibe metadados como hora e categoria.
 */

public class AdapterExibeListaMovimentacaoContas extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ExibirItemListaMovimentacaoContas> itens;
    private final Context context;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // Listener para comunicação com a Activity
    private final OnItemActionListener listener;

    // Interface atualizada: Agora suporta DELETAR (via Swipe/Lixeira) e LONG CLICK
    public interface OnItemActionListener {
        void onDeleteClick(Movimentacao movimentacao); // Usado pelo Swipe
        void onLongClick(Movimentacao movimentacao);   // Usado pelo Segurar
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
            // Layout do Cabeçalho (Data)
            View v = inflater.inflate(R.layout.item_contas_header_dia, parent, false);
            return new HeaderViewHolder(v);

        } else {
            // Layout da Conta (O XML que limpamos e tiramos o botão)
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
            ((MovimentoViewHolder) holder).bind(item.movimentacao);
        }
    }

    // --------- ViewHolder do Cabeçalho ---------
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
                textSaldoDia.setText("Saldo: " + currencyFormat.format(item.saldoDia));
                int color = item.saldoDia >= 0 ? Color.parseColor("#008000") : Color.parseColor("#B00020");
                textSaldoDia.setTextColor(color);
            }
        }
    }

    // --------- ViewHolder do Item ---------
    class MovimentoViewHolder extends RecyclerView.ViewHolder {

        TextView textTitulo, textCategoria, textValor, textData, textHora;
        // 1. Declarar a View do indicador
        View viewIndicadorCor;

        MovimentoViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitulo = itemView.findViewById(R.id.textAdapterTitulo);
            textCategoria = itemView.findViewById(R.id.textAdapterCategoria);
            textValor = itemView.findViewById(R.id.textAdapterValor);
            textData = itemView.findViewById(R.id.textAdapterData);
            textHora = itemView.findViewById(R.id.textAdapterHora);

            // 2. Ligar a variável ao ID do seu XML
            viewIndicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
        }

        void bind(Movimentacao mov) {
            textTitulo.setText(mov.getDescricao());
            textCategoria.setText(mov.getCategoria());
            textData.setText(mov.getData());
            textHora.setText(mov.getHora());

            double valor = mov.getValor();

            // 3. Lógica para definir a cor do indicador
            if ("d".equals(mov.getTipo())) {
                // DESPESA: Vermelho
                textValor.setText("- " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#E53935"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#E53935"));
            } else {
                // RECEITA: Verde
                textValor.setText("+ " + currencyFormat.format(valor));
                textValor.setTextColor(Color.parseColor("#00D39E"));
                viewIndicadorCor.setBackgroundColor(Color.parseColor("#00D39E"));
            }

            // 1. CLIQUE CURTO -> EDITAR (Como já estava)
            itemView.setOnClickListener(v -> {
                // Se preferir, pode passar via interface também: listener.onEditClick(mov);
                // Mas deixar direto aqui funciona bem se a Activity de destino for fixa
                Intent intent = new Intent(context, EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", mov);
                intent.putExtra("keyFirebase", mov.getKey());
                // Precisamos usar casting se quisermos usar o launcher da Activity principal,
                // mas startActivity direto funciona (só não atualiza a lista ao voltar automaticamente sem o launcher)
                // O ideal aqui seria usar uma interface para editar também, mas vamos manter simples:
                context.startActivity(intent);
            });

            // 2. CLIQUE LONGO -> EXCLUIR (NOVO!)
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(mov);
                }
                return true; // Retorna true para consumir o evento e não disparar o clique curto depois
            });
        }
    }
}