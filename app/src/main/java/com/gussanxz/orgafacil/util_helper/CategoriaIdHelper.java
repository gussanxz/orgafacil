package com.gussanxz.orgafacil.util_helper;

import java.util.Locale;

public final class CategoriaIdHelper {
    private CategoriaIdHelper() {}

    public static String slugify(String s) {
        if (s == null) return "categoria";
        String t = s.trim().toLowerCase(Locale.ROOT);

        t = t.replace("ç", "c")
                .replace("ã", "a").replace("á", "a").replace("à", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o")
                .replace("ú", "u");

        t = t.replaceAll("[^a-z0-9_ ]", "");
        t = t.replace(" ", "_");

        if (t.isEmpty()) t = "categoria";
        return t;
    }
}
