package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.comum.visual.ui.ContasActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.CadastroActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

/**
 * MainActivity (Intro)
 * Gerencia a apresentação inicial e o acesso rápido via Google com proteção de dados.
 */
public class MainActivity extends IntroActivity {

    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Inicialize os objetos ESSENCIAIS antes de qualquer check de login
        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();
        // Inicialize o helper do Google logo no início
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDadosGoogle);

        // 2. Agora sim, faça o Auto-Login
        if (com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession.isUserLogged()) {
            abrirTelaHome();
            super.onCreate(savedInstanceState); // Chamada mínima necessária para a biblioteca de Intro
            return;
        }

        super.onCreate(savedInstanceState);

        // [SEGURANÇA] Impede prints e gravação de tela
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        View overlay = findViewById(R.id.loading_overlay);
        if (overlay != null) loadingHelper = new LoadingHelper(overlay);

        setButtonBackVisible(false);
        setButtonNextVisible(false);

        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.ac_main_intro_comece_agora)
                .canGoForward(false)
                .build()
        );
    }

    /**
     * SAFETY NET: Verifica integridade entre Auth e Firestore.
     */
    private void iniciarFluxoSegurancaDadosGoogle() {
        if (!FirebaseSession.isUserLogged()) return;
        if (loadingHelper != null) loadingHelper.exibir();

        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                if (loadingHelper != null) loadingHelper.ocultar();
                abrirTelaHome();
            } else {
                // Caso o usuário se cadastre pelo botão do Google na tela inicial
                FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
                if (user != null) {
                    usuarioService.inicializarNovoUsuario(user, taskService -> {
                        if (loadingHelper != null) loadingHelper.ocultar();
                        abrirTelaHome();
                    });
                }
            }
        });
    }

    public void btEntrar(View view){
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btCadastrar(View view){
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void btGoogle(View view) {
        resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                } else {
                    if (loadingHelper != null) loadingHelper.ocultar();
                }
            }
    );

    // --- NAVEGAÇÃO COM BIOMETRIA ---

    public void abrirTelaHome() {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            verificarBiometriaENavegar(intent);
        });
    }

    private void verificarBiometriaENavegar(Intent intent) {
        if (isPinObrigatorio()) {
            autenticarComDispositivo(() -> {
                startActivity(intent);
                finish();
            });
        } else {
            startActivity(intent);
            finish();
        }
    }

    private void autenticarComDispositivo(Runnable onSuccess) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear OrgaFácil")
                .setSubtitle("Use biometria ou senha do dispositivo")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (onSuccess != null) onSuccess.run();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private boolean isPinObrigatorio() {
        return getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE).getBoolean("pin_obrigatorio", true);
    }
}