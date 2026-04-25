package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.boletos.BoletosActivity;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.ConfigsActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual.ResumoContasActivity;
import com.gussanxz.orgafacil.funcionalidades.mercado.ui.activities.ResumoListaMercadoActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.util_helper.AppLogger;
import com.gussanxz.orgafacil.util_helper.DialogLogoutHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;
import com.gussanxz.orgafacil.util_helper.SecurityConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * HomeActivity
 *
 * Tela inicial do OrgaFácil. Exibe a grade de módulos disponíveis.
 *
 * Responsabilidades desta Activity:
 *  1. Aplicar o tema e os insets de sistema (status bar / nav bar).
 *  2. Preencher os cards usando {@link HomeCardBinder} com dados de {@link HomeMenuConfig}.
 *  3. Carregar e exibir o nome do usuário autenticado.
 *  4. Gerenciar autenticação biométrica (bloqueio por inatividade).
 *  5. Verificar e solicitar permissões de runtime.
 *
 * O que NÃO está aqui (princípio da responsabilidade única):
 *  - Configuração visual dos cards → HomeCardBinder
 *  - Definição dos itens do menu   → HomeMenuConfig
 *  - Modelo de um item             → HomeMenuItem
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // ─────────────────────────────────────────────────────────────
    // Views
    // ─────────────────────────────────────────────────────────────

    /** Raiz do layout — ocultada durante autenticação biométrica */
    private View layoutPrincipal;

    private TextView textViewNomeUsuario;

    /**
     * Cards na mesma ordem dos índices em {@link HomeMenuConfig}.
     * A posição do array é a única ligação entre Activity e Config.
     */
    private CardView[] cards;

    // ─────────────────────────────────────────────────────────────
    // Estado
    // ─────────────────────────────────────────────────────────────

    private PreferenciasRepository prefsRepository;
    private boolean autenticadoNestaSessao = false;
    private long ultimoBackgroundTime = 0;

    // ─────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_home);

        prefsRepository = new PreferenciasRepository();

        inicializarViews();
        vincularCards();
        configurarCardsBloqueados();
        configurarBotaoVoltar();

        verificarSegurancaBiometrica();
        carregarNomeUsuario();
        carregarPreferenciasUsuario();
        solicitarPermissoes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarSegurancaBiometrica();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ultimoBackgroundTime = System.currentTimeMillis();
    }

    // ─────────────────────────────────────────────────────────────
    // Inicialização
    // ─────────────────────────────────────────────────────────────

    private void inicializarViews() {
        layoutPrincipal      = findViewById(R.id.main);
        textViewNomeUsuario  = findViewById(R.id.textViewNomeUsuario);

        // Trata insets de sistema (edge-to-edge)
        if (layoutPrincipal != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutPrincipal, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }
    }

    /**
     * Vincula cada CardView ao seu respectivo {@link HomeMenuItem}.
     * A ordem do array espelha exatamente {@link HomeMenuConfig#obterItens()}.
     */
    private void vincularCards() {
        cards = new CardView[] {
                findViewById(R.id.cardContas),       // IDX_CONTAS       = 0
                findViewById(R.id.cardVendas),       // IDX_VENDAS       = 1
                findViewById(R.id.cardMercado),      // IDX_MERCADO      = 2
                findViewById(R.id.cardAtividades),   // IDX_ATIVIDADES   = 3
                findViewById(R.id.cardBoleto),       // IDX_BOLETO       = 4
                findViewById(R.id.cardMinhaConta),   // IDX_MINHA_CONTA  = 5
        };

        HomeMenuItem[] itens = HomeMenuConfig.obterItens();

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null && i < itens.length) {
                HomeCardBinder.bind(cards[i], itens[i], this);
            }
        }
    }

    /**
     * Configura o click de "Em breve" nos cards desabilitados.
     * Os cards habilitados já possuem android:onClick declarado no XML.
     */
    private void configurarCardsBloqueados() {
        HomeMenuItem[] itens = HomeMenuConfig.obterItens();
        View.OnClickListener listenerBloqueio = v ->
                Toast.makeText(this,
                        R.string.aviso_funcionalidade_em_breve,
                        Toast.LENGTH_SHORT).show();

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null && i < itens.length && !itens[i].isHabilitado()) {
                cards[i].setOnClickListener(listenerBloqueio);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Nome do usuário
    // ─────────────────────────────────────────────────────────────

    /**
     * Carrega o primeiro nome do usuário autenticado via FirebaseAuth.
     * Usa a saudação correta de acordo com a hora do dia.
     */
    private void carregarNomeUsuario() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || textViewNomeUsuario == null) return;

        String nomeCompleto = usuario.getDisplayName();
        String primeiroNome = extrairPrimeiroNome(nomeCompleto);
        textViewNomeUsuario.setText(primeiroNome);
    }

    /**
     * Retorna apenas o primeiro nome de um nome completo.
     * Ex: "Gustavo Santos" → "Gustavo Santos" (mantém completo se for curto,
     * retorna "Usuário" se vier nulo).
     */
    private String extrairPrimeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) {
            return getString(R.string.nome_usuario_padrao);
        }
        return nomeCompleto.trim();
    }

    // ─────────────────────────────────────────────────────────────
    // Preferências e tema
    // ─────────────────────────────────────────────────────────────

    private void carregarPreferenciasUsuario() {
        SharedPreferences cache = getSharedPreferences(TemaHelper.PREF_NAME, MODE_PRIVATE);
        String temaCache = cache.getString(TemaHelper.KEY_TEMA, PreferenciasModel.TEMA_SISTEMA);

        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                if (prefs == null || prefs.getVisual() == null) return;
                String temaFirestore = prefs.getVisual().getTema();
                if (!temaCache.equals(temaFirestore)) {
                    TemaHelper.aplicarTema(temaFirestore);
                }
            }

            @Override
            public void onErro(String erro) {
                AppLogger.e(TAG, erro);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Navegação
    // ─────────────────────────────────────────────────────────────

    public void acessarResumoContasActivity(View view) {
        startActivity(new Intent(this, ResumoContasActivity.class));
    }

    public void acessarResumoVendasAcitivity(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
    }

    public void acessarListaMercado(View view) {
        startActivity(new Intent(this, ResumoListaMercadoActivity.class));
    }

    public void acessarConfigs(View view) {
        startActivity(new Intent(this, ConfigsActivity.class));
    }

    public void acessarBoletos(View view) {
        startActivity(new Intent(this, BoletosActivity.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Botão voltar
    // ─────────────────────────────────────────────────────────────

    private void configurarBotaoVoltar() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        DialogLogoutHelper.mostrarDialogo(HomeActivity.this);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Segurança biométrica (mantida sem alterações de lógica)
    // ─────────────────────────────────────────────────────────────

    private void verificarSegurancaBiometrica() {

        SharedPreferences prefs =
                getSharedPreferences(SecurityConstants.PREF_NAME, MODE_PRIVATE);

        boolean pinObrigatorio =
                prefs.getBoolean(SecurityConstants.KEY_PIN_OBRIGATORIO,
                        SecurityConstants.DEFAULT_PIN_OBRIGATORIO);

        if (!pinObrigatorio) {
            layoutPrincipal.setVisibility(View.VISIBLE);
            return;
        }

        long tempoEmBackground =
                System.currentTimeMillis() - ultimoBackgroundTime;

        if (autenticadoNestaSessao &&
                tempoEmBackground < SecurityConstants.TEMPO_MAXIMO_INATIVIDADE) {

            layoutPrincipal.setVisibility(View.VISIBLE);
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(this);

        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            layoutPrincipal.setVisibility(View.INVISIBLE);
            autenticarComDispositivo();
        } else {
            layoutPrincipal.setVisibility(View.VISIBLE);
        }
    }

    private void autenticarComDispositivo() {

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt =
                new BiometricPrompt(this, executor,
                        new BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    @NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                autenticadoNestaSessao = true;
                                layoutPrincipal.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAuthenticationError(
                                    int errorCode,
                                    @NonNull CharSequence errString) {

                                super.onAuthenticationError(errorCode, errString);

                                if (isFinishing() || isDestroyed()) return;

                                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                        errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                                    finishAffinity();
                                } else {
                                    Toast.makeText(HomeActivity.this,
                                            errString,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("OrgaFácil Protegido")
                        .setSubtitle("Confirme sua identidade para acessar suas finanças")
                        .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // ─────────────────────────────────────────────────────────────
    // Permissões de runtime
    // ─────────────────────────────────────────────────────────────

    private void solicitarPermissoes() {
        List<String> permissoesPendentes = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissoesPendentes.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissoesPendentes.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissoesPendentes.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissoesPendentes.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissoesPendentes.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissoesPendentes.toArray(new String[0]), 100);
        }
    }
}