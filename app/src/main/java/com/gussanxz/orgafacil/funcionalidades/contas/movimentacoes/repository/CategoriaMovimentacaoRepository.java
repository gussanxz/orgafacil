package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository para Categoria de Movimentações.
 * Gerencia as categorias (Alimentação, Salário, etc) organizadas por mapas.
 */
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
     * Lista categorias ativas filtradas por Tipo (Receita/Despesa).
     * [CLEAN CODE]: Agora aceita o Enum para evitar buscas erradas.
     */
    public void listarAtivasPorTipo(@NonNull TipoCategoriaContas tipo, @NonNull RepoQueryCallback cb) {
        FirestoreSchema.contasCategoriasCol()
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                // Filtra pelo ID do Enum (1 ou 2) para categorias
                .whereEqualTo("tipo", tipo.getId())
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    /**
     * Lista TODAS as categorias ativas (sem filtro de tipo).
     */
    public void listarAtivas(@NonNull RepoQueryCallback cb) {
        FirestoreSchema.contasCategoriasCol()
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    /**
     * Salva ou Edita usando o Modelo.
     * [CLEAN CODE]: Usar o objeto garante que a estrutura de Mapas seja respeitada.
     */
    public void salvar(@NonNull ContasCategoriaModel categoria, @NonNull RepoVoidCallback cb) {

        // Se não tiver ID, gera um novo
        String id = (categoria.getId() == null || categoria.getId().isEmpty())
                ? FirestoreSchema.contasCategoriasCol().document().getId()
                : categoria.getId();

        categoria.setId(id);

        FirestoreSchema.contasCategoriaDoc(id)
                .set(categoria, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    /**
     * Soft Delete (apenas desativa).
     */
    public void desativar(@NonNull String categoriaId, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put(ContasCategoriaModel.CAMPO_ATIVA, false);

        FirestoreSchema.contasCategoriaDoc(categoriaId)
                .update(patch) // Update é mais limpo que Set(merge) para um único campo
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}