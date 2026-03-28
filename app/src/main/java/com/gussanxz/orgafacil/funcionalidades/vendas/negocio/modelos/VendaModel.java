package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VendaModel implements Serializable {
    // Status
    public static final String STATUS_EM_ABERTO  = "EM_ABERTO";
    public static final String STATUS_FINALIZADA = "FINALIZADA";
    public static final String STATUS_CANCELADA  = "CANCELADA";

    // Forma de pagamento
    public static final String PAGAMENTO_PIX     = "PIX";
    public static final String PAGAMENTO_DINHEIRO = "Dinheiro";
    public static final String PAGAMENTO_CREDITO  = "Crédito";
    public static final String PAGAMENTO_DEBITO   = "Débito";
    private String diaKey;
    private String id;
    private int numeroVenda;
    private long dataHoraAberturaMillis;
    private long dataHoraFechamentoMillis;
    private String formaPagamento;
    private int quantidadeTotal;
    private double valorTotal;
    private double acrescimo = 0.0;
    private double desconto = 0.0;
    private String status;
    private List<ItemVendaRegistradaModel> itens = new ArrayList<>();

    public VendaModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getDataHoraFechamentoMillis() {
        return dataHoraFechamentoMillis;
    }

    public void setDataHoraFechamentoMillis(long dataHoraFechamentoMillis) {
        this.dataHoraFechamentoMillis = dataHoraFechamentoMillis;
    }

    public long getDataHoraAberturaMillis() {
        return dataHoraAberturaMillis;
    }

    public void setDataHoraAberturaMillis(long dataHoraAberturaMillis) {
        this.dataHoraAberturaMillis = dataHoraAberturaMillis;
    }

    public int getNumeroVenda() {
        return numeroVenda;
    }

    public void setNumeroVenda(int numeroVenda) {
        this.numeroVenda = numeroVenda;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(int quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ItemVendaRegistradaModel> getItens() {
        return itens;
    }

    public void setItens(List<ItemVendaRegistradaModel> itens) {
        this.itens = itens;
    }

    public double getAcrescimo() { return acrescimo; }
    public void setAcrescimo(double acrescimo) { this.acrescimo = acrescimo; }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; }
    public String getDiaKey() { return diaKey; }
    public void setDiaKey(String diaKey) { this.diaKey = diaKey; }

}