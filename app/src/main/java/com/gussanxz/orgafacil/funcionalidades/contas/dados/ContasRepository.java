package com.gussanxz.orgafacil.funcionalidades.contas.dados;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;

import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ContasRepository (schema novo + compat UI antiga)
 *
 * Schema novo:
 * {ROOT}/{uid}/moduloSistema/contas/contasMovimentacoes/{movId}
 * {ROOT}/{uid}/moduloSistema/contas/Resumos/ultimos
 *
 * Este repository também expõe métodos "compat" usados nas telas atuais:
 * - recuperarResumo(...)
 * - recuperarMovimentacoes(...)
 * - excluirMovimentacao(...)
 */
public class ContasRepository {

    // 🔑 UID explícito, resolvido fora
    private final String uid;

    public ContasRepository(@NonNull String uid) {
        this.uid = uid;
    }

    // ===== callbacks internos (pra poder usar ContasRepository.RepoVoidCallback) =====
    public interface RepoVoidCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    // ========= CALLBACKS "compat" (usadas pelas Activities) =========

    public interface DadosCallback {
        void onSucesso(List<MovimentacaoModel> lista);
        void onErro(String erro);
    }

    public interface ResumoCallback {
        void onSucesso(double saldo, String nome);
        void onErro(String erro);
    }

    public interface SimplesCallback {
        void onSucesso();
        void onErro(String erro);
    }

    // ========= HELPERS =========

    private static String safeStr(@Nullable String s) {
        return (s == null) ? "" : s;
    }

    private static String mapTipoNovoParaLegado(@Nullable String tipoNovo) {
        if (tipoNovo == null) return "d";
        return "Receita".equalsIgnoreCase(tipoNovo) ? "r" : "d";
    }

    private static double centToDouble(@Nullable Object valorCentObj) {
        if (valorCentObj == null) return 0.0;
        try {
            if (valorCentObj instanceof Number) {
                long cent = ((Number) valorCentObj).longValue();
                return cent / 100.0;
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    // =========================
    // RESUMO (compat)
    // =========================

    public void recuperarResumo(@NonNull ResumoCallback cb) {
        FirestoreSchema.configPerfilDoc(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String nome = safeStr(doc.getString("nome"));
                    if (nome.isEmpty()) nome = safeStr(doc.getString("name"));
                    if (nome.isEmpty()) nome = "Usuário";
                    cb.onSucesso(0.0, nome);
                })
                .addOnFailureListener(e -> cb.onErro(e.getMessage()));
    }

    // =========================
    // MOVIMENTAÇÕES (compat)
    // =========================

    public void recuperarMovimentacoes(@NonNull DadosCallback cb) {
        FirestoreSchema.contasMovimentacoesCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .addOnSuccessListener(snap -> {
                    List<MovimentacaoModel> lista = new ArrayList<>();

                    for (QueryDocumentSnapshot d : snap) {
                        if (d.contains("deletedAt") && d.get("deletedAt") != null) continue;

                        MovimentacaoModel m = new MovimentacaoModel();
                        m.setKey(d.getId());
                        m.setData(d.getString("data"));
                        m.setHora(d.getString("hora"));
                        m.setDescricao(d.getString("descricao"));

                        String cat = d.getString("categoriaNome");
                        if (cat == null) cat = d.getString("categoriaNomeSnapshot");
                        m.setCategoria(cat);

                        m.setTipo(mapTipoNovoParaLegado(d.getString("tipo")));
                        m.setValor(centToDouble(d.get("valorCent")));

                        lista.add(m);
                    }

                    cb.onSucesso(lista);
                })
                .addOnFailureListener(e -> cb.onErro(e.getMessage()));
    }

    // =========================
    // EXCLUSÃO (compat)
    // =========================

    public void excluirMovimentacao(@NonNull MovimentacaoModel mov, @NonNull SimplesCallback cb) {
        String movId = mov.getKey();
        if (movId == null || movId.trim().isEmpty()) {
            cb.onErro("Movimentação sem ID (key vazia).");
            return;
        }

        String tipoNovo = "r".equals(mov.getTipo()) ? "Receita" : "Despesa";

        softDeleteMovimentacao(movId, new RepoVoidCallback() {
            @Override
            public void onSuccess() {
                recalcularUltimoPorTipo(tipoNovo, new RepoVoidCallback() {
                    @Override public void onSuccess() { cb.onSucesso(); }
                    @Override public void onError(Exception e) { cb.onSucesso(); }
                });
            }

            @Override
            public void onError(Exception e) {
                cb.onErro(e.getMessage());
            }
        });
    }

    // =========================
    // API NOVA
    // =========================

    public void softDeleteMovimentacao(@NonNull String movId, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deletedAt", Timestamp.now());

        FirestoreSchema.contasMovimentacaoDoc(uid, movId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    public void recalcularUltimoPorTipo(@NonNull String tipoMov, @NonNull RepoVoidCallback cb) {
        FirestoreSchema.contasMovimentacoesCol(uid)
                .whereEqualTo("tipo", tipoMov)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        limparCampoUltimo(tipoMov, cb);
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);

                    Map<String, Object> snapshot = new HashMap<>();
                    snapshot.put("movId", doc.getId());
                    snapshot.put("createdAt", doc.get("createdAt"));
                    snapshot.put("valorCent", doc.get("valorCent"));
                    snapshot.put("categoriaId", doc.get("categoriaId"));
                    snapshot.put("categoriaNomeSnapshot", doc.get("categoriaNome"));

                    setUltimo(tipoMov, snapshot, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    public void setUltimo(@NonNull String tipoMov, @NonNull Map<String, Object> snapshot, @NonNull RepoVoidCallback cb) {
        String campo = "Receita".equalsIgnoreCase(tipoMov) ? "ultimaEntrada" : "ultimaSaida";

        Map<String, Object> patch = new HashMap<>();
        patch.put(campo, snapshot);
        patch.put("updatedAt", Timestamp.now());

        FirestoreSchema.contasResumoUltimosDoc(uid)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    private void limparCampoUltimo(@NonNull String tipoMov, @NonNull RepoVoidCallback cb) {
        String campo = "Receita".equalsIgnoreCase(tipoMov) ? "ultimaEntrada" : "ultimaSaida";

        Map<String, Object> patch = new HashMap<>();
        patch.put(campo, null);
        patch.put("updatedAt", Timestamp.now());

        FirestoreSchema.contasResumoUltimosDoc(uid)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }
}
