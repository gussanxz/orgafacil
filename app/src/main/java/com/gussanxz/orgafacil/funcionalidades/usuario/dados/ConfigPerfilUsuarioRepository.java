package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

import java.util.HashMap;
import java.util.Map;

public class ConfigPerfilUsuarioRepository {

    // =======================
    // CRIAÇÃO / ATUALIZAÇÃO
    // =======================

    /**
     * Inicializa metadados do documento raiz do usuário.
     * Caminho: {ROOT}/{uid}
     */
    public Task<Void> inicializarMetadadosRaiz(FirebaseUser user) {
        if (user == null) return null;

        String uid = user.getUid();

        Map<String, Object> meta = new HashMap<>();
        meta.put("statusConta", ConfigPerfilUsuarioModel.StatusConta.ATIVO.name());
        meta.put("tipoPerfil", ConfigPerfilUsuarioModel.TipoPerfil.PESSOAL.name());
        meta.put("planoAtivo", ConfigPerfilUsuarioModel.PlanoAtivo.GRATUITO.name());
        meta.put("dataCriacao", FieldValue.serverTimestamp());
        meta.put("provedor", obterProvedorLogin(user));

        return FirestoreSchema.userDoc(uid).set(meta, SetOptions.merge());
    }

    /**
     * Cria ou atualiza o perfil do usuário.
     * Caminho: {ROOT}/{uid}/config/config_perfil
     */
    public Task<Void> salvarPerfil(ConfigPerfilUsuarioModel perfil) {
        if (perfil == null || perfil.getIdUsuario() == null) return null;

        String uid = perfil.getIdUsuario();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", perfil.getNome() != null ? perfil.getNome() : "Novo Usuário");
        dados.put("email", perfil.getEmail() != null ? perfil.getEmail() : "");
        dados.put("updatedAt", FieldValue.serverTimestamp());

        return FirestoreSchema.configPerfilDoc(uid)
                .set(dados, SetOptions.merge());
    }

    /**
     * Atualiza campos específicos do perfil.
     */
    public Task<Void> atualizarPerfil(ConfigPerfilUsuarioModel perfil) {
        if (perfil == null || perfil.getIdUsuario() == null) return null;

        String uid = perfil.getIdUsuario();

        Map<String, Object> updates = new HashMap<>();
        if (perfil.getNome() != null) updates.put("nome", perfil.getNome());
        if (perfil.getFotoUrl() != null) updates.put("fotoUrl", perfil.getFotoUrl());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return FirestoreSchema.configPerfilDoc(uid).update(updates);
    }

    // =======================
    // EXCLUSÃO DE CONTA
    // =======================

    /**
     * Remove Firestore + Auth.
     * OBS: Firebase exige login recente.
     */
    public void excluirContaSemSenha(FirebaseUser user, OnCompleteListener<Void> listener) {
        if (user == null) return;

        String uid = user.getUid();

        FirestoreSchema.userDoc(uid)
                .delete()
                .addOnSuccessListener(aVoid ->
                        user.delete().addOnCompleteListener(listener)
                )
                .addOnFailureListener(e ->
                        listener.onComplete(null)
                );
    }

    // =======================
    // LEITURA / VERIFICAÇÃO
    // =======================

    /**
     * Usado na SplashActivity.
     * Apenas verifica se o perfil existe.
     */
    public Task<DocumentSnapshot> verificarExistenciaPerfil(String uid) {
        return FirestoreSchema.configPerfilDoc(uid).get();
    }

    // =======================
    // SESSÃO / ERROS
    // =======================

    public void deslogar() {
        FirebaseAuth.getInstance().signOut();
    }

    public String mapearErroAutenticacao(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "A senha fornecida é muito fraca.";
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "As credenciais informadas são inválidas.";
        if (e instanceof FirebaseAuthUserCollisionException)
            return "Este e-mail já está associado a outra conta.";
        if (e instanceof FirebaseAuthInvalidUserException)
            return "Não foi possível encontrar este usuário.";

        if (e != null && e.getMessage() != null && e.getMessage().contains("recent login")) {
            return "Por segurança, faça login novamente para realizar esta ação.";
        }

        return "Erro na operação: " +
                (e != null ? e.getLocalizedMessage() : "Desconhecido");
    }

    // =======================
    // HELPERS
    // =======================

    private String obterProvedorLogin(FirebaseUser user) {
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    return "google.com";
                }
            }
        }
        return "password";
    }
}
