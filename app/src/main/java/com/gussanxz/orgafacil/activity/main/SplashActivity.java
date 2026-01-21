package com.gussanxz.orgafacil.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.activity.main.HomeActivity;
import com.gussanxz.orgafacil.activity.main.MainActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;

public class SplashActivity extends AppCompatActivity {

    private boolean isVerificacaoConcluida = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Instala a Splash Screen NOVA (Antes do super.onCreate)
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // 2. Não precisa de setContentView! A tela vai continuar mostrando a logo do sistema.

        // 3. Configura a condição para MANTER a logo na tela
        splashScreen.setKeepOnScreenCondition(() -> {
            // Enquanto for 'true', a logo fica travada na tela.
            // Quando virar 'false', a logo some e o app abre.
            return !isVerificacaoConcluida;
        });

        // 4. Inicia a verificação
        verificarLogin();
    }

    private void verificarLogin() {
        FirebaseAuth autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();

        // Se não tiver cache local, libera logo
        if (usuarioAtual == null) {
            irParaIntro();
            return;
        }

        // Se tiver cache, valida no servidor
        usuarioAtual.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                irParaHome();
            } else {
                try {
                    throw task.getException();
                }
                catch (com.google.firebase.auth.FirebaseAuthInvalidUserException e) {
                    autenticacao.signOut();
                    Toast.makeText(this, "Conta não existe mais.", Toast.LENGTH_LONG).show();
                    irParaIntro();
                }
                catch (com.google.firebase.FirebaseNetworkException e) {
                    Toast.makeText(this, "Sem internet. Entrando offline...", Toast.LENGTH_SHORT).show();
                    irParaHome(); // Permite offline
                }
                catch (Exception e) {
                    autenticacao.signOut();
                    irParaIntro();
                }
            }
        });
    }

    private void irParaHome() {
        isVerificacaoConcluida = true; // Libera a Splash do sistema
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void irParaIntro() {
        isVerificacaoConcluida = true; // Libera a Splash do sistema
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}