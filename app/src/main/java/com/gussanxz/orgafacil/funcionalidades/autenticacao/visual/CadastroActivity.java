package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.util_helper.GoogleLoginHelper;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;
import com.gussanxz.orgafacil.util_helper.TemaHelper;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

public class CadastroActivity extends com.gussanxz.orgafacil.funcionalidades.autenticacao.regras.BaseAuthActivity {

    private RadioButton acessarTelaLogin;
    private EditText campoNome, campoEmail, campoSenha, campoSenhaConfirmacao;
    private Button botaoCadastrar, botaoGoogle;
    private View v1, v2, v3, v4; // Barras de força
    private TextView textDicaSenha;

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

        // Inicialização das barras de força
        v1 = findViewById(R.id.viewForca1);
        v2 = findViewById(R.id.viewForca2);
        v3 = findViewById(R.id.viewForca3);
        v4 = findViewById(R.id.viewForca4);
        textDicaSenha = findViewById(R.id.textDicaSenha);

        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));
        googleLoginHelper = new GoogleLoginHelper(this, this::iniciarFluxoSegurancaDados);

        VisibilidadeHelper.ativarAlternanciaSenha(campoSenha);
        VisibilidadeHelper.ativarAlternanciaSenha(campoSenhaConfirmacao);

        botaoCadastrar.setEnabled(false);
        botaoCadastrar.setAlpha(0.5f);
    }

    private void configurarListeners() {
        botaoCadastrar.setOnClickListener(v -> validarEPreProcessarCadastro());
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
                    String senha = s.toString();
                    int forca = calcularForcaSenha(senha);
                    atualizarBarraVisual(forca, senha.isEmpty());
                    atualizarEstadoBotao();
                };
                debounceHandler.postDelayed(runnableSenha, 200);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private int calcularForcaSenha(String senha) {
        int nivel = 0;
        if (senha.length() >= 6) nivel++;
        if (senha.matches(".*[A-Z].*")) nivel++;
        if (senha.matches(".*[0-9].*")) nivel++;
        if (senha.matches(".*[@#$%^&+=!].*")) nivel++;
        return nivel;
    }

    private void atualizarBarraVisual(int nivel, boolean vazia) {
        int neutro = Color.parseColor("#DDDDDD");
        v1.setBackgroundColor(neutro);
        v2.setBackgroundColor(neutro);
        v3.setBackgroundColor(neutro);
        v4.setBackgroundColor(neutro);

        if (vazia) {
            textDicaSenha.setVisibility(View.GONE);
            return;
        }

        textDicaSenha.setVisibility(View.VISIBLE);

        if (nivel >= 1) {
            v1.setBackgroundColor(Color.RED);
            textDicaSenha.setText("Muito fraca");
            textDicaSenha.setTextColor(Color.RED);
        }
        if (nivel >= 2) {
            int laranja = Color.parseColor("#FF9800");
            v1.setBackgroundColor(laranja);
            v2.setBackgroundColor(laranja);
            textDicaSenha.setText("Fraca");
            textDicaSenha.setTextColor(laranja);
        }
        if (nivel >= 3) {
            int amarelo = Color.parseColor("#FBC02D");
            v1.setBackgroundColor(amarelo);
            v2.setBackgroundColor(amarelo);
            v3.setBackgroundColor(amarelo);
            textDicaSenha.setText("Boa");
            textDicaSenha.setTextColor(amarelo);
        }
        if (nivel >= 4) {
            int verde = Color.parseColor("#4CAF50");
            v1.setBackgroundColor(verde);
            v2.setBackgroundColor(verde);
            v3.setBackgroundColor(verde);
            v4.setBackgroundColor(verde);
            textDicaSenha.setText("Forte");
            textDicaSenha.setTextColor(verde);
        }
    }

    private void atualizarEstadoBotao() {
        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();

        // O botão só habilita se a senha for no mínimo "Boa" (nível 3)
        boolean senhaSegura = calcularForcaSenha(senha) >= 3;
        boolean emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean tudoOk = !nome.isEmpty() && emailValido && senhaSegura;

        botaoCadastrar.setEnabled(tudoOk);
        botaoCadastrar.setAlpha(tudoOk ? 1.0f : 0.5f);
    }

    private void exibirDialogoTermos(boolean viaGoogle) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_termos, null);
        bottomSheet.setContentView(view);

        CheckBox checkDialog = view.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = view.findViewById(R.id.btnAceitarTermos);

        checkDialog.setOnCheckedChangeListener((buttonView, isChecked) -> btnAceitar.setEnabled(isChecked));

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