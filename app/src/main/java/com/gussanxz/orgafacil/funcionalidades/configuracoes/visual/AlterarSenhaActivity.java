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
import com.gussanxz.orgafacil.funcionalidades.autenticacao.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.dados.UsuarioRepository;
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
    private UsuarioRepository usuarioRepository;

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
        usuarioRepository = new UsuarioRepository();
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

                // Critérios: Senhas iguais e tamanho mínimo de 6 caracteres
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

        // Chamada delegada ao Repositório para operações de rede
        usuarioRepository.alterarSenhaComReautenticacao(user, senhaAtual, nova, task -> {

            // Oculta loading e reabilita o botão independente do resultado
            loadingHelper.ocultar();
            btnSalvarSenha.setEnabled(true);

            if (task.isSuccessful()) {
                // SUCESSO: Feedback visual e Logout forçado
                Toast.makeText(this, "Senha alterada! Faça login com a nova senha.", Toast.LENGTH_LONG).show();

                // Método centralizado no repositório
                usuarioRepository.deslogar();

                // Redirecionamento para Login limpando a pilha de navegação
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // FALHA: Provável erro na senha atual (reautenticação)
                Toast.makeText(this, "Falha: Verifique se sua senha atual está correta.", Toast.LENGTH_LONG).show();
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