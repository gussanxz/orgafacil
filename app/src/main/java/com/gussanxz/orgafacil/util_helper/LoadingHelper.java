package com.gussanxz.orgafacil.util_helper;

import android.view.View;

/**
 * Classe Helper para gerenciar o estado de carregamento de forma independente.
 */
public class LoadingHelper {

    private final View loadingView;

    public LoadingHelper(View loadingView) {
        this.loadingView = loadingView;
    }

    /**
     * Exibe o overlay de loading e bloqueia interações.
     */
    public void exibir() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Oculta o overlay de loading.
     */
    public void ocultar() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
    }
}