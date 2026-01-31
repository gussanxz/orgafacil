package com.gussanxz.orgafacil.funcionalidades.configuracoes.negocio.modelos;

import com.google.firebase.firestore.Exclude;

/**
 * CLASSE DE MODELO (ENTITY)
 * Representa o "O que é" um usuário no sistema OrgaFacil.
 * Esta classe é "burra": ela não sabe que o Firebase existe.
 */
public class Usuario {

    private String idUsuario;
    private String nome;
    private String email;
    private String senha;

    // Campos para cálculos locais de dashboard
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    // Construtor vazio: Obrigatório para o Firebase converter o documento em objeto
    public Usuario() {}

    // --- GETTERS E SETTERS ---

    @Exclude // O ID do documento não precisa ser repetido dentro do JSON do Firestore
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Exclude // Senha é sensível e nunca deve ser salva no banco de dados comum
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public Double getProventosTotal() { return proventosTotal; }
    public void setProventosTotal(Double proventosTotal) { this.proventosTotal = proventosTotal; }

    public Double getDespesaTotal() { return despesaTotal; }
    public void setDespesaTotal(Double despesaTotal) { this.despesaTotal = despesaTotal; }
}