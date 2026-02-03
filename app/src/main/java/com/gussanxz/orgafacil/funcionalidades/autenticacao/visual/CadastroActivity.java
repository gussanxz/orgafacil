package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

public class CadastroActivity extends com.gussanxz.orgafacil.funcionalidades.autenticacao.regras.BaseAuthActivity {

    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;
    // Removido o CheckBox da tela principal pois ele não existe mais no seu XML
    private LoadingHelper loadingHelper;
    private GoogleLoginHelper googleLoginHelper;

    private final Handler debounceHandler = new Handler();
    private Runnable runnableSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);

        botaoCadastrar.setEnabled(false);
        botaoCadastrar.setAlpha(0.5f);
    }

    private void configurarListeners() {
        botaoCadastrar.setOnClickListener(v -> validarEPreProcessarCadastro());

        // PASSO B: Interceptando o clique do Google para mostrar os termos primeiro
        botaoGoogle.setOnClickListener(v -> exibirDialogoTermos(true));

        acessarTelaLogin.setOnClickListener(v -> abrirTelaLogin());
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
                    validarForcaSenha(s.toString());
                    atualizarEstadoBotao();
                };
                debounceHandler.postDelayed(runnableSenha, 300);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validarForcaSenha(String senha) {
        Drawable cadeado = ContextCompat.getDrawable(this, R.drawable.ic_cadeado_cinza_24);
        Drawable olhoAtual = campoSenha.getCompoundDrawables()[2];

        if (senha.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$")) {
            Drawable checkIcon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle_24);
            if (checkIcon != null) checkIcon.setTint(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, checkIcon, null);
            campoSenha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_green_dark)));
        } else {
            campoSenha.setCompoundDrawablesWithIntrinsicBounds(cadeado, null, olhoAtual, null);
            campoSenha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cor_texto)));
        }
    }

    private void atualizarEstadoBotao() {
        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();

        boolean senhaForte = senha.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$");
        boolean emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

        // Agora não depende mais do CheckBox principal, apenas dos dados
        boolean tudoOk = !nome.isEmpty() && emailValido && senhaForte;

        botaoCadastrar.setEnabled(tudoOk);
        botaoCadastrar.setAlpha(tudoOk ? 1.0f : 0.5f);
    }

    // PASSO C: Dialog Inteligente que decide o próximo passo
    private void exibirDialogoTermos(boolean viaGoogle) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_termos, null);
        bottomSheet.setContentView(view);

        CheckBox checkDialog = view.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = view.findViewById(R.id.btnAceitarTermos);

        checkDialog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // A mágica acontece aqui: ao mudar o enabled, o backgroundTint muda de cor sozinho
            btnAceitar.setEnabled(isChecked);
        });

        btnAceitar.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (viaGoogle) {
                resultLauncherGoogle.launch(googleLoginHelper.getSignInIntent());
            } else {
                executarCadastroFirebase();
            }
        });

        bottomSheet.show();
    }

    private void validarEPreProcessarCadastro() {
        String senha = campoSenha.getText().toString().trim();
        String conf = campoSenhaConfirmacao.getText().toString().trim();

        if (!senha.equals(conf)) {
            campoSenhaConfirmacao.setError("As senhas não coincidem!");
            return;
        }

        // Se os dados estão OK, abre os termos para o "aperto de mão" final
        exibirDialogoTermos(false);
    }

    private void executarCadastroFirebase() {
        loadingHelper.exibir();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString().trim();

        autenticacao.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        iniciarFluxoSegurancaDados();
                    } else {
                        loadingHelper.ocultar();
                        Toast.makeText(this, perfilRepository.mapearErroAutenticacao(task.getException()), Toast.LENGTH_SHORT).show();
                    }
                });
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