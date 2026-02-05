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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.regras.BaseAuthActivity;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

public class LoginActivity extends BaseAuthActivity {

    private EditText campoEmail, campoSenha;
    private Button botaoEntrar, botaoGoogle;
    private TextView recuperarSenha;
    private RadioButton acessarTelaCadastro;
    private LoadingHelper loadingHelper;
    private GoogleLoginHelper googleLoginHelper;

    // [LIMPEZA] Não declaramos 'usuarioRepository' aqui porque HERDAMOS da BaseAuthActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_login);

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
        configurarListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Se o usuário já estiver logado (ex: rotacionou a tela),
        // a BaseAuthActivity assume e checa o banco.
        if (autenticacao.getCurrentUser() != null) {
            iniciarFluxoSegurancaDados();
        }
    }

    @Override
    protected LoadingHelper getLoadingHelper() {
        return loadingHelper;
    }

    private void inicializarComponentes() {
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);
        botaoGoogle = findViewById(R.id.btnGoogle);

        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        // Se o usuário clicar em Google DENTRO desta tela, também usamos a lógica da mãe
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDados);

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
    }

    private void configurarListeners() {
        botaoEntrar.setOnClickListener(v -> validarEntradasLogin());

        if (botaoGoogle != null) {
            botaoGoogle.setOnClickListener(v ->
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
                        // [O PULO DO GATO]
                        // O Login técnico funcionou. Agora chamamos a MÃE (BaseAuthActivity)
                        // para checar se a conta está ativa, desativada ou pendente.
                        iniciarFluxoSegurancaDados();
                    } else {
                        loadingHelper.ocultar();
                        // Usamos o 'usuarioRepository' que veio da HERANÇA
                        String erro = usuarioRepository.mapearErroAutenticacao(task.getException());
                        Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exibirDialogoRecuperacao() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recuperar_senha, null);
        dialog.setContentView(view);

        View bottomSheetInternal = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

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
                    dialog.dismiss();
                    Toast.makeText(this, "Link enviado! Verifique seu e-mail.", Toast.LENGTH_LONG).show();
                } else {
                    String erro = usuarioRepository.mapearErroAutenticacao(task.getException());
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
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

    public void abrirTelaCadastro() {
        startActivity(new Intent(this, CadastroActivity.class));
    }
}