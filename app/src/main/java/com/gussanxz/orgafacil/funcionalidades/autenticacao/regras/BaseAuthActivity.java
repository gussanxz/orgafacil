package com.gussanxz.orgafacil.funcionalidades.autenticacao.regras;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater; // Adicionado para inflar o layout
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioService;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.UsuarioModel;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * BaseAuthActivity
 * Classe pai para atividades que lidam com login/cadastro.
 * Centraliza a lógica de verificar se o usuário existe, está ativo ou precisa aceitar termos.
 */
public abstract class BaseAuthActivity extends AppCompatActivity {

    protected UsuarioRepository usuarioRepository;
    protected UsuarioService usuarioService;
    protected FirebaseAuth autenticacao;
    private LoadingHelper loadingHelper; // Instância local para evitar NullPointer

    // As atividades filhas devem fornecer o loading para controle visual
    protected abstract LoadingHelper getLoadingHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicialização segura dos serviços
        usuarioRepository = new UsuarioRepository();
        usuarioService = new UsuarioService();
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();

        // Segurança: Bloqueia prints de tela (dados sensíveis)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * Inicia o fluxo de verificação de conta.
     * Deve ser chamado pelas Activities filhas (Login/Cadastro) após o sucesso do Auth.
     */
    protected void iniciarFluxoSegurancaDados() {
        if (!FirebaseSession.isUserLogged()) return;

        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        // Tenta usar o helper da filha, senão cria um local temporário
        loadingHelper = getLoadingHelper();
        if (loadingHelper != null) loadingHelper.exibir();

        usuarioRepository.verificarSeUsuarioExiste(user.getUid()).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) return;

            if (!task.isSuccessful()) {
                abortarLogin("Erro de conexão ao verificar perfil.");
                return;
            }

            DocumentSnapshot doc = task.getResult();

            if (doc != null && doc.exists()) {
                // CENÁRIO A: USUÁRIO JÁ EXISTE NO BANCO
                try {
                    UsuarioModel usuarioModel = doc.toObject(UsuarioModel.class);
                    if (usuarioModel != null) {
                        verificarStatusEEntrar(usuarioModel);
                    } else {
                        // Fallback: Documento existe mas não converteu
                        usuarioRepository.atualizarUltimaAtividade();
                        navegarParaHome("Bem-vindo de volta!");
                    }
                } catch (Exception e) {
                    // Erro de parsing: Tenta entrar mesmo assim
                    navegarParaHome("Bem-vindo!");
                }
            } else {
                // CENÁRIO B: USUÁRIO NOVO (Primeiro Login)
                if (loadingHelper != null) loadingHelper.ocultar();
                exibirDialogoTermos(user);
            }
        });
    }

    private void verificarStatusEEntrar(UsuarioModel usuario) {
        // [ATUALIZADO] Acessa o status dentro do grupo DadosConta
        String statusAtual = (usuario.getDadosConta() != null)
                ? usuario.getDadosConta().getStatus()
                : "ATIVO"; // Default se nulo

        if ("DESATIVADO".equals(statusAtual)) {
            if (loadingHelper != null) loadingHelper.ocultar();
            exibirConfirmacaoReativacao();
        } else {
            // Usuário Ativo: Atualiza timestamp e entra
            usuarioRepository.atualizarUltimaAtividade();
            navegarParaHome("Bem-vindo de volta!");
        }
    }

    private void exibirConfirmacaoReativacao() {
        if (isFinishing()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reativar Conta?")
                .setMessage("Sua conta está desativada. Deseja reativá-la para recuperar seus dados?")
                .setCancelable(false)
                // Dentro de exibirConfirmacaoReativacao
                .setPositiveButton("Sim, Reativar", (dialog, which) -> {
                    if (loadingHelper != null) loadingHelper.exibir();

                    Task<Void> task = usuarioRepository.reativarContaLogica();

                    if (task != null) {
                        task.addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                usuarioRepository.atualizarUltimaAtividade();
                                navegarParaHome("Conta reativada!");
                            } else {
                                abortarLogin("Erro ao reativar.");
                            }
                        });
                    } else {
                        abortarLogin("Erro crítico: Usuário não identificado.");
                    }
                });
    }

    private void exibirDialogoTermos(FirebaseUser user) {
        if (user == null || isFinishing()) return;

        // Infla o layout do dialog
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.dialog_termos, null);

        CheckBox checkTermos = viewDialog.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = viewDialog.findViewById(R.id.btnAceitarTermos);
        // Desabilita botão inicialmente
        if (btnAceitar != null) btnAceitar.setEnabled(false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(viewDialog)
                .setCancelable(true)
                .create();

        dialog.setOnCancelListener(d -> realizarLogoutDeLimpeza());

        if (checkTermos != null && btnAceitar != null) {
            checkTermos.setOnCheckedChangeListener((buttonView, isChecked) ->
                    btnAceitar.setEnabled(isChecked)
            );

            btnAceitar.setOnClickListener(v -> {
                dialog.dismiss();
                if (loadingHelper != null) loadingHelper.exibir();
                // Chama o Service atualizado para criar a conta
                criarDadosDoNovoUsuario(user);
            });
        }

        dialog.show();
        configurarVisualDialogo(dialog);
    }

    private void criarDadosDoNovoUsuario(FirebaseUser user) {
        // [ATUALIZADO] O Service agora preenche os Mapas corretamente
        usuarioService.inicializarNovoUsuario(user, task -> {
            if (isFinishing() || isDestroyed()) return;

            if (loadingHelper != null) loadingHelper.ocultar();

            if (task != null && task.isSuccessful()) {
                navegarParaHome("Conta configurada com sucesso!");
            } else {
                abortarLogin("Erro ao configurar sua conta. Tente novamente.");
            }
        });
    }

    protected void navegarParaHome(String mensagem) {
        if (loadingHelper != null) loadingHelper.ocultar();
        if (mensagem != null) Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void realizarLogoutDeLimpeza() {
        autenticacao.signOut();
        Toast.makeText(this, "É necessário aceitar os termos.", Toast.LENGTH_SHORT).show();
        if (loadingHelper != null) loadingHelper.ocultar();
    }

    private void abortarLogin(String erro) {
        if (loadingHelper != null) loadingHelper.ocultar();
        autenticacao.signOut();
        Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
    }

    private void configurarVisualDialogo(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = (int) (displayMetrics.widthPixels * 0.90);
            int displayHeight = (int) (displayMetrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(displayWidth, displayHeight);
        }
    }
}