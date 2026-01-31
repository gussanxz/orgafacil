package com.gussanxz.orgafacil.funcionalidades.usuario;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CLASSE REPOSITORY - USUÁRIO
 * Centraliza toda a manipulação de dados do usuário e autenticação.
 */
public class UsuarioRepository {

    private static final String TAG = "UsuarioRepository";

    // --- MÉTODOS DE PERFIL ---

    /**
     * Salva o perfil completo do usuário (usado no cadastro).
     */
    public void salvarPerfil(UsuarioModel usuarioModel) {
        String uid = usuarioModel.getIdUsuario();
        if (uid == null) return;

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", usuarioModel.getNome() != null ? usuarioModel.getNome() : "Novo Usuário");
        perfil.put("email", usuarioModel.getEmail());
        perfil.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .set(perfil, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao salvar perfil", e));
    }

    /**
     * Atualiza campos específicos do perfil (usado na PerfilActivity).
     */
    public void atualizarPerfil(UsuarioModel usuarioModel) {
        String uid = usuarioModel.getIdUsuario();
        if (uid == null) return;

        Map<String, Object> patch = new HashMap<>();
        if (usuarioModel.getNome() != null) patch.put("nome", usuarioModel.getNome());
        patch.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .update(patch)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Perfil atualizado com sucesso"))
                .addOnFailureListener(e -> Log.e(TAG, "Falha na atualização do perfil", e));
    }

    // --- MÉTODOS DE AUTENTICAÇÃO E SEGURANÇA ---

    /**
     * Altera a senha do usuário com reautenticação obrigatória.
     */
    public void alterarSenhaComReautenticacao(FirebaseUser user, String senhaAtual, String novaSenha, OnCompleteListener<Void> listener) {
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), senhaAtual);

        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                user.updatePassword(novaSenha).addOnCompleteListener(listener);
            } else {
                listener.onComplete(reauthTask);
            }
        });
    }

    /**
     * EXCLUSÃO DEFINITIVA: Limpa Firestore e deleta Auth.
     * Importante: Exige login recente via senha.
     */
    public void excluirContaDefinitivamente(FirebaseUser user, String senha, OnCompleteListener<Void> listener) {
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), senha);

        // 1. Reautentica
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                String uid = user.getUid();

                // 2. Remove o documento de usuário no Firestore
                FirestoreSchema.userDoc(uid).delete().addOnSuccessListener(aVoid -> {
                    // 3. Remove a conta de autenticação
                    user.delete().addOnCompleteListener(listener);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao apagar Firestore durante exclusão", e);
                    listener.onComplete(null);
                });
            } else {
                listener.onComplete(reauthTask);
            }
        });
    }

    /**
     * Envia e-mail de redefinição de senha para o usuário.
     * O Firebase cuidará do envio do link seguro para o endereço informado.
     */
    public void enviarEmailRecuperacao(String email, OnCompleteListener<Void> listener) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }

    public void deslogar() {
        FirebaseAuth.getInstance().signOut();
    }

    // --- MÉTODOS DE INICIALIZAÇÃO DE SISTEMA ---

    /**
     * Cria categorias iniciais para novos usuários via Batch.
     */
    public void inicializarCategoriasPadrao(String uid) {
        List<String> categorias = Arrays.asList(
                "Alimentação", "Aluguel", "Lazer", "Mercado", "Educação", "Saúde"
        );

        FirebaseFirestore fs = FirestoreSchema.db();
        WriteBatch batch = fs.batch();

        int ordem = 0;
        for (String nome : categorias) {
            String slugId = gerarSlug(nome);

            DocumentReference ref = FirestoreSchema.userDoc(uid)
                    .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                    .collection(FirestoreSchema.CONTAS_CATEGORIAS).document(slugId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("tipo", "Despesa");
            doc.put("ativo", true);
            doc.put("ordem", ordem++);
            doc.put("createdAt", FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Categorias padrão criadas"))
                .addOnFailureListener(e -> Log.e(TAG, "Erro no batch de categorias", e));
    }

    /**
     * Transforma texto em ID amigável (slug).
     */
    private String gerarSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "_") // Garante apenas letras, números e underline
                .replaceAll("_+", "_"); // Evita múltiplos underlines seguidos
    }

    /**
     * Lógica de Safety Net: Verifica se o usuário tem o perfil criado.
     * Se não tiver, recria os dados básicos para evitar crash.
     */
    public void garantirDadosIniciais(FirebaseUser user, OnCompleteListener<Void> listener) {
        String uid = user.getUid();

        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        listener.onComplete(null); // Tudo certo, pode seguir
                    } else {
                        // Se não existir, cria o básico
                        UsuarioModel novo = new UsuarioModel();
                        novo.setIdUsuario(uid);
                        novo.setEmail(user.getEmail());
                        novo.setNome(user.getDisplayName() != null ? user.getDisplayName() : "Usuário");

                        salvarPerfil(novo);
                        inicializarCategoriasPadrao(uid);
                        listener.onComplete(null);
                    }
                });
    }

    public String mapearErroAutenticacao(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return "Digite uma senha mais forte (mínimo 6 caracteres).";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Por favor, digite um e-mail válido.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Esta conta já foi cadastrada!";
        if (e instanceof FirebaseAuthInvalidUserException) return "Usuário não cadastrado.";
        return "Erro: " + e.getLocalizedMessage();
    }
}