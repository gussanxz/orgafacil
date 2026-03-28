package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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

    private static final String TAG = "SimuladorDebug";

    // -------------------------------------------------------------------------
    // Campos existentes
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Campos — Simulador "Posso Comprar?"
    // -------------------------------------------------------------------------
    private EditText editSimuladorGasto;
    private EditText editNumeroParcelas;
    private CheckBox checkParcelar;
    private View layoutResultadoSimulador;
    private View layoutCenario1;
    private View layoutCenario2;
    private View layoutCenario3;
    private TextView textCenario1Titulo;
    private TextView textCenario1Corpo;
    private TextView textCenario2Titulo;
    private TextView textCenario2Corpo;
    private TextView textCenario3Titulo;
    private TextView textCenario3Corpo;

    // Flag de formatação do TextWatcher
    private boolean isFormattingSimulador = false;

    // -------------------------------------------------------------------------
    // Estado compartilhado — começa em -1 para detectar "ainda não carregou"
    // -------------------------------------------------------------------------
    private long gastoTotalCentavos = -1;
    private int diasRestantesNoMes  = 1;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

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
        textMetaStatus           = view.findViewById(R.id.textMetaStatus);
        textMetaRestante         = view.findViewById(R.id.textMetaRestante);
        textMetaProjecao         = view.findViewById(R.id.textMetaProjecao);
        lineChartEvolucao        = view.findViewById(R.id.lineChartEvolucao);
        textGastoAtual           = view.findViewById(R.id.textGastoAtual);
        textMetaTotalCard        = view.findViewById(R.id.textMetaTotalCard);
        textDetalheGastoDia      = view.findViewById(R.id.textDetalheGastoDia);

        editSimuladorGasto       = view.findViewById(R.id.editSimuladorGasto);
        editNumeroParcelas       = view.findViewById(R.id.editNumeroParcelas);
        checkParcelar            = view.findViewById(R.id.checkParcelar);
        layoutResultadoSimulador = view.findViewById(R.id.layoutResultadoSimulador);
        layoutCenario1           = view.findViewById(R.id.layoutCenario1);
        layoutCenario2           = view.findViewById(R.id.layoutCenario2);
        layoutCenario3           = view.findViewById(R.id.layoutCenario3);
        textCenario1Titulo       = view.findViewById(R.id.textCenario1Titulo);
        textCenario1Corpo        = view.findViewById(R.id.textCenario1Corpo);
        textCenario2Titulo       = view.findViewById(R.id.textCenario2Titulo);
        textCenario2Corpo        = view.findViewById(R.id.textCenario2Corpo);
        textCenario3Titulo       = view.findViewById(R.id.textCenario3Titulo);
        textCenario3Corpo        = view.findViewById(R.id.textCenario3Corpo);

        view.findViewById(R.id.layoutEditarMeta)
                .setOnClickListener(v -> abrirDialogEdicaoMeta());

        // TextWatcher — formatação de moeda estilo centavos crescentes
        if (editSimuladorGasto != null) {
            editSimuladorGasto.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormattingSimulador) return;
                    isFormattingSimulador = true;

                    String digits = s.toString().replaceAll("[^\\d]", "");
                    if (digits.length() > 11) digits = digits.substring(digits.length() - 11);

                    long centavos = 0;
                    try { centavos = Long.parseLong(digits); } catch (NumberFormatException ignored) {}

                    String formatted = String.format(Locale.getDefault(), "%,.2f", centavos / 100.0);
                    editSimuladorGasto.setText(formatted);
                    editSimuladorGasto.setSelection(formatted.length());

                    isFormattingSimulador = false;
                }
            });
        }

        if (checkParcelar != null) {
            checkParcelar.setOnCheckedChangeListener((btn, isChecked) -> {
                if (editNumeroParcelas != null) {
                    editNumeroParcelas.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (!isChecked) editNumeroParcelas.setText("");
                }
                executarSimuladorSeSePossivel();
            });
        }

        View btnSimular = view.findViewById(R.id.btnSimular);
        if (btnSimular != null) {
            btnSimular.setOnClickListener(v -> {
                v.bringToFront();
                executarSimulador();
            });
        }

        if (layoutResultadoSimulador != null) {
            layoutResultadoSimulador.setClickable(false);
            layoutResultadoSimulador.setFocusable(false);
        }

        repository = new MovimentacaoRepository();
        carregarDadosPlanejamento();
    }

    // -------------------------------------------------------------------------
    // Carregamento de dados
    // -------------------------------------------------------------------------

    private void carregarDadosPlanejamento() {
        Calendar cal = Calendar.getInstance();
        Date hoje = cal.getTime();
        int diaAtual     = cal.get(Calendar.DAY_OF_MONTH);
        int totalDiasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        diasRestantesNoMes = Math.max(totalDiasMes - diaAtual + 1, 1);

        // Marca como "carregando" — o simulador vai bloquear se tentar antes do callback
        gastoTotalCentavos = -1;

        Log.d(TAG, "carregarDadosPlanejamento()"
                + " diaAtual=" + diaAtual
                + " totalDiasMes=" + totalDiasMes
                + " diasRestantes=" + diasRestantesNoMes);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date inicioMes = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioMes, hoje,
                new MovimentacaoRepository.DadosCallback() {
                    @Override
                    public void onSucesso(List<MovimentacaoModel> lista) {
                        if (!isAdded()) return;

                        // LOG 1 — Quantos registros chegaram?
                        Log.d(TAG, "onSucesso() — movimentações recebidas: " + lista.size());

                        int contDespesas = 0;
                        for (MovimentacaoModel mov : lista) {
                            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                                contDespesas++;
                                Log.d(TAG, "  DESPESA"
                                        + " id=" + mov.getId()
                                        + " valor=" + mov.getValor() + " centavos"
                                        + " data=" + mov.getData_movimentacao());
                            }
                        }
                        Log.d(TAG, "onSucesso() — total de despesas: " + contDespesas);

                        processarPlanejamento(lista, diaAtual, totalDiasMes);
                    }

                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        Log.e(TAG, "onErro() — " + erro);
                        Toast.makeText(requireContext(),
                                "Erro ao carregar planejamento: " + erro,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void processarPlanejamento(List<MovimentacaoModel> lista,
                                       int diaAtual, int totalDiasMes) {
        gastoTotalCentavos = 0;
        Map<Integer, Long> gastosPorDia = new HashMap<>();

        for (MovimentacaoModel mov : lista) {
            if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                gastoTotalCentavos += mov.getValor();
                if (mov.getData_movimentacao() != null) {
                    try {
                        Calendar c = Calendar.getInstance();
                        c.setTime(mov.getData_movimentacao().toDate());
                        int dia = c.get(Calendar.DAY_OF_MONTH);
                        gastosPorDia.put(dia, gastosPorDia.getOrDefault(dia, 0L) + mov.getValor());
                    } catch (Exception e) {
                        Log.w(TAG, "Data corrompida, ignorando agrupamento por dia.");
                    }
                }
            }
        }

        android.content.Context ctx = getContext();
        if (ctx == null) return;

        long metaCentavos = ctx.getSharedPreferences(
                        "OrgaFacilPrefs", android.content.Context.MODE_PRIVATE)
                .getLong("meta_mensal", 300000L);

        // LOG 2 — Estado final que o simulador vai consumir
        Log.d(TAG, "processarPlanejamento()"
                + "\n  gastoTotal=" + gastoTotalCentavos + " centavos"
                + " (" + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(gastoTotalCentavos)) + ")"
                + "\n  meta=" + metaCentavos + " centavos"
                + " (" + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(metaCentavos)) + ")"
                + "\n  diasRestantes=" + diasRestantesNoMes
                + "\n  restante=" + MoedaHelper.formatarParaBRL(
                MoedaHelper.centavosParaDouble(metaCentavos - gastoTotalCentavos)));

        int percentual = 0;
        if (metaCentavos > 0) {
            percentual = (int) ((gastoTotalCentavos * 100) / metaCentavos);
        } else if (gastoTotalCentavos > 0) {
            percentual = 100;
        }

        int corStatus;
        String mensagemGamificada;

        if (percentual < 60) {
            corStatus = Color.parseColor("#A5D6A7");
            mensagemGamificada = "Progresso Saudável!";
        } else if (percentual <= 80) {
            corStatus = Color.parseColor("#FFE082");
            mensagemGamificada = "Atenção, chegando no limite!";
        } else {
            corStatus = Color.parseColor("#EF9A9A");
            mensagemGamificada = "Alerta vermelho! Pise no freio.";
        }

        textMetaStatus.setText(mensagemGamificada);
        textMetaStatus.setTextColor(corStatus);
        if (iconMetaStatus != null) iconMetaStatus.setColorFilter(corStatus);
        if (textPercentualMarcador != null) textPercentualMarcador.setText(percentual + "%");

        if (layoutProgressoContainer != null && viewMarcador != null) {
            float bias = Math.min(Math.max(percentual / 100f, 0f), 1f);
            androidx.constraintlayout.widget.ConstraintSet set =
                    new androidx.constraintlayout.widget.ConstraintSet();
            set.clone(layoutProgressoContainer);
            set.setHorizontalBias(R.id.viewMarcador, bias);
            set.applyTo(layoutProgressoContainer);
        }

        long restante = metaCentavos - gastoTotalCentavos;
        if (restante >= 0) {
            textMetaRestante.setText("Restante: " +
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(restante)));
            textMetaRestante.setTextColor(Color.parseColor("#BDBDBD"));
        } else {
            textMetaRestante.setText("Estourou em " +
                    MoedaHelper.formatarParaBRL(
                            MoedaHelper.centavosParaDouble(Math.abs(restante))));
            textMetaRestante.setTextColor(Color.parseColor("#EF9A9A"));
        }

        long mediaDiaria   = diaAtual > 0 ? gastoTotalCentavos / diaAtual : 0;
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

        if (textMetaTotalCard != null)
            textMetaTotalCard.setText(
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(metaCentavos)));
        if (textGastoAtual != null)
            textGastoAtual.setText("Gasto Atual: " +
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(gastoTotalCentavos)));

        List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
        long somaCumulativaCentavos = 0;
        for (int i = 1; i <= totalDiasMes; i++) {
            somaCumulativaCentavos += gastosPorDia.getOrDefault(i, 0L);
            float valorEmReais = (float) MoedaHelper.centavosParaDouble(somaCumulativaCentavos);
            if (i <= diaAtual)
                entries.add(new com.github.mikephil.charting.data.Entry(i, valorEmReais));
        }

        configurarGraficoCumulativo(entries, diaAtual, metaCentavos, totalDiasMes);
    }

    // -------------------------------------------------------------------------
    // Gráfico
    // -------------------------------------------------------------------------

    private void configurarGraficoCumulativo(
            List<com.github.mikephil.charting.data.Entry> entries,
            int diaAtual, long metaCentavos, int totalDiasMes) {

        int corTexto = Color.parseColor("#BDBDBD");
        int corLinha = Color.parseColor("#A5D6A7");

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(entries, "Evolução de Gastos");
        dataSet.setColor(corLinha);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#FFFFFF"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(corLinha);
        dataSet.setFillAlpha(40);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighLightColor(Color.parseColor("#80FFFFFF"));
        dataSet.setHighlightLineWidth(1.5f);

        com.github.mikephil.charting.data.LineData data =
                new com.github.mikephil.charting.data.LineData(dataSet);
        lineChartEvolucao.setData(data);

        XAxis xAxis = lineChartEvolucao.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(7, false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        xAxis.setTextColor(corTexto);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(1f);
        xAxis.setAxisMaximum(totalDiasMes);

        YAxis yAxisLeft = lineChartEvolucao.getAxisLeft();
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000)
                    return String.format(Locale.getDefault(), "R$%.0fk", value / 1000f);
                return String.format(Locale.getDefault(), "R$%.0f", value);
            }
        });
        yAxisLeft.setTextColor(corTexto);
        yAxisLeft.setGridColor(Color.parseColor("#33FFFFFF"));
        lineChartEvolucao.getAxisRight().setEnabled(false);

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

        lineChartEvolucao.setOnChartValueSelectedListener(
                new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(
                            com.github.mikephil.charting.data.Entry e,
                            com.github.mikephil.charting.highlight.Highlight h) {
                        if (textDetalheGastoDia != null) {
                            int dia = (int) e.getX();
                            float valorEmReais = e.getY();
                            textDetalheGastoDia.setText("Dia " + dia + ": acumulado de " +
                                    MoedaHelper.formatarParaBRL((double) valorEmReais));
                            textDetalheGastoDia.setTextColor(Color.parseColor("#FFFFFF"));
                            textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.BOLD);
                        }
                    }

                    @Override
                    public void onNothingSelected() {
                        if (textDetalheGastoDia != null) {
                            textDetalheGastoDia.setText("Toque em um ponto para ver o detalhe");
                            textDetalheGastoDia.setTextColor(Color.parseColor("#BDBDBD"));
                            textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.NORMAL);
                        }
                    }
                });

        lineChartEvolucao.animateX(800);
        lineChartEvolucao.invalidate();
    }

    // -------------------------------------------------------------------------
    // Simulador "Posso Comprar?"
    // -------------------------------------------------------------------------

    private void executarSimuladorSeSePossivel() {
        if (editSimuladorGasto == null) return;
        String texto = editSimuladorGasto.getText().toString().trim();
        if (!TextUtils.isEmpty(texto)) executarSimulador();
    }

    private void executarSimulador() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        // GUARD — dados ainda não chegaram do repositório
        if (gastoTotalCentavos == -1) {
            mostrarErroSimulador("Aguarde, os dados ainda estão carregando...");
            Log.w(TAG, "executarSimulador() chamado antes de processarPlanejamento() terminar.");
            return;
        }

        resetarCenarios();

        if (editSimuladorGasto == null) return;

        // O campo vem formatado ("1.500,00") — normaliza para parseDouble
        String valorStr = editSimuladorGasto.getText().toString().trim()
                .replace(".", "")
                .replace(",", ".");

        if (TextUtils.isEmpty(valorStr) || valorStr.equals(".")) {
            mostrarErroSimulador("Digite o valor da compra antes de simular.");
            return;
        }

        double valorCompra;
        try {
            valorCompra = Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            mostrarErroSimulador("Valor inválido. Use apenas números.");
            return;
        }

        if (Double.isNaN(valorCompra) || Double.isInfinite(valorCompra) || valorCompra <= 0) {
            mostrarErroSimulador("O valor da compra deve ser maior que zero.");
            return;
        }

        if (valorCompra > 1_000_000_000.0) {
            mostrarErroSimulador("Valor muito alto. Verifique o número digitado.");
            return;
        }

        boolean parcelar = checkParcelar != null && checkParcelar.isChecked();
        int numeroParcelas = 1;

        if (parcelar) {
            if (editNumeroParcelas == null) {
                parcelar = false;
            } else {
                String parcStr = editNumeroParcelas.getText().toString().trim();
                if (TextUtils.isEmpty(parcStr)) {
                    mostrarErroSimulador("Informe o número de parcelas.");
                    return;
                }
                try {
                    numeroParcelas = Integer.parseInt(parcStr);
                } catch (NumberFormatException e) {
                    mostrarErroSimulador("Número de parcelas inválido.");
                    return;
                }
                if (numeroParcelas < 2) {
                    mostrarErroSimulador("Parcelamento requer mínimo de 2 parcelas.");
                    editNumeroParcelas.setText("2");
                    numeroParcelas = 2;
                }
                if (numeroParcelas > 360) {
                    mostrarErroSimulador("Número de parcelas muito alto (máximo: 360).");
                    return;
                }
            }
        }

        long metaCentavos;
        try {
            metaCentavos = ctx.getSharedPreferences("OrgaFacilPrefs",
                            android.content.Context.MODE_PRIVATE)
                    .getLong("meta_mensal", 300000L);
        } catch (Exception e) {
            mostrarErroSimulador("Erro ao ler a meta mensal. Tente novamente.");
            return;
        }

        if (metaCentavos <= 0) {
            mostrarErroSimulador("Defina uma meta mensal antes de simular.");
            return;
        }

        double restante         = MoedaHelper.centavosParaDouble(metaCentavos - gastoTotalCentavos);
        int diasRest            = Math.max(diasRestantesNoMes, 1);
        double limiteDiario     = restante / diasRest;
        double valorMes         = valorCompra / numeroParcelas;
        double restanteApos     = restante - valorMes;
        double limiteDiarioApos = restanteApos / diasRest;

        // LOG 3 — Tudo que o simulador está usando para calcular os cenários
        Log.d(TAG, "executarSimulador()"
                + "\n  valorCompra=" + valorCompra
                + "\n  numeroParcelas=" + numeroParcelas
                + "\n  valorMes=" + valorMes
                + "\n  gastoTotalCentavos=" + gastoTotalCentavos
                + " (" + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(gastoTotalCentavos)) + ")"
                + "\n  metaCentavos=" + metaCentavos
                + " (" + MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(metaCentavos)) + ")"
                + "\n  restante=" + restante
                + "\n  diasRest=" + diasRest
                + "\n  limiteDiario=" + limiteDiario
                + "\n  restanteApos=" + restanteApos
                + "\n  limiteDiarioApos=" + limiteDiarioApos);

        if (layoutResultadoSimulador != null)
            layoutResultadoSimulador.setVisibility(View.VISIBLE);

        exibirCenario1(valorMes, valorCompra, limiteDiario, limiteDiarioApos,
                restante, restanteApos, parcelar);
        exibirCenario2(valorCompra, valorMes, numeroParcelas, parcelar);
        exibirCenario3(valorMes, restanteApos, limiteDiario);
    }

    private void resetarCenarios() {
        if (layoutCenario1 != null) {
            layoutCenario1.setVisibility(View.GONE);
            if (textCenario1Titulo != null) textCenario1Titulo.setText("");
            if (textCenario1Corpo  != null) textCenario1Corpo.setText("");
        }
        if (layoutCenario2 != null) {
            layoutCenario2.setVisibility(View.GONE);
            if (textCenario2Titulo != null) textCenario2Titulo.setText("");
            if (textCenario2Corpo  != null) textCenario2Corpo.setText("");
        }
        if (layoutCenario3 != null) {
            layoutCenario3.setVisibility(View.GONE);
            if (textCenario3Titulo != null) textCenario3Titulo.setText("");
            if (textCenario3Corpo  != null) textCenario3Corpo.setText("");
        }
    }

    private void exibirCenario1(double valorMes, double valorCompra,
                                double limiteDiario, double limiteDiarioApos,
                                double restante, double restanteApos, boolean parcelar) {
        if (layoutCenario1 == null || textCenario1Titulo == null || textCenario1Corpo == null)
            return;

        layoutCenario1.setVisibility(View.VISIBLE);
        String tagParcela = parcelar
                ? " (parcela: " + MoedaHelper.formatarParaBRL(valorMes) + "/mês)"
                : "";

        if (restanteApos >= 0) {
            boolean apertado = limiteDiarioApos <= 5.0;
            if (!apertado) {
                textCenario1Titulo.setText("✅ Cabe no orçamento");
                textCenario1Titulo.setTextColor(Color.parseColor("#A5D6A7"));
                layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_verde);
                textCenario1Corpo.setText(
                        "Seu limite diário cai de "
                                + MoedaHelper.formatarParaBRL(limiteDiario)
                                + " para " + MoedaHelper.formatarParaBRL(limiteDiarioApos)
                                + "/dia" + tagParcela + ". Você ainda tem espaço.");
            } else {
                textCenario1Titulo.setText("⚠️ Cabe, mas vai apertar");
                textCenario1Titulo.setTextColor(Color.parseColor("#FFE082"));
                layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_amarelo);
                textCenario1Corpo.setText(
                        "Seu limite diário cai para apenas "
                                + MoedaHelper.formatarParaBRL(Math.max(limiteDiarioApos, 0))
                                + "/dia" + tagParcela + ". Vai ficar bem justo até o fim do mês.");
            }
        } else {
            textCenario1Titulo.setText("🚫 Orçamento insuficiente");
            textCenario1Titulo.setTextColor(Color.parseColor("#EF9A9A"));
            layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_vermelho);
            textCenario1Corpo.setText(
                    "Seu saldo restante é "
                            + MoedaHelper.formatarParaBRL(restante)
                            + tagParcela + ". A compra estoura em "
                            + MoedaHelper.formatarParaBRL(Math.abs(restanteApos)) + ".");
        }
    }

    private void exibirCenario2(double valorCompra, double valorMes,
                                int numeroParcelas, boolean parcelar) {
        if (layoutCenario2 == null || textCenario2Titulo == null || textCenario2Corpo == null)
            return;

        if (!parcelar || numeroParcelas < 2) {
            layoutCenario2.setVisibility(View.GONE);
            return;
        }

        layoutCenario2.setVisibility(View.VISIBLE);
        layoutCenario2.setBackgroundResource(R.drawable.bg_posso_comprar_amarelo);

        int parcelasFuturas = numeroParcelas - 1;
        String sufixoMeses  = parcelasFuturas == 1 ? "mês" : "meses";

        textCenario2Titulo.setText("💳 Atenção ao parcelamento");
        textCenario2Titulo.setTextColor(Color.parseColor("#FFE082"));
        textCenario2Corpo.setText(
                MoedaHelper.formatarParaBRL(valorMes) + "/mês por " + numeroParcelas
                        + "x = " + MoedaHelper.formatarParaBRL(valorCompra) + " no total. "
                        + "Você compromete os próximos " + parcelasFuturas + " " + sufixoMeses
                        + ". Parcelar não é desconto.");
    }

    private void exibirCenario3(double valorMes, double restanteApos, double limiteDiario) {
        if (layoutCenario3 == null || textCenario3Titulo == null || textCenario3Corpo == null)
            return;

        if (restanteApos >= 0) {
            layoutCenario3.setVisibility(View.GONE);
            return;
        }

        layoutCenario3.setVisibility(View.VISIBLE);
        layoutCenario3.setBackgroundResource(R.drawable.bg_posso_comprar_azul);
        textCenario3Titulo.setText("💡 Como chegar lá");
        textCenario3Titulo.setTextColor(Color.parseColor("#82B4E8"));

        double falta = Math.abs(restanteApos);
        String plano;

        if (limiteDiario <= 0) {
            plano = "Seu limite diário já está esgotado. Revise a meta ou adie para o próximo mês.";
        } else {
            int diasEspera = (int) Math.ceil(falta / limiteDiario);
            if (diasEspera >= diasRestantesNoMes) {
                plano = "Mesmo guardando tudo, precisaria de " + diasEspera
                        + " dias — só restam " + diasRestantesNoMes + ". Planeje para o próximo mês.";
            } else {
                String sufixo = diasEspera == 1 ? "dia" : "dias";
                plano = "Guarde seu limite diário por " + diasEspera + " " + sufixo
                        + " e a compra estará dentro do orçamento.";
            }
        }

        textCenario3Corpo.setText(
                "Faltam " + MoedaHelper.formatarParaBRL(falta) + " para fechar a conta. " + plano);
    }

    private void mostrarErroSimulador(String mensagem) {
        boolean exibidoInline = false;

        if (layoutResultadoSimulador != null
                && textCenario1Titulo != null
                && textCenario1Corpo != null
                && layoutCenario1 != null) {

            layoutResultadoSimulador.setVisibility(View.VISIBLE);
            layoutCenario1.setVisibility(View.VISIBLE);
            layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_vermelho);
            textCenario1Titulo.setText("Atenção");
            textCenario1Titulo.setTextColor(Color.parseColor("#EF9A9A"));
            textCenario1Corpo.setText(mensagem);

            if (layoutCenario2 != null) layoutCenario2.setVisibility(View.GONE);
            if (layoutCenario3 != null) layoutCenario3.setVisibility(View.GONE);

            exibidoInline = true;
        }

        if (!exibidoInline) {
            android.content.Context ctx = getContext();
            if (ctx != null) Toast.makeText(ctx, mensagem, Toast.LENGTH_LONG).show();
        }
    }

    // -------------------------------------------------------------------------
    // Dialog de edição de meta
    // -------------------------------------------------------------------------

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
                    String valorDigitado = input.getText().toString().trim();
                    if (!valorDigitado.isEmpty()) {
                        try {
                            double valorDouble = Double.parseDouble(
                                    valorDigitado.replace(",", "."));

                            if (valorDouble <= 0) {
                                Toast.makeText(getContext(),
                                        "A meta deve ser maior que zero.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (valorDouble > 1_000_000_000.0) {
                                Toast.makeText(getContext(),
                                        "Valor muito alto. Verifique o número.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long valorEmCentavos = (long) (valorDouble * 100);
                            android.content.Context ctx = getContext();
                            if (ctx == null) return;

                            ctx.getSharedPreferences("OrgaFacilPrefs",
                                            android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("meta_mensal", valorEmCentavos)
                                    .apply();

                            Log.d(TAG, "Meta salva: " + valorEmCentavos + " centavos ("
                                    + MoedaHelper.formatarParaBRL(valorDouble) + ")");

                            carregarDadosPlanejamento();

                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(),
                                    "Valor inválido. Use apenas números (ex: 1500.00).",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Digite um valor para a meta.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}