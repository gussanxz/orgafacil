package com.gussanxz.orgafacil.funcionalidades.autenticacao.regras;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository; // [ATUALIZADO]
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioService;      // [ATUALIZADO]
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel; // [ATUALIZADO]
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

    protected void iniciarFluxoSegurancaDados() {
        if (!FirebaseSession.isUserLogged()) return;

        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        if (getLoadingHelper() != null) getLoadingHelper().exibir();

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
                        // Fallback: Documento existe mas não converteu (raro), assume ativo
                        usuarioRepository.atualizarUltimaAtividade();
                        navegarParaHome("Bem-vindo de volta!");
                    }
                } catch (Exception e) {
                    // Erro de parsing: Tenta entrar mesmo assim para não bloquear o user
                    navegarParaHome("Bem-vindo!");
                }
            } else {
                // CENÁRIO B: USUÁRIO NOVO (Primeiro Login)
                if (getLoadingHelper() != null) getLoadingHelper().ocultar();
                exibirDialogoTermos(user);
            }
        });
    }

    private void verificarStatusEEntrar(UsuarioModel usuario) {
        // [CORREÇÃO]: Comparação de Status via String (Enum.name()) conforme novo Model
        if (UsuarioModel.StatusConta.DESATIVADO.name().equals(usuario.getStatus())) {
            if (getLoadingHelper() != null) getLoadingHelper().ocultar();
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
                .setPositiveButton("Sim, Reativar", (dialog, which) -> {
                    if (getLoadingHelper() != null) getLoadingHelper().exibir();
                    // [CORREÇÃO]: Chama o método do Repository para reativar
                    usuarioRepository.reativarContaLogica().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            usuarioRepository.atualizarUltimaAtividade();
                            navegarParaHome("Conta reativada com sucesso!");
                        } else {
                            abortarLogin("Falha ao reativar conta. Tente novamente.");
                        }
                    });
                })
                .setNegativeButton("Não, Sair", (dialog, which) -> {
                    abortarLogin("Reativação cancelada.");
                })
                .show();
    }

    // O método 'executarReativacaoNoBanco' foi removido e incorporado no listener acima
    // para evitar duplicação de lógica ou chamadas perdidas.

    private void exibirDialogoTermos(FirebaseUser user) {
        if (user == null || isFinishing()) return;

        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_termos, null);

        // Elementos do Dialog Customizado
        CheckBox checkTermos = viewDialog.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = viewDialog.findViewById(R.id.btnAceitarTermos);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(viewDialog)
                .setCancelable(true) // Permite usar o botão "Voltar" do Android
                .create();

        // Se o usuário apertar "Voltar" ou clicar fora, considera como recusa
        dialog.setOnCancelListener(d -> realizarLogoutDeLimpeza());

        if (checkTermos != null && btnAceitar != null) {
            checkTermos.setOnCheckedChangeListener((buttonView, isChecked) ->
                    btnAceitar.setEnabled(isChecked)
            );

            btnAceitar.setOnClickListener(v -> {
                dialog.dismiss(); // Dismiss não dispara onCancelListener
                if (getLoadingHelper() != null) getLoadingHelper().exibir();
                criarDadosDoNovoUsuario(user);
            });
        }

        dialog.show();
        configurarVisualDialogo(dialog);
    }

    private void criarDadosDoNovoUsuario(FirebaseUser user) {
        // [ATENÇÃO]: UsuarioService deve usar o novo UsuarioRepository internamente
        usuarioService.inicializarNovoUsuario(user, task -> {
            if (isFinishing() || isDestroyed()) return;

            if (getLoadingHelper() != null) getLoadingHelper().ocultar();

            if (task != null && task.isSuccessful()) {
                navegarParaHome("Conta configurada com sucesso!");
            } else {
                abortarLogin("Erro ao configurar sua conta. Tente novamente.");
            }
        });
    }

    protected void navegarParaHome(String mensagem) {
        if (getLoadingHelper() != null) getLoadingHelper().ocultar();
        if (mensagem != null) Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();

        // Certifique-se que HomeActivity existe. Se mudou para ContasActivity, altere aqui.
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void realizarLogoutDeLimpeza() {
        autenticacao.signOut();
        Toast.makeText(this, "É necessário aceitar os termos.", Toast.LENGTH_SHORT).show();
        if (getLoadingHelper() != null) getLoadingHelper().ocultar();
    }

    private void abortarLogin(String erro) {
        if (getLoadingHelper() != null) getLoadingHelper().ocultar();
        autenticacao.signOut();
        Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
    }

    private void configurarVisualDialogo(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = (int) (displayMetrics.widthPixels * 0.90);
            int displayHeight = (int) (displayMetrics.heightPixels * 0.85); // 85% da altura
            dialog.getWindow().setLayout(displayWidth, displayHeight);
        }
    }
}