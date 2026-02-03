package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
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
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

public class MainActivity extends IntroActivity {

    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa as camadas de dados através da arquitetura stateless [cite: 2025-11-10]
        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();

        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // Helper utiliza a Session para gerenciar o estado da autenticação [cite: 2025-11-10]
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDadosGoogle);

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
     * Fluxo de segurança que utiliza a Session para validar o usuário [cite: 2025-11-10, 2026-01-31]
     */
    private void iniciarFluxoSegurancaDadosGoogle() {
        if (!FirebaseSession.isUserLogged()) return;

        if (loadingHelper != null) loadingHelper.exibir();

        // O repositório agora obtém o UID automaticamente via Session [cite: 2025-11-10]
        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                if (loadingHelper != null) loadingHelper.ocultar();
                Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                abrirTelaHome();
            } else {
                Toast.makeText(this, "Criando sua conta...", Toast.LENGTH_SHORT).show();
                // O Service também utiliza a Session internamente [cite: 2025-11-10]
                FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
                usuarioService.inicializarNovoUsuario(user, taskService -> {
                    if (loadingHelper != null) loadingHelper.ocultar();
                    abrirTelaHome();
                });
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
                    googleLoginHelper.lidarComResultadoGoogle(result.getData(), GoogleLoginHelper.MODO_MISTO);
                }
            }
    );

    public void abrirTelaHome() {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, HomeActivity.class);
            verificarBiometriaENavegar(intent);
        });
    }

    public void abrirTelaContas() {
        Intent intent = new Intent(this, ContasActivity.class);
        verificarBiometriaENavegar(intent);
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
                .setSubtitle("Use biometria ou senha do celular")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt prompt = new BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (onSuccess != null) onSuccess.run();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private boolean isPinObrigatorio() {
        SharedPreferences prefs = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE);
        return prefs.getBoolean("pin_obrigatorio", true);
    }
}