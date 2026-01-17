package com.gussanxz.orgafacil.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Usuario {

    private String idUsuario;
    private String nome;
    private String email;
    private String senha;
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    public Usuario() {
    }

    public void salvar() {
        FirebaseFirestore fs = ConfiguracaoFirebase.getFirestore();

        String uid = getIdUsuario();
        if (uid == null || uid.trim().isEmpty()) {
            Log.e("FS", "Usuario.salvar(): idUsuario vazio");
            return;
        }

        // 1) users/{uid} com perfil
        Map<String, Object> userDoc = new HashMap<>();
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", getNome());
        perfil.put("email", getEmail());
        userDoc.put("perfil", perfil);
        userDoc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .set(userDoc, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FS", "Erro users/{uid}", e));

        // 2) contas/main
        Map<String, Object> resumo = new HashMap<>();
        resumo.put("proventosTotal", 0.0);
        resumo.put("despesaTotal", 0.0);
        resumo.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .set(resumo, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FS", "Erro contas/main", e));

        // 3) Seed categorias padrão: users/{uid}/contas/categorias/{catId}
        seedCategoriasPadrao(fs, uid);
    }

    private void seedCategoriasPadrao(FirebaseFirestore fs, String uid) {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        WriteBatch batch = fs.batch();

        for (String nome : padroes) {
            // docId estável baseado no nome (evita duplicar seed)
            String catId = nome.toLowerCase(Locale.ROOT)
                    .replace("ç", "c")
                    .replace("ã", "a")
                    .replace("á", "a")
                    .replace("à", "a")
                    .replace("â", "a")
                    .replace("é", "e")
                    .replace("ê", "e")
                    .replace("í", "i")
                    .replace("ó", "o")
                    .replace("ô", "o")
                    .replace("ú", "u")
                    .replace(" ", "_");

            DocumentReference ref = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("categorias").document(catId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FS", "Seed categorias OK"))
                .addOnFailureListener(e -> Log.e("FS", "Erro seed categorias", e));
    }

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
