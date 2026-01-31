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
 * CLASSE REPOSITORY
 * Responsável por toda a manipulação de dados do Usuário no Firestore.
 * Se você mudar de banco no futuro, só precisará mexer aqui.
 */
public class UsuarioRepository {

    private static final String TAG = "UsuarioRepository";

    /**
     * SALVAR PERFIL
     * Cria ou sobrepõe o documento de perfil no schema novo: usuarios/{uid}/config/perfil.
     */
    public void salvarPerfil(Usuario usuario) {
        String uid = usuario.getIdUsuario();
        if (uid == null || uid.trim().isEmpty()) {
            Log.e(TAG, "Erro: Tentativa de salvar perfil sem UID.");
            return;
        }

        // Criamos um Map para enviar apenas o que queremos ao Firestore
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", usuario.getNome() != null ? usuario.getNome() : "Novo Usuário");
        perfil.put("email", usuario.getEmail());
        perfil.put("updatedAt", FieldValue.serverTimestamp()); // Marca a hora da alteração no servidor

        // Usa o centralizador de paths FirestoreSchema para manter o padrão
        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .set(perfil, SetOptions.merge()) // Merge evita apagar campos que já existiam
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao salvar perfil no Firestore", e));
    }

    /**
     * ATUALIZAR PERFIL
     * Usa o método 'update' para alterar apenas campos específicos sem risco de recriar o doc vazio.
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

    /**
     * SEED DE CATEGORIAS
     * Cria automaticamente a estrutura de categorias para um novo usuário.
     * Local: usuarios/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
     */
    public void inicializarCategoriasPadrao(String uid) {
        List<String> categorias = Arrays.asList(
                "Alimentação", "Aluguel", "Lazer", "Mercado", "Educação", "Saúde"
        );

        FirebaseFirestore fs = FirestoreSchema.db();
        WriteBatch batch = fs.batch(); // Batch permite gravar vários documentos de uma vez só

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

        // Executa todas as gravações do loop em uma única transação
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Categorias iniciais criadas!"))
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao criar categorias iniciais", e));
    }

    /**
     * UTILITÁRIO DE SLUG
     * Transforma "Educação Financeira" em "educacao_financeira" para ser usado como ID no Firestore.
     */
    private String gerarSlug(String input) {
        if (input == null) return "";
        // Remove acentos e caracteres especiais
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_"); // Troca espaços por underline
    }

    /**
     * Envia e-mail de redefinição de senha.
     */
    public void enviarEmailRecuperacao(String email, OnCompleteListener<Void> listener) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }

    /**
     * Exclui os dados do usuário no Firestore e depois a conta no Auth.
     * Nota: O Firebase exige login recente para deletar conta.
     */
    public void excluirContaCompleta(FirebaseUser user, OnCompleteListener<Void> listener) {
        String uid = user.getUid();

        // 1. Remove os documentos do Firestore primeiro
        // Aqui você pode adicionar lógica para deletar subcoleções se necessário
        FirestoreSchema.userDoc(uid).delete().addOnSuccessListener(unused -> {
            // 2. Após limpar o banco, deleta a conta de autenticação
            user.delete().addOnCompleteListener(listener);
        });
    }

    /**
     * Finaliza a sessão do usuário no Firebase e limpa dados sensíveis.
     */
    public void deslogar() {
        FirebaseAuth.getInstance().signOut();
        // No futuro, adicione aqui: Room.clearAllTables(), etc.
    }

    /**
     * Realiza a troca de senha exigindo reautenticação prévia por segurança.
     */
    public void alterarSenhaComReautenticacao(FirebaseUser user, String senhaAtual, String novaSenha, OnCompleteListener<Void> listener) {
        if (user == null || user.getEmail() == null) return;

        // Cria a credencial com o e-mail atual e a senha antiga fornecida
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), senhaAtual);

        // 1. Reautentica o usuário (exigência do Firebase para troca de senha)
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                // 2. Se a senha atual estiver correta, atualiza para a nova
                user.updatePassword(novaSenha).addOnCompleteListener(listener);
            } else {
                // Se falhar (senha atual errada), repassa a exceção para a Activity
                listener.onComplete(reauthTask);
            }
        });
    }
}