package com.gussanxz.orgafacil.util_helper;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class TemaHelper {
    public static final String PREF_NAME = "OrgaFacilPrefs";
    public static final String KEY_TEMA = "tema_escolhido";

    public static void aplicarTema(String tema) {
        if (tema == null) return;

        int modoAlvo;
        switch (tema) {
            case "ESCURO": modoAlvo = AppCompatDelegate.MODE_NIGHT_YES; break;
            case "CLARO": modoAlvo = AppCompatDelegate.MODE_NIGHT_NO; break;
            default: modoAlvo = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }

        // Só aplica se o modo atual for diferente do alvo (evita recriação inútil)
        if (AppCompatDelegate.getDefaultNightMode() != modoAlvo) {
            AppCompatDelegate.setDefaultNightMode(modoAlvo);
        }
    }

    // Salva o tema localmente para acesso imediato no próximo boot
    public static void salvarTemaCache(Context context, String tema) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TEMA, tema).apply();
    }

    // Lê o tema do cache e aplica
    public static void aplicarTemaDoCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String tema = prefs.getString(KEY_TEMA, "SISTEMA");
        aplicarTema(tema);
    }
}