package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.repository.ResumoFinanceiroRepository;

import java.util.Calendar;

public class ResumoGeralViewModel extends ViewModel {

    private final ResumoFinanceiroRepository repository;
    // [NOVO] Instanciamos o repositório de movimentações pois é ele quem sabe zerar o mês
    private final MovimentacaoRepository movRepository;

    private final MutableLiveData<ResumoFinanceiroModel> _resumoDados = new MutableLiveData<>();
    public LiveData<ResumoFinanceiroModel> resumoDados = _resumoDados;

    private ListenerRegistration listenerRegistration;

    public ResumoGeralViewModel() {
        this.repository = new ResumoFinanceiroRepository();
        this.movRepository = new MovimentacaoRepository(); // Inicializa o novo repositório
        iniciarMonitoramento();
    }

    /**
     * Inicia o listener em tempo real do Firestore.
     * Assim que algo mudar no banco (add, del, check, ou ZERAR O MÊS), este método dispara.
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
                Log.e("ResumoGeralViewModel", "Erro ao escutar resumo: " + erro);
            }
        });
    }

    /**
     * [NOVO] LÓGICA DE VIRADA DE MÊS
     * Verifica na memória do celular se entramos em um novo mês.
     * Se sim, avisa o Repositório para limpar o Firebase.
     */
    public void verificarViradaDeMes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OrgaFacilPrefs", Context.MODE_PRIVATE);
        int mesSalvo = prefs.getInt("mes_ultimo_acesso", -1);
        int mesAtual = Calendar.getInstance().get(Calendar.MONTH); // Janeiro = 0, Fevereiro = 1...

        // Se o usuário já usou o app antes, e o mês mudou:
        if (mesSalvo != -1 && mesSalvo != mesAtual) {
            Log.i("ResumoGeralViewModel", "Virada de Mês detectada! Atualizando estatísticas...");

            movRepository.zerarEstatisticasMensais(new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Log.i("ResumoGeralViewModel", msg);
                    // Só atualiza a memória do celular DEPOIS que o Firebase confirmar o sucesso
                    prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
                }

                @Override
                public void onErro(String erro) {
                    Log.e("ResumoGeralViewModel", "Falha ao zerar mês: " + erro);
                }
            });
        }
        // Se for a primeira vez que o usuário abre o app na vida:
        else if (mesSalvo == -1) {
            prefs.edit().putInt("mes_ultimo_acesso", mesAtual).apply();
        }
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