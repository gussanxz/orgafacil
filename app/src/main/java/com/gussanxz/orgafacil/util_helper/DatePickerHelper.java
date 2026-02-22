package com.gussanxz.orgafacil.util_helper;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatePickerHelper {
    private static final String TAG  =  "DatePickerHelper";
    public static String mesAnoDataEscolhida = "";
    public static String dataAtual;

    public static void showDatePickerDialog(Context context, final EditText editText) {
        // Obtem a data atual
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dataSelecionada = sdf.format(calendar.getTime());

                        editText.setText(dataSelecionada);

                        // Agora extraímos mês e ano
                        String[] dataSeparada = dataSelecionada.split("/");
                        String mes = dataSeparada[1]; // mês
                        String ano = dataSeparada[2]; // ano

                        String mesAnoDataEscolhida = mes + ano; // Ex: 04 + 2025 = "042025"

                        Log.i(TAG, "mesAno: " + mesAnoDataEscolhida);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }
    public static String mesAnoDataEscolhida( String data ) {

        String retornoData[] = data.split("/"); //ex: 23/01/2018
        String dia = retornoData[0]; //dia 23
        String mes = retornoData[1]; //mes 01
        String ano = retornoData[2]; //ano 2018
        Log.i(TAG, "Data informada: " + mes + "/" + ano );
        String mesAno = mes + ano;
        return  mesAno;

    }
    public static String setDataAtual(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dataAtual = sdf.format(new Date());
        return dataAtual;
    }
}
