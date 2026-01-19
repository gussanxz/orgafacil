package com.gussanxz.orgafacil.model;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.helper.DateCustom;

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

        String mesAnoLocal = DateCustom.mesAnoDataEscolhida(dataEscolhida);
        this.setData(dataEscolhida);
        this.setMesAno(mesAnoLocal);

        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();
        WriteBatch batch = fs.batch(); // Inicia o pacote de gravação atômica

        DocumentReference movRef;
        if (this.getKey() != null && !this.getKey().isEmpty()) {
            // É uma edição
            movRef = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("movimentacoes").document(mesAnoLocal)
                    .collection("itens").document(this.getKey());
        } else {
            // É novo: Gera ID automático
            movRef = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("movimentacoes").document(mesAnoLocal)
                    .collection("itens").document();
            this.setKey(movRef.getId());
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("data", this.getData());
        doc.put("hora", this.getHora());
        doc.put("categoria", this.getCategoria());
        doc.put("descricao", this.getDescricao());
        doc.put("tipo", this.getTipo());
        doc.put("valor", this.getValor());
        doc.put("key", this.getKey());
        doc.put("mesAno", mesAnoLocal);
        doc.put("createdAt", FieldValue.serverTimestamp()); // Usa Timestamp do servidor

        // Adiciona a gravação da movimentação no Batch
        batch.set(movRef, doc, SetOptions.merge());

        // Atualizar o Saldo (users/{uid}/contas/main)
        // Isso é CRUCIAL: O incremento acontece direto no servidor.
        DocumentReference contaMainRef = fs.collection("users").document(uid)
                .collection("contas").document("main");

        if ("r".equals(getTipo())) {
            batch.update(contaMainRef, "proventosTotal", FieldValue.increment(getValor()));
        } else if ("d".equals(getTipo())) {
            batch.update(contaMainRef, "despesaTotal", FieldValue.increment(getValor()));
        }

        adicionarUltimosLancamentosNoBatch(batch, fs, uid);

        // COMMIT: Envia tudo de uma vez
        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FireStore", "Movimentação, Saldo e Meta salvos com sucesso!"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro crítico ao salvar batch", e));
    }

    // Metodo auxiliar para organizar o código (agora recebe o batch)
    private void adicionarUltimosLancamentosNoBatch(WriteBatch batch, FirebaseFirestore fs, String uid) {
        String tipoMov = this.getTipo();

        Map<String, Object> info = new HashMap<>();
        info.put("categoria", this.getCategoria());
        info.put("descricao", this.getDescricao());
        info.put("data", this.getData());
        info.put("hora", this.getHora());
        info.put("valor", this.getValor()); // Adicionei valor, geralmente é útil exibir no resumo

        Map<String, Object> payload = new HashMap<>();
        if ("d".equals(tipoMov)) {
            payload.put("ultimaDespesa", info);
        } else if ("r".equals(tipoMov)) {
            payload.put("ultimoProvento", info);
        } else {
            return;
        }
        payload.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference metaRef = fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("_meta").document("ultimos");

        // Adiciona ao pacote
        batch.set(metaRef, payload, SetOptions.merge());
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
