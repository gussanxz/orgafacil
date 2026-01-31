package com.gussanxz.orgafacil.funcionalidades.firebase;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseUser;

/**
 * FirebaseSession
 * Gerencia o estado do usuário logado e centraliza o acesso ao UID. [cite: 2025-11-10]
 */
public final class FirebaseSession {

    private FirebaseSession() {}

    /**
     * Retorna o UID do usuário logado.
     * @throws IllegalStateException se o usuário não estiver autenticado. [cite: 2025-11-10]
     */
    @NonNull
    public static String getUserId() {
        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Acesso negado: Usuário não autenticado no Firebase.");
        }
        return user.getUid();
    }

    /**
     * Verifica se existe uma sessão ativa. [cite: 2025-11-10]
     */
    public static boolean isUserLogged() {
        return ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser() != null;
    }

    /**
     * Finaliza a sessão do usuário. [cite: 2025-11-10]
     */
    public static void logOut() {
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
    }
}