package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;

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

    public static List<ExibirItemListaMovimentacaoContas> agruparPorDiaOrdenar(List<MovimentacaoModel> movs, boolean ehModoFuturo) {

        // 1. Ordenação Inteligente baseada na aba selecionada
        List<MovimentacaoModel> copia = new ArrayList<>(movs);
        Collections.sort(copia, (o1, o2) -> {
            if (o1.getData_movimentacao() == null || o2.getData_movimentacao() == null) return 0;

            if (ehModoFuturo) {
                // [FUTURO]: O que vence MAIS CEDO primeiro (10/02 antes de 15/02)
                return o1.getData_movimentacao().compareTo(o2.getData_movimentacao());
            } else {
                // [HISTÓRICO]: O mais RECENTE primeiro (Hoje antes de Ontem)
                return o2.getData_movimentacao().compareTo(o1.getData_movimentacao());
            }
        });

        // 2. Agrupamento em Map para separar os itens por data string (Ex: "14/02/2026")
        Map<String, List<MovimentacaoModel>> porDia = new LinkedHashMap<>();
        for (MovimentacaoModel m : copia) {
            if (m.getData_movimentacao() == null) continue;
            String dataKey = sdfKey.format(m.getData_movimentacao().toDate());

            if (!porDia.containsKey(dataKey)) {
                porDia.put(dataKey, new ArrayList<>());
            }
            porDia.get(dataKey).add(m);
        }

        // 3. Criação da lista final mesclando Headers e Itens
        List<ExibirItemListaMovimentacaoContas> resultado = new ArrayList<>();

        // Preparação para exibir "Hoje" e "Ontem" no título do dia
        Date hoje = zerarHora(new Date());
        Calendar cal = Calendar.getInstance();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date ontem = cal.getTime();

        for (Map.Entry<String, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            String dataStr = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();

            // --- LÓGICA DE SALDO NO HEADER ---
            long saldoDiaCentavos = 0;
            for (MovimentacaoModel m : listaDoDia) {
                // [REGRA FINANCEIRA]: Apenas movimentações PAGAS alteram o saldo do cabeçalho.
                // Contas agendadas (pago = false) são exibidas na lista para controle,
                // mas não somadas ao saldo real do dia até serem confirmadas.
                if (m.isPago()) {
                    if (m.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                        saldoDiaCentavos -= m.getValor();
                    } else {
                        saldoDiaCentavos += m.getValor();
                    }
                }
            }

            // Tratamento visual para títulos amigáveis
            String tituloDia = dataStr;
            try {
                Date dataGrupo = sdfKey.parse(dataStr);
                if (dataGrupo != null) {
                    Date dataZerada = zerarHora(dataGrupo);
                    if (dataZerada.equals(hoje)) tituloDia = "Hoje";
                    else if (dataZerada.equals(ontem)) tituloDia = "Ontem";
                }
            } catch (Exception ignored) {}

            // Adiciona o Header com o saldo real calculado em centavos [cite: 2026-02-07]
            resultado.add(ExibirItemListaMovimentacaoContas.header(dataStr, tituloDia, (int) saldoDiaCentavos));

            // Adiciona cada item pertencente àquela data logo abaixo do Header
            for (MovimentacaoModel m : listaDoDia) {
                resultado.add(ExibirItemListaMovimentacaoContas.linha(m));
            }
        }
        return resultado;
    }

    /**
     * Zera horas, minutos e segundos para comparação precisa de datas.
     */
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