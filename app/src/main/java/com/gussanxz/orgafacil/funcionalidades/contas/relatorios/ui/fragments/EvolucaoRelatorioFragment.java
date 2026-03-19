package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class EvolucaoRelatorioFragment extends Fragment {

    private LineChart lineChartEvolucao;
    private BarChart barChartComparativo;
    private MovimentacaoRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evolucao_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lineChartEvolucao   = view.findViewById(R.id.lineChartEvolucao);
        barChartComparativo = view.findViewById(R.id.barChartComparativo);
        repository = new MovimentacaoRepository();
        carregarHistorico();
    }

    private void carregarHistorico() {
        repository.recuperarDadosEvolucao(6, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                if (!isAdded()) return;
                if (lista.isEmpty()) exibirEstadoVazio();
                else processarDadosParaGraficos(lista);
            }
            @Override
            public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(),
                        "Erro ao carregar evolução: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exibirEstadoVazio() {
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);
        if (lineChartEvolucao != null) {
            lineChartEvolucao.setNoDataText("Nenhuma movimentação nos últimos 6 meses");
            lineChartEvolucao.setNoDataTextColor(corTexto);
            lineChartEvolucao.invalidate();
        }
        if (barChartComparativo != null) {
            barChartComparativo.setNoDataText("Sem dados para comparativo");
            barChartComparativo.setNoDataTextColor(corTexto);
            barChartComparativo.invalidate();
        }
    }

    private void processarDadosParaGraficos(List<MovimentacaoModel> lista) {
        Map<String, Long> somaReceitas = new TreeMap<>();
        Map<String, Long> somaDespesas = new TreeMap<>();

        SimpleDateFormat chaveFormat = new SimpleDateFormat("yyyyMM", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM/yy", Locale.getDefault());
        Map<String, String> labelsX = new HashMap<>();

        for (MovimentacaoModel mov : lista) {
            if (mov.getData_movimentacao() == null) continue;
            String chave = chaveFormat.format(mov.getData_movimentacao().toDate());
            String label = labelFormat.format(mov.getData_movimentacao().toDate());
            labelsX.put(chave, label);

            if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                somaReceitas.put(chave, somaReceitas.getOrDefault(chave, 0L) + mov.getValor());
            } else {
                somaDespesas.put(chave, somaDespesas.getOrDefault(chave, 0L) + mov.getValor());
            }
        }

        List<Entry>    entriesReceitas = new ArrayList<>();
        List<Entry>    entriesDespesas = new ArrayList<>();
        List<BarEntry> entriesBarras  = new ArrayList<>();
        List<String>   eixoXLabels    = new ArrayList<>();

        List<String> chavesOrdenadas = new ArrayList<>(labelsX.keySet());
        Collections.sort(chavesOrdenadas);

        int index = 0;
        for (String chave : chavesOrdenadas) {
            float valorRec  = (float) MoedaHelper.centavosParaDouble(somaReceitas.getOrDefault(chave, 0L));
            float valorDesp = (float) MoedaHelper.centavosParaDouble(somaDespesas.getOrDefault(chave, 0L));

            entriesReceitas.add(new Entry(index, valorRec));
            entriesDespesas.add(new Entry(index, valorDesp));
            entriesBarras.add(new BarEntry(index, new float[]{valorRec, valorDesp}));
            eixoXLabels.add(labelsX.get(chave));
            index++;
        }

        configurarLineChart(entriesReceitas, entriesDespesas, eixoXLabels);
        configurarBarChart(entriesBarras, eixoXLabels);
    }

    // PONTO 2: cor_texto via ContextCompat, usada em todos os textos dos gráficos
    private ValueFormatter getBrlFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) return String.format(Locale.getDefault(), "R$%.0fk", value / 1000f);
                return String.format(Locale.getDefault(), "R$%.0f", value);
            }
        };
    }

    private void configurarLineChart(List<Entry> receitas, List<Entry> despesas,
                                     List<String> labels) {
        // PONTO 2: cor_texto para todos os textos do gráfico
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);

        LineDataSet setRec = new LineDataSet(receitas, "Receitas");
        setRec.setColor(Color.parseColor("#43A047"));
        setRec.setCircleColor(Color.parseColor("#43A047"));
        setRec.setCircleHoleColor(Color.WHITE);
        setRec.setLineWidth(3f);
        setRec.setCircleRadius(5f);
        setRec.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setRec.setDrawFilled(true);
        setRec.setFillColor(Color.parseColor("#43A047"));
        setRec.setFillAlpha(30);
        setRec.setDrawValues(false);

        LineDataSet setDesp = new LineDataSet(despesas, "Despesas");
        setDesp.setColor(Color.parseColor("#E53935"));
        setDesp.setCircleColor(Color.parseColor("#E53935"));
        setDesp.setCircleHoleColor(Color.WHITE);
        setDesp.setLineWidth(3f);
        setDesp.setCircleRadius(5f);
        setDesp.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setDesp.setDrawFilled(true);
        setDesp.setFillColor(Color.parseColor("#E53935"));
        setDesp.setFillAlpha(30);
        setDesp.setDrawValues(false);

        LineData data = new LineData(setRec, setDesp);
        lineChartEvolucao.setData(data);

        XAxis xAxis = lineChartEvolucao.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(corTexto);         // PONTO 2
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        YAxis yAxisLeft = lineChartEvolucao.getAxisLeft();
        yAxisLeft.setValueFormatter(getBrlFormatter());
        yAxisLeft.setTextColor(corTexto);     // PONTO 2
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setGridColor(Color.parseColor("#EEEEEE"));

        lineChartEvolucao.getAxisRight().setEnabled(false);
        lineChartEvolucao.getDescription().setEnabled(false);

        com.github.mikephil.charting.components.Legend legend = lineChartEvolucao.getLegend();
        legend.setTextSize(13f);
        legend.setTextColor(corTexto);        // PONTO 2
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);

        lineChartEvolucao.setExtraBottomOffset(10f);
        lineChartEvolucao.animateX(1000);
        lineChartEvolucao.invalidate();
    }

    private void configurarBarChart(List<BarEntry> entries, List<String> labels) {
        // PONTO 2: cor_texto via ContextCompat
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(
                Color.parseColor("#43A047"),  // Verde = Receita
                Color.parseColor("#E53935")   // Vermelho = Despesa
        );
        set.setStackLabels(new String[]{"Receitas", "Despesas"});
        set.setDrawValues(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.5f);
        barChartComparativo.setData(data);

        XAxis xAxis = barChartComparativo.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(corTexto);         // PONTO 2
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        YAxis yAxisLeft = barChartComparativo.getAxisLeft();
        yAxisLeft.setValueFormatter(getBrlFormatter());
        yAxisLeft.setTextColor(corTexto);     // PONTO 2
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setGridColor(Color.parseColor("#EEEEEE"));

        barChartComparativo.getAxisRight().setEnabled(false);
        barChartComparativo.getDescription().setEnabled(false);

        com.github.mikephil.charting.components.Legend legend = barChartComparativo.getLegend();
        legend.setTextSize(13f);
        legend.setTextColor(corTexto);        // PONTO 2

        barChartComparativo.setExtraBottomOffset(10f);
        barChartComparativo.animateY(1000);
        barChartComparativo.invalidate();
    }
}