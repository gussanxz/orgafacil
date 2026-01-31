package com.gussanxz.orgafacil.funcionalidades.firebase;

import androidx.annotation.NonNull;

/**
 * FirebaseSession
 * * Gerencia o estado do usuário logado e centraliza o acesso ao UID.
 * Isola a lógica de autenticação das definições de banco de dados.
 */
public final class FirebaseSession {

    private FirebaseSession() {}

    /**
     * Retorna o UID do usuário logado.
     * @throws IllegalStateException se o usuário não estiver autenticado.
     */
    @NonNull
    public static String getUserId() {
        String uid = ConfiguracaoFirestore.getFirebaseAutenticacao().getUid();
        if (uid == null) {
            throw new IllegalStateException("Acesso negado: Usuário não autenticado no Firebase.");
        }
        return uid;
    }

    /**
     * Verifica se existe uma sessão ativa.
     */
    public static boolean isUserLogged() {
        return ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser() != null;
    }
}