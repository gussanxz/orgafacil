package com.gussanxz.orgafacil.data.model;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import com.gussanxz.orgafacil.data.config.FirestoreSchema;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Movimentacao (schema novo) — com regra:
 * - NOVO: define createdAt (server) e atualiza Resumos/ultimos
 * - EDIÇÃO: NÃO altera createdAt e NÃO atualiza Resumos/ultimos (edição = correção)
 *
 * Salva em:
 * {ROOT}/{uid}/moduloSistema/contas/contasMovimentacoes/{movId}
 *
 * Resumo:
 * {ROOT}/{uid}/moduloSistema/contas/Resumos/ultimos
 */
public class Movimentacao implements Serializable {

    // Mantidos por compatibilidade UI atual
    private String data;      // dd/MM/yyyy
    private String hora;      // HH:mm
    private String categoria; // nome (hoje)
    private String descricao;
    private String tipo;      // "d" despesa | "r" proventos (legado)
    private double valor;     // legado (double)

    // Id do documento no Firestore (schema novo)
    private String key;

    // legado (não precisa mais para path, mantido por compatibilidade)
    private String mesAno;

    public Movimentacao() {}

    /**
     * Salva no schema novo.
     *
     * Regras:
     * - Se key estiver vazia => NOVO documento:
     *   - createdAt = serverTimestamp()
     *   - updatedAt = serverTimestamp()
     *   - atualiza Resumos/ultimos (ultimaEntrada/ultimaSaida)
     *
     * - Se key estiver preenchida => EDIÇÃO (correção):
     *   - NÃO mexe em createdAt
     *   - updatedAt = serverTimestamp()
     *   - NÃO atualiza Resumos/ultimos
     */
    public void salvar(String uid, String dataEscolhida) {
        if (uid == null || uid.trim().isEmpty()) {
            Log.e("FireStore", "Movimentacao.salvar(): uid vazio");
            return;
        }

        this.setData(dataEscolhida);

        FirebaseFirestore fs = FirestoreSchema.db();
        WriteBatch batch = fs.batch();

        boolean isNovo = (this.getKey() == null || this.getKey().trim().isEmpty());

        // referência do doc (novo schema)
        DocumentReference movRef;
        if (isNovo) {
            movRef = FirestoreSchema.userDoc(uid)
                    .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                    .collection(FirestoreSchema.CONTAS_MOV).document();
            this.setKey(movRef.getId());
        } else {
            movRef = FirestoreSchema.userDoc(uid)
                    .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                    .collection(FirestoreSchema.CONTAS_MOV).document(this.getKey());
        }

        // Keys por data
        Date dateObj = parseBrDate(dataEscolhida);
        String diaKey = FirestoreSchema.diaKey(dateObj);
        String mesKey = FirestoreSchema.mesKey(dateObj);

        // tipo legado -> tipo novo
        // "r" => Receita | "d" => Despesa
        String tipoNovo = "r".equals(getTipo()) ? "Receita" : "Despesa";

        // valor double -> centavos
        int valorCent = (int) Math.round(getValor() * 100.0);

        Map<String, Object> doc = new HashMap<>();
        doc.put("diaKey", diaKey);
        doc.put("mesKey", mesKey);
        doc.put("tipo", tipoNovo); // "Receita" | "Despesa"
        doc.put("valorCent", valorCent);

        // campos úteis para UI
        doc.put("data", this.getData());
        doc.put("hora", this.getHora());
        doc.put("descricao", this.getDescricao());
        doc.put("categoriaNome", this.getCategoria());

        // timestamps
        doc.put("updatedAt", FieldValue.serverTimestamp());
        if (isNovo) {
            // só no novo (edição não pode alterar o ranking)
            doc.put("createdAt", FieldValue.serverTimestamp());
        }

        batch.set(movRef, doc, SetOptions.merge());

        // Atualiza "ultimos" SOMENTE se for NOVO
        if (isNovo) {
            adicionarUltimosLancamentosNoBatch(batch, uid, tipoNovo, valorCent);
        }

        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FireStore", isNovo
                        ? "Movimentação criada (schema novo)!"
                        : "Movimentação editada (correção, schema novo)!"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao salvar movimentação (schema novo)", e));
    }

    /**
     * Atualiza Resumos/ultimos:
     * - Receita => ultimaEntrada
     * - Despesa => ultimaSaida
     *
     * Observação: Só é chamado em NOVO lançamento.
     */
    private void adicionarUltimosLancamentosNoBatch(WriteBatch batch,
                                                    String uid,
                                                    String tipoNovo,
                                                    int valorCent) {

        Map<String, Object> info = new HashMap<>();
        info.put("movId", this.getKey());
        // não precisa duplicar createdAt real aqui, mas ajuda para debug
        info.put("createdAt", FieldValue.serverTimestamp());
        info.put("valorCent", valorCent);

        // quando tiver categoriaId real, preencha
        info.put("categoriaId", null);
        info.put("categoriaNomeSnapshot", this.getCategoria());

        // (opcional, recomendado) snapshot de descricao/hora para sugestão melhor
        info.put("descricaoSnapshot", this.getDescricao());
        info.put("horaSnapshot", this.getHora());

        String campo = "Receita".equalsIgnoreCase(tipoNovo) ? "ultimaEntrada" : "ultimaSaida";

        Map<String, Object> payload = new HashMap<>();
        payload.put(campo, info);
        payload.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference ultimosRef = FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_RESUMOS).document(FirestoreSchema.CONTAS_ULTIMOS);

        batch.set(ultimosRef, payload, SetOptions.merge());
    }

    private Date parseBrDate(String dataBr) {
        if (dataBr == null) return new Date();
        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dataBr);
        } catch (ParseException e) {
            return new Date();
        }
    }

    // --- GETTERS E SETTERS ---

    public String getMesAno() { return mesAno; }
    public void setMesAno(String mesAno) { this.mesAno = mesAno; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}
