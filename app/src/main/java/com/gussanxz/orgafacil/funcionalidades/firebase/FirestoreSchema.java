package com.gussanxz.orgafacil.funcionalidades.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FirestoreSchema
 *
 * Centraliza TODOS os caminhos do Firestore (rotas) do OrgaFacil.
 * Assim, qualquer mudança de estrutura exige alteração em 1 único arquivo.
 *
 * Modelo alvo:
 * usuarios/{uid}/config/{perfil|preferencias|seguranca}
 * usuarios/{uid}/moduloSistema/{contas|vendas}/...
 */
public final class FirestoreSchema {

    private FirestoreSchema() {}

    // ======== Ajuste rápido de "coleção raiz" ========
    // Se um dia vocês decidirem trocar "usuarios" -> "users", muda só aqui.
    public static final String ROOT = "teste";

    // ======== Nomes fixos do modelo ========
    public static final String CONFIG = "config";
    public static final String MODULO = "moduloSistema";

    // config docs
    public static final String PERFIL = "config_perfil";
    public static final String PREFERENCIAS = "config_preferencias";
    public static final String SEGURANCA = "config_seguranca";

    // moduloSistema/contas
    public static final String CONTAS = "contas";
    public static final String CONTAS_CATEGORIAS = "contas_categorias";
    public static final String CONTAS_MOV = "contas_movimentacoes";
    public static final String CONTAS_FUTURAS = "contas_futuras";
    public static final String CONTAS_RESUMOS = "contas_resumos";
    public static final String CONTAS_ULTIMOS = "contas_ultimos";

    // moduloSistema/vendas
    public static final String VENDAS = "vendas";
    public static final String VENDAS_CATEGORIAS = "vendas_categorias";
    public static final String VENDAS_CATALOGO = "vendas_catalogo";
    public static final String VENDAS_CAIXA = "vendas_caixa";
    public static final String VENDAS_VENDAS = "vendas_vendas";
    public static final String VENDAS_CLIENTES = "vendas_clientes";
    public static final String VENDAS_VENDEDORES = "vendas_vendedores";
    public static final String VENDAS_FORNECEDORES = "vendas_fornecedores";

    // ======== Base providers ========
    @NonNull
    public static FirebaseFirestore db() {
        return ConfiguracaoFirestore.getFirestore();
    }

    @Nullable
    public static String uidOrNull() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @NonNull
    public static String requireUid() {
        String uid = uidOrNull();
        if (uid == null) throw new IllegalStateException("Usuário não logado (uid null).");
        return uid;
    }

    // ======== Root: usuarios/{uid} ========
    @NonNull
    public static DocumentReference userDoc() {
        return db().collection(ROOT).document(requireUid());
    }

    @NonNull
    public static DocumentReference userDoc(@NonNull String uid) {
        return db().collection(ROOT).document(uid);
    }

    // ======== CONFIG ========
    @NonNull
    public static DocumentReference configPerfilDoc() {
        return userDoc().collection(CONFIG).document(PERFIL);
    }

    @NonNull
    public static DocumentReference configPreferenciasDoc() {
        return userDoc().collection(CONFIG).document(PREFERENCIAS);
    }

    @NonNull
    public static DocumentReference configSegurancaDoc() {
        return userDoc().collection(CONFIG).document(SEGURANCA);
    }

    // ======== MODULO SISTEMA / CONTAS ========
    @NonNull
    public static CollectionReference contasCategoriasCol() {
        return userDoc().collection(MODULO).document(CONTAS).collection(CONTAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference contasCategoriaDoc(@NonNull String categoriaId) {
        return contasCategoriasCol().document(categoriaId);
    }

    @NonNull
    public static CollectionReference contasMovimentacoesCol() {
        return userDoc().collection(MODULO).document(CONTAS).collection(CONTAS_MOV);
    }

    @NonNull
    public static DocumentReference contasMovimentacaoDoc(@NonNull String movId) {
        return contasMovimentacoesCol().document(movId);
    }

    @NonNull
    public static CollectionReference contasFuturasCol() {
        return userDoc().collection(MODULO).document(CONTAS).collection(CONTAS_FUTURAS);
    }

    @NonNull
    public static DocumentReference contasFuturaDoc(@NonNull String contaFuturaId) {
        return contasFuturasCol().document(contaFuturaId);
    }

    /**
     * usuarios/{uid}/moduloSistema/contas/Resumos/ultimos
     */
    @NonNull
    public static DocumentReference contasResumoUltimosDoc() {
        return userDoc().collection(MODULO).document(CONTAS)
                .collection(CONTAS_RESUMOS).document(CONTAS_ULTIMOS);
    }

    // ======== MODULO SISTEMA / VENDAS ========
    @NonNull
    public static CollectionReference vendasCategoriasCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference vendasCategoriaDoc(@NonNull String categoriaId) {
        return vendasCategoriasCol().document(categoriaId);
    }

    @NonNull
    public static CollectionReference vendasCatalogoCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CATALOGO);
    }

    @NonNull
    public static DocumentReference vendasProdutoDoc(@NonNull String produtoId) {
        return vendasCatalogoCol().document(produtoId);
    }

    @NonNull
    public static CollectionReference vendasCaixaCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CAIXA);
    }

    @NonNull
    public static DocumentReference vendasCaixaDoc(@NonNull String caixaId) {
        return vendasCaixaCol().document(caixaId);
    }

    @NonNull
    public static CollectionReference vendasVendasCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_VENDAS);
    }

    @NonNull
    public static DocumentReference vendaDoc(@NonNull String vendaId) {
        return vendasVendasCol().document(vendaId);
    }

    @NonNull
    public static CollectionReference vendasClientesCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_CLIENTES);
    }

    @NonNull
    public static DocumentReference clienteDoc(@NonNull String clienteId) {
        return vendasClientesCol().document(clienteId);
    }

    @NonNull
    public static CollectionReference vendasVendedoresCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_VENDEDORES);
    }

    @NonNull
    public static DocumentReference vendedorDoc(@NonNull String vendedorId) {
        return vendasVendedoresCol().document(vendedorId);
    }

    @NonNull
    public static CollectionReference vendasFornecedoresCol() {
        return userDoc().collection(MODULO).document(VENDAS).collection(VENDAS_FORNECEDORES);
    }

    @NonNull
    public static DocumentReference fornecedorDoc(@NonNull String fornecedorId) {
        return vendasFornecedoresCol().document(fornecedorId);
    }

    // ======== Helpers de data (diaKey / mesKey) ========
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