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

        view.findViewById(R.id.layoutEditarMeta).setOnClickListener(v -> abrirDialogEdicaoMeta());

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

        // 1. Lógica da Meta (Agora dinâmica, lendo do SharedPreferences!)
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("OrgaFacilPrefs", android.content.Context.MODE_PRIVATE);
        long metaCentavos = prefs.getLong("meta_mensal", 300000L);

        // 🔥 CORREÇÃO: Blindagem contra Divisão por Zero 🔥
        int percentual = 0;
        if (metaCentavos > 0) {
            percentual = (int) ((gastoTotalCentavos * 100) / metaCentavos);
        } else if (gastoTotalCentavos > 0) {
            // Se a meta é 0 e o usuário gastou qualquer coisa, ele já estourou 100% da meta
            percentual = 100;
        }

        progressMetaMensal.setProgress(Math.min(percentual, 100));

        progressMetaMensal.setProgress(Math.min(percentual, 100));

        // 🔥 A MÁGICA DA GAMIFICAÇÃO: Cores e Emojis Dinâmicos 🔥
        int corStatus;
        String mensagemGamificada;

        if (percentual < 50) {
            corStatus = Color.parseColor("#43A047"); // Verde (Tranquilo)
            mensagemGamificada = "😎 Tudo sob controle!";
        } else if (percentual <= 80) {
            corStatus = Color.parseColor("#F57C00"); // Laranja (Atenção)
            mensagemGamificada = "⚠️ Atenção, chegando no limite!";
        } else {
            corStatus = Color.parseColor("#E53935"); // Vermelho (Perigo)
            mensagemGamificada = "🛑 Alerta vermelho! Pise no freio.";
        }

        // Aplicando a cor dinamicamente na barra de progresso
        progressMetaMensal.setProgressTintList(android.content.res.ColorStateList.valueOf(corStatus));

        // Atualizando o texto principal com a cor e o emoji
        textMetaStatus.setText(percentual + "% consumido\n" + mensagemGamificada);
        textMetaStatus.setTextColor(corStatus);

        // Tratando o saldo restante ou o valor estourado
        long restante = metaCentavos - gastoTotalCentavos;
        if (restante >= 0) {
            textMetaRestante.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(restante)) + " restantes");
            textMetaRestante.setTextColor(Color.parseColor("#757575")); // Cinza normal
        } else {
            // Se gastou mais que a meta, mostramos quanto estourou (usando Math.abs para tirar o sinal negativo)
            textMetaRestante.setText("Estourou em " + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(Math.abs(restante))));
            textMetaRestante.setTextColor(Color.parseColor("#E53935")); // Vermelho
        }

        // 2. Projeção "Fora da Caixa"
        long mediaDiaria = diaAtual > 0 ? gastoTotalCentavos / diaAtual : 0;
        long projecaoFinal = mediaDiaria * totalDiasMes;
        textMetaProjecao.setText("Projeção: " + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(projecaoFinal)) + " até o fim do mês.");

        if (projecaoFinal > metaCentavos) {
            textMetaProjecao.setTextColor(Color.parseColor("#E53935"));
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            textMetaProjecao.setTextColor(Color.parseColor("#F57C00")); // Laranja para projeção normal
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.NORMAL);
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

    private void abrirDialogEdicaoMeta() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
                            // Converte o que o usuário digitou para DOUBLE, e depois multiplica por 100 para virar LONG (centavos)
                            double valorDouble = Double.parseDouble(valorDigitado.replace(",", "."));
                            long valorEmCentavos = (long) (valorDouble * 100);

                            // Salva no SharedPreferences
                            android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("OrgaFacilPrefs", android.content.Context.MODE_PRIVATE);
                            prefs.edit().putLong("meta_mensal", valorEmCentavos).apply();

                            // Recarrega a tela com o novo valor!
                            carregarDadosPlanejamento();

                        } catch (NumberFormatException e) {
                            android.widget.Toast.makeText(getContext(), "Valor inválido", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}