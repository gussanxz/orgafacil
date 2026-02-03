package com.gussanxz.orgafacil.funcionalidades.contas.comum.dados;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.CategoriaIdHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriaContasRepository {

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    public Task<QuerySnapshot> listarAtivas() {
        return FirestoreSchema.contasCategoriasCol()
                .whereEqualTo("ativo", true)
                .get();
    }

    public void salvar(String nome, Callback callback) {
        String catId = CategoriaIdHelper.slugify(nome);
        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("tipo", "Despesa");
        doc.put("ativo", true);
        doc.put("ordem", 0);
        doc.put("createdAt", FieldValue.serverTimestamp());

        FirestoreSchema.contasCategoriaDoc(catId)
                .set(doc, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void verificarEExcluir(String nome, Callback callback) {
        // Verifica se existe movimentação usando essa categoria antes de deletar
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("categoriaNome", nome)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        callback.onErro("Categoria em uso — não pode ser excluída.");
                    } else {
                        excluir(nome, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao verificar uso: " + e.getMessage()));
    }

    private void excluir(String nome, Callback callback) {
        FirestoreSchema.contasCategoriaDoc(CategoriaIdHelper.slugify(nome))
                .delete()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void inicializarPadroes(Callback callback) {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        final int total = padroes.size();
        final int[] ok = {0};

        for (String nome : padroes) {
            salvar(nome, new Callback() {
                @Override
                public void onSucesso() {
                    ok[0]++;
                    if (ok[0] == total) callback.onSucesso();
                }

                @Override
                public void onErro(String erro) {
                    ok[0]++;
                    if (ok[0] == total) callback.onSucesso(); // Continua mesmo com erro individual
                }
            });
        }
    }
}