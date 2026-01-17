package com.gussanxz.orgafacil.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.ItemVenda;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterProdutoServico extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 1. Interface para o clique
    public interface OnItemClickListener {
        void onItemClick(ItemVenda item);
    }

    // Constantes dos Tipos de Visualização
    private static final int VIEW_PRODUTO_LISTA = 1;
    private static final int VIEW_PRODUTO_GRADE = 2;
    private static final int VIEW_SERVICO_LISTA = 3;
    private static final int VIEW_SERVICO_GRADE = 4;

    private List<ItemVenda> listaItens;
    private boolean isGridMode = false;
    private final OnItemClickListener listener; // Variável do listener

    // Formatador de moeda para R$ (Brasil)
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // 2. Construtor Atualizado recebendo o listener
    public AdapterProdutoServico(List<ItemVenda> listaItens, OnItemClickListener listener) {
        this.listaItens = listaItens;
        this.listener = listener;
    }

    public void setModoGrade(boolean ativarGrade) {
        this.isGridMode = ativarGrade;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ItemVenda item = listaItens.get(position);
        if (item.getTipo() == ItemVenda.TIPO_PRODUTO) {
            return isGridMode ? VIEW_PRODUTO_GRADE : VIEW_PRODUTO_LISTA;
        } else {
            return isGridMode ? VIEW_SERVICO_GRADE : VIEW_SERVICO_LISTA;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_PRODUTO_GRADE:
                view = inflater.inflate(R.layout.item_produto_grid, parent, false);
                return new ProdutoGradeViewHolder(view);

            case VIEW_SERVICO_GRADE:
                view = inflater.inflate(R.layout.item_servico_grid, parent, false);
                return new ServicoGradeViewHolder(view);

            case VIEW_SERVICO_LISTA:
                view = inflater.inflate(R.layout.item_servico_lista, parent, false);
                return new ServicoListaViewHolder(view);

            case VIEW_PRODUTO_LISTA:
            default:
                view = inflater.inflate(R.layout.item_produto_lista, parent, false);
                return new ProdutoListaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemVenda item = listaItens.get(position);

        // 3. Configurar o clique no item inteiro
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

        // Configurar os dados visuais
        if (holder instanceof ProdutoGradeViewHolder) {
            ((ProdutoGradeViewHolder) holder).bind(item);
        } else if (holder instanceof ProdutoListaViewHolder) {
            ((ProdutoListaViewHolder) holder).bind(item);
        } else if (holder instanceof ServicoGradeViewHolder) {
            ((ServicoGradeViewHolder) holder).bind(item);
        } else if (holder instanceof ServicoListaViewHolder) {
            ((ServicoListaViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    // =================================================================================
    // VIEWHOLDERS - PRODUTOS
    // =================================================================================

    class ProdutoGradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textPreco, textStatus;

        public ProdutoGradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeProduto);
            textPreco = itemView.findViewById(R.id.textDescProduto);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textPreco != null) textPreco.setText(formatadorMoeda.format(item.getPreco()));

            if(textStatus != null) {
                textStatus.setText("Produto");
                textStatus.setTextColor(0xFF2196F3); // Azul
            }
        }
    }

    class ProdutoListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textStatus;

        public ProdutoListaViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeProduto);
            textDescricao = itemView.findViewById(R.id.textDescProduto);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textDescricao != null) textDescricao.setText(item.getDescricao());

            if(textStatus != null) {
                textStatus.setText("Produto");
                textStatus.setTextColor(0xFF2196F3); // Azul
            }
        }
    }

    // =================================================================================
    // VIEWHOLDERS - SERVIÇOS
    // =================================================================================

    class ServicoGradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textPreco, textStatus;

        public ServicoGradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeServico);
            textPreco = itemView.findViewById(R.id.textDescServico);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textPreco != null) textPreco.setText(formatadorMoeda.format(item.getPreco()));

            if (textStatus != null) {
                textStatus.setText("Serviço");
                textStatus.setTextColor(0xFFFF9800); // Laranja
            }
        }
    }

    class ServicoListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textStatus;

        public ServicoListaViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeServico);
            textDescricao = itemView.findViewById(R.id.textDescServico);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textDescricao != null) textDescricao.setText(item.getDescricao());
            if (textStatus != null) {
                textStatus.setText("Serviço");
                textStatus.setTextColor(0xFFFF9800); // Laranja
            }
        }
    }

    public void atualizarLista(List<ItemVenda> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }
}