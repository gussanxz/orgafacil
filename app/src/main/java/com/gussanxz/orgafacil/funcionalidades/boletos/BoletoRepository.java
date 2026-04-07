package com.gussanxz.orgafacil.funcionalidades.boletos;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class BoletoRepository {

    public interface Callback { void onSucesso(String msg); void onErro(String erro); }
    public interface ListaCallback { void onSucesso(List<BoletoModel> lista); void onErro(String erro); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private com.google.firebase.firestore.CollectionReference colecao() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return db.collection("usuarios").document(uid).collection("boletos");
    }

    public void salvar(BoletoModel boleto, Callback callback) {
        // 1) Cria a despesa em Contas
        MovimentacaoModel mov = new MovimentacaoModel();
        mov.setDescricao(boleto.getDescricao() != null ? boleto.getDescricao() : "Boleto");
        mov.setValor(boleto.getValor());
        mov.setTipo(TipoCategoriaContas.DESPESA.name());
        mov.setCategoria_id("geral_despesa");
        mov.setCategoria_nome("Geral");
        mov.setPago(false);
        if (boleto.getDataVencimento() != null) {
            mov.setData_vencimento(boleto.getDataVencimento());
            mov.setData_movimentacao(boleto.getDataVencimento());
        } else {
            mov.setData_vencimento(Timestamp.now());
            mov.setData_movimentacao(Timestamp.now());
        }

        new MovimentacaoRepository().salvar(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                // 2) Vincula o ID da movimentação e salva o boleto
                boleto.setMovimentacaoId(mov.getId());
                com.google.firebase.firestore.DocumentReference ref = colecao().document();
                boleto.setId(ref.getId());
                ref.set(boleto)
                        .addOnSuccessListener(v -> callback.onSucesso("Boleto salvo e despesa lançada!"))
                        .addOnFailureListener(e -> callback.onErro(e.getMessage()));
            }
            @Override
            public void onErro(String erro) { callback.onErro(erro); }
        });
    }

    public void listar(ListaCallback callback) {
        colecao().orderBy("dataVencimento", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<BoletoModel> lista = new ArrayList<>();
                    for (var doc : snap) lista.add(doc.toObject(BoletoModel.class));
                    callback.onSucesso(lista);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }
}