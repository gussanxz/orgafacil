package com.gussanxz.orgafacil.model;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Movimentacao implements Serializable {

    private String data;
    private String hora;
    private String categoria;
    private String descricao;
    private String tipo;
    private double valor;
    private String key;

    public Movimentacao() {
    }

    public void salvar (String uid, String dataEscolhida, Movimentacao mov) {
        String mesAno = DatePickerHelper.mesAnoDataEscolhida(dataEscolhida);

        mov.setData(dataEscolhida);

        // 1) Firestore
        FirebaseFirestore fs = ConfiguracaoFirebase.getFirestore();

        // usa um ID estável: se vier key do RTDB, reaproveita; senão gera
        String docId = (mov.getKey() != null && !mov.getKey().trim().isEmpty())
                ? mov.getKey()
                : fs.collection("_").document().getId();

        mov.setKey(docId);

        Map<String, Object> doc = new HashMap<>();
        doc.put("data", mov.getData());
        doc.put("hora", mov.getHora());
        doc.put("categoria", mov.getCategoria());
        doc.put("descricao", mov.getDescricao());
        doc.put("tipo", mov.getTipo());
        doc.put("valor", mov.getValor());
        doc.put("key", mov.getKey());
        doc.put("mesAno", mesAno);
        doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // 1) Firestore (GRAVA DE VERDADE)
        fs.collection("users")
                .document(uid)
                .collection("movimentacoes")
                .document(docId)
                .set(doc)
                .addOnSuccessListener(unused -> Log.d("FS", "Mov gravada: " + docId))
                .addOnFailureListener(e -> Log.e("FS", "Erro ao gravar", e));



        // 2) Realtime (mantém enquanto migra)
        DatabaseReference rtdb = ConfiguracaoFirebase.getFirebaseDatabase();
        rtdb.child( "movimentacao" )
                .child( uid )
                .child( mesAno )
                .child(docId)
                .setValue(mov);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }



    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }
}
