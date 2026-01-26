package com.gussanxz.orgafacil.data.model;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.Exclude;

import com.gussanxz.orgafacil.data.config.FirestoreSchema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.text.Normalizer;

/**
 * Usuario
 *
 * O que faz:
 * - Representa dados básicos do usuário (nome/email/senha local).
 * - Inicializa estrutura mínima no Firestore conforme schema NOVO:
 *   usuarios/{uid}/config/perfil
 *   usuarios/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
 *
 * Impacto:
 * - Ao centralizar o path via FirestoreSchema, trocar ROOT muda o banco inteiro.
 * - Evita "users" hardcoded e evita manter dados em esquema antigo.
 */
public class Usuario {

    private String idUsuario;
    private String nome;
    private String email;
    private String senha;

    // Campos de uso local (não persistem como "verdade" do schema novo)
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    public Usuario() {}

    /**
     * Salva / atualiza o perfil no schema novo:
     * usuarios/{uid}/config/perfil
     */
    public void salvar() {
        String uid = getIdUsuario();
        if (uid == null || uid.trim().isEmpty()) {
            Log.e("FireStore", "Usuario.salvar(): uid vazio");
            return;
        }

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", getNome() != null ? getNome() : "Usuário");
        perfil.put("email", getEmail());
        perfil.put("dataCriação", FieldValue.serverTimestamp()); // só será setado se não existir
        perfil.put("updatedAt", FieldValue.serverTimestamp());

        // /usuarios/{uid}/config/perfil
        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .set(perfil, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao salvar perfil", e));
    }

    /**
     * Atualiza campos do perfil usando update (não cria doc do nada).
     * Se o doc não existir, update falha -> isso é bom para integridade.
     */
    public void atualizar() {
        String uid = getIdUsuario();
        if (uid == null || uid.trim().isEmpty()) return;

        Map<String, Object> patch = new HashMap<>();
        if (getNome() != null) patch.put("nome", getNome());
        patch.put("updatedAt", FieldValue.serverTimestamp());

        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.CONFIG)
                .document(FirestoreSchema.PERFIL)
                .update(patch)
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Perfil atualizado com sucesso"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao atualizar perfil", e));
    }

    /**
     * Inicializa dados operacionais no schema novo.
     *
     * Hoje: seed de categorias padrão em:
     * usuarios/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
     *
     * (você pode adicionar também criação de /Resumos/ultimos vazio no futuro)
     */
    public void inicializarNovosDados() {
        String uid = getIdUsuario();
        if (uid == null || uid.trim().isEmpty()) return;

        seedCategoriasPadrao(uid);
    }

    private void seedCategoriasPadrao(String uid) {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        FirebaseFirestore fs = FirestoreSchema.db();
        WriteBatch batch = fs.batch();

        int ordem = 0;
        for (String nome : padroes) {
            String catId = gerarSlug(nome);

            // usuarios/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
            DocumentReference ref = FirestoreSchema.userDoc(uid)
                    .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                    .collection(FirestoreSchema.CONTAS_CATEGORIAS).document(catId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("tipo", "Despesa");  // padrão (ajuste se quiser separar)
            doc.put("ativo", true);
            doc.put("ordem", ordem++);
            doc.put("createdAt", FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FireStore", "Seed categorias OK (schema novo)"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro seed categorias", e));
    }

    private String gerarSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_");
    }

    // --- GETTERS E SETTERS ---

    public Double getProventosTotal() { return proventosTotal; }
    public void setProventosTotal(Double proventosTotal) { this.proventosTotal = proventosTotal; }

    public Double getDespesaTotal() { return despesaTotal; }
    public void setDespesaTotal(Double despesaTotal) { this.despesaTotal = despesaTotal; }

    @Exclude
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Exclude
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}