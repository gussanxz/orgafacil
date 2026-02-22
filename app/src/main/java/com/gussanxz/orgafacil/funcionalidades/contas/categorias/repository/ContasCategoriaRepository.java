package com.gussanxz.orgafacil.funcionalidades.contas.categorias.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.util_helper.CategoriaIdHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Repository responsável pela gestão de categorias de contas.
 * [CORRIGIDO]: Adaptado para a estrutura aninhada (Visual/Financeiro) do Model.
 */
public class ContasCategoriaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso();
        void onErro(String erro);
    }

    /**
     * Salva ou atualiza uma categoria.
     * Gera o ID baseado no 'visual.nome' se for um novo cadastro.
     */
    public void salvar(ContasCategoriaModel categoria, Callback callback) {
        // Se for nova e não tiver ID, geramos pelo nome localizado no sub-objeto Visual
        if (categoria.getId() == null || categoria.getId().isEmpty()) {
            // Acessa o nome dentro do objeto Visual
            String nomeCategoria = (categoria.getVisual() != null) ? categoria.getVisual().getNome() : null;

            if (nomeCategoria != null && !nomeCategoria.isEmpty()) {
                // Cria um ID amigável (Slug) baseado no nome
                String idGerado = CategoriaIdHelper.slugify(nomeCategoria);
                categoria.setId(idGerado);
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
     * REGRA DE NEGÓCIO: Só exclui se não houver movimentações vinculadas.
     * Busca pelo campo "categoria_id" na raiz do MovimentacaoModel (que foi achatado).
     */
    public void verificarEExcluir(ContasCategoriaModel categoria, Callback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("categoria_id", categoria.getId())
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
     * Inicializa o app com categorias padrão.
     * [CORREÇÃO]: Preenche os dados dentro do sub-objeto VISUAL.
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
        // O construtor do Model já inicializa 'new Visual()' e 'new Financeiro()'
        ContasCategoriaModel cat = new ContasCategoriaModel();

        // [CORRIGIDO]: Configuração correta acessando o sub-objeto Visual
        cat.getVisual().setNome(nome);
        cat.getVisual().setCor("#757575"); // Cor padrão cinza
        cat.getVisual().setIcone("ic_padrao"); // Ícone padrão

        cat.setId(CategoriaIdHelper.slugify(nome));
        cat.setTipo(tipo.getId());
        cat.setAtiva(true);

        batch.set(FirestoreSchema.contasCategoriaDoc(cat.getId()), cat);
    }

    /**
     * Query para listagem.
     * CAMPO_NOME (visual.nome) e CAMPO_TIPO (tipo) são constantes do Model.
     */
    public Query listarAtivasPorTipo(TipoCategoriaContas tipo) {
        return FirestoreSchema.contasCategoriasCol()
                .whereEqualTo(ContasCategoriaModel.CAMPO_TIPO, tipo.getId())
                .whereEqualTo(ContasCategoriaModel.CAMPO_ATIVA, true)
                // Ordena por "visual.nome" (definido na constante CAMPO_NOME do Model)
                .orderBy(ContasCategoriaModel.CAMPO_NOME, Query.Direction.ASCENDING);
    }
}