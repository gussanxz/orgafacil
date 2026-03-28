package com.gussanxz.orgafacil.util_helper;

import java.text.NumberFormat;
import java.util.Locale;

public class MoedaHelper {

    public static String formatarParaBRL(double valor) {
        NumberFormat formatador = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatador.format(valor); // Já lida com pontos de milhar e negativos nativamente
    }

    /**
     * NOVO: Método direto! Recebe os centavos (long), converte e já formata.
     * Evita repetição de código nas Activities.
     */
    public static String formatarCentavosParaBRL(long centavos) {
        return formatarParaBRL(centavosParaDouble(centavos));
    }

    public static double centavosParaDouble(long centavos) {
        return (double) centavos / 100.0;
    }

    public static long doubleParaCentavos(double valor) {
        return Math.round(valor * 100);
    }
}