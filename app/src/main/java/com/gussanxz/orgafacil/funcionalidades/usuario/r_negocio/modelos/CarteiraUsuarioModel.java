package com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos;

/**
 * Representa a saúde financeira do usuário para cálculos de dashboard.
 * Salvo em: teste > UID > moduloSistema > modulo_financeiro
 */
public class CarteiraUsuarioModel {
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    public CarteiraUsuarioModel() {}

    public Double getProventosTotal() { return proventosTotal; }
    public void setProventosTotal(Double proventosTotal) { this.proventosTotal = proventosTotal; }

    public Double getDespesaTotal() { return despesaTotal; }
    public void setDespesaTotal(Double despesaTotal) { this.despesaTotal = despesaTotal; }

    // Método utilitário que não é salvo no banco, apenas para uso na UI
    public Double getSaldoGeral() {
        return (proventosTotal != null ? proventosTotal : 0.0) - (despesaTotal != null ? despesaTotal : 0.0);
    }
}