package com.gussanxz.orgafacil.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.contas.ContasActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import android.content.SharedPreferences;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;

public class MainActivity extends IntroActivity {

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //setContentView(R.layout.activity_main);

        verificarUsuarioLogado();

        setButtonBackVisible(false);
        setButtonNextVisible(false);

        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_1)
                .build()
        );
        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_2)
                .build()
        );
        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_3)
                .build()
        );
        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_4)
                .build()
        );
        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_cadastro)
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

    public void verificarUsuarioLogado(){
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        //autenticacao.signOut();
        if ( autenticacao.getCurrentUser() != null ){
            abrirTelaHome();
        }
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
    public void abrirTelaHome() {
        if (isPinObrigatorio()) {
            autenticarComDispositivo(() -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        } else {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    private void autenticarComDispositivo(Runnable onSuccess) {
        BiometricManager bm = BiometricManager.from(this);
        int can = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear OrgaFácil")
                .setSubtitle("Use biometria ou a credencial do dispositivo")
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