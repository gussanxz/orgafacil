package com.gussanxz.orgafacil.util_helper;

import java.text.NumberFormat;
import java.util.Locale;

public class MoedaHelper {

    public static String formatarParaBRL(double valor) {
        NumberFormat formatador = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatador.format(valor);
    }

    /**
     * Converte Centavos (long) para Decimal (double) -> Para exibir na tela.
     */
    public static double centavosParaDouble(long centavos) {
        return (double) centavos / 100.0;
    }

    /**
     * Converte Decimal (double) para Centavos (long) -> Para salvar no Firestore.
     */
    public static long doubleParaCentavos(double valor) {
        return Math.round(valor * 100);
    }
}