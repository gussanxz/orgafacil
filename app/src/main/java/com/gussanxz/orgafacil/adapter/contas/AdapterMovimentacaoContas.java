package com.gussanxz.orgafacil.adapter.contas;

import android.content.Context;
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
import com.gussanxz.orgafacil.model.Movimentacao;

import java.util.ArrayList;
import java.util.List;

public class AdapterMovimentacaoContas extends RecyclerView.Adapter<AdapterMovimentacaoContas.MyViewHolder> {

    private List<Movimentacao> movimentacoes;
    private Context context;
    private OnMovimentacaoListener listener; // Listener para avisar a Activity

    // 1. Interface para comunicar cliques (Editar e Excluir)
    public interface OnMovimentacaoListener {
        void onEditarClick(Movimentacao movimentacao);
        void onExcluirClick(Movimentacao movimentacao);
    }

    // 2. Construtor Atualizado
    public AdapterMovimentacaoContas(List<Movimentacao> movimentacoes, Context context, OnMovimentacaoListener listener) {
        this.movimentacoes = movimentacoes;
        this.context = context;
        this.listener = listener;
    }

    // Método para atualizar a lista (usado em filtros/updates)
    public void atualizarLista(List<Movimentacao> novaLista) {
        this.movimentacoes = new ArrayList<>(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ATENÇÃO: Confirme se o nome do seu XML novo é 'adapter_contas_movimentacao' mesmo
        View itemLista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_contas_movimentacao, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Movimentacao movimentacao = movimentacoes.get(position);

        // Preenche os textos básicos
        holder.titulo.setText(movimentacao.getDescricao());
        holder.categoria.setText(movimentacao.getCategoria());
        holder.hora.setText(movimentacao.getHora());

        // Formatação Simples da Data (Ex: 24/01/2026 -> 24/01)
        String dataOriginal = movimentacao.getData();
        if (dataOriginal != null && dataOriginal.length() >= 5) {
            holder.data.setText(dataOriginal.substring(0, 5));
        } else {
            holder.data.setText(dataOriginal);
        }

        // --- LÓGICA DE CORES (RECEITA vs DESPESA) ---
        if (movimentacao.getTipo().equals("d")) {
            // DESPESA (Vermelho)
            holder.valor.setText("- R$ " + movimentacao.getValor());
            holder.valor.setTextColor(Color.parseColor("#F44336"));

            // Barra lateral vermelha
            holder.indicadorCor.setBackgroundColor(Color.parseColor("#F44336"));

            // Ícone Vermelho
            holder.icone.setImageResource(R.drawable.ic_label_24); // Ou icone de dinheiro off
            holder.icone.setColorFilter(Color.parseColor("#EF9A9A"));

        } else {
            // RECEITA (Verde)
            holder.valor.setText("R$ " + movimentacao.getValor());
            holder.valor.setTextColor(Color.parseColor("#00D39E")); // Verde Água

            // Barra lateral verde
            holder.indicadorCor.setBackgroundColor(Color.parseColor("#4CAF50"));

            // Ícone Verde
            holder.icone.setImageResource(R.drawable.ic_label_24); // Ou icone de dinheiro on
            holder.icone.setColorFilter(Color.parseColor("#80CBC4"));
        }

        // --- CLIQUES ---

        // 1. Clique no Card -> Avisa a Activity para EDITAR
        holder.itemView.setOnClickListener(v -> {
            listener.onEditarClick(movimentacao);
        });

        // 2. Clique na Lixeira -> Avisa a Activity para EXCLUIR
        holder.btnExcluir.setOnClickListener(v -> {
            listener.onExcluirClick(movimentacao);
        });
    }

    @Override
    public int getItemCount() {
        return movimentacoes.size();
    }

    // --- VIEWHOLDER (Ligando os componentes do XML novo) ---
    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView titulo, valor, categoria, data, hora;
        ImageButton btnExcluir;
        View indicadorCor;
        ImageView icone;

        public MyViewHolder(View itemView) {
            super(itemView);

            titulo = itemView.findViewById(R.id.textAdapterTitulo);
            valor = itemView.findViewById(R.id.textAdapterValor);
            categoria = itemView.findViewById(R.id.textAdapterCategoria);
            data = itemView.findViewById(R.id.textAdapterData);
            hora = itemView.findViewById(R.id.textAdapterHora);

            // Novos componentes
            btnExcluir = itemView.findViewById(R.id.btnExcluirConta);
            indicadorCor = itemView.findViewById(R.id.viewIndicadorCor);
            icone = itemView.findViewById(R.id.imageIconeConta);
        }
    }
}