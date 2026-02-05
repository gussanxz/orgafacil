package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos;

import java.io.Serializable;

public class MovimentacaoModel implements Serializable {
    private String data;
    private String hora;
    private String categoria;
    private String descricao;
    private String tipo;      // "d" despesa | "r" receita
    private double valor;
    private String key;
    private String mesAno;

    public MovimentacaoModel() {}

    // --- Mantenha apenas os GETTERS e SETTERS ---
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}