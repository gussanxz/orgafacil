package com.gussanxz.orgafacil.helper;

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
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.model.Usuario;

public class GoogleLoginHelper {

    private Activity activity;
    private FirebaseAuth autenticacao;
    private GoogleSignInClient mGoogleSignInClient;
    private Runnable onSuccessCallback; // Ação para executar após sucesso (ex: abrir Home)

    // Constantes para identificar de onde veio o clique
    public static final int MODO_MISTO = 0;    // Tela Comece Agora
    public static final int MODO_LOGIN = 1;    // Tela Login
    public static final int MODO_CADASTRO = 2; // Tela Cadastro

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
        // Desloga do Google antes de iniciar para permitir escolher conta novamente
        mGoogleSignInClient.signOut();
        return mGoogleSignInClient.getSignInIntent();
    }

    public void lidarComResultadoGoogle(Intent data, int modoOperacao) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account, modoOperacao);
            }
        } catch (ApiException e) {
            Toast.makeText(activity, "Erro Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            Log.e("GoogleLogin", "Erro API", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, int modoOperacao) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        autenticacao.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = autenticacao.getCurrentUser();
                        boolean isNovoUsuario = task.getResult().getAdditionalUserInfo().isNewUser();

                        // --- LÓGICA DE MENSAGENS (TOASTS) ---
                        if (isNovoUsuario) {
                            // Cenário: USUÁRIO NOVO
                            switch (modoOperacao) {
                                case MODO_MISTO:
                                    Toast.makeText(activity, "Conta criada, novo usuário!", Toast.LENGTH_SHORT).show();
                                    break;
                                case MODO_LOGIN:
                                    Toast.makeText(activity, "Usuário novo identificado, criando cadastro...", Toast.LENGTH_SHORT).show();
                                    break;
                                case MODO_CADASTRO:
                                    Toast.makeText(activity, "Realizando cadastro!", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                            // Como é novo, SEMPRE salva no banco
                            salvarNovoUsuarioGoogle(user);

                        } else {
                            // Cenário: USUÁRIO JÁ EXISTENTE
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
                            // Usuário antigo apenas segue para a Home
                            if (onSuccessCallback != null) onSuccessCallback.run();
                        }

                    } else {
                        Toast.makeText(activity, "Erro na autenticação.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void salvarNovoUsuarioGoogle(FirebaseUser userFirebase) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(userFirebase.getUid());
        usuario.setNome(userFirebase.getDisplayName() != null ? userFirebase.getDisplayName() : "Usuário");
        usuario.setEmail(userFirebase.getEmail());

        usuario.salvar();
        usuario.inicializarNovosDados();

        // Após salvar, chama o callback para ir para a Home
        if (onSuccessCallback != null) onSuccessCallback.run();
    }
}