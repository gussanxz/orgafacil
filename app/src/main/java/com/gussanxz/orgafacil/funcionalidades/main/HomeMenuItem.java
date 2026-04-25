package com.gussanxz.orgafacil.funcionalidades.main;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * HomeMenuItem
 *
 * Representa um item da grade da tela inicial.
 *
 * Boas práticas aplicadas:
 *  - Dados completamente separados da View (sem lógica de UI aqui).
 *  - Usa @DrawableRes / @StringRes para segurança em tempo de compilação.
 *  - Imutável por design: todos os campos são final, sem setters.
 *  - O enum StatusModulo centraliza os possíveis estados, evitando
 *    strings mágicas espalhadas pelo código.
 *
 * Para adicionar um novo item na home, basta incluir uma entrada em
 * HomeMenuConfig.java — nenhuma outra classe precisa ser alterada.
 */
public final class HomeMenuItem {

    // ─────────────────────────────────────────────────────────────
    // Status disponíveis para um módulo
    // ─────────────────────────────────────────────────────────────

    public enum StatusModulo {
        /** Funcionalidade ativa e disponível para uso */
        EM_TESTE,

        /** Funcionalidade em construção, parcialmente disponível */
        EM_DESENVOLVIMENTO,

        /** Funcionalidade ainda não disponível */
        EM_BREVE
    }

    // ─────────────────────────────────────────────────────────────
    // Campos
    // ─────────────────────────────────────────────────────────────

    @DrawableRes
    private final int iconeRes;

    @StringRes
    private final int tituloRes;

    private final StatusModulo status;

    /**
     * Quando true, o card fica clicável normalmente.
     * Quando false, o click é interceptado pelo HomeActivity
     * e exibe o Toast "Disponível em breve".
     */
    private final boolean habilitado;

    // ─────────────────────────────────────────────────────────────
    // Construtor
    // ─────────────────────────────────────────────────────────────

    public HomeMenuItem(
            @DrawableRes int iconeRes,
            @StringRes int tituloRes,
            StatusModulo status,
            boolean habilitado) {

        this.iconeRes   = iconeRes;
        this.tituloRes  = tituloRes;
        this.status     = status;
        this.habilitado = habilitado;
    }

    // ─────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────

    @DrawableRes
    public int getIconeRes() { return iconeRes; }

    @StringRes
    public int getTituloRes() { return tituloRes; }

    public StatusModulo getStatus() { return status; }

    public boolean isHabilitado() { return habilitado; }
}