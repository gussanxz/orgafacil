package com.gussanxz.orgafacil.funcionalidades.boletos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class BoletoModel {
    private String id;
    private String descricao;
    private String codigoBarras;
    private long valor; // em centavos
    private Timestamp dataVencimento;
    private boolean pago = false;
    private String movimentacaoId; // id gerado após criar a despesa em Contas

    @ServerTimestamp
    private Timestamp dataCriacao;

    public BoletoModel() {}

    // getters e setters para todos os campos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public long getValor() { return valor; }
    public void setValor(long valor) { this.valor = valor; }
    public Timestamp getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(Timestamp dataVencimento) { this.dataVencimento = dataVencimento; }
    public boolean isPago() { return pago; }
    public void setPago(boolean pago) { this.pago = pago; }
    public String getMovimentacaoId() { return movimentacaoId; }
    public void setMovimentacaoId(String movimentacaoId) { this.movimentacaoId = movimentacaoId; }
    public Timestamp getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Timestamp dataCriacao) { this.dataCriacao = dataCriacao; }
}