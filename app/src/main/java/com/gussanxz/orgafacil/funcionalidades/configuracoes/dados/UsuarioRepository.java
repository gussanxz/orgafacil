package com.gussanxz.orgafacil.funcionalidades.configuracoes.dados;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.negocio.modelos.Usuario;

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
    public void salvarPerfil(Usuario usuario) {
        String uid = usuario.getIdUsuario();
        if (uid == null) return;

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", usuario.getNome() != null ? usuario.getNome() : "Novo Usuário");
        perfil.put("email", usuario.getEmail());
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
    public void atualizarPerfil(Usuario usuario) {
        String uid = usuario.getIdUsuario();
        if (uid == null) return;

        Map<String, Object> patch = new HashMap<>();
        if (usuario.getNome() != null) patch.put("nome", usuario.getNome());
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
}