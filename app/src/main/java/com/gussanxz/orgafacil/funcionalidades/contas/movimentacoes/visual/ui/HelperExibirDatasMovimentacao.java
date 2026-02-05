package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HELPER: MovimentacoesGrouper
 *
 * RESPONSABILIDADE: Processar a lista bruta de movimentações, realizando ordenação cronológica e agrupamento por data.
 * Localizado em: helper (ou ui.contas, se preferir manter a lógica de funcionalidade).
 *
 * * O QUE ELA FAZ:
 * 1. Ordenação Cronológica: Organiza os itens do mais recente para o mais antigo (data + hora).
 * 2. Agrupamento por Dia: Reúne todas as movimentações de uma mesma data sob um único cabeçalho.
 * 3. Cálculo de Saldo Diário: Soma receitas e subtrai despesas de cada dia para exibir o saldo parcial no Header.
 * 4. Humanização de Datas: Converte datas estáticas em títulos amigáveis como "Hoje" ou "Ontem".
 * 5. Preparação de Dados: Converte a lista de 'Movimentacao' em uma lista achatada de 'MovimentoItem'.
 */
public class HelperExibirDatasMovimentacao {

    private static final SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat sdfDataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat sdfDataTitulo = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static List<ExibirItemListaMovimentacaoContas> agruparPorDiaOrdenar(List<MovimentacaoModel> movs) {
        // 1) ordenar por data+hora DECRESCENTE
        List<MovimentacaoModel> copia = new ArrayList<>(movs);
        Collections.sort(copia, new Comparator<MovimentacaoModel>() {
            @Override
            public int compare(MovimentacaoModel o1, MovimentacaoModel o2) {
                Date d1 = parseDataHora(o1.getData(), o1.getHora());
                Date d2 = parseDataHora(o2.getData(), o2.getHora());
                if (d1 == null || d2 == null) return 0;
                // mais recente primeiro
                return d2.compareTo(d1);
            }
        });

        // 2) agrupar por data (string dd/MM/yyyy) mantendo ordem
        Map<String, List<MovimentacaoModel>> porDia = new LinkedHashMap<>();
        for (MovimentacaoModel m : copia) {
            String data = m.getData();
            if (data == null) data = "";
            List<MovimentacaoModel> lista = porDia.get(data);
            if (lista == null) {
                lista = new ArrayList<>();
                porDia.put(data, lista);
            }
            lista.add(m);
        }

        // 3) montar lista achatada com header + linhas
        List<ExibirItemListaMovimentacaoContas> resultado = new ArrayList<>();

        Date hoje = zerarHora(new Date());
        Date ontem = new Date(hoje.getTime() - 24L*60L*60L*1000L);

        for (Map.Entry<String, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            String dataStr = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();

            // saldo do dia = receitas - despesas
            double saldoDia = 0.0;
            for (MovimentacaoModel m : listaDoDia) {
                if ("d".equals(m.getTipo())) {
                    saldoDia -= m.getValor();
                } else {
                    saldoDia += m.getValor();
                }
            }

            // título: Hoje / Ontem / data normal
            String tituloDia = dataStr;
            Date data;
            try {
                data = sdfData.parse(dataStr);
            } catch (ParseException e) {
                data = null;
            }

            if (data != null) {
                Date dataZerada = zerarHora(data);
                if (dataZerada.equals(hoje)) {
                    tituloDia = "Hoje - " + dataStr;
                } else if (dataZerada.equals(ontem)) {
                    tituloDia = "Ontem - " + dataStr;
                } else {
                    tituloDia = dataStr;
                }
            }

            resultado.add(ExibirItemListaMovimentacaoContas.header(dataStr, tituloDia, saldoDia));

            for (MovimentacaoModel m : listaDoDia) {
                resultado.add(ExibirItemListaMovimentacaoContas.linha(m));
            }
        }

        return resultado;
    }

    private static Date parseDataHora(String dataStr, String horaStr) {
        if (dataStr == null || dataStr.isEmpty()) return null;
        if (horaStr == null || horaStr.isEmpty()) horaStr = "00:00";
        try {
            return sdfDataHora.parse(dataStr + " " + horaStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date zerarHora(Date d) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(d);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
