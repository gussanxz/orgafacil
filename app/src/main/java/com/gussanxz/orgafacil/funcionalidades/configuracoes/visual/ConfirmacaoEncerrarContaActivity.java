package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.UsuarioRepository;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * ConfirmacaoEncerrarContaActivity
 * Tela crítica para encerramento definitivo de conta.
 * Exige reautenticação por senha para validar a operação no Firebase.
 */
public class ConfirmacaoEncerrarContaActivity extends AppCompatActivity {

    private TextInputEditText editSenhaEncerramento;
    private Button btnConfirmarEncerramento, btnCancelar;
    private LoadingHelper loadingHelper;
    private UsuarioRepository usuarioRepository;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_confirmacao_encerrar_conta);

        // Inicialização de componentes e lógica
        usuarioRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Helper de carregamento (certifique-se de ter o include no XML)
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        bindViews();
        configurarListeners();
    }

    private void bindViews() {
        editSenhaEncerramento = findViewById(R.id.editSenhaEncerramento);
        btnConfirmarEncerramento = findViewById(R.id.btnConfirmarEncerramento);
        btnCancelar = findViewById(R.id.btnCancelarEncerramento);
    }

    private void configurarListeners() {
        // Inicia o processo de exclusão
        btnConfirmarEncerramento.setOnClickListener(v -> processarEncerramento());

        // Apenas fecha a tela e volta para Segurança
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * Valida os campos e solicita o encerramento ao repositório.
     */
    private void processarEncerramento() {
        String senha = editSenhaEncerramento.getText().toString().trim();

        if (senha.isEmpty()) {
            editSenhaEncerramento.setError("Digite sua senha para confirmar");
            editSenhaEncerramento.requestFocus();
            return;
        }

        loadingHelper.exibir();
        btnConfirmarEncerramento.setEnabled(false); // Evita cliques duplos

        // Chamada ao método centralizado no UsuarioRepository
        usuarioRepository.excluirContaDefinitivamente(user, senha, task -> {
            loadingHelper.ocultar();
            btnConfirmarEncerramento.setEnabled(true);

            if (task != null && task.isSuccessful()) {
                finalizarSessaoEApp();
            } else {
                // Tradução do erro (ex: senha incorreta ou erro de rede)
                String msgErro = usuarioRepository.mapearErroAutenticacao(task.getException());
                editSenhaEncerramento.setError(msgErro);
                Toast.makeText(this, msgErro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Limpa os dados de autenticação local e redireciona para o Login.
     */
    private void finalizarSessaoEApp() {
        Toast.makeText(this, "Conta encerrada com sucesso. Sentiremos sua falta!", Toast.LENGTH_LONG).show();

        // Logout definitivo do Firebase
        FirebaseAuth.getInstance().signOut();

        // Navegação de segurança: limpa o histórico de telas
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}