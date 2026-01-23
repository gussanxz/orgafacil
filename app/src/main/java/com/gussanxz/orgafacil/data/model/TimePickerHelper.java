package com.gussanxz.orgafacil.data.model;

import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimePickerHelper {
    public static String horaAtual;
    public static void showTimePickerDialog (Context context, EditText campoHora) {
        campoHora.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hora = calendar.get(Calendar.HOUR_OF_DAY);
            int minuto =  calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        String horaFormatada = String.format("%02d:%02d", hourOfDay, minute);
                        campoHora.setText(horaFormatada);
                    },
                    hora,
                    minuto,
                    true
            );
            timePickerDialog.show();
        });
    }
    public static String setHoraAtual(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        horaAtual = sdf.format(new Date());
        return horaAtual;
    }
}
