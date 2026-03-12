package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia;

import java.util.Calendar;
import java.util.Date;

public class MovimentacaoModel implements Parcelable {

    public static final String CAMPO_DATA_MOVIMENTACAO = "data_movimentacao";
    public static final String CAMPO_PAGO              = "pago";

    // ── campos básicos ───────────────────────────────────────────────────────
    private String id;
    private String descricao;
    private long valor = 0;
    private String categoria_id;
    private String categoria_nome;
    private boolean pago = true;

    @ServerTimestamp
    private Timestamp data_criacao;

    @PropertyName("data_movimentacao")
    private Timestamp data_movimentacao;

    @PropertyName("data_vencimento_original")
    private Timestamp data_vencimento_original;

    @PropertyName("data_vencimento")
    private Timestamp data_vencimento;

    @PropertyName("data_pagamento")
    private Timestamp data_pagamento;

    private String tipo; // TipoCategoriaContas name()

    // ── campos de série / parcelamento ───────────────────────────────────────
    private String recorrencia_id;
    private int    parcela_atual;
    private int    total_parcelas;
    private String recorrencia_tipo;
    private int recorrencia_intervalo = 0;

    // ── construtores ─────────────────────────────────────────────────────────

    public MovimentacaoModel() {}

    protected MovimentacaoModel(Parcel in) {
        id              = in.readString();
        descricao       = in.readString();
        valor           = in.readLong();
        categoria_id    = in.readString();
        categoria_nome  = in.readString();
        pago            = in.readByte() != 0;
        tipo            = in.readString();

        data_criacao             = readTimestamp(in);
        data_movimentacao        = readTimestamp(in);
        data_vencimento_original = readTimestamp(in);

        data_vencimento          = readTimestamp(in);
        data_pagamento           = readTimestamp(in);


        recorrencia_id         = in.readString();
        parcela_atual          = in.readInt();
        total_parcelas         = in.readInt();
        recorrencia_tipo       = in.readString();
        recorrencia_intervalo  = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(descricao);
        dest.writeLong(valor);
        dest.writeString(categoria_id);
        dest.writeString(categoria_nome);
        dest.writeByte((byte) (pago ? 1 : 0));
        dest.writeString(tipo);

        writeTimestamp(dest, data_criacao);
        writeTimestamp(dest, data_movimentacao);
        writeTimestamp(dest, data_vencimento_original);

        writeTimestamp(dest, data_vencimento);
        writeTimestamp(dest, data_pagamento);

        dest.writeString(recorrencia_id);
        dest.writeInt(parcela_atual);
        dest.writeInt(total_parcelas);
        dest.writeString(recorrencia_tipo);
        dest.writeInt(recorrencia_intervalo);
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<MovimentacaoModel> CREATOR = new Creator<MovimentacaoModel>() {
        @Override public MovimentacaoModel createFromParcel(Parcel in) { return new MovimentacaoModel(in); }
        @Override public MovimentacaoModel[] newArray(int size)        { return new MovimentacaoModel[size]; }
    };

    // ── helpers de Timestamp para Parcel ────────────────────────────────────

    private static void writeTimestamp(Parcel dest, Timestamp ts) {
        if (ts == null) { dest.writeByte((byte) 0); return; }
        dest.writeByte((byte) 1);
        dest.writeLong(ts.getSeconds());
        dest.writeInt(ts.getNanoseconds());
    }

    private static Timestamp readTimestamp(Parcel in) {
        if (in.readByte() == 0) return null;
        return new Timestamp(in.readLong(), in.readInt());
    }

    // ── getters / setters ────────────────────────────────────────────────────

    @Exclude public String getId()            { return id; }
    @Exclude public void setId(String id)     { this.id = id; }

    public String getDescricao()           { return descricao; }
    public void setDescricao(String d)     { this.descricao = d; }

    public long getValor()                 { return valor; }
    public void setValor(long v)           { this.valor = v; }

    public String getCategoria_id()        { return categoria_id; }
    public void setCategoria_id(String v)  { this.categoria_id = v; }

    public String getCategoria_nome()      { return categoria_nome; }
    public void setCategoria_nome(String v){ this.categoria_nome = v; }

    @PropertyName("pago") public boolean isPago()         { return pago; }
    @PropertyName("pago") public void setPago(boolean v)  { this.pago = v; }

    @PropertyName("data_movimentacao")
    public Timestamp getData_movimentacao()                { return data_movimentacao; }
    @PropertyName("data_movimentacao")
    public void setData_movimentacao(Timestamp v)          { this.data_movimentacao = v; }

    @PropertyName("data_criacao")
    public Timestamp getData_criacao()                     { return data_criacao; }
    @PropertyName("data_criacao")
    public void setData_criacao(Timestamp v)               { this.data_criacao = v; }

    @PropertyName("data_vencimento_original")
    public Timestamp getData_vencimento_original()         { return data_vencimento_original; }
    @PropertyName("data_vencimento_original")
    public void setData_vencimento_original(Timestamp v)   { this.data_vencimento_original = v; }

    @PropertyName("data_vencimento")
    public Timestamp getData_vencimento() { return data_vencimento; }
    @PropertyName("data_vencimento")
    public void setData_vencimento(Timestamp v) { this.data_vencimento = v; }

    @PropertyName("data_pagamento")
    public Timestamp getData_pagamento() { return data_pagamento; }
    @PropertyName("data_pagamento")
    public void setData_pagamento(Timestamp v) { this.data_pagamento = v; }

    public String getTipo()                { return tipo; }
    public void setTipo(String v)          { this.tipo = v; }

    public String getRecorrencia_id()      { return recorrencia_id; }
    public void setRecorrencia_id(String v){ this.recorrencia_id = v; }

    public int getParcela_atual()          { return parcela_atual; }
    public void setParcela_atual(int v)    { this.parcela_atual = v; }

    public int getTotal_parcelas()         { return total_parcelas; }
    public void setTotal_parcelas(int v)   { this.total_parcelas = v; }

    public String getRecorrencia_tipo()    { return recorrencia_tipo; }
    public void setRecorrencia_tipo(String v){ this.recorrencia_tipo = v; }

    public int getRecorrencia_intervalo()  { return recorrencia_intervalo; }
    public void setRecorrencia_intervalo(int v){ this.recorrencia_intervalo = v; }

    // ── helpers @Exclude ─────────────────────────────────────────────────────

    @Exclude
    public TipoCategoriaContas getTipoEnum() {
        if (tipo == null) return TipoCategoriaContas.DESPESA;
        try {
            return TipoCategoriaContas.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            try {
                return TipoCategoriaContas.desdeId(Integer.parseInt(tipo));
            } catch (Exception ex) {
                return TipoCategoriaContas.DESPESA;
            }
        }
    }

    @Exclude
    public void setTipoEnum(TipoCategoriaContas tipoEnum) {
        if (tipoEnum != null) this.tipo = tipoEnum.name();
    }

    @Exclude
    public TipoRecorrencia getTipoRecorrenciaEnum() {
        return TipoRecorrencia.fromString(recorrencia_tipo);
    }

    @Exclude
    public void setTipoRecorrenciaEnum(TipoRecorrencia t) {
        this.recorrencia_tipo = (t != null) ? t.name() : null;
    }

    @Exclude
    public int getTipoIdLegacy() {
        TipoCategoriaContas e = getTipoEnum();
        return (e != null) ? e.getId() : 0;
    }

    @Exclude
    public boolean estaVencida() {
        Timestamp ref = (data_vencimento != null) ? data_vencimento : data_movimentacao;
        if (pago || ref == null) return false;

        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0); hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0); hoje.set(Calendar.MILLISECOND, 0);

