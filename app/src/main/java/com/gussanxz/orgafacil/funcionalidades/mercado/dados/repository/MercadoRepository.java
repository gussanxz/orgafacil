package com.gussanxz.orgafacil.funcionalidades.mercado.dados.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSmartBatch;
import com.gussanxz.orgafacil.funcionalidades.mercado.dados.model.ItemMercadoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MercadoRepository
 *
 * Camada de acesso ao Firestore para o módulo de Lista de Mercado.
 * Segue o padrão de repositórios já estabelecido no projeto.
 *
 * Estrutura Firestore:
 *   ROOT/{uid}/moduloSistema/mercado/
 *     mercado_listas/{listaId}/
 *       mercado_itens/{itemId}
 *     mercado_memoria_precos/{nomeProdutoKey}
 */
public class MercadoRepository {

    // ─── ID fixo da lista ativa (MVP 1.0 = lista única por usuário) ──────────
    public static final String LISTA_ATIVA_ID = "lista_ativa";

    // ─── Campos Firestore ─────────────────────────────────────────────────────
    private static final String CAMPO_NOME            = "nome";
    private static final String CAMPO_CATEGORIA       = "categoria";
    private static final String CAMPO_VALOR_CENTAVOS  = "valorCentavos";
    private static final String CAMPO_QUANTIDADE      = "quantidade";
    private static final String CAMPO_NO_CARRINHO     = "noCarrinho";
    private static final String CAMPO_CRIADO_EM       = "criadoEm";
    private static final String CAMPO_ATUALIZADO_EM   = "atualizadoEm";
    private static final String CAMPO_ULTIMO_PRECO    = "ultimoValorCentavos";
    private static final String CAMPO_ULTIMO_NOME     = "nome";

    // ─── Callbacks genéricos ──────────────────────────────────────────────────

    public interface Callback {
        void onSucesso();
        void onErro(String mensagem);
    }

    public interface CallbackLista {
        void onSucesso(List<ItemMercadoModel> itens);
        void onErro(String mensagem);
    }

    public interface CallbackId {
        void onSucesso(String firestoreId);
        void onErro(String mensagem);
    }

    public interface CallbackPreco {
        void onSucesso(int valorCentavos);   // RN01 – centavos
        void onNaoEncontrado();
        void onErro(String mensagem);
    }

    // ─── Singleton leve ───────────────────────────────────────────────────────
    private static MercadoRepository instance;

    public static MercadoRepository getInstance() {
        if (instance == null) instance = new MercadoRepository();
        return instance;
    }

    private MercadoRepository() {}

    // ─── CRUD de Itens ────────────────────────────────────────────────────────

