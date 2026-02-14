package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // --- ENCAPSULAMENTO (Segregado para evitar colisão entre abas) ---

    // LiveData exclusivo para a aba HISTÓRICO
    private final MutableLiveData<List<MovimentacaoModel>> _listaHistorico = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaHistorico = _listaHistorico;

    // LiveData exclusivo para a aba FUTURO
    private final MutableLiveData<List<MovimentacaoModel>> _listaFutura = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaFutura = _listaFutura;

    // Saldo (Mantém compartilhado pois é um dado global calculado sobre o histórico pago)
    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public LiveData<Long> saldoPeriodo = _saldoPeriodo;

    // --- CACHE DE DADOS ---
    // Mantemos duas listas separadas na memória para que uma não apague a outra
    private List<MovimentacaoModel> cacheHistorico = new ArrayList<>();
    private List<MovimentacaoModel> cacheFuturo = new ArrayList<>();

    // Estado dos filtros atuais (para reaplicar quando os dados chegarem do banco)
    private String lastQuery = "";
    private Date lastInicio = null;
    private Date lastFim = null;

    // --- MÉTODOS DE DADOS ---

    /**
     * [CLEAN CODE]: Busca dados e direciona para o cache correto (Histórico ou Futuro).
     * @param ehModoFuturo Define qual repositório chamar e onde salvar os dados.
     */
    public void fetchDados(MovimentacaoRepository repo, boolean ehModoFuturo, MovimentacaoRepository.DadosCallback callbackExterno) {

        // [FIX CRÍTICO]: Use Date em vez de long (currentTimeMillis).
        // O Firestore exige objeto Date para comparar com Timestamp.
        Date agora = new Date();

        MovimentacaoRepository.DadosCallback internalCallback = new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> lista) {
                // 1. Salva na lista correta para não misturar as abas
                if (ehModoFuturo) {
                    cacheFuturo = new ArrayList<>(lista);
                } else {
                    cacheHistorico = new ArrayList<>(lista);
                }

                // 2. Reaplica os filtros atuais (texto/data) para atualizar a UI imediatamente
                aplicarFiltros(lastQuery, lastInicio, lastFim);

                if (callbackExterno != null) callbackExterno.onSucesso(lista);
            }

            @Override
            public void onErro(String erro) {
                if (callbackExterno != null) callbackExterno.onErro(erro);
            }
        };

        // Decisão de qual método do repositório chamar
        if (ehModoFuturo) {
            repo.recuperarContasFuturas(agora, internalCallback);
        } else {
            repo.recuperarHistorico(agora, internalCallback);
        }
    }

    /**
     * Lógica de filtro e cálculo de saldo.
     * [PRECISÃO]: Mantém o cálculo em long (centavos) para evitar erros de double [cite: 2026-02-07].
     * Agora aplica os filtros em AMBAS as listas simultaneamente.
     */
    public void aplicarFiltros(String query, Date inicio, Date fim) {
        // Salva o estado dos filtros
        this.lastQuery = query;
        this.lastInicio = inicio;
        this.lastFim = fim;

        // --- Processa Lista de Histórico ---
        List<MovimentacaoModel> resHistorico = filtrarListaGenerica(cacheHistorico, query, inicio, fim, false);
        _listaHistorico.setValue(resHistorico);

        // --- Processa Lista Futura ---
        List<MovimentacaoModel> resFuturo = filtrarListaGenerica(cacheFuturo, query, inicio, fim, true);
        _listaFutura.setValue(resFuturo);

        // --- Calcula Saldo ---
        // O saldo é calculado baseado nos itens visíveis do HISTÓRICO que foram PAGOS.
        calcularSaldo(resHistorico);
    }

    /**
     * Método auxiliar para não duplicar a lógica de filtro.
     */
    private List<MovimentacaoModel> filtrarListaGenerica(List<MovimentacaoModel> origem, String query, Date inicio, Date fim, boolean isModoFuturo) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (MovimentacaoModel m : origem) {
            // 1. Verificação de Segurança
            if (m == null) continue;

            // [LÓGICA DE EXIBIÇÃO POR STATUS]

            // Regra A: Modo FUTURO
            // Se estamos vendo o futuro, escondemos o que JÁ FOI PAGO (evita duplicidade com histórico).
            if (isModoFuturo && m.isPago()) continue;

            // Regra B: Modo HISTÓRICO
            // Mostramos tudo (Pagos e Pendentes atrasados/hoje) para permitir o Check.

            // 2. Filtro de Período
            boolean noPeriodo = true;
            if (inicio != null && fim != null) {
                if (m.getData_movimentacao() != null) {
                    Date dM = m.getData_movimentacao().toDate();
                    noPeriodo = !dM.before(inicio) && !dM.after(fim);
                } else {
                    noPeriodo = false;
                }
            }

            // 3. Filtro de Texto
            if (noPeriodo) {
                String descricao = (m.getDescricao() != null) ? m.getDescricao().toLowerCase() : "";
                String categoria = (m.getCategoria_nome() != null) ? m.getCategoria_nome().toLowerCase() : "";

                if (q.isEmpty() || descricao.contains(q) || categoria.contains(q)) {
                    filtrados.add(m);
                }
            }
        }
        return filtrados;
    }

    /**
     * Calcula o saldo baseado apenas nos itens PAGOS da lista fornecida.
     */
    private void calcularSaldo(List<MovimentacaoModel> listaParaSaldo) {
        long saldoCentavos = 0; // [cite: 2026-02-07]

        for (MovimentacaoModel m : listaParaSaldo) {
            // [REGRA FINANCEIRA]: Apenas o que está pago impacta o saldo real.
            if (m.isPago()) {
                if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                    saldoCentavos += m.getValor();
                } else {
                    saldoCentavos -= m.getValor();
                }
            }
        }
        _saldoPeriodo.setValue(saldoCentavos);
    }
}