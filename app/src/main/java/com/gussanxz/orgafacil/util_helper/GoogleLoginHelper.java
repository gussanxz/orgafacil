package com.gussanxz.orgafacil.util_helper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;

/**
 * Helper responsável exclusivamente pela autenticação técnica via Google.
 * A lógica de negócio (Login vs Cadastro) é delegada à Activity via callback.
 */
public class GoogleLoginHelper {

    private final Activity activity;
    private final FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;
    private final Runnable onSuccessCallback;

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

    /**
     * Retorna a Intent para abrir o seletor de contas do Google.
     * Faz o signOut prévio para garantir que o usuário possa trocar de conta se desejar.
     */
    public Intent getSignInIntent() {
        mGoogleSignInClient.signOut();
        return mGoogleSignInClient.getSignInIntent();
    }

    /**
     * Limpa a sessão do Google. Útil para fluxos de logout ou segurança.
     */
    public void recarregarSessaoGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d("GoogleLogin", "Sessão do Google limpa com sucesso.");
        });
    }

    /**
     * Lida com o resultado vindo do ActivityResultLauncher.
     */
    public void lidarComResultadoGoogle(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            // Código 12501: Usuário cancelou a seleção da conta.
            if (e.getStatusCode() != 12501) {
                Toast.makeText(activity, "Erro Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
            Log.e("GoogleLogin", "Erro na API do Google: " + e.getStatusCode());
        }
    }

    /**
     * Vincula a conta Google autenticada à sessão do Firebase Auth.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        // Notifica a Activity para iniciar a "Safety Net" (verificação Firestore)
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                    } else {
                        Toast.makeText(activity, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}