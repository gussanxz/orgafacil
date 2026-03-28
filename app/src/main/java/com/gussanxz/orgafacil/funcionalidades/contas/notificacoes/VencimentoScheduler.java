package com.gussanxz.orgafacil.funcionalidades.contas.notificacoes;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class VencimentoScheduler {

    public static final String WORK_NAME = "vencimento_diario";

    private VencimentoScheduler() {}

    public static void agendar(Context ctx) {
        Constraints restricoes = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                VencimentoWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(restricoes)
                .setInitialDelay(calcularAtrasoInicial(), TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work
        );
    }

    public static void cancelar(Context ctx) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME);
    }

    private static long calcularAtrasoInicial() {
        Calendar agora = Calendar.getInstance();
        Calendar alvo  = Calendar.getInstance();
        alvo.set(Calendar.HOUR_OF_DAY, 9);
        alvo.set(Calendar.MINUTE,      0);
        alvo.set(Calendar.SECOND,      0);
        alvo.set(Calendar.MILLISECOND, 0);
        if (alvo.before(agora)) alvo.add(Calendar.DAY_OF_YEAR, 1);
        return alvo.getTimeInMillis() - agora.getTimeInMillis();
    }
}