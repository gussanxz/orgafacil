package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;

/**
 * SplashActivity
 * Atua como o "porteiro" do aplicativo, decidindo o destino do usuário. [cite: 2026-01-31]
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private ConfigPerfilUsuarioRepository perfilRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial do sistema
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        configurarAnimacaoSaida(splashScreen);

        super.onCreate(savedInstanceState);

        // Inicializa a camada de repositório [cite: 2025-11-10]
        perfilRepository = new ConfigPerfilUsuarioRepository();

        // 2. Mantém a logo na tela enquanto verificamos a sessão [cite: 2026-01-31]
        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarSessao();
    }

    private void verificarSessao() {
        // Usa a Session para verificar o estado [cite: 2025-11-10]
        if (!FirebaseSession.isUserLogged()) {
            irParaIntro();
            return;
        }

        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();

        // TRAVA 1: Sincroniza com o servidor Auth para checar se o UID ainda é válido [cite: 2026-01-31]
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                garantirIntegridadeFirestore();
            } else {
                // Sessão inválida ou usuário deletado no console [cite: 2026-01-31]
                FirebaseSession.logOut();
                irParaIntro();
            }
        });
    }

    /**
     * TRAVA 2: Verifica se os dados existem no banco.
     * Agora usa o método stateless do repository. [cite: 2025-11-10, 2026-01-31]
     */
    private void garantirIntegridadeFirestore() {
        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                // Tudo certo: Auth e Banco sincronizados [cite: 2026-01-31]
                irParaHome();
            } else {
                // Sessão órfã: existe login mas os dados sumiram [cite: 2026-01-31]
                FirebaseSession.logOut();
                irParaIntro();
            }
        });
    }

    // --- Navegação ---

    private void irParaHome() {
        isVerificacaoConcluida = true;
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void irParaIntro() {
        isVerificacaoConcluida = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void configurarAnimacaoSaida(SplashScreen splashScreen) {
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(splashScreenView.getView(), View.ALPHA, 1f, 0f);
            fadeOut.setDuration(500L);
            fadeOut.setInterpolator(new AccelerateInterpolator());
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    splashScreenView.remove();
                }
            });
            fadeOut.start();
        });
    }
}