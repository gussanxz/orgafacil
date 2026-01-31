package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;

/**
 * ProdutoRepository
 *
 * O que faz:
 * - CRUD + Listener de produtos do módulo vendas mantendo o caminho atual:
 *   {ROOT}/{uid}/vendas_produtos/{produtoId}
 *
 * Impacto:
 * - Agora respeita FirestoreSchema.ROOT (ex: "usuarios" / "teste")
 * - Remove hardcode "users" em todos os paths
 * - Mantém compatibilidade com telas atuais (não migra ainda o schema)
 */
public class ProdutoRepository {

    private final FirebaseFirestore db;
    private final String uid;

    // Mantém o nome da coleção atual para não quebrar o app agora
    private static final String COL_VENDAS_PRODUTOS = "vendas_produtos";

    public ProdutoRepository() {
        this.db = FirestoreSchema.db();
        this.uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(java.util.List<ProdutoModel> lista);
        void onErro(String erro);
    }

    /**
     * Salvar/Editar produto
     *
     * Caminho:
     * {ROOT}/{uid}/vendas_produtos/{id}
     */
    public void salvar(@NonNull ProdutoModel produtoModel, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        boolean isEdicao = (produtoModel.getId() != null && !produtoModel.getId().isEmpty());
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection(COL_VENDAS_PRODUTOS)
                    .document(produtoModel.getId());
        } else {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection(COL_VENDAS_PRODUTOS)
                    .document();
            produtoModel.setId(docRef.getId());
        }

        docRef.set(produtoModel)
                .addOnSuccessListener(aVoid ->
                        callback.onSucesso(isEdicao ? "Produto atualizado!" : "Produto salvo!")
                )
                .addOnFailureListener(e ->
                        callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao salvar produto")
                );
    }

    /**
     * Listener em tempo real (lista de produtos)
     *
     * Impacto:
     * - Bom para UI de catálogo
     * - Cada alteração gera evento (custo: leituras por mudança)
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        if (uid == null) return null;

        return FirestoreSchema.userDoc(uid)
                .collection(COL_VENDAS_PRODUTOS)
                .orderBy("nome")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro ao listar produtos");
                        return;
                    }

                    java.util.List<ProdutoModel> lista = new java.util.ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(ProdutoModel.class);

                        // Garante IDs (toObjects pode não preencher id)
                        int i = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            if (i < lista.size()) lista.get(i).setId(doc.getId());
                            i++;
                        }
                    }

                    callback.onNovosDados(lista);
                });
    }
}