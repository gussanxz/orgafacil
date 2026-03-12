package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.exportacao.GeradorPDF;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model.TopGastoDTO;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model.TopGastosAdapter;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

public class ResumoRelatorioFragment extends Fragment {

    // Componentes Visuais
    private TextView textTotalReceitas, textTotalDespesas, textSaldoResumo;

    // Dados e Dependências
    private MovimentacaoRepository repository;
    private List<MovimentacaoModel> listaDoMesAtual = new ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView recyclerTopMaioresGastos;
    private TopGastosAdapter topGastosAdapter;
    private PieChart pieChartCategorias;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resumo_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Vinculando os componentes do XML
        textTotalReceitas = view.findViewById(R.id.textTotalReceitas);
        textTotalDespesas = view.findViewById(R.id.textTotalDespesas);
        textSaldoResumo = view.findViewById(R.id.textSaldoResumo);
        pieChartCategorias = view.findViewById(R.id.pieChartCategorias);

        repository = new MovimentacaoRepository();

        recyclerTopMaioresGastos = view.findViewById(R.id.recyclerTopMaioresGastos);
        recyclerTopMaioresGastos.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        topGastosAdapter = new TopGastosAdapter();
        recyclerTopMaioresGastos.setAdapter(topGastosAdapter);

        carregarResumoMesAtual();
        configurarVisualDoGrafico();
    }

    private void calcularEAtualizarDashboard() {
        long totalReceitas = 0;
        long totalDespesas = 0;

        java.util.Map<String, Long> mapaGastosPorCategoria = new java.util.HashMap<>();

        for (MovimentacaoModel mov : listaDoMesAtual) {
            if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                totalReceitas += mov.getValor();
            } else {
                totalDespesas += mov.getValor();

                String nomeCat = mov.getCategoria_nome() != null && !mov.getCategoria_nome().isEmpty()
                        ? mov.getCategoria_nome() : "Sem Categoria";

                long somaAtual = mapaGastosPorCategoria.containsKey(nomeCat) ? mapaGastosPorCategoria.get(nomeCat) : 0L;
                mapaGastosPorCategoria.put(nomeCat, somaAtual + mov.getValor());
            }
        }

        long saldoAtual = totalReceitas - totalDespesas;

        textTotalReceitas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalReceitas)));
        textTotalDespesas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalDespesas)));
        textSaldoResumo.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(saldoAtual)));

        if (saldoAtual >= 0) {
            textSaldoResumo.setTextColor(android.graphics.Color.parseColor("#43A047"));
        } else {
            textSaldoResumo.setTextColor(android.graphics.Color.parseColor("#E53935"));
        }

        // --- LÓGICA DO TOP 5 MAIORES GASTOS ---
        List<TopGastoDTO> listaRanking = new ArrayList<>();
        if (totalDespesas > 0) {
            List<java.util.Map.Entry<String, Long>> entradasGastos = new ArrayList<>(mapaGastosPorCategoria.entrySet());
            java.util.Collections.sort(entradasGastos, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            int limite = Math.min(entradasGastos.size(), 5);
            for (int i = 0; i < limite; i++) {
                String nome = entradasGastos.get(i).getKey();
                long valor = entradasGastos.get(i).getValue();
                int percentual = (int) Math.round(((double) valor / totalDespesas) * 100);
                listaRanking.add(new TopGastoDTO(nome, valor, percentual));
            }
        }
        topGastosAdapter.atualizarLista(listaRanking);

        // 🔥 CORREÇÃO: Acionando os insights inteligentes após o cálculo do dashboard
        gerarInsightsInteligentes(totalDespesas, mapaGastosPorCategoria);

        if (totalDespesas > 0) {

            pieChartCategorias.setTouchEnabled(true); // Reativa o toque caso estivesse desativado

            List<PieEntry> entradasGrafico = new ArrayList<>();

            for (Map.Entry<String, Long> entry : mapaGastosPorCategoria.entrySet()) {
                float valorParaGrafico = (float) MoedaHelper.centavosParaDouble(entry.getValue());
                entradasGrafico.add(new PieEntry(valorParaGrafico, entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entradasGrafico, "Categorias");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            List<Integer> cores = new ArrayList<>();
            for (int c : ColorTemplate.MATERIAL_COLORS) cores.add(c);
            for (int c : ColorTemplate.PASTEL_COLORS) cores.add(c);
            dataSet.setColors(cores);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(pieChartCategorias));
            data.setValueTextSize(12f);
            data.setValueTextColor(android.graphics.Color.WHITE);
            data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            pieChartCategorias.setData(data);

            String totalFormatado = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalDespesas));
            pieChartCategorias.setCenterText("Total\n" + totalFormatado);
            pieChartCategorias.setCenterTextSize(16f);
            pieChartCategorias.setCenterTextColor(android.graphics.Color.parseColor("#E53935"));

            pieChartCategorias.animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
            pieChartCategorias.invalidate();

        } else {
            // 🔥 A MÁGICA FORA DA CAIXA: O ESTADO VAZIO ANIMADO 🔥

            List<PieEntry> entradasVazias = new ArrayList<>();
            // Adicionamos uma única fatia valendo 100% para fechar o círculo
            entradasVazias.add(new PieEntry(100f, ""));

            PieDataSet dataSetVazio = new PieDataSet(entradasVazias, "");
            dataSetVazio.setColor(android.graphics.Color.parseColor("#EEEEEE")); // Um cinza bem suave e neutro
            dataSetVazio.setDrawValues(false); // Esconde o texto de "100%" para não confundir
            dataSetVazio.setSelectionShift(0f); // Tira o pulinho se clicar

            PieData dataVazio = new PieData(dataSetVazio);
            pieChartCategorias.setData(dataVazio);

            // Uma mensagem super positiva no centro!
            pieChartCategorias.setCenterText("Uhuul!\nNenhum gasto\neste mês 💰");
            pieChartCategorias.setCenterTextSize(14f);
            pieChartCategorias.setCenterTextColor(android.graphics.Color.parseColor("#43A047")); // Verde sucesso

            pieChartCategorias.setTouchEnabled(false); // Desativa o toque para não rodar o gráfico vazio

            pieChartCategorias.animateY(1000); // Dá uma animada suave na rosca cinza
            pieChartCategorias.invalidate();
        }
    }

    // --- LÓGICA DE INSIGHTS ---

    private void gerarInsightsInteligentes(long totalDespesasAtual, Map<String, Long> mapaAtual) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date inicioMesPassado = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date fimMesPassado = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioMesPassado, fimMesPassado, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> listaMesPassado) {
                if (!isAdded()) return;

                Map<String, Long> mapaPassado = agruparPorCategoria(listaMesPassado);
                boolean houveAumentoSignificativo = false;

                for (Map.Entry<String, Long> entry : mapaAtual.entrySet()) {
                    String categoria = entry.getKey();
                    long valorAtual = entry.getValue();
                    long valorPassado = mapaPassado.getOrDefault(categoria, 0L);

                    if (valorPassado > 0) {
                        double crescimento = ((double) (valorAtual - valorPassado) / valorPassado) * 100;

                        // Se cresceu mais de 20% e o gasto for maior que R$ 50,00 (5000 centavos)
                        if (crescimento >= 20 && valorAtual > 5000) {
                            exibirCardInsight(categoria, (int) crescimento, valorAtual);
                            houveAumentoSignificativo = true;
                            break; // Mostra apenas o insight mais crítico
                        }
                    }
                }

                if (!houveAumentoSignificativo) {
                    exibirInsightPositivo();
                }
            }

            @Override public void onErro(String erro) {
                // Silencioso para não atrapalhar a UX caso falte dados do mês passado
            }
        });
    }

    private Map<String, Long> agruparPorCategoria(List<MovimentacaoModel> lista) {
        Map<String, Long> mapa = new HashMap<>();
        for (MovimentacaoModel m : lista) {
            if (m.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                mapa.put(m.getCategoria_nome(), mapa.getOrDefault(m.getCategoria_nome(), 0L) + m.getValor());
            }
        }
        return mapa;
    }

    private void exibirCardInsight(String categoria, int porcentagem, long valorAtual) {
        if (getView() == null) return;

        View card = getView().findViewById(R.id.cardInsight);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);

        if (card != null && textMensagem != null) {
            String valorFmt = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(valorAtual));
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor("#FFF3E0"));

            String mensagem = String.format("Atenção! Seus gastos com '%s' subiram %d%% este mês, totalizando %s.",
                    categoria, porcentagem, valorFmt);
            textMensagem.setText(mensagem);
        }
    }

    private void exibirInsightPositivo() {
        if (getView() == null) return;

        View card = getView().findViewById(R.id.cardInsight);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);

        if (card != null && textMensagem != null) {
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor("#E8F5E9"));
            textMensagem.setText("Parabéns! Seus gastos estão sob controle e não houve picos inesperados este mês. Continue assim!");
        }
    }

    private void carregarResumoMesAtual() {

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date inicio = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH,
                cal.getActualMaximum(Calendar.DAY_OF_MONTH));

        Date fim = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicio, fim,
                new MovimentacaoRepository.DadosCallback() {

                    @Override
                    public void onSucesso(List<MovimentacaoModel> lista) {

                        if(!isAdded()) return;

                        listaDoMesAtual = lista;

                        calcularEAtualizarDashboard();
                    }

                    @Override
                    public void onErro(String erro) {

                        if(!isAdded()) return;

                        Toast.makeText(requireContext(),
                                "Erro ao carregar dados",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarVisualDoGrafico() {
        // 1. Mensagem de Loading (Aparece super rápido enquanto busca no banco)
        pieChartCategorias.setNoDataText("Carregando seus gastos...");
        pieChartCategorias.setNoDataTextColor(android.graphics.Color.parseColor("#9E9E9E"));

        // 2. Transformando em "Donut"
        pieChartCategorias.setUsePercentValues(true);
        pieChartCategorias.getDescription().setEnabled(false);
        pieChartCategorias.setExtraOffsets(5, 10, 5, 5);

        pieChartCategorias.setDragDecelerationFrictionCoef(0.95f);

        pieChartCategorias.setDrawHoleEnabled(true);
        pieChartCategorias.setHoleColor(android.graphics.Color.WHITE);
        pieChartCategorias.setTransparentCircleColor(android.graphics.Color.WHITE);
        pieChartCategorias.setTransparentCircleAlpha(110);
        pieChartCategorias.setHoleRadius(58f);
        pieChartCategorias.setTransparentCircleRadius(61f);

        pieChartCategorias.setDrawCenterText(true);
        pieChartCategorias.setRotationAngle(0);
        pieChartCategorias.setRotationEnabled(true);
        pieChartCategorias.setHighlightPerTapEnabled(true);

        pieChartCategorias.getLegend().setEnabled(false);
    }
}