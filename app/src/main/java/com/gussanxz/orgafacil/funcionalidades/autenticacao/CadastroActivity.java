package com.gussanxz.orgafacil.funcionalidades.autenticacao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

// IMPORTS DA NOVA ESTRUTURA (REPOSITORY PATTERN)
import com.gussanxz.orgafacil.funcionalidades.configuracoes.dados.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.negocio.modelos.Usuario;

/**
 * CadastroActivity
 * Gerencia a criação de novas contas através de formulário ou Google Sign-In.
 */
public class CadastroActivity extends AppCompatActivity {

    // Componentes de Interface
    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;

    // Ferramentas de Autenticação e Dados
    private FirebaseAuth autenticacao;
    private Usuario usuario;
    private GoogleLoginHelper googleLoginHelper;

    // Repositório centralizado para operações no Firestore
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Suporte para telas de ponta a ponta
        setContentView(R.layout.ac_main_intro_cadastro);

        // Ajuste automático de padding para barras de sistema (Status/Navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializa instâncias necessárias
        usuarioRepository = new UsuarioRepository();
        inicializarComponentes();
        googleLoginHelper = new GoogleLoginHelper(this, this::abrirTelaHome);

        // --- CLIQUES ---

        // Ação de Cadastro Manual
        botaoCadastrar.setOnClickListener(view -> {
            String textoNome = campoNome.getText().toString();
            String textoEmail = campoEmail.getText().toString();
            String textoSenha = campoSenha.getText().toString();
            String textoSenhaConfirmacao = campoSenhaConfirmacao.getText().toString();

            if (validarCampos(textoNome, textoEmail, textoSenha, textoSenhaConfirmacao)) {
                usuario = new Usuario();
                usuario.setNome(textoNome);
                usuario.setEmail(textoEmail);
                usuario.setSenha(textoSenha); // Senha usada apenas localmente para o Auth

                cadastrarUsuarioFormulario();
            }
        });

        // Ação de Cadastro via Google
        botaoGoogle.setOnClickListener(v -> {
            resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
        });

        // Navegação para tela de Login
        acessarTelaLogin.setOnClickListener(view -> abrirTelaLogin());

        // Ativa o ícone de "olhinho" para alternar visibilidade da senha
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);
    }

    /**
     * Lançador de resultado para o fluxo do Google Sign-In.
     */
    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData(), GoogleLoginHelper.MODO_CADASTRO);
                }
            }
    );

    /**
     * Fluxo de Cadastro Manual:
     * 1. Cria a conta no Firebase Authentication.
     * 2. Em caso de sucesso, salva o perfil e inicializa categorias no Firestore via Repository.
     */
    public void cadastrarUsuarioFormulario() {
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();

        autenticacao.createUserWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Cadastro no Auth OK -> Pegamos o UID gerado
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setIdUsuario(idUsuario);

                        // PERSISTÊNCIA: Delegamos ao repositório para manter a Activity limpa
                        usuarioRepository.salvarPerfil(usuario); // Cria o doc config/perfil
                        usuarioRepository.inicializarCategoriasPadrao(idUsuario); // Cria as categorias iniciais

                        Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar usuário!", Toast.LENGTH_SHORT).show();
                        abrirTelaHome();

                    } else {
                        // Falha na criação da conta (ex: email já existe)
                        tratarErrosAuth(task);
                    }
                });
    }

    /**
     * Inicializa os IDs dos componentes do layout XML.
     */
    private void inicializarComponentes() {
        campoNome = findViewById(R.id.editNome);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        campoSenhaConfirmacao = findViewById(R.id.editSenhaConfirmacao);
        botaoCadastrar = findViewById(R.id.buttonCadastrar);
        botaoGoogle = findViewById(R.id.btnCadastrarGoogle);
        acessarTelaLogin = findViewById(R.id.radioButtonLogin);
    }

    /**
     * Validações básicas de preenchimento e confirmação de senha.
     */
    private boolean validarCampos(String nome, String email, String senha, String confSenha) {
        if (nome.isEmpty()) { Toast.makeText(this, "Preencha o nome.", Toast.LENGTH_SHORT).show(); return false; }
        if (email.isEmpty()) { Toast.makeText(this, "Preencha o email.", Toast.LENGTH_SHORT).show(); return false; }
        if (senha.isEmpty()) { Toast.makeText(this, "Preencha a senha.", Toast.LENGTH_SHORT).show(); return false; }
        if (confSenha.isEmpty()) { Toast.makeText(this, "Confirme a senha.", Toast.LENGTH_SHORT).show(); return false; }
        if (!senha.equals(confSenha)) { Toast.makeText(this, "Senhas não conferem!", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    /**
     * Traduz exceções técnicas do Firebase para mensagens amigáveis ao usuário.
     */
    private void tratarErrosAuth(Task<AuthResult> task) {
        String excecao;
        try {
            throw task.getException();
        } catch (FirebaseAuthWeakPasswordException e) {
            excecao = "Digite uma senha mais forte!";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            excecao = "Por favor, digite um e-mail válido";
        } catch (FirebaseAuthUserCollisionException e) {
            excecao = "Esta conta já foi cadastrada!";
        } catch (Exception e) {
            excecao = "Erro ao cadastrar: " + e.getMessage();
        }
        Toast.makeText(CadastroActivity.this, excecao, Toast.LENGTH_SHORT).show();
    }

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}