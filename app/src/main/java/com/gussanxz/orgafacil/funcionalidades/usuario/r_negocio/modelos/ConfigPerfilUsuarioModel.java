package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Representa os dados básicos e preferências do usuário.
 */
public class ConfigPerfilUsuarioModel {
    private String idUsuario;
    private String nome;
    private String email;
    private String fotoUrl;

    // --- CAMPOS DE STATUS E SOFT DELETE ---
    private StatusConta status;
    private Object dataDesativacao;

    // --- NOVOS CAMPOS: FUSO E VERSÃO ---
    private String fusoHorario; // Identificador como "America/Manaus"
    private String versaoApp;   // Versão capturada do sistema (ex: 1.0.2)

    // --- AUDITORIA E CONSENTIMENTO ---
    private boolean aceitouTermos;
    private Object dataAceite;
    private Object ultimaAtividade;
    private String versaoTermos;

    public enum TipoPerfil { PESSOAL, NEGOCIOS }
    public enum PlanoAtivo { GRATUITO, PREMIUM }
    public enum StatusConta { ATIVO, SUSPENSO, PENDENTE_EXCLUSAO, DESATIVADO }

    public ConfigPerfilUsuarioModel() {
        this.aceitouTermos = false;
        this.status = StatusConta.ATIVO;
    }

    // --- GETTERS E SETTERS ---

    @Exclude
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public StatusConta getStatus() { return status; }
    public void setStatus(StatusConta status) { this.status = status; }

    @ServerTimestamp
    public Object getDataDesativacao() { return dataDesativacao; }
    public void setDataDesativacao(Object dataDesativacao) { this.dataDesativacao = dataDesativacao; }

    // --- GETTERS E SETTERS: NOVOS CAMPOS ---
    public String getFusoHorario() { return fusoHorario; }
    public void setFusoHorario(String fusoHorario) { this.fusoHorario = fusoHorario; }

    public String getVersaoApp() { return versaoApp; }
    public void setVersaoApp(String versaoApp) { this.versaoApp = versaoApp; }

    public boolean isAceitouTermos() { return aceitouTermos; }
    public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

    @ServerTimestamp
    public Object getDataAceite() { return dataAceite; }
    public void setDataAceite(Object dataAceite) { this.dataAceite = dataAceite; }

    public String getVersaoTermos() { return versaoTermos; }
    public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }

    @ServerTimestamp
    public Object getUltimaAtividade() { return ultimaAtividade; }
    public void setUltimaAtividade(Object ultimaAtividade) { this.ultimaAtividade = ultimaAtividade; }
}