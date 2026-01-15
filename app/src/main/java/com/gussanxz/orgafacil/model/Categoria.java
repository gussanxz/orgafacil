package com.gussanxz.orgafacil.model;

import java.io.Serializable;

public class Categoria implements Serializable {
    
    private String id;
    private String nome;
    private String descricao;
    private int indexIcone;
    private boolean ativa;

    public Categoria() {}
    
    public Categoria (String id, String nome, String descricao, int indexIcone, boolean estaAtiva) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.indexIcone = indexIcone;
        this.ativa = estaAtiva;
    }

    //ID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    //NOME
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    //DESCRIÇÃO
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    //ICONE
    public int getIndexIcone() {
        return indexIcone;
    }

    //ATIVA
    public boolean isAtiva() {
        return ativa;
    }

    public void setIndexIcone(int indexIcone) {
        this.indexIcone = indexIcone;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}