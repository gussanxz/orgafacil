package com.gussanxz.orgafacil.util_helper;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateCustom {

    public static String dataAtual(){
        long data = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return simpleDateFormat.format(data);
    }

    public static String horaAtual(){
        long data = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return simpleDateFormat.format(data);
    }

    // Recebe "18/01/2026" e retorna "012026"
    public static String mesAnoDataEscolhida(String data){
        if(data != null && data.length() >= 10) {
            String retornoData[] = data.split("/");
            String dia = retornoData[0]; // 18
            String mes = retornoData[1]; // 01
            String ano = retornoData[2]; // 2026
            return mes + ano;
        }
        return null;
    }
}