    /**
     * RF01 – Carrega todos os itens da lista ativa, ordenados por data de criação.
     * RN03 – Firestore com cache offline habilitado por padrão (funciona sem rede).
     */
    public void carregarItens(@NonNull CallbackLista callback) {
        FirestoreSchema.mercadoItensCol(LISTA_ATIVA_ID)
                .orderBy(CAMPO_CRIADO_EM, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ItemMercadoModel> itens = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ItemMercadoModel item = documentParaItem(doc);
                        if (item != null) itens.add(item);
                    }
                    callback.onSucesso(itens);
                })
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao carregar itens."));
    }

    /**
     * RF01 – Adiciona um novo item à lista ativa.
     * RF06 – Após salvar, atualiza a memória de preços com o valor informado.
     * CT04 / CT05 – Validações feitas na Activity antes de chegar aqui.
     */
    public void adicionarItem(@NonNull ItemMercadoModel item, @NonNull CallbackId callback) {
        FirestoreSchema.mercadoItensCol(LISTA_ATIVA_ID)
                .add(item.toMap())
                .addOnSuccessListener(docRef -> {
                    item.setFirestoreId(docRef.getId());
                    // RF06 – salva memória de preços em paralelo (não bloqueia o callback)
                    salvarMemoriaPreco(item.getNome(), item.getValorCentavos());
                    callback.onSucesso(docRef.getId());
                })
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao adicionar item."));
    }

    /**
     * RF01 – Atualiza valor e/ou quantidade de um item.
     * RF06 – Atualiza memória de preços se o valor mudou.
     */
    public void atualizarValorEQuantidade(@NonNull ItemMercadoModel item, @NonNull Callback callback) {
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) {
            callback.onErro("Item sem ID — não é possível atualizar.");
            return;
        }

        FirestoreSchema.mercadoItemDoc(LISTA_ATIVA_ID, item.getFirestoreId())
                .update(item.toMapValorQtd())
                .addOnSuccessListener(v -> {
                    // RF06 – atualiza memória de preços após edição de valor
                    salvarMemoriaPreco(item.getNome(), item.getValorCentavos());
                    callback.onSucesso();
                })
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao atualizar item."));
    }

    /**
     * RF02 – Atualiza apenas o status "no carrinho" de um item.
     * Usa mapa parcial para não sobrescrever outros campos.
     */
    public void atualizarCarrinho(@NonNull ItemMercadoModel item, @NonNull Callback callback) {
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) {
            callback.onErro("Item sem ID — não é possível atualizar carrinho.");
            return;
        }

        FirestoreSchema.mercadoItemDoc(LISTA_ATIVA_ID, item.getFirestoreId())
                .update(item.toMapCarrinho())
                .addOnSuccessListener(v -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao atualizar carrinho."));
    }

    /**
     * RF05 – Atualiza apenas a categoria de um item.
     */
    public void atualizarCategoria(@NonNull ItemMercadoModel item,
                                   @Nullable Callback callback) {
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) return;
        Map<String, Object> map = new HashMap<>();
        map.put("categoria",    item.getCategoria());
        map.put("atualizadoEm", FirestoreSchema.nowTs());
        FirestoreSchema.mercadoItemDoc(LISTA_ATIVA_ID, item.getFirestoreId())
                .update(map)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSucesso(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onErro(e.getMessage()); });
    }

    /**
     * RF01 – Exclui um item da lista.
     */
    public void excluirItem(@NonNull ItemMercadoModel item, @NonNull Callback callback) {
        if (item.getFirestoreId() == null || item.getFirestoreId().isEmpty()) {
            callback.onErro("Item sem ID — não é possível excluir.");
            return;
        }

        FirestoreSchema.mercadoItemDoc(LISTA_ATIVA_ID, item.getFirestoreId())
                .delete()
                .addOnSuccessListener(v -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao excluir item."));
    }

    /**
     * Limpa todos os itens marcados como "no carrinho" de uma vez (batch).
     * Operação de limpeza do carrinho.
     */
    public void limparItensDoCarrinho(@NonNull List<ItemMercadoModel> itensMarcados,
                                      @NonNull Callback callback) {
        if (itensMarcados.isEmpty()) {
            callback.onSucesso();
            return;
        }

        FirebaseFirestore db = com.gussanxz.orgafacil.funcionalidades.firebase
                .ConfiguracaoFirestore.getFirestore();
        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);

        for (ItemMercadoModel item : itensMarcados) {
            if (item.getFirestoreId() != null && !item.getFirestoreId().isEmpty()) {
                batch.delete(FirestoreSchema.mercadoItemDoc(LISTA_ATIVA_ID, item.getFirestoreId()));
            }
        }

        batch.commit(new FirestoreSmartBatch.Callback() {
            @Override public void onSucesso() { callback.onSucesso(); }
            @Override public void onErro(String erro) { callback.onErro(erro); }
        });
    }

    // ─── Memória de Preços (RF06) ─────────────────────────────────────────────

    /**
     * RF06 – Salva o último preço pago por um produto.
     * Chave = nome normalizado (lowercase, sem espaços extras) → CT11.
     * RN01 – valor em centavos.
     */
    public void salvarMemoriaPreco(@NonNull String nomeProduto, int valorCentavos) {
        Map<String, Object> data = new HashMap<>();
        data.put(CAMPO_ULTIMO_NOME,   nomeProduto);
        data.put(CAMPO_ULTIMO_PRECO,  valorCentavos);   // RN01 – centavos
        data.put(CAMPO_ATUALIZADO_EM, FirestoreSchema.nowTs());

        // set() com merge para não sobrescrever campos futuros
        FirestoreSchema.mercadoMemoriaPrecoDoc(nomeProduto)
                .set(data)
                .addOnFailureListener(e -> {
                    // Falha silenciosa — memória de preços é best-effort
                });
    }

    /**
     * RF06 – Busca o último preço registrado para um produto.
     * CT10 – retorna o valor em centavos se encontrar.
     * CT11 – busca é case-insensitive (chave normalizada em lowercase).
     * CT12 – chama onNaoEncontrado() se produto inédito, sem travar.
     */
    public void buscarMemoriaPreco(@NonNull String nomeProduto,
                                   @NonNull CallbackPreco callback) {
        FirestoreSchema.mercadoMemoriaPrecoDoc(nomeProduto)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains(CAMPO_ULTIMO_PRECO)) {
                        Long valorLong = doc.getLong(CAMPO_ULTIMO_PRECO);
                        if (valorLong != null) {
                            callback.onSucesso(valorLong.intValue()); // RN01 – centavos
                        } else {
                            callback.onNaoEncontrado();
                        }
                    } else {
                        // CT12 – produto inédito
                        callback.onNaoEncontrado();
                    }
                })
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao buscar preço."));
    }

    // ─── Helper de deserialização ─────────────────────────────────────────────

    private ItemMercadoModel documentParaItem(DocumentSnapshot doc) {
        if (!doc.exists()) return null;
        try {
            ItemMercadoModel item = new ItemMercadoModel();
            item.setFirestoreId(doc.getId());
            item.setNome(doc.getString(CAMPO_NOME));
            item.setCategoria(doc.getString(CAMPO_CATEGORIA));

            Long valorLong = doc.getLong(CAMPO_VALOR_CENTAVOS);
            item.setValorCentavos(valorLong != null ? valorLong.intValue() : 0);

            Long qtdLong = doc.getLong(CAMPO_QUANTIDADE);
            item.setQuantidade(qtdLong != null ? qtdLong.intValue() : 1);

            Boolean noCarrinho = doc.getBoolean(CAMPO_NO_CARRINHO);
            item.setNoCarrinho(noCarrinho != null && noCarrinho);

            item.setCriadoEm(doc.getTimestamp(CAMPO_CRIADO_EM));
            item.setAtualizadoEm(doc.getTimestamp(CAMPO_ATUALIZADO_EM));

            return item;
        } catch (Exception e) {
            return null; // documento corrompido — ignora sem crashar
        }
    }
}