package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioRepository;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

/**
 * AlterarSenhaActivity
 * Gerencia a troca de senha com validação em tempo real e reautenticação obrigatória.
 */
public class AlterarSenhaActivity extends AppCompatActivity {

    // Componentes de Interface
    private TextInputEditText editSenhaAtual, editNovaSenha, editConfirmarSenha;
    private Button btnSalvarSenha;

    // Utilitário de Carregamento (Substitui o ProgressBar direto)
    private LoadingHelper loadingHelper;

    private FirebaseUser user;
    private UsuarioRepository perfilRepository; // Atualizado para o novo repositório

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_configs_alterar_senha);

        // Configuração das margens das barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // Inicialização de repositório e autenticação
        perfilRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        inicializarComponentes();

        // Ativa a verificação dinâmica dos campos
        configurarValidacaoTempoReal();

        // Clique para processar a alteração
        btnSalvarSenha.setOnClickListener(v -> validarETrocarSenha());
    }

    /**
     * Validação em Tempo Real
     * Controla o estado do botão "Salvar" com base nos critérios de segurança.
     */
    private void configurarValidacaoTempoReal() {
        TextWatcher validacaoWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String nova = editNovaSenha.getText().toString().trim();
                String conf = editConfirmarSenha.getText().toString().trim();

                // Critérios: Senhas iguais e tamanho mínimo de 6 caracteres [cite: 2025-11-10]
                boolean senhasIguais = nova.equals(conf) && !nova.isEmpty();
                boolean tamanhoMinimo = nova.length() >= 6;

                if (!senhasIguais && !conf.isEmpty()) {
                    editConfirmarSenha.setError("As senhas não coincidem");
                }

                // Habilita o botão apenas se os requisitos forem atendidos
                btnSalvarSenha.setEnabled(senhasIguais && tamanhoMinimo);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editNovaSenha.addTextChangedListener(validacaoWatcher);
        editConfirmarSenha.addTextChangedListener(validacaoWatcher);

        // Botão inicia desativado para garantir a validação inicial
        btnSalvarSenha.setEnabled(false);
    }

    /**
     * Orquestra o fluxo de troca de senha.
     * Reautentica, altera a senha e encerra a sessão por segurança.
     */
    private void validarETrocarSenha() {
        if (user == null) return;

        String senhaAtual = editSenhaAtual.getText().toString().trim();
        String nova = editNovaSenha.getText().toString().trim();

        if (senhaAtual.isEmpty()) {
            editSenhaAtual.setError("Digite sua senha atual");
            return;
        }

        // Exibe loading via Helper e bloqueia o botão
        btnSalvarSenha.setEnabled(false);
        loadingHelper.exibir();

        // Nota: O método de reautenticação deve estar descomentado no repositório [cite: 2025-11-10]
        // Chamada direta via Firebase Auth mantendo a lógica de mapeamento de erro do repositório
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), senhaAtual);

        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                user.updatePassword(nova).addOnCompleteListener(task -> {
                    loadingHelper.ocultar();
                    btnSalvarSenha.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Senha alterada! Faça login novamente.", Toast.LENGTH_LONG).show();

                        // Logout centralizado no repositório
                        perfilRepository.deslogar();

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                loadingHelper.ocultar();
                btnSalvarSenha.setEnabled(true);
                String erro = perfilRepository.mapearErroAutenticacao(reauthTask.getException());
                Toast.makeText(this, "Erro de Reautenticação: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vincula componentes e inicializa o Helper de carregamento.
     */
    private void inicializarComponentes() {
        editSenhaAtual = findViewById(R.id.editSenhaAtual);
        editNovaSenha = findViewById(R.id.editNovaSenha);
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha);
        btnSalvarSenha = findViewById(R.id.btnSalvarSenha);

        // Inicializa o helper usando o ID do layout incluído no XML
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));
    }
}