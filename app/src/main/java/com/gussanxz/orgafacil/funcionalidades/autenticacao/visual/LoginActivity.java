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
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService; // Import adicionado
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

/**
 * LoginActivity
 * Gerencia a entrada do usuário e delega a lógica de dados para o ConfigPerfilUsuarioRepository.
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
    private ConfigPerfilUsuarioRepository perfilRepository;
    private UsuarioService usuarioService; // Declarado para resolver o erro [cite: 2025-11-10]
    private LoadingHelper loadingHelper;

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

        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioService = new UsuarioService(); // Instanciado para resolver o erro [cite: 2025-11-10]
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDados);

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    private void configurarListeners() {
        botaoEntrar.setOnClickListener(v -> validarEntradasLogin());

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v ->
                    resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent())
            );
        }

        acessarTelaCadastro.setOnClickListener(view -> abrirTelaCadastro());
        recuperarSenha.setOnClickListener(view -> exibirDialogoRecuperacao());
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
                    if (task.isSuccessful()) {
                        iniciarFluxoSegurancaDados();
                    } else {
                        loadingHelper.ocultar();
                        String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * SAFETY NET ATUALIZADA: Garante integridade total entre Auth e Firestore.
     * Prioriza a correção de documentos fantasmas usando validação de timestamp.
     */
    private void iniciarFluxoSegurancaDados() {
        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) return;

        loadingHelper.exibir();

        // 1. Apenas VERIFICAMOS se o perfil existe no Firestore sem criar nada [cite: 2026-01-31]
        perfilRepository.verificarExistenciaPerfil(user.getUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {

                if (task.getResult().exists()) {
                    // CENÁRIO 1: Usuário real com dados íntegros [cite: 2026-01-31]
                    loadingHelper.ocultar();
                    Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
                    abrirTelaHome();
                } else {
                    // CENÁRIO 2: Auth existe mas Firestore não (Login novo ou Fantasma) [cite: 2026-01-31]
                    tratarUsuarioSemDocumento(user);
                }

            } else {
                // CENÁRIO 3: Falha técnica (Rede ou Permissão) [cite: 2026-01-31]
                loadingHelper.ocultar();
                String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                Toast.makeText(this, "Erro de sincronização: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Filtro de Segurança: Decide se inicializa os dados ou expulsa a sessão órfã.
     */
    private void tratarUsuarioSemDocumento(FirebaseUser user) {
        // Pegamos o momento exato do último login no Firebase Auth [cite: 2026-01-31]
        long momentoLogin = user.getMetadata().getLastSignInTimestamp();
        long agora = System.currentTimeMillis();

        // Se o login ocorreu há menos de 30 segundos, é uma ação legítima de agora [cite: 2025-11-10]
        if (agora - momentoLogin < 30000) {
            Toast.makeText(this, "Criando sua conta... Bem-vindo!", Toast.LENGTH_SHORT).show();

            // Inicializa toda a estrutura (Perfil, Carteira, Categorias) via Service [cite: 2025-11-10]
            usuarioService.inicializarNovoUsuario(user, taskService -> {
                loadingHelper.ocultar();
                abrirTelaHome();
            });
        } else {
            // Sessão antiga sem documento = Dados deletados no console [cite: 2025-11-10]
            // Limpamos tudo para evitar a recriação automática infinita [cite: 2026-01-31]
            loadingHelper.ocultar();
            perfilRepository.deslogar();
            if (googleLoginHelper != null) googleLoginHelper.recarregarSessaoGoogle();

            Toast.makeText(this, "Sessão expirada ou conta removida pelo sistema.", Toast.LENGTH_LONG).show();
        }
    }

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

            autenticacao.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Link enviado! Verifique seu e-mail.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
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

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro() {
        startActivity(new Intent(this, CadastroActivity.class));
    }
}