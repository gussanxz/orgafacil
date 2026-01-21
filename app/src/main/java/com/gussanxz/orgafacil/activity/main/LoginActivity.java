package com.gussanxz.orgafacil.activity.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.GoogleLoginHelper; // Helper Importado
import com.gussanxz.orgafacil.helper.VisibilidadeHelper;
import com.gussanxz.orgafacil.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText campoEmail, campoSenha;
    private Button botaoEntrar, btnLoginGoogle;
    private TextView recuperarSenha;
    private RadioButton acessarTelaCadastro;

    private Usuario usuario;
    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper; // Instância do Helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        // 1. Inicializa o Helper
        // A ação de sucesso será verificar os dados no Firestore (para garantir integridade)
        googleLoginHelper = new GoogleLoginHelper(this, this::verificarSeUsuarioTemDadosNoFirestore);

        // --- BOTÃO LOGIN MANUAL ---
        botaoEntrar.setOnClickListener(v -> {
            String textoEmail = campoEmail.getText().toString();
            String textoSenha = campoSenha.getText().toString();

            if (!textoEmail.isEmpty()) {
                if (!textoSenha.isEmpty()) {
                    usuario = new Usuario();
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    validarLoginManual();
                } else {
                    Toast.makeText(LoginActivity.this, "Preencha a senha.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "Preencha o email.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- BOTÃO LOGIN GOOGLE ---
        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> {
                // Chama a intent do Google via Helper
                resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
            });
        }

        acessarTelaCadastro.setOnClickListener(view -> abrirTelaCadastro());
        recuperarSenha.setOnClickListener(view -> recuperarSenha());

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    // --- LÓGICA DO HELPER (Receiver) ---
    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Delega o resultado para o Helper processar (Verificar novo usuário, pedir senha, etc)
                    googleLoginHelper.lidarComResultadoGoogle(result.getData(), GoogleLoginHelper.MODO_LOGIN);
                } else {
                    Toast.makeText(this, "Login Google cancelado.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // --- LOGIN MANUAL (EMAIL/SENHA) ---
    public void validarLoginManual(){
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if ( task.isSuccessful() ) {
                    // Login manual ok, verifica dados no banco
                    verificarSeUsuarioTemDadosNoFirestore();
                } else {
                    tratarErrosLogin(task);
                }
            }
        });
    }

    // --- VERIFICAÇÃO DE DADOS (SAFETY NET) ---
    // Este método é chamado tanto pelo Login Manual quanto pelo Callback do Google Helper
    private void verificarSeUsuarioTemDadosNoFirestore() {
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        if (autenticacao.getCurrentUser() == null) return;

        String uid = autenticacao.getUid();

        // Verifica se existe o documento users/{uid}
        ConfiguracaoFirestore.getFirestore()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // [CENÁRIO 1] Usuário já tem dados. Tudo certo.
                        abrirTelaHome();
                    } else {
                        // [CENÁRIO 2] Usuário logou (provavelmente antigo ou erro de criação), mas sem dados.
                        // Recria os dados básicos para evitar crash na Home.
                        recriarDadosUsuario(autenticacao.getCurrentUser().getEmail(), autenticacao.getCurrentUser().getDisplayName(), uid);
                    }
                })
                .addOnFailureListener(e -> {
                    // Em caso de erro de rede, tenta abrir a home de qualquer jeito
                    // A HomeActivity deve tratar falta de conexão
                    abrirTelaHome();
                });
    }

    private void recriarDadosUsuario(String email, String nome, String uid) {
        Usuario usuarioMigrado = new Usuario();
        usuarioMigrado.setIdUsuario(uid);
        usuarioMigrado.setEmail(email);
        usuarioMigrado.setNome(nome != null ? nome : "Usuário");

        usuarioMigrado.salvar();
        usuarioMigrado.inicializarNovosDados(); // Cria saldo 0 e categorias

        Toast.makeText(LoginActivity.this, "Dados sincronizados com sucesso.", Toast.LENGTH_SHORT).show();
        abrirTelaHome();
    }

    // --- MÉTODOS UTILITÁRIOS ---

    private void inicializarComponentes() {
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
    }

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro(){
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void recuperarSenha() {
        String email = campoEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Digite seu e-mail no campo acima para recuperar a senha.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Link de redefinição enviado para " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erro ao enviar email. Verifique se o endereço está correto.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void tratarErrosLogin(Task<AuthResult> task) {
        String excecao = "";
        try {
            throw task.getException();
        } catch (FirebaseAuthInvalidUserException e) {
            excecao = "Usuário não está cadastrado.";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            excecao = "E-mail e/ou senha incorretos.";
        } catch (Exception e) {
            excecao = "Erro ao logar: " + e.getMessage();
            e.printStackTrace();
        }
        Toast.makeText(LoginActivity.this, excecao, Toast.LENGTH_SHORT).show();
    }
}