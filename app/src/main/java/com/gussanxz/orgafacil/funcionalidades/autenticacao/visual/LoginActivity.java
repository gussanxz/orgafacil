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

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.usuario.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

/**
 * LoginActivity
 * Gerencia a entrada do usuário e delega a lógica de dados para o UsuarioRepository.
 */
public class LoginActivity extends AppCompatActivity {

    // Componentes de UI
    private EditText campoEmail, campoSenha;
    private Button botaoEntrar, btnLoginGoogle;
    private TextView recuperarSenha;
    private RadioButton acessarTelaCadastro;

    // Helpers e Ferramentas
    private FirebaseAuth autenticacao;
    private GoogleLoginHelper googleLoginHelper;
    private UsuarioRepository usuarioRepository;
    private LoadingHelper loadingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_login);

        // Configuração de Insets para preenchimento de tela
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();
        configurarListeners();
    }

    /**
     * Vincula componentes e instanciar ferramentas de suporte.
     */
    private void inicializarComponentes() {
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);

        // Inicia Repositório e Helpers
        usuarioRepository = new UsuarioRepository();
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // Google helper com callback para a Safety Net no Repository
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDados);

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    /**
     * Centraliza a configuração de cliques.
     */
    private void configurarListeners() {
        // Login Manual
        botaoEntrar.setOnClickListener(v -> validarEntradasLogin());

        // Login Google
        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v ->
                    resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent())
            );
        }

        acessarTelaCadastro.setOnClickListener(view -> abrirTelaCadastro());

        // Agora abre o BottomSheet moderno de recuperação
        recuperarSenha.setOnClickListener(view -> exibirDialogoRecuperacao());
    }

    /**
     * Valida os campos e inicia o processo de login no Firebase.
     */
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
                    if (task.isSuccessful()) {
                        iniciarFluxoSegurancaDados();
                    } else {
                        loadingHelper.ocultar();
                        // Delega a tradução do erro para o Repository
                        String erro = usuarioRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * SAFETY NET: Delega ao Repository a verificação e criação de dados iniciais.
     */
    private void iniciarFluxoSegurancaDados() {
        if (autenticacao.getCurrentUser() == null) return;

        // O Repository agora cuida de verificar o schema e recriar dados se necessário
        usuarioRepository.garantirDadosIniciais(autenticacao.getCurrentUser(), task -> {
            loadingHelper.ocultar();
            abrirTelaHome();
        });
    }

    /**
     * Exibe o diálogo moderno (BottomSheet) para recuperação de senha.
     */
    private void exibirDialogoRecuperacao() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recuperar_senha, null);
        dialog.setContentView(view);

        TextInputEditText editEmailRecuperar = view.findViewById(R.id.editEmailRecuperar);
        Button btnEnviar = view.findViewById(R.id.btnEnviarLink);

        btnEnviar.setOnClickListener(v -> {
            String email = editEmailRecuperar.getText().toString().trim();
            if (email.isEmpty()) {
                editEmailRecuperar.setError("Informe o e-mail");
                return;
            }

            usuarioRepository.enviarEmailRecuperacao(email, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Link enviado! Verifique seu e-mail.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Erro ao enviar link.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Gerenciador de resultado do Login Google
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

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro() {
        startActivity(new Intent(this, CadastroActivity.class));
    }
}