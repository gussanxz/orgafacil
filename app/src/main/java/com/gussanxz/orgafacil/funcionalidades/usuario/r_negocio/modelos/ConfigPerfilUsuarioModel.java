package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Representa os dados básicos e preferências do usuário.
 * Salvo em: teste > UID > config > config_perfil_usuario
 */
public class ConfigPerfilUsuarioModel {
    private String idUsuario;
    private String nome;
    private String email;
    private String fotoUrl;

    // --- CAMPOS DE STATUS E SOFT DELETE ---
    private StatusConta status;
    private Object dataDesativacao;

    // --- NOVOS CAMPOS DE AUDITORIA E CONSENTIMENTO ---
    private boolean aceitouTermos;
    private Object dataAceite; // Usamos Object para suportar FieldValue.serverTimestamp()
    private String versaoTermos;

    // --- ENUMS DE NEGÓCIO INTERNOS ---
    public enum TipoPerfil { PESSOAL, NEGOCIOS }
    public enum PlanoAtivo { GRATUITO, PREMIUM }

    // Atualizado para incluir o estado de DESATIVADO
    public enum StatusConta { ATIVO, SUSPENSO, PENDENTE_EXCLUSAO, DESATIVADO }

    public ConfigPerfilUsuarioModel() {
        // Por padrão, garantimos que comece como falso até que o fluxo de aceite ocorra
        this.aceitouTermos = false;
        // Toda conta nova nasce com status ATIVO
        this.status = StatusConta.ATIVO;
    }

    // --- GETTERS E SETTERS BÁSICOS ---

    @Exclude
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    // --- GETTERS E SETTERS DE STATUS (Soft Delete) ---

    public StatusConta getStatus() { return status; }
    public void setStatus(StatusConta status) { this.status = status; }

    @ServerTimestamp // Gera o carimbo de data automaticamente no servidor Firebase
    public Object getDataDesativacao() { return dataDesativacao; }
    public void setDataDesativacao(Object dataDesativacao) { this.dataDesativacao = dataDesativacao; }

    // --- GETTERS E SETTERS DOS TERMOS ---

    public boolean isAceitouTermos() { return aceitouTermos; }
    public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

    @ServerTimestamp
    public Object getDataAceite() { return dataAceite; }
    public void setDataAceite(Object dataAceite) { this.dataAceite = dataAceite; }

    public String getVersaoTermos() { return versaoTermos; }
    public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }
}