package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments;

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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class EvolucaoVendasFragment extends Fragment {

    private BarChart barChartEvolucao;
    private ChipGroup chipGroupMeses;
    private TextView txtEvolucaoMelhorMes, txtEvolucaoMelhorValor;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;
    private List<VendaModel> listaCompleta = new ArrayList<>();
    private int mesesFiltro = 6;

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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
        barChartEvolucao      = view.findViewById(R.id.barChartEvolucaoVendas);
        chipGroupMeses        = view.findViewById(R.id.chipGroupEvolucaoVendas);
        txtEvolucaoMelhorMes  = view.findViewById(R.id.txtEvolucaoMelhorMes);
        txtEvolucaoMelhorValor = view.findViewById(R.id.txtEvolucaoMelhorValor);
        vendaRepository = new VendaRepository();

        chipGroupMeses.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEv3m)       mesesFiltro = 3;
            else if (id == R.id.chipEv6m)  mesesFiltro = 6;
            else if (id == R.id.chipEv12m) mesesFiltro = 12;
            atualizarGrafico();
        });

        configurarBarChart();
    }

    @Override
    public void onStart() {
        super.onStart();
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
    public void onStop() {
        super.onStop();
        if (listenerRegistration != null) { listenerRegistration.remove(); listenerRegistration = null; }
    }

    private void configurarBarChart() {
        barChartEvolucao.getDescription().setEnabled(false);
        barChartEvolucao.setDrawGridBackground(false);
        barChartEvolucao.setDrawBarShadow(false);
        barChartEvolucao.getLegend().setEnabled(false);
        barChartEvolucao.setTouchEnabled(true);
        barChartEvolucao.setPinchZoom(false);
        barChartEvolucao.setDoubleTapToZoomEnabled(false);

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
                return value >= 1000 ? String.format("R$%.0fk", value / 1000) : String.format("R$%.0f", value);
            }
        });
        barChartEvolucao.getAxisRight().setEnabled(false);
    }

    private void atualizarGrafico() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat fmtLabel = new SimpleDateFormat("MMM", new Locale("pt", "BR"));
        TreeMap<String, Double> mapa = new TreeMap<>();
        String[] labels = new String[mesesFiltro];

        for (int i = mesesFiltro - 1; i >= 0; i--) {
            Calendar ref = (Calendar) cal.clone();
            ref.add(Calendar.MONTH, -i);
            String chave = ref.get(Calendar.YEAR) + "-" + String.format("%02d", ref.get(Calendar.MONTH));
            labels[mesesFiltro - 1 - i] = fmtLabel.format(ref.getTime());
            mapa.put(chave, 0.0);
        }

        for (VendaModel v : listaCompleta) {
            if (!VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) continue;
            long ts = v.getDataHoraFechamentoMillis() > 0 ? v.getDataHoraFechamentoMillis() : v.getDataHoraAberturaMillis();
            Calendar vc = Calendar.getInstance();
            vc.setTimeInMillis(ts);
            String chave = vc.get(Calendar.YEAR) + "-" + String.format("%02d", vc.get(Calendar.MONTH));
            if (mapa.containsKey(chave)) mapa.put(chave, mapa.get(chave) + v.getValorTotal());
        }

        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        double melhorValor = 0;
        int melhorIdx = 0;
        for (Double valor : mapa.values()) {
            entries.add(new BarEntry(idx, valor.floatValue()));
            if (valor > melhorValor) { melhorValor = valor; melhorIdx = idx; }
            idx++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Vendas");
        dataSet.setColor(requireContext().getColor(R.color.colorPrimary));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChartEvolucao.setData(data);

        barChartEvolucao.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartEvolucao.getXAxis().setLabelCount(mesesFiltro);
        barChartEvolucao.invalidate();

        // Melhor mês
        if (melhorValor > 0 && labels.length > melhorIdx) {
            txtEvolucaoMelhorMes.setText("Melhor mês: " + labels[melhorIdx]);
            txtEvolucaoMelhorValor.setText(fmt.format(melhorValor));
        } else {
            txtEvolucaoMelhorMes.setText("Sem dados no período");
            txtEvolucaoMelhorValor.setText("");
        }
    }
}