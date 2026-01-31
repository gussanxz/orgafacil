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
 * Centraliza TODOS os caminhos do Firestore.
 *
 * REGRA:
 * - Firestore NÃO conhece Auth
 * - UID é responsabilidade da aplicação
 *
 * Este schema oferece:
 * ✔ API nova (UID explícito)
 * ✔ Overloads de compatibilidade (legado)
 */
public final class FirestoreSchema {

    private FirestoreSchema() {}

    // ================= ROOT =================

    public static final String ROOT = "teste";

    // ================= NOMES FIXOS =================

    public static final String CONFIG = "config";
    public static final String MODULO = "moduloSistema";

    // config
    public static final String PERFIL = "config_perfil";
    public static final String PREFERENCIAS = "config_preferencias";
    public static final String SEGURANCA = "config_seguranca";

    // contas
    public static final String CONTAS = "contas";
    public static final String CONTAS_CATEGORIAS = "contas_categorias";
    public static final String CONTAS_MOV = "contas_movimentacoes";
    public static final String CONTAS_FUTURAS = "contas_futuras";
    public static final String CONTAS_RESUMOS = "contas_resumos";
    public static final String CONTAS_ULTIMOS = "contas_ultimos";

    // vendas
    public static final String VENDAS = "vendas";
    public static final String VENDAS_CATEGORIAS = "vendas_categorias";
    public static final String VENDAS_CATALOGO = "vendas_catalogo";
    public static final String VENDAS_CAIXA = "vendas_caixa";
    public static final String VENDAS_VENDAS = "vendas_vendas";
    public static final String VENDAS_CLIENTES = "vendas_clientes";
    public static final String VENDAS_VENDEDORES = "vendas_vendedores";
    public static final String VENDAS_FORNECEDORES = "vendas_fornecedores";

    // ================= BASE =================

    @NonNull
    public static FirebaseFirestore db() {
        return ConfiguracaoFirestore.getFirestore();
    }

    // ================= UID HELPERS =================

    @Nullable
    public static String uidOrNull() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @NonNull
    public static String requireUid() {
        String uid = uidOrNull();
        if (uid == null) throw new IllegalStateException("Usuário não logado");
        return uid;
    }

    // ================= ROOT DOC =================

    @NonNull
    public static DocumentReference userDoc(@NonNull String uid) {
        return db().collection(ROOT).document(uid);
    }

    // ================= CONFIG =================

    @NonNull
    public static DocumentReference configPerfilDoc(@NonNull String uid) {
        return userDoc(uid).collection(CONFIG).document(PERFIL);
    }

    // ================= CONTAS =================

    @NonNull
    public static CollectionReference contasMovimentacoesCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(CONTAS)
                .collection(CONTAS_MOV);
    }

    @NonNull
    public static DocumentReference contasMovimentacaoDoc(
            @NonNull String uid,
            @NonNull String movId
    ) {
        return contasMovimentacoesCol(uid).document(movId);
    }

    @NonNull
    public static CollectionReference contasCategoriasCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(CONTAS)
                .collection(CONTAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference contasCategoriaDoc(
            @NonNull String uid,
            @NonNull String categoriaId
    ) {
        return contasCategoriasCol(uid).document(categoriaId);
    }

    @NonNull
    public static DocumentReference contasResumoUltimosDoc(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(CONTAS)
                .collection(CONTAS_RESUMOS)
                .document(CONTAS_ULTIMOS);
    }

    // ================= VENDAS =================

    public static CollectionReference vendasCategoriasCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_CATEGORIAS);
    }

    @NonNull
    public static DocumentReference vendasCategoriaDoc(
            @NonNull String uid,
            @NonNull String categoriaId
    ) {
        return vendasCategoriasCol(uid).document(categoriaId);
    }

    // ---------- CATÁLOGO ----------

    @NonNull
    public static CollectionReference vendasCatalogoCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_CATALOGO);
    }

    @NonNull
    public static DocumentReference vendasProdutoDoc(
            @NonNull String uid,
            @NonNull String produtoId
    ) {
        return vendasCatalogoCol(uid).document(produtoId);
    }

    // ---------- CAIXA ----------

    @NonNull
    public static CollectionReference vendasCaixaCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_CAIXA);
    }

    @NonNull
    public static DocumentReference vendasCaixaDoc(
            @NonNull String uid,
            @NonNull String caixaId
    ) {
        return vendasCaixaCol(uid).document(caixaId);
    }

    // ---------- VENDAS ----------

    @NonNull
    public static CollectionReference vendasVendasCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_VENDAS);
    }

    @NonNull
    public static DocumentReference vendaDoc(
            @NonNull String uid,
            @NonNull String vendaId
    ) {
        return vendasVendasCol(uid).document(vendaId);
    }

    // ---------- PESSOAS ----------

    @NonNull
    public static CollectionReference vendasClientesCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_CLIENTES);
    }

    @NonNull
    public static DocumentReference clienteDoc(
            @NonNull String uid,
            @NonNull String clienteId
    ) {
        return vendasClientesCol(uid).document(clienteId);
    }

    @NonNull
    public static CollectionReference vendasVendedoresCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_VENDEDORES);
    }

    @NonNull
    public static DocumentReference vendedorDoc(
            @NonNull String uid,
            @NonNull String vendedorId
    ) {
        return vendasVendedoresCol(uid).document(vendedorId);
    }

    @NonNull
    public static CollectionReference vendasFornecedoresCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(VENDAS)
                .collection(VENDAS_FORNECEDORES);
    }

    @NonNull
    public static DocumentReference fornecedorDoc(
            @NonNull String uid,
            @NonNull String fornecedorId
    ) {
        return vendasFornecedoresCol(uid).document(fornecedorId);
    }

    // ================= CONTAS FUTURAS =================

    @NonNull
    public static CollectionReference contasFuturasCol(@NonNull String uid) {
        return userDoc(uid)
                .collection(MODULO)
                .document(CONTAS)
                .collection(CONTAS_FUTURAS);
    }

    @NonNull
    public static DocumentReference contasFuturaDoc(
            @NonNull String uid,
            @NonNull String contaFuturaId
    ) {
        return contasFuturasCol(uid).document(contaFuturaId);
    }

    // ===== CONTAS FUTURAS (LEGADO) =====

    public static CollectionReference contasFuturasCol() {
        return contasFuturasCol(requireUid());
    }

    public static DocumentReference contasFuturaDoc(@NonNull String contaFuturaId) {
        return contasFuturaDoc(requireUid(), contaFuturaId);
    }


    // ================= LEGADO (COMPAT) =================
    // ⚠ NÃO USAR EM CÓDIGO NOVO

    public static CollectionReference contasMovimentacoesCol() {
        return contasMovimentacoesCol(requireUid());
    }

    public static DocumentReference contasMovimentacaoDoc(@NonNull String movId) {
        return contasMovimentacaoDoc(requireUid(), movId);
    }

    public static CollectionReference contasCategoriasCol() {
        return contasCategoriasCol(requireUid());
    }

    public static DocumentReference contasCategoriaDoc(@NonNull String categoriaId) {
        return contasCategoriaDoc(requireUid(), categoriaId);
    }

    public static DocumentReference contasResumoUltimosDoc() {
        return contasResumoUltimosDoc(requireUid());
    }

    // ================= HELPERS =================

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
