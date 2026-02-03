package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * ConfirmacaoEncerrarContaActivity
 * Agora utiliza a estratégia de Soft Delete (Desativação Lógica).
 * Mantém os dados preservados no banco, mas bloqueia o acesso do usuário.
 */
public class ConfirmacaoEncerrarContaActivity extends AppCompatActivity {

    private TextInputLayout layoutSenha;
    private TextInputEditText editSenhaEncerramento;
    private Button btnConfirmarEncerramento, btnCancelar;
    private LoadingHelper loadingHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_confirmacao_encerrar_conta);

        // Bloqueia prints por segurança em telas sensíveis
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        perfilRepository = new ConfigPerfilUsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        bindViews();
        configurarInterfaceParaSoftDelete();
        configurarListeners();
    }

    private void bindViews() {
        layoutSenha = findViewById(R.id.layoutSenhaEncerramento);
        editSenhaEncerramento = findViewById(R.id.editSenhaEncerramento);
        btnConfirmarEncerramento = findViewById(R.id.btnConfirmarEncerramento);
        btnCancelar = findViewById(R.id.btnCancelarEncerramento);
    }

    /**
     * Remove a necessidade de senha, já que o Soft Delete
     * não exige re-autenticação do Firebase Auth.
     */
    private void configurarInterfaceParaSoftDelete() {
        if (layoutSenha != null) {
            layoutSenha.setVisibility(View.GONE);
        }
        btnConfirmarEncerramento.setText("Sim, desejo desativar minha conta");
    }

    private void configurarListeners() {
        btnConfirmarEncerramento.setOnClickListener(v -> processarDesativacaoLogica());
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * FLUXO ATUALIZADO: Apenas marca o status como DESATIVADO no Firestore.
     * Isso preserva o histórico financeiro e evita erros de "login recente".
     */
    private void processarDesativacaoLogica() {
        if (user == null) return;

        loadingHelper.exibir();

        // 1. Marca no banco de dados como desativado
        perfilRepository.desativarContaLogica(user.getUid()).addOnCompleteListener(task -> {
            loadingHelper.ocultar();

            if (task.isSuccessful()) {
                // 2. Com o banco atualizado, encerramos a sessão local
                finalizarSessaoEDeslogar();
            } else {
                String erro = "Não foi possível desativar a conta agora. Tente novamente.";
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finalizarSessaoEDeslogar() {
        // Encerra a sessão no Firebase Auth
        perfilRepository.deslogar();

        Toast.makeText(this, "Sua conta foi desativada com sucesso.", Toast.LENGTH_LONG).show();

        // Redireciona para o login e limpa a pilha de telas
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}