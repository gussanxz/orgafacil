package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper responsável por transformar a lista bruta do Firestore em uma lista
 * organizada com Headers de data e cálculo de saldo diário.
 */
public class HelperExibirDatasMovimentacao {

    private static final SimpleDateFormat sdfKey = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static List<AdapterItemListaMovimentacao> agruparPorDiaOrdenar(List<MovimentacaoModel> movs, boolean ehModoFuturo) {

        // 1. Ordenação Inteligente
        List<MovimentacaoModel> copia = new ArrayList<>(movs);
        Collections.sort(copia, (o1, o2) -> {
            if (o1.getData_movimentacao() == null || o2.getData_movimentacao() == null) return 0;

            if (ehModoFuturo) {
                // Contas Futuras: O que vence MAIS CEDO primeiro (Ordem Crescente)
                return o1.getData_movimentacao().compareTo(o2.getData_movimentacao());
            } else {
                // Histórico: O mais RECENTE primeiro (Ordem Decrescente)
                return o2.getData_movimentacao().compareTo(o1.getData_movimentacao());
            }
        });

        // 2. Agrupamento por Dia
        Map<String, List<MovimentacaoModel>> porDia = new LinkedHashMap<>();
        for (MovimentacaoModel m : copia) {
            if (m.getData_movimentacao() == null) continue;
            String dataKey = sdfKey.format(m.getData_movimentacao().toDate());

            if (!porDia.containsKey(dataKey)) {
                porDia.put(dataKey, new ArrayList<>());
            }
            porDia.get(dataKey).add(m);
        }

        // 3. Montagem da Lista Final (Misturando Headers e Linhas normais)
        List<AdapterItemListaMovimentacao> resultado = new ArrayList<>();

        Date hoje = zerarHora(new Date());
        Calendar cal = Calendar.getInstance();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date ontem = cal.getTime();

        for (Map.Entry<String, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            String dataStr = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();

            // Lógica de Saldo no Header (Sempre usando INT para precisão em centavos)
            long saldoDiaCentavos = 0;
            for (MovimentacaoModel m : listaDoDia) {
                // No modo futuro, somamos a estimativa. No histórico, somamos o que foi pago.
                if (ehModoFuturo || m.isPago()) {
                    if (m.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                        saldoDiaCentavos -= m.getValor();
                    } else {
                        saldoDiaCentavos += m.getValor();
                    }
                }
            }

            // Tratamento visual para "Hoje" e "Ontem"
            String tituloDia = dataStr;
            try {
                Date dataGrupo = sdfKey.parse(dataStr);
                if (dataGrupo != null) {
                    Date dataZerada = zerarHora(dataGrupo);
                    if (dataZerada.equals(hoje)) tituloDia = "Hoje";
                    else if (dataZerada.equals(ontem)) tituloDia = "Ontem";
                }
            } catch (Exception ignored) {}

            // Adiciona a "Ficha" do Cabeçalho
            resultado.add(AdapterItemListaMovimentacao.header(dataStr, tituloDia, (int) saldoDiaCentavos));

            // Adiciona as "Fichas" das Movimentações logo abaixo
            for (MovimentacaoModel m : listaDoDia) {
                resultado.add(AdapterItemListaMovimentacao.linha(m));
            }
        }
        return resultado;
    }

    private static Date zerarHora(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}