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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;

/**
 * SplashActivity
 * Atua como o "porteiro" do aplicativo.
 *
 * REGRA:
 * - Nunca cria dados
 * - Nunca chama Service
 * - Apenas valida estado
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private ConfigPerfilUsuarioRepository perfilRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(view -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                    view.getView(),
                    View.ALPHA,
                    1f,
                    0f
            );
            fadeOut.setDuration(500);
            fadeOut.setInterpolator(new AccelerateInterpolator());
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.remove();
                }
            });
            fadeOut.start();
        });

        super.onCreate(savedInstanceState);

        perfilRepository = new ConfigPerfilUsuarioRepository();

        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarSessao();
    }

    /**
     * Fluxo único e determinístico
     */
    private void verificarSessao() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            irParaIntro();
            return;
        }

        // Confirma com o servidor se o usuário ainda existe
        user.reload().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                FirebaseAuth.getInstance().signOut();
                irParaIntro();
                return;
            }

            verificarPerfilFirestore(user);
        });
    }

    /**
     * Verifica se o perfil existe no Firestore
     */
    private void verificarPerfilFirestore(FirebaseUser user) {
        perfilRepository.verificarExistenciaPerfil(user.getUid())
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().exists()) {

                        irParaHome();

                    } else {
                        FirebaseAuth.getInstance().signOut();
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
}
