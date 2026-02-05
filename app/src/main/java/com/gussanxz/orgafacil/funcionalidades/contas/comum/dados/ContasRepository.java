package com.gussanxz.orgafacil.funcionalidades.contas.comum.dados;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ContasRepository (Bridge entre o Schema novo e a UI Legada)
 * * Responsável por traduzir os dados em centavos do Firestore para o
 * formato double que a sua Activity atual espera.
 */
public class ContasRepository {

    // Interfaces de compatibilidade para a UI atual
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

    // Helpers de conversão
    private static String safeStr(@Nullable String s) { return (s == null) ? "" : s; }

    private static double centToDouble(@Nullable Object valorCentObj) {
        if (valorCentObj instanceof Number) {
            return ((Number) valorCentObj).longValue() / 100.0;
        }
        return 0.0;
    }

    // Interface obrigatória para o callback de exclusão
    public interface RepoVoidCallback {
        void onSuccess();
        void onError(Exception e);
    }
    /**
     * Realiza a exclusão lógica (soft delete) marcando o campo deletedAt.
     */
    public void softDeleteMovimentacao(@NonNull String movId, @NonNull RepoVoidCallback cb) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deletedAt", com.google.firebase.Timestamp.now());

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .set(patch, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    /**
     * Recupera o nome do usuário para o cabeçalho "Olá, Fulano".
     */
    public void recuperarResumo(@NonNull ResumoCallback cb) {
        FirestoreSchema.configPerfilDoc().get()
                .addOnSuccessListener(doc -> {
                    String nome = doc.getString("nome");
                    if (nome == null) nome = doc.getString("name");
                    cb.onSucesso(0.0, safeStr(nome).isEmpty() ? "Usuário" : nome);
                })
                .addOnFailureListener(e -> cb.onErro(e.getMessage()));
    }

    /**
     * Busca movimentações convertendo o schema de centavos para double.
     */
    public void recuperarMovimentacoes(@NonNull DadosCallback cb) {
        FirestoreSchema.contasMovimentacoesCol()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snap -> {
                    List<MovimentacaoModel> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        // Ignora itens com soft delete
                        if (d.get("deletedAt") != null) continue;

                        MovimentacaoModel m = new MovimentacaoModel();
                        m.setKey(d.getId());
                        m.setData(d.getString("data"));
                        m.setHora(d.getString("hora"));
                        m.setDescricao(d.getString("descricao"));

                        String cat = d.getString("categoriaNome");
                        m.setCategoria(cat != null ? cat : d.getString("categoriaNomeSnapshot"));

                        // Converte Tipo: "Receita" -> "r", "Despesa" -> "d"
                        String tipoNovo = d.getString("tipo");
                        m.setTipo("Receita".equalsIgnoreCase(tipoNovo) ? "r" : "d");

                        // Converte Valor: 1050 (cent) -> 10.50 (double)
                        m.setValor(centToDouble(d.get("valorCent")));

                        lista.add(m);
                    }
                    cb.onSucesso(lista);
                })
                .addOnFailureListener(e -> cb.onErro(e.getMessage()));
    }

    /**
     * Exclui a movimentação (Soft Delete) e atualiza o ponteiro de "últimos".
     */
    public void excluirMovimentacao(@NonNull MovimentacaoModel mov, @NonNull SimplesCallback cb) {
        String movId = mov.getKey();
        if (movId == null) {
            cb.onErro("ID inválido");
            return;
        }

        Map<String, Object> patch = new HashMap<>();
        patch.put("deletedAt", Timestamp.now());

        FirestoreSchema.contasMovimentacaoDoc(movId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Recalcula o ponteiro para que o dashboard não mostre um item deletado
                    String tipoNovo = "r".equals(mov.getTipo()) ? "Receita" : "Despesa";
                    recalcularUltimoPorTipo(tipoNovo);
                    cb.onSucesso();
                })
                .addOnFailureListener(e -> cb.onErro(e.getMessage()));
    }

    /**
     * Lógica interna para manter o documento de "Resumos/ultimos" sincronizado.
     */
    private void recalcularUltimoPorTipo(String tipoNovo) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("tipo", tipoNovo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    String campo = "Receita".equalsIgnoreCase(tipoNovo) ? "ultimaEntrada" : "ultimaSaida";
                    Map<String, Object> update = new HashMap<>();

                    if (snap.isEmpty()) {
                        update.put(campo, null);
                    } else {
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        Map<String, Object> info = new HashMap<>();
                        info.put("movId", doc.getId());
                        info.put("valorCent", doc.get("valorCent"));
                        info.put("categoriaNomeSnapshot", doc.get("categoriaNome"));
                        info.put("createdAt", doc.get("createdAt"));
                        update.put(campo, info);
                    }

                    FirestoreSchema.contasResumoUltimosDoc().set(update, SetOptions.merge());
                });
    }
}