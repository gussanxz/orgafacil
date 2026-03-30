package com.gussanxz.orgafacil.funcionalidades.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
    public static final String ROOT   = "teste"; // Mudar para "usuarios" em produção
    public static final String CONFIG = "config";
    public static final String MODULO = "moduloSistema";

    // Sub-documentos de Configuração
    public static final String PERFIL       = "config_perfil";
    public static final String PREFERENCIAS = "config_preferencias";
    public static final String SEGURANCA    = "config_seguranca";

    // ======== MÓDULO CONTAS ========
    public static final String CONTAS       = "contas";
    public static final String RESUMO_GERAL = "resumo_geral";

    // Subcoleções
    public static final String CONTAS_MOVIMENTACOES      = "contas_movimentacoes";
    public static final String CONTAS_CATEGORIAS         = "contas_categorias";
    public static final String CONTAS_A_PAGAR_RECEBER    = "contas_a_pagar_receber";

    // ======== MÓDULO VENDAS ========
    public static final String VENDAS            = "vendas";
    public static final String VENDAS_CATEGORIAS = "vendas_categorias";
    public static final String VENDAS_CATALOGO   = "vendas_catalogo";
    public static final String VENDAS_CAIXA      = "vendas_caixa";
    public static final String VENDAS_VENDAS     = "vendas_vendas";
    public static final String VENDAS_CLIENTES   = "vendas_clientes";
    public static final String VENDAS_VENDEDORES = "vendedores";
    public static final String VENDAS_FORNECEDORES = "fornecedores";

    // ======== MÓDULO MERCADO ========
    public static final String MERCADO              = "mercado";
    public static final String MERCADO_LISTAS       = "mercado_listas";
    public static final String MERCADO_ITENS        = "mercado_itens";
    public static final String MERCADO_MEMORIA_PRECOS = "mercado_memoria_precos";

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

    // ======== MÓDULO CONTAS ========

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

    // ======== MÓDULO SISTEMA / VENDAS ========

    @NonNull
    public static CollectionReference vendasCategoriasCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference vendasCategoriaDoc(@NonNull String categoriaId) {
        return vendasCategoriasCol().document(categoriaId);
    }

    @NonNull
    public static CollectionReference vendasCatalogoCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CATALOGO);
    }

    @NonNull
    public static DocumentReference vendasProdutoDoc(@NonNull String produtoId) {
        return vendasCatalogoCol().document(produtoId);
    }

    @NonNull
    public static Query vendasCatalogoPorTipoQuery(@NonNull String tipo) {
        return vendasCatalogoCol().whereEqualTo("tipo", tipo);
    }

    @NonNull
    public static CollectionReference vendasCaixaCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CAIXA);
    }

    @NonNull
    public static DocumentReference vendasCaixaDoc(@NonNull String caixaId) {
        return vendasCaixaCol().document(caixaId);
    }

    @NonNull
    public static DocumentReference vendasResumoDoc() {
        return myUserDoc().collection(MODULO).document(VENDAS);
    }

    @NonNull
    public static CollectionReference vendasVendasCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_VENDAS);
    }

    @NonNull
    public static DocumentReference vendaDoc(@NonNull String vendaId) {
        return vendasVendasCol().document(vendaId);
    }

    @NonNull
    public static CollectionReference vendasClientesCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CLIENTES);
    }

    @NonNull
    public static DocumentReference clienteDoc(@NonNull String clienteId) {
        return vendasClientesCol().document(clienteId);
    }

    @NonNull
    public static CollectionReference vendasVendedoresCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_VENDEDORES);
    }

    @NonNull
    public static DocumentReference vendedorDoc(@NonNull String vendedorId) {
        return vendasVendedoresCol().document(vendedorId);
    }

    @NonNull
    public static CollectionReference vendasFornecedoresCol() {
        return myUserDoc().collection(MODULO).document(VENDAS).collection(VENDAS_FORNECEDORES);
    }

    @NonNull
    public static DocumentReference fornecedorDoc(@NonNull String fornecedorId) {
        return vendasFornecedoresCol().document(fornecedorId);
    }

    // ======== MÓDULO MERCADO ========

    /**
     * Raiz do módulo mercado do usuário.
     * Caminho: ROOT/{uid}/moduloSistema/mercado
     */
    @NonNull
    public static DocumentReference mercadoRaizDoc() {
        return myUserDoc().collection(MODULO).document(MERCADO);
    }

    /**
     * Coleção de listas de mercado.
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_listas
     */
    @NonNull
    public static CollectionReference mercadoListasCol() {
        return mercadoRaizDoc().collection(MERCADO_LISTAS);
    }

    /**
     * Documento de uma lista específica.
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_listas/{listaId}
     */
    @NonNull
    public static DocumentReference mercadoListaDoc(@NonNull String listaId) {
        return mercadoListasCol().document(listaId);
    }

    /**
     * Coleção de itens de uma lista específica.
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_listas/{listaId}/mercado_itens
     */
    @NonNull
    public static CollectionReference mercadoItensCol(@NonNull String listaId) {
        return mercadoListaDoc(listaId).collection(MERCADO_ITENS);
    }

    /**
     * Documento de um item específico dentro de uma lista.
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_listas/{listaId}/mercado_itens/{itemId}
     */
    @NonNull
    public static DocumentReference mercadoItemDoc(@NonNull String listaId,
                                                   @NonNull String itemId) {
        return mercadoItensCol(listaId).document(itemId);
    }

    /**
     * Coleção de memória de preços (RF06).
     * Chave = nome do produto (normalizado em lowercase).
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_memoria_precos
     */
    @NonNull
    public static CollectionReference mercadoMemoriaPrecosCol() {
        return mercadoRaizDoc().collection(MERCADO_MEMORIA_PRECOS);
    }

    /**
     * Documento de memória de preço para um produto específico.
     * Caminho: ROOT/{uid}/moduloSistema/mercado/mercado_memoria_precos/{nomeProdutoKey}
     */
    @NonNull
    public static DocumentReference mercadoMemoriaPrecoDoc(@NonNull String nomeProdutoKey) {
        // Normaliza o nome para usar como chave do documento (lowercase, sem espaços extras)
        return mercadoMemoriaPrecosCol().document(nomeProdutoKey.trim().toLowerCase());
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