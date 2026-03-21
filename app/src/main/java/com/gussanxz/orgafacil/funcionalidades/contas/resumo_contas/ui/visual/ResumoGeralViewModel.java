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
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository; // NOVO IMPORT

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ResumoGeralViewModel extends ViewModel {

    private final ResumoFinanceiroRepository repository;
    private final MovimentacaoRepository movRepository;
    private final UsuarioRepository usuarioRepository; // NOVO

    private final MutableLiveData<ResumoFinanceiroModel> _resumoDados = new MutableLiveData<>();
    public LiveData<ResumoFinanceiroModel> resumoDados = _resumoDados;

    private final MutableLiveData<Integer> _contasUrgentes = new MutableLiveData<>(0);
    public LiveData<Integer> contasUrgentes = _contasUrgentes;

    // --- [NOVO] LIVEDATA PARA O NOME DO USUÁRIO ---
    private final MutableLiveData<String> _nomeUsuario = new MutableLiveData<>();
    public LiveData<String> nomeUsuario = _nomeUsuario;

    private ListenerRegistration listenerRegistration;

    public ResumoGeralViewModel() {
        this.repository = new ResumoFinanceiroRepository();
        this.movRepository = new MovimentacaoRepository();
        this.usuarioRepository = new UsuarioRepository(); // NOVO

        iniciarMonitoramento();
        buscarNomeUsuario(); // NOVO
    }

    // --- [NOVO] MÉTODO PARA BUSCAR O NOME ---
    private void buscarNomeUsuario() {
        usuarioRepository.obterNomeUsuario(nome -> {
            _nomeUsuario.setValue(nome);
        });
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
        SharedPreferences prefs = context.getSharedPreferences(
                "OrgaFacilPrefs", Context.MODE_PRIVATE);

        // Formato yyyyMM — inclui o ano para evitar falso-positivo quando o
        // usuário não abre o app por 12+ meses e o mês coincide com o salvo.
        // Ex: março/2024 = "202403", março/2025 = "202503" — sempre diferentes.
        Calendar agora = Calendar.getInstance();
        String periodoAtual = String.format(
                Locale.getDefault(), "%04d%02d",
                agora.get(Calendar.YEAR),
                agora.get(Calendar.MONTH) + 1); // MONTH é 0-based — +1 para ficar legível

        // Migração: versões anteriores gravavam um int com putInt().
        // getString() numa chave do tipo int lança ClassCastException no Android —
        // não retorna null nem o fallback. O try/catch captura isso e trata como
        // "primeiro acesso após update": reseta uma vez e estabiliza.
        String periodoSalvo;
        try {
            periodoSalvo = prefs.getString("mes_ultimo_acesso", "");
        } catch (ClassCastException e) {
            // Chave existia como int (versão anterior do app).
            // Sobrescreve com o novo formato String e reseta as estatísticas,
            // pois não sabemos há quanto tempo o usuário não abre o app.
            prefs.edit().putString("mes_ultimo_acesso", periodoAtual).apply();
            Log.i("ResumoGeralViewModel",
                    "Migração mes_ultimo_acesso: int → String. Zerando estatísticas.");
            zerarEstatisticasComLog(periodoAtual);
            return;
        }

        if (periodoSalvo.isEmpty()) {
            // Primeiro acesso absoluto (prefs vazias) — registra sem zerar.
            prefs.edit().putString("mes_ultimo_acesso", periodoAtual).apply();
            return;
        }

        if (periodoSalvo.equals(periodoAtual)) {
            // Mesmo mês e mesmo ano — nada a fazer.
            return;
        }

        // Período diferente: mês ou ano mudou. Atualiza ANTES do callback
        // assíncrono pelo mesmo motivo documentado na versão anterior — evita
        // que uma segunda entrada no app no mesmo ciclo dispare reset duplo.
        prefs.edit().putString("mes_ultimo_acesso", periodoAtual).apply();

        Log.i("ResumoGeralViewModel",
                "Virada detectada: " + periodoSalvo + " → " + periodoAtual
                        + ". Zerando estatísticas...");

        zerarEstatisticasComLog(periodoAtual);
    }

    // Método auxiliar privado — extrai a chamada ao repository com o log de
// resultado para que verificarViradaDeMes() não repita o mesmo bloco
// em dois lugares (virada normal + migração).
// Não substitui nem remove nenhum método existente.
    private void zerarEstatisticasComLog(String periodoAtual) {
        movRepository.zerarEstatisticasMensais(new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Log.i("ResumoGeralViewModel", msg);
            }

            @Override
            public void onErro(String erro) {
                Log.e("ResumoGeralViewModel", "Falha ao zerar mês: " + erro);
                // Não reverte a prefs: preferimos não zerar duas vezes
                // a correr o risco de loop de reset em falhas de rede.
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