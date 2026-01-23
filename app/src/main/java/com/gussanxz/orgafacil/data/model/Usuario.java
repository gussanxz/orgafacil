package com.gussanxz.orgafacil.data.model;

import com.gussanxz.orgafacil.data.config.ConfiguracaoFirestore;
import android.util.Log;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.text.Normalizer;

public class Usuario {

    private String idUsuario;
    private String nome;
    private String email;
    private String senha;

    // Esses campos servem apenas para leitura ou uso local.
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    // Construtor vazio para o Firestore
    public Usuario() {
    }

    // Este metodo deve ser usado para cadastrar ou salvar dados iniciais.
    public void salvar() {

        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();
        String uid = getIdUsuario();

        if (uid == null || uid.trim().isEmpty()) {
            Log.e("FireStore", "Usuario.salvar(): uid vazio");
            return;
        }

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("nome", getNome());
        userDoc.put("email", getEmail());
        userDoc.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .set(userDoc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao salvar perfil", e));
    }

    // --- NOVO MÉTODO ADICIONADO ---
    // Usado especificamente para a tela de Perfil.
    // Ele usa .update() em vez de .set(), o que falha se o documento não existir (segurança).
    public void atualizar() {
        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();
        String uid = getIdUsuario();

        if (uid == null || uid.trim().isEmpty()) {
            return;
        }

        // Cria um mapa com APENAS o que queremos mudar (neste caso, o nome)
        Map<String, Object> dadosAtualizar = new HashMap<>();

        if (getNome() != null) {
            dadosAtualizar.put("nome", getNome());
        }

        // Se precisar atualizar outros campos no futuro, adicione aqui.
        // Não atualizamos email aqui pois o email é chave de autenticação.

        dadosAtualizar.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users")
                .document(uid)
                .update(dadosAtualizar) // .update altera só o que enviamos
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Usuário atualizado com sucesso"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao atualizar usuário", e));
    }

    public void inicializarNovosDados() {

        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();
        String uid = getIdUsuario();

        if (uid == null) return;

        Map<String, Object> resumo = new HashMap<>();
        resumo.put("proventosTotal", 0.0);
        resumo.put("despesaTotal", 0.0);
        resumo.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .set(resumo, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao iniciar conta", e));

        seedCategoriasPadrao(fs, uid);
    }

    private void seedCategoriasPadrao(FirebaseFirestore fs, String uid) {

        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        WriteBatch batch = fs.batch();

        for (String nome : padroes) {
            String catId = gerarSlug(nome);
            Log.d("FireStore", "Categoria padrão setadas pro usuário");

            DocumentReference ref = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("categorias").document(catId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FireStore", "Seed categorias OK"))
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
    public Double getProventosTotal() {
        return proventosTotal;
    }

    public void setProventosTotal(Double proventosTotal) {
        this.proventosTotal = proventosTotal;
    }

    public Double getDespesaTotal() {
        return despesaTotal;
    }

    public void setDespesaTotal(Double despesaTotal) {
        this.despesaTotal = despesaTotal;
    }

    @Exclude
    public String getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }


    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @Exclude
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
}