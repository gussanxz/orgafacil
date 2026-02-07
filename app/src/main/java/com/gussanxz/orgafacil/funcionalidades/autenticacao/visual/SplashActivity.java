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
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

/**
 * SplashActivity
 * Gerencia a entrada, animação e verifica o STATUS da conta (Ativo/Desativado).
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial do sistema
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        // Configura animação ANTES do super.onCreate
        configurarAnimacaoSaida(splashScreen);

        super.onCreate(savedInstanceState);

        usuarioRepository = new UsuarioRepository();

        // 2. Mantém a logo na tela enquanto verificamos a sessão
        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarSessao();
    }

    private void verificarSessao() {
        if (ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser() == null) {
            irParaIntro();
            return;
        }

        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                validarStatusNoBanco(user.getUid());
            } else {
                FirebaseSession.logOut(this);
                irParaIntro();
            }
        });
    }

    private void validarStatusNoBanco(String uid) {
        usuarioRepository.verificarSeUsuarioExiste(uid).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();

                if (doc.exists()) {
                    try {
                        UsuarioModel usuario = doc.toObject(UsuarioModel.class);

                        // [CORREÇÃO]: Comparação segura de String vs Enum.name()
                        // Usamos o .equals() porque getStatus() retorna String no novo Model.
                        if (usuario != null &&
                                UsuarioModel.StatusConta.ATIVO.name().equals(usuario.getStatus())) {

                            usuarioRepository.atualizarUltimaAtividade();
                            irParaHome();

                        } else {
                            // Conta DESATIVADA ou SUSPENSA -> Logout
                            FirebaseSession.logOut(this);
                            irParaIntro();
                        }
                    } catch (Exception e) {
                        FirebaseSession.logOut(this);
                        irParaIntro();
                    }
                } else {
                    FirebaseSession.logOut(this);
                    irParaIntro();
                }
            } else {
                irParaIntro();
            }
        });
    }

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