package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual.ResumoContasActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.util_helper.DialogLogoutHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

import java.util.concurrent.Executor;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private TextView textoContas, textoVendas, textoMercado, textoAtividades, textoConfigs;
    private View layoutPrincipal;
    private PreferenciasRepository prefsRepository;

    // Controle de sessão para evitar sobreposição de prompts
    private boolean autenticadoNestaSessao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);

        // [SEGURANÇA] Privacy Screen: Impede prints e esconde saldo na tela de apps recentes
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        prefsRepository = new PreferenciasRepository();
        inicializarComponentes();
        configurarBotoesBloqueados();
        configurarBotaoVoltar();

        // Primeira verificação ao abrir
        verificarSegurancaBiometrica();
        carregarPreferenciasUsuario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [SEGURANÇA] Verifica biometria sempre que o app volta para o primeiro plano
        verificarSegurancaBiometrica();
    }

    // --- LÓGICA DE SEGURANÇA BIOMÉTRICA ---

    private void verificarSegurancaBiometrica() {
        // Se já está autenticado e apenas navegou internamente, não pede de novo
        if (autenticadoNestaSessao) {
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
            return;
        }

        boolean pinObrigatorio = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE)
                .getBoolean("pin_obrigatorio", true);

        if (pinObrigatorio) {
            BiometricManager biometricManager = BiometricManager.from(this);
            int canAuthenticate = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL);

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                // Esconde conteúdo até o sucesso da digital
                if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.INVISIBLE);
                autenticarComDispositivo();
            } else {
                // Hardware indisponível ou digital não cadastrada: libera para não travar o usuário
                if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
        }
    }

    private void autenticarComDispositivo() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                autenticadoNestaSessao = true;
                if (layoutPrincipal != null) layoutPrincipal.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (isFinishing() || isDestroyed()) return;

                // Erros de cancelamento ou excesso de tentativas fecham o app por segurança
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    finishAffinity();
                } else {
                    Toast.makeText(HomeActivity.this, errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("OrgaFácil Protegido")
                .setSubtitle("Confirme sua identidade para acessar suas finanças")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Ao voltar para o app vindo de fora, resetamos o estado para exigir digital novamente
        autenticadoNestaSessao = false;
    }

    // --- COMPONENTES E INICIALIZAÇÃO ---

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
                DialogLogoutHelper.mostrarDialogo(HomeActivity.this);
            }
        });
    }

    private void carregarPreferenciasUsuario() {
        SharedPreferences sharedPreferences = getSharedPreferences(TemaHelper.PREF_NAME, MODE_PRIVATE);
        String temaCache = sharedPreferences.getString(TemaHelper.KEY_TEMA, PreferenciasModel.TEMA_SISTEMA);
        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                if (prefs != null && prefs.getVisual() != null) {
                    String temaFirestore = prefs.getVisual().getTema();
                    if (!temaCache.equals(temaFirestore)) {
                        TemaHelper.aplicarTema(temaFirestore);
                    }
                }
            }
            @Override
            public void onErro(String erro) { Log.e(TAG, erro); }
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

    // --- NAVEGAÇÃO ---

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