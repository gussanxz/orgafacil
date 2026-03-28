package com.gussanxz.orgafacil.funcionalidades.mercado.dados.model;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Model de dados de um item da Lista de Mercado.
 *
 * RN01 – todos os valores financeiros são armazenados em CENTAVOS (int).
 * A conversão para exibição (÷ 100) ocorre exclusivamente na camada de apresentação.
 */
public class ItemMercadoModel {

    // ID do documento no Firestore — não persiste como campo (é o doc ID)
    private String    firestoreId;

    private String    nome;
    private String    categoria;
    private int       valorCentavos;   // RN01 – ex: R$ 25,90 → 2590
    private int       quantidade;
    private boolean   noCarrinho;
    private Timestamp criadoEm;
    private Timestamp atualizadoEm;

    // ─── Construtor vazio obrigatório para Firestore ──────────────────────────
    public ItemMercadoModel() {}

    public ItemMercadoModel(String nome, String categoria,
                            int valorCentavos, int quantidade) {
        this.nome          = nome;
        this.categoria     = categoria;
        this.valorCentavos = valorCentavos;
        this.quantidade    = quantidade;
        this.noCarrinho    = false;
        this.criadoEm      = Timestamp.now();
        this.atualizadoEm  = Timestamp.now();
    }

    // ─── Serialização para o Firestore ───────────────────────────────────────

    /** Mapa completo para inserção de um novo item. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("nome",          nome);
        map.put("categoria",     categoria);
        map.put("valorCentavos", valorCentavos);   // RN01 – persiste em centavos
        map.put("quantidade",    quantidade);
        map.put("noCarrinho",    noCarrinho);
        map.put("criadoEm",      criadoEm != null ? criadoEm : Timestamp.now());
        map.put("atualizadoEm",  Timestamp.now());
        return map;
    }

    /** Mapa parcial para atualizar só o status "no carrinho" (RF02). */
    public Map<String, Object> toMapCarrinho() {
        Map<String, Object> map = new HashMap<>();
        map.put("noCarrinho",   noCarrinho);
        map.put("atualizadoEm", Timestamp.now());
        return map;
    }

    /** Mapa parcial para atualizar valor e quantidade inline (RF01). */
    public Map<String, Object> toMapValorQtd() {
        Map<String, Object> map = new HashMap<>();
        map.put("valorCentavos", valorCentavos);
        map.put("quantidade",    quantidade);
        map.put("atualizadoEm",  Timestamp.now());
        return map;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getFirestoreId()                     { return firestoreId; }
    public void   setFirestoreId(String firestoreId)   { this.firestoreId = firestoreId; }

    public String getNome()                            { return nome; }
    public void   setNome(String nome)                 { this.nome = nome; }

    public String getCategoria()                       { return categoria; }
    public void   setCategoria(String categoria)       { this.categoria = categoria; }

    /** RN01 – valor em centavos (int). */
    public int  getValorCentavos()                     { return valorCentavos; }
    public void setValorCentavos(int valorCentavos)    { this.valorCentavos = valorCentavos; }

    public int  getQuantidade()                        { return quantidade; }
    public void setQuantidade(int quantidade)          { this.quantidade = quantidade; }

    public boolean isNoCarrinho()                      { return noCarrinho; }
    public void    setNoCarrinho(boolean noCarrinho)   { this.noCarrinho = noCarrinho; }

    public Timestamp getCriadoEm()                     { return criadoEm; }
    public void      setCriadoEm(Timestamp criadoEm)   { this.criadoEm = criadoEm; }

    public Timestamp getAtualizadoEm()                 { return atualizadoEm; }
    public void setAtualizadoEm(Timestamp atualizadoEm){ this.atualizadoEm = atualizadoEm; }

    /**
     * RN01 – subtotal em centavos.
     * Retorna long para detectar overflow antes da verificação (CT04).
     */
    public long getSubtotalCentavos() {
        return (long) valorCentavos * quantidade;
    }
}