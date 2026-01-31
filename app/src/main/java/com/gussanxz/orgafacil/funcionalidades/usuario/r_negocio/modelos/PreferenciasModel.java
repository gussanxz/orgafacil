package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

/**
 * Modelo para as configurações de interface e uso do app.
 */
public class PreferenciasModel {
    private String tema; // "CLARO", "ESCURO", "SISTEMA"
    private String moeda; // "BRL"
    private boolean esconderSaldo;
    private long updatedAt;

    // Construtor vazio para o Firebase
    public PreferenciasModel() {
    }

    public PreferenciasModel(String tema, String moeda, boolean esconderSaldo) {
        this.tema = tema;
        this.moeda = moeda;
        this.esconderSaldo = esconderSaldo;
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters e Setters
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public boolean isEsconderSaldo() { return esconderSaldo; }
    public void setEsconderSaldo(boolean esconderSaldo) { this.esconderSaldo = esconderSaldo; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}