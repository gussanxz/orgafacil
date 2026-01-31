package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.Exclude;

/**
 * Representa os dados básicos e preferências do usuário.
 * Salvo em: teste > UID > config > config_perfil_usuario
 */
public class ConfigPerfilUsuarioModel {
    private String idUsuario;
    private String nome;
    private String email;
    private String fotoUrl;

    // --- ENUMS DE NEGÓCIO INTERNOS ---
    public enum TipoPerfil { PESSOAL, NEGOCIOS }
    public enum PlanoAtivo { GRATUITO, PREMIUM }
    public enum StatusConta { ATIVO, SUSPENSO, PENDENTE_EXCLUSAO }

    public ConfigPerfilUsuarioModel() {}

    @Exclude
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}