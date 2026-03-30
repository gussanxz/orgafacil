package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;

import java.util.ArrayList;
import java.util.List;

public class CatalogoRepository {

    private final CategoriaCatalogoRepository categoriaRepository;

    public CatalogoRepository() {
        this.categoriaRepository = new CategoriaCatalogoRepository();
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<CatalogoModel> lista);
        void onErro(String erro);
    }

    // ── Salvar (novo ou edição) ───────────────────────────────────────
    public void salvar(@NonNull CatalogoModel model, @NonNull Callback callback) {
        try {
            normalizarCategoria(model);

            boolean categoriaPadrao = CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO
                    .equals(model.getCategoriaId());

            if (categoriaPadrao) {
                categoriaRepository.garantirCategoriaPadrao(new CategoriaCatalogoRepository.Callback() {
                    @Override public void onSucesso(String msg) { salvarNoFirestore(model, callback); }
                    @Override public void onErro(String erro)   { callback.onErro(erro); }
                });
            } else {
                salvarNoFirestore(model, callback);
            }
        } catch (IllegalStateException e) {
            callback.onErro("Sessão expirada: " + e.getMessage());
        }
    }

    private void salvarNoFirestore(@NonNull CatalogoModel model, @NonNull Callback callback) {
        boolean isEdicao = model.getId() != null && !model.getId().isEmpty();
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.vendasProdutoDoc(model.getId());
        } else {
            docRef = FirestoreSchema.vendasCatalogoCol().document();
            model.setId(docRef.getId());
        }

        docRef.set(model)
                .addOnSuccessListener(v -> callback.onSucesso(
                        isEdicao ? "Item atualizado!" : "Item salvo!"))
                .addOnFailureListener(e -> callback.onErro(
                        e.getMessage() != null ? e.getMessage() : "Erro ao salvar"));
    }

    // ── Listener em tempo real (todos) ────────────────────────────────
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        return listarTempoRealFiltrado(null, callback);
    }

    // ── Listener filtrado por tipo ────────────────────────────────────
    public ListenerRegistration listarTempoRealFiltrado(
            String tipoFiltro, @NonNull ListaCallback callback) {
        try {
            Query query = FirestoreSchema.vendasCatalogoCol()
                    .orderBy("nome", Query.Direction.ASCENDING);

            if (tipoFiltro != null) {
                query = query.whereEqualTo("tipo", tipoFiltro);
            }

            return query.addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro");
                    return;
                }
                List<CatalogoModel> lista = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        CatalogoModel model = fromDocument(doc);
                        if (model != null) lista.add(model);
                    }
                }
                callback.onNovosDados(lista);
            });
        } catch (IllegalStateException e) {
            callback.onErro("Usuário não logado");
            return null;
        }
    }

    // ── Deserialização tolerante a dados legados ──────────────────────
    /**
     * Converte um DocumentSnapshot em CatalogoModel de forma segura.
     *
     * Problema: documentos antigos (ProdutoModel / ServicoModel) gravavam o
     * campo "tipo" como int/Long (0 = produto, 1 = serviço). O novo modelo
     * usa String ("produto" / "servico"). O toObjects() do Firestore explode
     * ao tentar converter Long → String diretamente.
     *
     * Solução: ler o campo "tipo" manualmente antes de tentar toObject(),
     * e se for numérico, converter para a String equivalente antes de
     * popular o modelo.
     */
    private CatalogoModel fromDocument(@NonNull DocumentSnapshot doc) {
        try {
            // Lê o campo "tipo" sem conversão automática
            Object tipoRaw = doc.get("tipo");
            String tipoStr = resolverTipo(tipoRaw);

            // Se o tipo vier como numérico (dado legado), removemos temporariamente
            // para que toObject() não tente converter Long → String e quebre.
            // Fazemos isso clonando os dados e substituindo o campo manualmente.
            CatalogoModel model;
            if (tipoRaw instanceof Long || tipoRaw instanceof Integer) {
                // Dado legado: deserializa ignorando o campo "tipo" e seta manualmente
                model = docParaModelSemTipo(doc);
            } else {
                // Dado novo: deserialização normal
                model = doc.toObject(CatalogoModel.class);
            }

            if (model == null) return null;
            model.setId(doc.getId());
            model.setTipo(tipoStr);
            return model;

        } catch (Exception e) {
            // Documento corrompido ou incompatível — ignora e segue
            return null;
        }
    }

    /**
     * Converte o valor bruto do campo "tipo" para a String correta.
     *   - Long 0  → "produto"
     *   - Long 1  → "servico"
     *   - String  → usa diretamente (valida se é um dos dois valores conhecidos)
     *   - null    → "produto" (default seguro)
     */
    private String resolverTipo(Object tipoRaw) {
        if (tipoRaw instanceof String) {
            String s = (String) tipoRaw;
            return CatalogoModel.TIPO_STR_SERVICO.equals(s)
                    ? CatalogoModel.TIPO_STR_SERVICO
                    : CatalogoModel.TIPO_STR_PRODUTO;
        }
        if (tipoRaw instanceof Long) {
            return ((Long) tipoRaw) == 1L
                    ? CatalogoModel.TIPO_STR_SERVICO
                    : CatalogoModel.TIPO_STR_PRODUTO;
        }
        if (tipoRaw instanceof Integer) {
            return ((Integer) tipoRaw) == 1
                    ? CatalogoModel.TIPO_STR_SERVICO
                    : CatalogoModel.TIPO_STR_PRODUTO;
        }
        return CatalogoModel.TIPO_STR_PRODUTO;
    }

    /**
     * Para documentos legados onde "tipo" é numérico, popula o CatalogoModel
     * lendo cada campo individualmente para evitar a falha de conversão.
     */
    private CatalogoModel docParaModelSemTipo(@NonNull DocumentSnapshot doc) {
        CatalogoModel m = new CatalogoModel();

        String nome = doc.getString("nome");
        if (nome == null) nome = doc.getString("descricao"); // fallback campo legado
        m.setNome(nome != null ? nome : "");

        String descricao = doc.getString("descricao");
        m.setDescricao(descricao != null ? descricao : "");

        String categoriaId = doc.getString("categoriaId");
        m.setCategoriaId(categoriaId != null ? categoriaId : CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO);

        String categoria = doc.getString("categoria");
        m.setCategoria(categoria != null ? categoria : CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);

        // Preço: campo pode ser "preco" (novo) ou "valor" (legado ServicoModel)
        Double preco = doc.getDouble("preco");
        if (preco == null) preco = doc.getDouble("valor");
        m.setPreco(preco != null ? preco : 0.0);

        Boolean statusAtivo = doc.getBoolean("statusAtivo");
        m.setStatusAtivo(statusAtivo != null ? statusAtivo : true);

        Long iconeIndex = doc.getLong("iconeIndex");
        m.setIconeIndex(iconeIndex != null ? iconeIndex.intValue() : 7);

        return m;
    }

    // ── Normalização de categoria ─────────────────────────────────────
    private void normalizarCategoria(@NonNull CatalogoModel model) {
        String cId   = model.getCategoriaId();
        String cNome = model.getCategoria();
        boolean idVazio   = cId == null || cId.trim().isEmpty();
        boolean nomeVazio = cNome == null || cNome.trim().isEmpty();
        if (idVazio || nomeVazio) {
            model.setCategoriaId(CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO);
            model.setCategoria(CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);
        }
    }
}