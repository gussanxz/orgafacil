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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity; // Assumindo que esta é a tela de "Bem-vindo/Opções"
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

/**
 * SplashActivity
 * Gerencia a entrada, animação e verifica o STATUS da conta (Ativo/Desativado).
 */
public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;
    private UsuarioRepository usuarioRepository; // Renomeado para consistência

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen oficial do sistema
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        configurarAnimacaoSaida(splashScreen);

        super.onCreate(savedInstanceState);

        // Se você tiver um layout XML para a splash (para versões antigas do Android), set aqui.
        // Se for só API 12+, pode não precisar de setContentView se o tema cuidar disso.
        // setContentView(R.layout.ac_splash);

        usuarioRepository = new UsuarioRepository();

        // 2. Mantém a logo na tela enquanto verificamos a sessão
        splashScreen.setKeepOnScreenCondition(() -> !isVerificacaoConcluida);

        verificarSessao();
    }

    private void verificarSessao() {
        // Se não tem usuário no Auth, vai pra Intro
        if (ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser() == null) {
            irParaIntro();
            return;
        }

        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();

        // Recarrega o usuário para garantir que não foi desabilitado no console do Firebase
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                validarStatusNoBanco(user.getUid());
            } else {
                // Token expirou ou usuário deletado no console
                FirebaseSession.logOut(this);
                irParaIntro();
            }
        });
    }

    /**
     * Verifica se o usuário existe E se está ATIVO.
     */
    private void validarStatusNoBanco(String uid) {
        usuarioRepository.verificarSeUsuarioExiste(uid).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();

                if (doc.exists()) {
                    try {
                        UsuarioModel usuario = doc.toObject(UsuarioModel.class);

                        // [LÓGICA CRÍTICA] Só entra se estiver ATIVO.
                        if (usuario != null && usuario.getStatus() == UsuarioModel.StatusConta.ATIVO) {
                            usuarioRepository.atualizarUltimaAtividade();
                            irParaHome();
                        } else {
                            // Se estiver DESATIVADO, SUSPENSO ou null -> Manda pro Login/Intro.
                            // Lá na Intro/Login, ao tentar logar, a BaseAuthActivity vai oferecer a Reativação.
                            FirebaseSession.logOut(this);
                            irParaIntro();
                        }
                    } catch (Exception e) {
                        // Erro ao converter dados (banco corrompido ou antigo) -> Logout por segurança
                        FirebaseSession.logOut(this);
                        irParaIntro();
                    }
                } else {
                    // Logado no Auth mas sem dados no Banco (Inconsistência) -> Logout
                    FirebaseSession.logOut(this);
                    irParaIntro();
                }
            } else {
                // Erro de conexão ou Firestore offline -> Tenta mandar pro login
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
        // Importante: Certifique-se que MainActivity aqui é a tela de "Bem-vindo / Login / Cadastro"
        // e não aquela MainActivity "Dispatcher" que criamos no passo anterior.
        // Se a MainActivity for o Dispatcher, aponte aqui direto para LoginActivity.class
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