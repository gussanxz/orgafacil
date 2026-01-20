package com.gussanxz.orgafacil.helper;

import android.app.Activity;
import android.content.Intent;
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

    public void lidarComResultadoGoogle(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            // Código 12501 é quando o usuário cancela a seleção, não precisa mostrar erro
            if (e.getStatusCode() != 12501) {
                Toast.makeText(activity, "Erro Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
                            // --- CENÁRIO: NOVO USUÁRIO ---
                            // Como removemos a senha, apenas salvamos os dados do Google no Firestore
                            salvarNovoUsuarioGoogle(userFirebase);
                        } else {
                            // --- CENÁRIO: USUÁRIO JÁ EXISTE ---
                            // Apenas prossegue para o app
                            if (onSuccessCallback != null) onSuccessCallback.run();
                        }
                    } else {
                        Toast.makeText(activity, "Erro ao autenticar com Firebase.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void salvarNovoUsuarioGoogle(FirebaseUser userFirebase) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(userFirebase.getUid());
        usuario.setNome(userFirebase.getDisplayName());
        usuario.setEmail(userFirebase.getEmail());
        // Não definimos senha (usuario.setSenha) pois é login Google puro

        // Salva os dados básicos (Nome, Email) na coleção users/{uid}
        usuario.salvar();

        // Inicializa a conta financeira (Saldo 0.00 e Categorias Padrão)
        usuario.inicializarNovosDados();

        Toast.makeText(activity, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();

        // Chama o callback para ir para a Home
        if (onSuccessCallback != null) onSuccessCallback.run();
    }
}