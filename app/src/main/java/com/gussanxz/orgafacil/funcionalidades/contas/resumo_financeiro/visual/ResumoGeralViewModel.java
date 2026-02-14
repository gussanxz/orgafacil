package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.repository.ResumoFinanceiroRepository;

public class ResumoGeralViewModel extends ViewModel {

    private final ResumoFinanceiroRepository repository;
    private final MutableLiveData<ResumoFinanceiroModel> _resumoDados = new MutableLiveData<>();
    public LiveData<ResumoFinanceiroModel> resumoDados = _resumoDados;

    private ListenerRegistration listenerRegistration;

    public ResumoGeralViewModel() {
        this.repository = new ResumoFinanceiroRepository();
        iniciarMonitoramento();
    }

    /**
     * Inicia o listener em tempo real do Firestore.
     * Assim que algo mudar no banco (add, del, check), este método dispara.
     */
    private void iniciarMonitoramento() {
        listenerRegistration = repository.escutarResumoGeral(new ResumoFinanceiroRepository.ResumoCallback() {
            @Override
            public void onUpdate(ResumoFinanceiroModel resumo) {
                _resumoDados.setValue(resumo);
            }

            @Override
            public void onError(String erro) {
                // Em caso de erro, pode-se logar ou definir um estado de erro
            }
        });
    }

    /**
     * Limpa o listener quando a tela é destruída para economizar bateria e dados.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}