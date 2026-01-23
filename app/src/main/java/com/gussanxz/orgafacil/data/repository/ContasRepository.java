package com.gussanxz.orgafacil.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.data.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.data.model.Movimentacao;

import java.util.ArrayList;
import java.util.List;

public class ContasRepository {

    private final FirebaseFirestore fs;
    private final String uid;

    public ContasRepository() {
        this.fs = ConfiguracaoFirestore.getFirestore();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    // --- INTERFACES PARA RESPOSTAS (CALLBACKS) ---
    public interface DadosCallback {
        void onSucesso(List<Movimentacao> lista);
        void onErro(String erro);
    }

    public interface ResumoCallback {
        void onSucesso(Double saldo, String nomeUsuario);
    }

    public interface SimplesCallback {
        void onSucesso();
        void onErro(String erro);
    }

    // --- MÉTODOS DE AÇÃO ---

    public void recuperarMovimentacoes(DadosCallback callback) {
        if (uid == null) return;
        List<Movimentacao> lista = new ArrayList<>();

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes")
                .get()
                .addOnSuccessListener(mesesSnap -> {
                    if (mesesSnap.isEmpty()) {
                        callback.onSucesso(lista); // Retorna lista vazia
                        return;
                    }

                    final int totalMeses = mesesSnap.size();
                    final int[] done = {0};

                    for (QueryDocumentSnapshot mesDoc : mesesSnap) {
                        String mesAno = mesDoc.getId();
                        mesDoc.getReference().collection("itens").get()
                                .addOnSuccessListener(itensSnap -> {
                                    for (QueryDocumentSnapshot d : itensSnap) {
                                        try {
                                            Movimentacao m = d.toObject(Movimentacao.class);
                                            m.setKey(d.getId());
                                            m.setMesAno(mesAno);
                                            lista.add(m);
                                        } catch (Exception e) { e.printStackTrace(); }
                                    }
                                    done[0]++;
                                    if (done[0] == totalMeses) {
                                        callback.onSucesso(lista);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void excluirMovimentacao(Movimentacao mov, SimplesCallback callback) {
        if (uid == null || mov == null) return;

        // Lógica de pegar mês se estiver vazio
        String mesAno = mov.getMesAno();
        if (mesAno == null && mov.getData() != null && mov.getData().length() >= 10) {
            mesAno = mov.getData().substring(3, 5) + mov.getData().substring(6);
        }

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes").document(mesAno)
                .collection("itens").document(mov.getKey())
                .delete()
                .addOnSuccessListener(unused -> {
                    // Atualiza saldo
                    String campo = "r".equals(mov.getTipo()) ? "proventosTotal" : "despesaTotal";
                    fs.collection("users").document(uid)
                            .collection("contas").document("main")
                            .update(campo, FieldValue.increment(-mov.getValor()));

                    callback.onSucesso();
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarResumo(ResumoCallback callback) {
        if (uid == null) return;

        // Busca Nome e Saldo em paralelo (simplificado aqui para buscar saldo)
        fs.collection("users").document(uid).collection("contas").document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    Double d = doc.getDouble("despesaTotal");
                    Double p = doc.getDouble("proventosTotal");
                    double saldo = (p != null ? p : 0.0) - (d != null ? d : 0.0);

                    // Busca nome separadamente
                    fs.collection("users").document(uid).get().addOnSuccessListener(u -> {
                        String nome = u.getString("nome");
                        callback.onSucesso(saldo, nome != null ? nome : "Usuário");
                    });
                });
    }
}