package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model.TopGastoDTO;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.dados.model.TopGastosAdapter;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumoRelatorioFragment extends Fragment {

    private TextView textTotalReceitas, textTotalDespesas, textSaldoResumo;
    private ImageView btnMesAnterior, btnMesProximo;
    private TextView textMesAtualFiltro;
    private LinearLayout containerMesFiltro;

    // Novas Views para a Aba Top 5
    private TextView btnAbaDespesas, btnAbaReceitas;
    private boolean exibindoDespesasNoTop5 = true; // Controle da aba ativa

    private MovimentacaoRepository repository;
    private List<MovimentacaoModel> listaDoMesAtual = new ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView recyclerTopMaioresGastos;
    private TopGastosAdapter topGastosAdapter;
    private PieChart pieChartCategorias;
    private List<MovimentacaoModel> listaDoMesPassado = new ArrayList<>();

    private Calendar mesSelecionado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resumo_relatorio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textTotalReceitas = view.findViewById(R.id.textTotalReceitas);
        textTotalDespesas = view.findViewById(R.id.textTotalDespesas);
        textSaldoResumo = view.findViewById(R.id.textSaldoResumo);
        pieChartCategorias = view.findViewById(R.id.pieChartCategorias);

        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        btnMesProximo = view.findViewById(R.id.btnMesProximo);
        textMesAtualFiltro = view.findViewById(R.id.textMesAtualFiltro);
        containerMesFiltro = view.findViewById(R.id.containerMesFiltro);

        btnAbaDespesas = view.findViewById(R.id.btnAbaDespesas);
        btnAbaReceitas = view.findViewById(R.id.btnAbaReceitas);

        repository = new MovimentacaoRepository();

        recyclerTopMaioresGastos = view.findViewById(R.id.recyclerTopMaioresGastos);
        recyclerTopMaioresGastos.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        topGastosAdapter = new TopGastosAdapter();
        recyclerTopMaioresGastos.setAdapter(topGastosAdapter);

        topGastosAdapter.setOnItemClickListener(item -> {
            if (item.isMovimentacaoIndividual() && item.getMovimentacaoModel() != null) {
                Intent intent = new Intent(requireContext(), EditarMovimentacaoActivity.class);
                intent.putExtra("movimentacaoSelecionada", item.getMovimentacaoModel());
                intent.putExtra("DIRETO_PRA_EDICAO", false);
                intent.putExtra("MODO_APENAS_VISUALIZACAO", true);
                startActivity(intent);
            }
        });

        View cardInsight = view.findViewById(R.id.cardInsight);
        if (cardInsight != null) cardInsight.setVisibility(View.GONE);

        mesSelecionado = Calendar.getInstance();
        atualizarTextoMes();
        configurarBotoes();

        configurarVisualDoGrafico();
        carregarDadosDoMes();
    }

    private void atualizarTextoMes() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM/yy", new Locale("pt", "BR"));
        String dataFormatada = sdf.format(mesSelecionado.getTime());
        dataFormatada = dataFormatada.substring(0, 1).toUpperCase() + dataFormatada.substring(1);
        textMesAtualFiltro.setText(dataFormatada);
    }

    private void configurarBotoes() {
        btnMesAnterior.setOnClickListener(v -> {
            mesSelecionado.add(Calendar.MONTH, -1);
            atualizarTextoMes();
            carregarDadosDoMes();
        });

        btnMesProximo.setOnClickListener(v -> {
            mesSelecionado.add(Calendar.MONTH, 1);
            atualizarTextoMes();
            carregarDadosDoMes();
        });

        // 1. SOLUÇÃO: Custom Picker só de Mês e Ano
        containerMesFiltro.setOnClickListener(v -> mostrarSeletorDeMesAno());

        // 6. SOLUÇÃO: Alternar Abas Despesas/Receitas
        btnAbaDespesas.setOnClickListener(v -> {
            exibindoDespesasNoTop5 = true;
            atualizarVisualAbas();
            atualizarListaTop5();
        });

        btnAbaReceitas.setOnClickListener(v -> {
            exibindoDespesasNoTop5 = false;
            atualizarVisualAbas();
            atualizarListaTop5();
        });
    }

    private void mostrarSeletorDeMesAno() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER);

        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"});
        monthPicker.setValue(mesSelecionado.get(Calendar.MONTH));

        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(2050);
        yearPicker.setValue(mesSelecionado.get(Calendar.YEAR));

        layout.addView(monthPicker);
        layout.addView(yearPicker);

        new AlertDialog.Builder(requireContext())
                .setTitle("Selecione o Mês")
                .setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    mesSelecionado.set(Calendar.MONTH, monthPicker.getValue());
                    mesSelecionado.set(Calendar.YEAR, yearPicker.getValue());
                    atualizarTextoMes();
                    carregarDadosDoMes();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atualizarVisualAbas() {
        if (exibindoDespesasNoTop5) {
            btnAbaDespesas.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnAbaDespesas.setTextColor(Color.WHITE);
            btnAbaReceitas.setBackgroundResource(0);
            btnAbaReceitas.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            btnAbaReceitas.setBackgroundResource(R.drawable.bg_rounded_dark);
            btnAbaReceitas.setTextColor(Color.WHITE);
            btnAbaDespesas.setBackgroundResource(0);
            btnAbaDespesas.setTextColor(Color.parseColor("#9E9E9E"));
        }
    }

    private void carregarDadosDoMes() {
        Calendar cal = (Calendar) mesSelecionado.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        Date fimAtual = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date inicioAtual = cal.getTime();

        cal.add(Calendar.MONTH, -1);
        Date inicioPassado = cal.getTime();

        repository.buscarMovimentacoesParaExportacao(inicioPassado, fimAtual,
                new MovimentacaoRepository.DadosCallback() {
                    @Override
                    public void onSucesso(List<MovimentacaoModel> lista) {
                        if (!isAdded()) return;
                        listaDoMesAtual.clear();
                        listaDoMesPassado.clear();
                        for (MovimentacaoModel m : lista) {
                            if (m.getData_movimentacao() != null) {
                                if (m.getData_movimentacao().toDate().before(inicioAtual)) {
                                    listaDoMesPassado.add(m);
                                } else {
                                    listaDoMesAtual.add(m);
                                }
                            }
                        }
                        calcularEAtualizarDashboard();
                    }
                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calcularEAtualizarDashboard() {
        long totalReceitas = 0;
        long totalDespesas = 0;

        for (MovimentacaoModel mov : listaDoMesAtual) {
            if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                totalReceitas += mov.getValor();
            }
            // CORREÇÃO: Conta estritamente as Despesas, ignorando Transferências/Outros
            else if (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                totalDespesas += mov.getValor();
            }
        }

        long saldoAtual = totalReceitas - totalDespesas;

        textTotalReceitas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalReceitas)));
        textTotalDespesas.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalDespesas)));
        textSaldoResumo.setText(MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(Math.abs(saldoAtual))));

        if (saldoAtual > 0) textSaldoResumo.setTextColor(Color.parseColor("#4CAF50"));
        else if (saldoAtual < 0) textSaldoResumo.setTextColor(Color.parseColor("#E53935"));
        else textSaldoResumo.setTextColor(Color.parseColor("#757575"));

        // Como nós colocamos a chamada do gráfico lá dentro desse método,
        // chamar ele aqui já resolve a lista E o gráfico da aba correta de uma vez só!
        atualizarListaTop5();

        Map<String, Long> mapaDespesas = agruparPorCategoria(listaDoMesAtual, TipoCategoriaContas.DESPESA);
        gerarInsightsInteligentes(totalReceitas, totalDespesas, mapaDespesas);

        // Removemos os IFs e ELSEs que chamavam o atualizarGraficoPizza() daqui
        // porque agora atualizarListaTop5() faz esse trabalho.
    }

    private void atualizarListaTop5() {
        // Filtra as movimentações do tipo ativo (despesa ou receita)
        TipoCategoriaContas tipoAtivo = exibindoDespesasNoTop5
                ? TipoCategoriaContas.DESPESA
                : TipoCategoriaContas.RECEITA;

        List<MovimentacaoModel> filtradas = new ArrayList<>();
        for (MovimentacaoModel m : listaDoMesAtual) {
            if (m.getTipoEnum() == tipoAtivo) filtradas.add(m);
        }

        // Ordena por valor absoluto decrescente — maior gasto/receita primeiro
        filtradas.sort((a, b) -> Long.compare(Math.abs(b.getValor()), Math.abs(a.getValor())));

        // Calcula total do tipo para percentual relativo
        long total = 0;
        for (MovimentacaoModel m : filtradas) total += Math.abs(m.getValor());

        List<TopGastoDTO> listaRanking = new ArrayList<>();
        int limite = Math.min(filtradas.size(), 5);
        for (int i = 0; i < limite; i++) {
            MovimentacaoModel mov = filtradas.get(i);
            double percentual = total > 0
                    ? ((double) Math.abs(mov.getValor()) / total) * 100.0
                    : 0;
            listaRanking.add(new TopGastoDTO(mov, percentual, exibindoDespesasNoTop5));
        }

        topGastosAdapter.atualizarLista(listaRanking);

        // Gráfico de pizza continua agrupado por categoria — não muda
        Map<String, Long> mapa = agruparPorCategoria(listaDoMesAtual, tipoAtivo);
        atualizarGraficoPizza(mapa);
    }

    // 3, 4 e 5. SOLUÇÃO: Exibe todos, Limpa o gráfico e joga Nome + % para a Legenda
    // 3, 4 e 5. SOLUÇÃO: Exibe todos, Limpa o gráfico e joga Nome + % para a Legenda
    private void atualizarGraficoPizza(Map<String, Long> mapaAtual) {

        // Limpa agressivamente o gráfico antigo da tela antes de desenhar o novo
        pieChartCategorias.clear();

        long somaTotalExata = 0;
        for (Long v : mapaAtual.values()) {
            somaTotalExata += v;
        }

        if (somaTotalExata > 0) {
            pieChartCategorias.setTouchEnabled(true);
            List<PieEntry> entradasGrafico = new ArrayList<>();

            List<Map.Entry<String, Long>> entradasOrdenadas = new ArrayList<>(mapaAtual.entrySet());
            entradasOrdenadas.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            for (Map.Entry<String, Long> entry : entradasOrdenadas) {
                float valorParaGrafico = (float) MoedaHelper.centavosParaDouble(entry.getValue());

                // Cálculo preciso da porcentagem
                double percentual = ((double) entry.getValue() / somaTotalExata) * 100.0;

                // Regra para valores muito pequenos não ficarem "0,0%"
                String percStr;
                if (percentual > 0 && percentual < 0.1) {
                    percStr = String.format(Locale.getDefault(), "<%.1f%%", 0.1); // Exibe <0,1%
                } else {
                    percStr = String.format(Locale.getDefault(), "%.1f%%", percentual); // Exibe normal
                }

                String legendaComPorcentagem = String.format(Locale.getDefault(), "%s (%s)", entry.getKey(), percStr);

                entradasGrafico.add(new PieEntry(valorParaGrafico, legendaComPorcentagem));
            }

            PieDataSet dataSet = new PieDataSet(entradasGrafico, "");
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(5f);
            dataSet.setDrawValues(false);

            int[] coresDespesas = {
                    Color.parseColor("#E53935"), Color.parseColor("#FB8C00"),
                    Color.parseColor("#8E24AA"), Color.parseColor("#3949AB"),
                    Color.parseColor("#00ACC1"), Color.parseColor("#F4511E"),
                    Color.parseColor("#9E9E9E")
            };

            int[] coresReceitas = {
                    Color.parseColor("#43A047"), Color.parseColor("#7CB342"),
                    Color.parseColor("#C0CA33"), Color.parseColor("#00897B"),
                    Color.parseColor("#2E7D32"), Color.parseColor("#00695C"),
                    Color.parseColor("#9E9E9E")
            };

            int[] paletaBase = exibindoDespesasNoTop5 ? coresDespesas : coresReceitas;
            List<Integer> cores = new ArrayList<>();
            for (int i = 0; i < entradasGrafico.size(); i++) {
                cores.add(paletaBase[i % paletaBase.length]);
            }
            dataSet.setColors(cores);

            PieData data = new PieData(dataSet);
            pieChartCategorias.setData(data);

            pieChartCategorias.setDrawCenterText(false);
            pieChartCategorias.setCenterText("");

            pieChartCategorias.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
            pieChartCategorias.invalidate();
        } else {
            List<PieEntry> entradasVazias = new ArrayList<>();
            entradasVazias.add(new PieEntry(100f, ""));
            PieDataSet dataSetVazio = new PieDataSet(entradasVazias, "");
            dataSetVazio.setColor(Color.parseColor("#EEEEEE"));
            dataSetVazio.setDrawValues(false);
            dataSetVazio.setSelectionShift(0f);
            PieData dataVazio = new PieData(dataSetVazio);

            // Reabilita a legenda caso tenha sido desativada, senão fica sem nada em volta
            pieChartCategorias.getLegend().setEnabled(true);

            pieChartCategorias.setData(dataVazio);

            pieChartCategorias.setDrawCenterText(true);
            // Mensagem dinâmica: se for receita exibe uma, se despesa exibe outra
            if (exibindoDespesasNoTop5) {
                pieChartCategorias.setCenterText("Uhuul!\nNenhuma despesa\neste mês 💰");
            } else {
                pieChartCategorias.setCenterText("Poxa!\nNenhuma receita\neste mês 💸");
            }

            pieChartCategorias.setCenterTextSize(14f);
            pieChartCategorias.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.cor_texto));

            pieChartCategorias.setTouchEnabled(false);
            pieChartCategorias.animateY(1000);
            pieChartCategorias.invalidate();
        }
    }

    // 2. SOLUÇÃO: Filtro Inteligente com mais cenários de vida real
    private void gerarInsightsInteligentes(long totalReceitas, long totalDespesas, Map<String, Long> mapaAtual) {
        if (totalReceitas == 0 && totalDespesas == 0) {
            exibirInsightPersonalizado("Mês sem Movimentações", "Nenhuma receita ou despesa registrada neste mês ainda.", "#F5F5F5", "#757575");
            return;
        } else if (totalDespesas == 0 && totalReceitas > 0) {
            exibirInsightPersonalizado("Excelente!", "Você teve receitas, mas não registrou nenhum gasto neste mês.", "#E8F5E9", "#2E7D32");
            return;
        } else if (totalReceitas == 0 && totalDespesas > 0) {
            exibirInsightPersonalizado("Atenção Total", "Você está gastando dinheiro, mas ainda não registrou nenhuma receita este mês.", "#FFEBEE", "#C62828");
            return;
        } else if (totalDespesas > totalReceitas) {
            exibirInsightPersonalizado("Sinal Vermelho!", "Cuidado! Suas despesas já ultrapassaram suas receitas este mês.", "#FFEBEE", "#C62828");
            return;
        }

        Map<String, Long> mapaPassado = agruparPorCategoria(listaDoMesPassado, TipoCategoriaContas.DESPESA);
        boolean houveAumentoSignificativo = false;

        for (Map.Entry<String, Long> entry : mapaAtual.entrySet()) {
            String categoria = entry.getKey();
            long valorAtual = entry.getValue();
            long valorPassado = mapaPassado.getOrDefault(categoria, 0L);

            if (valorPassado > 0) {
                double crescimento = ((double) (valorAtual - valorPassado) / valorPassado) * 100;
                if (crescimento >= 20 && valorAtual > 5000) { // 50 reais
                    exibirCardInsight(categoria, (int) crescimento, valorAtual);
                    houveAumentoSignificativo = true;
                    break;
                }
            }
        }

        if (!houveAumentoSignificativo) {
            exibirInsightPersonalizado("Gastos sob Controle", "Parabéns! Seus gastos estão dentro do esperado e sobrou dinheiro.", "#E8F5E9", "#2E7D32");
        }
    }

    private void exibirInsightPersonalizado(String titulo, String mensagem, String corFundo, String corDestaque) {
        if (getView() == null) return;
        View card = getView().findViewById(R.id.cardInsight);
        TextView textTitulo = getView().findViewById(R.id.textInsightTitulo);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);
        ImageView icone = getView().findViewById(R.id.imageInsightIcone);

        if (card != null && textMensagem != null) {
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor(corFundo));
            if(textTitulo != null) { textTitulo.setText(titulo); textTitulo.setTextColor(Color.parseColor(corDestaque)); }
            if(icone != null) icone.setColorFilter(Color.parseColor(corDestaque));
            textMensagem.setText(mensagem);
        }
    }

    private void exibirCardInsight(String categoria, int porcentagem, long valorAtual) {
        if (getView() == null) return;
        View card = getView().findViewById(R.id.cardInsight);
        TextView textTitulo = getView().findViewById(R.id.textInsightTitulo);
        TextView textMensagem = getView().findViewById(R.id.textInsightMensagem);
        ImageView icone = getView().findViewById(R.id.imageInsightIcone);

        if (card != null && textMensagem != null) {
            String valorFmt = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(valorAtual));
            card.setVisibility(View.VISIBLE);
            card.setBackgroundColor(Color.parseColor("#FFF3E0"));
            if(textTitulo != null) { textTitulo.setText("Atenção aos Gastos"); textTitulo.setTextColor(Color.parseColor("#F57C00")); }
            if(icone != null) icone.setColorFilter(Color.parseColor("#F57C00"));
            textMensagem.setText(String.format("Seus gastos com '%s' subiram %d%% este mês, totalizando %s.", categoria, porcentagem, valorFmt));
        }
    }

    private void configurarVisualDoGrafico() {
        int corTexto = ContextCompat.getColor(requireContext(), R.color.cor_texto);
        int corBackground = ContextCompat.getColor(requireContext(), R.color.cor_background);

        pieChartCategorias.setNoDataText("Carregando seus gastos...");
        pieChartCategorias.setNoDataTextColor(corTexto);
        pieChartCategorias.setUsePercentValues(true);
        pieChartCategorias.getDescription().setEnabled(false);

        pieChartCategorias.setDragDecelerationFrictionCoef(0.95f);
        pieChartCategorias.setDrawHoleEnabled(true);
        pieChartCategorias.setHoleColor(corBackground);
        pieChartCategorias.setTransparentCircleColor(corBackground);
        pieChartCategorias.setTransparentCircleAlpha(50);
        pieChartCategorias.setHoleRadius(50f);
        pieChartCategorias.setTransparentCircleRadius(55f);
        pieChartCategorias.setRotationEnabled(false);
        pieChartCategorias.setHighlightPerTapEnabled(true);

        pieChartCategorias.setDrawCenterText(false);

        // NOVO: Esconde o texto da fatia (Label) que ficava em volta do anel,
        // porque a legenda agora já faz esse papel.
        pieChartCategorias.setDrawEntryLabels(false);

        com.github.mikephil.charting.components.Legend legend = pieChartCategorias.getLegend();
        legend.setEnabled(true);
        legend.setWordWrapEnabled(true);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setTextColor(corTexto);
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(15f);
        legend.setYEntrySpace(5f); // NOVO: Espaçamento entre as linhas da legenda caso quebrem
    }

    private static Map<String, Long> agruparPorCategoria(List<MovimentacaoModel> lista, TipoCategoriaContas tipo) {
        Map<String, Long> mapa = new HashMap<>();
        for (MovimentacaoModel m : lista) {
            if (m.getTipoEnum() != tipo) continue;
            String nome = (m.getCategoria_nome() != null && !m.getCategoria_nome().isEmpty())
                    ? m.getCategoria_nome() : "Sem Categoria";
            mapa.put(nome, mapa.getOrDefault(nome, 0L) + m.getValor());
        }
        return mapa;
    }
}