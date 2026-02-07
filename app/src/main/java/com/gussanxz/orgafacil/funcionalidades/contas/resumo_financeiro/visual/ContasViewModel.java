package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContasViewModel extends ViewModel {

    // Encapsulamento: _versaoPrivada para alterar, versaoPublica para a Activity observar
    private final MutableLiveData<List<MovimentacaoModel>> _listaFiltrada = new MutableLiveData<>();
    public LiveData<List<MovimentacaoModel>> listaFiltrada = _listaFiltrada;

    private final MutableLiveData<Long> _saldoPeriodo = new MutableLiveData<>();
    public LiveData<Long> saldoPeriodo = _saldoPeriodo;

    private List<MovimentacaoModel> listaCompleta = new ArrayList<>();

    /**
     * Atualiza a fonte de dados principal vinda do Firebase.
     */
    public void carregarLista(List<MovimentacaoModel> novaLista) {
        this.listaCompleta = novaLista;
        aplicarFiltros("", null, null);
    }

    /**
     * Lógica de filtro e cálculo de saldo.
     * Retirada da Activity para ser testável e independente de UI.
     */
    public void aplicarFiltros(String query, Date inicio, Date fim) {
        List<MovimentacaoModel> filtrados = new ArrayList<>();
        long saldoCentavos = 0;
        String q = (query != null) ? query.toLowerCase() : "";

        for (MovimentacaoModel m : listaCompleta) {
            // Filtro de Período
            boolean noPeriodo = true;
            if (inicio != null && fim != null && m.getData_movimentacao() != null) {
                Date dM = m.getData_movimentacao().toDate();
                noPeriodo = !dM.before(inicio) && !dM.after(fim);
            }

            // Filtro de Texto (Descrição ou Categoria)
            if (noPeriodo && (q.isEmpty()
                    || m.getDescricao().toLowerCase().contains(q)
                    || m.getCategoria_nome().toLowerCase().contains(q))) {

                filtrados.add(m);

                // Cálculo de Saldo (Regra de Ouro: INT/LONG)
                if (m.getTipo() == TipoCategoriaContas.RECEITA.getId()) {
                    saldoCentavos += m.getValor();
                } else {
                    saldoCentavos -= m.getValor();
                }
            }
        }
        _listaFiltrada.setValue(filtrados);
        _saldoPeriodo.setValue(saldoCentavos);
    }
}