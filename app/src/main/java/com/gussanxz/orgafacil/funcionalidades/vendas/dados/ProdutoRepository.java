package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ProdutoRepository
 *
 * Responsabilidade:
 * - CRUD e Listener em tempo real para "Produtos" no módulo de vendas.
 * - Utiliza FirebaseSession para identidade e FirestoreSchema para caminhos. [cite: 2025-11-10]
 */
public class ProdutoRepository {

    // Nome da coleção centralizado
    private static final String COL_VENDAS_PRODUTOS = "vendas_produtos";

    public ProdutoRepository() {
        // O repositório agora é "stateless" em relação ao UID. [cite: 2025-11-10]
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<ProdutoModel> lista);
        void onErro(String erro);
    }

    /**
     * Salvar ou Editar produto.
     * Utiliza o myUserDoc() do Schema que já resolve o UID internamente. [cite: 2025-11-10]
     */
    public void salvar(@NonNull ProdutoModel produtoModel, @NonNull Callback callback) {
        try {
            boolean isEdicao = (produtoModel.getId() != null && !produtoModel.getId().isEmpty());
            DocumentReference docRef;

            if (isEdicao) {
                docRef = FirestoreSchema.myUserDoc()
                        .collection(COL_VENDAS_PRODUTOS)
                        .document(produtoModel.getId());
            } else {
                docRef = FirestoreSchema.myUserDoc()
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

        } catch (IllegalStateException e) {
            // Tratamento de erro caso o usuário perca a sessão. [cite: 2025-11-10]
            callback.onErro("Sessão expirada: " + e.getMessage());
        }
    }

    /**
     * Listener em tempo real para listagem de produtos.
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.myUserDoc()
                    .collection(COL_VENDAS_PRODUTOS)
                    .orderBy("nome", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro ao listar produtos");
                            return;
                        }

                        List<ProdutoModel> lista = new ArrayList<>();
                        if (snapshots != null) {
                            lista = snapshots.toObjects(ProdutoModel.class);

                            // Sincroniza os IDs dos documentos com os objetos da lista
                            for (int i = 0; i < snapshots.size(); i++) {
                                if (i < lista.size()) {
                                    lista.get(i).setId(snapshots.getDocuments().get(i).getId());
                                }
                            }
                        }

                        callback.onNovosDados(lista);
                    });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }
}