        Calendar venc = Calendar.getInstance();
        venc.setTime(ref.toDate());
        venc.set(Calendar.HOUR_OF_DAY, 0); venc.set(Calendar.MINUTE, 0);
        venc.set(Calendar.SECOND, 0); venc.set(Calendar.MILLISECOND, 0);

        return venc.before(hoje);
    }

    @Exclude
    public boolean venceEmBreve() {
        Timestamp ref = (data_vencimento != null) ? data_vencimento : data_movimentacao;
        if (pago || ref == null) return false;
        long diff = ref.toDate().getTime() - new Date().getTime();
        return diff >= 0 && diff <= 48L * 60 * 60 * 1000;
    }

    @Exclude
    public int diasParaVencimento() {
        Timestamp ref = (data_vencimento != null) ? data_vencimento : data_movimentacao;
        if (pago || ref == null) return Integer.MAX_VALUE;

        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0); hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0); hoje.set(Calendar.MILLISECOND, 0);

        Calendar venc = Calendar.getInstance();
        venc.setTime(ref.toDate());
        venc.set(Calendar.HOUR_OF_DAY, 0); venc.set(Calendar.MINUTE, 0);
        venc.set(Calendar.SECOND, 0); venc.set(Calendar.MILLISECOND, 0);

        return (int) Math.round(
                (double)(venc.getTimeInMillis() - hoje.getTimeInMillis()) / (1000 * 60 * 60 * 24)
        );
    }
}