package com.gussanxz.orgafacil.data.repository;

import androidx.annotation.NonNull;

/**
 * RepoVoidCallback
 *
 * O que faz:
 * - Para operações que só precisam "ok/falhou" (create, update, delete).
 *
 * Impacto:
 * - Evita duplicação de callbacks e deixa a UI simples.
 */
public interface RepoVoidCallback {
    void onSuccess();
    void onError(Exception e);
}