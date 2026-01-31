package com.gussanxz.orgafacil.funcionalidades.comum.dados;

import static com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema.VENDAS_CATEGORIAS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CategoriaRepository
 *
 * O que faz:
 * - CRUD + listener em tempo real para categorias do módulo vendas
 * - Mantém o caminho atual:
 *   {ROOT}/{uid}/vendas_categorias/{id}
 *
 * Impacto:
 * - Agora respeita FirestoreSchema.ROOT
 * - Remove hardcode de "users"
 * - Remove duplicidade/confusão de caminhos (tinha docRef sobrescrito)
 *
 * Futuro (migração opcional):
 * - mover para: {ROOT}/{uid}/moduloSistema/vendas/categorias/{categoriaId}
 *   e adaptar telas sem quebrar dados
 */
public class CategoriaRepository {

    private final FirebaseFirestore db;
    private final String uid;

    // Mantemos o nome da coleção atual para compatibilidade com o app
    private static final String COL_VENDAS_CATEGORIAS = "vendas_categorias";

    public CategoriaRepository() {
        this.db = FirestoreSchema.db();
        this.uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public interface CategoriaCallback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<Categoria> lista);
        void onErro(String erro);
    }

    /**
     * Salvar/Editar categoria
     *
     * Caminho atual preservado:
     * {ROOT}/{uid}/vendas_categorias/{id}
     */
    public void salvar(@NonNull Categoria categoria, @NonNull CategoriaCallback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection(VENDAS_CATEGORIAS)
                    .document(categoria.getId());
        } else {
            docRef = FirestoreSchema.userDoc(uid)
                    .collection(VENDAS_CATEGORIAS)
                    .document();
            categoria.setId(docRef.getId());
        }

        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Atualizado!" : "Criado!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao salvar categoria"));
    }

    /**
     * Listar categorias em tempo real
     *
     * Impacto:
     * - Listener em tempo real deixa UI sempre atualizada
     * - Cada mudança gera evento (custo: leituras por atualização)
     */
    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        if (uid == null) return null;

        return FirestoreSchema.userDoc(uid)
                .collection(COL_VENDAS_CATEGORIAS)
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage() != null ? error.getMessage() : "Erro ao listar categorias");
                        return;
                    }

                    List<Categoria> lista = new ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(Categoria.class);

                        // Garante IDs caso o toObjects não setar o id
                        int i = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            if (i < lista.size()) lista.get(i).setId(doc.getId());
                            i++;
                        }
                    }

                    callback.onNovosDados(lista);
                });
    }

    /**
     * Excluir categoria
     */
    public void excluir(@NonNull String idCategoria, @NonNull CategoriaCallback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        FirestoreSchema.userDoc(uid)
                .collection(COL_VENDAS_CATEGORIAS)
                .document(idCategoria)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao excluir categoria"));
    }

    /**
     * ContasRepository
     *
     * Centraliza o CRUD/queries do módulo Contas:
     * - categorias
     * - movimentações
     * - contas futuras
     * - resumo "Resumos/ultimos" (ultimaEntrada / ultimaSaida)
     *
     * Impacto:
     * - Tira paths das Activities (menos bug e mais fácil de migrar)
     * - Mantém leitura rápida/barata via documento de resumo
     * - Permite padronizar regras (ex: recomendação por tipo)
     */
    public static class ContasRepository {

        // =========================
        // CALLBACKS
        // =========================

        public interface RepoCallback<T> {
            void onSuccess(T result);
            void onError(Exception e);
        }

        public interface RepoVoidCallback {
            void onSuccess();
            void onError(Exception e);
        }

        /** Compat com código atual das Activities (onSucesso/onErro). */
        public interface SimplesCallback {
            void onSucesso();
            void onErro(String erro);
        }

        // =========================
        // CATEGORIAS (contasCategorias)
        // =========================

        public void listarCategoriasAtivas(@NonNull RepoCallback<QuerySnapshot> cb) {
            FirestoreSchema.contasCategoriasCol()
                    .whereEqualTo("ativo", true)
                    .orderBy("ordem", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);
        }

        /**
         * Salva/edita categoria.
         * Observação: createdAt ideal é serverTimestamp pra consistência.
         */
        public void salvarCategoria(@NonNull String categoriaId,
                                    @NonNull Map<String, Object> data,
                                    @NonNull RepoVoidCallback cb) {

            if (!data.containsKey("createdAt")) data.put("createdAt", FieldValue.serverTimestamp());

            FirestoreSchema.contasCategoriaDoc(categoriaId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        public void desativarCategoria(@NonNull String categoriaId, @NonNull RepoVoidCallback cb) {
            Map<String, Object> patch = new HashMap<>();
            patch.put("ativo", false);

            FirestoreSchema.contasCategoriaDoc(categoriaId)
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        // =========================
        // MOVIMENTACOES (contasMovimentacoes)
        // =========================

        public void listarUltimasMovimentacoes(int limit, @NonNull RepoCallback<QuerySnapshot> cb) {
            FirestoreSchema.contasMovimentacoesCol()
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);
        }

        public void listarUltimasPorTipo(@NonNull String tipoMov, int limit, @NonNull RepoCallback<QuerySnapshot> cb) {
            String tipo = normalizeTipo(tipoMov);
            if (tipo == null) {
                cb.onError(new IllegalArgumentException("Tipo inválido: " + tipoMov));
                return;
            }

            FirestoreSchema.contasMovimentacoesCol()
                    .whereEqualTo("tipo", tipo)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);
        }

        /**
         * Cria/atualiza uma movimentação no schema novo.
         *
         * - createdAt: serverTimestamp
         * - diaKey/mesKey: padroniza consultas por dia/mês
         */
        public void salvarMovimentacao(@NonNull String movId,
                                       @NonNull Map<String, Object> data,
                                       @NonNull RepoVoidCallback cb) {

            if (!data.containsKey("createdAt")) data.put("createdAt", FieldValue.serverTimestamp());
            if (!data.containsKey("diaKey")) data.put("diaKey", FirestoreSchema.diaKey(new Date()));
            if (!data.containsKey("mesKey")) data.put("mesKey", FirestoreSchema.mesKey(new Date()));

            FirestoreSchema.contasMovimentacaoDoc(movId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        /**
         * Soft delete: marca deletedAt.
         * Regra: UI deve filtrar deletedAt != null se você usar esse modo.
         */
        public void softDeleteMovimentacao(@NonNull String movId, @NonNull RepoVoidCallback cb) {
            Map<String, Object> patch = new HashMap<>();
            patch.put("deletedAt", FieldValue.serverTimestamp());

            FirestoreSchema.contasMovimentacaoDoc(movId)
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        // =========================
        // EXCLUIR (USADO PELAS ACTIVITIES)
        // =========================

        /**
         * Exclui (hard delete) uma movimentação e mantém "Resumos/ultimos" correto.
         *
         * Requisito:
         * - se apagar a última daquele tipo => recalcular anterior
         * - se não existir anterior => limpar o campo
         * - receita mexe só em ultimaEntrada
         * - despesa mexe só em ultimaSaida
         */
        public void excluirMovimentacao(@NonNull MovimentacaoModel mov, @NonNull SimplesCallback cb) {
            String movId = mov.getKey();
            if (movId == null || movId.trim().isEmpty()) {
                cb.onErro("Movimentação sem ID (key) para excluir.");
                return;
            }

            String tipoMov = normalizeTipo(mov.getTipo());
            if (tipoMov == null) {
                cb.onErro("Tipo de movimentação inválido.");
                return;
            }

            // 1) Lê resumo atual para saber se o mov era o "último" do tipo
            getUltimosResumo(new RepoCallback<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot resumoDoc) {
                    boolean eraUltimo = isMovIdIgualAoUltimoDoTipo(resumoDoc, tipoMov, movId);

                    // 2) Deleta o doc no caminho novo: teste/{uid}/moduloSistema/contas/contasMovimentacoes/{movId}
                    FirestoreSchema.contasMovimentacaoDoc(movId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                // 3) Se era o último, recalcula; senão, não faz query extra (mais barato)
                                if (eraUltimo) {
                                    recalcularUltimoPorTipo(tipoMov, new ContasRepository.RepoVoidCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // ok
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            // erro
                                        }
                                    });

                                } else {
                                    cb.onSucesso();
                                }
                            })
                            .addOnFailureListener(e -> cb.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao excluir."));
                }

                @Override
                public void onError(Exception e) {
                    cb.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao ler resumo de últimos.");
                }
            });
        }

        // =========================
        // CONTAS FUTURAS (contasFuturas)
        // =========================

        public void listarContasFuturasPendentes(@NonNull RepoCallback<QuerySnapshot> cb) {
            FirestoreSchema.contasFuturasCol()
                    .whereEqualTo("status", "PENDENTE")
                    .orderBy("vencimento", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);
        }

        public void salvarContaFutura(@NonNull String contaFuturaId,
                                      @NonNull Map<String, Object> data,
                                      @NonNull RepoVoidCallback cb) {

            if (!data.containsKey("createdAt")) data.put("createdAt", FieldValue.serverTimestamp());

            FirestoreSchema.contasFuturaDoc(contaFuturaId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        public void atualizarStatusContaFutura(@NonNull String contaFuturaId,
                                               @NonNull String status,
                                               @NonNull RepoVoidCallback cb) {

            Map<String, Object> patch = new HashMap<>();
            patch.put("status", status);

            FirestoreSchema.contasFuturaDoc(contaFuturaId)
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        // =========================
        // RESUMO "ULTIMOS" (Resumos/ultimos)
        // =========================

        public void getUltimosResumo(@NonNull RepoCallback<DocumentSnapshot> cb) {
            FirestoreSchema.contasResumoUltimosDoc()
                    .get()
                    .addOnSuccessListener(cb::onSuccess)
                    .addOnFailureListener(cb::onError);
        }

        /**
         * Atualiza ultimaEntrada/ultimaSaida via merge.
         */
        public void setUltimo(@NonNull String tipoMov,
                              @NonNull Map<String, Object> snapshot,
                              @NonNull RepoVoidCallback cb) {

            String tipo = normalizeTipo(tipoMov);
            if (tipo == null) {
                cb.onError(new IllegalArgumentException("Tipo inválido: " + tipoMov));
                return;
            }

            String campo = "Receita".equalsIgnoreCase(tipo) ? "ultimaEntrada" : "ultimaSaida";

            Map<String, Object> patch = new HashMap<>();
            patch.put(campo, snapshot);

            FirestoreSchema.contasResumoUltimosDoc()
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        /**
         * Recalcula ultimaEntrada/ultimaSaida procurando a movimentação mais recente daquele tipo.
         * - query pequena: tipo + createdAt desc + limit 1
         */
        public void recalcularUltimoPorTipo(@NonNull String tipoMov, @NonNull RepoVoidCallback cb) {
            String tipo = normalizeTipo(tipoMov);
            if (tipo == null) {
                cb.onError(new IllegalArgumentException("Tipo inválido: " + tipoMov));
                return;
            }

            FirestoreSchema.contasMovimentacoesCol()
                    .whereEqualTo("tipo", tipo)
                    // se você implementar soft-delete pra valer, aqui você filtra:
                    // .whereEqualTo("deletedAt", null)  // só funciona bem se o campo existir em todos docs (padrão)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            limparCampoUltimo(tipo, cb);
                            return;
                        }

                        DocumentSnapshot doc = snap.getDocuments().get(0);

                        Map<String, Object> snapshot = new HashMap<>();
                        snapshot.put("movId", doc.getId());
                        snapshot.put("createdAt", doc.get("createdAt"));
                        snapshot.put("valorCent", doc.get("valorCent"));
                        snapshot.put("categoriaId", doc.get("categoriaId"));

                        // compat: tenta "categoriaNome" e, se não tiver, tenta "categoriaNomeSnapshot"
                        Object catNome = doc.get("categoriaNome");
                        if (catNome == null) catNome = doc.get("categoriaNomeSnapshot");
                        snapshot.put("categoriaNomeSnapshot", catNome);

                        setUltimo(tipo, snapshot, cb);
                    })
                    .addOnFailureListener(cb::onError);
        }

        private void limparCampoUltimo(@NonNull String tipoMov, @NonNull RepoVoidCallback cb) {
            String tipo = normalizeTipo(tipoMov);
            if (tipo == null) {
                cb.onError(new IllegalArgumentException("Tipo inválido: " + tipoMov));
                return;
            }

            String campo = "Receita".equalsIgnoreCase(tipo) ? "ultimaEntrada" : "ultimaSaida";

            Map<String, Object> patch = new HashMap<>();
            patch.put(campo, null);

            FirestoreSchema.contasResumoUltimosDoc()
                    .set(patch, SetOptions.merge())
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onError);
        }

        // =========================
        // HELPERS (internos)
        // =========================

        /**
         * Normaliza tipos vindos do app (compatibilidade):
         * - "r" => "Receita"
         * - "d" => "Despesa"
         * - "Receita"/"Despesa" mantém
         */
        @Nullable
        private String normalizeTipo(@Nullable String tipo) {
            if (tipo == null) return null;
            String t = tipo.trim();
            if (t.isEmpty()) return null;

            if ("r".equalsIgnoreCase(t)) return "Receita";
            if ("d".equalsIgnoreCase(t)) return "Despesa";

            if ("receita".equalsIgnoreCase(t)) return "Receita";
            if ("despesa".equalsIgnoreCase(t)) return "Despesa";

            return null;
        }

        /**
         * Verifica se movId é o mesmo do resumo do tipo correspondente.
         */
        private boolean isMovIdIgualAoUltimoDoTipo(@NonNull DocumentSnapshot resumoDoc,
                                                   @NonNull String tipoMov,
                                                   @NonNull String movId) {

            String tipo = normalizeTipo(tipoMov);
            if (tipo == null) return false;

            String campo = "Receita".equalsIgnoreCase(tipo) ? "ultimaEntrada" : "ultimaSaida";
            Object val = resumoDoc.get(campo);
            if (!(val instanceof Map)) return false;

            Map<?, ?> m = (Map<?, ?>) val;
            Object id = m.get("movId");
            return id != null && movId.equals(String.valueOf(id));
        }
    }
}