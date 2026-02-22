package com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos;

import java.io.Serializable;

public class Categoria implements Serializable {

    // DEFINIÇÃO DOS TIPOS PERMITIDOS
    // Garante que só se use esses valores no código
    public enum Tipo {
        RECEITA,  // Entradas financeiras
        DESPESA,  // Saídas financeiras
        PRODUTO,  // Itens de estoque/venda
        SERVICO   // Mão de obra
    }
    
    private String id;
    private String nome;
    private String descricao;
    private int indexIcone;
    private String urlImagem;
    private boolean ativa;
    private String tipo; // Salva como String no banco ("RECEITA" ou "DESPESA")

    public Categoria() {}

    public Categoria(String id, String nome, String descricao, int indexIcone, String urlImagem, boolean estaAtiva, Tipo tipoEnum) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.indexIcone = indexIcone;
        this.urlImagem = urlImagem;
        this.ativa = estaAtiva;
        this.tipo = tipoEnum.toString();

    }

    // --- GETTERS E SETTERS ---

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
    public void setIndexIcone(int indexIcone) {
        this.indexIcone = indexIcone;
    }


    //ATIVA
    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
    public boolean isAtiva() {
        return ativa;
    }


    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUrlImagem() {
        return urlImagem;
    }

    public void setUrlImagem(String urlImagem) {
        this.urlImagem = urlImagem;
    }

    // Auxiliar para Enum
    public void setTipoEnum(Tipo tipoEnum) {
        this.tipo = tipoEnum.toString();
    }
}
