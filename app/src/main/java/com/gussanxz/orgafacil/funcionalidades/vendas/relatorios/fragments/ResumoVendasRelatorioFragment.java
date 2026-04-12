package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private TextView txtPagPrincipal, txtPagPercentual;
    private ImageView btnMesAnterior, btnMesProximo;
    private TextView txtMesAtual;
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

        txtTotalVendido   = view.findViewById(R.id.txtRelVendasTotal);
        txtQtdVendas      = view.findViewById(R.id.txtRelVendasQtd);
        txtTicketMedio    = view.findViewById(R.id.txtRelVendasTicket);
        txtPagPrincipal   = view.findViewById(R.id.txtRelVendasPagPrincipal);
        txtPagPercentual  = view.findViewById(R.id.txtRelVendasPagPercentual);
        btnMesAnterior    = view.findViewById(R.id.btnRelVendasMesAnterior);
        btnMesProximo     = view.findViewById(R.id.btnRelVendasMesProximo);
        txtMesAtual       = view.findViewById(R.id.txtRelVendasMesAtual);
        layoutVazio       = view.findViewById(R.id.layoutRelVendasVazio);
        recyclerTopProdutos = view.findViewById(R.id.recyclerRelVendasTopProdutos);

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

        List<VendaModel> doMes = new ArrayList<>();
        for (VendaModel v : listaCompleta) {
            if (!VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) continue;
            Calendar cal = Calendar.getInstance();
            long ts = v.getDataHoraFechamentoMillis() > 0
                    ? v.getDataHoraFechamentoMillis()
                    : v.getDataHoraAberturaMillis();
            cal.setTimeInMillis(ts);
            if (cal.get(Calendar.YEAR) == anoFiltro && cal.get(Calendar.MONTH) == mesFiltro) {
                doMes.add(v);
            }
        }

        boolean vazio = doMes.isEmpty();
        layoutVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        recyclerTopProdutos.setVisibility(vazio ? View.GONE : View.VISIBLE);
        cardListaCompleta.setVisibility(vazio ? View.GONE : View.VISIBLE);

        if (vazio) {
            txtTotalVendido.setText(fmt.format(0));
            txtQtdVendas.setText("0 vendas");
            txtTicketMedio.setText(fmt.format(0));
            txtPagPrincipal.setText("—");
            txtPagPercentual.setText("");
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

        // Forma de pagamento mais usada
        Map<String, Integer> contagemPag = new HashMap<>();
        for (VendaModel v : doMes) {
            String pag = v.getFormaPagamento();
            if (pag != null) contagemPag.put(pag, contagemPag.getOrDefault(pag, 0) + 1);
        }
        String pagTop = "";
        int maxPag = 0;
        for (Map.Entry<String, Integer> e : contagemPag.entrySet()) {
            if (e.getValue() > maxPag) { maxPag = e.getValue(); pagTop = e.getKey(); }
        }
        int pct = doMes.size() > 0 ? (maxPag * 100) / doMes.size() : 0;
        txtPagPrincipal.setText(pagTop.isEmpty() ? "—" : pagTop);
        txtPagPercentual.setText(pagTop.isEmpty() ? "" : pct + "% das vendas");

        // Salva cache e renderiza conforme aba ativa
        doMesCache = doMes;
        renderizarTop5(doMes);
        renderizarListaCompleta(doMes);
    }

    private void atualizarVisualAbas() {
        if (exibindoProdutos) {
            btnAbaProdutos.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnAbaProdutos.setTextColor(Color.WHITE);
            btnAbaCategorias.setBackgroundResource(0);
            btnAbaCategorias.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            btnAbaCategorias.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnAbaCategorias.setTextColor(Color.WHITE);
            btnAbaProdutos.setBackgroundResource(0);
            btnAbaProdutos.setTextColor(Color.parseColor("#9E9E9E"));
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
        if (listaExibindoProdutos) {
            btnListaCompletaProdutos.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnListaCompletaProdutos.setTextColor(Color.WHITE);
            btnListaCompletaCategorias.setBackgroundResource(0);
            btnListaCompletaCategorias.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            btnListaCompletaCategorias.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnListaCompletaCategorias.setTextColor(Color.WHITE);
            btnListaCompletaProdutos.setBackgroundResource(0);
            btnListaCompletaProdutos.setTextColor(Color.parseColor("#9E9E9E"));
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