package com.gussanxz.orgafacil.activity.main;

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
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.helper.VisibilidadeHelper;
import com.gussanxz.orgafacil.model.Usuario;

public class CadastroActivity extends AppCompatActivity {

    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;

    private FirebaseAuth autenticacao;
    private Usuario usuario;
    private GoogleLoginHelper googleLoginHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_cadastro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        // 1. Inicializa o Helper do Google
        googleLoginHelper = new GoogleLoginHelper(this, this::abrirTelaHome);

        // --- CADASTRO VIA FORMULÁRIO ---
        botaoCadastrar.setOnClickListener(view -> {
            String textoNome = campoNome.getText().toString();
            String textoEmail = campoEmail.getText().toString();
            String textoSenha = campoSenha.getText().toString();
            String textoSenhaConfirmacao = campoSenhaConfirmacao.getText().toString();

            if (validarCampos(textoNome, textoEmail, textoSenha, textoSenhaConfirmacao)) {
                usuario = new Usuario();
                usuario.setNome(textoNome);
                usuario.setEmail(textoEmail);
                usuario.setSenha(textoSenha);

                // MUDANÇA AQUI: Chamamos o cadastro direto.
                // O Firebase Auth vai verificar automaticamente se o email já existe.
                cadastrarUsuarioFormulario();
            }
        });

        // --- CADASTRO VIA GOOGLE ---
        botaoGoogle = findViewById(R.id.btnCadastrarGoogle);
        botaoGoogle.setOnClickListener(v -> {
            resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
        });

        acessarTelaLogin.setOnClickListener(view -> abrirTelaLogin());

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);
    }

    // --- LÓGICA DO HELPER ---
    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                }
            }
    );

    // --- CADASTRO MANUAL ---
    public void cadastrarUsuarioFormulario() {
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();

        // Tenta criar usuário
        autenticacao.createUserWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sucesso: Salva no Firestore
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setIdUsuario(idUsuario);
                        usuario.salvar();
                        usuario.inicializarNovosDados();

                        Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar usuário!", Toast.LENGTH_SHORT).show();
                        abrirTelaHome();

                    } else {
                        // Erro: (Ex: Email duplicado) cai aqui
                        tratarErrosAuth(task);
                    }
                });
    }

    // --- UTILITÁRIOS ---
    private void inicializarComponentes() {
        campoNome = findViewById(R.id.editNome);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        campoSenhaConfirmacao = findViewById(R.id.editSenhaConfirmacao);
        botaoCadastrar = findViewById(R.id.buttonCadastrar);
        acessarTelaLogin = findViewById(R.id.radioButtonLogin);
    }

    private boolean validarCampos(String nome, String email, String senha, String confSenha) {
        if (nome.isEmpty()) { Toast.makeText(this, "Preencha o nome.", Toast.LENGTH_SHORT).show(); return false; }
        if (email.isEmpty()) { Toast.makeText(this, "Preencha o email.", Toast.LENGTH_SHORT).show(); return false; }
        if (senha.isEmpty()) { Toast.makeText(this, "Preencha a senha.", Toast.LENGTH_SHORT).show(); return false; }
        if (confSenha.isEmpty()) { Toast.makeText(this, "Confirme a senha.", Toast.LENGTH_SHORT).show(); return false; }
        if (!senha.equals(confSenha)) { Toast.makeText(this, "Senhas não conferem!", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private void tratarErrosAuth(Task<AuthResult> task) {
        String excecao = "";
        try {
            throw task.getException();
        } catch (FirebaseAuthWeakPasswordException e) {
            excecao = "Digite uma senha mais forte!";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            excecao = "Por favor, digite um e-mail válido";
        } catch (FirebaseAuthUserCollisionException e) {
            // AQUI ESTÁ A MÁGICA: O Auth avisa se o email já existe
            excecao = "Esta conta já foi cadastrada!";
        } catch (Exception e) {
            excecao = "Erro ao cadastrar: " + e.getMessage();
            e.printStackTrace();
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