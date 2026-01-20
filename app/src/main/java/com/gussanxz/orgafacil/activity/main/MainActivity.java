package com.gussanxz.orgafacil.activity.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.contas.ContasActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.GoogleLoginHelper; // IMPORTANTE
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

public class MainActivity extends IntroActivity {

    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper; // Instância do nosso novo Helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inicializa o Helper
        // Passamos "this" (a activity) e uma ação para executar quando o login der certo (abrirTelaHome)
        googleLoginHelper = new GoogleLoginHelper(this, this::abrirTelaHome);

        verificarUsuarioLogado();

        setButtonBackVisible(false);
        setButtonNextVisible(false);

        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.ac_main_intro_comece_agora)
                .canGoForward(false)
                .build()
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        verificarUsuarioLogado();
    }

    public void btEntrar(View view){
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btCadastrar(View view){
        startActivity(new Intent(this, CadastroActivity.class));
    }

    // --- LÓGICA GOOGLE SIMPLIFICADA ---

    public void btGoogle(View view) {
        // Usa o helper para pegar a intent
        resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Passa o resultado para o helper resolver a lógica
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                }
            }
    );

    // --- NAVEGAÇÃO E BIOMETRIA (MANTIDO) ---

    public void verificarUsuarioLogado(){
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        if ( autenticacao.getCurrentUser() != null ){
            abrirTelaHome();
        }
    }

    public void abrirTelaHome() {
        // Se a chamada vier de um callback do helper, garantimos que roda na Thread principal
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