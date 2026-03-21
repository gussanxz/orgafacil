package com.gussanxz.orgafacil.funcionalidades.contas.notificacoes;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * WorkManager e Firebase precisam ser inicializados manualmente em testes
 * porque o ContentProvider automático não roda no ambiente Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
public class VencimentoWorkerTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();

        // Inicializa WorkManager com executor síncrono para testes
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config);

        // Inicializa Firebase com valores fictícios (só para não lançar IllegalStateException)
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx,
                    new FirebaseOptions.Builder()
                            .setApplicationId("1:000000000000:android:0000000000000000")
                            .setApiKey("fake-api-key")
                            .setProjectId("fake-project")
                            .build());
        }
    }

    // ── Worker ────────────────────────────────────────────────────────────────

    @Test
    public void worker_semUsuarioAutenticado_retornaSuccess() throws Exception {
        // Firebase inicializado mas sem login → getCurrentUser() = null
        // Worker deve retornar SUCCESS silenciosamente, sem crash
        VencimentoWorker worker = TestListenableWorkerBuilder
                .from(ctx, VencimentoWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);
    }

    // ── Canal de notificação ──────────────────────────────────────────────────

    @Test
    public void canal_criar_naoLancaExcecao() {
        try {
            VencimentoNotificationChannel.criar(ctx);
        } catch (Exception e) {
            fail("VencimentoNotificationChannel.criar() lançou: " + e.getMessage());
        }
    }

    @Test
    public void canal_criar_duasVezes_ehIdempotente() {
        try {
            VencimentoNotificationChannel.criar(ctx);
            VencimentoNotificationChannel.criar(ctx);
        } catch (Exception e) {
            fail("Segunda chamada lançou exceção: " + e.getMessage());
        }
    }

    @Test
    public void canal_channelId_naoEhNuloNemVazio() {
        assertNotNull(VencimentoNotificationChannel.CHANNEL_ID);
        assertFalse(VencimentoNotificationChannel.CHANNEL_ID.isEmpty());
    }

    // ── Scheduler ─────────────────────────────────────────────────────────────

    @Test
    public void scheduler_agendar_naoLancaExcecao() {
        try {
            VencimentoScheduler.agendar(ctx);
        } catch (Exception e) {
            fail("VencimentoScheduler.agendar() lançou: " + e.getMessage());
        }
    }

    @Test
    public void scheduler_agendarDuasVezes_ehIdempotente() {
        try {
            VencimentoScheduler.agendar(ctx);
            VencimentoScheduler.agendar(ctx);
        } catch (Exception e) {
            fail("Segunda chamada agendar() lançou: " + e.getMessage());
        }
    }

    @Test
    public void scheduler_cancelar_naoLancaExcecao() {
        try {
            VencimentoScheduler.agendar(ctx);
            VencimentoScheduler.cancelar(ctx);
        } catch (Exception e) {
            fail("cancelar() lançou: " + e.getMessage());
        }
    }

    @Test
    public void scheduler_workName_naoEhNuloNemVazio() {
        assertNotNull(VencimentoScheduler.WORK_NAME);
        assertFalse(VencimentoScheduler.WORK_NAME.isEmpty());
    }
}