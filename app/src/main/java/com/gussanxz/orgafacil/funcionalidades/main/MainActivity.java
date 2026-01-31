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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.CadastroActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository; // Adicionado [cite: 2026-01-22]
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService; // Adicionado [cite: 2025-11-10]
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper; // Adicionado [cite: 2025-11-10]
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

public class MainActivity extends IntroActivity {

    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository; // [cite: 2026-01-22]
    private UsuarioService usuarioService; // [cite: 2025-11-10]
    private LoadingHelper loadingHelper; // Substituindo o AlertDialog antigo [cite: 2025-11-10]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa as camadas de dados [cite: 2025-11-10, 2026-01-22]
        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();

        // Altere para o ID do seu loading overlay no layout ac_main_intro_comece_agora
        // Se não houver um overlay nesse layout específico, você pode usar um Toast ou Dialog provisório
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // Ajuste no Helper: Agora ele chama a Safety Net unificada [cite: 2026-01-31]
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDadosGoogle);

        // Configuração da Intro
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
     * SAFETY NET UNIFICADA PARA O BOTÃO GOOGLE DA INTRO [cite: 2026-01-31]
     */
    private void iniciarFluxoSegurancaDadosGoogle() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (loadingHelper != null) loadingHelper.exibir();

        // Verifica se o usuário já existe no Firestore [cite: 2026-01-31]
        perfilRepository.verificarExistenciaPerfil(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                // CENÁRIO: Usuário antigo. [cite: 2026-01-31]
                if (loadingHelper != null) loadingHelper.ocultar();
                Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                abrirTelaHome();
            } else {
                // CENÁRIO: Usuário novo ou dados deletados. [cite: 2026-01-31]
                Toast.makeText(this, "Criando sua conta... Bem-vindo!", Toast.LENGTH_SHORT).show();
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

    // --- NAVEGAÇÃO E BIOMETRIA ---
    public void abrirTelaHome() {
        runOnUiThread(() -> {
            if (isPinObrigatorio()) {
                autenticarComDispositivo(() -> {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
            } else {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }
        });
    }

    // ... (restante dos métodos de biometria permanecem iguais)

    public void abrirTelaContas() {
        if (isPinObrigatorio()) {
            autenticarComDispositivo(() -> {
                startActivity(new Intent(this, ContasActivity.class));
                finish();
            });
        } else {
            startActivity(new Intent(this, ContasActivity.class));
            finish();
        }
    }

    private void autenticarComDispositivo(Runnable onSuccess) {
        BiometricManager bm = BiometricManager.from(this);

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