package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

public class PreferenciasRepository {

    private final String uid;
    private static final String COL_CONFIG = "config";
    private static final String DOC_PREFERENCIAS = "config_preferencias";

    public PreferenciasRepository() {
        this.uid = FirebaseSession.isUserLogged() ? FirebaseSession.getUserId() : null;
    }

    public interface Callback {
        void onSucesso(PreferenciasModel prefs);
        void onErro(String erro);
    }

    // ============================================================================================
    // 1. SALVAR (Create & Update)
    // Atualiza o banco e o cache local ao mesmo tempo.
    // ============================================================================================
    public void salvar(@NonNull Context context, @NonNull PreferenciasModel prefs, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        // Força atualização da data pelo servidor (ServerTimestamp)
        // Isso garante que o campo no Java seja nulo para o Firestore preencher no Cloud.
        prefs.setDataAtualizacao(null);

        // Atualiza cache local imediatamente (UX rápida)
        atualizarCacheLocal(context, prefs);

        getDocumentoRef()
                .set(prefs) // Linha 44: O Model agora corrigido permite a serialização correta
                .addOnSuccessListener(aVoid -> callback.onSucesso(prefs))
                .addOnFailureListener(e -> callback.onErro("Erro ao salvar: " + e.getMessage()));
    }

    // ============================================================================================
    // 2. OBTER (Read)
    // Busca do servidor. Se não existir, cria o padrão.
    // ============================================================================================
    public void obter(@NonNull Context context, @NonNull Callback callback) {
        if (uid == null) return;

        getDocumentoRef().get()
                .addOnSuccessListener(documentSnapshot -> {
                    // O método toObject agora funcionará sem o erro de retorno 'Object'
                    PreferenciasModel prefs = documentSnapshot.toObject(PreferenciasModel.class);

                    if (prefs == null) {
                        prefs = new PreferenciasModel();
                    }

                    atualizarCacheLocal(context, prefs);
                    callback.onSucesso(prefs);
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao baixar: " + e.getMessage()));
    }

    // ============================================================================================
    // 3. RESETAR (Delete / Restaurar Padrões)
    // ============================================================================================
    public void resetar(@NonNull Context context, @NonNull Callback callback) {
        PreferenciasModel padrao = new PreferenciasModel();
        salvar(context, padrao, callback);
    }

    // ============================================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================================

    private DocumentReference getDocumentoRef() {
        return FirestoreSchema.userDoc(uid)
                .collection(COL_CONFIG)
                .document(DOC_PREFERENCIAS);
    }

    private void atualizarCacheLocal(Context context, PreferenciasModel prefs) {
        if (prefs == null) return;

        if (prefs.getVisual() != null) {
            TemaHelper.salvarTemaCache(context, prefs.getVisual().getTema());
            FirebaseSession.putBoolean(context, "esconder_saldo_pref", prefs.getVisual().isEsconderSaldo());
        }

        if (prefs.getRegional() != null) {
            FirebaseSession.putString(context, "moeda_pref", prefs.getRegional().getMoeda());
        }
    }
}