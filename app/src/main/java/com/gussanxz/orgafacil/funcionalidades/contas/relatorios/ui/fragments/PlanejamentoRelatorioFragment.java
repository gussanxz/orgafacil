package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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

    // -------------------------------------------------------------------------
    // Campos existentes (não removidos)
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
    // Novos campos — Simulador "Posso Comprar?"
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

    // -------------------------------------------------------------------------
    // Estado compartilhado entre carregamento e simulador
    // -------------------------------------------------------------------------
    /**
     * Mantido como campo da classe para que executarSimulador() possa acessar
     * o gasto atual sem uma nova consulta ao repositório.
     */
    private long gastoTotalCentavos = 0;
    private int diasRestantesNoMes  = 1; // padrão seguro para evitar divisão por zero

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

        // --- Bind: campos originais ---
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

        // --- Bind: simulador ---
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

        // --- Listeners ---
        view.findViewById(R.id.layoutEditarMeta)
                .setOnClickListener(v -> abrirDialogEdicaoMeta());

        if (checkParcelar != null) {
            checkParcelar.setOnCheckedChangeListener((btn, isChecked) -> {
                if (editNumeroParcelas != null) {
                    editNumeroParcelas.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (!isChecked) {
                        // Limpa o campo ao desmarcar para não confundir próximo uso
                        editNumeroParcelas.setText("");
                    }
                }
                // Re-simula automaticamente ao alternar o checkbox, se já há valor
                executarSimuladorSeSePossivel();
            });
        }

        View btnSimular = view.findViewById(R.id.btnSimular);
        if (btnSimular != null) {
            btnSimular.setOnClickListener(v -> executarSimulador());
        }

        // --- Repositório e dados ---
        repository = new MovimentacaoRepository();
        carregarDadosPlanejamento();
    }

    // -------------------------------------------------------------------------
    // Carregamento de dados (código original preservado)
    // -------------------------------------------------------------------------

    private void carregarDadosPlanejamento() {
        Calendar cal = Calendar.getInstance();
        Date hoje = cal.getTime();
        int diaAtual     = cal.get(Calendar.DAY_OF_MONTH);
        int totalDiasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Atualiza diasRestantesNoMes para o simulador usar sem nova consulta
        diasRestantesNoMes = Math.max(totalDiasMes - diaAtual + 1, 1);

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
        // Reseta o campo da classe antes de somar
        gastoTotalCentavos = 0;
        Map<Integer, Long> gastosPorDia = new HashMap<>();

        // 1. Mantemos sua lógica intacta de varredura e agrupamento por dia
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
                        // Data corrompida: ignora o agrupamento por dia deste item
                        // mas mantém o valor no total geral
                    }
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
        if (textMetaTotalCard != null) {
            textMetaTotalCard.setText(
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(metaCentavos)));
        }
        if (textGastoAtual != null) {
            textGastoAtual.setText("Gasto Atual: " +
                    MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(gastoTotalCentavos)));
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

    // -------------------------------------------------------------------------
    // Gráfico (código original preservado)
    // -------------------------------------------------------------------------

    private void configurarGraficoCumulativo(
            List<com.github.mikephil.charting.data.Entry> entries,
            int diaAtual, long metaCentavos, int totalDiasMes) {

        // Cores adaptadas para o modo escuro da imagem
        int corTexto = Color.parseColor("#BDBDBD"); // Cinza claro para os eixos
        int corLinha = Color.parseColor("#A5D6A7"); // Verde suave

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(entries, "Evolução de Gastos");
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

        com.github.mikephil.charting.data.LineData data =
                new com.github.mikephil.charting.data.LineData(dataSet);
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
                if (value >= 1000)
                    return String.format(Locale.getDefault(), "R$%.0fk", value / 1000f);
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
        lineChartEvolucao.setOnChartValueSelectedListener(
                new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(
                            com.github.mikephil.charting.data.Entry e,
                            com.github.mikephil.charting.highlight.Highlight h) {
                        if (textDetalheGastoDia != null) {
                            int dia = (int) e.getX();
                            float valorEmReais = e.getY();
                            textDetalheGastoDia.setText("Dia " + dia + ": Gasto acumulado de " +
                                    MoedaHelper.formatarParaBRL((double) valorEmReais));
                            textDetalheGastoDia.setTextColor(Color.parseColor("#FFFFFF"));
                            textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.BOLD);
                        }
                    }

                    @Override
                    public void onNothingSelected() {
                        if (textDetalheGastoDia != null) {
                            textDetalheGastoDia.setText(
                                    "Toque em um ponto do gráfico para ver os detalhes");
                            textDetalheGastoDia.setTextColor(Color.parseColor("#BDBDBD"));
                            textDetalheGastoDia.setTypeface(null, android.graphics.Typeface.NORMAL);
                        }
                    }
                });

        lineChartEvolucao.animateX(800);
        lineChartEvolucao.invalidate();
    }

    // -------------------------------------------------------------------------
    // Simulador "Posso Comprar?" — três cenários com cobertura total de erros
    // -------------------------------------------------------------------------

    /**
     * Executa o simulador apenas se houver um valor digitado.
     * Usado para re-simular automaticamente quando o checkbox muda.
     */
    private void executarSimuladorSeSePossivel() {
        if (editSimuladorGasto == null) return;
        String texto = editSimuladorGasto.getText().toString().trim();
        if (!TextUtils.isEmpty(texto)) {
            executarSimulador();
        }
    }

    /**
     * Ponto de entrada do simulador. Valida todos os inputs antes de calcular.
     *
     * Cenários de erro cobertos:
     *  - Fragment desanexado (getContext() == null)
     *  - Campo de valor vazio ou somente espaços
     *  - Valor não-numérico (letras, símbolos, overflow de double)
     *  - Valor zero ou negativo
     *  - Valor absurdamente alto (> 1 bilhão) — protege contra overflow nos cálculos
     *  - Parcelas marcadas mas campo em branco
     *  - Parcelas com valor não-numérico
     *  - Parcelas < 2 quando "parcelar" está marcado
     *  - Parcelas > 360 (limite razoável de 30 anos)
     *  - Meta mensal zerada no SharedPreferences
     *  - gastoTotalCentavos ainda não carregado (Fragment recém-criado)
     *  - diasRestantesNoMes = 0 (último dia do mês)
     */
    private void executarSimulador() {
        android.content.Context ctx = getContext();
        if (ctx == null) return; // Fragment desanexado

        // --- 1. Valida o campo de valor ---
        if (editSimuladorGasto == null) return;
        String valorStr = editSimuladorGasto.getText().toString().trim().replace(",", ".");

        if (TextUtils.isEmpty(valorStr)) {
            mostrarErroSimulador("Digite o valor da compra antes de simular.");
            return;
        }

        double valorCompra;
        try {
            valorCompra = Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            mostrarErroSimulador("Valor inválido. Use apenas números (ex: 150.00).");
            return;
        }

        if (Double.isNaN(valorCompra) || Double.isInfinite(valorCompra)) {
            mostrarErroSimulador("Valor inválido. Tente novamente.");
            return;
        }

        if (valorCompra <= 0) {
            mostrarErroSimulador("O valor da compra deve ser maior que zero.");
            return;
        }

        // Proteção contra overflow: mais de 1 bilhão de reais não faz sentido no contexto
        if (valorCompra > 1_000_000_000.0) {
            mostrarErroSimulador("Valor muito alto. Verifique o número digitado.");
            return;
        }

        // --- 2. Valida parcelas ---
        boolean parcelar = checkParcelar != null && checkParcelar.isChecked();
        int numeroParcelas = 1;

        if (parcelar) {
            if (editNumeroParcelas == null) {
                // View não encontrada — falha silenciosa, trata como à vista
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
                    mostrarErroSimulador("Número de parcelas inválido. Use apenas dígitos.");
                    return;
                }

                if (numeroParcelas < 2) {
                    mostrarErroSimulador("Parcelamento requer mínimo de 2 parcelas.");
                    editNumeroParcelas.setText("2");
                    numeroParcelas = 2;
                    // Não retorna: continua com 2 parcelas como correção automática
                }

                if (numeroParcelas > 360) {
                    mostrarErroSimulador("Número de parcelas muito alto (máximo: 360).");
                    return;
                }
            }
        }

        // --- 3. Lê a meta mensal do SharedPreferences ---
        long metaCentavos;
        try {
            metaCentavos = ctx.getSharedPreferences("OrgaFacilPrefs",
                            android.content.Context.MODE_PRIVATE)
                    .getLong("meta_mensal", 300000L);
        } catch (Exception e) {
            mostrarErroSimulador("Erro ao ler a meta mensal. Tente novamente.");
            return;
        }

        // Meta zerada não faz sentido para o simulador
        if (metaCentavos <= 0) {
            mostrarErroSimulador("Defina uma meta mensal antes de simular.");
            return;
        }

        // --- 4. Calcula valores base com proteção contra divisão por zero ---
        double restante   = MoedaHelper.centavosParaDouble(metaCentavos - gastoTotalCentavos);
        int diasRest      = Math.max(diasRestantesNoMes, 1); // nunca divide por zero
        double limiteDiario = restante / diasRest;

        double valorMes     = valorCompra / numeroParcelas;
        double restanteApos = restante - valorMes;
        double limiteDiarioApos = restanteApos / diasRest;

        // --- 5. Exibe os cenários ---
        if (layoutResultadoSimulador != null) {
            layoutResultadoSimulador.setVisibility(View.VISIBLE);
        }

        exibirCenario1(valorMes, valorCompra, limiteDiario, limiteDiarioApos,
                restante, restanteApos, parcelar);

        exibirCenario2(valorCompra, valorMes, numeroParcelas, parcelar);

        exibirCenario3(valorMes, restanteApos, limiteDiario);
    }

    /**
     * Cenário 1 — Choque de Realidade: impacto no limite diário.
     *
     * Exibido sempre. Cor muda conforme o saldo pós-compra:
     *  - Verde: compra dentro do orçamento e limite diário confortável (> R$5)
     *  - Amarelo: dentro do orçamento mas limite diário muito baixo (≤ R$5)
     *  - Vermelho: orçamento estourado pela parcela deste mês
     */
    private void exibirCenario1(double valorMes, double valorCompra,
                                double limiteDiario, double limiteDiarioApos,
                                double restante, double restanteApos,
                                boolean parcelar) {
        if (layoutCenario1 == null || textCenario1Titulo == null || textCenario1Corpo == null)
            return;

        layoutCenario1.setVisibility(View.VISIBLE);

        String infoParcela = parcelar
                ? " (parcela de " + MoedaHelper.formatarParaBRL(valorMes) + ")"
                : "";

        if (restanteApos >= 0) {
            // Decide se é situação confortável ou de atenção
            boolean limiteBaixo = limiteDiarioApos <= 5.0;

            textCenario1Titulo.setText("Cenário 1 — Impacto no limite diário");

            if (!limiteBaixo) {
                // Verde: tudo certo
                textCenario1Titulo.setTextColor(Color.parseColor("#A5D6A7"));
                layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_verde);
                textCenario1Corpo.setText(
                        "Seu limite diário atual é "
                                + MoedaHelper.formatarParaBRL(limiteDiario) + "/dia. "
                                + "Se você comprar isso" + infoParcela
                                + ", seu limite cairá para "
                                + MoedaHelper.formatarParaBRL(limiteDiarioApos)
                                + "/dia até o final do mês. Ainda há espaço!"
                );
            } else {
                // Amarelo: cabe, mas fica apertado
                textCenario1Titulo.setTextColor(Color.parseColor("#FFE082"));
                layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_amarelo);
                textCenario1Corpo.setText(
                        "Seu limite diário atual é "
                                + MoedaHelper.formatarParaBRL(limiteDiario) + "/dia. "
                                + "Se você comprar isso" + infoParcela
                                + ", seu limite diário cairá para apenas "
                                + MoedaHelper.formatarParaBRL(Math.max(limiteDiarioApos, 0))
                                + "/dia. Vai ficar bem apertado até o fim do mês."
                );
            }

        } else {
            // Vermelho: estoura já neste mês
            textCenario1Titulo.setText("Cenário 1 — Orçamento insuficiente");
            textCenario1Titulo.setTextColor(Color.parseColor("#EF9A9A"));
            layoutCenario1.setBackgroundResource(R.drawable.bg_posso_comprar_vermelho);
            textCenario1Corpo.setText(
                    "Seu orçamento restante é "
                            + MoedaHelper.formatarParaBRL(restante) + ". "
                            + "Esta compra" + infoParcela
                            + " ultrapassa o disponível em "
                            + MoedaHelper.formatarParaBRL(Math.abs(restanteApos)) + "."
            );
        }
    }

    /**
     * Cenário 2 — O Parcelamento "Sem Juros": o falso alívio.
     *
     * Só exibido quando "parcelar" está marcado com ≥ 2 parcelas.
     * Mostra o comprometimento dos meses futuros.
     */
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

        textCenario2Titulo.setText("Cenário 2 — O parcelamento sem juros");
        textCenario2Titulo.setTextColor(Color.parseColor("#FFE082"));
        textCenario2Corpo.setText(
                "Parcelar divide o custo, mas não zera o preço. "
                        + "Você pagará " + MoedaHelper.formatarParaBRL(valorMes)
                        + " neste mês e comprometerá "
                        + MoedaHelper.formatarParaBRL(valorMes) + "/mês nos próximos "
                        + parcelasFuturas + " " + sufixoMeses + ". "
                        + "No total são "
                        + MoedaHelper.formatarParaBRL(valorCompra)
                        + " saindo do seu bolso — parcelar não é desconto."
        );
    }

    /**
     * Cenário 3 — Adiamento Inteligente: plano de ação quando o orçamento estoura.
     *
     * Só exibido quando a compra (ou sua parcela deste mês) estoura o saldo restante.
     * Calcula quantos dias o usuário precisa "guardar" o limite diário para conseguir comprar.
     *
     * Casos especiais cobertos:
     *  - limiteDiario = 0 (meta já totalmente gasta): orienta a revisar a meta
     *  - diasEspera > diasRestantesNoMes: a compra não cabe mais neste mês de forma alguma
     */
    private void exibirCenario3(double valorMes, double restanteApos, double limiteDiario) {
        if (layoutCenario3 == null || textCenario3Titulo == null || textCenario3Corpo == null)
            return;

        if (restanteApos >= 0) {
            layoutCenario3.setVisibility(View.GONE);
            return;
        }

        layoutCenario3.setVisibility(View.VISIBLE);
        layoutCenario3.setBackgroundResource(R.drawable.bg_posso_comprar_azul);

        textCenario3Titulo.setText("Cenário 3 — Adiamento inteligente");
        textCenario3Titulo.setTextColor(Color.parseColor("#82B4E8"));

        double falta = Math.abs(restanteApos);

        String plano;
        if (limiteDiario <= 0) {
            // Limite diário zerado ou negativo: meta já consumida
            plano = "Seu limite diário já está esgotado. "
                    + "Considere revisar sua meta ou adiar a compra para o próximo mês.";
        } else {
            int diasEspera = (int) Math.ceil(falta / limiteDiario);
            if (diasEspera >= diasRestantesNoMes) {
                // Mesmo economizando tudo, não dá para comprar este mês
                plano = "Esta compra não cabe neste mês. "
                        + "Se você guardar todo o seu limite diário, precisaria de "
                        + diasEspera + " dias — mas só restam " + diasRestantesNoMes + ". "
                        + "Planeje para o próximo mês!";
            } else {
                String sufixoDias = diasEspera == 1 ? "dia" : "dias";
                plano = "Se você guardar seu limite diário por "
                        + diasEspera + " " + sufixoDias
                        + ", poderá comprar isso sem quebrar o orçamento!";
            }
        }

        textCenario3Corpo.setText(
                "Esta compra estoura sua meta em "
                        + MoedaHelper.formatarParaBRL(falta) + ". "
                        + "Alternativa: " + plano
        );
    }

    /**
     * Exibe uma mensagem de erro no lugar dos cenários e oculta os painéis de resultado.
     * Garante que o usuário veja o feedback sem um Toast que some rapidamente.
     *
     * Também usa Toast como fallback caso as views do simulador não estejam disponíveis.
     */
    private void mostrarErroSimulador(String mensagem) {
        // Tenta exibir inline primeiro
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

        // Toast como fallback (ou complemento para erros críticos)
        if (!exibidoInline) {
            android.content.Context ctx = getContext();
            if (ctx != null) {
                Toast.makeText(ctx, mensagem, Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dialog de edição de meta (código original preservado)
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
                                        "A meta deve ser maior que zero.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (valorDouble > 1_000_000_000.0) {
                                Toast.makeText(getContext(),
                                        "Valor muito alto. Verifique o número.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

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
                            Toast.makeText(getContext(),
                                    "Valor inválido. Use apenas números (ex: 1500.00).",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Digite um valor para a meta.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}