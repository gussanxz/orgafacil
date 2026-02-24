package com.gussanxz.orgafacil.util_helper;

public class SecurityConstants {

    private SecurityConstants() {}

    public static final String PREF_NAME = "OrgaFacilPrefs";
    public static final String KEY_PIN_OBRIGATORIO = "pin_obrigatorio";

    // Estado padrão oficial do sistema
    public static final boolean DEFAULT_PIN_OBRIGATORIO = false;

    // Tempo máximo em background antes de exigir nova autenticação (30s)
    public static final long TEMPO_MAXIMO_INATIVIDADE = 30_000;
}