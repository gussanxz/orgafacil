package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // [CORREÇÃO] Import necessário para o ServerTimestamp funcionar

public class UsuarioModel {

    // --- IDENTIDADE ---
    private String uid;
    private String nome;
    private String email;
    private String fotoUrl;
    private String providerId;

    // --- STATUS & PLANO ---
    private StatusConta status;
    private TipoPerfil tipoPerfil;
    private PlanoAtivo planoAtivo;

    // --- FLUXO & SISTEMA ---
    private String versaoApp;

    // --- AUDITORIA ---
    private boolean aceitouTermos;
    private String versaoTermos;

    // [CORREÇÃO] Mudança de Object para Date para evitar o crash
    private Date dataAceite;
    private Date dataCriacaoConta;
    private Date ultimaAtividade;
    private Date dataDesativacaoConta;

    // --- ENUMS ---
    public enum TipoPerfil { PESSOAL, EMPRESARIAL }
    public enum PlanoAtivo { GRATUITO, PREMIUM }
    public enum StatusConta { ATIVO, DESATIVADO }

    // =========================================================================
    // CONSTRUTOR 1: OBRIGATÓRIO PARA O FIRESTORE (Não mexa aqui)
    // O Firestore usa isso para ler o banco. Não colocamos lógica de negócio aqui.
    // =========================================================================
    public UsuarioModel() {
    }

    // =========================================================================
    // CONSTRUTOR 2: PARA CRIAÇÃO DE NOVO USUÁRIO (Use este no seu código)
    // Aqui você obriga a passar os dados reais.
    // =========================================================================
    public UsuarioModel(String uid, String nome, String email, String providerId) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.providerId = providerId;

        // VALORES INICIAIS PADRÃO (Regra de Negócio)
        // Todo usuário novo começa como Ativo, Pessoal e Gratuito até que mude.
        this.status = StatusConta.ATIVO;
        this.tipoPerfil = TipoPerfil.PESSOAL;
        this.planoAtivo = PlanoAtivo.GRATUITO;

        // Termos iniciam false até ele clicar em "Aceitar"
        this.aceitouTermos = false;
    }

    // --- GETTERS E SETTERS ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public StatusConta getStatus() { return status; }
    public void setStatus(StatusConta status) { this.status = status; }

    public TipoPerfil getTipoPerfil() { return tipoPerfil; }
    public void setTipoPerfil(TipoPerfil tipoPerfil) { this.tipoPerfil = tipoPerfil; }

    public PlanoAtivo getPlanoAtivo() { return planoAtivo; }
    public void setPlanoAtivo(PlanoAtivo planoAtivo) { this.planoAtivo = planoAtivo; }

    public String getVersaoApp() { return versaoApp; }
    public void setVersaoApp(String versaoApp) { this.versaoApp = versaoApp; }

    public boolean isAceitouTermos() { return aceitouTermos; }
    public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

    public String getVersaoTermos() { return versaoTermos; }
    public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }

    // [CORREÇÃO] Getters e Setters alterados para receber/retornar Date
    @ServerTimestamp
    public Date getDataAceite() { return dataAceite; }
    public void setDataAceite(Date dataAceite) { this.dataAceite = dataAceite; }

    @ServerTimestamp
    public Date getDataCriacaoConta() { return dataCriacaoConta; }
    public void setDataCriacaoConta(Date dataCriacaoConta) { this.dataCriacaoConta = dataCriacaoConta; }

    @ServerTimestamp
    public Date getUltimaAtividade() { return ultimaAtividade; }
    public void setUltimaAtividade(Date ultimaAtividade) { this.ultimaAtividade = ultimaAtividade; }

    @ServerTimestamp
    public Date getDataDesativacaoConta() { return dataDesativacaoConta; }
    public void setDataDesativacaoConta(Date dataDesativacaoConta) { this.dataDesativacaoConta = dataDesativacaoConta; }
}