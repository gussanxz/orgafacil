package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evolucao_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lineChartEvolucao = view.findViewById(R.id.lineChartEvolucao);
        barChartComparativo = view.findViewById(R.id.barChartComparativo);
        repository = new MovimentacaoRepository();

        carregarHistorico();
    }

    private void carregarHistorico() {
        // Buscamos os últimos 6 meses
        repository.recuperarDadosEvolucao(6, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                if (isAdded()) processarDadosParaGraficos(lista);
            }

            @Override
            public void onErro(String erro) {
                if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processarDadosParaGraficos(List<MovimentacaoModel> lista) {
        // TreeMap mantém a ordem cronológica pelas chaves (ex: 202601, 202602)
        Map<String, Long> somaReceitas = new TreeMap<>();
        Map<String, Long> somaDespesas = new TreeMap<>();

        // Formatos para agrupamento e exibição
        SimpleDateFormat chaveFormat = new SimpleDateFormat("yyyyMM", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        Map<String, String> labelsX = new HashMap<>();

        // 1. Agrupar movimentações por Mês/Ano
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

        // 2. Preparar as entradas para os gráficos
        List<Entry> entriesReceitas = new ArrayList<>();
        List<Entry> entriesDespesas = new ArrayList<>();
        List<BarEntry> entriesBarras = new ArrayList<>();
        List<String> eixoXLabels = new ArrayList<>();

        int index = 0;
        List<String> chavesOrdenadas = new ArrayList<>(labelsX.keySet());
        Collections.sort(chavesOrdenadas);

        for (String chave : chavesOrdenadas) {
            // Convertendo centavos (long) para double usando seu MoedaHelper
            float valorRec = (float) MoedaHelper.centavosParaDouble(somaReceitas.getOrDefault(chave, 0L));
            float valorDesp = (float) MoedaHelper.centavosParaDouble(somaDespesas.getOrDefault(chave, 0L));

            entriesReceitas.add(new Entry(index, valorRec));
            entriesDespesas.add(new Entry(index, valorDesp));
            entriesBarras.add(new BarEntry(index, valorDesp)); // Barra foca em despesas mensais

            eixoXLabels.add(labelsX.get(chave));
            index++;
        }

        configurarLineChart(entriesReceitas, entriesDespesas, eixoXLabels);
        configurarBarChart(entriesBarras, eixoXLabels);
    }

    private void configurarLineChart(List<Entry> receitas, List<Entry> despesas, List<String> labels) {
        LineDataSet setRec = new LineDataSet(receitas, "Receitas");
        setRec.setColor(Color.parseColor("#43A047"));
        setRec.setCircleColor(Color.parseColor("#43A047"));
        setRec.setLineWidth(3f);
        setRec.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Linha suavizada (fora da caixa!)

        LineDataSet setDesp = new LineDataSet(despesas, "Despesas");
        setDesp.setColor(Color.parseColor("#E53935"));
        setDesp.setCircleColor(Color.parseColor("#E53935"));
        setDesp.setLineWidth(3f);
        setDesp.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(setRec, setDesp);
        lineChartEvolucao.setData(data);

        // Estilização do Eixo X
        XAxis xAxis = lineChartEvolucao.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        lineChartEvolucao.getDescription().setEnabled(false);
        lineChartEvolucao.animateX(1000);
        lineChartEvolucao.invalidate();
    }

    private void configurarBarChart(List<BarEntry> entries, List<String> labels) {
        BarDataSet set = new BarDataSet(entries, "Total de Gastos");
        set.setColor(Color.parseColor("#1976D2")); // Azul para destacar comparativo

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        barChartComparativo.setData(data);
        barChartComparativo.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartComparativo.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartComparativo.getDescription().setEnabled(false);
        barChartComparativo.animateY(1000);
        barChartComparativo.invalidate();
    }
}