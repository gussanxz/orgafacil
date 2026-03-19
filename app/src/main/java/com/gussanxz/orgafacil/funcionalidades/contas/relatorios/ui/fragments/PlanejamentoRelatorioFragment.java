package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanejamentoRelatorioFragment extends Fragment {

    private ProgressBar progressMetaMensal;
    private TextView textMetaStatus, textMetaRestante, textMetaProjecao;
    private BarChart barChartDias;
    private MovimentacaoRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planejamento_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressMetaMensal = view.findViewById(R.id.progressMetaMensal);
        textMetaStatus     = view.findViewById(R.id.textMetaStatus);
        textMetaRestante   = view.findViewById(R.id.textMetaRestante);
        textMetaProjecao   = view.findViewById(R.id.textMetaProjecao);
        barChartDias       = view.findViewById(R.id.barChartDias);

        view.findViewById(R.id.layoutEditarMeta).setOnClickListener(v -> abrirDialogEdicaoMeta());

        repository = new MovimentacaoRepository();
        carregarDadosPlanejamento();
    }

    private void carregarDadosPlanejamento() {
        Calendar cal = Calendar.getInstance();
        Date hoje = cal.getTime();
        int diaAtual     = cal.get(Calendar.DAY_OF_MONTH);
        int totalDiasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date inicioMes = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioMes, hoje,
                new MovimentacaoRepository.DadosCallback() {
                    @Override
                    public void onSucesso(List<MovimentacaoModel> lista) {
                        if (!isAdded()) return;
                        processarPlanejamento(lista, diaAtual, totalDiasMes);
                    }
                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Erro ao carregar planejamento: " + erro,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void processarPlanejamento(List<MovimentacaoModel> lista,
                                       int diaAtual, int totalDiasMes) {
        long gastoTotalCentavos = 0;
        Map<Integer, Long> gastosPorDia = new HashMap<>();

        for (MovimentacaoModel mov : lista) {
            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                gastoTotalCentavos += mov.getValor();
                if (mov.getData_movimentacao() != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(mov.getData_movimentacao().toDate());
                    int dia = c.get(Calendar.DAY_OF_MONTH);
                    gastosPorDia.put(dia, gastosPorDia.getOrDefault(dia, 0L) + mov.getValor());
                }
            }
        }

        // BUG 8 CORRIGIDO: getContext() em vez de requireActivity()
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        long metaCentavos = ctx.getSharedPreferences(
                        "OrgaFacilPrefs", android.content.Context.MODE_PRIVATE)
                .getLong("meta_mensal", 300000L);

        // Blindagem contra divisão por zero
        int percentual = 0;
        if (metaCentavos > 0) {
            percentual = (int) ((gastoTotalCentavos * 100) / metaCentavos);
        } else if (gastoTotalCentavos > 0) {
            percentual = 100;
        }

        // BUG 7 CORRIGIDO: setProgress chamado apenas uma vez
        progressMetaMensal.setProgress(Math.min(percentual, 100));

        int corStatus;
        String mensagemGamificada;

        if (percentual < 50) {
            corStatus = Color.parseColor("#43A047");
            mensagemGamificada = "😎 Tudo sob controle!";
        } else if (percentual <= 80) {
            corStatus = Color.parseColor("#F57C00");
            mensagemGamificada = "⚠️ Atenção, chegando no limite!";
        } else {
            corStatus = Color.parseColor("#E53935");
            mensagemGamificada = "🛑 Alerta vermelho! Pise no freio.";
        }

        progressMetaMensal.setProgressTintList(
                android.content.res.ColorStateList.valueOf(corStatus));

        textMetaStatus.setText(percentual + "% consumido\n" + mensagemGamificada);
        textMetaStatus.setTextColor(corStatus);

        long restante = metaCentavos - gastoTotalCentavos;
        if (restante >= 0) {
            textMetaRestante.setText(
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(restante))
                            + " restantes");
            textMetaRestante.setTextColor(Color.parseColor("#757575"));
        } else {
            textMetaRestante.setText("Estourou em " +
                    MoedaHelper.formatarParaBRL(
                            MoedaHelper.centavosParaDouble(Math.abs(restante))));
            textMetaRestante.setTextColor(Color.parseColor("#E53935"));
        }

        long mediaDiaria  = diaAtual > 0 ? gastoTotalCentavos / diaAtual : 0;
        long projecaoFinal = mediaDiaria * totalDiasMes;
        textMetaProjecao.setText("Projeção: "
                + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(projecaoFinal))
                + " até o fim do mês.");

        if (projecaoFinal > metaCentavos) {
            textMetaProjecao.setTextColor(Color.parseColor("#E53935"));
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            textMetaProjecao.setTextColor(Color.parseColor("#F57C00"));
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 1; i <= totalDiasMes; i++) {
            float valor = (float) MoedaHelper.centavosParaDouble(
                    gastosPorDia.getOrDefault(i, 0L));
            entries.add(new BarEntry(i, valor));
        }

        configurarGraficoDiario(entries, diaAtual, metaCentavos, totalDiasMes);
    }

    private void configurarGraficoDiario(List<BarEntry> entries, int diaAtual,
                                         long metaCentavos, int totalDiasMes) {
        // PONTO 2: cor_texto via ContextCompat para visibilidade em temas claro/escuro
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);

        BarDataSet dataSet = new BarDataSet(entries, "Gastos Diários");
        dataSet.setColor(Color.parseColor("#1E88E5"));
        dataSet.setHighLightColor(Color.parseColor("#FB8C00"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        barChartDias.setData(data);

        // Eixo X: apenas 7 labels para não poluir
        XAxis xAxis = barChartDias.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(7, true);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(corTexto);          // PONTO 2
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0.5f);
        xAxis.setAxisMaximum(totalDiasMes + 0.5f);

        // Eixo Y com formatação BRL e cor_texto
        YAxis yAxisLeft = barChartDias.getAxisLeft();
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) return String.format(Locale.getDefault(), "R$%.0fk", value / 1000f);
                return String.format(Locale.getDefault(), "R$%.0f", value);
            }
        });
        yAxisLeft.setTextColor(corTexto);      // PONTO 2
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setGridColor(Color.parseColor("#EEEEEE"));
        barChartDias.getAxisRight().setEnabled(false);

        // Linha de meta diária tracejada
        if (metaCentavos > 0) {
            float metaDiariaReais = (float) MoedaHelper.centavosParaDouble(
                    metaCentavos / totalDiasMes);
            LimitLine linhaMeta = new LimitLine(metaDiariaReais, "Meta/dia");
            linhaMeta.setLineColor(Color.parseColor("#E53935"));
            linhaMeta.setLineWidth(1.5f);
            linhaMeta.setTextColor(Color.parseColor("#E53935"));
            linhaMeta.setTextSize(10f);
            linhaMeta.enableDashedLine(10f, 5f, 0f);
            yAxisLeft.removeAllLimitLines();
            yAxisLeft.addLimitLine(linhaMeta);
            yAxisLeft.setDrawLimitLinesBehindData(true);
        }

        // Destaca o dia atual
        if (diaAtual >= 1 && diaAtual <= entries.size()) {
            barChartDias.highlightValue(diaAtual, 0, false);
        }

        barChartDias.getDescription().setEnabled(false);
        barChartDias.getLegend().setEnabled(false);
        barChartDias.setFitBars(true);
        barChartDias.setExtraBottomOffset(8f);
        barChartDias.animateY(900);
        barChartDias.invalidate();
    }

    private void abrirDialogEdicaoMeta() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Ex: 3000.00");
        input.setPadding(50, 50, 50, 50);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Definir Meta Mensal")
                .setMessage("Qual o valor máximo que você deseja gastar este mês?")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String valorDigitado = input.getText().toString();
                    if (!valorDigitado.isEmpty()) {
                        try {
                            double valorDouble = Double.parseDouble(
                                    valorDigitado.replace(",", "."));
                            long valorEmCentavos = (long) (valorDouble * 100);

                            // BUG 8 CORRIGIDO: getContext() em vez de requireActivity()
                            android.content.Context ctx = getContext();
                            if (ctx == null) return;

                            ctx.getSharedPreferences("OrgaFacilPrefs",
                                            android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("meta_mensal", valorEmCentavos)
                                    .apply();

                            carregarDadosPlanejamento();

                        } catch (NumberFormatException e) {
                            android.widget.Toast.makeText(getContext(),
                                    "Valor inválido", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}