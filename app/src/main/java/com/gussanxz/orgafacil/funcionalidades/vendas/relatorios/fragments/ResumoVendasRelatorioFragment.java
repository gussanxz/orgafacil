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

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.adapter.TopProdutosVendasAdapter;

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

    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;
    private List<VendaModel> listaCompleta = new ArrayList<>();

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

        mesSelecionado = Calendar.getInstance();
        atualizarTextMes();

        vendaRepository = new VendaRepository();

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
    public void onStart() {
        super.onStart();
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
    public void onStop() {
        super.onStop();
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

        if (vazio) {
            txtTotalVendido.setText(fmt.format(0));
            txtQtdVendas.setText("0 vendas");
            txtTicketMedio.setText(fmt.format(0));
            txtPagPrincipal.setText("—");
            txtPagPercentual.setText("");
            topAdapter.atualizar(new ArrayList<>());
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

        // Top 5 produtos/serviços
        Map<String, double[]> rankMap = new LinkedHashMap<>();
        for (VendaModel v : doMes) {
            if (v.getItens() == null) continue;
            for (ItemVendaRegistradaModel item : v.getItens()) {
                String nome = item.getNome();
                if (nome == null) continue;
                double[] dados = rankMap.getOrDefault(nome, new double[]{0, 0});
                dados[0] += item.getQuantidade();   // qtd
                dados[1] += item.getPrecoUnitario() * item.getQuantidade(); // valor
                rankMap.put(nome, dados);
            }
        }

        // Ordena por valor total desc, pega top 5
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
                    i + 1, e.getKey(),
                    (int) e.getValue()[0],
                    e.getValue()[1],
                    pctItem
            ));
        }
        topAdapter.atualizar(topItens);
    }
}