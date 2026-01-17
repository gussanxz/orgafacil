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

    // Constantes dos Tipos de Visualização
    private static final int VIEW_PRODUTO_LISTA = 1;
    private static final int VIEW_PRODUTO_GRADE = 2;
    private static final int VIEW_SERVICO_LISTA = 3;
    private static final int VIEW_SERVICO_GRADE = 4;

    private List<ItemVenda> listaItens;
    private boolean isGridMode = false;

    // Formatador de moeda para R$ (Brasil)
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public AdapterProdutoServico(List<ItemVenda> listaItens) {
        this.listaItens = listaItens;
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

        // AQUI CARREGAMOS OS 4 XMLS DIFERENTES
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
    // VIEWHOLDERS - PRODUTOS (Usam textNomeProduto, textDescProduto, textStatus)
    // =================================================================================

    class ProdutoGradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textPreco, textStatus;

        public ProdutoGradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeProduto);
            textPreco = itemView.findViewById(R.id.textDescProduto); // Na grade, usamos o campo Desc para por o preço
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textPreco != null) textPreco.setText(formatadorMoeda.format(item.getPreco()));

            // MUDANÇA AQUI:
            if(textStatus != null) {
                textStatus.setText("Produto"); // Texto fixo
                textStatus.setTextColor(0xFF2196F3); // Opcional: Cor Azul para Produto
                // Ou use: textStatus.setTextColor(Color.parseColor("#2196F3"));
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

            // MUDANÇA AQUI:
            if(textStatus != null) {
                textStatus.setText("Produto");
                textStatus.setTextColor(0xFF2196F3); // Azul
            }
        }
    }

    // =================================================================================
    // VIEWHOLDERS - SERVIÇOS (Usam textNomeServico, textDescServico)
    // =================================================================================

    class ServicoGradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textPreco, textStatus; // Serviços na grade não usam status no seu pedido

        public ServicoGradeViewHolder(@NonNull View itemView) {
            super(itemView);
            // ATENÇÃO: IDs específicos de SERVIÇO
            textNome = itemView.findViewById(R.id.textNomeServico);
            textPreco = itemView.findViewById(R.id.textDescServico);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textPreco != null) textPreco.setText(formatadorMoeda.format(item.getPreco()));

            if (textStatus != null) {
                textStatus.setText("Serviço");
                // Escolha uma das cores abaixo:

                // Opção 1: Laranja (Bem diferente do azul)
                textStatus.setTextColor(0xFFFF9800);

                // Opção 2: Roxo (Fica elegante)
                // textStatus.setTextColor(0xFF673AB7);
            }
        }
    }

    class ServicoListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textStatus;

        public ServicoListaViewHolder(@NonNull View itemView) {
            super(itemView);
            // ATENÇÃO: IDs específicos de SERVIÇO
            textNome = itemView.findViewById(R.id.textNomeServico);
            textDescricao = itemView.findViewById(R.id.textDescServico);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ItemVenda item) {
            if(textNome != null) textNome.setText(item.getNome());
            if(textDescricao != null) textDescricao.setText(item.getDescricao());
            if (textStatus != null) {
                textStatus.setText("Serviço");
                // Escolha uma das cores abaixo:

                // Opção 1: Laranja (Bem diferente do azul)
                textStatus.setTextColor(0xFFFF9800);

                // Opção 2: Roxo (Fica elegante)
                // textStatus.setTextColor(0xFF673AB7);
            }
        }
    }

    // Adicione este método para permitir filtrar
    public void atualizarLista(List<ItemVenda> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }
}