package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.ServerTimestamp;

/**
 * Modelo para as configurações de interface (UI) e comportamento do app.
 * Caminho no Firestore: users/{uid}/config/preferencias
 */
public class PreferenciasModel {

    // --- CONSTANTES PARA O TEMA ---
    public static final String TEMA_SISTEMA = "SISTEMA";
    public static final String TEMA_CLARO = "CLARO";
    public static final String TEMA_ESCURO = "ESCURO";

    // --- DADOS DE VISUALIZAÇÃO ---
    private String tema;            // "CLARO", "ESCURO", "SISTEMA"
    private String moeda;           // "BRL", "USD"
    private String fusoHorario;     // "America/Manaus"
    private boolean esconderSaldo;

    // --- COMPORTAMENTO ---
    private boolean notificacoesHabilitadas;

    // --- METADADOS ---
    private Object dataAtualizacao; // Renomeado de updatedAt

    // =========================================================================
    // CONSTRUTOR VAZIO (Define os padrões do App)
    // =========================================================================
    public PreferenciasModel() {
        this.tema = TEMA_SISTEMA;
        this.moeda = "BRL";
        this.esconderSaldo = false;
        this.notificacoesHabilitadas = true;
    }

    // --- GETTERS E SETTERS ---

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public String getFusoHorario() { return fusoHorario; }
    public void setFusoHorario(String fusoHorario) { this.fusoHorario = fusoHorario; }

    public boolean isEsconderSaldo() { return esconderSaldo; }
    public void setEsconderSaldo(boolean esconderSaldo) { this.esconderSaldo = esconderSaldo; }

    public boolean isNotificacoesHabilitadas() { return notificacoesHabilitadas; }
    public void setNotificacoesHabilitadas(boolean notificacoesHabilitadas) { this.notificacoesHabilitadas = notificacoesHabilitadas; }

    @ServerTimestamp
    public Object getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(Object dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}