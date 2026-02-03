package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Importado para o delay do Splash
import android.view.View;
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
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.CadastroActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

/**
 * MainActivity (Intro)
 * Gerencia a apresentação inicial, Auto-Login e validação de status da conta.
 */
public class MainActivity extends IntroActivity {

    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Inicialização de objetos essenciais
        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDadosGoogle);

        // 2. AUTO-LOGIN com Delay para Splash e Validação de Status
        if (FirebaseSession.isUserLogged()) {
            // [MODIFICADO] Adiciona 2 segundos para o Splash aparecer antes de processar
            new Handler().postDelayed(this::verificarStatusEBaterPonto, 2000);

            // Define o layout de carregamento/splash enquanto o Handler espera
            setContentView(R.layout.util_loading);
            super.onCreate(savedInstanceState);
            return;
        }

        super.onCreate(savedInstanceState);

        // [SEGURANÇA] Bloqueia capturas de tela
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        View overlay = findViewById(R.id.loading_overlay);
        if (overlay != null) loadingHelper = new LoadingHelper(overlay);

        configurarSlides();
    }

    /**
     * Checa se a conta está ativa no Firestore antes de permitir o acesso.
     */
    private void verificarStatusEBaterPonto() {
        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
        if (user == null) return;

        perfilRepository.obterMetadadosRaiz(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String status = task.getResult().getString("statusConta");

                // [CORRIGIDO] Se desativada, usa o seu método de Diálogo de Reativação
                if (ConfigPerfilUsuarioModel.StatusConta.DESATIVADO.name().equals(status)) {
                    exibirDialogoReativacao(user.getUid());
                } else {
                    abrirTelaHome();
                }
            } else {
                abrirTelaHome();
            }
        });
    }

    private void configurarSlides() {
        setButtonBackVisible(false);
        setButtonNextVisible(false);

        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.ac_main_intro_comece_agora)
                .canGoForward(false)
                .build()
        );
    }

    private void iniciarFluxoSegurancaDadosGoogle() {
        if (!FirebaseSession.isUserLogged()) return;
        if (loadingHelper != null) loadingHelper.exibir();

        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                if (loadingHelper != null) loadingHelper.ocultar();
                abrirTelaHome();
            } else {
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

    public void btEntrar(View view) {
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btCadastrar(View view) {
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

    private void exibirDialogoReativacao(String uid) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Conta Desativada")
                .setMessage("Identificamos que sua conta está desativada. Deseja reativá-la agora para acessar seus dados?")
                .setCancelable(false)
                .setPositiveButton("Reativar Conta", (dialog, which) -> {
                    perfilRepository.reativarContaLogica(uid).addOnSuccessListener(v -> {
                        Toast.makeText(this, "Bem-vindo de volta! Conta reativada.", Toast.LENGTH_SHORT).show();
                        abrirTelaHome();
                    });
                })
                .setNegativeButton("Sair", (dialog, which) -> {
                    perfilRepository.deslogar();
                    recreate();
                })
                .show();
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