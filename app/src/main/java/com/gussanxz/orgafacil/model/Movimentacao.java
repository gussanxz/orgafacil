package com.gussanxz.orgafacil.model;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Movimentacao implements Serializable {

    private String data;
    private String hora;
    private String categoria;
    private String descricao;
    private String tipo; // "d" despesa | "r" proventos
    private double valor;
    private String key;
    private String mesAno; // <-- ajuda MUITO para excluir/filtrar

    public Movimentacao() {}

    public void salvar(String uid, String dataEscolhida) {
        String mesAnoLocal = DatePickerHelper.mesAnoDataEscolhida(dataEscolhida);

        this.setData(dataEscolhida);
        this.setMesAno(mesAnoLocal);

        FirebaseFirestore fs = ConfiguracaoFirebase.getFirestore();

        String movId = (this.getKey() != null && !this.getKey().trim().isEmpty())
                ? this.getKey()
                : fs.collection("_").document().getId();

        this.setKey(movId);

        Map<String, Object> doc = new HashMap<>();
        doc.put("data", this.getData());
        doc.put("hora", this.getHora());
        doc.put("categoria", this.getCategoria());
        doc.put("descricao", this.getDescricao());
        doc.put("tipo", this.getTipo());
        doc.put("valor", this.getValor());
        doc.put("key", this.getKey());
        doc.put("mesAno", mesAnoLocal);
        doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        DocumentReference ref = fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes").document(mesAnoLocal)
                .collection("itens").document(movId);

        ref.set(doc, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d("FS", "Mov gravada: " + mesAnoLocal + "/" + movId);
                    atualizarUltimosLancamentos(fs, uid);
                })
                .addOnFailureListener(e -> Log.e("FS", "Erro ao gravar movimentacao", e));
    }

    private void atualizarUltimosLancamentos(FirebaseFirestore fs, String uid) {
        String tipoMov = this.getTipo(); // "d" ou "r"

        Map<String, Object> info = new HashMap<>();
        info.put("categoria", this.getCategoria());
        info.put("descricao", this.getDescricao());
        info.put("data", this.getData());
        info.put("hora", this.getHora());

        Map<String, Object> payload = new HashMap<>();
        if ("d".equals(tipoMov)) {
            payload.put("ultimaDespesa", info);
        } else if ("r".equals(tipoMov)) {
            payload.put("ultimoProvento", info);
        } else {
            return;
        }
        payload.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("_meta").document("ultimos")
                .set(payload, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FS", "Erro ao atualizar ultimos", e));
    }

    // getters/setters...

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
