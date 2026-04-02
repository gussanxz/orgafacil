package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

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
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

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

        private static final int TIPO_PRODUTO = 1;
        private static final int TIPO_SERVICO = 2;

        private List<ItemVendaModel> listaItens;
        private final OnItemClickListener listener;
        private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        public interface OnItemClickListener {
                void onItemClick(ItemVendaModel item);
        }

        // CORREÇÃO 2: O nome do construtor deve ser IGUAL ao nome da classe
        public AdapterFiltroPorPSNovaVenda(List<ItemVendaModel> listaItens, OnItemClickListener listener) {
                this.listaItens = listaItens;
                this.listener = listener;
        }

        // Método para atualizar a lista (Filtro)
        public void atualizarLista(List<ItemVendaModel> novaLista) {
                this.listaItens = novaLista;
                notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
                return listaItens.get(position).getTipo() == ItemVendaModel.TIPO_PRODUTO
                        ? TIPO_PRODUTO
                        : TIPO_SERVICO;
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
                holder.bind(listaItens.get(position));
        }

        @Override
        public int getItemCount() {
                return listaItens != null ? listaItens.size() : 0;
        }

        // ViewHolder Interno
        public class VendaViewHolder extends RecyclerView.ViewHolder {
                TextView textNome;
                TextView textPreco;
                TextView textStatus;
                ImageView imgIcone;
                ImageButton btnExcluirProduto;

                public VendaViewHolder(@NonNull View itemView) {
                        super(itemView);

                        // Tenta achar os IDs para Produto
                        textNome = itemView.findViewById(R.id.textNomeProduto);
                        if (textNome == null) {
                                textNome = itemView.findViewById(R.id.textNomeServico);
                        }

                        textPreco = itemView.findViewById(R.id.textDescProduto);
                        if (textPreco == null) {
                                textPreco = itemView.findViewById(R.id.textDescServico);
                        }

                        textStatus = itemView.findViewById(R.id.textStatus);
                        imgIcone = itemView.findViewById(R.id.imageIconeProduto);
                        if (imgIcone == null) imgIcone = itemView.findViewById(R.id.imgIcone);
                        btnExcluirProduto = itemView.findViewById(R.id.btnExcluirProduto);
                }

                void bind(ItemVendaModel item) {
                        if (textNome != null) {
                                textNome.setText(item.getNome());
                        }

                        if (textPreco != null) {
                                textPreco.setText(formatadorMoeda.format(item.getPreco()));
                        }

                        if (textStatus != null) {
                                textStatus.setText("ATIVO");
                                textStatus.setTextColor(Color.parseColor("#2E7D32"));
                        }

                        if (btnExcluirProduto != null) {
                                btnExcluirProduto.setVisibility(View.GONE);
                        }

                        if (imgIcone != null) {
                                if (item instanceof CatalogoModel && ((CatalogoModel) item).isProduto()) {
                                        imgIcone.setImageResource(getIconeProdutoPorIndex(((CatalogoModel) item).getIconeIndex()));
                                        imgIcone.setColorFilter(Color.parseColor("#EF6C00"));
                                } else {
                                        imgIcone.setImageResource(R.drawable.ic_paid_28);
                                        imgIcone.setColorFilter(Color.parseColor("#1565C0"));
                                }
                        }

                        itemView.setOnClickListener(v -> listener.onItemClick(item));
                }
        }

        private int getIconeProdutoPorIndex(int index) {
                switch (index) {
                        case 0: return R.drawable.ic_categorias_mercado_24;
                        case 1: return R.drawable.ic_categorias_roupas_24;
                        case 2: return R.drawable.ic_categorias_comida_24;
                        case 3: return R.drawable.ic_categorias_bebidas_24;
                        case 4: return R.drawable.ic_categorias_eletronicos_24;
                        case 5: return R.drawable.ic_categorias_spa_24;
                        case 6: return R.drawable.ic_categorias_fitness_24;
                        case 7: return R.drawable.ic_categorias_geral_24;
                        case 8: return R.drawable.ic_categorias_ferramentas_24;
                        case 9: return R.drawable.ic_categorias_papelaria_24;
                        case 10: return R.drawable.ic_categorias_casa_24;
                        case 11: return R.drawable.ic_categorias_brinquedos_24;
                        default: return R.drawable.ic_categorias_geral_24;
                }
        }
}