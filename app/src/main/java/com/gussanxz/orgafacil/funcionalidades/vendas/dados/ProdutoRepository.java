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
    private final CategoriaCatalogoRepository categoriaRepository;

    // Nome da coleção centralizado

    public ProdutoRepository() {
        this.categoriaRepository = new CategoriaCatalogoRepository();
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
            normalizarCategoriaProduto(produtoModel);

            boolean categoriaPadrao = CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO
                    .equals(produtoModel.getCategoria());

            if (categoriaPadrao) {
                categoriaRepository.garantirCategoriaPadrao(new CategoriaCatalogoRepository.Callback() {
                    @Override
                    public void onSucesso(String mensagem) {
                        salvarProdutoNoFirestore(produtoModel, callback);
                    }

                    @Override
                    public void onErro(String erro) {
                        callback.onErro(erro);
                    }
                });
            } else {
                salvarProdutoNoFirestore(produtoModel, callback);
            }

        } catch (IllegalStateException e) {
            callback.onErro("Sessão expirada: " + e.getMessage());
        }
    }

    private void salvarProdutoNoFirestore(@NonNull ProdutoModel produtoModel, @NonNull Callback callback) {
        boolean isEdicao = (produtoModel.getId() != null && !produtoModel.getId().isEmpty());
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.vendasProdutoDoc(produtoModel.getId());
        } else {
            docRef = FirestoreSchema.vendasProdutosCol().document();
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
     * Listener em tempo real para listagem de produtos.
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasProdutosCol()
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

    private void normalizarCategoriaProduto(@NonNull ProdutoModel produtoModel) {
        String categoria = produtoModel.getCategoria();

        if (categoria == null || categoria.trim().isEmpty()) {
            produtoModel.setCategoria(CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);
        } else {
            produtoModel.setCategoria(categoria.trim());
        }
    }
}