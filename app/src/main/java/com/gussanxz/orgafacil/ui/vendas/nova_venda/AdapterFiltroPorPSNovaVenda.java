package com.gussanxz.orgafacil.ui.vendas.nova_venda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.ItemVenda;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * ADAPTER: AdapterFiltroPorPSNovaVenda
 *
 * RESPONSABILIDADE:  Gerenciar a exibição e o filtro dinâmico de itens (Produtos e Serviços)
 * na grade principal da tela de Nova Venda.
 * Localizado em: ui.vendas.nova_venda (Organização por Funcionalidade).
 *
 * O QUE ELA FAZ:
 * 1. Diferenciação por Tipo: Seleciona automaticamente entre o layout de Produto
 * e o layout de Serviço baseado no dado vindo do modelo.
 *
 * 2. Suporte a Filtros: Fornece o metodo 'atualizarLista' para reagir aos cliques
 * nas categorias (Chips) da Activity.
 *
 * 3. Gerenciamento de Layouts: Infla 'item_nova_venda_produto_grid' ou
 * 'item_nova_venda_servico_grid' conforme a necessidade.
 *
 * 4. Resiliência de IDs: Possui lógica de "fallback" no ViewHolder para encontrar
 * componentes mesmo que os layouts usem nomes de IDs diferentes.
 *
 * 5. Formatação: Exibe os valores monetários no padrão brasileiro (R$).
 */

// CORREÇÃO 1: Ajustado o tipo do Adapter para usar o VendaViewHolder específico
public class AdapterFiltroPorPSNovaVenda extends RecyclerView.Adapter<AdapterFiltroPorPSNovaVenda.VendaViewHolder> {

        private List<ItemVenda> listaItens;
        private final OnItemClickListener listener;
        private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        // Constantes para identificar o tipo
        private static final int TIPO_PRODUTO = 1;
        private static final int TIPO_SERVICO = 2;

        // Interface de clique
        public interface OnItemClickListener {
                void onItemClick(ItemVenda item);
        }

        // CORREÇÃO 2: O nome do construtor deve ser IGUAL ao nome da classe
        public AdapterFiltroPorPSNovaVenda(List<ItemVenda> listaItens, OnItemClickListener listener) {
                this.listaItens = listaItens;
                this.listener = listener;
        }

        // Método para atualizar a lista (Filtro)
        public void atualizarLista(List<ItemVenda> novaLista) {
                this.listaItens = novaLista;
                notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
                // Usa o tipo definido na sua Model ItemVenda
                // Certifique-se que ItemVenda.TIPO_PRODUTO existe e é público na sua Model
                return listaItens.get(position).getTipo() == ItemVenda.TIPO_PRODUTO ? TIPO_PRODUTO : TIPO_SERVICO;
        }

        @NonNull
        @Override
        public VendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                View view;

                // Infla o layout correspondente
                // CORREÇÃO 3: Certifique-se que os nomes dos arquivos XML abaixo existem na pasta layout
                if (viewType == TIPO_PRODUTO) {
                        view = inflater.inflate(R.layout.item_nova_venda_produto_grid, parent, false);
                } else {
                        view = inflater.inflate(R.layout.item_nova_venda_servico_grid, parent, false);
                }

                return new VendaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VendaViewHolder holder, int position) {
                ItemVenda item = listaItens.get(position);
                holder.bind(item);
        }

        @Override
        public int getItemCount() {
                return listaItens != null ? listaItens.size() : 0;
        }

        // ViewHolder Interno
        public class VendaViewHolder extends RecyclerView.ViewHolder {
                TextView textNome, textPreco;
                ImageView imgIcone;

                public VendaViewHolder(@NonNull View itemView) {
                        super(itemView);

                        // Tenta achar os IDs para Produto
                        textNome = itemView.findViewById(R.id.textNomeProduto);
                        textPreco = itemView.findViewById(R.id.textDescProduto);
                        imgIcone = itemView.findViewById(R.id.imgIcone); // Certifique-se que adicionou esse ID no XML

                        // Fallback: Se não achou os IDs de produto, tenta achar os de Serviço
                        // (Isso permite usar XMLs com IDs diferentes se necessário)
                        if (textNome == null) {
                                textNome = itemView.findViewById(R.id.textNomeServico);
                        }
                        if (textPreco == null) {
                                textPreco = itemView.findViewById(R.id.textDescServico);
                        }
                }

                void bind(ItemVenda item) {
                        if (textNome != null) {
                                textNome.setText(item.getNome());
                        }

                        if (textPreco != null) {
                                textPreco.setText(formatadorMoeda.format(item.getPreco()));
                        }

                        // Configura o clique
                        itemView.setOnClickListener(v -> listener.onItemClick(item));
                }
        }
}