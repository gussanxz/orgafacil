package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.regras.BaseAuthActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.SenhaForcaHelper; // [NOVO]
import com.gussanxz.orgafacil.util_helper.TemaHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

public class CadastroActivity extends BaseAuthActivity {

    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;

    // [REFATORADO] Removemos as Views soltas e adicionamos o Helper
    private SenhaForcaHelper senhaForcaHelper;
    private LoadingHelper loadingHelper;
    private GoogleLoginHelper googleLoginHelper;

    private final Handler debounceHandler = new Handler();
    private Runnable runnableSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_cadastro);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                        insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
                return insets;
            });
        }

        inicializarComponentes();
        configurarMonitoresCampos();
        configurarListeners();
    }

    @Override
    protected LoadingHelper getLoadingHelper() {
        return loadingHelper;
    }

    private void inicializarComponentes() {
        campoNome = findViewById(R.id.editNome);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        campoSenhaConfirmacao = findViewById(R.id.editSenhaConfirmacao);
        botaoCadastrar = findViewById(R.id.buttonCadastrar);
        botaoGoogle = findViewById(R.id.btnGoogle);
        acessarTelaLogin = findViewById(R.id.radioButtonLogin);

        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDados);

        // [NOVO] Inicializa o Helper de Senha passando as Views necessárias
        senhaForcaHelper = new SenhaForcaHelper(
                findViewById(R.id.viewForca1),
                findViewById(R.id.viewForca2),
                findViewById(R.id.viewForca3),
                findViewById(R.id.viewForca4),
                findViewById(R.id.textDicaSenha)
        );

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);

        botaoCadastrar.setEnabled(false);
        botaoCadastrar.setAlpha(0.5f);
    }

    private void configurarListeners() {
        botaoCadastrar.setOnClickListener(v -> validarEExecutarCadastro());
        botaoGoogle.setOnClickListener(v ->
                resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent())
        );
        acessarTelaLogin.setOnClickListener(v -> abrirTelaLogin());
    }

    private void validarEExecutarCadastro() {
        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString().trim();
        String conf = campoSenhaConfirmacao.getText().toString().trim();

        if (nome.isEmpty()) {
            campoNome.setError("Preencha o nome");
            return;
        }

        // Valida se a senha é segura usando o Helper (evita código duplicado)
        if (!senhaForcaHelper.ehSegura(senha)) {
            campoSenha.setError("A senha precisa ser pelo menos 'Boa'");
            return;
        }

        if (!senha.equals(conf)) {
            campoSenhaConfirmacao.setError("As senhas não coincidem!");
            return;
        }

        loadingHelper.exibir();
        criarContaAuth(nome, email, senha);
    }

    private void criarContaAuth(String nome, String email, String senha) {
        autenticacao.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        UserProfileChangeRequest updates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(nome)
                                .build();

                        if (autenticacao.getCurrentUser() != null) {
                            autenticacao.getCurrentUser().updateProfile(updates).addOnCompleteListener(t -> {
                                iniciarFluxoSegurancaDados();
                            });
                        }
                    } else {
                        loadingHelper.ocultar();
                        tratarErroCadastro(task.getException());
                    }
                });
    }

    private void tratarErroCadastro(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            Toast.makeText(this, "E-mail já cadastrado. Redirecionando...", Toast.LENGTH_LONG).show();
            abrirTelaLogin();
        } else {
            String erro = usuarioRepository.mapearErroAutenticacao(exception);
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarMonitoresCampos() {
        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                atualizarEstadoBotao();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        };

        campoNome.addTextChangedListener(commonWatcher);
        campoEmail.addTextChangedListener(commonWatcher);
        campoSenhaConfirmacao.addTextChangedListener(commonWatcher);

        campoSenha.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnableSenha != null) debounceHandler.removeCallbacks(runnableSenha);
                runnableSenha = () -> {
                    String senha = s.toString();

                    // [NOVO] O Helper cuida de atualizar as cores e texto
                    senhaForcaHelper.atualizarVisual(senha);

                    atualizarEstadoBotao();
                };
                debounceHandler.postDelayed(runnableSenha, 200);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void atualizarEstadoBotao() {
        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();

        // [NOVO] Usa o Helper para verificar a lógica (nível >= 3)
        boolean senhaSegura = senhaForcaHelper.ehSegura(senha);
        boolean emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean tudoOk = !nome.isEmpty() && emailValido && senhaSegura;

        botaoCadastrar.setEnabled(tudoOk);
        botaoCadastrar.setAlpha(tudoOk ? 1.0f : 0.5f);
    }

    private final ActivityResultLauncher<Intent> resultLauncherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    googleLoginHelper.lidarComResultadoGoogle(result.getData());
                } else {
                    loadingHelper.ocultar();
                }
            }
    );

    public void abrirTelaLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}