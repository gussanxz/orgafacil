package com.gussanxz.orgafacil.funcionalidades.contas.categorias.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.CategoriaIdHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Repository responsável pela gestão de categorias de contas.
 * Fontes: Firebase Firestore Documentation & Clean Architecture Principles.
 */
public class ContasCategoriaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    /**
     * Salva ou atualiza uma categoria usando SetOptions.merge() para preservar campos existentes.
     * [MELHORIA]: Validação de nome antes de gerar o ID via Slugify.
     */
    public void salvar(ContasCategoriaModel categoria, Callback callback) {
        // Se for nova e não tiver ID, geramos pelo nome dentro do mapa visual
        if (categoria.getId() == null || categoria.getId().isEmpty()) {
            if (categoria.getVisual() != null && categoria.getVisual().getNome() != null) {
                categoria.setId(CategoriaIdHelper.slugify(categoria.getVisual().getNome()));
            } else {
                callback.onErro("Erro: Nome da categoria é obrigatório para gerar o ID.");
                return;
            }
        }

        FirestoreSchema.contasCategoriaDoc(categoria.getId())
                .set(categoria, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * REGRA DE NEGÓCIO: Só exclui se não houver movimentações vinculadas no Firestore.
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

    /**
     * Remove fisicamente o documento da categoria.
     */
    private void deletar(String id, Callback callback) {
        FirestoreSchema.contasCategoriaDoc(id).delete()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Inicializa o app com categorias padrão (Alimentação, Lazer, etc).
     * [ATUALIZADO]: Preenche os dados dentro do grupo VISUAL para respeitar o novo Modelo.
     */
    public void inicializarPadroes(Callback callback) {
        WriteBatch batch = db.batch();

        // Categorias de Receita
        List<String> receitas = Arrays.asList("Salário", "Investimentos", "Extra");
        for (String nome : receitas) {
            adicionarAoBatch(batch, nome, TipoCategoriaContas.RECEITA);
        }

        // Categorias de Despesa
        List<String> despesas = Arrays.asList("Alimentação", "Moradia", "Transporte", "Lazer", "Saúde");
        for (String nome : despesas) {
            adicionarAoBatch(batch, nome, TipoCategoriaContas.DESPESA);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    private void adicionarAoBatch(WriteBatch batch, String nome, TipoCategoriaContas tipo) {
        ContasCategoriaModel cat = new ContasCategoriaModel();
        cat.getVisual().setNome(nome);
        cat.setId(CategoriaIdHelper.slugify(nome));
        cat.setTipo(tipo.getId());
        cat.setAtiva(true);
        batch.set(FirestoreSchema.contasCategoriaDoc(cat.getId()), cat);
    }

    /**
     * Query para listagem nas telas de seleção.
     * [MELHORIA]: Agora recebe o Enum TipoCategoriaContas para maior segurança de tipos.
     * CAMPO_NOME no Model já aponta para "visual.nome" (Dot Notation).
     */
    public Query listarAtivasPorTipo(TipoCategoriaContas tipo) {
        return FirestoreSchema.contasCategoriasCol()
                .whereEqualTo(ContasCategoriaModel.CAMPO_TIPO, tipo.getId())
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING);
    }
}