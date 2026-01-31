package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoCallback;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoVoidCallback;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VendasRepository {

    private final String uid;

    public VendasRepository(@NonNull String uid) {
        this.uid = uid;
    }

    // =========================
    // CATEGORIAS
    // =========================

    public void listarCategoriasAtivas(@NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasCategoriasCol(uid)
                .whereEqualTo("statusAtivo", true)
                .orderBy("ordem", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void salvarCategoria(
            @NonNull String categoriaId,
            @NonNull Map<String, Object> data,
            @NonNull RepoVoidCallback cb
    ) {
        FirestoreSchema.vendasCategoriaDoc(uid, categoriaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // CATÁLOGO
    // =========================

    public void listarProdutosAtivos(@NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasCatalogoCol(uid)
                .whereEqualTo("statusAtivo", true)
                .orderBy("nome", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void salvarProduto(
            @NonNull String produtoId,
            @NonNull Map<String, Object> data,
            @NonNull RepoVoidCallback cb
    ) {
        if (!data.containsKey("createdAt")) {
            data.put("createdAt", Timestamp.now());
        }
        data.put("updatedAt", Timestamp.now());

        FirestoreSchema.vendasProdutoDoc(uid, produtoId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void atualizarEstoque(
            @NonNull String produtoId,
            int estoqueNovo,
            @NonNull RepoVoidCallback cb
    ) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("estoque", estoqueNovo);
        patch.put("updatedAt", Timestamp.now());

        FirestoreSchema.vendasProdutoDoc(uid, produtoId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // CAIXA
    // =========================

    public void abrirCaixa(
            @NonNull String caixaId,
            @NonNull Map<String, Object> data,
            @NonNull RepoVoidCallback cb
    ) {
        data.putIfAbsent("abertoEm", Timestamp.now());
        data.putIfAbsent("status", "ABERTO");
        data.putIfAbsent("diaKey", FirestoreSchema.diaKey(new Date()));
        data.putIfAbsent("mesKey", FirestoreSchema.mesKey(new Date()));

        FirestoreSchema.vendasCaixaDoc(uid, caixaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void fecharCaixa(
            @NonNull String caixaId,
            @NonNull Map<String, Object> patch,
            @NonNull RepoVoidCallback cb
    ) {
        patch.put("fechadoEm", Timestamp.now());
        patch.put("status", "FECHADO");

        FirestoreSchema.vendasCaixaDoc(uid, caixaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // VENDAS
    // =========================

    public void salvarVenda(
            @NonNull String vendaId,
            @NonNull Map<String, Object> data,
            @NonNull RepoVoidCallback cb
    ) {
        data.putIfAbsent("createdAt", Timestamp.now());
        data.putIfAbsent("diaKey", FirestoreSchema.diaKey(new Date()));
        data.putIfAbsent("mesKey", FirestoreSchema.mesKey(new Date()));

        FirestoreSchema.vendaDoc(uid, vendaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasDoDia(
            @NonNull String diaKey,
            int limit,
            @NonNull RepoCallback<QuerySnapshot> cb
    ) {
        FirestoreSchema.vendasVendasCol(uid)
                .whereEqualTo("diaKey", diaKey)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasPorCaixa(
            @NonNull String caixaId,
            int limit,
            @NonNull RepoCallback<QuerySnapshot> cb
    ) {
        FirestoreSchema.vendasVendasCol(uid)
                .whereEqualTo("caixaId", caixaId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasPorCliente(
            @NonNull String clienteId,
            int limit,
            @NonNull RepoCallback<QuerySnapshot> cb
    ) {
        FirestoreSchema.vendasVendasCol(uid)
                .whereEqualTo("clienteId", clienteId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void atualizarStatusVenda(
            @NonNull String vendaId,
            @NonNull String status,
            @NonNull RepoVoidCallback cb
    ) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", status);

        FirestoreSchema.vendaDoc(uid, vendaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // PESSOAS
    // =========================

    public void salvarCliente(@NonNull String clienteId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.clienteDoc(uid, clienteId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void salvarVendedor(@NonNull String vendedorId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.vendedorDoc(uid, vendedorId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void salvarFornecedor(@NonNull String fornecedorId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.fornecedorDoc(uid, fornecedorId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}
