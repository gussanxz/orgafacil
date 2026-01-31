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
 * A lógica de criação de dados no Firestore foi movida para o UsuarioRepository
 * para evitar duplicidade e conflitos.
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
        // Força o logout do Google antes de iniciar para permitir trocar de conta
        mGoogleSignInClient.signOut();
        return mGoogleSignInClient.getSignInIntent();
    }

    public void lidarComResultadoGoogle(Intent data, int modoOperacao) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) firebaseAuthWithGoogle(account, modoOperacao);
        } catch (ApiException e) {
            Toast.makeText(activity, "Erro Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            Log.e("GoogleLogin", "Erro na API do Google", e);
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

                    // Identifica se é um usuário novo apenas para fins de Log/Toast
                    boolean isNovoUsuario = false;
                    try {
                        if (task.getResult() != null && task.getResult().getAdditionalUserInfo() != null) {
                            isNovoUsuario = task.getResult().getAdditionalUserInfo().isNewUser();
                        }
                    } catch (Exception ignored) {}

                    // Logs informativos baseados no modo de operação
                    processarMensagensLog(modoOperacao, isNovoUsuario);

                    // ✅ DELEGAÇÃO: Notifica a Activity que o login foi feito.
                    // A partir daqui, a Activity chamará o UsuarioRepository.garantirDadosIniciais()
                    // que centraliza toda a lógica de segurança e criação de documentos.
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                });
    }

    /**
     * Gerencia os Toasts de feedback para o usuário.
     */
    private void processarMensagensLog(int modo, boolean novo) {
        if (novo) {
            Toast.makeText(activity, "Preparando sua nova conta...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show();
        }
    }
}