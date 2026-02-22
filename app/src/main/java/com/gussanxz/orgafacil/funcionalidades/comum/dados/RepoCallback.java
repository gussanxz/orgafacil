package com.gussanxz.orgafacil.funcionalidades.comum.dados;

/**
 * RepoCallback<T>
 *
 * O que faz:
 * - Padroniza retorno assíncrono do Firestore (sucesso/erro).
 *
 * Impacto:
 * - Mantém o código consistente em todas as telas.
 * - Facilita logging e tratamento uniforme de erros.
 */
public interface RepoCallback<T> {
    void onSuccess(T data);
    void onError(Exception e);
}