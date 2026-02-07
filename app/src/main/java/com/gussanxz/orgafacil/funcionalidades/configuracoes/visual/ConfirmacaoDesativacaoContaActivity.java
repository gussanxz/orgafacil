package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.UsuarioRepository;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * ConfirmacaoDesativacaoContaActivity
 * Tela responsável por confirmar a DESATIVAÇÃO TEMPORÁRIA (Soft Delete).
 * O usuário pode reativar a conta apenas fazendo login novamente no futuro.
 */
public class ConfirmacaoDesativacaoContaActivity extends AppCompatActivity {

    private TextInputLayout layoutSenha;
    private Button btnConfirmarDesativacao, btnCancelar;
    private LoadingHelper loadingHelper;

    private UsuarioRepository usuarioRepository;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Atenção: Lembre-se de renomear o arquivo de layout XML se desejar,
        // ou mantenha o antigo se o visual for o mesmo.
        setContentView(R.layout.ac_main_confirmacao_encerrar_conta);

        // Bloqueia prints por segurança
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        usuarioRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        // loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        bindViews();
        configurarInterface();
        configurarListeners();
    }

    private void bindViews() {
        // IDs do Layout (Recomendo atualizar os IDs no XML também para btnConfirmarDesativacao)
        layoutSenha = findViewById(R.id.layoutSenhaEncerramento);
        btnConfirmarDesativacao = findViewById(R.id.btnConfirmarEncerramento);
        btnCancelar = findViewById(R.id.btnCancelarEncerramento);
    }

    private void configurarInterface() {
        // Oculta campo de senha pois Desativação não exige re-auth crítica
        if (layoutSenha != null) {
            layoutSenha.setVisibility(View.GONE);
        }
        // Texto claro para o usuário não achar que vai perder tudo
        btnConfirmarDesativacao.setText("Confirmar Desativação");
    }

    private void configurarListeners() {
        btnConfirmarDesativacao.setOnClickListener(v -> executarDesativacao());
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * Executa a lógica de Soft Delete.
     */
    private void executarDesativacao() {
        if (loadingHelper != null) loadingHelper.exibir();
        btnConfirmarDesativacao.setEnabled(false);

        // Chama o repositório para alterar o status para "DESATIVADO"
        usuarioRepository.desativarContaLogica().addOnCompleteListener(task -> {

            if (loadingHelper != null) loadingHelper.ocultar();

            if (task.isSuccessful()) {
                finalizarSessao();
            } else {
                btnConfirmarDesativacao.setEnabled(true);
                String erro = "Erro ao desativar. Verifique sua conexão.";
                if (task.getException() != null) {
                    erro = task.getException().getMessage();
                }
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finalizarSessao() {
        usuarioRepository.deslogar();

        Toast.makeText(this, "Conta desativada. Faça login para reativar.", Toast.LENGTH_LONG).show();

        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}