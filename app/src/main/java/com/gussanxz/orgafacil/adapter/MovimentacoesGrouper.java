package com.gussanxz.orgafacil.adapter;

import com.gussanxz.orgafacil.adapter.MovimentoItem;
import com.gussanxz.orgafacil.model.Movimentacao;

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

public class MovimentacoesGrouper {

    private static final SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat sdfDataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat sdfDataTitulo = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static List<MovimentoItem> agruparPorDiaOrdenar(List<Movimentacao> movs) {
        // 1) ordenar por data+hora DECRESCENTE
        List<Movimentacao> copia = new ArrayList<>(movs);
        Collections.sort(copia, new Comparator<Movimentacao>() {
            @Override
            public int compare(Movimentacao o1, Movimentacao o2) {
                Date d1 = parseDataHora(o1.getData(), o1.getHora());
                Date d2 = parseDataHora(o2.getData(), o2.getHora());
                if (d1 == null || d2 == null) return 0;
                // mais recente primeiro
                return d2.compareTo(d1);
            }
        });

        // 2) agrupar por data (string dd/MM/yyyy) mantendo ordem
        Map<String, List<Movimentacao>> porDia = new LinkedHashMap<>();
        for (Movimentacao m : copia) {
            String data = m.getData();
            if (data == null) data = "";
            List<Movimentacao> lista = porDia.get(data);
            if (lista == null) {
                lista = new ArrayList<>();
                porDia.put(data, lista);
            }
            lista.add(m);
        }

        // 3) montar lista achatada com header + linhas
        List<MovimentoItem> resultado = new ArrayList<>();

        Date hoje = zerarHora(new Date());
        Date ontem = new Date(hoje.getTime() - 24L*60L*60L*1000L);

        for (Map.Entry<String, List<Movimentacao>> entry : porDia.entrySet()) {
            String dataStr = entry.getKey();
            List<Movimentacao> listaDoDia = entry.getValue();

            // saldo do dia = receitas - despesas
            double saldoDia = 0.0;
            for (Movimentacao m : listaDoDia) {
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

            resultado.add(MovimentoItem.header(dataStr, tituloDia, saldoDia));

            for (Movimentacao m : listaDoDia) {
                resultado.add(MovimentoItem.linha(m));
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
