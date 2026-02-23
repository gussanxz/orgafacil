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

    public void verificarViradaDeMes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OrgaFacilPrefs", Context.MODE_PRIVATE);
        int mesSalvo = prefs.getInt("mes_ultimo_acesso", -1);
        int mesAtual = Calendar.getInstance().get(Calendar.MONTH);

        if (mesSalvo != -1 && mesSalvo != mesAtual) {
            Log.i("ResumoGeralViewModel", "Virada de Mês detectada! Atualizando estatísticas...");

            movRepository.zerarEstatisticasMensais(new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Log.i("ResumoGeralViewModel", msg);
                    prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
                }

                @Override
                public void onErro(String erro) {
                    Log.e("ResumoGeralViewModel", "Falha ao zerar mês: " + erro);
                }
            });
        }
        else if (mesSalvo == -1) {
            prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}