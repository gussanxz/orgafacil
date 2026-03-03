package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model;

import java.util.List;

public class ResumoParcelamentoModel {
    public String recorrenciaId;
    public int totalParcelas;
    public int parcelasPagas;
    public int parcelasPendentes;
    public long valorTotalCentavos;       // soma de todas as parcelas
    public long valorPagoCentavos;        // soma das pagas
    public long valorRestanteCentavos;    // soma das pendentes
    public MovimentacaoModel proximaPendente; // a próxima a vencer

    public double percentualConcluido() {
        if (totalParcelas == 0) return 0;
        return (double) parcelasPagas / totalParcelas * 100.0;
    }

    /** Constrói o resumo a partir da lista bruta de parcelas */
    public static ResumoParcelamentoModel calcular(List<MovimentacaoModel> parcelas) {
        ResumoParcelamentoModel r = new ResumoParcelamentoModel();
        if (parcelas == null || parcelas.isEmpty()) return r;

        r.recorrenciaId = parcelas.get(0).getRecorrencia_id();
        r.totalParcelas = parcelas.size();

        for (MovimentacaoModel p : parcelas) {
            r.valorTotalCentavos += p.getValor();
            if (p.isPago()) {
                r.parcelasPagas++;
                r.valorPagoCentavos += p.getValor();
            } else {
                r.parcelasPendentes++;
                r.valorRestanteCentavos += p.getValor();
                // Pega a próxima pendente (a de menor parcela_atual não paga)
                if (r.proximaPendente == null ||
                        p.getParcela_atual() < r.proximaPendente.getParcela_atual()) {
                    r.proximaPendente = p;
                }
            }
        }
        return r;
    }
}