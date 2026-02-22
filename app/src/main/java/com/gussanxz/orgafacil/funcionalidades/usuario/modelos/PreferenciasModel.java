package com.gussanxz.orgafacil.funcionalidades.usuario.modelos;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

/**
 * Modelo para as configurações de interface (UI) e comportamento do app.
 * Caminho no Firestore: users/{uid}/config/preferencias
 * Estrutura:
 * - visual: { tema, esconderSaldo }
 * - regional: { moeda, fusoHorario }
 * - funcionalidades: { notificacoesHabilitadas }
 */
public class PreferenciasModel implements Serializable {

    // --- CONSTANTES DE VALORES ---
    public static final String TEMA_SISTEMA = "SISTEMA";
    public static final String TEMA_CLARO = "CLARO";
    public static final String TEMA_ESCURO = "ESCURO";

    // --- CONSTANTES DE CAMINHO (Dot Notation para Updates) ---
    public static final String CAMPO_TEMA = "visual.tema";
    public static final String CAMPO_ESCONDER_SALDO = "visual.esconderSaldo";
    public static final String CAMPO_MOEDA = "regional.moeda";
    public static final String CAMPO_FUSO = "regional.fusoHorario";
    public static final String CAMPO_NOTIFICACOES = "funcionalidades.notificacoesHabilitadas";
    public static final String CAMPO_DATA_ATUALIZACAO = "dataAtualizacao";

    // =========================================================================
    // GRUPOS DE DADOS (Mapas)
    // =========================================================================
    private Visual visual;
    private Regional regional;
    private Funcionalidades funcionalidades;

    // Metadados na raiz
    @ServerTimestamp
    private Date dataAtualizacao;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public PreferenciasModel() {
        // Inicializa os grupos com os valores padrão do App
        this.visual = new Visual();
        this.regional = new Regional();
        this.funcionalidades = new Funcionalidades();
    }

    // =========================================================================
    // GETTERS E SETTERS DA RAIZ
    // =========================================================================

    public Visual getVisual() { return visual; }
    public void setVisual(Visual visual) { this.visual = visual; }

    public Regional getRegional() { return regional; }
    public void setRegional(Regional regional) { this.regional = regional; }

    public Funcionalidades getFuncionalidades() { return funcionalidades; }
    public void setFuncionalidades(Funcionalidades funcionalidades) { this.funcionalidades = funcionalidades; }

    public Date getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(Date dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    // =========================================================================
    // CLASSES INTERNAS (Mapas do Firestore)
    // =========================================================================

    public static class Visual implements Serializable {
        private String tema;
        private boolean esconderSaldo;

        public Visual() {
            // Padrões Visuais
            this.tema = TEMA_SISTEMA;
            this.esconderSaldo = false;
        }

        public String getTema() { return tema; }
        public void setTema(String tema) { this.tema = tema; }
        public boolean isEsconderSaldo() { return esconderSaldo; }
        public void setEsconderSaldo(boolean esconderSaldo) { this.esconderSaldo = esconderSaldo; }
    }

    public static class Regional implements Serializable {
        private String moeda;
        private String fusoHorario;

        public Regional() {
            // Padrões Regionais
            this.moeda = "BRL";
            this.fusoHorario = "America/Manaus"; // Ou null para pegar do sistema depois
        }

        public String getMoeda() { return moeda; }
        public void setMoeda(String moeda) { this.moeda = moeda; }
        public String getFusoHorario() { return fusoHorario; }
        public void setFusoHorario(String fusoHorario) { this.fusoHorario = fusoHorario; }
    }

    public static class Funcionalidades implements Serializable {
        private boolean notificacoesHabilitadas;

        public Funcionalidades() {
            // Padrões de Comportamento
            this.notificacoesHabilitadas = true;
        }

        public boolean isNotificacoesHabilitadas() { return notificacoesHabilitadas; }
        public void setNotificacoesHabilitadas(boolean notificacoesHabilitadas) { this.notificacoesHabilitadas = notificacoesHabilitadas; }
    }
}