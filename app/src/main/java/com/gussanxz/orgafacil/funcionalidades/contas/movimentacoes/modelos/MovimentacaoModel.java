package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;

import java.io.Serializable;

/**
 * MovimentacaoModel (Versão Corrigida e Unificada)
 * Garante o mapeamento correto dos campos do Firestore para a aplicação.
 * * [ATENÇÃO]: Se o seu banco de dados salva os campos na raiz do documento
 * (ex: 'pago', 'valor', 'data_movimentacao'), os métodos marcados com @PropertyName
 * farão a ligação correta.
 */
public class MovimentacaoModel implements Serializable {

    // --- Constantes de Caminho para Queries ---
    // Usadas no Repository para ordenação e filtro
    public static final String CAMPO_DATA_MOVIMENTACAO = "data_movimentacao";
    public static final String CAMPO_PAGO = "pago";

    // --- ATRIBUTOS DA RAIZ (Mapeamento Direto com Firestore) ---
    private String id;
    private String descricao;
    private int valor = 0; // Centavos para precisão [cite: 2026-02-07]
    private String categoria_id;
    private String categoria_nome;

    // [IMPORTANTE]: O campo 'pago' define se é Histórico (true) ou Futuro (false)
    private boolean pago = true;

    @ServerTimestamp
    private Timestamp data_criacao;

    // Mapeamento explícito para o campo de data no Firestore
    @PropertyName("data_movimentacao")
    private Timestamp data_movimentacao;

    // Armazena o tipo como String ("RECEITA" ou "DESPESA") para legibilidade
    private String tipo;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public MovimentacaoModel() {
        // Construtor vazio obrigatório para o Firebase
    }

    // =========================================================================
    // GETTERS E SETTERS (Com anotações do Firestore)
    // =========================================================================

    @Exclude // O ID é a chave do documento, não precisa estar no corpo JSON
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }

    public String getCategoria_id() { return categoria_id; }
    public void setCategoria_id(String categoria_id) { this.categoria_id = categoria_id; }

    public String getCategoria_nome() { return categoria_nome; }
    public void setCategoria_nome(String categoria_nome) { this.categoria_nome = categoria_nome; }

    // [IMPORTANTE]: Getter/Setter para o status de pagamento
    @PropertyName("pago")
    public boolean isPago() { return pago; }
    @PropertyName("pago")
    public void setPago(boolean pago) { this.pago = pago; }

    @PropertyName("data_movimentacao")
    public Timestamp getData_movimentacao() { return data_movimentacao; }
    @PropertyName("data_movimentacao")
    public void setData_movimentacao(Timestamp data_movimentacao) { this.data_movimentacao = data_movimentacao; }

    @PropertyName("data_criacao")
    public Timestamp getData_criacao() { return data_criacao; }
    @PropertyName("data_criacao")
    public void setData_criacao(Timestamp data_criacao) { this.data_criacao = data_criacao; }

    // Getter/Setter puro para o campo 'tipo' (String)
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    // =========================================================================
    // HELPER METHODS (Lógica de Negócio e Enums)
    // =========================================================================

    /**
     * Converte a String "RECEITA"/"DESPESA" do banco para o Enum Java.
     * Útil para switch/case e lógicas condicionais no código.
     */
    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        if (tipo == null) return TipoCategoriaContas.DESPESA; // Valor padrão seguro
        try {
            return TipoCategoriaContas.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            // Fallback para suportar bancos antigos que usavam ID numérico
            try {
                int id = Integer.parseInt(tipo);
                return TipoCategoriaContas.desdeId(id);
            } catch (Exception ex) {
                return TipoCategoriaContas.DESPESA;
            }
        }
    }

    /**
     * Define o tipo usando o Enum, mas salva como String no banco.
     */
    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) {
            this.tipo = tipoEnum.name();
        }
    }

    /**
     * Método de compatibilidade para código legado que espera int (ID).
     */
    @Exclude
    public int getTipoIdLegacy() {
        TipoCategoriaContas enumTipo = getTipoEnum();
        return (enumTipo != null) ? enumTipo.getId() : 0;
    }
}