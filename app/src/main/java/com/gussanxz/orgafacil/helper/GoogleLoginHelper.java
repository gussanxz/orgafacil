package com.gussanxz.orgafacil.helper;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.model.Usuario;

public class GoogleLoginHelper {

    private Activity activity;
    private FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;
    private Runnable onSuccessCallback; // O que fazer quando terminar tudo com sucesso

    public GoogleLoginHelper(Activity activity, Runnable onSuccessCallback) {
        this.activity = activity;
        this.onSuccessCallback = onSuccessCallback;
        this.autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        configurarGoogleSignIn();
    }

    private void configurarGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public Intent getSignInIntent() {
        return mGoogleSignInClient.getSignInIntent();
    }

    public void lidarComResultadoGoogle(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Toast.makeText(activity, "Erro Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser userFirebase = autenticacao.getCurrentUser();
                        boolean isNovoUsuario = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNovoUsuario) {
                            // É novo? Pede senha para vincular
                            abrirDialogDefinirSenha(userFirebase);
                        } else {
                            // Já existe? Sucesso direto
                            if (onSuccessCallback != null) onSuccessCallback.run();
                        }
                    } else {
                        Toast.makeText(activity, "Erro autenticação Firebase.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void abrirDialogDefinirSenha(FirebaseUser userFirebase) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Complete seu cadastro");
        builder.setMessage("Para segurança da sua conta, defina uma senha:");

        final EditText inputSenha = new EditText(activity);
        inputSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputSenha.setHint("Digite sua senha");

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 0, 50, 0);
        container.addView(inputSenha, params);
        builder.setView(container);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String senha = inputSenha.getText().toString();
            if (senha.length() >= 6) {
                vincularSenhaAoGoogle(userFirebase, senha);
            } else {
                Toast.makeText(activity, "Senha deve ter no mínimo 6 caracteres", Toast.LENGTH_LONG).show();
                // O ideal seria reabrir o dialog aqui, mas simplificamos
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void vincularSenhaAoGoogle(FirebaseUser user, String senha) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), senha);

        user.linkWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        // Salvar dados do usuário
                        Usuario usuario = new Usuario();
                        usuario.setIdUsuario(user.getUid());
                        usuario.setNome(user.getDisplayName());
                        usuario.setEmail(user.getEmail());
                        usuario.setSenha(senha); // Salva apenas para referência se necessário, mas o Auth já gerencia
                        usuario.salvar();
                        usuario.inicializarNovosDados();

                        Toast.makeText(activity, "Cadastro concluído!", Toast.LENGTH_SHORT).show();
                        if (onSuccessCallback != null) onSuccessCallback.run();
                    } else {
                        Toast.makeText(activity, "Erro ao vincular: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}