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

    // --- NOVOS CAMPOS DE AUDITORIA E CONSENTIMENTO ---
    private boolean aceitouTermos;
    private Object dataAceite; // Usamos Object para suportar FieldValue.serverTimestamp()
    private String versaoTermos;

    // --- ENUMS DE NEGÓCIO INTERNOS ---
    public enum TipoPerfil { PESSOAL, NEGOCIOS }
    public enum PlanoAtivo { GRATUITO, PREMIUM }
    public enum StatusConta { ATIVO, SUSPENSO, PENDENTE_EXCLUSAO }

    public ConfigPerfilUsuarioModel() {
        // Por padrão, garantimos que comece como falso até que o fluxo de aceite ocorra
        this.aceitouTermos = false;
    }

    @Exclude
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    // --- GETTERS E SETTERS DOS TERMOS ---

    public boolean isAceitouTermos() { return aceitouTermos; }
    public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

    @ServerTimestamp // Anotação para o Firestore entender que deve gerar a data no servidor
    public Object getDataAceite() { return dataAceite; }
    public void setDataAceite(Object dataAceite) { this.dataAceite = dataAceite; }

    public String getVersaoTermos() { return versaoTermos; }
    public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }
}