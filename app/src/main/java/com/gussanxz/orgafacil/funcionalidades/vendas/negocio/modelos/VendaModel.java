package com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VendaModel implements Serializable {

    public static final String STATUS_FINALIZADA = "FINALIZADA";

    private String id;
    private long dataHoraMillis;
    private String formaPagamento;
    private int quantidadeTotal;
    private double valorTotal;
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

    public long getDataHoraMillis() {
        return dataHoraMillis;
    }

    public void setDataHoraMillis(long dataHoraMillis) {
        this.dataHoraMillis = dataHoraMillis;
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
}