package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import java.util.Map;

public class PlanejamentoRelatorioFragment extends Fragment {

    private ProgressBar progressMetaMensal;
    private TextView textMetaStatus, textMetaRestante, textMetaProjecao;
    private BarChart barChartDias;
    private MovimentacaoRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planejamento_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressMetaMensal = view.findViewById(R.id.progressMetaMensal);
        textMetaStatus = view.findViewById(R.id.textMetaStatus);
        textMetaRestante = view.findViewById(R.id.textMetaRestante);
        textMetaProjecao = view.findViewById(R.id.textMetaProjecao);
        barChartDias = view.findViewById(R.id.barChartDias);

        repository = new MovimentacaoRepository();
        carregarDadosPlanejamento();
    }

    private void carregarDadosPlanejamento() {
        Calendar cal = Calendar.getInstance();
        Date hoje = cal.getTime();
        int diaAtual = cal.get(Calendar.DAY_OF_MONTH);
        int totalDiasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date inicioMes = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioMes, hoje, new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                if (isAdded()) processarPlanejamento(lista, diaAtual, totalDiasMes);
            }

            @Override
            public void onErro(String erro) {}
        });
    }

    private void processarPlanejamento(List<MovimentacaoModel> lista, int diaAtual, int totalDiasMes) {
        long gastoTotalCentavos = 0;
        Map<Integer, Long> gastosPorDia = new HashMap<>();

        for (MovimentacaoModel mov : lista) {
            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                gastoTotalCentavos += mov.getValor();

                Calendar c = Calendar.getInstance();
                c.setTime(mov.getData_movimentacao().toDate());
                int dia = c.get(Calendar.DAY_OF_MONTH);
                gastosPorDia.put(dia, gastosPorDia.getOrDefault(dia, 0L) + mov.getValor());
            }
        }

        // 1. Lógica da Meta (Exemplo: Meta fixa de R$ 3000 ou buscar do seu ResumoGeralViewModel)
        long metaCentavos = 300000; // R$ 3.000,00
        int percentual = (int) ((gastoTotalCentavos * 100) / metaCentavos);

        progressMetaMensal.setProgress(Math.min(percentual, 100));
        textMetaStatus.setText(percentual + "% consumido");

        long restante = metaCentavos - gastoTotalCentavos;
        textMetaRestante.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(restante)) + " restantes");

        // 2. Projeção "Fora da Caixa"
        long mediaDiaria = gastoTotalCentavos / diaAtual;
        long projecaoFinal = mediaDiaria * totalDiasMes;
        textMetaProjecao.setText("Projeção: " + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(projecaoFinal)) + " até o fim do mês.");

        if (projecaoFinal > metaCentavos) {
            textMetaProjecao.setTextColor(Color.RED);
        }

        // 3. Gráfico de Barras Diário
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 1; i <= totalDiasMes; i++) {
            float valor = (float) MoedaHelper.centavosParaDouble(gastosPorDia.getOrDefault(i, 0L));
            entries.add(new BarEntry(i, valor));
        }

        configurarGraficoDiario(entries);
    }

    private void configurarGraficoDiario(List<BarEntry> entries) {
        BarDataSet dataSet = new BarDataSet(entries, "Gastos Diários");
        dataSet.setColor(Color.parseColor("#2196F3"));

        BarData data = new BarData(dataSet);
        barChartDias.setData(data);
        barChartDias.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartDias.getXAxis().setLabelCount(10); // Mostra alguns dias para não poluir
        barChartDias.animateY(1000);
        barChartDias.getDescription().setEnabled(false);
        barChartDias.invalidate();
    }
}