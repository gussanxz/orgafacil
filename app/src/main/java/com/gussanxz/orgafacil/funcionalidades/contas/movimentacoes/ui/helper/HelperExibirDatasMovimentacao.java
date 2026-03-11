package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.util_helper.DateHelper;

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

/**
 * HelperExibirDatasMovimentacao
 *
 * Agrupa movimentações por dia e produz a lista mista (cabeçalhos + linhas)
 * que o AdapterMovimentacaoLista consome.
 *
 * CORREÇÃO: saldoDiaCentavos era calculado como long corretamente, mas sofria
 * um cast silencioso para int ao chamar header(). Valores acima de R$ 21.474,83
 * resultavam em saldo negativo ou zero exibido no cabeçalho.
 * Agora todo o pipeline usa long — nenhum cast para int em nenhum ponto.
 */
public class HelperExibirDatasMovimentacao {

    private static final SimpleDateFormat sdfKey = new SimpleDateFormat(DateHelper.FORMATO_EXIBICAO, Locale.getDefault());

    /**
     * Agrupa e ordena movimentações por dia, retornando uma lista mesclada de
     * cabeçalhos e linhas pronta para o adapter.
     *
     * @param movs        lista bruta de movimentações
     * @param ehModoFuturo true = pendentes (ordem crescente), false = histórico (decrescente)
     * @return lista de {@link AdapterItemListaMovimentacao} com headers e linhas intercalados
     */
    public static List<AdapterItemListaMovimentacao> agruparPorDiaOrdenar(
            List<MovimentacaoModel> movs, boolean ehModoFuturo) {

        Date hoje = zerarHora(new Date());

        // ── 1. Ordena os itens individualmente ────────────────────────────────
        movs.sort((m1, m2) -> {
            if (m1.getData_movimentacao() == null || m2.getData_movimentacao() == null) return 0;
            if (ehModoFuturo) {
                // Crescente: vencimentos mais próximos (inclusive vencidos) no topo
                return m1.getData_movimentacao().toDate()
                        .compareTo(m2.getData_movimentacao().toDate());
            }
            // Decrescente: movimentações mais recentes no topo
            return m2.getData_movimentacao().toDate()
                    .compareTo(m1.getData_movimentacao().toDate());
        });

        // ── 2. Agrupa em mapa ordenado por data ───────────────────────────────
        Map<Date, List<MovimentacaoModel>> porDia = ehModoFuturo
                ? new TreeMap<>(Comparator.naturalOrder())   // crescente para futuro
                : new TreeMap<>(Collections.reverseOrder()); // decrescente para histórico

        for (MovimentacaoModel m : movs) {
            if (m.getData_movimentacao() == null) continue;

            Date dataZerada = zerarHora(m.getData_movimentacao().toDate());

            // Histórico: ignora movimentações com data futura (maior que hoje)
            if (!ehModoFuturo && dataZerada.after(hoje)) continue;

            porDia.putIfAbsent(dataZerada, new ArrayList<>());
            porDia.get(dataZerada).add(m);
        }

        // ── 3. Referências para labels especiais ──────────────────────────────
        Calendar cal = Calendar.getInstance();
        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date ontem = cal.getTime();

        cal.setTime(hoje);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date amanha = cal.getTime();

        // ── 4. Constrói a lista final com headers e linhas ────────────────────
        List<AdapterItemListaMovimentacao> resultado = new ArrayList<>();

        for (Map.Entry<Date, List<MovimentacaoModel>> entry : porDia.entrySet()) {
            Date dataGrupo    = entry.getKey();
            List<MovimentacaoModel> listaDoDia = entry.getValue();
            String dataStr    = sdfKey.format(dataGrupo);

            // ── Saldo do dia em centavos (long — nunca cast para int) ─────────
            long saldoDiaCentavos = 0L; // long explícito: evita acumulação em int

            for (MovimentacaoModel m : listaDoDia) {
                if (ehModoFuturo || m.isPago()) {
                    if (m.getTipoEnum() == TipoCategoriaContas.DESPESA) {
                        saldoDiaCentavos -= m.getValor(); // getValor() é long
                    } else {
                        saldoDiaCentavos += m.getValor();
                    }
                }
            }

            // ── Label do cabeçalho ────────────────────────────────────────────
            String tituloDia = dataStr;
            if (dataGrupo.equals(hoje))                     tituloDia = "Hoje";
            else if (dataGrupo.equals(ontem))               tituloDia = "Ontem";
            else if (dataGrupo.equals(amanha) && ehModoFuturo) tituloDia = "Amanhã";

            // CORREÇÃO: passa saldoDiaCentavos como long — sem (int) cast
            // O método header() agora aceita long, eliminando o overflow silencioso
            resultado.add(AdapterItemListaMovimentacao.header(dataStr, tituloDia, saldoDiaCentavos));

            for (MovimentacaoModel m : listaDoDia) {
                resultado.add(AdapterItemListaMovimentacao.linha(m));
            }
        }

        return resultado;
    }

    // ── Utilitário ────────────────────────────────────────────────────────────

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