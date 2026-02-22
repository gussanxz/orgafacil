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

/**
 * VendasRepository
 *
 * O que faz:
 * - Centraliza o módulo Vendas:
 *   categorias, catálogo, caixa, vendas e pessoas.
 *
 * Impacto:
 * - Performance: evita "joins" (Firestore não tem join), usando snapshot na venda.
 * - Organização: regras comerciais isoladas do módulo Contas e de Config.
 * - Custo: queries previsíveis (por dia/mês, por caixa, por cliente).
 */
public class VendasRepository {

    // =========================
    // CATEGORIAS (vendas/categorias)
    // =========================

    public void listarCategoriasAtivas(@NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasCategoriasCol()
                .whereEqualTo("statusAtivo", true)
                .orderBy("ordem", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void salvarCategoria(@NonNull String categoriaId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.vendasCategoriaDoc(categoriaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // CATALOGO (vendas/catalogoVendas)
    // =========================

    public void listarProdutosAtivos(@NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasCatalogoCol()
                .whereEqualTo("statusAtivo", true)
                .orderBy("nome", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void salvarProduto(@NonNull String produtoId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        if (!data.containsKey("createdAt")) data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        FirestoreSchema.vendasProdutoDoc(produtoId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void atualizarEstoque(@NonNull String produtoId, int estoqueNovo, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("estoque", estoqueNovo);
        patch.put("updatedAt", Timestamp.now());

        FirestoreSchema.vendasProdutoDoc(produtoId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // CAIXA (vendas/caixa)
    // =========================

    public void abrirCaixa(@NonNull String caixaId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        if (!data.containsKey("abertoEm")) data.put("abertoEm", Timestamp.now());
        if (!data.containsKey("status")) data.put("status", "ABERTO");
        if (!data.containsKey("diaKey")) data.put("diaKey", FirestoreSchema.diaKey(new Date()));
        if (!data.containsKey("mesKey")) data.put("mesKey", FirestoreSchema.mesKey(new Date()));

        FirestoreSchema.vendasCaixaDoc(caixaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void fecharCaixa(@NonNull String caixaId, @NonNull Map<String, Object> patch, @NonNull RepoVoidCallback cb) {
        patch.put("fechadoEm", Timestamp.now());
        patch.put("status", "FECHADO");

        FirestoreSchema.vendasCaixaDoc(caixaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // VENDAS (vendas/Vendas) - atenção ao nome "Vendas" com V maiúsculo
    // =========================

    /**
     * Cria uma venda com snapshot de itens.
     *
     * Impacto:
     * - Snapshot garante consistência histórica (preço/nome não mudam na venda).
     * - UI fica rápida: ler 1 doc já tem tudo.
     */
    public void salvarVenda(@NonNull String vendaId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        if (!data.containsKey("createdAt")) data.put("createdAt", Timestamp.now());
        if (!data.containsKey("diaKey")) data.put("diaKey", FirestoreSchema.diaKey(new Date()));
        if (!data.containsKey("mesKey")) data.put("mesKey", FirestoreSchema.mesKey(new Date()));

        FirestoreSchema.vendaDoc(vendaId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasDoDia(@NonNull String diaKey, int limit, @NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasVendasCol()
                .whereEqualTo("diaKey", diaKey)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasPorCaixa(@NonNull String caixaId, int limit, @NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasVendasCol()
                .whereEqualTo("caixaId", caixaId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void listarVendasPorCliente(@NonNull String clienteId, int limit, @NonNull RepoCallback<QuerySnapshot> cb) {
        FirestoreSchema.vendasVendasCol()
                .whereEqualTo("clienteId", clienteId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void atualizarStatusVenda(@NonNull String vendaId, @NonNull String status, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("status", status);

        FirestoreSchema.vendaDoc(vendaId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    // =========================
    // PESSOAS (clientes / vendedores / fornecedores)
    // =========================

    public void salvarCliente(@NonNull String clienteId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.clienteDoc(clienteId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void atualizarUltimaCompraCliente(@NonNull String clienteId, @NonNull Timestamp ultimaCompraEm, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("ultimaCompraEm", ultimaCompraEm);

        FirestoreSchema.clienteDoc(clienteId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void salvarVendedor(@NonNull String vendedorId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.vendedorDoc(vendedorId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void atualizarUltimaVendaVendedor(@NonNull String vendedorId, @NonNull Timestamp ultimaVenda, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("ultimaVenda", ultimaVenda);

        FirestoreSchema.vendedorDoc(vendedorId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void salvarFornecedor(@NonNull String fornecedorId, @NonNull Map<String, Object> data, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.fornecedorDoc(fornecedorId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}
