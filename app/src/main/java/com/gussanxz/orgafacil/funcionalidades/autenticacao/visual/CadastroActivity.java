package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;

/**
 * CadastroActivity
 * Gerencia a criação de conta, validação de termos e segurança de dados.
 * Segue o padrão de delegar lógica de dados ao ConfigPerfilUsuarioRepository.
 *
 * REGRA:
 * Cadastro SEMPRE cria dados.
 */
public class CadastroActivity extends AppCompatActivity {

    // Componentes de UI
    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;
    private CheckBox checkTermos;

    // Helpers e Ferramentas
    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper;
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_cadastro);

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
        configurarTextoClicavelTermos();
        configurarListeners();
    }

    private void inicializarComponentes() {
        campoNome = findViewById(R.id.editNome);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        campoSenhaConfirmacao = findViewById(R.id.editSenhaConfirmacao);
        botaoCadastrar = findViewById(R.id.buttonCadastrar);
        botaoGoogle = findViewById(R.id.btnCadastrarGoogle);
        acessarTelaLogin = findViewById(R.id.radioButtonLogin);
        checkTermos = findViewById(R.id.checkTermos);

        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService();
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // 🔐 Google apenas autentica → cadastro sempre cria
        googleLoginHelper = new GoogleLoginHelper(
                this,
                modo -> processarCadastroGoogle()
        );

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);
    }

    private void configurarListeners() {
        botaoCadastrar.setOnClickListener(v -> validarEProcessarCadastro());

        botaoGoogle.setOnClickListener(v -> {
            loadingHelper.exibir();
            resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
        });

        acessarTelaLogin.setOnClickListener(v -> abrirTelaLogin());
    }

    /**
     * Valida os campos e termos antes de criar a conta no Firebase.
     */
    private void validarEProcessarCadastro() {
        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString().trim();
        String conf = campoSenhaConfirmacao.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || conf.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(conf)) {
            campoSenhaConfirmacao.setError("As senhas não coincidem!");
            return;
        }

        if (!checkTermos.isChecked()) {
            Toast.makeText(this, "Você precisa aceitar os termos para continuar.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingHelper.exibir();

        autenticacao.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();

                        enviarEmailVerificacao(user);

                        usuarioService.inicializarNovoUsuario(user, t -> {
                            loadingHelper.ocultar();
                            abrirTelaHome();
                        });

                    } else {
                        loadingHelper.ocultar();
                        String erro =
                                perfilRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * CADASTRO VIA GOOGLE
     * Sempre cria dados
     */
    private void processarCadastroGoogle() {
        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        usuarioService.inicializarNovoUsuario(user, t -> {
            loadingHelper.ocultar();
            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
            abrirTelaHome();
        });
    }

    private void enviarEmailVerificacao(FirebaseUser user) {
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(
                            this,
                            "Link de verificação enviado ao e-mail!",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    /**
     * Termos clicáveis
     */
    private void configurarTextoClicavelTermos() {
        String textoCompleto =
                "Ao me cadastrar, concordo com os Termos de Uso e Política de Privacidade";

        SpannableString spannableString = new SpannableString(textoCompleto);

        int inicioClique = textoCompleto.indexOf("Termos de Uso");
        int fimClique = textoCompleto.length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(CadastroActivity.this, TermosActivity.class));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(getResources().getColor(R.color.cor_texto));
                ds.setFakeBoldText(true);
            }
        };

        spannableString.setSpan(
                clickableSpan,
                inicioClique,
                fimClique,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        checkTermos.setText(spannableString);
        checkTermos.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            googleLoginHelper.lidarComResultadoGoogle(
                                    result.getData(),
                                    GoogleLoginHelper.MODO_CADASTRO
                            );
                        } else {
                            loadingHelper.ocultar();
                        }
                    }
            );

    public void abrirTelaHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void abrirTelaLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
