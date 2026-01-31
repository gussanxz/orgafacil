package com.gussanxz.orgafacil.util_helper;

import java.text.NumberFormat;
import java.util.Locale;

public class MoedaHelper {
    public static String formatarParaBRL(double valor) {
        // Como o foco é BRL, forçamos o Locale do Brasil
        NumberFormat formatador = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatador.format(valor);
    }
}