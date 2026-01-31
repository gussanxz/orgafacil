package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

/**
 * SERVICE: USUÁRIO
 * Orquestra a criação coordenada entre Perfil e Carteira.
 *
 * REGRA DE OURO:
 * Se este Service for chamado, o contexto JÁ decidiu que é um CADASTRO.
 * Aqui não existe mais lógica de decisão, apenas execução.
 *
 * EXCEÇÃO CONTROLADA:
 * - Login via Google pode criar usuário AUTOMATICAMENTE,
 *   desde que a conta tenha sido criada AGORA.
 */
public class UsuarioService {

    private static final String TAG = "UsuarioService";

    private final ConfigPerfilUsuarioRepository perfilRepository;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService() {
        this.perfilRepository = new ConfigPerfilUsuarioRepository();
        this.carteiraRepository = new CarteiraRepository();
    }

    /**
     * FLUXO HÍBRIDO SEGURO PARA LOGIN VIA GOOGLE
     *
     * REGRA:
     * - Se Firestore existe → entra
     * - Se Firestore NÃO existe:
     *      - cria SOMENTE se a conta Google acabou de nascer
     *      - caso contrário, considera conta inválida (excluída)
     *
     * ESTE MÉTODO:
     * - Centraliza a regra
     * - Elimina documento fantasma
     * - Preserva UX simples (Google cria e entra)
     */
    public void processarLoginGoogle(
            FirebaseUser user,
            Runnable onUsuarioExistente,
            Runnable onUsuarioCriado,
            Runnable onContaInvalida
    ) {
        if (user == null) {
            Log.e(TAG, "FirebaseUser nulo no login Google.");
            return;
        }

        long created = user.getMetadata().getCreationTimestamp();
        long lastLogin = user.getMetadata().getLastSignInTimestamp();

        // Conta Google acabou de ser criada agora
        boolean contaNova = Math.abs(created - lastLogin) < 2000;

        perfilRepository.verificarExistenciaPerfil(user.getUid())
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().exists()) {

                        // Usuário antigo, dados íntegros
                        Log.d(TAG, "Login Google: usuário existente.");
                        if (onUsuarioExistente != null) onUsuarioExistente.run();
                        return;
                    }

                    if (contaNova) {
                        // Usuário novo via Google → criação automática permitida
                        Log.d(TAG, "Login Google: criando novo usuário.");

                        inicializarNovoUsuario(user, t -> {
                            if (onUsuarioCriado != null) onUsuarioCriado.run();
                        });

                    } else {
                        // Sessão reaproveitada sem Firestore → conta excluída
                        Log.w(TAG, "Login Google inválido: sessão antiga sem Firestore.");

                        FirebaseAuth.getInstance().signOut();

                        if (onContaInvalida != null) onContaInvalida.run();
                    }
                });
    }

    /**
     * EXECUÇÃO PURA DE CADASTRO
     *
     * IMPORTANTE:
     * - Este método NÃO verifica se o usuário já existe.
     * - A decisão de criar ou não pertence ao contexto chamador.
     * - Aqui apenas executamos o pipeline completo.
     */
    public void inicializarNovoUsuario(FirebaseUser user, OnCompleteListener<Void> listener) {
        if (user == null) {
            Log.e(TAG, "FirebaseUser nulo. Abortando inicialização.");
            return;
        }

        String uid = user.getUid();

        // 1. Metadados na raiz
        perfilRepository.inicializarMetadadosRaiz(user).addOnSuccessListener(aVoid -> {

            // 2. Perfil
            ConfigPerfilUsuarioModel perfil = new ConfigPerfilUsuarioModel();
            perfil.setIdUsuario(uid);
            perfil.setNome(user.getDisplayName() != null ? user.getDisplayName() : "Usuário");
            perfil.setEmail(user.getEmail());

            perfilRepository.salvarPerfil(perfil).addOnSuccessListener(aVoid1 -> {

                // 3. Carteira
                carteiraRepository.inicializarSaldosContas(uid).addOnSuccessListener(aVoid2 -> {

                    // 4. Categorias padrão
                    carteiraRepository.inicializarCategoriasPadrao(uid).addOnSuccessListener(aVoid3 -> {
                        Log.d(TAG, "Usuário inicializado com sucesso: " + uid);
                        listener.onComplete(null);

                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao criar categorias padrão", e);
                        listener.onComplete(null);
                    });

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao criar carteira", e);
                    listener.onComplete(null);
                });

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Erro ao salvar perfil", e);
                listener.onComplete(null);
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erro ao inicializar metadados raiz", e);
            listener.onComplete(null);
        });
    }
}
