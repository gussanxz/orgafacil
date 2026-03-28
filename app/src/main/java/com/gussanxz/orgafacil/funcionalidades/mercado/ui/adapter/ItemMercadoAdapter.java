package com.gussanxz.orgafacil.funcionalidades.mercado.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.mercado.dados.model.ItemMercadoModel;
import com.gussanxz.orgafacil.funcionalidades.mercado.dados.repository.MercadoRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ItemMercadoAdapter
        extends RecyclerView.Adapter<ItemMercadoAdapter.ViewHolder> {

    public static final List<String> CATEGORIAS = Arrays.asList(
            "Limpeza", "Açougue", "Hortifruti", "Padaria",
            "Mercearia", "Laticínios", "Bebidas", "Higiene", "Outros"
    );

    public interface OnItemInteractionListener {
        void onCarrinhoToggle(ItemMercadoModel item, boolean noCarrinho);

        void onValorAlterado(ItemMercadoModel item, int novoValorCentavos);

        void onQuantidadeAlterada(ItemMercadoModel item, int novaQtd);

        void onExcluirItem(ItemMercadoModel item);

        void onItemNovoPronto(ItemMercadoModel item);
    }

    private final List<ItemMercadoModel> lista;
    private final OnItemInteractionListener listener;
    private final MercadoRepository repository;

    public ItemMercadoAdapter(List<ItemMercadoModel> lista, OnItemInteractionListener listener) {
        this.lista = lista;
        this.listener = listener;
        this.repository = MercadoRepository.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lista_mercado, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
        ItemMercadoModel item = lista.get(position);

        // ── Nome ─────────────────────────────────────────────────────────────
        vh.editNome.setOnFocusChangeListener(null);
        vh.editNome.setText(item.getNome() != null ? item.getNome() : "");
        vh.editNome.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) return;
            String nome = vh.editNome.getText() != null
                    ? vh.editNome.getText().toString().trim() : "";
            item.setNome(nome);
            if (nome.isEmpty()) return;
            if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) {
                repository.buscarMemoriaPreco(nome, new MercadoRepository.CallbackPreco() {
                    @Override
                    public void onSucesso(int valorCentavos) {
                        if (item.getValorCentavos() == 0 && valorCentavos > 0) {
                            item.setValorCentavos(valorCentavos);
                            vh.editValor.removeTextChangedListener(vh.moneyWatcher);
                            vh.moneyWatcher.setCentavos(valorCentavos);
                            String fmt = formatarParaEdicao(valorCentavos);
                            vh.editValor.setText(fmt);
                            vh.editValor.setSelection(fmt.length());
                            vh.editValor.addTextChangedListener(vh.moneyWatcher);
                            atualizarSubtotal(vh, item);
                        }
                        listener.onItemNovoPronto(item);
                    }

                    @Override
                    public void onNaoEncontrado() {
                        listener.onItemNovoPronto(item);
                    }

                    @Override
                    public void onErro(String m) {
                        listener.onItemNovoPronto(item);
                    }
                });
            }
        });

        // ── Valor (BUG 4: MoneyWatcher — entrada por centavo) ─────────────────
        vh.editValor.removeTextChangedListener(vh.moneyWatcher);
        vh.moneyWatcher.item = item;
        vh.moneyWatcher.setCentavos(item.getValorCentavos());
        vh.editValor.setText(formatarParaEdicao(item.getValorCentavos()));
        vh.editValor.addTextChangedListener(vh.moneyWatcher);

        // ── Categoria (BUG 1 + SUG F) ─────────────────────────────────────────
        // ── Categoria (Forçando para baixo) ─────────────────────────────────────────
        int cor = corParaCategoria(item.getCategoria());
        atualizarBadgeCategoria(vh, item.getCategoria(), cor);
        vh.viewIndicador.setBackgroundColor(cor);

        vh.btnSelecionarCategoria.setOnClickListener(v -> {
            // 1. Instancia o ListPopupWindow passando o contexto
            android.widget.ListPopupWindow listPopupWindow = new android.widget.ListPopupWindow(v.getContext());

            // 2. Define o elemento que servirá de "teto" para o dropdown
            listPopupWindow.setAnchorView(v);

            // 3. Cria um ArrayAdapter simples com a sua lista de CATEGORIAS
            android.widget.ArrayAdapter<String> adapterPopup = new android.widget.ArrayAdapter<>(
                    v.getContext(),
                    android.R.layout.simple_list_item_1,
                    CATEGORIAS
            );
            listPopupWindow.setAdapter(adapterPopup);

            // 4. Ajusta a largura para não ficar muito espremido ou largo demais
            listPopupWindow.setWidth(android.widget.ListPopupWindow.WRAP_CONTENT);

            // 5. Trata o clique no item da lista (mudamos 'position' para 'pos')
            listPopupWindow.setOnItemClickListener((parent, view, pos, id) -> {
                String cat = CATEGORIAS.get(pos); // <-- usando o 'pos' aqui
                item.setCategoria(cat);
                int c = corParaCategoria(cat);
                atualizarBadgeCategoria(vh, cat, c);
                vh.viewIndicador.setBackgroundColor(c);

                // Atualiza no Firebase se o item já existir
                if (item.getFirestoreId() != null && !item.getFirestoreId().isEmpty()) {
                    repository.atualizarCategoria(item, null);
                }

                // Fecha o dropdown após a seleção
                listPopupWindow.dismiss();
            });

            // 6. Exibe o dropdown
            listPopupWindow.show();
        });

        // ... (código dos steppers fica igual) ...

        // ── Exclusão com Dialog ──────────────────────────────────────────────────
        vh.btnExcluir.setOnClickListener(v -> {
            String nomeItem = (item.getNome() == null || item.getNome().trim().isEmpty()) ? "este item vazio" : item.getNome();
            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Excluir Item")
                    .setMessage("Tem certeza que deseja remover " + nomeItem + " da sua lista?")
                    .setPositiveButton("Excluir", (dialog, which) -> listener.onExcluirItem(item))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // ── Quantidade (SUG A: EditText direto) ───────────────────────────────
        vh.editQuantidade.removeTextChangedListener(vh.qtdWatcher);
        vh.editQuantidade.setText(String.valueOf(item.getQuantidade()));
        vh.qtdWatcher.item = item;
        vh.editQuantidade.addTextChangedListener(vh.qtdWatcher);

        atualizarSubtotal(vh, item);

        // ── Checkbox (BUG 10 + SUG E) ─────────────────────────────────────────
        vh.checkboxCarrinho.setOnCheckedChangeListener(null);
        vh.checkboxCarrinho.setChecked(item.isNoCarrinho());
        aplicarEstiloCarrinho(vh, item.isNoCarrinho(), false);

        vh.checkboxCarrinho.setOnClickListener(v -> {
            boolean isChecked = vh.checkboxCarrinho.isChecked();

            // Validação: não permite marcar se o valor for 0
            if (isChecked && item.getValorCentavos() <= 0) {
                vh.checkboxCarrinho.setChecked(false); // Desmarca imediatamente
                android.widget.Toast.makeText(v.getContext(),
                        "Adicione o valor do item antes de colocar no carrinho",
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Fluxo normal
                item.setNoCarrinho(isChecked);
                aplicarEstiloCarrinho(vh, isChecked, true);
                listener.onCarrinhoToggle(item, isChecked);
            }
        });
        // ── Steppers ─────────────────────────────────────────────────────────
        vh.btnMenos.setOnClickListener(v -> {
            int q = item.getQuantidade();
            if (q > 1) {
                item.setQuantidade(q - 1);
                vh.editQuantidade.removeTextChangedListener(vh.qtdWatcher);
                vh.editQuantidade.setText(String.valueOf(q - 1));
                vh.editQuantidade.addTextChangedListener(vh.qtdWatcher);
                atualizarSubtotal(vh, item);
                listener.onQuantidadeAlterada(item, q - 1);
            }
        });
        vh.btnMais.setOnClickListener(v -> {
            int q = item.getQuantidade() + 1;
            item.setQuantidade(q);
            vh.editQuantidade.removeTextChangedListener(vh.qtdWatcher);
            vh.editQuantidade.setText(String.valueOf(q));
            vh.editQuantidade.addTextChangedListener(vh.qtdWatcher);
            atualizarSubtotal(vh, item);
            listener.onQuantidadeAlterada(item, q);
        });

        vh.btnExcluir.setOnClickListener(v -> listener.onExcluirItem(item));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // ─── Subtotal em tempo real (BUG 3) ──────────────────────────────────────
    void atualizarSubtotal(ViewHolder vh, ItemMercadoModel item) {
        long sub = item.getSubtotalCentavos();
        vh.textSubtotal.setText(sub <= 0 ? "Subtotal: —"
                : "Subtotal: " + formatarMoeda((int) sub));
    }

    // ─── BUG 10 + SUG E: estilo "no carrinho" ────────────────────────────────
    private void aplicarEstiloCarrinho(ViewHolder vh, boolean noCarrinho, boolean animar) {
        if (noCarrinho) {
            vh.checkboxCarrinho.setBackgroundResource(R.drawable.ic_check_box_selecionado_24);
            vh.checkboxCarrinho.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#43A047")));
            vh.editNome.setPaintFlags(vh.editNome.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            vh.editNome.setAlpha(0.45f);
            vh.card.setCardBackgroundColor(Color.parseColor("#F1FBF3"));
            vh.textSubtotal.setTextColor(Color.parseColor("#388E3C"));
        } else {
            vh.checkboxCarrinho.setBackgroundResource(R.drawable.ic_check_box_vazio_24);
            vh.checkboxCarrinho.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            vh.editNome.setPaintFlags(vh.editNome.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            vh.editNome.setAlpha(1f);
            vh.card.setCardBackgroundColor(Color.WHITE);
            vh.textSubtotal.setTextColor(Color.parseColor("#555555"));
        }
        if (animar) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(vh.checkboxCarrinho, "scaleX", 1f, 1.3f, 1f),
                    ObjectAnimator.ofFloat(vh.checkboxCarrinho, "scaleY", 1f, 1.3f, 1f));
            set.setDuration(200);
            set.start();
        }
    }

    // ─── Badge categoria ─────────────────────────────────────────────────────
    private void atualizarBadgeCategoria(ViewHolder vh, String categoria, int cor) {
        vh.textCategoria.setText(
                (categoria == null || categoria.isEmpty()) ? "Escolher categoria" : categoria);
        vh.viewDotCategoria.setBackgroundColor(cor);
    }

    // ─── Cor por categoria ────────────────────────────────────────────────────
    public static int corParaCategoria(String categoria) {
        if (categoria == null || categoria.isEmpty()) return Color.parseColor("#CCCCCC");
        switch (categoria.toLowerCase(Locale.getDefault())) {
            case "açougue":
                return Color.parseColor("#E53935");
            case "hortifruti":
                return Color.parseColor("#43A047");
            case "limpeza":
                return Color.parseColor("#29B6F6");
            case "padaria":
                return Color.parseColor("#FF7043");
            case "laticínios":
                return Color.parseColor("#AB47BC");
            case "bebidas":
                return Color.parseColor("#1E88E5");
            case "higiene":
                return Color.parseColor("#00ACC1");
            case "outros":
                return Color.parseColor("#78909C");
            default:
                return Color.parseColor("#FFA726"); // mercearia
        }
    }

    // ─── Formatação RN01 ─────────────────────────────────────────────────────
    String formatarParaEdicao(int centavos) {
        return String.format(Locale.US, "%d,%02d", centavos / 100, centavos % 100);
    }

    private String formatarMoeda(int centavos) {
        return String.format(Locale.US, "R$ %d,%02d", centavos / 100, centavos % 100);
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        View viewIndicador;
        EditText editNome;
        EditText editValor;
        View viewDotCategoria;
        TextView textCategoria;
        LinearLayout btnSelecionarCategoria;
        EditText editQuantidade;
        TextView textSubtotal;
        ImageView btnMenos;
        ImageView btnMais;
        CheckBox checkboxCarrinho;
        ImageView btnExcluir;
        MoneyTextWatcher moneyWatcher;
        MoneyTextWatcher.QtdTextWatcher qtdWatcher;

        ViewHolder(@NonNull View v, @NonNull ItemMercadoAdapter adapter) {
            super(v);
            card = v.findViewById(R.id.cardItemMercado);
            viewIndicador = v.findViewById(R.id.viewIndicadorCategoria);
            editNome = v.findViewById(R.id.textNomeProduto);
            editValor = v.findViewById(R.id.editValorUnitario);
            viewDotCategoria = v.findViewById(R.id.viewDotCategoria);
            textCategoria = v.findViewById(R.id.textCategoriaProduto);
            btnSelecionarCategoria = v.findViewById(R.id.btnSelecionarCategoria);
            editQuantidade = v.findViewById(R.id.textQuantidade);
            textSubtotal = v.findViewById(R.id.textSubtotalItem);
            btnMenos = v.findViewById(R.id.btnDiminuirQtd);
            btnMais = v.findViewById(R.id.btnAumentarQtd);
            checkboxCarrinho = v.findViewById(R.id.checkboxNoCarrinho);
            btnExcluir = v.findViewById(R.id.btnExcluirItem);
            moneyWatcher = new MoneyTextWatcher(adapter, this);
            qtdWatcher = new MoneyTextWatcher.QtdTextWatcher(adapter, this);
        }
    }

    // ─── BUG 4: MoneyTextWatcher ──────────────────────────────────────────────
    static class MoneyTextWatcher implements TextWatcher {
        ItemMercadoModel item;
        private int centavosAtual = 0;
        private boolean atualizando = false;
        private final ItemMercadoAdapter adapter;
        private final ViewHolder vh;

        MoneyTextWatcher(ItemMercadoAdapter adapter, ViewHolder vh) {
            this.adapter = adapter;
            this.vh = vh;
        }

        void setCentavos(int c) {
            this.centavosAtual = c;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (item == null) return;
            try {
                int q = Integer.parseInt(s.toString().trim());

                // Impede quantidade negativa ou zero
                if (q <= 0) {
                    vh.editQuantidade.removeTextChangedListener(this);
                    vh.editQuantidade.setText("1");
                    vh.editQuantidade.setSelection(1);
                    vh.editQuantidade.addTextChangedListener(this);
                    q = 1; // força a ser 1
                }

                if (q != item.getQuantidade()) {
                    item.setQuantidade(q);
                    adapter.atualizarSubtotal(vh, item);
                    adapter.listener.onQuantidadeAlterada(item, q);
                }
            } catch (NumberFormatException ignored) {
                // Se apagar tudo, podemos deixar em branco temporariamente até ele digitar
            }
        }

        // ─── QtdTextWatcher (SUG A + BUG 3) ──────────────────────────────────────
        static class QtdTextWatcher implements TextWatcher {
            ItemMercadoModel item;
            private final ItemMercadoAdapter adapter;
            private final ViewHolder vh;

            QtdTextWatcher(ItemMercadoAdapter adapter, ViewHolder vh) {
                this.adapter = adapter;
                this.vh = vh;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (item == null) return;
                try {
                    int q = Integer.parseInt(s.toString().trim());
                    if (q > 0 && q != item.getQuantidade()) {
                        item.setQuantidade(q);
                        adapter.atualizarSubtotal(vh, item);
                        adapter.listener.onQuantidadeAlterada(item, q);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}