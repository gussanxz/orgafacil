package com.gussanxz.orgafacil.funcionalidades.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseUser;

/**
 * FirebaseSession
 * Gerencia o estado do usuário logado, centraliza o acesso ao UID
 * e gerencia o cache local de preferências.
 */
public final class FirebaseSession {

    private static final String PREF_NAME = "OrgaFacilPrefs";
    public static final String KEY_MOEDA = "moeda_padrao";

    private FirebaseSession() {}

    // --- GESTÃO DE AUTENTICAÇÃO ---

    @NonNull
    public static String getUserId() {
        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Acesso negado: Usuário não autenticado no Firebase.");
        }
        return user.getUid();
    }

    public static boolean isUserLogged() {
        return ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser() != null;
    }

    public static void logOut(Context context) {
        clearCache(context);
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
    }

    // --- GESTÃO DE CACHE LOCAL (SharedPreferences) ---

    private static SharedPreferences getPrefs(Context context) {
        // Usa getApplicationContext para evitar memory leaks com Activities
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Limpa o cache local de preferências.
     * Útil para garantir que um novo usuário não veja as configs do anterior.
     */
    public static void clearCache(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static void putString(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getPrefs(context).getString(key, defaultValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPrefs(context).getBoolean(key, defaultValue);
    }
}