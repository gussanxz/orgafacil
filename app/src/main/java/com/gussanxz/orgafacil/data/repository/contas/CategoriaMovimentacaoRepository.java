package com.gussanxz.orgafacil.data.repository.financeiro;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.data.config.FirestoreSchema;

import java.util.HashMap;
import java.util.Map;

public class CategoriaMovimentacaoRepository {

    public interface RepoVoidCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface RepoQueryCallback {
        void onSuccess(QuerySnapshot result);
        void onError(Exception e);
    }

    /**
     * Lista categorias financeiras ativas.
     * Caminho definido em FirestoreSchema.contasCategoriasCol()
     */
    public void listarAtivas(@NonNull RepoQueryCallback cb) {
        FirestoreSchema.contasCategoriasCol()
                .whereEqualTo("ativo", true)
                .orderBy("ordem", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    /**
     * Salva ou Edita categoria financeira.
     * Regra: Usa MERGE para não perder campos extras e MAP para flexibilidade.
     */
    public void salvar(@NonNull String categoriaId,
                       @NonNull Map<String, Object> data,
                       @NonNull RepoVoidCallback cb) {

        // Garante timestamp se for novo
        if (!data.containsKey("createdAt")) {
            data.put("createdAt", FieldValue.serverTimestamp());
        }

        FirestoreSchema.contasCategoriaDoc(categoriaId)
                .set(data, SetOptions.merge()) // O segredo do financeiro está aqui
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    /**
     * Soft Delete (apenas desativa).
     */
    public void desativar(@NonNull String categoriaId, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("ativo", false);

        FirestoreSchema.contasCategoriaDoc(categoriaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}