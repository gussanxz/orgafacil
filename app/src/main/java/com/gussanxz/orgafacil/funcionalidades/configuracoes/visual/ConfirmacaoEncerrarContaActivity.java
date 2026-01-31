package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * ConfirmacaoEncerrarContaActivity
 * Tela para encerramento de conta.
 * Atualmente simplificada para apenas confirmação direta.
 */
public class ConfirmacaoEncerrarContaActivity extends AppCompatActivity {

    private TextInputLayout layoutSenha;
    private TextInputEditText editSenhaEncerramento;
    private Button btnConfirmarEncerramento, btnCancelar;
    private LoadingHelper loadingHelper;
    private ConfigPerfilUsuarioRepository perfilRepository; // Atualizado para o novo repositório
    private FirebaseUser user;
    private GoogleLoginHelper googleLoginHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_confirmacao_encerrar_conta);

        // Inicialização com foco no repositório de perfil
        perfilRepository = new ConfigPerfilUsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Inicializa o helper usando o ID do layout incluído no XML
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        bindViews();
        configurarInterfaceSimplificada();
        configurarListeners();
    }

    private void bindViews() {
        layoutSenha = findViewById(R.id.layoutSenhaEncerramento);
        editSenhaEncerramento = findViewById(R.id.editSenhaEncerramento);
        btnConfirmarEncerramento = findViewById(R.id.btnConfirmarEncerramento);
        btnCancelar = findViewById(R.id.btnCancelarEncerramento);
    }

    /**
     * Configura a interface para o modo de testes/provisório.
     * Esconde a obrigatoriedade de senha para agilizar o desenvolvimento.
     */
    private void configurarInterfaceSimplificada() {
        if (layoutSenha != null) {
            layoutSenha.setVisibility(View.GONE);
        }
        btnConfirmarEncerramento.setText("Sim, desejo excluir minha conta");
    }

    private void configurarListeners() {
        btnConfirmarEncerramento.setOnClickListener(v -> processarExclusaoDireta());
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * FLUXO ATUAL: Exclusão direta sem validação de senha/código.
     * Utiliza o repositório para apagar os dados do Firestore antes de deletar o Auth.
     */
    private void processarExclusaoDireta() {
        loadingHelper.exibir();

        perfilRepository.excluirContaSemSenha(user, task -> {
            loadingHelper.ocultar();

            if (task != null && task.isSuccessful()) {
                // SUCESSO TOTAL: Firestore e Auth limpos.
                finalizarSessaoEApp();
            } else {
                // FALHA: Provavelmente precisa de login recente
                String erro = perfilRepository.mapearErroAutenticacao(task != null ? task.getException() : null);
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finalizarSessaoEApp() {
        if (googleLoginHelper != null) {
            googleLoginHelper.recarregarSessaoGoogle(); // Mata o cache do Google [cite: 2026-01-31]
        }
        FirebaseAuth.getInstance().signOut(); // Mata a sessão Firebase [cite: 2025-11-10]

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    /* ========================================================================================
    MÉTODOS COMENTADOS PARA IMPLEMENTAÇÃO FUTURA (FLUXO HÍBRIDO SENHA/GOOGLE)
    ========================================================================================

    private void processarFluxoGoogle() {
        loadingHelper.exibir();
        btnConfirmarEncerramento.setEnabled(false);

        perfilRepository.enviarCodigoVerificacaoExclusao(user.getEmail(), task -> {
            loadingHelper.ocultar();
            if (task.isSuccessful()) {
                Toast.makeText(this, "E-mail enviado! Verifique sua caixa.", Toast.LENGTH_LONG).show();
                btnConfirmarEncerramento.setText("E-mail enviado");
                btnConfirmarEncerramento.postDelayed(this::finish, 3000);
            } else {
                btnConfirmarEncerramento.setEnabled(true);
                String msg = perfilRepository.mapearErroAutenticacao(task.getException());
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processarEncerramentoPadrao() {
        String senha = editSenhaEncerramento.getText().toString().trim();
        if (senha.isEmpty()) {
            editSenhaEncerramento.setError("Digite sua senha");
            return;
        }
        loadingHelper.exibir();
        perfilRepository.excluirContaDefinitivamente(user, senha, task -> {
            loadingHelper.ocultar();
            if (task != null && task.isSuccessful()) {
                finalizarSessaoEApp();
            } else {
                String msgErro = perfilRepository.mapearErroAutenticacao(task.getException());
                Toast.makeText(this, msgErro, Toast.LENGTH_SHORT).show();
            }
        });
    }
    */
}