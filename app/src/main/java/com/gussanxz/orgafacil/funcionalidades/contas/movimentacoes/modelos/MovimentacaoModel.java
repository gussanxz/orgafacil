package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;

import java.io.Serializable;

/**
 * MovimentacaoModel
 * Representa uma transação real (Receita ou Despesa).
 * Estrutura:
 * - Raiz: id, data_criacao
 * - detalhes: { descricao, valor, tipo, data_movimentacao }
 * - categoria: { id, nome, icone, cor }
 */
public class MovimentacaoModel implements Serializable {

    // --- Constantes de Caminho (Dot Notation para Queries e Updates) ---
    public static final String CAMPO_ID = "id";
    public static final String CAMPO_DATA_CRIACAO = "data_criacao";

    // Grupo Detalhes
    public static final String CAMPO_DESCRICAO = "detalhes.descricao";
    public static final String CAMPO_VALOR = "detalhes.valor";
    public static final String CAMPO_TIPO = "detalhes.tipo";
    public static final String CAMPO_DATA_MOVIMENTACAO = "detalhes.data_movimentacao";

    // Grupo Categoria
    public static final String CAMPO_CAT_ID = "categoria.id";
    public static final String CAMPO_CAT_NOME = "categoria.nome";
    public static final String CAMPO_CAT_ICONE = "categoria.icone";
    public static final String CAMPO_CAT_COR = "categoria.cor";

    // --- ATRIBUTOS DA RAIZ ---
    private String id;

    private Detalhes detalhes;
    private CategoriaDados categoria;

    @ServerTimestamp
    private Timestamp data_criacao;

    // =========================================================================
    // CONSTRUTOR
    // =========================================================================
    public MovimentacaoModel() {
        this.detalhes = new Detalhes();
        this.categoria = new CategoriaDados();
    }

    // =========================================================================
    // GETTERS E SETTERS DA RAIZ
    // =========================================================================

    @PropertyName(CAMPO_ID)
    public String getId() { return id; }
    @PropertyName(CAMPO_ID)
    public void setId(String id) { this.id = id; }

    public Detalhes getDetalhes() { return detalhes; }
    public void setDetalhes(Detalhes detalhes) { this.detalhes = detalhes; }

    public CategoriaDados getCategoria() { return categoria; }
    public void setCategoria(CategoriaDados categoria) { this.categoria = categoria; }

    @PropertyName(CAMPO_DATA_CRIACAO)
    public Timestamp getData_criacao() { return data_criacao; }
    @PropertyName(CAMPO_DATA_CRIACAO)
    public void setData_criacao(Timestamp data_criacao) { this.data_criacao = data_criacao; }


    // =========================================================================
    // CLASSES INTERNAS (Mapas)
    // =========================================================================

    public static class Detalhes implements Serializable {
        private String descricao;
        private int valor = 0; // Centavos
        private int tipo = 0;  // 1=Receita, 2=Despesa
        private Timestamp data_movimentacao;

        public Detalhes() {}

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public int getValor() { return valor; }
        public void setValor(int valor) { this.valor = valor; }
        public int getTipo() { return tipo; }
        public void setTipo(int tipo) { this.tipo = tipo; }
        public Timestamp getData_movimentacao() { return data_movimentacao; }
        public void setData_movimentacao(Timestamp data_movimentacao) { this.data_movimentacao = data_movimentacao; }
    }

    public static class CategoriaDados implements Serializable {
        private String id;
        private String nome;
        private String icone;
        private String cor;

        public CategoriaDados() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getIcone() { return icone; }
        public void setIcone(String icone) { this.icone = icone; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
    }


    // =========================================================================
    // MÉTODOS DE COMPATIBILIDADE (@Exclude)
    // Esses métodos garantem que o restante do App continue funcionando
    // sem precisar refatorar todas as Activities agora.
    // =========================================================================

    @Exclude
    public String getDescricao() { return detalhes.getDescricao(); }
    @Exclude
    public void setDescricao(String descricao) { detalhes.setDescricao(descricao); }

    @Exclude
    public int getValor() { return detalhes.getValor(); }
    @Exclude
    public void setValor(int valor) { detalhes.setValor(valor); }

    @Exclude
    public int getTipo() { return detalhes.getTipo(); }
    @Exclude
    public void setTipo(int tipo) { detalhes.setTipo(tipo); }

    @Exclude
    public Timestamp getData_movimentacao() { return detalhes.getData_movimentacao(); }
    @Exclude
    public void setData_movimentacao(Timestamp data) { detalhes.setData_movimentacao(data); }

    @Exclude
    public String getCategoria_id() { return categoria.getId(); }
    @Exclude
    public void setCategoria_id(String id) { categoria.setId(id); }

    @Exclude
    public String getCategoria_nome() { return categoria.getNome(); }
    @Exclude
    public void setCategoria_nome(String nome) { categoria.setNome(nome); }

    @Exclude
    public String getCategoria_icone() { return categoria.getIcone(); }
    @Exclude
    public void setCategoria_icone(String icone) { categoria.setIcone(icone); }

    @Exclude
    public String getCategoria_cor() { return categoria.getCor(); }
    @Exclude
    public void setCategoria_cor(String cor) { categoria.setCor(cor); }

    // --- Helpers de Enum ---

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        return TipoCategoriaContas.desdeId(detalhes.getTipo());
    }

    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) {
            detalhes.setTipo(tipoEnum.getId());
        }
    }
}