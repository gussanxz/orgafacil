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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.data.model.Usuario;

public class GoogleLoginHelper {

    private final Activity activity;
    private final FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;

    // ✅ Callback simples (abre Home)
    private final Runnable onSuccessCallback;

    // Constantes
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
        // Opcional: se quiser sempre escolher conta
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
            Log.e("GoogleLogin", "Erro API", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, int modoOperacao) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(activity, "Erro na autenticação.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = autenticacao.getCurrentUser();

                    boolean isNovoUsuario = false;
                    try {
                        if (task.getResult() != null
                                && task.getResult().getAdditionalUserInfo() != null) {
                            isNovoUsuario = task.getResult().getAdditionalUserInfo().isNewUser();
                        }
                    } catch (Exception ignored) {}

                    // ✅ 1) NAVEGA IMEDIATO (NUNCA BLOQUEIA POR FIRESTORE)
                    if (onSuccessCallback != null) onSuccessCallback.run();

                    // ✅ 2) Toasts (só informativo)
                    if (isNovoUsuario) {
                        switch (modoOperacao) {
                            case MODO_MISTO:
                                Toast.makeText(activity, "Conta criada, novo usuário!", Toast.LENGTH_SHORT).show();
                                break;
                            case MODO_LOGIN:
                                Toast.makeText(activity, "Usuário novo identificado!", Toast.LENGTH_SHORT).show();
                                break;
                            case MODO_CADASTRO:
                                Toast.makeText(activity, "Realizando cadastro!", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        // ✅ 3) Setup Firestore em paralelo (não trava navegação)
                        if (user != null) salvarNovoUsuarioGoogleSemBloquear(user);

                    } else {
                        switch (modoOperacao) {
                            case MODO_MISTO:
                                Toast.makeText(activity, "Login realizado, bem-vindo!", Toast.LENGTH_SHORT).show();
                                break;
                            case MODO_LOGIN:
                                Toast.makeText(activity, "Fazendo login...", Toast.LENGTH_SHORT).show();
                                break;
                            case MODO_CADASTRO:
                                Toast.makeText(activity, "Usuário já cadastrado, fazendo login...", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    /**
     * ✅ Importante: não pode bloquear a navegação.
     * Se falhar (ex: PERMISSION_DENIED), só loga/Toast.
     */
    private void salvarNovoUsuarioGoogleSemBloquear(FirebaseUser userFirebase) {
        try {
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(userFirebase.getUid());
            usuario.setNome(userFirebase.getDisplayName() != null ? userFirebase.getDisplayName() : "Usuário");
            usuario.setEmail(userFirebase.getEmail());

            // Essas chamadas provavelmente são async. Não dependa delas pra navegar.
            usuario.salvar();
            usuario.inicializarNovosDados();

        } catch (Exception e) {
            Log.e("GoogleLogin", "Falha ao iniciar setup do usuário", e);
            Toast.makeText(activity, "Login OK, mas falhou setup inicial.", Toast.LENGTH_SHORT).show();
        }
    }
}
