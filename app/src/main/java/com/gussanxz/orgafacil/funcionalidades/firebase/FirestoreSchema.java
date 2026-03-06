package com.gussanxz.orgafacil.funcionalidades.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FirestoreSchema
 *
 * Centraliza TODOS os caminhos do Firestore (rotas).
 * Camada de mapeamento de dados (Data Mapping Layer).
 */
public final class FirestoreSchema {

    private FirestoreSchema() {}

    // ======== Coleções Raiz ========
    public static final String ROOT = "teste"; // Mudar para "usuarios" em produção
    public static final String CONFIG = "config";

    // Sub-documentos de Configuração
    public static final String PERFIL = "config_perfil";
    public static final String PREFERENCIAS = "config_preferencias";
    public static final String SEGURANCA = "config_seguranca";

    // ======== MÓDULO CONTAS ========
    public static final String CONTAS = "contas";
    public static final String RESUMO_GERAL = "resumo_geral";

    // Subcoleções
    public static final String CONTAS_MOVIMENTACOES = "contas_movimentacoes";
    public static final String CONTAS_CATEGORIAS = "contas_categorias";
    public static final String CONTAS_A_PAGAR_RECEBER = "contas_a_pagar_receber";

    // ======== MÓDULO VENDAS ========
    public static final String VENDAS = "vendas";
    public static final String VENDAS_RESUMO_GERAL = "resumo_geral";

    public static final String VENDAS_CATEGORIAS = "categorias";
    public static final String VENDAS_PRODUTOS = "produtos";
    public static final String VENDAS_CAIXA = "caixa";
    public static final String VENDAS_VENDAS = "vendas_registradas";
    public static final String VENDAS_CLIENTES = "clientes";
    public static final String VENDAS_VENDEDORES = "vendedores";
    public static final String VENDAS_FORNECEDORES = "fornecedores";

    // ======== Referências Base ========

    static FirebaseFirestore db() {
        return ConfiguracaoFirestore.getFirestore();
    }

    @NonNull
    public static String requireUid() {
        return FirebaseSession.getUserId();
    }

    @NonNull
    public static DocumentReference myUserDoc() {
        return userDoc(requireUid());
    }

    @NonNull
    public static DocumentReference userDoc(@NonNull String uid) {
        return db().collection(ROOT).document(uid);
    }

    // ======== CONFIG ========

    @NonNull
    public static DocumentReference configPerfilDoc() {
        return myUserDoc().collection(CONFIG).document(PERFIL);
    }

    @NonNull
    public static DocumentReference configPreferenciasDoc() {
        return myUserDoc().collection(CONFIG).document(PREFERENCIAS);
    }

    @NonNull
    public static DocumentReference configSegurancaDoc() {
        return myUserDoc().collection(CONFIG).document(SEGURANCA);
    }

    // ======== MÓDULO CONTAS  ========

    /**
     * Retorna o Documento Mágico 'resumo_geral'.
     * Aqui residem os saldos totais e indicadores de saúde financeira.
     */
    @NonNull
    public static DocumentReference contasResumoDoc() {
        return myUserDoc().collection(CONTAS).document(RESUMO_GERAL);
    }

    @NonNull
    public static CollectionReference contasCategoriasCol() {
        return contasResumoDoc().collection(CONTAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference contasCategoriaDoc(@NonNull String categoriaId) {
        return contasCategoriasCol().document(categoriaId);
    }

    @NonNull
    public static CollectionReference contasMovimentacoesCol() {
        return contasResumoDoc().collection(CONTAS_MOVIMENTACOES);
    }

    @NonNull
    public static DocumentReference contasMovimentacaoDoc(@NonNull String movId) {
        return contasMovimentacoesCol().document(movId);
    }

    @NonNull
    public static CollectionReference contasFuturasCol() {
        return contasResumoDoc().collection(CONTAS_A_PAGAR_RECEBER);
    }

    @NonNull
    public static DocumentReference contasFuturaDoc(@NonNull String contaId) {
        return contasFuturasCol().document(contaId);
    }

    // ======== VENDAS ========

    @NonNull
    public static DocumentReference vendasResumoDoc() {
        return myUserDoc().collection(VENDAS).document(VENDAS_RESUMO_GERAL);
    }

    @NonNull
    public static CollectionReference vendasCategoriasCol() {
        return vendasResumoDoc().collection(VENDAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference vendasCategoriaDoc(@NonNull String categoriaId) {
        return vendasCategoriasCol().document(categoriaId);
    }

    @NonNull
    public static CollectionReference vendasProdutosCol() {
        return vendasResumoDoc().collection(VENDAS_PRODUTOS);
    }

    @NonNull
    public static DocumentReference vendasProdutoDoc(@NonNull String produtoId) {
        return vendasProdutosCol().document(produtoId);
    }

    @NonNull
    public static CollectionReference vendasCaixaCol() {
        return vendasResumoDoc().collection(VENDAS_CAIXA);
    }

    @NonNull
    public static DocumentReference vendasCaixaDoc(@NonNull String caixaId) {
        return vendasCaixaCol().document(caixaId);
    }

    @NonNull
    public static CollectionReference vendasVendasCol() {
        return vendasResumoDoc().collection(VENDAS_VENDAS);
    }

    @NonNull
    public static DocumentReference vendaDoc(@NonNull String vendaId) {
        return vendasVendasCol().document(vendaId);
    }

    @NonNull
    public static CollectionReference vendasClientesCol() {
        return vendasResumoDoc().collection(VENDAS_CLIENTES);
    }

    @NonNull
    public static DocumentReference clienteDoc(@NonNull String clienteId) {
        return vendasClientesCol().document(clienteId);
    }

    @NonNull
    public static CollectionReference vendasVendedoresCol() {
        return vendasResumoDoc().collection(VENDAS_VENDEDORES);
    }

    @NonNull
    public static DocumentReference vendedorDoc(@NonNull String vendedorId) {
        return vendasVendedoresCol().document(vendedorId);
    }

    @NonNull
    public static CollectionReference vendasFornecedoresCol() {
        return vendasResumoDoc().collection(VENDAS_FORNECEDORES);
    }

    @NonNull
    public static DocumentReference fornecedorDoc(@NonNull String fornecedorId) {
        return vendasFornecedoresCol().document(fornecedorId);
    }

    // ======== Helpers de Utilidade ========

    @NonNull
    public static String diaKey(@Nullable Date date) {
        Date d = (date != null) ? date : new Date();
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    @NonNull
    public static String mesKey(@Nullable Date date) {
        Date d = (date != null) ? date : new Date();
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(d);
    }

    @NonNull
    public static Timestamp nowTs() {
        return Timestamp.now();
    }
}