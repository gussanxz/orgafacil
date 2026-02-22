package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.ConfigsActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual.ResumoContasActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.util_helper.DialogLogoutHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

import java.util.concurrent.Executor;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private TextView textoContas, textoVendas, textoMercado, textoAtividades, textoConfigs;
    private View layoutPrincipal; // Para esconder o conteúdo até autenticar
    private PreferenciasRepository prefsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplica o tema imediatamente antes de inflar a View
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);

        prefsRepository = new PreferenciasRepository();

        inicializarComponentes();
        configurarBotoesBloqueados();
        configurarBotaoVoltar();

        // [SEGURANÇA] Biometria é checada ANTES de carregar qualquer dado sensível
        verificarSegurancaBiometrica();

        // Sincroniza preferências do Firestore
        carregarPreferenciasUsuario();
    }

    // --- LÓGICA DE SEGURANÇA BIOMÉTRICA ---

    private void verificarSegurancaBiometrica() {
        // Verifica no SharedPreferences local se o PIN está habilitado
        boolean pinObrigatorio = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE)
                .getBoolean("pin_obrigatorio", true);

        if (pinObrigatorio) {
            // Esconde o conteúdo sensível imediatamente para garantir privacidade
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.INVISIBLE);
            autenticarComDispositivo();
        } else {
            // Se não tiver proteção ativa, garante visibilidade
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
        }
    }

    private void autenticarComDispositivo() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Sucesso: Revela a interface do app
                if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                if (isFinishing() || isDestroyed()) return;

                // Se o usuário cancelar ou o dispositivo não tiver credenciais, fecha o app por segurança
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {

                    finishAffinity();
                } else {
                    Toast.makeText(HomeActivity.this, "Autenticação necessária: " + errString, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("OrgaFácil Protegido")
                .setSubtitle("Toque no sensor para acessar seus dados")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // --- COMPONENTES E INICIALIZAÇÃO ---

    private void inicializarComponentes() {
        layoutPrincipal = findViewById(R.id.main);

        // Ajuste de insets para EdgeToEdge
        if (layoutPrincipal != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutPrincipal, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        textoContas = findViewById(R.id.textViewContas);
        textoVendas = findViewById(R.id.textViewVendas);
        textoMercado = findViewById(R.id.textViewMercado);
        textoAtividades = findViewById(R.id.textViewAtividades);
        textoConfigs = findViewById(R.id.textViewConfigs);
    }

    private void configurarBotaoVoltar() {
        // Callback para gerenciar o encerramento da sessão ao voltar
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DialogLogoutHelper.mostrarDialogo(HomeActivity.this);
            }
        });
    }

    /**
     * CORREÇÃO: Sincronização de preferências adaptada para a nova estrutura de mapas.
     */
    private void carregarPreferenciasUsuario() {
        SharedPreferences sharedPreferences = getSharedPreferences(TemaHelper.PREF_NAME, MODE_PRIVATE);
        String temaCache = sharedPreferences.getString(TemaHelper.KEY_TEMA, PreferenciasModel.TEMA_SISTEMA);

        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                // CORREÇÃO: Acessando o tema através do sub-objeto 'Visual'
                if (prefs != null && prefs.getVisual() != null) {
                    String temaFirestore = prefs.getVisual().getTema();

                    // Se o tema no banco for diferente do cache local, sincroniza e aplica
                    if (!temaCache.equals(temaFirestore)) {
                        TemaHelper.aplicarTema(temaFirestore);
                    }
                }
            }
            @Override
            public void onErro(String erro) {
                Log.e(TAG, "Erro ao buscar preferências do Firestore: " + erro);
            }
        });
    }

    private void configurarBotoesBloqueados() {
        View.OnClickListener listenerBloqueio = view ->
                Toast.makeText(this, "Funcionalidade disponível em breve!", Toast.LENGTH_SHORT).show();

        int[] idsBloqueados = {R.id.imageViewMercado, R.id.imageViewTodo, R.id.imageViewBoletoCPF};
        for (int id : idsBloqueados) {
            View v = findViewById(id);
            if (v != null) v.setOnClickListener(listenerBloqueio);
        }
    }

    // --- MÉTODOS DE NAVEGAÇÃO ---

    public void acessarResumoContasActivity(View view) {
        startActivity(new Intent(this, ResumoContasActivity.class));
    }

    public void acessarResumoVendasAcitivity(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
    }

    public void acessarConfigs(View view) {
        startActivity(new Intent(this, ConfigsActivity.class));
    }
}