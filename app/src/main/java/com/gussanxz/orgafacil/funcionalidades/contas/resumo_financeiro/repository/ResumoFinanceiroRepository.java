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
     * Ideal para telas de Dashboard.
     */
    public ListenerRegistration escutarResumoGeral(ResumoCallback callback) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        return resumoRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                callback.onError(e.getMessage());
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // O Firestore converte automaticamente os mapas aninhados para as classes internas
                ResumoFinanceiroModel resumo = snapshot.toObject(ResumoFinanceiroModel.class);

                if (resumo != null) {
                    // Executa a regra de negócio (que agora usa os getters dos sub-objetos)
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

        // [ATUALIZADO] Acessa o grupo Inteligência para definir o status
        if (novoResumo.getInteligencia() != null) {
            novoResumo.getInteligencia().setStatusSaudeFinanceira(3); // Saudável
        }

        FirestoreSchema.contasResumoDoc().set(novoResumo);
    }

    /**
     * Permite atualizar o limite de gastos mensal.
     * Funciona perfeitamente pois a constante no Model agora vale "inteligencia.limiteGastosMensal"
     */
    public void atualizarLimiteMensal(int limiteCentavos) {
        FirestoreSchema.contasResumoDoc().update(
                ResumoFinanceiroModel.CAMPO_LIMITE_GASTOS_MENSAL, limiteCentavos
        );
    }

    /**
     * Recupera o resumo uma única vez (sem ficar escutando).
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