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
    /**
     * ID do caixa ao qual esta venda pertence.
     * null → venda legada (anterior ao fluxo de caixa), tratada como "caixa_0".
     */
    private String caixaId;
    /**
     * Nome legível do caixa no formato "yyyyMMdd_N" (ex.: "20260420_1").
     * Desnormalizado para exibição sem consulta extra.
     */
    private String nomeCaixa;
    private int numeroVenda;
    private long dataHoraAberturaMillis;
    private long dataHoraFechamentoMillis;
    private String formaPagamento;
    private int quantidadeTotal;
    private double valorTotal;
    private double acrescimo = 0.0;
    private double desconto = 0.0;
    private double valorRecebidoDinheiro = 0.0;
    private double trocoDinheiro = 0.0;
    private String status;
    private String clienteId;
    private String clienteNome;
    private String clienteTelefone;
    private String vendedorId;
    private String vendedorNome;
    private String vendedorEmail;
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
    public double getValorRecebidoDinheiro() { return valorRecebidoDinheiro; }
    public void setValorRecebidoDinheiro(double valorRecebidoDinheiro) { this.valorRecebidoDinheiro = valorRecebidoDinheiro; }
    public double getTrocoDinheiro() { return trocoDinheiro; }
    public void setTrocoDinheiro(double trocoDinheiro) { this.trocoDinheiro = trocoDinheiro; }
    public String getDiaKey() { return diaKey; }
    public void setDiaKey(String diaKey) { this.diaKey = diaKey; }

    public String getCaixaId() { return caixaId; }
    public void setCaixaId(String caixaId) { this.caixaId = caixaId; }

    public String getNomeCaixa() { return nomeCaixa; }
    public void setNomeCaixa(String nomeCaixa) { this.nomeCaixa = nomeCaixa; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteTelefone() { return clienteTelefone; }
    public void setClienteTelefone(String clienteTelefone) { this.clienteTelefone = clienteTelefone; }

    public String getVendedorId() { return vendedorId; }
    public void setVendedorId(String vendedorId) { this.vendedorId = vendedorId; }

    public String getVendedorNome() { return vendedorNome; }
    public void setVendedorNome(String vendedorNome) { this.vendedorNome = vendedorNome; }

    public String getVendedorEmail() { return vendedorEmail; }
    public void setVendedorEmail(String vendedorEmail) { this.vendedorEmail = vendedorEmail; }
}
