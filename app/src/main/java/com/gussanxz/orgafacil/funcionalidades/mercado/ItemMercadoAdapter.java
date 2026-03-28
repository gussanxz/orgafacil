package com.gussanxz.orgafacil.funcionalidades.mercado;

import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;

import java.util.List;

/**
 * Adapter para a lista de itens do mercado.
 *
 * Cada item exibe (RF01, RF02, RF05 – ponto 5 do design):
 *   • Barra lateral colorida por categoria
 *   • Nome (16sp bold)
 *   • Categoria (11sp cinza)
 *   • Valor editável inline (R$) × stepper de quantidade = Subtotal
 *   • CheckBox "No Carrinho" grande (48dp) à direita
 *   • Botão excluir
 */
public class ItemMercadoAdapter
        extends RecyclerView.Adapter<ItemMercadoAdapter.ViewHolder> {

    // ─── Interface de callbacks ───────────────────────────────────────────────
    public interface OnItemInteractionListener {
        /** RF02 – toggling do carrinho. */
        void onCarrinhoToggle(ItemMercado item, boolean noCarrinho);

        /** RF01 – valor editado inline; recebe centavos (RN01). */
        void onValorAlterado(ItemMercado item, int novoValorCentavos);

        /** RF01 – stepper de quantidade. */
        void onQuantidadeAlterada(ItemMercado item, int novaQtd);

        /** RF01 – exclusão. */
        void onExcluirItem(ItemMercado item);
    }

    private final List<ItemMercado> lista;
    private final OnItemInteractionListener listener;

    public ItemMercadoAdapter(List<ItemMercado> lista, OnItemInteractionListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    // ─── Inflate ──────────────────────────────────────────────────────────────
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lista_mercado, parent, false);
        return new ViewHolder(view);
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
        ItemMercado item = lista.get(position);

        // Nome e categoria
        vh.textNome.setText(item.getNome());
        vh.textCategoria.setText(item.getCategoria());

        // Cor lateral por categoria (RF05)
        vh.viewIndicador.setBackgroundColor(corParaCategoria(item.getCategoria()));

        // Valor unitário: exibe centavos ÷ 100 (RN01 – só na apresentação)
        // Remove o TextWatcher antes de setar para evitar loop
        vh.editValor.removeTextChangedListener(vh.valorWatcher);
        vh.editValor.setText(formatarValorParaEdicao(item.getValorCentavos()));
        vh.editValor.addTextChangedListener(vh.valorWatcher);
        vh.valorWatcher.item = item;

        // Quantidade
        vh.textQuantidade.setText(String.valueOf(item.getQuantidade()));

        // Subtotal (RN01)
        vh.textSubtotal.setText("Subtotal: " + formatarMoeda((int) item.getSubtotalCentavos()));

        // Riscar nome se estiver no carrinho
        atualizarEstiloCarrinho(vh, item.isNoCarrinho());

        // CheckBox (RF02) – remove listener antes de setar checked para evitar loop
        vh.checkboxCarrinho.setOnCheckedChangeListener(null);
        vh.checkboxCarrinho.setChecked(item.isNoCarrinho());
        vh.checkboxCarrinho.setOnCheckedChangeListener((btn, isChecked) -> {
            item.setNoCarrinho(isChecked);
            atualizarEstiloCarrinho(vh, isChecked);
            listener.onCarrinhoToggle(item, isChecked);
        });

        // Stepper – diminuir
        vh.btnMenos.setOnClickListener(v -> {
            int qtdAtual = item.getQuantidade();
            if (qtdAtual > 1) {
                listener.onQuantidadeAlterada(item, qtdAtual - 1);
            }
        });

        // Stepper – aumentar
        vh.btnMais.setOnClickListener(v ->
                listener.onQuantidadeAlterada(item, item.getQuantidade() + 1));

        // Excluir
        vh.btnExcluir.setOnClickListener(v -> listener.onExcluirItem(item));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    // ─── Estilo "no carrinho" ─────────────────────────────────────────────────
    private void atualizarEstiloCarrinho(ViewHolder vh, boolean noCarrinho) {
        if (noCarrinho) {
            vh.textNome.setPaintFlags(
                    vh.textNome.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            vh.textNome.setAlpha(0.5f);
        } else {
            vh.textNome.setPaintFlags(
                    vh.textNome.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            vh.textNome.setAlpha(1f);
        }
    }

    // ─── Cor da barra lateral por categoria (RF05) ───────────────────────────
    private int corParaCategoria(String categoria) {
        if (categoria == null) return android.graphics.Color.parseColor("#CCCCCC");
        switch (categoria.toLowerCase()) {
            case "açougue":    return android.graphics.Color.parseColor("#E53935");
            case "hortifruti": return android.graphics.Color.parseColor("#43A047");
            case "limpeza":    return android.graphics.Color.parseColor("#29B6F6");
            case "padaria":    return android.graphics.Color.parseColor("#FF7043");
            case "laticínios": return android.graphics.Color.parseColor("#AB47BC");
            case "bebidas":    return android.graphics.Color.parseColor("#1E88E5");
            case "higiene":    return android.graphics.Color.parseColor("#00ACC1");
            case "mercearia":
            default:           return android.graphics.Color.parseColor("#FFA726");
        }
    }

    // ─── Helpers de formatação (RN01) ────────────────────────────────────────

    /** Para o EditText de edição: "25,90" */
    private String formatarValorParaEdicao(int centavos) {
        return String.format("%,.2f", centavos / 100.0)
                .replace(".", ",");
    }

    /** Para exibição de rótulos: "R$ 25,90" */
    private String formatarMoeda(int centavos) {
        double valor = centavos / 100.0;
        return String.format("R$ %,.2f", valor)
                .replace(",", "X").replace(".", ",").replace("X", ".");
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        View      viewIndicador;
        TextView  textNome;
        TextView  textCategoria;
        EditText  editValor;
        TextView  textQuantidade;
        TextView  textSubtotal;
        ImageView btnMenos;
        ImageView btnMais;
        CheckBox  checkboxCarrinho;
        ImageView btnExcluir;

        ValorTextWatcher valorWatcher;

        ViewHolder(@NonNull View v) {
            super(v);
            viewIndicador    = v.findViewById(R.id.viewIndicadorCategoria);
            textNome         = v.findViewById(R.id.textNomeProduto);
            textCategoria    = v.findViewById(R.id.textCategoriaProduto);
            editValor        = v.findViewById(R.id.editValorUnitario);
            textQuantidade   = v.findViewById(R.id.textQuantidade);
            textSubtotal     = v.findViewById(R.id.textSubtotalItem);
            btnMenos         = v.findViewById(R.id.btnDiminuirQtd);
            btnMais          = v.findViewById(R.id.btnAumentarQtd);
            checkboxCarrinho = v.findViewById(R.id.checkboxNoCarrinho);
            btnExcluir       = v.findViewById(R.id.btnExcluirItem);

            valorWatcher = new ValorTextWatcher(null);
            editValor.addTextChangedListener(valorWatcher);
        }
    }

    // ─── TextWatcher para o campo de valor (RF01, RN01, CT04, CT05) ──────────
    static class ValorTextWatcher implements TextWatcher {
        ItemMercado item;
        private final ItemMercadoAdapter adapter;

        // Construtor usado apenas para inicializar — item é setado no bind
        ValorTextWatcher(ItemMercadoAdapter adapter) {
            this.adapter = adapter;
        }

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (item == null || adapter == null) return;

            String raw = s.toString().replace(",", ".");
            try {
                double valorDouble = Double.parseDouble(raw);
                // RN01 – converte para centavos
                int centavos = (int) Math.round(valorDouble * 100);
                adapter.listener.onValorAlterado(item, centavos);
            } catch (NumberFormatException ignored) {
                // Campo em branco ou inválido — CT05 tratado no listener
            }
        }
    }
}