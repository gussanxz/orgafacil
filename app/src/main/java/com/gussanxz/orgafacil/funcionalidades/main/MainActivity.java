package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient; // Adicionado import faltante
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.CadastroActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioService;
// Import do novo Model
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.UsuarioModel;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;

public class MainActivity extends IntroActivity {

    private GoogleLoginHelper googleLoginHelper;
    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    // Launcher do Google Login precisa ser declarado antes do onCreate
    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // O Helper vai processar o resultado e chamar o callback 'processarLoginGoogle'
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        // Segurança: Bloqueia prints de tela na intro (onde pode aparecer e-mail/contas)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);

        usuarioRepository = new UsuarioRepository();
        usuarioService = new UsuarioService();

        // Configuração de Loading (Se houver overlay no XML da intro)
        View overlay = findViewById(R.id.loading_overlay);
        if (overlay != null) loadingHelper = new LoadingHelper(overlay);

        // Helper de Login Google com Callback de Sucesso (Referência de Método ::)
        googleLoginHelper = new GoogleLoginHelper(this, this::processarLoginGoogle);

        configurarSlides();
    }

    private void configurarSlides() {
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
     * Chamado quando o Google retorna SUCESSO.
     * Verifica banco de dados e exibe Termos se necessário.
     */
    private void processarLoginGoogle() {
        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
        if (user == null) return;

        if (loadingHelper != null) loadingHelper.exibir();

        // 1. Verifica se o usuário já existe
        usuarioRepository.verificarSeUsuarioExiste(user.getUid()).addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                abortarLogin("Erro de conexão com o banco.");
                return;
            }

            DocumentSnapshot doc = task.getResult();

            if (doc != null && doc.exists()) {
                // ---> USUÁRIO JÁ CADASTRADO <---
                try {
                    UsuarioModel usuarioModel = doc.toObject(UsuarioModel.class);

                    // [ATUALIZADO] Verifica status no grupo DadosConta
                    String statusAtual = "ATIVO";
                    if (usuarioModel != null && usuarioModel.getDadosConta() != null) {
                        statusAtual = usuarioModel.getDadosConta().getStatus();
                    }

                    if ("DESATIVADO".equals(statusAtual)) {
                        if (loadingHelper != null) loadingHelper.ocultar();
                        // Conta desativada: manda para LoginActivity lidar com a reativação
                        // (Pois a intro não tem UI para confirmar reativação)
                        Toast.makeText(this, "Conta desativada. Faça login para reativar.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, LoginActivity.class));
                    } else {
                        // Conta Ativa: Atualiza timestamp e entra
                        usuarioRepository.atualizarUltimaAtividade();
                        irParaHome();
                    }
                } catch (Exception e) {
                    // Fallback se houver erro de parsing
                    irParaHome();
                }
            } else {
                // ---> USUÁRIO NOVO (Primeiro Acesso) <---
                if (loadingHelper != null) loadingHelper.ocultar();
                exibirDialogoTermos(user);
            }
        });
    }

    private void exibirDialogoTermos(FirebaseUser user) {
        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_termos, null);

        // Elementos do Dialog Customizado
        CheckBox checkTermos = viewDialog.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = viewDialog.findViewById(R.id.btnAceitarTermos);
        // Botão Cancelar removido propositalmente (Flow único)

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(viewDialog)
                .setCancelable(true) // Permite cancelar clicando fora ou voltar
                .create();

        // AÇÃO DE RECUSA: Se cancelar/fechar o dialog, desloga do Google
        dialog.setOnCancelListener(d -> {
            ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
            GoogleSignInClient googleClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());
            googleClient.signOut();
            Toast.makeText(this, "Cadastro cancelado.", Toast.LENGTH_SHORT).show();
        });

        if (checkTermos != null && btnAceitar != null) {
            // Só habilita o botão se aceitar os termos
            checkTermos.setOnCheckedChangeListener((bv, isChecked) -> btnAceitar.setEnabled(isChecked));

            btnAceitar.setOnClickListener(v -> {
                // Ao clicar em aceitar, não dispara o OnCancelListener
                dialog.dismiss();
                if (loadingHelper != null) loadingHelper.exibir();
                criarConta(user);
            });
        }

        dialog.show();

        // Ajuste Visual (Fundo Transparente e Tamanho)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = (int) (displayMetrics.widthPixels * 0.90); // 90% da largura
            dialog.getWindow().setLayout(displayWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void criarConta(FirebaseUser user) {
        // [ATUALIZADO]: O UsuarioService já usa a nova estrutura internamente
        usuarioService.inicializarNovoUsuario(user, task -> {
            if (loadingHelper != null) loadingHelper.ocultar();

            if (task != null && task.isSuccessful()) {
                irParaHome();
            } else {
                abortarLogin("Erro ao criar conta. Tente novamente.");
            }
        });
    }

    private void irParaHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void abortarLogin(String erro) {
        if (loadingHelper != null) loadingHelper.ocultar();
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
    }

    // --- Métodos de Clique chamados pelo XML (OnClicks) ---

    public void btEntrar(View view) {
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btCadastrar(View view) {
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void btGoogle(View view) {
        // Inicia o fluxo de login Google
        resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
    }
}