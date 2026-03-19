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
    private List<MovimentacaoModel> listaDoMesPassado = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resumo_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textTotalReceitas = view.findViewById(R.id.textTotalReceitas);
        textTotalDespesas = view.findViewById(R.id.textTotalDespesas);
        textSaldoResumo = view.findViewById(R.id.textSaldoResumo);
        pieChartCategorias = view.findViewById(R.id.pieChartCategorias);

        repository = new MovimentacaoRepository();

        recyclerTopMaioresGastos = view.findViewById(R.id.recyclerTopMaioresGastos);
        recyclerTopMaioresGastos.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        topGastosAdapter = new TopGastosAdapter();
        recyclerTopMaioresGastos.setAdapter(topGastosAdapter);

        // Esconde o card de insight até que haja dados reais para exibir
        View cardInsight = view.findViewById(R.id.cardInsight);
        if (cardInsight != null) cardInsight.setVisibility(View.GONE);

        carregarResumoMesAtual();
        configurarVisualDoGrafico();
    }

    // =========================================================================
    // BUG CRÍTICO CORRIGIDO: calcularEAtualizarDashboard()
    //
    // Na refatoração anterior, o loop que acumulava totalReceitas e totalDespesas
    // foi removido junto com o loop de agrupamento. As variáveis ficavam = 0
    // para sempre, fazendo o dashboard mostrar R$ 0,00 em tudo.
    //
    // CORREÇÃO: o loop de totais é restaurado separadamente do agrupamento.
    // agruparDespesasPorCategoria() continua sendo o utilitário compartilhado,
    // mas totalReceitas e totalDespesas são acumulados no loop próprio aqui.
    // =========================================================================
    private void calcularEAtualizarDashboard() {
        long totalReceitas = 0;
        long totalDespesas = 0;

        // LOOP RESTAURADO: acumula receitas e despesas corretamente
        for (MovimentacaoModel mov : listaDoMesAtual) {
            if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                totalReceitas += mov.getValor();
            } else {
                totalDespesas += mov.getValor();
            }
        }

        // Agrupamento de categorias via utilitário compartilhado (não duplica o loop acima)
        Map<String, Long> mapaGastosPorCategoria = agruparDespesasPorCategoria(listaDoMesAtual);

        long saldoAtual = totalReceitas - totalDespesas;

        // Exibição com cores contrastantes
        textTotalReceitas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalReceitas)));
        textTotalDespesas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalDespesas)));
        textSaldoResumo.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(Math.abs(saldoAtual))));

        // Cor do saldo: verde positivo, vermelho negativo, cinza neutro
        if (saldoAtual > 0) {
            textSaldoResumo.setTextColor(Color.parseColor("#43A047"));
        } else if (saldoAtual < 0) {
            textSaldoResumo.setTextColor(Color.parseColor("#E53935"));
        } else {
            textSaldoResumo.setTextColor(Color.parseColor("#757575"));
        }

        // --- TOP 5 MAIORES GASTOS ---
        List<TopGastoDTO> listaRanking = new ArrayList<>();
        if (totalDespesas > 0) {
            List<Map.Entry<String, Long>> entradasGastos =
                    new ArrayList<>(mapaGastosPorCategoria.entrySet());
            java.util.Collections.sort(entradasGastos,
                    (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            int limite = Math.min(entradasGastos.size(), 5);
            for (int i = 0; i < limite; i++) {
                String nome = entradasGastos.get(i).getKey();
                long valor = entradasGastos.get(i).getValue();
                int percentual = (int) Math.round(((double) valor / totalDespesas) * 100);
                listaRanking.add(new TopGastoDTO(nome, valor, percentual));
            }
        }
        topGastosAdapter.atualizarLista(listaRanking);

        gerarInsightsInteligentes(totalDespesas, mapaGastosPorCategoria);

        // --- PIECHART COM PALETA CONTRASTANTE ---
        if (totalDespesas > 0) {
            pieChartCategorias.setTouchEnabled(true);

            List<PieEntry> entradasGrafico = new ArrayList<>();
            for (Map.Entry<String, Long> entry : mapaGastosPorCategoria.entrySet()) {
                float valorParaGrafico = (float) MoedaHelper.centavosParaDouble(entry.getValue());
                entradasGrafico.add(new PieEntry(valorParaGrafico, entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entradasGrafico, "Categorias");
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(8f);

            // Paleta contrastante: cores saturadas e bem distintas entre si
            int[] coresPaleta = {
                    Color.parseColor("#E53935"), // Vermelho intenso
                    Color.parseColor("#1E88E5"), // Azul vibrante
                    Color.parseColor("#43A047"), // Verde saturado
                    Color.parseColor("#FB8C00"), // Laranja forte
                    Color.parseColor("#8E24AA"), // Roxo vívido
                    Color.parseColor("#00ACC1"), // Ciano
                    Color.parseColor("#F4511E"), // Laranja-avermelhado
                    Color.parseColor("#3949AB"), // Índigo
                    Color.parseColor("#00897B"), // Verde-água
                    Color.parseColor("#FFB300"), // Âmbar
            };
            List<Integer> cores = new ArrayList<>();
            for (int c : coresPaleta) cores.add(c);
            dataSet.setColors(cores);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(pieChartCategorias));
            data.setValueTextSize(13f);
            data.setValueTextColor(Color.WHITE);
            data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            pieChartCategorias.setData(data);

            String totalFormatado = MoedaHelper.formatarParaBRL(
                    MoedaHelper.centavosParaDouble(totalDespesas));
            pieChartCategorias.setCenterText("Total\n" + totalFormatado);
            pieChartCategorias.setCenterTextSize(15f);
            pieChartCategorias.setCenterTextColor(Color.parseColor("#E53935"));
            pieChartCategorias.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            pieChartCategorias.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
            pieChartCategorias.invalidate();

        } else {
            // Estado vazio animado
            List<PieEntry> entradasVazias = new ArrayList<>();
            entradasVazias.add(new PieEntry(100f, ""));

            PieDataSet dataSetVazio = new PieDataSet(entradasVazias, "");
            dataSetVazio.setColor(Color.parseColor("#EEEEEE"));
            dataSetVazio.setDrawValues(false);
            dataSetVazio.setSelectionShift(0f);

            PieData dataVazio = new PieData(dataSetVazio);
            pieChartCategorias.setData(dataVazio);
            pieChartCategorias.setCenterText("Uhuul!\nNenhum gasto\neste mês 💰");
            pieChartCategorias.setCenterTextSize(14f);
            pieChartCategorias.setCenterTextColor(Color.parseColor("#43A047"));
            pieChartCategorias.setTouchEnabled(false);
            pieChartCategorias.animateY(1000);
            pieChartCategorias.invalidate();
        }
    }

    // --- LÓGICA DE INSIGHTS ---

    private void gerarInsightsInteligentes(long totalDespesasAtual, Map<String, Long> mapaAtual) {

        // CORREÇÃO: Usando a função estática e utilitária correta!
        Map<String, Long> mapaPassado = agruparDespesasPorCategoria(listaDoMesPassado);

        boolean houveAumentoSignificativo = false;

        for (Map.Entry<String, Long> entry : mapaAtual.entrySet()) {
            String categoria = entry.getKey();
            long valorAtual = entry.getValue();
            long valorPassado = mapaPassado.getOrDefault(categoria, 0L);

            if (valorPassado > 0) {
                double crescimento = ((double) (valorAtual - valorPassado) / valorPassado) * 100;

                if (crescimento >= 20 && valorAtual > 5000) {
                    exibirCardInsight(categoria, (int) crescimento, valorAtual);
                    houveAumentoSignificativo = true;
                    break;
                }
            }
        }

        if (!houveAumentoSignificativo) {
            exibirInsightPositivo();
        }
    }

    private void exibirCardInsight(String categoria, int porcentagem, long valorAtual) {
        if (getView() == null) return;
        View card = getView().findViewById(R.id.cardInsight);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);
        if (card != null && textMensagem != null) {
            String valorFmt = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(valorAtual));
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor("#FFF3E0"));
            textMensagem.setText(String.format(
                    "Atenção! Seus gastos com '%s' subiram %d%% este mês, totalizando %s.",
                    categoria, porcentagem, valorFmt));
        }
    }

    private void exibirInsightPositivo() {
        if (getView() == null) return;
        View card = getView().findViewById(R.id.cardInsight);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);
        if (card != null && textMensagem != null) {
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor("#E8F5E9"));
            textMensagem.setText(
                    "Parabéns! Seus gastos estão sob controle e não houve picos inesperados este mês. Continue assim!");
        }
    }

    private void carregarResumoMesAtual() {
        Calendar cal = Calendar.getInstance();

        // 1. Define o Fim: último dia do mês ATUAL
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date fimAtual = cal.getTime();

        // 2. Define o divisor de águas: primeiro dia do mês ATUAL
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date inicioAtual = cal.getTime();

        // 3. Define o Início real da busca: primeiro dia do mês PASSADO
        cal.add(Calendar.MONTH, -1);
        Date inicioPassado = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioPassado, fimAtual,
                new MovimentacaoRepository.DadosCallback() {

                    @Override
                    public void onSucesso(List<MovimentacaoModel> lista) {
                        if (!isAdded()) return;

                        listaDoMesAtual.clear();
                        listaDoMesPassado.clear();

                        // Mágica local: separamos os dados sem gastar mais queries no Firebase
                        for (MovimentacaoModel m : lista) {
                            if (m.getData_movimentacao() != null) {
                                if (m.getData_movimentacao().toDate().before(inicioAtual)) {
                                    listaDoMesPassado.add(m);
                                } else {
                                    listaDoMesAtual.add(m);
                                }
                            }
                        }

                        calcularEAtualizarDashboard();
                    }

                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarVisualDoGrafico() {
        pieChartCategorias.setNoDataText("Carregando seus gastos...");
        pieChartCategorias.setNoDataTextColor(Color.parseColor("#9E9E9E"));
        pieChartCategorias.setUsePercentValues(true);
        pieChartCategorias.getDescription().setEnabled(false);
        pieChartCategorias.setExtraOffsets(5, 10, 5, 5);
        pieChartCategorias.setDragDecelerationFrictionCoef(0.95f);
        pieChartCategorias.setDrawHoleEnabled(true);
        pieChartCategorias.setHoleColor(Color.WHITE);
        pieChartCategorias.setTransparentCircleColor(Color.WHITE);
        pieChartCategorias.setTransparentCircleAlpha(110);
        pieChartCategorias.setHoleRadius(58f);
        pieChartCategorias.setTransparentCircleRadius(61f);
        pieChartCategorias.setDrawCenterText(true);
        pieChartCategorias.setRotationAngle(0);
        pieChartCategorias.setRotationEnabled(true);
        pieChartCategorias.setHighlightPerTapEnabled(true);

        // Legenda habilitada com estilo limpo
        com.github.mikephil.charting.components.Legend legend = pieChartCategorias.getLegend();
        legend.setEnabled(true);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#424242"));
        legend.setWordWrapEnabled(true);
    }

    /**
     * Agrupa o valor total de despesas por nome de categoria.
     * Estático — sem acoplamento ao Fragment, testável de forma isolada.
     */
    private static Map<String, Long> agruparDespesasPorCategoria(List<MovimentacaoModel> lista) {
        Map<String, Long> mapa = new HashMap<>();
        for (MovimentacaoModel m : lista) {
            if (m.getTipoEnum() != TipoCategoriaContas.DESPESA) continue;
            String nome = (m.getCategoria_nome() != null && !m.getCategoria_nome().isEmpty())
                    ? m.getCategoria_nome() : "Sem Categoria";
            mapa.put(nome, mapa.getOrDefault(nome, 0L) + m.getValor());
        }
        return mapa;
    }
}