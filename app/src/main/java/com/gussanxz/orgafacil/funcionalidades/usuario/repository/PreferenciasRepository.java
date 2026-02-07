package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

public class PreferenciasRepository {

    private final String uid;
    private static final String COL_CONFIG = "config";
    private static final String DOC_PREFERENCIAS = "config_preferencias"; //

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
        prefs.setDataAtualizacao(null);

        // Atualiza cache local imediatamente (UX rápida)
        atualizarCacheLocal(context, prefs);

        getDocumentoRef()
                .set(prefs)
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
                    PreferenciasModel prefs = documentSnapshot.toObject(PreferenciasModel.class);

                    if (prefs == null) {
                        prefs = new PreferenciasModel(); // Retorna padrão se não existir (sem salvar no banco ainda)
                    }

                    atualizarCacheLocal(context, prefs);
                    callback.onSucesso(prefs);
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao baixar: " + e.getMessage()));
    }

    // ============================================================================================
    // 3. RESETAR (Delete / Restaurar Padrões)
    // Útil para botão "Restaurar Configurações de Fábrica"
    // ============================================================================================
    public void resetar(@NonNull Context context, @NonNull Callback callback) {
        // Cria um modelo novo zerado (padrões definidos no construtor vazio)
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
        TemaHelper.salvarTemaCache(context, prefs.getTema());
        FirebaseSession.putString(context, "moeda_pref", prefs.getMoeda());
        FirebaseSession.putBoolean(context, "esconder_saldo_pref", prefs.isEsconderSaldo());
    }
}