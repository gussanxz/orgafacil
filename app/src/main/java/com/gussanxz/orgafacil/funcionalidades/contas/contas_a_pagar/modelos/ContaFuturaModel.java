package com.gussanxz.orgafacil.funcionalidades.contas.contas_a_pagar.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;

/**
 * Modelo para Contas a Pagar ou Receber (Agendamentos).
 * Alimenta os indicadores de "Pendências" e "Previsão" no ResumoFinanceiro.
 * REGRA DE OURO: Valores monetários em INT (centavos).
 */
public class ContaFuturaModel {

    // --- Constantes para Mapeamento (Âncoras para Repositories) ---
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_DESCRICAO = "descricao";
    public static final String CAMPO_VALOR = "valor";
    public static final String CAMPO_TIPO = "tipo";
    public static final String CAMPO_DATA_VENCIMENTO = "data_vencimento";
    public static final String CAMPO_CAT_ID = "categoria_id";
    public static final String CAMPO_CAT_NOME = "categoria_nome";
    public static final String CAMPO_CAT_ICONE = "categoria_icone";
    public static final String CAMPO_CAT_COR = "categoria_cor";
    public static final String CAMPO_PAGO = "pago";
    public static final String CAMPO_FIXO = "fixo";
    public static final String CAMPO_DATA_CRIACAO = "data_criacao";

    private String id;
    private String descricao;
    private int valor = 0; // Centavos

    // 1 para RECEBER, 2 para PAGAR
    private int tipo = 0;

    private Timestamp data_vencimento;

    // Denormalização para exibição rápida
    private String categoria_id;
    private String categoria_nome;
    private String categoria_icone;
    private String categoria_cor;

    // Controle de Status
    private boolean pago = false;
    private boolean fixo = false; // Se a conta se repete mensalmente

    @ServerTimestamp
    private Timestamp data_criacao;

    public ContaFuturaModel() {}

    // --- Getters e Setters com PropertyName ---

    @PropertyName(CAMPO_ID)
    public String getId() { return id; }
    @PropertyName(CAMPO_ID)
    public void setId(String id) { this.id = id; }

    @PropertyName(CAMPO_DESCRICAO)
    public String getDescricao() { return descricao; }
    @PropertyName(CAMPO_DESCRICAO)
    public void setDescricao(String descricao) { this.descricao = descricao; }

    @PropertyName(CAMPO_VALOR)
    public int getValor() { return valor; }
    @PropertyName(CAMPO_VALOR)
    public void setValor(int valor) { this.valor = valor; }

    @PropertyName(CAMPO_TIPO)
    public int getTipo() { return tipo; }
    @PropertyName(CAMPO_TIPO)
    public void setTipo(int tipo) { this.tipo = tipo; }

    @PropertyName(CAMPO_DATA_VENCIMENTO)
    public Timestamp getData_vencimento() { return data_vencimento; }
    @PropertyName(CAMPO_DATA_VENCIMENTO)
    public void setData_vencimento(Timestamp data_vencimento) { this.data_vencimento = data_vencimento; }

    @PropertyName(CAMPO_CAT_ID)
    public String getCategoria_id() { return categoria_id; }
    @PropertyName(CAMPO_CAT_ID)
    public void setCategoria_id(String categoria_id) { this.categoria_id = categoria_id; }

    @PropertyName(CAMPO_CAT_NOME)
    public String getCategoria_nome() { return categoria_nome; }
    @PropertyName(CAMPO_CAT_NOME)
    public void setCategoria_nome(String categoria_nome) { this.categoria_nome = categoria_nome; }

    @PropertyName(CAMPO_CAT_ICONE)
    public String getCategoria_icone() { return categoria_icone; }
    @PropertyName(CAMPO_CAT_ICONE)
    public void setCategoria_icone(String categoria_icone) { this.categoria_icone = categoria_icone; }

    @PropertyName(CAMPO_CAT_COR)
    public String getCategoria_cor() { return categoria_cor; }
    @PropertyName(CAMPO_CAT_COR)
    public void setCategoria_cor(String categoria_cor) { this.categoria_cor = categoria_cor; }

    @PropertyName(CAMPO_PAGO)
    public boolean isPago() { return pago; }
    @PropertyName(CAMPO_PAGO)
    public void setPago(boolean pago) { this.pago = pago; }

    @PropertyName(CAMPO_FIXO)
    public boolean isFixo() { return fixo; }
    @PropertyName(CAMPO_FIXO)
    public void setFixo(boolean fixo) { this.fixo = fixo; }

    @PropertyName(CAMPO_DATA_CRIACAO)
    public Timestamp getData_criacao() { return data_criacao; }
    @PropertyName(CAMPO_DATA_CRIACAO)
    public void setData_criacao(Timestamp data_criacao) { this.data_criacao = data_criacao; }

    // --- Helpers de Enum e Lógica (Excluídos via @Exclude) ---

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        return TipoCategoriaContas.desdeId(this.tipo);
    }

    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) {
            this.tipo = tipoEnum.getId();
        }
    }

    @Exclude
    public boolean isAtrasada() {
        if (pago || data_vencimento == null) return false;
        return data_vencimento.toDate().before(new java.util.Date());
    }
}