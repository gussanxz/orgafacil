package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper responsável por agrupar a lista bruta em Headers de data e Fichas.
 * [CORRIGIDO]: A ordenação agora é feita 100% pelo Firebase (Repositório).
 */
public class HelperExibirDatasMovimentacao {

    private static final SimpleDateFormat sdfKey = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static List<AdapterItemListaMovimentacao> agruparPorDiaOrdenar(List<MovimentacaoModel> movs, boolean ehModoFuturo) {

        Date hoje = zerarHora(new Date());

        movs.sort((m1, m2) ->
                m2.getData_movimentacao().compareTo(m1.getData_movimentacao())
        );

        // 1. Agrupamento por Dia
        // Como o Firebase já mandou a lista ordenada (DESCENDING pro Histórico, ASCENDING pro Futuro),
        // o LinkedHashMap vai apenas "empilhar" os dias respeitando essa exata ordem de chegada.
        Map<String, List<MovimentacaoModel>> porDia = new LinkedHashMap<>();
        for (MovimentacaoModel m : movs) {
            if (m.getData_movimentacao() == null) continue;

            String dataKey = sdfKey.format(m.getData_movimentacao().toDate());

            if (!porDia.containsKey(dataKey)) {
                porDia.put(dataKey, new ArrayList<>());
            }
            porDia.get(dataKey).add(m);
        }

        // 2. Montagem da Lista Final (Headers e Fichas)
        List<AdapterItemListaMovimentacao> resultado = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date ontem = cal.getTime();

        for (Map.Entry<String, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            String dataStr = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();

            // Lógica de Saldo em Centavos (Int) para precisão financeira [cite: 2026-02-07]
            long saldoDiaCentavos = 0;
            for (MovimentacaoModel m : listaDoDia) {
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

            resultado.add(AdapterItemListaMovimentacao.header(dataStr, tituloDia, (int) saldoDiaCentavos));

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