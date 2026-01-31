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
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

public class MainActivity extends IntroActivity {

    private GoogleLoginHelper googleLoginHelper;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usuarioService = new UsuarioService();
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // Google Auth → delega tudo ao Service
        googleLoginHelper = new GoogleLoginHelper(
                this,
                this::processarGoogle
        );

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
     * CALLBACK ÚNICO PARA GOOGLE
     * Nenhuma decisão aqui. Apenas delegação.
     */
    private void processarGoogle(int modoOperacao) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (loadingHelper != null) loadingHelper.exibir();

        usuarioService.processarLoginGoogle(
                user,
                // Usuário já existia
                () -> {
                    if (loadingHelper != null) loadingHelper.ocultar();
                    Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                    abrirTelaHome();
                },
                // Usuário acabou de ser criado
                () -> {
                    if (loadingHelper != null) loadingHelper.ocultar();
                    Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                    abrirTelaHome();
                },
                // Conta inválida / excluída
                () -> {
                    if (loadingHelper != null) loadingHelper.ocultar();
                    Toast.makeText(
                            this,
                            "Conta não encontrada. Crie uma nova conta.",
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    public void btEntrar(View view){
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btGoogle(View view) {
        resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            // MODO_MISTO continua válido
                            googleLoginHelper.lidarComResultadoGoogle(
                                    result.getData(),
                                    GoogleLoginHelper.MODO_MISTO
                            );
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
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result
                    ) {
                        if (onSuccess != null) onSuccess.run();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private boolean isPinObrigatorio() {
        SharedPreferences prefs =
                getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE);
        return prefs.getBoolean("pin_obrigatorio", true);
    }
}
