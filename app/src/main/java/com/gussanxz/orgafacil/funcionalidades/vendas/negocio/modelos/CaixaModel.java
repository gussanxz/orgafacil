package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class CaixaModel implements Serializable {

    public static final String STATUS_ABERTO  = "ABERTO";
    public static final String STATUS_FECHADO = "FECHADO";
    /** ID reservado para vendas feitas antes da implantação do fluxo de caixa. */
    public static final String ID_LEGADO      = "caixa_0";

    private String id;
    private String status;
    private long   abertoEmMillis;
    private long   fechadoEmMillis;
    private String diaKey;
    private String mesKey;
    private String observacao;
    /** Quando true, permite adicionar vendas a este caixa mesmo após fechado (lançamento tardio). */
    private boolean permiteLancamentoTardio;
    /** Snapshot salvo no fechamento: quantidade de vendas finalizadas. */
    private int    qtdVendasFechamento;
    /** Snapshot salvo no fechamento: valor total das vendas finalizadas (R$). */
    private double valorTotalFechamento;

    public CaixaModel() {}

    // ── Getters & Setters ──────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAbertoEmMillis() { return abertoEmMillis; }
    public void setAbertoEmMillis(long abertoEmMillis) { this.abertoEmMillis = abertoEmMillis; }

    public long getFechadoEmMillis() { return fechadoEmMillis; }
    public void setFechadoEmMillis(long fechadoEmMillis) { this.fechadoEmMillis = fechadoEmMillis; }

    public String getDiaKey() { return diaKey; }
    public void setDiaKey(String diaKey) { this.diaKey = diaKey; }

    public String getMesKey() { return mesKey; }
    public void setMesKey(String mesKey) { this.mesKey = mesKey; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    @PropertyName("permiteLancamentoTardio")
    public boolean isPermiteLancamentoTardio() { return permiteLancamentoTardio; }
    @PropertyName("permiteLancamentoTardio")
    public void setPermiteLancamentoTardio(boolean permiteLancamentoTardio) {
        this.permiteLancamentoTardio = permiteLancamentoTardio;
    }

    public int    getQtdVendasFechamento()  { return qtdVendasFechamento; }
    public void   setQtdVendasFechamento(int qtdVendasFechamento) { this.qtdVendasFechamento = qtdVendasFechamento; }

    public double getValorTotalFechamento() { return valorTotalFechamento; }
    public void   setValorTotalFechamento(double valorTotalFechamento) { this.valorTotalFechamento = valorTotalFechamento; }

    // ── Helpers ────────────────────────────────────────────────────────

    public boolean isAberto()  { return STATUS_ABERTO.equals(status); }
    public boolean isFechado() { return STATUS_FECHADO.equals(status); }
    public boolean isLegado()  { return ID_LEGADO.equals(id); }
}
