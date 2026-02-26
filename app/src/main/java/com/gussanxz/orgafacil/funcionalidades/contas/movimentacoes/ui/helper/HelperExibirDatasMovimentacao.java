package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HelperExibirDatasMovimentacao {

    private static final SimpleDateFormat sdfKey = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static List<AdapterItemListaMovimentacao> agruparPorDiaOrdenar(List<MovimentacaoModel> movs, boolean ehModoFuturo) {
        Date hoje = zerarHora(new Date());

        // 1. ORDENAÇÃO UNIVERSAL (Sempre Decrescente: m2 compara com m1)
        movs.sort((m1, m2) -> {
            if (m1.getData_movimentacao() == null || m2.getData_movimentacao() == null) return 0;
            if (ehModoFuturo) {
                // Crescente: vencimentos mais próximos (e vencidos) no topo
                return m1.getData_movimentacao().toDate().compareTo(m2.getData_movimentacao().toDate());
            }
            // Decrescente: movimentações mais recentes no topo
            return m2.getData_movimentacao().toDate().compareTo(m1.getData_movimentacao().toDate());
        });

        // 2. ORDENAÇÃO DOS GRUPOS/CABEÇALHOS (Sempre Decrescente)
        Map<Date, List<MovimentacaoModel>> porDia = ehModoFuturo
                ? new TreeMap<>(Comparator.naturalOrder())   // crescente para futuro
                : new TreeMap<>(Collections.reverseOrder()); // decrescente para histórico

        for (MovimentacaoModel m : movs) {
            if (m.getData_movimentacao() == null) continue;
            Date dataZerada = zerarHora(m.getData_movimentacao().toDate());
            // HISTÓRICO: ignora movimentações com data futura (maior que hoje)
            if (!ehModoFuturo && dataZerada.after(hoje)) continue;
            porDia.putIfAbsent(dataZerada, new ArrayList<>());
            porDia.get(dataZerada).add(m);
        }

        List<AdapterItemListaMovimentacao> resultado = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date ontem = cal.getTime();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date amanha = cal.getTime();

        for (Map.Entry<Date, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            Date dataGrupo = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();
            String dataStr = sdfKey.format(dataGrupo);

            long saldoDiaCentavos = 0;
            for (MovimentacaoModel m : listaDoDia) {
                if (ehModoFuturo || m.isPago()) {
                    if (m.getTipoEnum() == TipoCategoriaContas.DESPESA) saldoDiaCentavos -= m.getValor();
                    else saldoDiaCentavos += m.getValor();
                }
            }

            String tituloDia = dataStr;
            if (dataGrupo.equals(hoje)) tituloDia = "Hoje";
            else if (dataGrupo.equals(ontem)) tituloDia = "Ontem";
            else if (dataGrupo.equals(amanha) && ehModoFuturo) tituloDia = "Amanhã";

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