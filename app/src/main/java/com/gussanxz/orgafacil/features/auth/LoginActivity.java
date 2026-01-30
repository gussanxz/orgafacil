package com.gussanxz.orgafacil.features.auth;

import android.content.Intent;
import android.os.Bundle;
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
import com.gussanxz.orgafacil.data.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.data.config.FirestoreSchema;
import com.gussanxz.orgafacil.features.main.HomeActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;
import com.gussanxz.orgafacil.data.model.Usuario;

/**
 * LoginActivity
 * - A verificação de "usuário tem dados" não olha mais users/{uid}
 * - Agora olha: {ROOT}/{uid}/config/perfil
 *
 * Impacto:
 * - ROOT passa a funcionar de verdade (teste/usuarios/users)
 * - A "safety net" recria dados no schema novo, evitando crash na home
 */
public class LoginActivity extends AppCompatActivity {

    private EditText campoEmail, campoSenha;
    private Button botaoEntrar, btnLoginGoogle;
    private TextView recuperarSenha;
    private RadioButton acessarTelaCadastro;

    private Usuario usuario;
    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper;

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

        // Google helper chama nosso "safety net" ao finalizar login
        googleLoginHelper = new GoogleLoginHelper(this, this::verificarSeUsuarioTemDadosNoFirestore);

        // Login manual
        botaoEntrar.setOnClickListener(v -> {
            String textoEmail = campoEmail.getText().toString();
            String textoSenha = campoSenha.getText().toString();

            if (textoEmail.isEmpty()) {
                Toast.makeText(this, "Preencha o email.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (textoSenha.isEmpty()) {
                Toast.makeText(this, "Preencha a senha.", Toast.LENGTH_SHORT).show();
                return;
            }

            usuario = new Usuario();
            usuario.setEmail(textoEmail);
            usuario.setSenha(textoSenha);
            validarLoginManual();
        });

        // Login Google
        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v ->
                    resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent())
            );
        }

        acessarTelaCadastro.setOnClickListener(view -> abrirTelaCadastro());
        recuperarSenha.setOnClickListener(view -> recuperarSenha());

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData(), GoogleLoginHelper.MODO_LOGIN);
                } else {
                    Toast.makeText(this, "Login Google cancelado.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    public void validarLoginManual() {
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            verificarSeUsuarioTemDadosNoFirestore();
                        } else {
                            tratarErrosLogin(task);
                        }
                    }
                });
    }

    /**
     * SAFETY NET (schema novo)
     * Verifica se existe:
     * {ROOT}/{uid}/config/perfil
     */
    private void verificarSeUsuarioTemDadosNoFirestore() {
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        if (autenticacao.getCurrentUser() == null) return;

        String uid = autenticacao.getUid();

        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        abrirTelaHome();
                    } else {
                        recriarDadosUsuario(
                                autenticacao.getCurrentUser().getEmail(),
                                autenticacao.getCurrentUser().getDisplayName(),
                                uid
                        );
                    }
                })
                .addOnFailureListener(e -> abrirTelaHome());
    }

    private void recriarDadosUsuario(String email, String nome, String uid) {
        Usuario usuarioMigrado = new Usuario();
        usuarioMigrado.setIdUsuario(uid);
        usuarioMigrado.setEmail(email);
        usuarioMigrado.setNome(nome != null ? nome : "Usuário");

        usuarioMigrado.salvar();              // config/perfil
        usuarioMigrado.inicializarNovosDados(); // seed categorias em moduloSistema/contas

        Toast.makeText(this, "Dados sincronizados com sucesso.", Toast.LENGTH_SHORT).show();
        abrirTelaHome();
    }

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

    public void abrirTelaCadastro() {
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
        String excecao;
        try {
            throw task.getException();
        } catch (FirebaseAuthInvalidUserException e) {
            excecao = "Usuário não está cadastrado.";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            excecao = "E-mail e/ou senha incorretos.";
        } catch (Exception e) {
            excecao = "Erro ao logar: " + e.getMessage();
        }
        Toast.makeText(this, excecao, Toast.LENGTH_SHORT).show();
    }
}