package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EvolucaoVendasFragment extends Fragment {

    private static final int MODO_DIA = 1;
    private static final int MODO_SEMANA = 2;
    private static final int MODO_15_DIAS = 3;
    private static final int MODO_MES_ATUAL = 4;
    private static final int MODO_INTERVALO = 5;
    private static final int MAX_DIAS_POR_DIA = 31;

    private BarChart barChartEvolucao;
    private ChipGroup chipGroupPeriodo;
    private View layoutDiaData, layoutIntervaloDatas;
    private MaterialButton btnDiaSelecionado, btnDataInicial, btnDataFinal;
    private TextView txtEvolucaoMelhorMes, txtEvolucaoMelhorValor;
    private TextView txtEvTotalPeriodo, txtEvTotalVendas, txtEvMediaMensal, txtEvMediaLabel;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;
    private final List<VendaModel> listaCompleta = new ArrayList<>();
    private int modoFiltro = MODO_SEMANA;
    private long diaSelecionadoMillis;
    private long intervaloInicioMillis;
    private long intervaloFimMillis;

    private final Locale localeBr = new Locale("pt", "BR");
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(localeBr);
    private final SimpleDateFormat fmtBotao = new SimpleDateFormat("dd/MM/yy", localeBr);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evolucao_vendas_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChartEvolucao = view.findViewById(R.id.barChartEvolucaoVendas);
        chipGroupPeriodo = view.findViewById(R.id.chipGroupEvolucaoVendas);
        layoutDiaData = view.findViewById(R.id.layoutEvDiaData);
        layoutIntervaloDatas = view.findViewById(R.id.layoutEvIntervaloDatas);
        btnDiaSelecionado = view.findViewById(R.id.btnEvDiaSelecionado);
        btnDataInicial = view.findViewById(R.id.btnEvDataInicial);
        btnDataFinal = view.findViewById(R.id.btnEvDataFinal);
        txtEvolucaoMelhorMes = view.findViewById(R.id.txtEvolucaoMelhorMes);
        txtEvolucaoMelhorValor = view.findViewById(R.id.txtEvolucaoMelhorValor);
        txtEvTotalPeriodo = view.findViewById(R.id.txtEvTotalPeriodo);
        txtEvTotalVendas = view.findViewById(R.id.txtEvTotalVendas);
        txtEvMediaMensal = view.findViewById(R.id.txtEvMediaMensal);
        txtEvMediaLabel = view.findViewById(R.id.txtEvMediaLabel);
        vendaRepository = new VendaRepository();

        configurarIntervaloPadrao();
        configurarAcoesPeriodo();
        configurarBarChart();
        atualizarBotaoDia();
        atualizarBotoesIntervalo();
    }

    @Override
    public void onResume() {
        super.onResume();
        listenerRegistration = vendaRepository.listarTempoReal(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaCompleta.clear();
                if (lista != null) listaCompleta.addAll(lista);
                atualizarGrafico();
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

    private void configurarAcoesPeriodo() {
        chipGroupPeriodo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEvDia) {
                modoFiltro = MODO_DIA;
            } else if (id == R.id.chipEvSemana) {
                modoFiltro = MODO_SEMANA;
            } else if (id == R.id.chipEv15Dias) {
                modoFiltro = MODO_15_DIAS;
            } else if (id == R.id.chipEvMesAtual) {
                modoFiltro = MODO_MES_ATUAL;
            } else if (id == R.id.chipEvIntervalo) {
                modoFiltro = MODO_INTERVALO;
            }
            atualizarVisibilidadeSeletores();
            atualizarGrafico();
        });

        btnDiaSelecionado.setOnClickListener(v -> abrirDatePickerDia());
        btnDataInicial.setOnClickListener(v -> abrirDatePicker(true));
        btnDataFinal.setOnClickListener(v -> abrirDatePicker(false));
        atualizarVisibilidadeSeletores();
    }

    private void configurarIntervaloPadrao() {
        Calendar dia = Calendar.getInstance();
        aplicarInicioDoDia(dia);
        diaSelecionadoMillis = dia.getTimeInMillis();

        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        aplicarInicioDoDia(inicio);

        Calendar fim = Calendar.getInstance();
        aplicarFimDoDia(fim);

        intervaloInicioMillis = inicio.getTimeInMillis();
        intervaloFimMillis = fim.getTimeInMillis();
    }

    private void configurarBarChart() {
        barChartEvolucao.getDescription().setEnabled(false);
        barChartEvolucao.setDrawGridBackground(false);
        barChartEvolucao.setDrawBarShadow(false);
        barChartEvolucao.getLegend().setEnabled(false);
        barChartEvolucao.setTouchEnabled(true);
        barChartEvolucao.setPinchZoom(false);
        barChartEvolucao.setDoubleTapToZoomEnabled(false);
        barChartEvolucao.setNoDataText("Sem vendas no periodo");

        XAxis xAxis = barChartEvolucao.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis left = barChartEvolucao.getAxisLeft();
        left.setDrawGridLines(true);
        left.setAxisMinimum(0f);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value >= 1000
                        ? String.format(localeBr, "R$%.0fk", value / 1000)
                        : String.format(localeBr, "R$%.0f", value);
            }
        });
        barChartEvolucao.getAxisRight().setEnabled(false);
    }

    private void atualizarGrafico() {
        PeriodoGrafico periodo = montarPeriodoGrafico();
        LinkedHashMap<String, Double> mapaValores = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> mapaQtd = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();

        for (BucketGrafico bucket : periodo.buckets) {
            mapaValores.put(bucket.chave, 0.0);
            mapaQtd.put(bucket.chave, 0);
            labels.add(bucket.label);
        }

        for (VendaModel venda : listaCompleta) {
            if (!VendaModel.STATUS_FINALIZADA.equals(venda.getStatus())) continue;
            long timestamp = obterTimestampVenda(venda);
            if (timestamp < periodo.inicioMillis || timestamp > periodo.fimMillis) continue;

            String chave = periodo.chavePara(timestamp);
            if (mapaValores.containsKey(chave)) {
                mapaValores.put(chave, mapaValores.get(chave) + venda.getValorTotal());
                mapaQtd.put(chave, mapaQtd.get(chave) + 1);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        int melhorIdx = 0;
        double melhorValor = 0;
        double totalPeriodo = 0;
        int totalVendasPeriodo = 0;
        int bucketsComDados = 0;

        for (Map.Entry<String, Double> entry : mapaValores.entrySet()) {
            double valor = entry.getValue();
            entries.add(new BarEntry(index, (float) valor));
            totalPeriodo += valor;
            totalVendasPeriodo += mapaQtd.getOrDefault(entry.getKey(), 0);
            if (valor > 0) bucketsComDados++;
            if (valor > melhorValor) {
                melhorValor = valor;
                melhorIdx = index;
            }
            index++;
        }

        int corPrimaria = requireContext().getColor(R.color.colorPrimary);
        BarDataSet dataSet = new BarDataSet(entries, "Vendas");
        dataSet.setColor(corPrimaria);
        dataSet.setDrawValues(labels.size() <= 10);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value <= 0) return "";
                return value >= 1000
                        ? String.format(localeBr, "%.1fk", value / 1000)
                        : String.format(localeBr, "%.0f", value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChartEvolucao.setData(data);
        barChartEvolucao.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartEvolucao.getXAxis().setLabelCount(Math.min(labels.size(), 8), false);
        barChartEvolucao.invalidate();

        if (melhorValor > 0 && labels.size() > melhorIdx) {
            txtEvolucaoMelhorMes.setText(periodo.melhorPrefixo + ": " + labels.get(melhorIdx));
            txtEvolucaoMelhorValor.setText(fmt.format(melhorValor));
        } else {
            txtEvolucaoMelhorMes.setText("Sem dados no periodo");
            txtEvolucaoMelhorValor.setText("");
        }

        txtEvTotalPeriodo.setText(fmt.format(totalPeriodo));
        txtEvTotalVendas.setText(totalVendasPeriodo + (totalVendasPeriodo == 1 ? " venda" : " vendas"));
        txtEvMediaLabel.setText(periodo.mediaLabel);
        double media = bucketsComDados > 0 ? totalPeriodo / bucketsComDados : 0;
        txtEvMediaMensal.setText(fmt.format(media));
    }

    private PeriodoGrafico montarPeriodoGrafico() {
        if (modoFiltro == MODO_DIA) {
            return montarPeriodoDia();
        }
        if (modoFiltro == MODO_15_DIAS) {
            return montarPeriodo15Dias();
        }
        if (modoFiltro == MODO_MES_ATUAL) {
            return montarPeriodoMesAtual();
        }
        if (modoFiltro == MODO_INTERVALO) {
            return montarPeriodoIntervalo();
        }
        return montarPeriodoSemana();
    }

    private PeriodoGrafico montarPeriodoDia() {
        Calendar inicio = Calendar.getInstance();
        inicio.setTimeInMillis(diaSelecionadoMillis);
        aplicarInicioDoDia(inicio);
        Calendar fim = (Calendar) inicio.clone();
        aplicarFimDoDia(fim);

        PeriodoGrafico periodo = new PeriodoGrafico();
        periodo.inicioMillis = inicio.getTimeInMillis();
        periodo.fimMillis = fim.getTimeInMillis();
        periodo.melhorPrefixo = "Melhor horario";
        periodo.mediaLabel = "Media por horario";

        for (int hora = 0; hora < 24; hora += 4) {
            String chave = String.format(Locale.ROOT, "%02d", hora);
            String label = String.format(Locale.ROOT, "%02dh", hora);
            periodo.buckets.add(new BucketGrafico(chave, label));
        }

        periodo.keyFactory = timestamp -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            int bloco = (cal.get(Calendar.HOUR_OF_DAY) / 4) * 4;
            return String.format(Locale.ROOT, "%02d", bloco);
        };
        return periodo;
    }

    private PeriodoGrafico montarPeriodo15Dias() {
        Calendar inicio = Calendar.getInstance();
        aplicarInicioDoDia(inicio);
        inicio.add(Calendar.DAY_OF_MONTH, -14);

        Calendar fim = Calendar.getInstance();
        aplicarFimDoDia(fim);

        return montarPeriodoPorDia(inicio, fim, "Melhor dia", "Media por dia");
    }

    private PeriodoGrafico montarPeriodoMesAtual() {
        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        aplicarInicioDoDia(inicio);

        Calendar fim = Calendar.getInstance();
        aplicarFimDoDia(fim);

        return montarPeriodoPorDia(inicio, fim, "Melhor dia", "Media por dia");
    }

    private PeriodoGrafico montarPeriodoSemana() {
        Calendar inicio = Calendar.getInstance();
        aplicarInicioDoDia(inicio);
        inicio.set(Calendar.DAY_OF_WEEK, inicio.getFirstDayOfWeek());

        Calendar fim = (Calendar) inicio.clone();
        fim.add(Calendar.DAY_OF_MONTH, 6);
        aplicarFimDoDia(fim);

        return montarPeriodoPorDia(inicio, fim, "Melhor dia", "Media por dia");
    }

    private PeriodoGrafico montarPeriodoIntervalo() {
        Calendar inicio = Calendar.getInstance();
        inicio.setTimeInMillis(intervaloInicioMillis);
        aplicarInicioDoDia(inicio);

        Calendar fim = Calendar.getInstance();
        fim.setTimeInMillis(intervaloFimMillis);
        aplicarFimDoDia(fim);

        if (fim.before(inicio)) {
            Calendar tmp = inicio;
            inicio = fim;
            fim = tmp;
            aplicarInicioDoDia(inicio);
            aplicarFimDoDia(fim);
            intervaloInicioMillis = inicio.getTimeInMillis();
            intervaloFimMillis = fim.getTimeInMillis();
            atualizarBotoesIntervalo();
        }

        long dias = contarDiasInclusivo(inicio, fim);
        if (dias > MAX_DIAS_POR_DIA) {
            return montarPeriodoPorSemana(inicio, fim);
        }

        return montarPeriodoPorDia(inicio, fim, "Melhor dia", "Media por dia");
    }

    private PeriodoGrafico montarPeriodoPorDia(Calendar inicio, Calendar fim, String melhorPrefixo, String mediaLabel) {
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyyMMdd", Locale.ROOT);
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", localeBr);
        PeriodoGrafico periodo = new PeriodoGrafico();
        periodo.inicioMillis = inicio.getTimeInMillis();
        periodo.fimMillis = fim.getTimeInMillis();
        periodo.melhorPrefixo = melhorPrefixo;
        periodo.mediaLabel = mediaLabel;

        Calendar cursor = (Calendar) inicio.clone();
        while (!cursor.after(fim)) {
            periodo.buckets.add(new BucketGrafico(
                    keyFormat.format(cursor.getTime()),
                    labelFormat.format(cursor.getTime())
            ));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        periodo.keyFactory = timestamp -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return keyFormat.format(cal.getTime());
        };
        return periodo;
    }

    private PeriodoGrafico montarPeriodoPorSemana(Calendar inicio, Calendar fim) {
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", localeBr);
        PeriodoGrafico periodo = new PeriodoGrafico();
        periodo.inicioMillis = inicio.getTimeInMillis();
        periodo.fimMillis = fim.getTimeInMillis();
        periodo.melhorPrefixo = "Melhor semana";
        periodo.mediaLabel = "Media por semana";

        Calendar cursor = (Calendar) inicio.clone();
        while (!cursor.after(fim)) {
            Calendar fimSemana = (Calendar) cursor.clone();
            fimSemana.add(Calendar.DAY_OF_MONTH, 6);
            if (fimSemana.after(fim)) {
                fimSemana = (Calendar) fim.clone();
            }

            periodo.buckets.add(new BucketGrafico(
                    String.valueOf(cursor.getTimeInMillis()),
                    labelFormat.format(cursor.getTime()) + "-" + labelFormat.format(fimSemana.getTime())
            ));
            cursor.add(Calendar.DAY_OF_MONTH, 7);
        }

        periodo.keyFactory = timestamp -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            Calendar semana = (Calendar) inicio.clone();
            while (!semana.after(fim)) {
                Calendar fimSemana = (Calendar) semana.clone();
                fimSemana.add(Calendar.DAY_OF_MONTH, 6);
                aplicarFimDoDia(fimSemana);
                if (!cal.before(semana) && !cal.after(fimSemana)) {
                    return String.valueOf(semana.getTimeInMillis());
                }
                semana.add(Calendar.DAY_OF_MONTH, 7);
            }
            return String.valueOf(inicio.getTimeInMillis());
        };
        return periodo;
    }

    private void abrirDatePickerDia() {
        Calendar selecionada = Calendar.getInstance();
        selecionada.setTimeInMillis(diaSelecionadoMillis);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar novaData = Calendar.getInstance();
                    novaData.set(year, month, dayOfMonth);
                    aplicarInicioDoDia(novaData);
                    diaSelecionadoMillis = novaData.getTimeInMillis();
                    atualizarBotaoDia();
                    atualizarGrafico();
                },
                selecionada.get(Calendar.YEAR),
                selecionada.get(Calendar.MONTH),
                selecionada.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void abrirDatePicker(boolean dataInicial) {
        Calendar selecionada = Calendar.getInstance();
        selecionada.setTimeInMillis(dataInicial ? intervaloInicioMillis : intervaloFimMillis);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar novaData = Calendar.getInstance();
                    novaData.set(year, month, dayOfMonth);
                    if (dataInicial) {
                        aplicarInicioDoDia(novaData);
                        intervaloInicioMillis = novaData.getTimeInMillis();
                    } else {
                        aplicarFimDoDia(novaData);
                        intervaloFimMillis = novaData.getTimeInMillis();
                    }
                    atualizarBotoesIntervalo();
                    atualizarGrafico();
                },
                selecionada.get(Calendar.YEAR),
                selecionada.get(Calendar.MONTH),
                selecionada.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void atualizarVisibilidadeSeletores() {
        layoutDiaData.setVisibility(modoFiltro == MODO_DIA ? View.VISIBLE : View.GONE);
        layoutIntervaloDatas.setVisibility(modoFiltro == MODO_INTERVALO ? View.VISIBLE : View.GONE);
    }

    private void atualizarBotaoDia() {
        btnDiaSelecionado.setText("Dia: " + fmtBotao.format(new Date(diaSelecionadoMillis)));
    }

    private void atualizarBotoesIntervalo() {
        btnDataInicial.setText("Inicio: " + fmtBotao.format(new Date(intervaloInicioMillis)));
        btnDataFinal.setText("Fim: " + fmtBotao.format(new Date(intervaloFimMillis)));
    }

    private long obterTimestampVenda(VendaModel venda) {
        return venda.getDataHoraFechamentoMillis() > 0
                ? venda.getDataHoraFechamentoMillis()
                : venda.getDataHoraAberturaMillis();
    }

    private void aplicarInicioDoDia(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void aplicarFimDoDia(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
    }

    private long contarDiasInclusivo(Calendar inicio, Calendar fim) {
        Calendar cursor = (Calendar) inicio.clone();
        long dias = 0;
        while (!cursor.after(fim)) {
            dias++;
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dias;
    }

    private interface KeyFactory {
        String chave(long timestamp);
    }

    private static class BucketGrafico {
        final String chave;
        final String label;

        BucketGrafico(String chave, String label) {
            this.chave = chave;
            this.label = label;
        }
    }

    private static class PeriodoGrafico {
        long inicioMillis;
        long fimMillis;
        String melhorPrefixo;
        String mediaLabel;
        KeyFactory keyFactory;
        final List<BucketGrafico> buckets = new ArrayList<>();

        String chavePara(long timestamp) {
            return keyFactory.chave(timestamp);
        }
    }
}
