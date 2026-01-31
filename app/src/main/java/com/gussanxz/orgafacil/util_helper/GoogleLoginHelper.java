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
 * Helper responsável exclusivamente pela autenticação via Google.
 * A lógica de decisão sobre o estado do usuário (novo/antigo) foi movida
 * definitivamente para o UsuarioService.
 *
 * ESTE HELPER:
 * - Apenas autentica no Firebase Auth
 * - NÃO cria dados
 * - NÃO decide login/cadastro
 */
public class GoogleLoginHelper {

    private final Activity activity;
    private final FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;

    // =========================
    // Callback tipado (mantido por compatibilidade)
    // =========================
    public interface AuthSuccessCallback {
        void onSuccess(int modoOperacao);
    }

    // Callback executado após o sucesso no Firebase Auth
    private final AuthSuccessCallback onSuccessCallback;

    // Constantes de modo de operação (LEGADO)
    public static final int MODO_MISTO = 0;
    public static final int MODO_LOGIN = 1;
    public static final int MODO_CADASTRO = 2;

    public GoogleLoginHelper(Activity activity, AuthSuccessCallback onSuccessCallback) {
        this.activity = activity;
        this.onSuccessCallback = onSuccessCallback;
        this.autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        configurarGoogleSignIn();
    }

    private void configurarGoogleSignIn() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(activity.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public Intent getSignInIntent() {
        // Força o seletor de contas a aparecer
        mGoogleSignInClient.signOut();
        return mGoogleSignInClient.getSignInIntent();
    }

    /**
     * NOVO: Método para limpar a sessão do Google explicitamente.
     * Use isso ao excluir a conta para evitar que o Google logue sozinho em seguida.
     */
    public void recarregarSessaoGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d("GoogleLogin", "Sessão do Google limpa com sucesso.");
        });
    }

    public void lidarComResultadoGoogle(Intent data, int modoOperacao) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            // StatusCode 12501 é quando o usuário apenas fecha o seletor
            if (e.getStatusCode() != 12501) {
                Toast.makeText(
                        activity,
                        "Erro Google: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT
                ).show();
            }

            Log.e("GoogleLogin", "Erro na API do Google: " + e.getStatusCode());
        }
    }

    /**
     * Realiza a troca da credencial do Google por uma sessão no Firebase Auth.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(
                                activity,
                                "Falha na autenticação com Firebase.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // ✅ DELEGAÇÃO FINAL:
                    // O Helper apenas informa que o Auth deu certo.
                    // A decisão de criar ou entrar é do UsuarioService.
                    if (onSuccessCallback != null) {
                        onSuccessCallback.onSuccess(MODO_MISTO);
                    }
                });
    }
}
