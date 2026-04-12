package com.gussanxz.orgafacil.funcionalidades.contas.notificacoes;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class VencimentoWorker extends Worker {

    private static final String TAG               = "VencimentoWorker";
    private static final int    NOTIF_ID_VENCIDAS = 1001;
    private static final int    NOTIF_ID_BREVE    = 1002;
    private static final long   TIMEOUT_SEGUNDOS  = 20;

    public VencimentoWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        // 1. Checar autenticação — FirestoreSchema.requireUid() lança exceção se não autenticado
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Usuário não autenticado — ignorando.");
            return Result.success();
        }

        // 2. Buscar movimentações não pagas
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<List<MovimentacaoModel>> resultMovRef    = new AtomicReference<>();
        AtomicReference<List<MovimentacaoModel>> resultFuturasRef = new AtomicReference<>();
        AtomicReference<Exception>               erroRef          = new AtomicReference<>();

        try {
            FirestoreSchema.contasMovimentacoesCol()
                    .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                    .get()
                    .addOnSuccessListener(snap -> {
                        List<MovimentacaoModel> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) {
                            MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                            m.setId(doc.getId());
                            lista.add(m);
                        }
                        resultMovRef.set(lista);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> { erroRef.set(e); latch.countDown(); });

            FirestoreSchema.contasFuturasCol()
                    .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                    .get()
                    .addOnSuccessListener(snap -> {
                        List<MovimentacaoModel> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) {
                            MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                            m.setId(doc.getId());
                            lista.add(m);
                        }
                        resultFuturasRef.set(lista);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> { erroRef.set(e); latch.countDown(); });

        } catch (Exception e) {
            // requireUid() lançou — usuário deslogou entre a checagem e a query
            AppLogger.e(TAG, "Erro ao montar query: " + e.getMessage());
            return Result.success();
        }

        // 3. Aguardar (Worker roda em thread background — latch é seguro aqui)
        try {
            if (!latch.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                AppLogger.w(TAG, "Timeout — RETRY agendado.");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        if (erroRef.get() != null) {
            AppLogger.e(TAG, "Erro Firestore: " + erroRef.get().getMessage());
            return Result.retry();
        }

        List<MovimentacaoModel> lista = new ArrayList<>();
        if (resultMovRef.get()    != null) lista.addAll(resultMovRef.get());
        if (resultFuturasRef.get() != null) lista.addAll(resultFuturasRef.get());
        if (lista.isEmpty()) return Result.success();

        // 4. Classificar usando os métodos prontos do Model
        List<MovimentacaoModel> vencidas = new ArrayList<>();
        List<MovimentacaoModel> emBreve  = new ArrayList<>();
        for (MovimentacaoModel m : lista) {
            if      (m.estaVencida())  vencidas.add(m);
            else if (m.venceEmBreve()) emBreve.add(m);
        }

        // 5. Disparar notificações separadas por tipo
        Context ctx = getApplicationContext();
        if (!vencidas.isEmpty()) notificar(ctx, NOTIF_ID_VENCIDAS,
                ctx.getString(R.string.notif_titulo_vencidas),
                buildTextoVencidas(ctx, vencidas));

        if (!emBreve.isEmpty()) notificar(ctx, NOTIF_ID_BREVE,
                ctx.getString(R.string.notif_titulo_em_breve),
                buildTextoEmBreve(ctx, emBreve));

        return Result.success();
    }

    // ── textos das notificações ───────────────────────────────────────────────

    private String buildTextoVencidas(Context ctx, List<MovimentacaoModel> lista) {
        if (lista.size() == 1) {
            MovimentacaoModel m = lista.get(0);
            return ctx.getString(R.string.notif_msg_vencida_singular,
                    nomeOuPadrao(m), Math.abs(m.diasParaVencimento()));
        }
        return ctx.getString(R.string.notif_msg_vencidas_plural, lista.size());
    }

    private String buildTextoEmBreve(Context ctx, List<MovimentacaoModel> lista) {
        if (lista.size() == 1) {
            MovimentacaoModel m = lista.get(0);
            int dias = m.diasParaVencimento();
            if (dias == 0) return ctx.getString(R.string.notif_msg_em_breve_hoje, nomeOuPadrao(m));
            return ctx.getString(R.string.notif_msg_em_breve_singular, nomeOuPadrao(m), dias);
        }
        return ctx.getString(R.string.notif_msg_em_breve_plural, lista.size());
    }

    private String nomeOuPadrao(MovimentacaoModel m) {
        String desc = m.getDescricao();
        return (desc != null && !desc.trim().isEmpty()) ? desc : m.getCategoria_nome();
    }

    // ── disparo ───────────────────────────────────────────────────────────────

    private void notificar(Context ctx, int id, String titulo, String mensagem) {
        android.content.Intent intent = new android.content.Intent(ctx,
                com.gussanxz.orgafacil.funcionalidades.contas.ContasActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                ctx, id, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(ctx, VencimentoNotificationChannel.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_hourglass_empty_24)
                        .setContentTitle(titulo)
                        .setContentText(mensagem)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, b.build());
    }
}