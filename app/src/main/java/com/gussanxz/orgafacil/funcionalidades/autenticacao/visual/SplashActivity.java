package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;

/**
 * SplashActivity
 * Atua como o "porteiro" do aplicativo, decidindo o destino do usuário.
 * Blindada contra documentos fantasmas via reload() e verificação de integridade.
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial do sistema
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                    splashScreenView.getView(),
                    View.ALPHA,
                    1f,
                    0f
            );
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

        super.onCreate(savedInstanceState);

        // Inicializa a camada de dados e serviços [cite: 2025-11-10]
        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();

        // 2. Mantém a logo na tela enquanto verificamos o login e os dados
        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarLogin();
    }

    private void verificarLogin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            irParaIntro();
            return;
        }

        // TRAVA 1: O reload pergunta ao servidor se o UID ainda existe [cite: 2026-01-31]
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Usuário real e ativo no Firebase Auth. Prossiga para o Firestore.
                garantirDadosFirestore(user);
            } else {
                // FALHA: Usuário deletado no console ou excluído. [cite: 2026-01-31]
                // Limpa o lixo local para evitar recriação fantasma [cite: 2025-11-10].
                FirebaseAuth.getInstance().signOut();
                irParaIntro();
            }
        });
    }

    /**
     * TRAVA 2: Verifica se o perfil existe no Firestore.
     * Se não existir, deslogamos para evitar a criação automática na LoginActivity.
     */
    private void garantirDadosFirestore(FirebaseUser user) {
        perfilRepository.verificarExistenciaPerfil(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {

                if (task.getResult().exists()) {
                    // Cenário: Integridade total. [cite: 2026-01-31]
                    irParaHome();
                } else {
                    // Cenário: Sessão órfã (Existe no Auth mas não no Firestore). [cite: 2026-01-31]
                    // Deslogamos aqui para que a LoginActivity não tente recriar via Safety Net.
                    FirebaseAuth.getInstance().signOut();
                    irParaIntro();
                }

            } else {
                // Erro de conexão ou permissão: manda para Intro por segurança.
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