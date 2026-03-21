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
    private com.github.mikephil.charting.charts.LineChart lineChartEvolucao;
    private TextView textGastoAtual;
    private TextView textMetaTotalCard;
    private MovimentacaoRepository repository;
    private View viewMarcador;
    private TextView textPercentualMarcador;
    private androidx.constraintlayout.widget.ConstraintLayout layoutProgressoContainer;
    private android.widget.ImageView iconMetaStatus;
    private TextView textDetalheGastoDia;

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

        viewMarcador             = view.findViewById(R.id.viewMarcador);
        textPercentualMarcador   = view.findViewById(R.id.textPercentualMarcador);
        layoutProgressoContainer = view.findViewById(R.id.layoutProgressoContainer);
        iconMetaStatus           = view.findViewById(R.id.iconMetaStatus);
        textMetaStatus     = view.findViewById(R.id.textMetaStatus);
        textMetaRestante   = view.findViewById(R.id.textMetaRestante);
        textMetaProjecao   = view.findViewById(R.id.textMetaProjecao);
        lineChartEvolucao = view.findViewById(R.id.lineChartEvolucao);
        textGastoAtual    = view.findViewById(R.id.textGastoAtual);
        textMetaTotalCard = view.findViewById(R.id.textMetaTotalCard);
        textDetalheGastoDia = view.findViewById(R.id.textDetalheGastoDia); // <--- ADICIONE ESTA LINHA

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

        // 1. Mantemos sua lógica intacta de varredura e agrupamento por dia
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

        // Como removemos o ProgressBar antigo do XML, comentamos para não dar Crash:
        // progressMetaMensal.setProgress(Math.min(percentual, 100));

        int corStatus;
        String mensagemGamificada;

        if (percentual < 60) {
            corStatus = Color.parseColor("#A5D6A7"); // Verde suave do design
            mensagemGamificada = "Progresso Saudável!";
        } else if (percentual <= 80) {
            corStatus = Color.parseColor("#FFE082"); // Amarelo suave
            mensagemGamificada = "Atenção, chegando no limite!";
        } else {
            corStatus = Color.parseColor("#EF9A9A"); // Vermelho suave
            mensagemGamificada = "Alerta vermelho! Pise no freio.";
        }

        textMetaStatus.setText(mensagemGamificada);
        textMetaStatus.setTextColor(corStatus);

        if (iconMetaStatus != null) {
            iconMetaStatus.setColorFilter(corStatus);
        }

        // --- A MÁGICA DA BARRA SEGMENTADA (MOVER O MARCADOR) ---
        if (textPercentualMarcador != null) {
            textPercentualMarcador.setText(percentual + "%");
        }

        if (layoutProgressoContainer != null && viewMarcador != null) {
            // Calcula a posição (bias) de 0.0 até 1.0
            float bias = percentual / 100f;
            if (bias > 1f) bias = 1f; // Trava no final se passar de 100%
            if (bias < 0f) bias = 0f;

            // Move a linha branca (marcador) pelo ConstraintLayout dinamicamente
            androidx.constraintlayout.widget.ConstraintSet set = new androidx.constraintlayout.widget.ConstraintSet();
            set.clone(layoutProgressoContainer);
            set.setHorizontalBias(R.id.viewMarcador, bias);
            set.applyTo(layoutProgressoContainer);
        }

        long restante = metaCentavos - gastoTotalCentavos;
        if (restante >= 0) {
            textMetaRestante.setText("Restante: " +
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(restante)));
            textMetaRestante.setTextColor(Color.parseColor("#BDBDBD")); // Cinza claro do design
        } else {
            textMetaRestante.setText("Estourou em " +
                    MoedaHelper.formatarParaBRL(
                            MoedaHelper.centavosParaDouble(Math.abs(restante))));
            textMetaRestante.setTextColor(Color.parseColor("#EF9A9A")); // Vermelho do design
        }

        long mediaDiaria  = diaAtual > 0 ? gastoTotalCentavos / diaAtual : 0;
        long projecaoFinal = mediaDiaria * totalDiasMes;
        textMetaProjecao.setText("Projeção: "
                + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(projecaoFinal))
                + " até o fim do mês.");

        if (projecaoFinal > metaCentavos) {
            textMetaProjecao.setTextColor(Color.parseColor("#EF9A9A"));
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            textMetaProjecao.setTextColor(Color.parseColor("#BDBDBD"));
            textMetaProjecao.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // --- INÍCIO DA INTEGRAÇÃO COM O NOVO LAYOUT ---

        // 2. Atualizamos os novos campos de texto do Header
        if(textMetaTotalCard != null) {
            textMetaTotalCard.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(metaCentavos)));
        }
        if(textGastoAtual != null) {
            textGastoAtual.setText("Gasto Atual: " + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(gastoTotalCentavos)));
        }

        // 3. Substituímos o BarEntry pelo Entry do LineChart e aplicamos a soma cumulativa em centavos
        List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
        long somaCumulativaCentavos = 0;

        for (int i = 1; i <= totalDiasMes; i++) {
            // Soma o gasto do dia ao total acumulado até aquele dia
            somaCumulativaCentavos += gastosPorDia.getOrDefault(i, 0L);
            float valorEmReais = (float) MoedaHelper.centavosParaDouble(somaCumulativaCentavos);

            // Só plota a linha até o dia atual, para não desenhar uma linha reta até o dia 31
            if (i <= diaAtual) {
                entries.add(new com.github.mikephil.charting.data.Entry(i, valorEmReais));
            }
        }

        // 4. Chamamos o novo método de configuração do gráfico de linha
        configurarGraficoCumulativo(entries, diaAtual, metaCentavos, totalDiasMes);
    }

    private void configurarGraficoCumulativo(List<com.github.mikephil.charting.data.Entry> entries, int diaAtual,
                                             long metaCentavos, int totalDiasMes) {
        // Cores adaptadas para o modo escuro da imagem
        int corTexto = Color.parseColor("#BDBDBD"); // Cinza claro para os eixos
        int corLinha = Color.parseColor("#A5D6A7"); // Verde suave

        com.github.mikephil.charting.data.LineDataSet dataSet = new com.github.mikephil.charting.data.LineDataSet(entries, "Evolução de Gastos");
        dataSet.setColor(corLinha);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#FFFFFF")); // Pontos brancos
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER); // Curva suave
        dataSet.setDrawFilled(true); // Preenchimento abaixo da linha
        dataSet.setFillColor(corLinha);
        dataSet.setFillAlpha(40);

        // --- ESTILO DO CLIQUE (HIGHLIGHT) IGUAL A IMAGEM ---
        dataSet.setDrawHighlightIndicators(true); // Permite a linha de mira
        dataSet.setDrawHorizontalHighlightIndicator(false); // Remove a linha horizontal
        dataSet.setHighLightColor(Color.parseColor("#80FFFFFF")); // Linha vertical branca semi-transparente
        dataSet.setHighlightLineWidth(1.5f);

        com.github.mikephil.charting.data.LineData data = new com.github.mikephil.charting.data.LineData(dataSet);
        lineChartEvolucao.setData(data);

        // Eixo X
        XAxis xAxis = lineChartEvolucao.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(7, false); // Mude de 'true' para 'false'. O 'true' forçava a quebra decimal no zoom!

        // As duas linhas mágicas que resolvem o bug do zoom:
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true); // Trava o zoom para pular de 1 em 1 no mínimo

        // Força o texto a ser desenhado como Inteiro, cortando qualquer ".0" ou ".5"
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        xAxis.setTextColor(corTexto);
        xAxis.setDrawGridLines(false); // Sem grade no fundo, visual mais limpo
        xAxis.setAxisMinimum(1f);
        xAxis.setAxisMaximum(totalDiasMes);

        // Eixo Y
        YAxis yAxisLeft = lineChartEvolucao.getAxisLeft();
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000) return String.format(Locale.getDefault(), "R$%.0fk", value / 1000f);
                return String.format(Locale.getDefault(), "R$%.0f", value);
            }
        });
        yAxisLeft.setTextColor(corTexto);
        yAxisLeft.setGridColor(Color.parseColor("#33FFFFFF")); // Grade muito sutil e clara
        lineChartEvolucao.getAxisRight().setEnabled(false);

        // Linha de Meta Total
        if (metaCentavos > 0) {
            float metaTotalReais = (float) MoedaHelper.centavosParaDouble(metaCentavos);
            LimitLine linhaMeta = new LimitLine(metaTotalReais, "Meta");
            linhaMeta.setLineColor(Color.parseColor("#FFFFFF"));
            linhaMeta.setLineWidth(1.5f);
            linhaMeta.setTextColor(Color.parseColor("#FFFFFF"));
            linhaMeta.enableDashedLine(10f, 10f, 0f);
            yAxisLeft.removeAllLimitLines();
            yAxisLeft.addLimitLine(linhaMeta);
        }

        lineChartEvolucao.getDescription().setEnabled(false);
        lineChartEvolucao.getLegend().setEnabled(false);

        // --- OUVINTE DE CLIQUES PARA ATUALIZAR O TEXTO ---
        lineChartEvolucao.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if(textDetalheGastoDia != null) {
                    int dia = (int) e.getX();
                    float valorEmReais = e.getY();
                    textDetalheGastoDia.setText("Dia " + dia + ": Gasto acumulado de " +
                            MoedaHelper.formatarParaBRL((double) valorEmReais));
                    textDetalheGastoDia.setTextColor(Color.parseColor("#FFFFFF")); // Fica branco quando selecionado
                    textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.BOLD);
                }
            }

            @Override
            public void onNothingSelected() {
                if(textDetalheGastoDia != null) {
                    textDetalheGastoDia.setText("Toque em um ponto do gráfico para ver os detalhes");
                    textDetalheGastoDia.setTextColor(Color.parseColor("#BDBDBD"));
                    textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        });

        lineChartEvolucao.animateX(800);
        lineChartEvolucao.invalidate();
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