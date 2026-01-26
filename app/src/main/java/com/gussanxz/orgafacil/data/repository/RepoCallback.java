package com.gussanxz.orgafacil.data.repository;

import androidx.annotation.NonNull;

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