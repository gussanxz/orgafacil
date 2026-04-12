package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoCallback;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendasRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments.TopProdutosVendasAdapter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumoVendasRelatorioFragment extends Fragment {

    private TextView txtTotalVendido, txtQtdVendas, txtTicketMedio;
    private TextView txtRelVendasTotalVariacao, txtRelVendasQtdVariacao;
    private LinearLayout layoutPagamentos;
    private MaterialCardView cardPagamentosResumo;
    private ImageView btnMesAnterior, btnMesProximo;
    private TextView txtMesAtual;
    private View cardTop5;
    private TextView txtTop5ColunaLabel;
    private RecyclerView recyclerTopProdutos;
    private TopProdutosVendasAdapter topAdapter;
    private View layoutVazio;
    private TextView btnAbaProdutos, btnAbaCategorias;
    private boolean exibindoProdutos = true;
    private List<VendaModel> doMesCache = new ArrayList<>();

    private View cardListaCompleta;
    private TextView txtListaCompletaTitulo, txtListaCompletaContagem, txtListaCompletaColunaLabel;
    private TextView btnListaCompletaProdutos, btnListaCompletaCategorias;
    private RecyclerView recyclerListaCompleta;
    private TodosProdutosVendasAdapter listaCompletaAdapter;
    private boolean listaExibindoProdutos = true;

    private VendaRepository vendaRepository;
    private VendasRepository vendasRepository;
    private ListenerRegistration listenerRegistration;
    private List<VendaModel> listaCompleta = new ArrayList<>();
    private Map<String, String> catalogoCategoriasMap = new HashMap<>();
    private Map<String, String> catalogoCategoriasIdMap = new HashMap<>();
    private Map<String, String> categoriaNomesMap = new HashMap<>();
    private Map<String, String> catalogoNomesMap = new HashMap<>();

    private Calendar mesSelecionado;
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat fmtMes = new SimpleDateFormat("MMM/yy", new Locale("pt", "BR"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resumo_vendas_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTotalVendido           = view.findViewById(R.id.txtRelVendasTotal);
        txtRelVendasTotalVariacao = view.findViewById(R.id.txtRelVendasTotalVariacao);
        txtQtdVendas              = view.findViewById(R.id.txtRelVendasQtd);
        txtRelVendasQtdVariacao   = view.findViewById(R.id.txtRelVendasQtdVariacao);
        txtTicketMedio            = view.findViewById(R.id.txtRelVendasTicket);
        layoutPagamentos          = view.findViewById(R.id.layoutPagamentos);
        cardPagamentosResumo      = view.findViewById(R.id.cardPagamentosResumo);
        btnMesAnterior            = view.findViewById(R.id.btnRelVendasMesAnterior);
        cardPagamentosResumo.setOnClickListener(v -> mostrarDialogPagamentos());
        btnMesProximo     = view.findViewById(R.id.btnRelVendasMesProximo);
        txtMesAtual       = view.findViewById(R.id.txtRelVendasMesAtual);
        layoutVazio          = view.findViewById(R.id.layoutRelVendasVazio);
        cardTop5             = view.findViewById(R.id.cardTop5);
        txtTop5ColunaLabel   = view.findViewById(R.id.txtTop5ColunaLabel);
        recyclerTopProdutos  = view.findViewById(R.id.recyclerRelVendasTopProdutos);

        recyclerTopProdutos.setLayoutManager(new LinearLayoutManager(requireContext()));
        topAdapter = new TopProdutosVendasAdapter();
        recyclerTopProdutos.setAdapter(topAdapter);

        cardListaCompleta          = view.findViewById(R.id.cardListaCompleta);
        txtListaCompletaTitulo     = view.findViewById(R.id.txtListaCompletaTitulo);
        txtListaCompletaContagem   = view.findViewById(R.id.txtListaCompletaContagem);
        txtListaCompletaColunaLabel= view.findViewById(R.id.txtListaCompletaColunaLabel);
        btnListaCompletaProdutos   = view.findViewById(R.id.btnListaCompletaProdutos);
        btnListaCompletaCategorias = view.findViewById(R.id.btnListaCompletaCategorias);
        recyclerListaCompleta      = view.findViewById(R.id.recyclerListaCompleta);
        recyclerListaCompleta.setLayoutManager(new LinearLayoutManager(requireContext()));
        listaCompletaAdapter = new TodosProdutosVendasAdapter();
        recyclerListaCompleta.setAdapter(listaCompletaAdapter);

        btnListaCompletaProdutos.setOnClickListener(v -> {
            listaExibindoProdutos = true;
            atualizarVisualToggleLista();
            renderizarListaCompleta(doMesCache);
        });
        btnListaCompletaCategorias.setOnClickListener(v -> {
            listaExibindoProdutos = false;
            atualizarVisualToggleLista();
            renderizarListaCompleta(doMesCache);
        });

        btnAbaProdutos   = view.findViewById(R.id.btnRelVendasAbaProdutos);
        btnAbaCategorias = view.findViewById(R.id.btnRelVendasAbaCategorias);

        btnAbaProdutos.setOnClickListener(v -> {
            exibindoProdutos = true;
            atualizarVisualAbas();
            renderizarTop5(doMesCache);
        });
        btnAbaCategorias.setOnClickListener(v -> {
            exibindoProdutos = false;
            atualizarVisualAbas();
            renderizarTop5(doMesCache);
        });

        mesSelecionado = Calendar.getInstance();
        atualizarTextMes();

        vendaRepository = new VendaRepository();
        vendasRepository = new VendasRepository();

        btnMesAnterior.setOnClickListener(v -> {
            mesSelecionado.add(Calendar.MONTH, -1);
            atualizarTextMes();
            calcularResumo();
        });
        btnMesProximo.setOnClickListener(v -> {
            mesSelecionado.add(Calendar.MONTH, 1);
            atualizarTextMes();
            calcularResumo();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Carrega todos os itens do catálogo (ativos e inativos) para resolver
        // nomes e categorias atuais nas vendas históricas.
        vendasRepository.listarTodoCatalogo(new RepoCallback<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot snap) {
                catalogoCategoriasMap.clear();
                catalogoCategoriasIdMap.clear();
                catalogoNomesMap.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String cat = doc.getString("categoria");
                    if (cat != null && !cat.isEmpty()) {
                        catalogoCategoriasMap.put(doc.getId(), cat);
                    }
                    String catId = doc.getString("categoriaId");
                    if (catId != null && !catId.isEmpty()) {
                        catalogoCategoriasIdMap.put(doc.getId(), catId);
                    }
                    String nome = doc.getString("nome");
                    if (nome != null && !nome.isEmpty()) {
                        catalogoNomesMap.put(doc.getId(), nome);
                    }
                }
                // Após o catálogo, carrega as categorias para resolver nomes atuais pelo ID.
                vendasRepository.listarTodasCategorias(new RepoCallback<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot catSnap) {
                        categoriaNomesMap.clear();
                        for (DocumentSnapshot catDoc : catSnap.getDocuments()) {
                            String nome = catDoc.getString("nome");
                            if (nome != null && !nome.isEmpty()) {
                                categoriaNomesMap.put(catDoc.getId(), nome);
                            }
                        }
                        iniciarListenerVendas();
                    }
                    @Override
                    public void onError(Exception e) {
                        iniciarListenerVendas();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                iniciarListenerVendas();
            }
        });
    }

    private void iniciarListenerVendas() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        listenerRegistration = vendaRepository.listarTempoReal(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaCompleta.clear();
                if (lista != null) listaCompleta.addAll(lista);
                calcularResumo();
            }
            @Override
            public void onErro(String erro) { }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void atualizarTextMes() {
        txtMesAtual.setText(fmtMes.format(mesSelecionado.getTime()).toUpperCase(new Locale("pt", "BR")));
    }

    private void calcularResumo() {
        int anoFiltro = mesSelecionado.get(Calendar.YEAR);
        int mesFiltro = mesSelecionado.get(Calendar.MONTH);

        // Mês anterior para comparativo
        Calendar mesAnteriorCal = (Calendar) mesSelecionado.clone();
        mesAnteriorCal.add(Calendar.MONTH, -1);
        int anoAnterior = mesAnteriorCal.get(Calendar.YEAR);
        int mesAnterior = mesAnteriorCal.get(Calendar.MONTH);

        List<VendaModel> doMes = new ArrayList<>();
        List<VendaModel> doMesAnterior = new ArrayList<>();

        for (VendaModel v : listaCompleta) {
            if (!VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) continue;
            Calendar cal = Calendar.getInstance();
            long ts = v.getDataHoraFechamentoMillis() > 0
                    ? v.getDataHoraFechamentoMillis()
                    : v.getDataHoraAberturaMillis();
            cal.setTimeInMillis(ts);
            int a = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH);
            if (a == anoFiltro && m == mesFiltro)       doMes.add(v);
            else if (a == anoAnterior && m == mesAnterior) doMesAnterior.add(v);
        }

        boolean vazio = doMes.isEmpty();
        layoutVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        cardTop5.setVisibility(vazio ? View.GONE : View.VISIBLE);
        cardListaCompleta.setVisibility(vazio ? View.GONE : View.VISIBLE);

        if (vazio) {
            txtTotalVendido.setText(fmt.format(0));
            txtQtdVendas.setText("0 vendas");
            txtTicketMedio.setText(fmt.format(0));
            txtRelVendasTotalVariacao.setVisibility(View.GONE);
            txtRelVendasQtdVariacao.setVisibility(View.GONE);
            layoutPagamentos.removeAllViews();
            topAdapter.atualizar(new ArrayList<>());
            listaCompletaAdapter.atualizar(new ArrayList<>());
            return;
        }

        // Total e ticket médio
        double total = 0;
        for (VendaModel v : doMes) total += v.getValorTotal();
        double ticket = total / doMes.size();

        txtTotalVendido.setText(fmt.format(total));
        txtQtdVendas.setText(doMes.size() + (doMes.size() == 1 ? " venda" : " vendas"));
        txtTicketMedio.setText(fmt.format(ticket));

        // Comparativo com mês anterior — Total
        double totalAnterior = 0;
        for (VendaModel v : doMesAnterior) totalAnterior += v.getValorTotal();
        exibirVariacaoTotal(total, totalAnterior);

        // Comparativo com mês anterior — Qtd de vendas
        exibirVariacaoQtd(doMes.size(), doMesAnterior.size());

        // Breakdown de formas de pagamento
        Map<String, Integer> contagemPag = new LinkedHashMap<>();
        for (VendaModel v : doMes) {
            String pag = v.getFormaPagamento();
            if (pag != null && !pag.isEmpty())
                contagemPag.put(pag, contagemPag.getOrDefault(pag, 0) + 1);
        }
        renderizarPagamentos(contagemPag, doMes.size());

        // Salva cache e renderiza conforme aba ativa
        doMesCache = doMes;
        renderizarTop5(doMes);
        renderizarListaCompleta(doMes);
    }

    private void exibirVariacaoTotal(double atual, double anterior) {
        if (anterior <= 0) {
            txtRelVendasTotalVariacao.setVisibility(View.GONE);
            return;
        }
        double variacao = ((atual - anterior) / anterior) * 100;
        boolean positivo = variacao >= 0;
        String sinal = positivo ? "↑" : "↓";
        txtRelVendasTotalVariacao.setText(
                String.format(Locale.ROOT, "%s %.0f%% vs mês ant.", sinal, Math.abs(variacao)));
        // Cores com alto contraste sobre fundo colorPrimary
        txtRelVendasTotalVariacao.setTextColor(
                positivo ? Color.parseColor("#B9F6CA") : Color.parseColor("#FF8A80"));
        txtRelVendasTotalVariacao.setVisibility(View.VISIBLE);
    }

    private void exibirVariacaoQtd(int atual, int anterior) {
        if (anterior <= 0) {
            txtRelVendasQtdVariacao.setVisibility(View.GONE);
            return;
        }
        int diff = atual - anterior;
        boolean positivo = diff >= 0;
        String sinal = positivo ? "+" : "";
        txtRelVendasQtdVariacao.setText(
                String.format(Locale.ROOT, "%s%d vs mês anterior", sinal, diff));
        txtRelVendasQtdVariacao.setTextColor(
                positivo ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        txtRelVendasQtdVariacao.setVisibility(View.VISIBLE);
    }

    private void renderizarPagamentos(Map<String, Integer> contagem, int totalVendas) {
        layoutPagamentos.removeAllViews();
        if (contagem.isEmpty()) return;

        // Ordena por contagem decrescente
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(contagem.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);
        int corSecundario = ContextCompat.getColor(requireContext(), R.color.cor_texto_secundario);

        for (int i = 0; i < Math.min(entries.size(), 3); i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            int pct = totalVendas > 0 ? (e.getValue() * 100) / totalVendas : 0;

            LinearLayout linha = new LinearLayout(requireContext());
            linha.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams paramsNome =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            TextView txtMetodo = new TextView(requireContext());
            txtMetodo.setLayoutParams(paramsNome);
            txtMetodo.setText(e.getKey());
            txtMetodo.setTextSize(12f);
            txtMetodo.setTextColor(corTexto);
            txtMetodo.setMaxLines(1);
            txtMetodo.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView txtPct = new TextView(requireContext());
            txtPct.setText(pct + "%");
            txtPct.setTextSize(12f);
            txtPct.setTextColor(corSecundario);
            txtPct.setTypeface(null, Typeface.BOLD);

            linha.addView(txtMetodo);
            linha.addView(txtPct);

            if (i > 0) {
                int dp4 = (int) (4 * requireContext().getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams rowParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dp4;
                linha.setLayoutParams(rowParams);
            }
            layoutPagamentos.addView(linha);
        }
    }

    private void atualizarVisualAbas() {
        int corInativo = ContextCompat.getColor(requireContext(), R.color.cor_texto_secundario);
        if (exibindoProdutos) {
            btnAbaProdutos.setBackgroundResource(R.drawable.bg_aba_ativa);
            btnAbaProdutos.setTextColor(Color.WHITE);
            btnAbaCategorias.setBackgroundResource(0);
            btnAbaCategorias.setTextColor(corInativo);
            txtTop5ColunaLabel.setText("Produto");
        } else {
            btnAbaCategorias.setBackgroundResource(R.drawable.bg_aba_ativa);
            btnAbaCategorias.setTextColor(Color.WHITE);
            btnAbaProdutos.setBackgroundResource(0);
            btnAbaProdutos.setTextColor(corInativo);
            txtTop5ColunaLabel.setText("Categoria");
        }
    }

    private void renderizarListaCompleta(List<VendaModel> doMes) {
        Map<String, double[]> mapa = new LinkedHashMap<>();
        for (VendaModel v : doMes) {
            if (v.getItens() == null) continue;
            for (ItemVendaRegistradaModel item : v.getItens()) {
                String chave;
                if (listaExibindoProdutos) {
                    chave = resolverNome(item);
                    if (chave == null || chave.isEmpty()) continue;
                } else {
                    chave = resolverCategoria(item);
                }
                double[] dados = mapa.getOrDefault(chave, new double[]{0, 0});
                dados[0] += item.getQuantidade();
                dados[1] += item.getPrecoUnitario() * item.getQuantidade();
                mapa.put(chave, dados);
            }
        }

        List<Map.Entry<String, double[]>> entries = new ArrayList<>(mapa.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));

        List<TodosProdutosVendasAdapter.ProdutoItem> itens = new ArrayList<>();
        for (Map.Entry<String, double[]> e : entries) {
            itens.add(new TodosProdutosVendasAdapter.ProdutoItem(
                    e.getKey(), (int) e.getValue()[0], e.getValue()[1]));
        }

        listaCompletaAdapter.atualizar(itens);

        if (listaExibindoProdutos) {
            txtListaCompletaTitulo.setText("Todos os produtos");
            txtListaCompletaColunaLabel.setText("Produto");
            txtListaCompletaContagem.setText(itens.size() + (itens.size() == 1 ? " produto" : " produtos"));
        } else {
            txtListaCompletaTitulo.setText("Todas as categorias");
            txtListaCompletaColunaLabel.setText("Categoria");
            txtListaCompletaContagem.setText(itens.size() + (itens.size() == 1 ? " categoria" : " categorias"));
        }
    }

    private void atualizarVisualToggleLista() {
        int corInativo = ContextCompat.getColor(requireContext(), R.color.cor_texto_secundario);
        if (listaExibindoProdutos) {
            btnListaCompletaProdutos.setBackgroundResource(R.drawable.bg_aba_ativa);
            btnListaCompletaProdutos.setTextColor(Color.WHITE);
            btnListaCompletaCategorias.setBackgroundResource(0);
            btnListaCompletaCategorias.setTextColor(corInativo);
        } else {
            btnListaCompletaCategorias.setBackgroundResource(R.drawable.bg_aba_ativa);
            btnListaCompletaCategorias.setTextColor(Color.WHITE);
            btnListaCompletaProdutos.setBackgroundResource(0);
            btnListaCompletaProdutos.setTextColor(corInativo);
        }
    }

    private void renderizarTop5(List<VendaModel> doMes) {
        Map<String, double[]> rankMap = new LinkedHashMap<>();
        for (VendaModel v : doMes) {
            if (v.getItens() == null) continue;
            for (ItemVendaRegistradaModel item : v.getItens()) {
                // Chave: nome do produto OU categoria, conforme aba ativa
                String chave;
                if (exibindoProdutos) {
                    chave = resolverNome(item);
                } else {
                    chave = resolverCategoria(item);
                }
                if (chave == null) continue;
                double[] dados = rankMap.getOrDefault(chave, new double[]{0, 0});
                dados[0] += item.getQuantidade();
                dados[1] += item.getPrecoUnitario() * item.getQuantidade();
                rankMap.put(chave, dados);
            }
        }

        List<Map.Entry<String, double[]>> entries = new ArrayList<>(rankMap.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));
        if (entries.size() > 5) entries = entries.subList(0, 5);

        double totalItens = 0;
        for (Map.Entry<String, double[]> e : entries) totalItens += e.getValue()[1];

        List<TopProdutosVendasAdapter.TopItemVenda> topItens = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, double[]> e = entries.get(i);
            int pctItem = totalItens > 0 ? (int) ((e.getValue()[1] / totalItens) * 100) : 0;
            topItens.add(new TopProdutosVendasAdapter.TopItemVenda(
                    i + 1, e.getKey(), (int) e.getValue()[0], e.getValue()[1], pctItem));
        }
        topAdapter.atualizar(topItens);
    }

    private void mostrarDialogPagamentos() {
        if (doMesCache == null || doMesCache.isEmpty()) return;

        // Agrega por forma de pagamento: contagem e valor total
        Map<String, Integer> contagemPag = new LinkedHashMap<>();
        Map<String, Double> valorPag = new LinkedHashMap<>();
        for (VendaModel v : doMesCache) {
            String pag = v.getFormaPagamento();
            if (pag == null || pag.isEmpty()) pag = "Não informado";
            contagemPag.put(pag, contagemPag.getOrDefault(pag, 0) + 1);
            valorPag.put(pag, valorPag.getOrDefault(pag, 0.0) + v.getValorTotal());
        }

        // Ordena do maior para o menor (por contagem)
        List<String> metodos = new ArrayList<>(contagemPag.keySet());
        metodos.sort((a, b) -> contagemPag.get(b) - contagemPag.get(a));

        int totalQtd = doMesCache.size();
        double totalValor = 0;
        for (double v : valorPag.values()) totalValor += v;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pagamentos_mes_vendas, null);

        TextView txtMes            = dialogView.findViewById(R.id.txtPagDialogMes);
        LinearLayout layoutItens   = dialogView.findViewById(R.id.layoutPagamentosDialogItens);
        TextView txtTotalQtd       = dialogView.findViewById(R.id.txtPagDialogTotalQtd);
        TextView txtTotalValor     = dialogView.findViewById(R.id.txtPagDialogTotalValor);
        MaterialButton btnFechar   = dialogView.findViewById(R.id.btnFecharDialogPagamentos);

        txtMes.setText(fmtMes.format(mesSelecionado.getTime()).toUpperCase(new Locale("pt", "BR")));
        txtTotalQtd.setText(String.valueOf(totalQtd));
        txtTotalValor.setText(fmt.format(totalValor));

        for (String metodo : metodos) {
            int count = contagemPag.get(metodo);
            double valor = valorPag.get(metodo);
            int pct = totalQtd > 0 ? (count * 100) / totalQtd : 0;
            layoutItens.addView(criarLinhaPagamentoDialog(metodo, count, pct, valor));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnFechar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private View criarLinhaPagamentoDialog(String metodo, int count, int pct, double valor) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        int dp8 = (int) (8 * density);
        int dp10 = (int) (10 * density);

        LinearLayout linha = new LinearLayout(requireContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setPadding(0, dp10, 0, dp10);

        // Nome da forma de pagamento
        TextView txtMetodo = new TextView(requireContext());
        LinearLayout.LayoutParams pNome =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        txtMetodo.setLayoutParams(pNome);
        txtMetodo.setText(metodo);
        txtMetodo.setTextSize(14f);
        txtMetodo.setTextColor(ContextCompat.getColor(requireContext(), R.color.cor_texto));
        txtMetodo.setMaxLines(1);
        txtMetodo.setEllipsize(TextUtils.TruncateAt.END);

        // Quantidade
        TextView txtCount = new TextView(requireContext());
        LinearLayout.LayoutParams pCount =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        pCount.leftMargin = dp8;
        txtCount.setLayoutParams(pCount);
        txtCount.setText(String.valueOf(count));
        txtCount.setTextSize(14f);
        txtCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.cor_texto_secundario));
        txtCount.setMinWidth((int) (32 * density));
        txtCount.setGravity(Gravity.END);

        // Percentual
        TextView txtPct = new TextView(requireContext());
        LinearLayout.LayoutParams pPct =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        pPct.leftMargin = dp8;
        txtPct.setLayoutParams(pPct);
        txtPct.setText(pct + "%");
        txtPct.setTextSize(13f);
        txtPct.setTextColor(ContextCompat.getColor(requireContext(), R.color.cor_texto_secundario));
        txtPct.setMinWidth((int) (36 * density));
        txtPct.setGravity(Gravity.END);

        // Valor total
        TextView txtValor = new TextView(requireContext());
        LinearLayout.LayoutParams pValor =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        pValor.leftMargin = dp8;
        txtValor.setLayoutParams(pValor);
        txtValor.setText(fmt.format(valor));
        txtValor.setTextSize(14f);
        txtValor.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        txtValor.setTypeface(null, Typeface.BOLD);
        txtValor.setMinWidth((int) (88 * density));
        txtValor.setGravity(Gravity.END);

        linha.addView(txtMetodo);
        linha.addView(txtCount);
        linha.addView(txtPct);
        linha.addView(txtValor);
        return linha;
    }

    private String resolverNome(ItemVendaRegistradaModel item) {
        if (item.getItemId() != null) {
            String nome = catalogoNomesMap.get(item.getItemId());
            if (nome != null && !nome.isEmpty()) return nome;
        }
        String fallback = item.getNome();
        return (fallback != null) ? fallback : "";
    }

    private String resolverCategoria(ItemVendaRegistradaModel item) {
        if (item.getItemId() != null) {
            // 1ª opção: categoriaId do produto → nome atual da entidade categoria
            String catId = catalogoCategoriasIdMap.get(item.getItemId());
            if (catId != null) {
                String catNome = categoriaNomesMap.get(catId);
                if (catNome != null && !catNome.isEmpty()) return catNome;
            }
            // 2ª opção: nome de categoria salvo no próprio documento do produto
            String cat = catalogoCategoriasMap.get(item.getItemId());
            if (cat != null && !cat.isEmpty()) return cat;
        }
        String fallback = item.getCategoria();
        return (fallback != null && !fallback.isEmpty()) ? fallback : "Sem categoria";
    }
}