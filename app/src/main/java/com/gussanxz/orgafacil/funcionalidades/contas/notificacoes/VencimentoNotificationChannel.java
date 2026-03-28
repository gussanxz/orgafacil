package com.gussanxz.orgafacil.funcionalidades.contas.notificacoes;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class VencimentoNotificationChannel {

    public static final String CHANNEL_ID   = "channel_vencimentos";
    public static final String CHANNEL_NAME = "Vencimentos";
    public static final String CHANNEL_DESC = "Alertas de contas vencidas e próximas do vencimento";

    private VencimentoNotificationChannel() {}

    public static void criar(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(CHANNEL_DESC);
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(canal);
    }
}