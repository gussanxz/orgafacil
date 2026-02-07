package com.gussanxz.orgafacil.funcionalidades.contas.categorias.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.CategoriaIdHelper;

import java.util.Arrays;
import java.util.List;

public class ContasCategoriaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    /**
     * Salva usando o Model e o Slugify para o ID.
     */
    public void salvar(ContasCategoriaModel categoria, Callback callback) {
        // Se for nova e não tiver ID, geramos pelo nome (Slugify)
        if (categoria.getId() == null || categoria.getId().isEmpty()) {
            categoria.setId(CategoriaIdHelper.slugify(categoria.getNome()));
        }

        FirestoreSchema.contasCategoriaDoc(categoria.getId())
                .set(categoria)
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * REGRA: Só exclui se não houver movimentações vinculadas.
     * Usamos as constantes ancoradas no MovimentacaoModel.
     */
    public void verificarEExcluir(ContasCategoriaModel categoria, Callback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_CAT_ID, categoria.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        callback.onErro("Categoria em uso — remova as movimentações antes de excluir.");
                    } else {
                        deletar(categoria.getId(), callback);
                    }
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao validar uso: " + e.getMessage()));
    }

    private void deletar(String id, Callback callback) {
        FirestoreSchema.contasCategoriaDoc(id).delete()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Inicializa o app com categorias prontas (Usando Batch para performance).
     */
    public void inicializarPadroes(Callback callback) {
        List<String> nomes = Arrays.asList(
                "Alimentação", "Lazer", "Mercado", "Moradia", "Saúde", "Transporte"
        );

        WriteBatch batch = db.batch();

        for (String nome : nomes) {
            ContasCategoriaModel cat = new ContasCategoriaModel();
            cat.setNome(nome);
            cat.setId(CategoriaIdHelper.slugify(nome));
            cat.setTipo(TipoCategoriaContas.DESPESA.getId()); // Padrões iniciais como Despesa
            cat.setIcone("ic_default"); // Placeholder para seu sistema de ícones
            cat.setAtiva(true);

            DocumentReference ref = FirestoreSchema.contasCategoriaDoc(cat.getId());
            batch.set(ref, cat);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Query para listagem nas telas de seleção (Spinners/Dialogs).
     */
    public Query listarAtivasPorTipo(int tipoId) {
        return FirestoreSchema.contasCategoriasCol()
                .whereEqualTo(ContasCategoriaModel.CAMPO_TIPO, tipoId)
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING);
    }
}