package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.UsuarioModel;

import java.util.HashMap;
import java.util.Map;

/**
 * UsuarioRepository
 * Responsável pela persistência e leitura de dados do usuário.
 * ATUALIZADO: Usa as constantes de caminho (Dot Notation) do UsuarioModel.
 */
public class UsuarioRepository {

    private final String uidAtual;

    public UsuarioRepository() {
        this.uidAtual = FirebaseSession.isUserLogged() ? FirebaseSession.getUserId() : null;
    }

    // ============================================================================================
    // 1. CRIAÇÃO E VERIFICAÇÃO
    // ============================================================================================

    public Task<DocumentSnapshot> verificarSeUsuarioExiste(String uid) {
        return FirestoreSchema.userDoc(uid).get();
    }

    /**
     * Salva o objeto completo.
     * O UsuarioService já montou a estrutura de mapas corretamente.
     */
    public Task<Void> salvarNovoUsuario(UsuarioModel usuario) {
        return FirestoreSchema.userDoc(usuario.getUid()).set(usuario);
    }

    // ============================================================================================
    // 2. ATUALIZAÇÕES PARCIAIS (Usando Constantes Blindadas)
    // ============================================================================================

    public Task<Void> atualizarUltimaAtividade() {
        if (uidAtual == null) return null;

        // CAMPO_ULTIMA_ATIVIDADE já vale "dadosApp.ultimaAtividade"
        return FirestoreSchema.userDoc(uidAtual)
                .update(UsuarioModel.CAMPO_ULTIMA_ATIVIDADE, FieldValue.serverTimestamp());
    }

    public Task<Void> atualizarDadosPerfil(String nome, String fotoUrl) {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();

        if (nome != null) {
            // CAMPO_NOME já vale "dadosPessoais.nome"
            updates.put(UsuarioModel.CAMPO_NOME, nome);
        }
        if (fotoUrl != null) {
            // CAMPO_FOTO já vale "dadosPessoais.fotoUrl"
            updates.put(UsuarioModel.CAMPO_FOTO, fotoUrl);
        }

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    // ============================================================================================
    // 3. GERENCIAMENTO DE CONTA (Status)
    // ============================================================================================

    public Task<Void> desativarContaLogica() {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        // Atualiza status e data de desativação nos caminhos corretos
        updates.put(UsuarioModel.CAMPO_STATUS, "DESATIVADO"); // Ou use o Enum se preferir
        updates.put(UsuarioModel.CAMPO_DATA_DESATIVACAO, FieldValue.serverTimestamp());

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    public Task<Void> reativarContaLogica() {
        // Busca o UID via Schema para garantir que não está nulo
        String uid = FirestoreSchema.requireUid();

        // Atualiza apenas os campos específicos dentro do objeto dadosConta
        return FirestoreSchema.userDoc(uid)
                .update(
                        "dadosConta.status", "ATIVO",
                        "dadosConta.dataDesativacao", null
                );
    }

    // ============================================================================================
    // 4. LEITURA DE DADOS
    // ============================================================================================

    /**
     * Recupera o nome para exibição.
     */
    public void obterNomeUsuario(final OnNomeRecuperadoCallback callback) {
        if (uidAtual == null) return;

        FirestoreSchema.userDoc(uidAtual).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // O Firestore entende "dadosPessoais.nome" automaticamente no getString
                String nome = doc.getString(UsuarioModel.CAMPO_NOME);
                callback.onResultado(nome != null ? nome : "Usuário");
            }
        });
    }

    public interface OnNomeRecuperadoCallback {
        void onResultado(String nome);
    }

    // ============================================================================================
    // 5. AUTH HELPERS (Mantidos)
    // ============================================================================================

    public void deslogar() {
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
    }

    public boolean estaLogado() {
        return FirebaseSession.isUserLogged();
    }

    public Task<AuthResult> vincularCredencial(AuthCredential credential) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.linkWithCredential(credential) : null;
    }

    public String mapearErroAutenticacao(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return "A senha fornecida é muito fraca.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "E-mail ou senha inválidos.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Este e-mail já está cadastrado.";
        if (e instanceof FirebaseAuthInvalidUserException) return "Conta não encontrada ou desativada.";
        return "Erro: " + (e != null ? e.getLocalizedMessage() : "Desconhecido");
    }

    public static String obterProvedorLogin(FirebaseUser user) {
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) return "google.com";
            }
        }
        return "password";
    }
}