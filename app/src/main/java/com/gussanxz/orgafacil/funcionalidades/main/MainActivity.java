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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.CadastroActivity;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        // Bloqueia prints por segurança
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);

        usuarioRepository = new UsuarioRepository();
        usuarioService = new UsuarioService();

        // Tenta encontrar o loading no layout da Intro (se você adicionou lá)
        View overlay = findViewById(R.id.loading_overlay);
        if (overlay != null) loadingHelper = new LoadingHelper(overlay);

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
     * Verifica banco de dados e exibe Termos se necessário, tudo nesta tela.
     */
    private void processarLoginGoogle() {
        FirebaseUser user = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();
        if (user == null) return;

        if (loadingHelper != null) loadingHelper.exibir();

        // 1. Verifica se o usuário já existe
        usuarioRepository.verificarSeUsuarioExiste(user.getUid()).addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                abortarLogin("Erro de conexão.");
                return;
            }

            DocumentSnapshot doc = task.getResult();

            if (doc != null && doc.exists()) {
                // ---> USUÁRIO EXISTE <---
                try {
                    UsuarioModel usuarioModel = doc.toObject(UsuarioModel.class);
                    // Se desativado, manda pro Login lidar com a reativação
                    if (usuarioModel != null && usuarioModel.getStatus() == UsuarioModel.StatusConta.DESATIVADO) {
                        if (loadingHelper != null) loadingHelper.ocultar();
                        startActivity(new Intent(this, LoginActivity.class));
                        // Não damos finish() para caso ele cancele a reativação, voltar pra cá
                    } else {
                        // Ativo: Entra direto
                        usuarioRepository.atualizarUltimaAtividade();
                        irParaHome();
                    }
                } catch (Exception e) {
                    irParaHome(); // Fallback
                }
            } else {
                // ---> USUÁRIO NOVO <---
                if (loadingHelper != null) loadingHelper.ocultar();
                exibirDialogoTermos(user);
            }
        });
    }

    private void exibirDialogoTermos(FirebaseUser user) {
        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_termos, null);
        CheckBox checkTermos = viewDialog.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = viewDialog.findViewById(R.id.btnAceitarTermos);
        // Botão Cancelar removido do código

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(viewDialog)
                .setCancelable(true) // Permite usar o botão "Voltar" do celular
                .create();

        // AÇÃO DE RECUSA: Se o usuário fechar o dialog (botão voltar), deslogamos.
        dialog.setOnCancelListener(d -> {
            ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
            GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();
            Toast.makeText(this, "Cadastro cancelado.", Toast.LENGTH_SHORT).show();
        });

        if (checkTermos != null && btnAceitar != null) {
            checkTermos.setOnCheckedChangeListener((bv, isChecked) -> btnAceitar.setEnabled(isChecked));

            btnAceitar.setOnClickListener(v -> {
                dialog.dismiss(); // Isso NÃO dispara o OnCancelListener, então não desloga
                if (loadingHelper != null) loadingHelper.exibir();
                criarConta(user);
            });
        }

        dialog.show();

        // Configura Visual Transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = (int) (displayMetrics.widthPixels * 0.90);
            int displayHeight = (int) (displayMetrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(displayWidth, displayHeight);
        }
    }

    private void criarConta(FirebaseUser user) {
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

    // --- Métodos chamados pelo XML (ac_main_intro_comece_agora) ---

    public void btEntrar(View view) {
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void btCadastrar(View view) {
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void btGoogle(View view) {
        resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                }
            }
    );
}