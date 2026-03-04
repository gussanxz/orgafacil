package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.repository.ResumoFinanceiroRepository;

import java.util.Calendar;
import java.util.List;

public class ResumoGeralViewModel extends ViewModel {

    private final ResumoFinanceiroRepository repository;
    private final MovimentacaoRepository movRepository;

    private final MutableLiveData<ResumoFinanceiroModel> _resumoDados = new MutableLiveData<>();
    public LiveData<ResumoFinanceiroModel> resumoDados = _resumoDados;

    // --- [NOVO] LIVEDATA DE URGÊNCIA ---
    private final MutableLiveData<Integer> _contasUrgentes = new MutableLiveData<>(0);
    public LiveData<Integer> contasUrgentes = _contasUrgentes;

    private ListenerRegistration listenerRegistration;

    public ResumoGeralViewModel() {
        this.repository = new ResumoFinanceiroRepository();
        this.movRepository = new MovimentacaoRepository();
        iniciarMonitoramento();
    }

    private void iniciarMonitoramento() {
        listenerRegistration = repository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
            @Override
            public void onUpdate(ResumoFinanceiroModel resumo) {
                _resumoDados.setValue(resumo);
            }

            @Override
            public void onError(String erro) {
                Log.e("ResumoGeralViewModel", "Erro ao escutar resumo: " + erro);
            }
        });
    }

    // --- [NOVO] MÉTODO PARA CALCULAR URGÊNCIAS ---
    public void calcularUrgencias(List<MovimentacaoModel> listaPendentes) {
        int urgentes = 0;
        for (MovimentacaoModel m : listaPendentes) {
            if (!m.isPago() && (m.estaVencida() || m.venceEmBreve())) {
                urgentes++;
            }
        }
        _contasUrgentes.setValue(urgentes);
    }

    /**
     * CORREÇÃO #3 — Virada de mês duplicada.
     *
     * Antes: tanto ContasActivity quanto ResumoGeralViewModel chamavam
     * zerarEstatisticasMensais() ao detectar virada de mês, com risco de:
     *   1. Duplo zeramente: ambas disparam na mesma sessão e o Firestore
     *      recebe duas escritas consecutivas de reset.
     *   2. Race condition: ContasActivity gravava mes_ultimo_acesso apenas
     *      no onSucesso do callback assíncrono. Se o ViewModel lesse a
     *      SharedPreference antes desse write completar, ambas veriam o
     *      mês antigo e disparariam o reset.
     *
     * Depois: a lógica vive exclusivamente aqui no ViewModel.
     *   - ContasActivity.verificarViradaDeMes() foi removida de lá.
     *   - ResumoContasActivity continua chamando viewModel.verificarViradaDeMes(context).
     *   - A SharedPreference mes_ultimo_acesso é atualizada de forma síncrona
     *     ANTES de chamar zerarEstatisticasMensais(), garantindo que nenhum
     *     outro ponto do app veja o mês antigo e dispare um segundo reset.
     *
     * Por que atualizar a prefs ANTES do callback?
     *   Se atualizarmos só no onSucesso e o app for para background antes
     *   do Firestore responder, na próxima abertura veríamos o mês antigo
     *   novamente e dispararíamos um segundo reset. Atualizar antes é a
     *   escolha conservadora: no pior caso (falha de rede) perdemos as
     *   estatísticas daquele mês, mas nunca zeramos duas vezes.
     */
    public void verificarViradaDeMes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OrgaFacilPrefs", Context.MODE_PRIVATE);
        int mesSalvo = prefs.getInt("mes_ultimo_acesso", -1);
        int mesAtual = Calendar.getInstance().get(Calendar.MONTH);

        if (mesSalvo == -1) {
            // Primeiro acesso: registra o mês atual sem zerar nada
            prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
            return;
        }

        if (mesSalvo == mesAtual) {
            // Mesmo mês — nada a fazer
            return;
        }

        // Mês virou: atualiza a prefs ANTES do callback assíncrono para
        // evitar que outra entrada no app dispare um segundo reset.
        prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();

        Log.i("ResumoGeralViewModel", "Virada de Mês detectada: " + mesSalvo + " → " + mesAtual + ". Zerando estatísticas...");

        movRepository.zerarEstatisticasMensais(new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Log.i("ResumoGeralViewModel", msg);
            }

            @Override
            public void onErro(String erro) {
                Log.e("ResumoGeralViewModel", "Falha ao zerar mês: " + erro);
                // Não reverte a prefs: preferimos não zerar duas vezes
                // a correr o risco de um loop de reset em falhas de rede.
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}