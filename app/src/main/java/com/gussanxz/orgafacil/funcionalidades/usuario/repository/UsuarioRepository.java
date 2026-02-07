package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

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
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

import java.util.HashMap;
import java.util.Map;

/**
 * UsuarioRepository
 * O que esta classe faz:
 * 1. Gerencia a persistência da Identidade (UsuarioModel) no Firestore.
 * 2. Utiliza as constantes do Model para evitar erros de digitação (Blindagem).
 * 3. Centraliza lógicas de Auth e mapeamento de erros.
 */
public class UsuarioRepository {

    private final String uidAtual;

    public UsuarioRepository() {
        // Pensa fora da caixa: Carregamos o UID da sessão para evitar NullPointer nas chamadas
        this.uidAtual = FirebaseSession.isUserLogged() ? FirebaseSession.getUserId() : null;
    }

    // ============================================================================================
    // 1. CRIAÇÃO E VERIFICAÇÃO (Login/Cadastro)
    // ============================================================================================

    /**
     * Verifica se o documento do usuário já existe no Firestore.
     */
    public Task<DocumentSnapshot> verificarSeUsuarioExiste(String uid) {
        return FirestoreSchema.userDoc(uid).get();
    }

    /**
     * Salva um NOVO usuário.
     * Implementa a lógica de garantir que timestamps iniciais venham do servidor.
     */
    public Task<Void> salvarNovoUsuario(UsuarioModel usuario) {
        // Blindagem: Garantimos que datas críticas iniciem nulas para o @ServerTimestamp atuar
        usuario.setDataCriacaoConta(null);
        usuario.setUltimaAtividade(null);

        // Se a versão do app não foi setada, obtemos via PackageManager
        if (usuario.getVersaoApp() == null) {
            usuario.setVersaoApp(obterVersaoAppGlobal());
        }

        return FirestoreSchema.userDoc(usuario.getUid()).set(usuario);
    }

    // ============================================================================================
    // 2. ATUALIZAÇÕES PARCIAIS (Update)
    // ============================================================================================

    /**
     * Atualiza o timestamp de última atividade usando a constante do Model.
     */
    public Task<Void> atualizarUltimaAtividade() {
        if (uidAtual == null) return null;
        return FirestoreSchema.userDoc(uidAtual)
                .update(UsuarioModel.CAMPO_ULTIMA_ATIVIDADE, FieldValue.serverTimestamp());
    }

    /**
     * Atualiza dados de perfil usando mapeamento por constantes (Blindagem).
     */
    public Task<Void> atualizarDadosPerfil(String nome, String fotoUrl) {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        if (nome != null) updates.put(UsuarioModel.CAMPO_NOME, nome);
        if (fotoUrl != null) updates.put(UsuarioModel.CAMPO_FOTO, fotoUrl);

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    // ============================================================================================
    // 3. GERENCIAMENTO DE CONTA (Status)
    // ============================================================================================

    /**
     * Desativa a conta logicamente (Soft Delete) alterando o Status para DESATIVADO.
     */
    public Task<Void> desativarContaLogica() {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        updates.put(UsuarioModel.CAMPO_STATUS, UsuarioModel.StatusConta.DESATIVADO.name());
        updates.put("dataDesativacaoConta", FieldValue.serverTimestamp());

        return FirestoreSchema.userDoc(uidAtual).update(updates);
    }

    /**
     * Reativa a conta e limpa a data de desativação.
     */
    public Task<Void> reativarContaLogica() {
        if (uidAtual == null) return null;

        Map<String, Object> updates = new HashMap<>();
        updates.put(UsuarioModel.CAMPO_STATUS, UsuarioModel.StatusConta.ATIVO.name());
        updates.put("dataDesativacaoConta", null);

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

    /**
     * Recupera o nome do usuário para saudações na UI.
     */
    public void obterNomeUsuario(final OnNomeRecuperadoCallback callback) {
        if (uidAtual == null) return;

        FirestoreSchema.userDoc(uidAtual).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String nome = doc.getString(UsuarioModel.CAMPO_NOME);
                callback.onResultado(nome != null ? nome : "Usuário");
            }
        });
    }

    public interface OnNomeRecuperadoCallback {
        void onResultado(String nome);
    }

    // --- MÉTODOS UTILITÁRIOS ---

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