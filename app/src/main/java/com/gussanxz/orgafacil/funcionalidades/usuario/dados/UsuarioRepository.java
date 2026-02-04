package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia os dados CORE do usuário (Identidade, Status, Plano).
 * Salva na raiz: users/{uid}
 */
public class UsuarioRepository {

    private final String uidAtual;

    public UsuarioRepository() {
        this.uidAtual = FirebaseSession.isUserLogged() ? FirebaseSession.getUserId() : null;
    }

    // ============================================================================================
    // 1. CRIAÇÃO E VERIFICAÇÃO (Login/Cadastro)
    // ============================================================================================

    /**
     * Verifica se o documento do usuário já existe no Firestore.
     * Essencial para decidir se devemos criar um novo ou apenas logar.
     */
    public Task<DocumentSnapshot> verificarSeUsuarioExiste(String uid) {
        return FirestoreSchema.userDoc(uid).get();
    }

    /**
     * Salva um NOVO usuário.
     * ATENÇÃO: Use apenas no cadastro inicial, pois sobrescreve dados de criação.
     */
    public Task<Void> salvarNovoUsuario(UsuarioModel usuario) {
        // Garante que as datas sejam geradas pelo servidor
        usuario.setDataCriacaoConta(null);
        usuario.setUltimaAtividade(null);

        // Se a versão do app não foi setada, tentamos obter
        if (usuario.getVersaoApp() == null) {
            usuario.setVersaoApp(obterVersaoAppGlobal());
        }

        return FirestoreSchema.userDoc(usuario.getUid()).set(usuario);
    }

    // ============================================================================================
    // 2. ATUALIZAÇÕES PARCIAIS (Update)
    // ============================================================================================

    /**
     * Atualiza apenas o timestamp de "Estou Vivo".
     * Deve ser chamado no onStart() da MainActivity.
     */
    public Task<Void> atualizarUltimaAtividade() {
        if (uidAtual == null) return null;
        return FirestoreSchema.userDoc(uidAtual)
                .update("ultimaAtividade", FieldValue.serverTimestamp());
    }

    /**
     * Atualiza dados cadastrais (Nome, Foto) sem mexer em status ou datas críticas.
     */
    public Task<Void> atualizarDadosPerfil(String nome, String fotoUrl) {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        if (nome != null) updates.put("nome", nome);
        if (fotoUrl != null) updates.put("fotoUrl", fotoUrl);

        // Não atualizamos 'updatedAt' aqui pois isso é do UsuarioModel (Identity),
        // logs de alteração ficam na subcoleção de auditoria se necessário.

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    // ============================================================================================
    // 3. GERENCIAMENTO DE CONTA (Status)
    // ============================================================================================

    public Task<Void> desativarContaLogica() {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", UsuarioModel.StatusConta.DESATIVADO.name()); //
        updates.put("dataDesativacaoConta", FieldValue.serverTimestamp());

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    public Task<Void> reativarContaLogica() {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", UsuarioModel.StatusConta.ATIVO.name());
        updates.put("dataDesativacaoConta", null); // Remove a data de exclusão

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    // ============================================================================================
    // 4. AUTH & SESSÃO (Helpers)
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

    // --- MÉTODOS UTILITÁRIOS (Helpers Estáticos) ---

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
                // Prioriza google.com se existir na lista de provedores
                if ("google.com".equals(profile.getProviderId())) return "google.com";
            }
        }
        return "password"; // Default
    }

    private String obterVersaoAppGlobal() {
        try {
            Context c = com.google.firebase.FirebaseApp.getInstance().getApplicationContext();
            PackageInfo pInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}