package com.gussanxz.orgafacil.funcionalidades.usuario.modelos;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

/**
 * UsuarioModel
 * Estrutura aninhada (Mapas) com Constantes de Caminho (Dot Notation).
 */
public class UsuarioModel implements Serializable {

    // =========================================================================
    // CONSTANTES DE MAPEAMENTO (Blindagem do Banco)
    // Atualizadas para apontar para os caminhos corretos dentro dos mapas.
    // Use isso no Repository para fazer updates parciais com segurança.
    // =========================================================================
    public static final String CAMPO_UID = "uid";

    // Caminhos para Dados Pessoais
    public static final String CAMPO_NOME = "dadosPessoais.nome";
    public static final String CAMPO_EMAIL = "dadosPessoais.email";
    public static final String CAMPO_FOTO = "dadosPessoais.fotoUrl";
    public static final String CAMPO_PROVIDER = "dadosPessoais.providerId";

    // Caminhos para Conta
    public static final String CAMPO_STATUS = "dadosConta.status";
    public static final String CAMPO_PLANO = "dadosConta.planoAtivo";
    public static final String CAMPO_TIPO_PERFIL = "dadosConta.tipoPerfil";
    public static final String CAMPO_DATA_CRIACAO = "dadosConta.dataCriacao";
    public static final String CAMPO_DATA_DESATIVACAO = "dadosConta.dataDesativacao";

    // Caminhos para App
    public static final String CAMPO_VERSAO_APP = "dadosApp.versaoApp";
    public static final String CAMPO_ULTIMA_ATIVIDADE = "dadosApp.ultimaAtividade";

    // Caminhos para Termos
    public static final String CAMPO_ACEITOU_TERMOS = "termosUso.aceitouTermos";
    public static final String CAMPO_VERSAO_TERMOS = "termosUso.versaoTermos";
    public static final String CAMPO_DATA_ACEITE = "termosUso.dataAceite";


    // =========================================================================
    // ATRIBUTOS PRINCIPAIS (Raiz do Documento)
    // =========================================================================
    private String uid;

    // Grupos de Dados (Mapas)
    private DadosPessoais dadosPessoais;
    private DadosConta dadosConta;
    private TermosUso termosUso;
    private DadosApp dadosApp;

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    public UsuarioModel() {}

    public UsuarioModel(String uid, String nome, String email, String providerId) {
        this.uid = uid;

        // Inicializa os sub-objetos imediatamente
        this.dadosPessoais = new DadosPessoais(nome, email, providerId);
        this.dadosConta = new DadosConta(); // Já nasce com defaults (ATIVO, GRATUITO)
        this.termosUso = new TermosUso();
        this.dadosApp = new DadosApp();
    }

    // =========================================================================
    // GETTERS E SETTERS DA RAIZ
    // =========================================================================

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public DadosPessoais getDadosPessoais() { return dadosPessoais; }
    public void setDadosPessoais(DadosPessoais dadosPessoais) { this.dadosPessoais = dadosPessoais; }

    public DadosConta getDadosConta() { return dadosConta; }
    public void setDadosConta(DadosConta dadosConta) { this.dadosConta = dadosConta; }

    public TermosUso getTermosUso() { return termosUso; }
    public void setTermosUso(TermosUso termosUso) { this.termosUso = termosUso; }

    public DadosApp getDadosApp() { return dadosApp; }
    public void setDadosApp(DadosApp dadosApp) { this.dadosApp = dadosApp; }

    // =========================================================================
    // CLASSES INTERNAS (Mapeamento dos Grupos/Mapas)
    // =========================================================================

    public static class DadosPessoais implements Serializable {
        private String nome;
        private String email;
        private String fotoUrl;
        private String providerId;

        public DadosPessoais() {}

        public DadosPessoais(String nome, String email, String providerId) {
            this.nome = nome;
            this.email = email;
            this.providerId = providerId;
        }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFotoUrl() { return fotoUrl; }
        public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
    }

    public static class DadosConta implements Serializable {
        private String status;
        private String planoAtivo;
        private String tipoPerfil; // PESSOAL ou EMPRESARIAL

        @ServerTimestamp private Date dataCriacao;
        @ServerTimestamp private Date dataDesativacao;

        public DadosConta() {
            this.status = "ATIVO";
            this.planoAtivo = "GRATUITO";
            this.tipoPerfil = "PESSOAL";
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPlanoAtivo() { return planoAtivo; }
        public void setPlanoAtivo(String planoAtivo) { this.planoAtivo = planoAtivo; }
        public String getTipoPerfil() { return tipoPerfil; }
        public void setTipoPerfil(String tipoPerfil) { this.tipoPerfil = tipoPerfil; }

        public Date getDataCriacao() { return dataCriacao; }
        public void setDataCriacao(Date dataCriacao) { this.dataCriacao = dataCriacao; }
        public Date getDataDesativacao() { return dataDesativacao; }
        public void setDataDesativacao(Date dataDesativacao) { this.dataDesativacao = dataDesativacao; }
    }

    public static class TermosUso implements Serializable {
        private boolean aceitouTermos;
        private String versaoTermos;
        @ServerTimestamp private Date dataAceite;

        public TermosUso() {}

        @PropertyName("aceitouTermos") // Garante o nome exato no banco
        public boolean isAceitouTermos() { return aceitouTermos; }
        @PropertyName("aceitouTermos")
        public void setAceitouTermos(boolean aceitouTermos) { this.aceitouTermos = aceitouTermos; }

        public String getVersaoTermos() { return versaoTermos; }
        public void setVersaoTermos(String versaoTermos) { this.versaoTermos = versaoTermos; }

        public Date getDataAceite() { return dataAceite; }
        public void setDataAceite(Date dataAceite) { this.dataAceite = dataAceite; }
    }

    public static class DadosApp implements Serializable {
        private String versaoApp;
        @ServerTimestamp private Date ultimaAtividade;

        public DadosApp() {}

        public String getVersaoApp() { return versaoApp; }
        public void setVersaoApp(String versaoApp) { this.versaoApp = versaoApp; }

        public Date getUltimaAtividade() { return ultimaAtividade; }
        public void setUltimaAtividade(Date ultimaAtividade) { this.ultimaAtividade = ultimaAtividade; }
    }
}