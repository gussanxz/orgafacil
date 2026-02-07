package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // Encapsulamento
    private final MutableLiveData<List<MovimentacaoModel>> _listaFiltrada = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaFiltrada = _listaFiltrada;

    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public LiveData<Long> saldoPeriodo = _saldoPeriodo;

    private List<MovimentacaoModel> listaCompleta = new ArrayList<>();

    /**
     * Atualiza a fonte de dados principal vinda do Firebase.
     */
    public void carregarLista(List<MovimentacaoModel> novaLista) {
        if (novaLista != null) {
            this.listaCompleta = new ArrayList<>(novaLista); // Cria cópia para segurança
        } else {
            this.listaCompleta = new ArrayList<>();
        }
        // Aplica filtro vazio para exibir tudo inicialmente
        aplicarFiltros("", null, null);
    }

    /**
     * Lógica de filtro e cálculo de saldo.
     * Blindada contra NullPointerExceptions.
     */
    public void aplicarFiltros(String query, Date inicio, Date fim) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        long saldoCentavos = 0;

        // Tratamento da query para evitar erros
        String q = (query != null) ? query.toLowerCase().trim() : "";

        for (MovimentacaoModel m : listaCompleta) {

            // 1. Verificação de Segurança (Ignora itens corrompidos)
            if (m == null) continue;

            // 2. Filtro de Período
            boolean noPeriodo = true;
            if (inicio != null && fim != null) {
                if (m.getData_movimentacao() != null) {
                    Date dM = m.getData_movimentacao().toDate();
                    // !before = depois ou igual | !after = antes ou igual
                    noPeriodo = !dM.before(inicio) && !dM.after(fim);
                } else {
                    noPeriodo = false; // Sem data não entra no filtro de data
                }
            }

            // 3. Filtro de Texto (Descrição ou Categoria)
            if (noPeriodo) {
                String descricao = (m.getDescricao() != null) ? m.getDescricao().toLowerCase() : "";
                String categoria = (m.getCategoria_nome() != null) ? m.getCategoria_nome().toLowerCase() : "";

                if (q.isEmpty() || descricao.contains(q) || categoria.contains(q)) {

                    filtrados.add(m);

                    // 4. Cálculo de Saldo
                    if (m.getTipo() == TipoCategoriaContas.RECEITA.getId()) {
                        saldoCentavos += m.getValor();
                    } else {
                        saldoCentavos -= m.getValor();
                    }
                }
            }
        }

        // Atualiza a UI
        _listaFiltrada.setValue(filtrados);
        _saldoPeriodo.setValue(saldoCentavos);
    }
}