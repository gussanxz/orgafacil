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
import com.google.android.material.chip.ChipGroup;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class EvolucaoRelatorioFragment extends Fragment {

    private LineChart lineChartEvolucao;
    private BarChart barChartComparativo;
    private ChipGroup chipGroupFiltroMeses;
    private MovimentacaoRepository repository;
    private int mesesFiltroAtual = 6; // Padrão: 6 meses

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

        lineChartEvolucao = view.findViewById(R.id.lineChartEvolucao);
        barChartComparativo = view.findViewById(R.id.barChartComparativo);
        chipGroupFiltroMeses = view.findViewById(R.id.chipGroupFiltroMeses);

        repository = new MovimentacaoRepository();

        configurarFiltros();
        carregarHistorico();
    }

    private void configurarFiltros() {
        chipGroupFiltroMeses.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chip3m) {
                mesesFiltroAtual = 3;
            } else if (checkedId == R.id.chip6m) {
                mesesFiltroAtual = 6;
            } else if (checkedId == R.id.chip12m) {
                mesesFiltroAtual = 12;
            }
            carregarHistorico();
        });
    }

    private void carregarHistorico() {
        repository.recuperarDadosEvolucao(mesesFiltroAtual, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                if (!isAdded()) return;

                List<MovimentacaoModel> listaFiltrada = new ArrayList<>();
                for(MovimentacaoModel m : lista) {
                    if (m.isPago()) listaFiltrada.add(m);
                }

                processarDadosParaGraficos(listaFiltrada);
            }
            @Override
            public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro ao carregar evolução: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exibirEstadoVazio() {
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);

        lineChartEvolucao.clear();
        lineChartEvolucao.setNoDataText("Nenhuma movimentação neste período.");
        lineChartEvolucao.setNoDataTextColor(corTexto);
        lineChartEvolucao.invalidate();

        barChartComparativo.clear();
        barChartComparativo.setNoDataText("Sem dados para comparativo.");
        barChartComparativo.setNoDataTextColor(corTexto);
        barChartComparativo.invalidate();
    }

    private void processarDadosParaGraficos(List<MovimentacaoModel> lista) {
        Map<String, Long> somaReceitas = new TreeMap<>();
        Map<String, Long> somaDespesas = new TreeMap<>();
        Map<String, String> labelsX = new TreeMap<>();

        SimpleDateFormat chaveFormat = new SimpleDateFormat("yyyyMM", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMM./yy", new Locale("pt", "BR"));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -(mesesFiltroAtual - 1));

        for (int i = 0; i < mesesFiltroAtual; i++) {
            String chave = chaveFormat.format(cal.getTime());
            String label = labelFormat.format(cal.getTime());
            label = label.substring(0, 1).toUpperCase() + label.substring(1);

            labelsX.put(chave, label);
            somaReceitas.put(chave, 0L);
            somaDespesas.put(chave, 0L);

            cal.add(Calendar.MONTH, 1);
        }

        for (MovimentacaoModel mov : lista) {
            if (mov.getData_movimentacao() == null) continue;

            String chave = chaveFormat.format(mov.getData_movimentacao().toDate());

            if (somaReceitas.containsKey(chave)) {
                if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                    somaReceitas.put(chave, somaReceitas.get(chave) + mov.getValor());
                } else if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                    somaDespesas.put(chave, somaDespesas.get(chave) + mov.getValor());
                }
            }
        }

        List<Entry> entriesReceitas = new ArrayList<>();
        List<Entry> entriesDespesas = new ArrayList<>();
        List<BarEntry> entriesBarras = new ArrayList<>();
        List<String> eixoXLabels = new ArrayList<>();

        float maxGastoLinha = 0f;
        float maxGastoBarra = 0f;

        int index = 0;
        for (String chave : somaReceitas.keySet()) {
            float valorRec  = (float) MoedaHelper.centavosParaDouble(somaReceitas.get(chave));
            float valorDesp = (float) MoedaHelper.centavosParaDouble(somaDespesas.get(chave));

            entriesReceitas.add(new Entry(index, valorRec));
            entriesDespesas.add(new Entry(index, valorDesp));
            entriesBarras.add(new BarEntry(index, new float[]{valorRec, valorDesp}));

            if (valorRec > maxGastoLinha) maxGastoLinha = valorRec;
            if (valorDesp > maxGastoLinha) maxGastoLinha = valorDesp;
            if ((valorRec + valorDesp) > maxGastoBarra) maxGastoBarra = (valorRec + valorDesp);

            eixoXLabels.add(labelsX.get(chave));
            index++;
        }

        float maxYLinha = maxGastoLinha * 1.2f;
        float maxYBarra = maxGastoBarra * 1.2f;

        if (maxYLinha == 0) maxYLinha = 1000f;
        if (maxYBarra == 0) maxYBarra = 1000f;

        configurarLineChart(entriesReceitas, entriesDespesas, eixoXLabels, maxYLinha);
        configurarBarChart(entriesBarras, eixoXLabels, maxYBarra);
    }

    // CORREÇÃO: Formatador inteligente com K, M, B exato ao seu mockup
    private ValueFormatter getSmartBrlFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0f) {
                    return "0"; // Base zerada
                } else if (value >= 1_000_000_000f) {
                    return formatarSufixo(value / 1_000_000_000f, "B");
                } else if (value >= 1_000_000f) {
                    return formatarSufixo(value / 1_000_000f, "M");
                } else if (value >= 1000f) {
                    return formatarSufixo(value / 1000f, "k");
                } else {
                    return String.format(Locale.getDefault(), "R$ %.0f", value);
                }
            }

            private String formatarSufixo(float val, String sufixo) {
                // Mantém 1 casa decimal como no seu print (ex: 6,0B)
                String formatado = String.format(Locale.getDefault(), "%.1f", val);
                return "R$ " + formatado + sufixo;
            }
        };
    }

    private void configurarLineChart(List<Entry> receitas, List<Entry> despesas, List<String> labels, float maxY) {
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);
        int corGrade = Color.parseColor("#33888888");

        LineDataSet setRec = new LineDataSet(receitas, "Receitas");
        setRec.setColor(Color.parseColor("#4CAF50"));
        setRec.setCircleColor(Color.parseColor("#4CAF50"));
        setRec.setCircleHoleColor(Color.parseColor("#252A40"));
        setRec.setLineWidth(3f);
        setRec.setCircleRadius(5f);
        setRec.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setRec.setDrawFilled(true);
        setRec.setFillColor(Color.parseColor("#4CAF50"));
        setRec.setFillAlpha(30);
        setRec.setDrawValues(false);

        LineDataSet setDesp = new LineDataSet(despesas, "Despesas");
        setDesp.setColor(Color.parseColor("#E53935"));
        setDesp.setCircleColor(Color.parseColor("#E53935"));
        setDesp.setCircleHoleColor(Color.parseColor("#252A40"));
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
        xAxis.setTextColor(corTexto);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        // CORREÇÃO: Inclinação de -45 graus no eixo X e offset maior
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setYOffset(5f);
        xAxis.setLabelCount(labels.size(), false);

        YAxis yAxisLeft = lineChartEvolucao.getAxisLeft();
        yAxisLeft.setValueFormatter(getSmartBrlFormatter());
        yAxisLeft.setTextColor(corTexto);
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setGridColor(corGrade);

        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(maxY);
        yAxisLeft.setLabelCount(5, true);

        lineChartEvolucao.getAxisRight().setEnabled(false);
        lineChartEvolucao.getDescription().setEnabled(false);

        com.github.mikephil.charting.components.Legend legend = lineChartEvolucao.getLegend();
        legend.setTextSize(14f);
        legend.setTextColor(corTexto);
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);
        legend.setXEntrySpace(20f);
        legend.setYOffset(5f);

        // CORREÇÃO: Aumentado o bottom offset para a legenda em -45 graus caber inteira
        lineChartEvolucao.setExtraBottomOffset(25f);
        lineChartEvolucao.animateX(800);
        lineChartEvolucao.invalidate();
    }

    private void configurarBarChart(List<BarEntry> entries, List<String> labels, float maxY) {
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);
        int corGrade = Color.parseColor("#33888888");

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#E53935")
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
        xAxis.setTextColor(corTexto);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);

        // CORREÇÃO: Inclinação de -45 graus
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setYOffset(5f);
        xAxis.setLabelCount(labels.size(), false);

        YAxis yAxisLeft = barChartComparativo.getAxisLeft();
        yAxisLeft.setValueFormatter(getSmartBrlFormatter());
        yAxisLeft.setTextColor(corTexto);
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setGridColor(corGrade);

        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(maxY);
        yAxisLeft.setLabelCount(5, true);

        barChartComparativo.getAxisRight().setEnabled(false);
        barChartComparativo.getDescription().setEnabled(false);

        com.github.mikephil.charting.components.Legend legend = barChartComparativo.getLegend();
        legend.setTextSize(14f);
        legend.setTextColor(corTexto);
        legend.setXEntrySpace(20f);
        legend.setYOffset(5f);

        // CORREÇÃO: Aumentado o bottom offset
        barChartComparativo.setExtraBottomOffset(25f);
        barChartComparativo.animateY(800);
        barChartComparativo.invalidate();
    }
}