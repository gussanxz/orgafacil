package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

import java.util.HashMap;
import java.util.Map;

public class ConfigPerfilUsuarioRepository {

    // --- MÉTODOS DE ESCRITA ---

    public Task<Void> inicializarMetadadosRaiz(FirebaseUser user) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("statusConta", ConfigPerfilUsuarioModel.StatusConta.ATIVO.name());
        meta.put("tipoPerfil", ConfigPerfilUsuarioModel.TipoPerfil.PESSOAL.name());
        meta.put("planoAtivo", ConfigPerfilUsuarioModel.PlanoAtivo.GRATUITO.name());
        meta.put("dataCriacao", FieldValue.serverTimestamp());
        meta.put("provedor", obterProvedorLogin(user));

        // [AUDITORIA] Registramos o aceite inicial na raiz por segurança extra
        meta.put("aceitouTermos", true);
        meta.put("versaoTermosCriacao", "1.0");

        return FirestoreSchema.userDoc(user.getUid()).set(meta, SetOptions.merge());
    }
    public Task<Void> salvarPerfil(ConfigPerfilUsuarioModel perfil) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", perfil.getNome() != null ? perfil.getNome() : "Novo Usuário");
        dados.put("email", perfil.getEmail() != null ? perfil.getEmail() : "");
        dados.put("updatedAt", FieldValue.serverTimestamp());

        // [NOVO] Salvando os dados de consentimento obrigatórios
        dados.put("aceitouTermos", perfil.isAceitouTermos());
        dados.put("versaoTermos", perfil.getVersaoTermos() != null ? perfil.getVersaoTermos() : "1.0");

        // Usamos serverTimestamp diretamente para garantir a precisão do horário do Google
        dados.put("dataAceite", FieldValue.serverTimestamp());

        return FirestoreSchema.configPerfilDoc().set(dados, SetOptions.merge());
    }

    public Task<Void> atualizarPerfil(ConfigPerfilUsuarioModel perfil) {
        Map<String, Object> updates = new HashMap<>();
        if (perfil.getNome() != null) updates.put("nome", perfil.getNome());
        if (perfil.getFotoUrl() != null) updates.put("fotoUrl", perfil.getFotoUrl());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return FirestoreSchema.configPerfilDoc().update(updates);
    }

    public void excluirContaSemSenha(FirebaseUser user, OnCompleteListener<Void> listener) {
        if (user == null) return;

        // 1. Deleta a raiz do usuário no Firestore via Schema
        FirestoreSchema.userDoc(user.getUid()).delete().addOnSuccessListener(aVoid -> {

            // 2. Limpa Cache via ConfiguracaoFirestore (Singleton centralizado)
            ConfiguracaoFirestore.getFirestore().clearPersistence().addOnCompleteListener(taskPersistence -> {

                // 3. Deleta Auth
                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deslogar();
                    }
                    listener.onComplete(task);
                });
            });

        }).addOnFailureListener(e -> listener.onComplete(null));
    }

    // --- MÉTODOS DE LEITURA ---

    public Task<DocumentSnapshot> verificarExistenciaPerfil() {
        // Usa myUserDoc() implicitamente através do configPerfilDoc()
        return FirestoreSchema.configPerfilDoc().get();
    }

    // --- MÉTODOS DE SESSÃO ---

    public void deslogar() {
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
    }

    public boolean estaLogado() {
        return FirebaseSession.isUserLogged();
    }

    // --- HELPERS ---

    public String mapearErroAutenticacao(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return "A senha fornecida é muito fraca.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "As credenciais informadas são inválidas.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Este e-mail já está associado a outra conta.";
        if (e instanceof FirebaseAuthInvalidUserException) return "Não foi possível encontrar este usuário.";

        if (e != null && e.getMessage() != null && e.getMessage().contains("recent login")) {
            return "Por segurança, você precisa fazer login novamente para realizar esta ação.";
        }

        return "Erro na operação: " + (e != null ? e.getLocalizedMessage() : "Desconhecido");
    }

    private String obterProvedorLogin(FirebaseUser user) {
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) return "google.com";
            }
        }
        return "password";
    }
}