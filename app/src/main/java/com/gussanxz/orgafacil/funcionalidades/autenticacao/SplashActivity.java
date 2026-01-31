package com.gussanxz.orgafacil.funcionalidades.autenticacao;

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
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.UsuarioRepository;

/**
 * SplashActivity
 * Atua como o "porteiro" do aplicativo, decidindo o destino do usuário.
 * Agora utiliza o UsuarioRepository para garantir a consistência dos dados antes de entrar.
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial do sistema
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            // Animação de Fade Out (Sumir suavemente)
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                    splashScreenView.getView(),
                    View.ALPHA,
                    1f,
                    0f
            );
            fadeOut.setDuration(500L); // 0.5 segundos
            fadeOut.setInterpolator(new AccelerateInterpolator());

            // Remove a view da splash quando a animação acabar
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    splashScreenView.remove();
                }
            });

            fadeOut.start();
        });

        super.onCreate(savedInstanceState);

        // Inicializa o repositório
        usuarioRepository = new UsuarioRepository();

        // 2. Mantém a logo na tela enquanto verificamos o login e os dados
        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarLogin();
    }

    private void verificarLogin() {
        FirebaseAuth autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();

        // Cenário A: Usuário não está logado
        if (usuarioAtual == null) {
            irParaIntro();
            return;
        }

        // Cenário B: Usuário logado. Vamos validar a sessão e a "Safety Net"
        usuarioAtual.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Se o login está OK, garantimos que o Firestore tenha o perfil/categorias
                garantirDadosFirestore(usuarioAtual);
            } else {
                tratarErroSessao(task.getException(), autenticacao);
            }
        });
    }

    /**
     * Usa a nossa Safety Net do Repository para garantir que o app
     * não abra sem os dados essenciais (mesmo em uma atualização ou reinstalação).
     */
    private void garantirDadosFirestore(FirebaseUser user) {
        usuarioRepository.garantirDadosIniciais(user, task -> irParaHome());
    }

    private void tratarErroSessao(Exception e, FirebaseAuth auth) {
        // Delegamos o mapeamento de erro para o Repository
        String mensagem = usuarioRepository.mapearErroAutenticacao(e);

        if (mensagem.contains("não cadastrado") || mensagem.contains("incorretos")) {
            auth.signOut();
            Toast.makeText(this, "Sessão expirada. Entre novamente.", Toast.LENGTH_LONG).show();
            irParaIntro();
        } else {
            // Em caso de erro de rede, permitimos entrar offline
            Toast.makeText(this, "Entrando offline...", Toast.LENGTH_SHORT).show();
            irParaHome();
        }
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