package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import android.util.Log;
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

    private static final String TAG = "ConfigPerfilRepository";

    // --- MÉTODOS DE ESCRITA (CRIAÇÃO/EDIÇÃO/EXCLUSÃO) ---

    public Task<Void> inicializarMetadadosRaiz(FirebaseUser user) {
        String uid = user.getUid();
        Map<String, Object> meta = new HashMap<>();
        meta.put("statusConta", ConfigPerfilUsuarioModel.StatusConta.ATIVO.name());
        meta.put("tipoPerfil", ConfigPerfilUsuarioModel.TipoPerfil.PESSOAL.name());
        meta.put("planoAtivo", ConfigPerfilUsuarioModel.PlanoAtivo.GRATUITO.name());
        meta.put("dataCriacao", FieldValue.serverTimestamp());
        meta.put("provedor", obterProvedorLogin(user));

        return FirestoreSchema.userDoc(uid).set(meta, SetOptions.merge());
    }

    public Task<Void> salvarPerfil(ConfigPerfilUsuarioModel perfil) {
        String uid = perfil.getIdUsuario();
        if (uid == null) return null;

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", perfil.getNome() != null ? perfil.getNome() : "Novo Usuário");
        dados.put("email", perfil.getEmail() != null ? perfil.getEmail() : "");
        dados.put("updatedAt", FieldValue.serverTimestamp());

        return FirestoreSchema.userDoc(uid)
                .collection("config")
                .document("config_perfil_usuario")
                .set(dados, SetOptions.merge());
    }

    public Task<Void> atualizarPerfil(ConfigPerfilUsuarioModel perfil) {
        String uid = perfil.getIdUsuario();
        if (uid == null) return null;

        Map<String, Object> updates = new HashMap<>();
        if (perfil.getNome() != null) updates.put("nome", perfil.getNome());
        if (perfil.getFotoUrl() != null) updates.put("fotoUrl", perfil.getFotoUrl());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return FirestoreSchema.userDoc(uid)
                .collection("config")
                .document("config_perfil_usuario")
                .update(updates);
    }

    /**
     * EXCLUSÃO SIMPLIFICADA (PROVISÓRIA): Limpa Firestore e deleta Auth sem senha.
     * Importante: O Firebase exige login recente para esta operação.
     */
    /**
     * Remove os dados do Firestore e a conta do Firebase Auth.
     * Requer OnCompleteListener para notificar o sucesso/falha à Activity.
     */
    public void excluirContaSemSenha(FirebaseUser user, OnCompleteListener<Void> listener) {
        if (user == null) return;
        String uid = user.getUid();

        // 1. Deleta o documento no Firestore (Nuvem)
        FirestoreSchema.userDoc(uid).delete().addOnSuccessListener(aVoid -> {

            // 2. Limpa o Cache Local (O SEGREDO ANTI-FANTASMA)
            // Agora isso vai funcionar porque adicionamos o getFirestore() no Schema
            FirestoreSchema.getFirestore().clearPersistence().addOnCompleteListener(taskPersistence -> {

                // 3. Deleta a conta de Autenticação
                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseAuth.getInstance().signOut();
                    }
                    listener.onComplete(task);
                });
            });

        }).addOnFailureListener(e -> {
            // Em caso de erro, notifica mesmo assim
            listener.onComplete(null);
        });
    }

    // --- MÉTODOS DE LEITURA (VERIFICAÇÃO) ---

    /**
     * NOVO: Apenas consulta se o perfil existe.
     * Use este método na SplashActivity para decidir se o usuário entra ou sai.
     */
    public Task<DocumentSnapshot> verificarExistenciaPerfil(String uid) {
        return FirestoreSchema.userDoc(uid)
                .collection("config")
                .document("config_perfil_usuario")
                .get();
    }

    // --- MÉTODOS DE SESSÃO ---

    public void deslogar() {
        FirebaseAuth.getInstance().signOut();
    }

    public String mapearErroAutenticacao(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return "A senha fornecida é muito fraca.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "As credenciais informadas são inválidas.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Este e-mail já está associado a outra conta.";
        if (e instanceof FirebaseAuthInvalidUserException) return "Não foi possível encontrar este usuário.";

        // NOVO: Tratativa para operações sensíveis que exigem login recente [cite: 2025-11-10]
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