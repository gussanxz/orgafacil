package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

public class UsuarioService {

    private final FirebaseFirestore db;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService() {
        this.db = ConfiguracaoFirestore.getFirestore();
        this.carteiraRepository = new CarteiraRepository();
    }

    /**
     * Interface para retorno simples
     */
    public interface CriacaoCallback {
        void onConcluido(com.google.android.gms.tasks.Task<Void> task);
    }

    /**
     * Cria Usuário + Carteira + Categorias em uma única transação atômica.
     */
    public void inicializarNovoUsuario(FirebaseUser user, CriacaoCallback callback) {
        if (user == null) return;

        String uid = user.getUid();

        // 1. Inicia o Batch
        WriteBatch batch = db.batch();

        // 2. Prepara dados do Usuário (Raiz)
        DocumentReference userRef = com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema.userDoc(uid);

        UsuarioModel novoUsuario = new UsuarioModel(
                uid,
                obterNome(user),
                user.getEmail(),
                obterProvedor(user)
        );

        if (user.getPhotoUrl() != null) {
            novoUsuario.setFotoUrl(user.getPhotoUrl().toString());
        }

        batch.set(userRef, novoUsuario);

        // 3. Prepara dados Financeiros
        carteiraRepository.prepararCarteiraInicial(batch, uid);
        carteiraRepository.prepararCategoriasPadrao(batch, uid);

        // 4. Commit
        batch.commit().addOnCompleteListener(task -> {
            if (callback != null) {
                callback.onConcluido(task);
            }
        });
    }

    // --- Helpers Privados ---

    private String obterNome(FirebaseUser user) {
        return (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : "Usuário";
    }

    private String obterProvedor(FirebaseUser user) {
        if (user.getProviderData() == null || user.getProviderData().isEmpty()) return "password";
        // Se tiver mais de 1 provider data, geralmente o segundo é o do Google/Facebook
        return (user.getProviderData().size() > 1) ? "google.com" : "password";
    }
}