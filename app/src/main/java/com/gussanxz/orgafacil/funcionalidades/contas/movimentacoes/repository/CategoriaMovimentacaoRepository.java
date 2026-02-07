package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel; // Importante
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

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
     * [ATUALIZADO]: Usa as constantes do Model para garantir que busca em "visual.nome" e "ativa".
     */
    public void listarAtivas(@NonNull RepoQueryCallback cb) {
        FirestoreSchema.contasCategoriasCol()
                // CAMPO_ATIVA = "ativa" (e não mais "ativo")
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                // CAMPO_NOME = "visual.nome" (Ordenação alfabética por padrão)
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    /**
     * Salva ou Edita categoria financeira.
     * [NOTA]: Ao passar um Map aqui, certifique-se que as chaves seguem a Dot Notation
     * (ex: "visual.nome", "financeiro.limiteMensal") se for usar campos aninhados.
     */
    public void salvar(@NonNull String categoriaId,
                       @NonNull Map<String, Object> data,
                       @NonNull RepoVoidCallback cb) {

        // Garante timestamp se for novo (Mantido como campo de auditoria na raiz)
        if (!data.containsKey("createdAt")) {
            data.put("createdAt", FieldValue.serverTimestamp());
        }

        FirestoreSchema.contasCategoriaDoc(categoriaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    /**
     * Soft Delete (apenas desativa).
     * [ATUALIZADO]: Usa a constante correta para desativar.
     */
    public void desativar(@NonNull String categoriaId, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();

        // Atualiza o campo "ativa" para false
        patch.put(ContasCategoriaModel.CAMPO_ATIVA, false);

        FirestoreSchema.contasCategoriaDoc(categoriaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}