package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import android.content.Context;
import androidx.annotation.NonNull;

import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

/**
 * PreferenciasRepository
 * Gerencia a persistência das configurações no Firestore e
 * sincroniza automaticamente o cache local (SharedPreferences).
 */
public class PreferenciasRepository {

    private final String uid;
    private static final String COL_CONFIG = "config";
    private static final String DOC_PREFERENCIAS = "config_preferencias";

    public PreferenciasRepository() {
        // Utiliza sua classe centralizada para obter o UID
        this.uid = FirebaseSession.isUserLogged() ? FirebaseSession.getUserId() : null;
    }

    public interface Callback {
        void onSucesso(PreferenciasModel prefs);
        void onErro(String erro);
    }

    /**
     * Salva na nuvem e atualiza o cache local imediatamente.
     */
    public void salvar(@NonNull Context context, @NonNull PreferenciasModel prefs, @NonNull Callback callback) {
        if (uid == null) {
            callback.onErro("Usuário não logado");
            return;
        }

        prefs.setUpdatedAt(System.currentTimeMillis());

        // 1. Sincroniza o Cache Local antes/durante a escrita na nuvem [cite: 2026-01-31]
        atualizarCacheLocal(context, prefs);

        FirestoreSchema.userDoc(uid)
                .collection(COL_CONFIG)
                .document(DOC_PREFERENCIAS)
                .set(prefs)
                .addOnSuccessListener(aVoid -> callback.onSucesso(prefs))
                .addOnFailureListener(e -> callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao salvar"));
    }

    /**
     * Recupera da nuvem e garante que o cache local esteja atualizado.
     */
    public void obter(@NonNull Context context, @NonNull Callback callback) {
        if (uid == null) return;

        FirestoreSchema.userDoc(uid)
                .collection(COL_CONFIG)
                .document(DOC_PREFERENCIAS)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    PreferenciasModel prefs = documentSnapshot.toObject(PreferenciasModel.class);
                    if (prefs == null) {
                        // Padrão de segurança caso o documento não exista
                        prefs = new PreferenciasModel("SISTEMA", "BRL", false);
                    }

                    // 2. Sincroniza o Cache Local com os dados vindos da nuvem [cite: 2026-01-31]
                    atualizarCacheLocal(context, prefs);

                    callback.onSucesso(prefs);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage() != null ? e.getMessage() : "Erro ao carregar"));
    }

    /**
     * Método centralizado para garantir que as SharedPreferences
     * reflitam os dados mais recentes do modelo.
     */
    private void atualizarCacheLocal(Context context, PreferenciasModel prefs) {
        TemaHelper.salvarTemaCache(context, prefs.getTema());
        FirebaseSession.putString(context, FirebaseSession.KEY_MOEDA, prefs.getMoeda());
        FirebaseSession.putBoolean(context, "esconder_saldo", prefs.isEsconderSaldo());
    }
}