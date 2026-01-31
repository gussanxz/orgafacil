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
 * A lógica de decisão sobre o estado do usuário (novo/antigo) foi movida para o fluxo
 * de Safety Net na LoginActivity/Repository para garantir integridade com o Firestore.
 */
public class GoogleLoginHelper {

    private final Activity activity;
    private final FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;

    // Callback executado após o sucesso no Firebase Auth
    private final Runnable onSuccessCallback;

    // Constantes de modo de operação
    public static final int MODO_MISTO = 0;
    public static final int MODO_LOGIN = 1;
    public static final int MODO_CADASTRO = 2;

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
        // Força o seletor de contas a aparecer, essencial para o fluxo de "Criar Conta"
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
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) firebaseAuthWithGoogle(account, modoOperacao);
        } catch (ApiException e) {
            // StatusCode 12501 é quando o usuário apenas fecha o seletor (não é um erro real)
            if (e.getStatusCode() != 12501) {
                Toast.makeText(activity, "Erro Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
            Log.e("GoogleLogin", "Erro na API do Google: " + e.getStatusCode());
        }
    }

    /**
     * Realiza a troca da credencial do Google por uma sessão no Firebase Auth.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, int modoOperacao) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(activity, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ✅ DELEGAÇÃO: Notifica a Activity que o login Auth foi feito.
                    // Agora a LoginActivity usará a Safety Net para verificar se os dados
                    // existem no Firestore e decidir se mostra "Bem-vindo" ou "Criando conta".
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                });
    }
}