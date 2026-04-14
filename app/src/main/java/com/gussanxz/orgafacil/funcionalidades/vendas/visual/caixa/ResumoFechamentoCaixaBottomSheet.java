package com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CaixaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BottomSheet exibido ao clicar em "Fechar Caixa".
 * Mostra o resumo completo do caixa antes da confirmação do fechamento.
 */
public class ResumoFechamentoCaixaBottomSheet extends BottomSheetDialogFragment {

    private CaixaModel       caixa;
    private List<VendaModel> vendasFinalizadas;
    private Runnable         onConfirmar;

    private final SimpleDateFormat fmtDataHora = new SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));
    private final NumberFormat fmtMoeda = NumberFormat.getCurrencyInstance(
            new Locale("pt", "BR"));

    /** Cria a instância com os dados necessários para o resumo. */
    public static ResumoFechamentoCaixaBottomSheet criar(
            @NonNull CaixaModel caixa,
            @NonNull List<VendaModel> vendasFinalizadas,
            @NonNull Runnable onConfirmar) {
        ResumoFechamentoCaixaBottomSheet bs = new ResumoFechamentoCaixaBottomSheet();
        bs.caixa             = caixa;
        bs.vendasFinalizadas = vendasFinalizadas;
        bs.onConfirmar       = onConfirmar;
        return bs;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bs_resumo_fechamento_caixa, container, false);
        popularPeriodo(view);
        popularResumoVendas(view);
        popularFormasPagamento(view);
        configurarItensVendidos(view);
        configurarBotoes(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog d = (BottomSheetDialog) getDialog();
        if (d == null) return;
        FrameLayout sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        BottomSheetBehavior<FrameLayout> beh = BottomSheetBehavior.from(sheet);
        beh.setState(BottomSheetBehavior.STATE_EXPANDED);
        beh.setSkipCollapsed(true);
    }

    // ── Seção: Período ─────────────────────────────────────────────────

    private void popularPeriodo(View view) {
        TextView txtAbertura   = view.findViewById(R.id.txtResumoAbertura);
        TextView txtFechamento = view.findViewById(R.id.txtResumoFechamento);

        if (caixa != null && txtAbertura != null) {
            txtAbertura.setText(fmtDataHora.format(new Date(caixa.getAbertoEmMillis())));
        }
        if (txtFechamento != null) {
            txtFechamento.setText(fmtDataHora.format(new Date()));
        }
    }

    // ── Seção: Resumo de Vendas ────────────────────────────────────────

    private void popularResumoVendas(View view) {
        TextView txtQtd   = view.findViewById(R.id.txtResumoQtdVendas);
        TextView txtTotal = view.findViewById(R.id.txtResumoTotalVendas);

        int qtd = vendasFinalizadas != null ? vendasFinalizadas.size() : 0;
        double total = 0;
        if (vendasFinalizadas != null) {
            for (VendaModel v : vendasFinalizadas) total += v.getValorTotal();
        }

        if (txtQtd   != null) txtQtd.setText(String.valueOf(qtd));
        if (txtTotal != null) txtTotal.setText(fmtMoeda.format(total));
    }

    // ── Seção: Formas de Pagamento ─────────────────────────────────────

    private void popularFormasPagamento(View view) {
        LinearLayout container = view.findViewById(R.id.containerFormasPagamento);
        View         card      = view.findViewById(R.id.cardFormasPagamento);
        if (container == null || vendasFinalizadas == null || vendasFinalizadas.isEmpty()) {
            if (card != null) card.setVisibility(View.GONE);
            return;
        }

        // Ordem preferencial de exibição
        List<String> ordemPreferida = new ArrayList<>(Arrays.asList(
                VendaModel.PAGAMENTO_PIX,
                VendaModel.PAGAMENTO_DINHEIRO,
                VendaModel.PAGAMENTO_CREDITO,
                VendaModel.PAGAMENTO_DEBITO
        ));

        // Agrupa por forma de pagamento: [total, count]
        Map<String, double[]> mapa = new LinkedHashMap<>();
        for (VendaModel v : vendasFinalizadas) {
            String forma = v.getFormaPagamento();
            if (forma == null || forma.isEmpty()) forma = "Outros";
            double[] dados = mapa.getOrDefault(forma, new double[]{0, 0});
            dados[0] += v.getValorTotal();
            dados[1]++;
            mapa.put(forma, dados);
        }

        if (mapa.isEmpty()) {
            if (card != null) card.setVisibility(View.GONE);
            return;
        }

        // Adiciona formas extras não previstas na lista de ordem
        for (String chave : mapa.keySet()) {
            if (!ordemPreferida.contains(chave)) ordemPreferida.add(chave);
        }

        for (String forma : ordemPreferida) {
            if (!mapa.containsKey(forma)) continue;
            double[] dados = mapa.get(forma);
            adicionarLinhaForma(container, forma, dados[0], (int) dados[1]);
        }
    }

    private void adicionarLinhaForma(LinearLayout container,
                                     String forma, double valor, int qtd) {
        LinearLayout linha = new LinearLayout(requireContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setPadding(0, dp(7), 0, dp(7));

        // Nome da forma de pagamento
        TextView txtForma = new TextView(requireContext());
        txtForma.setText(forma);
        txtForma.setTextSize(14);
        LinearLayout.LayoutParams lpForma = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        txtForma.setLayoutParams(lpForma);

        // Quantidade de vendas
        TextView txtQtd = new TextView(requireContext());
        txtQtd.setText(qtd + (qtd == 1 ? " venda" : " vendas"));
        txtQtd.setTextSize(12);
        txtQtd.setTextColor(0xFF757575);
        LinearLayout.LayoutParams lpQtd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpQtd.setMarginEnd(dp(14));
        txtQtd.setLayoutParams(lpQtd);

        // Valor total
        TextView txtValor = new TextView(requireContext());
        txtValor.setText(fmtMoeda.format(valor));
        txtValor.setTextSize(14);
        txtValor.setTypeface(null, Typeface.BOLD);
        txtValor.setTextColor(0xFF2E7D32); // verde escuro

        linha.addView(txtForma);
        linha.addView(txtQtd);
        linha.addView(txtValor);
        container.addView(linha);
    }

    // ── Seção: Itens Vendidos ──────────────────────────────────────────

    private void configurarItensVendidos(View view) {
        LinearLayout headerItens    = view.findViewById(R.id.headerItensVendidos);
        LinearLayout containerItens = view.findViewById(R.id.containerItensVendidos);
        View         divisor        = view.findViewById(R.id.divisorItens);
        ImageView    imgSeta        = view.findViewById(R.id.imgSetaItens);
        View         card           = view.findViewById(R.id.cardItensVendidos);

        // Agrega itens de todas as vendas finalizadas por nome
        Map<String, ItemResumo> mapa = new LinkedHashMap<>();
        if (vendasFinalizadas != null) {
            for (VendaModel v : vendasFinalizadas) {
                if (v.getItens() == null) continue;
                for (ItemVendaRegistradaModel item : v.getItens()) {
                    String chave = item.getNome() != null ? item.getNome() : "—";
                    if (!mapa.containsKey(chave)) {
                        mapa.put(chave, new ItemResumo(chave, item.getTipo()));
                    }
                    ItemResumo r = mapa.get(chave);
                    r.quantidade += item.getQuantidade();
                    r.valorTotal += item.getSubtotal();
                }
            }
        }

        if (mapa.isEmpty() || containerItens == null) {
            if (card != null) card.setVisibility(View.GONE);
            return;
        }

        // Preenche as linhas de itens
        for (ItemResumo r : mapa.values()) {
            adicionarLinhaItem(containerItens, r);
        }

        // Configura o toggle de expandir/recolher
        if (headerItens != null) {
            headerItens.setOnClickListener(v -> {
                boolean visivel = containerItens.getVisibility() == View.VISIBLE;
                containerItens.setVisibility(visivel ? View.GONE : View.VISIBLE);
                if (divisor != null) divisor.setVisibility(visivel ? View.GONE : View.VISIBLE);
                if (imgSeta != null)  imgSeta.setRotation(visivel ? 0f : 180f);
            });
        }
    }

    private void adicionarLinhaItem(LinearLayout container, ItemResumo r) {
        LinearLayout linha = new LinearLayout(requireContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setPadding(0, dp(6), 0, dp(6));

        // Label do tipo (Produto / Serviço)
        TextView txtTipo = new TextView(requireContext());
        boolean isProduto = r.tipo == ItemVendaRegistradaModel.TIPO_PRODUTO;
        txtTipo.setText(isProduto ? "Produto" : "Serviço");
        txtTipo.setTextSize(10);
        txtTipo.setTextColor(isProduto ? 0xFF2E7D32 : 0xFF1565C0);
        LinearLayout.LayoutParams lpTipo = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTipo.setMarginEnd(dp(10));
        txtTipo.setLayoutParams(lpTipo);

        // Nome do item
        TextView txtNome = new TextView(requireContext());
        txtNome.setText(r.nome);
        txtNome.setTextSize(13);
        LinearLayout.LayoutParams lpNome = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        txtNome.setLayoutParams(lpNome);

        // Quantidade
        TextView txtQtd = new TextView(requireContext());
        txtQtd.setText(r.quantidade + "x");
        txtQtd.setTextSize(13);
        txtQtd.setTextColor(0xFF757575);
        LinearLayout.LayoutParams lpQtd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpQtd.setMarginEnd(dp(12));
        txtQtd.setLayoutParams(lpQtd);

        // Valor
        TextView txtValor = new TextView(requireContext());
        txtValor.setText(fmtMoeda.format(r.valorTotal));
        txtValor.setTextSize(13);
        txtValor.setTypeface(null, Typeface.BOLD);

        linha.addView(txtTipo);
        linha.addView(txtNome);
        linha.addView(txtQtd);
        linha.addView(txtValor);
        container.addView(linha);
    }

    // ── Botões ─────────────────────────────────────────────────────────

    private void configurarBotoes(View view) {
        LinearLayout btnCancelar  = view.findViewById(R.id.btnCancelarFechamento);
        LinearLayout btnConfirmar = view.findViewById(R.id.btnConfirmarFechamento);

        if (btnCancelar  != null) btnCancelar.setOnClickListener(v -> dismiss());
        if (btnConfirmar != null) btnConfirmar.setOnClickListener(v -> {
            dismiss();
            if (onConfirmar != null) onConfirmar.run();
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private int dp(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Modelo interno ─────────────────────────────────────────────────

    private static class ItemResumo {
        String nome;
        int    tipo;
        int    quantidade;
        double valorTotal;

        ItemResumo(String nome, int tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }
    }
}
