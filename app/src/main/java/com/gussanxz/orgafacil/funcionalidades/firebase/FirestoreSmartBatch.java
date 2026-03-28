package com.gussanxz.orgafacil.funcionalidades.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class FirestoreSmartBatch {

    // ── Interface de callback genérica para não depender de repositórios específicos
    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    private final FirebaseFirestore firestore;
    private WriteBatch loteAtual;
    private int contadorOperacoes = 0;
    private final List<WriteBatch> todosOsLotes = new ArrayList<>();

    public FirestoreSmartBatch(FirebaseFirestore firestore) {
        this.firestore = firestore;
        novoLote();
    }

    private void novoLote() {
        loteAtual = firestore.batch();
        todosOsLotes.add(loteAtual);
        contadorOperacoes = 0;
    }

    private void checarLimite() {
        // O limite absoluto do Firestore é 500. 400 mantém uma margem excelente e segura.
        if (contadorOperacoes >= 400) novoLote();
        contadorOperacoes++;
    }

    public void set(DocumentReference ref, Object data) {
        checarLimite();
        loteAtual.set(ref, data);
    }

    public void update(DocumentReference ref, String field, Object value) {
        checarLimite();
        loteAtual.update(ref, field, value);
    }

    public void delete(DocumentReference ref) {
        checarLimite();
        loteAtual.delete(ref);
    }

    /**
     * Dispara todos os lotes simultaneamente usando a API de Tasks do Android.
     */
    public void commit(Callback callback) {
        List<Task<Void>> tasks = new ArrayList<>();

        // Adiciona o commit de todos os lotes na nossa lista de tarefas
        for (WriteBatch lote : todosOsLotes) {
            tasks.add(lote.commit());
        }

        // Tasks.whenAll aguarda a conclusão de TODAS as tarefas em paralelo.
        // Se uma falhar, ele cai no FailureListener.
        Tasks.whenAll(tasks)
                .addOnSuccessListener(aVoid -> callback.onSucesso())
                .addOnFailureListener(e -> {
                    String erro = e.getMessage() != null ? e.getMessage() : "Erro desconhecido no processamento em lote.";
                    callback.onErro("Falha na sincronização dos lotes: " + erro);
                });
    }
}