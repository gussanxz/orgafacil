package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

/**
 * ResumoFinanceiroRepository
 * Responsável por gerenciar o "Documento Mágico" que consolida os saldos e indicadores.
 * Este repositório é reativo (escuta mudanças no Firebase em tempo real).
 */
public class ResumoFinanceiroRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ResumoCallback {
        void onUpdate(ResumoFinanceiroModel resumo);
        void onError(String erro);
    }

    /**
     * Escuta o resumo financeiro geral.
     * Ideal para telas de Dashboard ou Resumo de Contas, onde o saldo
     * deve atualizar sozinho quando uma movimentação é inserida/editada.
     */
    public ListenerRegistration escutarResumoGeral(ResumoCallback callback) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        return resumoRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                callback.onError(e.getMessage());
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // Converte o documento Firestore para o nosso Model blindado
                ResumoFinanceiroModel resumo = snapshot.toObject(ResumoFinanceiroModel.class);

                if (resumo != null) {
                    // Executa a regra de negócio de saúde financeira antes de enviar para a UI
                    resumo.calcularStatusSaude();
                    callback.onUpdate(resumo);
                }
            } else {
                // Se o documento não existir (usuário novo), cria um zerado
                inicializarResumoNovoUsuario();
            }
        });
    }

    /**
     * Cria o documento inicial de resumo para evitar erros de ponteiro nulo.
     */
    public void inicializarResumoNovoUsuario() {
        ResumoFinanceiroModel novoResumo = new ResumoFinanceiroModel();
        // Garante que o usuário comece com status saudável
        novoResumo.setStatus_saude_financeira(3);

        FirestoreSchema.contasResumoDoc().set(novoResumo);
    }

    /**
     * Permite atualizar o limite de gastos mensal (Teto de Gastos).
     * Usa as constantes ancoradas do Model para evitar strings mágicas.
     */
    public void atualizarLimiteMensal(int limiteCentavos) {
        FirestoreSchema.contasResumoDoc().update(
                ResumoFinanceiroModel.CAMPO_LIMITE_GASTOS_MENSAL, limiteCentavos
        );
    }

    /**
     * Recupera o resumo uma única vez (sem ficar escutando).
     * Útil para relatórios pontuais.
     */
    public void obterResumoSnapshot(ResumoCallback callback) {
        FirestoreSchema.contasResumoDoc().get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ResumoFinanceiroModel resumo = documentSnapshot.toObject(ResumoFinanceiroModel.class);
                        if (resumo != null) {
                            resumo.calcularStatusSaude();
                            callback.onUpdate(resumo);
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}