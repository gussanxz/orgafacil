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
import com.gussanxz.orgafacil.funcionalidades.configuracoes.visual.ConfigsActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual.ResumoContasActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.PreferenciasModel;
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
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);

        prefsRepository = new PreferenciasRepository();

        inicializarComponentes();
        configurarBotoesBloqueados();
        configurarBotaoVoltar();

        // [SEGURANÇA] Biometria é checada ANTES de carregar qualquer dado
        verificarSegurancaBiometrica();

        carregarPreferenciasUsuario();
    }

    // --- NOVA LÓGICA DE SEGURANÇA ---

    private void verificarSegurancaBiometrica() {
        boolean pinObrigatorio = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE)
                .getBoolean("pin_obrigatorio", true);

        if (pinObrigatorio) {
            // Esconde o conteúdo sensível imediatamente
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.INVISIBLE);
            autenticarComDispositivo();
        } else {
            // Se não tiver PIN, garante que está visível
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
        }
    }

    private void autenticarComDispositivo() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Sucesso: Mostra a tela
                if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                // Evita crash se a activity já estiver fechando
                if (isFinishing() || isDestroyed()) return;

                // Erro crítico ou cancelamento pelo usuário: Fecha o app para proteger os dados
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {

                    finishAffinity(); // Fecha o app todo
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

    // --- FIM DA LÓGICA DE SEGURANÇA ---

    private void inicializarComponentes() {
        layoutPrincipal = findViewById(R.id.main);

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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // [CORREÇÃO] O Helper atual só pede o Contexto, não precisa passar Repository
                DialogLogoutHelper.mostrarDialogo(HomeActivity.this);
            }
        });
    }

    private void carregarPreferenciasUsuario() {
        // Usa SharedPreferences padrão para comparar o cache local
        SharedPreferences sharedPreferences = getSharedPreferences(TemaHelper.PREF_NAME, MODE_PRIVATE);
        String temaCache = sharedPreferences.getString(TemaHelper.KEY_TEMA, PreferenciasModel.TEMA_SISTEMA);

        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                if (prefs != null) {
                    // Se o tema no banco for diferente do cache atual, aplica e recria
                    if (!temaCache.equals(prefs.getTema())) {
                        TemaHelper.aplicarTema(prefs.getTema());
                    }
                }
            }
            @Override
            public void onErro(String erro) {
                Log.e(TAG, "Erro ao buscar preferências: " + erro);
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

    // --- Navegação ---

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