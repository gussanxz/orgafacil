package com.gussanxz.orgafacil.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.model.ItemVenda;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdapterProdutoServico extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LISTA = 1;
    private static final int TYPE_GRADE = 2;

    private List<ItemVenda> listaItens;
    private boolean isGridMode = false;
    private final OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(ItemVenda item);
    }

    public AdapterProdutoServico(List<ItemVenda> listaItens, OnItemClickListener listener) {
        this.listaItens = listaItens;
        this.listener = listener;
    }

    // Chamado pela Activity quando o toggle muda
    public void setModoGrade(boolean ativarGrade) {
        this.isGridMode = ativarGrade;
        notifyDataSetChanged(); // Força o RecyclerView a redesenhar tudo
    }

    public void atualizarLista(List<ItemVenda> novaLista) {
        this.listaItens = novaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? TYPE_GRADE : TYPE_LISTA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_GRADE) {
            // Importante: Layout de Grade (Quadrado)
            View view = inflater.inflate(R.layout.item_venda_grade, parent, false);
            return new GradeViewHolder(view);
        } else {
            // Importante: Layout de Lista (Horizontal)
            View view = inflater.inflate(R.layout.item_venda_lista, parent, false);
            return new ListaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemVenda item = listaItens.get(position);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));

        if (holder instanceof GradeViewHolder) {
            ((GradeViewHolder) holder).bind(item);
        } else if (holder instanceof ListaViewHolder) {
            ((ListaViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return listaItens != null ? listaItens.size() : 0;
    }

    // --- LÓGICA CENTRALIZADA DE VISUAL (O SEGREDO DA ORGANIZAÇÃO) ---
    private void aplicarEstiloVisual(boolean isProduto, TextView txtTag, ImageView imgIcone, CardView cardIcone) {

            int corDestaque; // Cor do Texto e da Tinta do Ícone
            int corFundoElements = Color.WHITE; // Fundo Branco (fixo)

            // Ícone FIXO de Câmera (Placeholder da futura foto)
            int iconeRes = R.drawable.ic_camera_alt_120;

            String textoTag;
            int backgroundRes;

            if (isProduto) {
                // === PRODUTO: Laranja ===
                corDestaque = Color.parseColor("#EF6C00");
                textoTag = "PRODUTO";
                backgroundRes = R.drawable.bg_tag_produto;
            } else {
                // === SERVIÇO: Azul Escuro ===
                corDestaque = Color.parseColor("#1565C0");
                textoTag = "SERVIÇO";
                backgroundRes = R.drawable.bg_tag_servico;
            }

            // 1. Configura a Tag (Texto)
            txtTag.setText(textoTag);
            txtTag.setTextColor(corDestaque);
            txtTag.setBackgroundResource(backgroundRes);

            // Garante que o fundo da tag (shape) fique branco
            if (txtTag.getBackground() != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(
                        txtTag.getBackground(),
                        corFundoElements
                );
            }

            // 2. Configura o Ícone (Câmera Fixa)
            imgIcone.setImageResource(iconeRes);

            // AQUI ESTÁ O TRUQUE:
            // Pintamos a câmera com a cor da categoria (Laranja ou Azul).
            // Se quiser a câmera cinza (neutra), mude 'corDestaque' para Color.GRAY abaixo.
            imgIcone.setColorFilter(corDestaque);

            // 3. Configura o Fundo do Card do Ícone (Branco)
            cardIcone.setCardBackgroundColor(corFundoElements);
    }

    // --- VIEWHOLDER: GRADE ---
    class GradeViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag;
        ImageView imageIcone;
        CardView cardIcone;

        public GradeViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem);
            textPreco = itemView.findViewById(R.id.textPrecoItem);
            textTipoTag = itemView.findViewById(R.id.textTipoTag);
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVenda item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());
            textPreco.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco()));

            // Usa o método centralizado
            aplicarEstiloVisual(
                    item.getTipo() == ItemVenda.TIPO_PRODUTO,
                    textTipoTag, imageIcone, cardIcone
            );
        }
    }

    // --- VIEWHOLDER: LISTA ---
    class ListaViewHolder extends RecyclerView.ViewHolder {
        TextView textNome, textDescricao, textPreco, textTipoTag;
        ImageView imageIcone;
        CardView cardIcone;

        public ListaViewHolder(@NonNull View itemView) {
            super(itemView);
            textNome = itemView.findViewById(R.id.textNomeItem);
            textDescricao = itemView.findViewById(R.id.textDescricaoItem);
            textPreco = itemView.findViewById(R.id.textPrecoItem);
            textTipoTag = itemView.findViewById(R.id.textTipoTag);
            imageIcone = itemView.findViewById(R.id.imageIcone);
            cardIcone = itemView.findViewById(R.id.cardIcone);
        }

        void bind(ItemVenda item) {
            textNome.setText(item.getNome());
            textDescricao.setText(item.getDescricao());
            textPreco.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.getPreco()));

            // Usa o método centralizado
            aplicarEstiloVisual(
                    item.getTipo() == ItemVenda.TIPO_PRODUTO,
                    textTipoTag, imageIcone, cardIcone
            );
        }
    }
}