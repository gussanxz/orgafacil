package com.gussanxz.orgafacil.activity.main;

import android.app.PendingIntent;
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
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.VisibilidadeHelper;
import com.gussanxz.orgafacil.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText campoEmail, campoSenha;
    private Button botaoEntrar;
    private TextView recuperarSenha;
    private Usuario usuario;
    private FirebaseAuth autenticacao;
    private RadioButton acessarTelaCadastro;
    private Button btnLoginGoogle;

    // Google Sign In
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private final ActivityResultLauncher<IntentSenderRequest> oneTapLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    Toast.makeText(LoginActivity.this, "Login cancelado.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                    String idToken = credential.getGoogleIdToken();

                    autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    Log.d(TAG, "FirebaseApp name: " + com.google.firebase.FirebaseApp.getInstance().getName());

                    autenticacao.signInWithCredential(firebaseCredential)
                            .addOnSuccessListener(authResult -> {

                                // Verifica se é um usuário novo
                                boolean isNewUser = authResult.getAdditionalUserInfo() != null
                                        && authResult.getAdditionalUserInfo().isNewUser();

                                if (isNewUser) {

                                    // CADASTRO AUTOMÁTICO VIA GOOGLE
                                    // Precisamos criar a estrutura do banco (Users + Contas) agora

                                    String uid = authResult.getUser().getUid();
                                    String email = authResult.getUser().getEmail();
                                    String nome = authResult.getUser().getDisplayName(); // O Google já nos dá o nome!

                                    Usuario novoUsuario = new Usuario();
                                    novoUsuario.setIdUsuario(uid);
                                    novoUsuario.setEmail(email);
                                    novoUsuario.setNome(nome != null ? nome : "Usuário Google");

                                    // 1. Salva o perfil
                                    novoUsuario.salvar();

                                    // 2. CRUCIAL: Inicializa o financeiro (Saldo 0 e Categorias)
                                    // Sem isso, o app travaria na Home
                                    novoUsuario.inicializarNovosDados();

                                    Toast.makeText(LoginActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                                }

                                // Seja novo ou antigo, redireciona para a Home
                                abrirTelaHome();

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Falha no login Google", e);
                                Toast.makeText(LoginActivity.this,
                                        "Erro ao entrar com Google: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                } catch (ApiException e) {
                    Log.e(TAG, "Erro One Tap", e);
                    Toast.makeText(LoginActivity.this, "Erro no login Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

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

        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);

        botaoEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String textoEmail = campoEmail.getText().toString();
                String textoSenha = campoSenha.getText().toString();

                if (!textoEmail.isEmpty()) {
                    if (!textoSenha.isEmpty()) {

                        usuario = new Usuario();
                        usuario.setEmail( textoEmail );
                        usuario.setSenha( textoSenha );
                        validarLogin();

                    } else {
                        Toast.makeText(LoginActivity.this, "Preencha a senha.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Preencha o email.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        acessarTelaCadastro.setOnClickListener(view -> abrirTelaCadastro());

        recuperarSenha.setOnClickListener(view -> recuperarSenha());

        EditText campoSenha = findViewById(R.id.editSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);

        configurarLoginGoogle();
    }
    private void configurarLoginGoogle() {

        // Apenas prepara o cliente, não loga ainda
        oneTapClient = Identity.getSignInClient(this);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .setAutoSelectEnabled(false)
                .build();

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> iniciarLoginGoogle());
        }
    }

    private void iniciarLoginGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        PendingIntent pendingIntent = result.getPendingIntent();
                        IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                        oneTapLauncher.launch(request);
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao lançar intent do Google", e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "beginSignIn falhou", e);
                    Toast.makeText(LoginActivity.this, "Não foi possível iniciar login Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public void validarLogin(){

        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if ( task.isSuccessful() ) {

                    abrirTelaHome();

                } else {

                    String excecao = "";
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        excecao = "Usuario não está cadastrado";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        excecao = "E-mail e/ou senha não correspondem a um usuário cadastrado";
                    } catch (Exception e) {
                        excecao = "Erro ao logar: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this, excecao, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro(){
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void recuperarSenha() {
        EditText campoEmail = findViewById(R.id.editEmail);
        String email = campoEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Digite o e-mail", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Link de redefinição enviado para " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erro ao enviar o link. Verifique o e-mail.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}