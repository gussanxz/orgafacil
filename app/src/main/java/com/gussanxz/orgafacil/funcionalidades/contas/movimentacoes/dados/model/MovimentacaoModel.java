package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;

import java.util.Date;

/**
 * MovimentacaoModel (Versão Corrigida e Unificada)
 * Garante o mapeamento correto dos campos do Firestore para a aplicação.
 */
public class MovimentacaoModel implements Parcelable {

    public static final String CAMPO_DATA_MOVIMENTACAO = "data_movimentacao";
    public static final String CAMPO_PAGO = "pago";

    private String id;
    private String descricao;
    private int valor = 0;
    private String categoria_id;
    private String categoria_nome;
    private boolean pago = true;

    @ServerTimestamp
    private Timestamp data_criacao;

    @PropertyName("data_movimentacao")
    private Timestamp data_movimentacao;

    private String tipo;

    // --- [NOVO] CAMPOS DE RECORRÊNCIA ---
    private String recorrencia_id;
    private int parcela_atual;
    private int total_parcelas;

    public MovimentacaoModel() {
        // Construtor vazio obrigatório
    }

    protected MovimentacaoModel(Parcel in) {
        id = in.readString();
        descricao = in.readString();
        valor = in.readInt();
        categoria_id = in.readString();
        categoria_nome = in.readString();
        pago = in.readByte() != 0;
        tipo = in.readString();

        data_criacao = readTimestamp(in);
        data_movimentacao = readTimestamp(in);

        // Lendo os novos campos no Parcel
        recorrencia_id = in.readString();
        parcela_atual = in.readInt();
        total_parcelas = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(descricao);
        dest.writeInt(valor);
        dest.writeString(categoria_id);
        dest.writeString(categoria_nome);
        dest.writeByte((byte) (pago ? 1 : 0));
        dest.writeString(tipo);

        writeTimestamp(dest, data_criacao);
        writeTimestamp(dest, data_movimentacao);

        // Escrevendo os novos campos no Parcel
        dest.writeString(recorrencia_id);
        dest.writeInt(parcela_atual);
        dest.writeInt(total_parcelas);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<MovimentacaoModel> CREATOR = new Creator<MovimentacaoModel>() {
        @Override
        public MovimentacaoModel createFromParcel(Parcel in) { return new MovimentacaoModel(in); }
        @Override
        public MovimentacaoModel[] newArray(int size) { return new MovimentacaoModel[size]; }
    };

    private static void writeTimestamp(Parcel dest, Timestamp ts) {
        if (ts == null) {
            dest.writeByte((byte) 0);
            return;
        }
        dest.writeByte((byte) 1);
        dest.writeLong(ts.getSeconds());
        dest.writeInt(ts.getNanoseconds());
    }

    private static Timestamp readTimestamp(Parcel in) {
        boolean has = in.readByte() != 0;
        if (!has) return null;
        long seconds = in.readLong();
        int nanos = in.readInt();
        return new Timestamp(seconds, nanos);
    }

    // --- GETTERS E SETTERS ---

    @Exclude
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

    @PropertyName("data_vencimento_original")
    private Timestamp data_vencimento_original;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    // --- [NOVO] GETTERS E SETTERS RECORRÊNCIA ---
    public String getRecorrencia_id() { return recorrencia_id; }
    public void setRecorrencia_id(String recorrencia_id) { this.recorrencia_id = recorrencia_id; }

    public int getParcela_atual() { return parcela_atual; }
    public void setParcela_atual(int parcela_atual) { this.parcela_atual = parcela_atual; }

    public int getTotal_parcelas() { return total_parcelas; }
    public void setTotal_parcelas(int total_parcelas) { this.total_parcelas = total_parcelas; }

    @PropertyName("data_vencimento_original")
    public Timestamp getData_vencimento_original() { return data_vencimento_original; }

    @PropertyName("data_vencimento_original")
    public void setData_vencimento_original(Timestamp data_vencimento_original) { this.data_vencimento_original = data_vencimento_original; }

    // --- HELPERS E ENUMS ---

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        if (tipo == null) return TipoCategoriaContas.DESPESA;
        try {
            return TipoCategoriaContas.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            try {
                int id = Integer.parseInt(tipo);
                return TipoCategoriaContas.desdeId(id);
            } catch (Exception ex) {
                return TipoCategoriaContas.DESPESA;
            }
        }
    }

    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) {
            this.tipo = tipoEnum.name();
        }
    }

    @Exclude
    public int getTipoIdLegacy() {
        TipoCategoriaContas enumTipo = getTipoEnum();
        return (enumTipo != null) ? enumTipo.getId() : 0;
    }

    @Exclude
    public boolean estaVencida() {
        if (pago || data_movimentacao == null) return false;
        return data_movimentacao.toDate().before(new Date());
    }

    // --- [NOVO] HELPER DE URGÊNCIA ---
    @Exclude
    public boolean venceEmBreve() {
        if (pago || data_movimentacao == null) return false;
        long diff = data_movimentacao.toDate().getTime() - new Date().getTime();
        long quarentaEOitoHoras = 48 * 60 * 60 * 1000L;
        return diff >= 0 && diff <= quarentaEOitoHoras;
    }
}