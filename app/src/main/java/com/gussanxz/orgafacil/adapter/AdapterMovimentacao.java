package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.content.Intent; // IMPORTANTE
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.contas.DespesasActivity; // IMPORTANTE
import com.gussanxz.orgafacil.model.Movimentacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jamilton Damasceno
 */

public class AdapterMovimentacao extends RecyclerView.Adapter<AdapterMovimentacao.MyViewHolder> {

    List<Movimentacao> movimentacoes;
    Context context;

    public AdapterMovimentacao(List<Movimentacao> movimentacoes, Context context) {
        this.movimentacoes = movimentacoes;
        this.context = context;
    }

    public void atualizarLista(List<Movimentacao> novaLista) {
        this.movimentacoes = new ArrayList<>(novaLista);
        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLista = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_contas_movimentacao, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Movimentacao movimentacao = movimentacoes.get(position);

        holder.titulo.setText(movimentacao.getDescricao());
        holder.valor.setText(String.valueOf(movimentacao.getValor()));
        holder.categoria.setText(movimentacao.getCategoria());
        holder.valor.setTextColor(context.getResources().getColor(R.color.colorAccentProventos));
        holder.data.setText(movimentacao.getData());
        holder.hora.setText(movimentacao.getHora());

        if (movimentacao.getTipo().equals("d")) {
            holder.valor.setTextColor(context.getResources().getColor(R.color.colorAccent));
            holder.valor.setText("-" + movimentacao.getValor());
        }

        // =========================================================================
        // AQUI ESTAVA FALTANDO: O CLIQUE PARA ABRIR A TELA DE EDIÇÃO
        // =========================================================================
        holder.itemView.setOnClickListener(v -> {
            Intent intent;

            // Verifica se é Despesa ("d") ou Receita ("r") para abrir a tela certa
            if (movimentacao.getTipo().equals("d")) {
                intent = new Intent(context, DespesasActivity.class);
            } else {
                // Caso você tenha uma ReceitasActivity, descomente abaixo:
                intent = new Intent(context, DespesasActivity.class);
                // Se não tiver, pode deixar apontando pra Despesas ou tratar depois
            }

            // PASSANDO OS DADOS PARA A TELA DE EDIÇÃO
            // O "chave" é o mais importante para o botão Excluir aparecer!
            intent.putExtra("chave", movimentacao.getKey());
            intent.putExtra("valor", movimentacao.getValor());
            intent.putExtra("categoria", movimentacao.getCategoria());
            intent.putExtra("descricao", movimentacao.getDescricao());
            intent.putExtra("data", movimentacao.getData());
            intent.putExtra("hora", movimentacao.getHora());
            intent.putExtra("tipo", movimentacao.getTipo());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movimentacoes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView titulo, valor, categoria, data, hora;

        public MyViewHolder(View itemView) {
            super(itemView);

            titulo = itemView.findViewById(R.id.textAdapterTitulo);
            valor = itemView.findViewById(R.id.textAdapterValor);
            categoria = itemView.findViewById(R.id.textAdapterCategoria);
            data = itemView.findViewById(R.id.textAdapterData);
            hora = itemView.findViewById(R.id.textAdapterHora);
        }
    }
}