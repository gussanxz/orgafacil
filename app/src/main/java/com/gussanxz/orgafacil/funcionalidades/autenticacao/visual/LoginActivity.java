package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

/**
 * LoginActivity
 * Gerencia a entrada do usuário.
 *
 * REGRA:
 * - Login nunca cria dados
 * - Google aqui é login puro
 */
public class LoginActivity extends AppCompatActivity {

    // Componentes de UI
    private EditText campoEmail, campoSenha;
    private Button botaoEntrar, btnLoginGoogle;
    private TextView recuperarSenha;
    private RadioButton acessarTelaCadastro;

    // Helpers
    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        inicializarComponentes();
        configurarListeners();
    }

    private void inicializarComponentes() {
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);

        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        perfilRepository = new ConfigPerfilUsuarioRepository();
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // 🔐 Google aqui = LOGIN PURO
        googleLoginHelper = new GoogleLoginHelper(
                this,
                modo -> processarLoginGoogle()
        );

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    private void configurarListeners() {
        botaoEntrar.setOnClickListener(v -> validarEntradasLogin());

        btnLoginGoogle.setOnClickListener(v ->
                resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent())
        );

        acessarTelaCadastro.setOnClickListener(v -> abrirTelaCadastro());
        recuperarSenha.setOnClickListener(v -> exibirDialogoRecuperacao());
    }

    private void validarEntradasLogin() {
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingHelper.exibir();

        autenticacao.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    loadingHelper.ocultar();

                    if (task.isSuccessful()) {
                        iniciarLoginEmail();
                    } else {
                        String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * LOGIN EMAIL
     * Nunca cria dados
     */
    private void iniciarLoginEmail() {
        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        perfilRepository.verificarExistenciaPerfil(user.getUid())
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().exists()) {

                        Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                        abrirTelaHome();

                    } else {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(
                                this,
                                "Conta não encontrada. Crie uma nova conta.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * LOGIN GOOGLE
     * Nunca cria dados
     */
    private void processarLoginGoogle() {
        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        loadingHelper.exibir();

        perfilRepository.verificarExistenciaPerfil(user.getUid())
                .addOnCompleteListener(task -> {
                    loadingHelper.ocultar();

                    if (task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().exists()) {

                        Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                        abrirTelaHome();

                    } else {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(
                                this,
                                "Conta não encontrada. Crie uma nova conta.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void exibirDialogoRecuperacao() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recuperar_senha, null);
        dialog.setContentView(view);

        TextInputEditText editEmailRecuperar =
                view.findViewById(R.id.editEmailRecuperar);
        Button btnEnviar = view.findViewById(R.id.btnEnviarLink);

        btnEnviar.setOnClickListener(v -> {
            String email = editEmailRecuperar.getText().toString().trim();
            if (email.isEmpty()) {
                editEmailRecuperar.setError("Informe o e-mail");
                return;
            }

            autenticacao.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    this,
                                    "Link enviado! Verifique seu e-mail.",
                                    Toast.LENGTH_LONG
                            ).show();
                            dialog.dismiss();
                        } else {
                            String erro =
                                    perfilRepository.mapearErroAutenticacao(
                                            task.getException()
                                    );
                            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            googleLoginHelper.lidarComResultadoGoogle(
                                    result.getData(),
                                    GoogleLoginHelper.MODO_LOGIN
                            );
                        }
                    }
            );

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro() {
        startActivity(new Intent(this, CadastroActivity.class));
    }
}
