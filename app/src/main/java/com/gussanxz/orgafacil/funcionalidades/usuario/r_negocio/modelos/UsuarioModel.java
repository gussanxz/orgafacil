package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

/**
 * UsuarioModel
 * Representa a Identidade e Auditoria do usuário no Firestore.
 */
public class UsuarioModel implements Serializable {

    // =========================================================================
    // CONSTANTES DE MAPEAMENTO (Blindagem do Banco)
    // =========================================================================
    public static final String CAMPO_UID = "uid";
    public static final String CAMPO_NOME = "nome";
    public static final String CAMPO_EMAIL = "email";
    public static final String CAMPO_FOTO = "fotoUrl";
    public static final String CAMPO_PROVIDER = "providerId";
    public static final String CAMPO_TIPO_PERFIL = "tipoPerfil";
    public static final String CAMPO_PLANO = "planoAtivo";
    public static final String CAMPO_STATUS = "status";
    public static final String CAMPO_ACEITOU_TERMOS = "aceitouTermos";
    public static final String CAMPO_VERSAO_APP = "versaoApp";
    public static final String CAMPO_VERSAO_TERMOS = "versaoTermos";
    public static final String CAMPO_DATA_CRIACAO = "dataCriacaoConta";
    public static final String CAMPO_DATA_ACEITE = "dataAceite";
    public static final String CAMPO_ULTIMA_ATIVIDADE = "ultimaAtividade";

    // --- IDENTIDADE ---
    private String uid;
    private String nome;
    private String email;
    private String fotoUrl;
    private String providerId;

    // --- STATUS & PLANO (Usando String para persistir o nome do Enum no Firestore) ---
    private String status;
    private String tipoPerfil;
    private String planoAtivo;

    // --- FLUXO & SISTEMA ---
    private String versaoApp;
    private boolean aceitouTermos;
    private String versaoTermos;

    // --- AUDITORIA (Timestamps) ---
    private Date dataAceite;
    private Date dataCriacaoConta;
    private Date ultimaAtividade;
    private Date dataDesativacaoConta;

    // --- ENUMS ---
    public enum TipoPerfil { PESSOAL, EMPRESARIAL }
    public enum PlanoAtivo { GRATUITO, PREMIUM }
    public enum StatusConta { ATIVO, DESATIVADO }

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    // Obrigatório para o Firestore
    public UsuarioModel() {}

    // Para criação de novo usuário
    public UsuarioModel(String uid, String nome, String email, String providerId) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.providerId = providerId;

        // Padrões iniciais conforme regra de negócio
        this.status = StatusConta.ATIVO.name();
        this.tipoPerfil = TipoPerfil.PESSOAL.name();
        this.planoAtivo = PlanoAtivo.GRATUITO.name();
        this.aceitouTermos = false;
    }

    // =========================================================================
    // GETTERS E SETTERS (Com anotações @PropertyName para garantir o mapeamento)
    // =========================================================================

    @PropertyName(CAMPO_UID)
    public String getUid() { return uid; }
    @PropertyName(CAMPO_UID)
    public void setUid(String uid) { this.uid = uid; }

    @PropertyName(CAMPO_NOME)
    public String getNome() { return nome; }
    @PropertyName(CAMPO_NOME)
    public void setNome(String nome) { this.nome = nome; }

    @PropertyName(CAMPO_EMAIL)
    public String getEmail() { return email; }
    @PropertyName(CAMPO_EMAIL)
    public void setEmail(String email) { this.email = email; }

    @PropertyName(CAMPO_FOTO)
    public String getFotoUrl() { return fotoUrl; }
    @PropertyName(CAMPO_FOTO)
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    @PropertyName(CAMPO_PROVIDER)
    public String getProviderId() { return providerId; }
    @PropertyName(CAMPO_PROVIDER)
    public void setProviderId(String providerId) { this.providerId = providerId; }

    @PropertyName(CAMPO_STATUS)
    public String getStatus() { return status; }
    @PropertyName(CAMPO_STATUS)
    public void setStatus(String status) { this.status = status; }

    @PropertyName(CAMPO_TIPO_PERFIL)
    public String getTipoPerfil() { return tipoPerfil; }
    @PropertyName(CAMPO_TIPO_PERFIL)
    public void setTipoPerfil(String tipoPerfil) { this.tipoPerfil = tipoPerfil; }

    @PropertyName(CAMPO_PLANO)
    public String getPlanoAtivo() { return planoAtivo; }
    @PropertyName(CAMPO_PLANO)
    public void setPlanoAtivo(String planoAtivo) { this.planoAtivo = planoAtivo; }

    @PropertyName(CAMPO_VERSAO_APP)
    public String getVersaoApp() { return versaoApp; }
    @PropertyName(CAMPO_VERSAO_APP)
    public void setVersaoApp(String versaoApp) { this.versaoApp = versaoApp; }

    @PropertyName(CAMPO_ACEITOU_TERMOS)
    public boolean isAceitouTermos() { return aceitouTermos; }
    @PropertyName(CAMPO_ACEITOU_TERMOS)
    public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

    @PropertyName(CAMPO_VERSAO_TERMOS)
    public String getVersaoTermos() { return versaoTermos; }
    @PropertyName(CAMPO_VERSAO_TERMOS)
    public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }

    @ServerTimestamp
    @PropertyName(CAMPO_DATA_ACEITE)
    public Date getDataAceite() { return dataAceite; }
    @PropertyName(CAMPO_DATA_ACEITE)
    public void setDataAceite(Date dataAceite) { this.dataAceite = dataAceite; }

    @ServerTimestamp
    @PropertyName(CAMPO_DATA_CRIACAO)
    public Date getDataCriacaoConta() { return dataCriacaoConta; }
    @PropertyName(CAMPO_DATA_CRIACAO)
    public void setDataCriacaoConta(Date dataCriacaoConta) { this.dataCriacaoConta = dataCriacaoConta; }

    @ServerTimestamp
    @PropertyName(CAMPO_ULTIMA_ATIVIDADE)
    public Date getUltimaAtividade() { return ultimaAtividade; }
    @PropertyName(CAMPO_ULTIMA_ATIVIDADE)
    public void setUltimaAtividade(Date ultimaAtividade) { this.ultimaAtividade = ultimaAtividade; }

    @ServerTimestamp
    public Date getDataDesativacaoConta() { return dataDesativacaoConta; }
    public void setDataDesativacaoConta(Date dataDesativacaoConta) { this.dataDesativacaoConta = dataDesativacaoConta; }
}