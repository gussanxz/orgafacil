package com.gussanxz.orgafacil.util_helper;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * DateHelper - Centralizador de formatos e operações de data do OrgaFacil.
 * [BOA PRÁTICA]: Centralizar formatos evita bugs de persistência e UI.
 */
public class DateHelper {

    public static final String FORMATO_EXIBICAO = "dd/MM/yy";
    public static final String FORMATO_HORA     = "HH:mm";
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    // ThreadLocal: cada thread (incluindo o ExecutorService do filtro) tem sua própria instância
    private static SimpleDateFormat getSdfExibicao() {
        return new SimpleDateFormat(FORMATO_EXIBICAO, BR_LOCALE);
    }
    private static SimpleDateFormat getSdfHora() {
        return new SimpleDateFormat(FORMATO_HORA, BR_LOCALE);
    }

    public static String dataAtual()  { return getSdfExibicao().format(new Date()); }
    public static String horaAtual()  { return getSdfHora().format(new Date()); }

    public static String mesAnoChave(String data) {
        if (data != null && data.contains("/")) {
            String[] partes = data.split("/");
            if (partes.length >= 3) return partes[1] + partes[2];
        }
        return "";
    }

    public static void exibirSeletorData(Context context, EditText campoAlvo) {
        final Calendar cal = Calendar.getInstance();
        try {
            Date dataCampo = getSdfExibicao().parse(campoAlvo.getText().toString());
            if (dataCampo != null) cal.setTime(dataCampo);
        } catch (Exception ignored) {}

        new DatePickerDialog(context, (view, year, month, day) -> {
            cal.set(year, month, day);
            campoAlvo.setText(getSdfExibicao().format(cal.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    public static String formatarData(Date date) { return getSdfExibicao().format(date); }
    public static String formatarHora(Date date)  { return getSdfHora().format(date); }

    /** Converte String DD/MM/AA → Date com hora 00:00:00 (início do dia) */
    public static Date parsearData(String dataTexto) {
        try {
            Date d = getSdfExibicao().parse(dataTexto);
            return d != null ? truncarParaInicioDia(d) : truncarParaInicioDia(new Date());
        } catch (Exception e) {
            return truncarParaInicioDia(new Date());
        }
    }

    /** Converte String DD/MM/AA → Date com hora 23:59:59.999 (fim do dia) — use para filtro final */
    public static Date parsearDataFim(String dataTexto) {
        try {
            Date d = getSdfExibicao().parse(dataTexto);
            return d != null ? maximizarParaFimDia(d) : maximizarParaFimDia(new Date());
        } catch (Exception e) {
            return maximizarParaFimDia(new Date());
        }
    }

    /** Converte String DD/MM/AA e HH:mm → Date */
    public static Date parsearDataHora(String data, String hora) {
        try {
            return new SimpleDateFormat(FORMATO_EXIBICAO + " " + FORMATO_HORA, BR_LOCALE)
                    .parse(data + " " + hora);
        } catch (Exception e) {
            return new Date();
        }
    }

    /** Zera hora/minuto/segundo/ms de uma Date (início do dia) */
    public static Date truncarParaInicioDia(Date data) {
        Calendar c = Calendar.getInstance();
        c.setTime(data);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** Define hora 23:59:59.999 de uma Date (fim do dia) */
    public static Date maximizarParaFimDia(Date data) {
        Calendar c = Calendar.getInstance();
        c.setTime(data);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }
}