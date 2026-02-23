package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * MovimentacaoRepository (Unificado e Blindado)
 * Agora, "Histórico" e "Contas Futuras" moram no mesmo lugar.
 * O que diferencia um do outro é apenas o status "pago" (true ou false).
 */
public class MovimentacaoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Interfaces para avisar a tela (UI) quando o banco terminar o trabalho
    public interface Callback {
        void onSucesso(String msg);
        void onErro(String erro);
    }

    public interface DadosCallback {
        void onSucesso(List<MovimentacaoModel> lista);
        void onErro(String erro);
    }

    // Interface específica para paginação, que devolve o marcador da próxima página
    public interface DadosPaginadosCallback {
        void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDocumento);
        void onErro(String erro);
    }

    // =========================================================================
    // 1. QUERIES (BUSCA DE DADOS NO FIREBASE)
    // =========================================================================

    /**
     * Busca o Histórico: Tudo que foi pago ou lançado até a data de hoje.
     * Ordena do mais recente para o mais antigo (DESCENDING).
     */
    public void recuperarHistorico(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereLessThanOrEqualTo(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, dataReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                // Limite de segurança para não estourar a cota de leitura do Firebase
                .limit(100)
                .get()
                .addOnSuccessListener(querySnapshots -> callback.onSucesso(processarSnapshots(querySnapshots)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Recupera o Histórico em blocos (Paginação/Scroll Infinito).
     * O ViewModel passará o 'ultimoDocumento' para continuar a lista de onde parou.
     */
    public void recuperarHistoricoPaginado(Date dataReferencia, DocumentSnapshot ultimoDocumentoVisivel, DadosPaginadosCallback callback) {
        Query query = FirestoreSchema.contasMovimentacoesCol()
                .whereLessThanOrEqualTo(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, dataReferencia)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .limit(100);

        // Se já existe um documento de referência, começa a buscar DEPOIS dele
        if (ultimoDocumentoVisivel != null) {
            query = query.startAfter(ultimoDocumentoVisivel);
        }

        query.get()
                .addOnSuccessListener(querySnapshots -> {
                    List<MovimentacaoModel> lista = processarSnapshots(querySnapshots);

                    DocumentSnapshot novoUltimoDoc = null;
                    if (!querySnapshots.isEmpty()) {
                        // Salva o último documento desta busca para ser o "marcador" da próxima
                        novoUltimoDoc = querySnapshots.getDocuments().get(querySnapshots.size() - 1);
                    }

                    callback.onSucesso(lista, novoUltimoDoc);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Busca as Pendências: Tudo que tem status 'pago == false'.
     * Ordena do vencimento mais antigo para o mais distante (ASCENDING).
     */
    public void recuperarContasFuturas(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                // Garante que só mostre na aba de futuros o que está pendente
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> callback.onSucesso(processarSnapshots(querySnapshots)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * [NOVO]: Recupera Contas Futuras em blocos (Paginação).
     */
    public void recuperarContasFuturasPaginado(Date dataReferencia, DocumentSnapshot ultimoDocumentoVisivel, DadosPaginadosCallback callback) {
        Query query = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .limit(100);

        // Se tem um marcador de página, continua a partir dele
        if (ultimoDocumentoVisivel != null) {
            query = query.startAfter(ultimoDocumentoVisivel);
        }

        query.get()
                .addOnSuccessListener(querySnapshots -> {
                    List<MovimentacaoModel> lista = processarSnapshots(querySnapshots);

                    DocumentSnapshot novoUltimoDoc = null;
                    if (!querySnapshots.isEmpty()) {
                        novoUltimoDoc = querySnapshots.getDocuments().get(querySnapshots.size() - 1);
                    }

                    callback.onSucesso(lista, novoUltimoDoc);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // Utilitário para transformar o "JSON" do Firebase no nosso MovimentacaoModel
    private List<MovimentacaoModel> processarSnapshots(Iterable<QueryDocumentSnapshot> snapshots) {
        List<MovimentacaoModel> lista = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshots) {
            MovimentacaoModel mov = doc.toObject(MovimentacaoModel.class);
            mov.setId(doc.getId()); // Essencial: Salva o ID do documento dentro do objeto
            lista.add(mov);
        }
        return lista;
    }

    // =========================================================================
    // 2. ESCRITA (CRIAR, EXCLUIR, EDITAR E CONFIRMAR)
    // =========================================================================

    public void salvar(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();

        DocumentReference movRef = FirestoreSchema.contasMovimentacoesCol().document();
        mov.setId(movRef.getId());
        batch.set(movRef, mov);

        aplicarImpacto(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Lançado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void excluir(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();
        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
        batch.delete(movRef);

        aplicarImpacto(batch, mov, -1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void editar(MovimentacaoModel movAntigo, MovimentacaoModel movNovo, Callback callback) {
        WriteBatch batch = db.batch();
        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(movNovo.getId());

        batch.set(movRef, movNovo);

        aplicarImpacto(batch, movAntigo, -1);
        aplicarImpacto(batch, movNovo, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Editado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        if (mov.isPago()) {
            callback.onErro("Esta movimentação já consta como paga.");
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference movRef = FirestoreSchema.contasMovimentacaoDoc(mov.getId());

        impactoPendente(batch, mov, -1);

        mov.setPago(true);
        batch.update(movRef, MovimentacaoModel.CAMPO_PAGO, true);

        impactoRealizado(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Confirmado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // =========================================================================
    // 3. O CORAÇÃO FINANCEIRO (CÁLCULOS NO BATCH)
    // =========================================================================

    private void aplicarImpacto(WriteBatch batch, MovimentacaoModel mov, int fator) {
        if (mov.isPago()) {
            impactoRealizado(batch, mov, fator);
        } else {
            impactoPendente(batch, mov, fator);
        }
    }

    private void impactoRealizado(WriteBatch batch, MovimentacaoModel mov, int fator) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        int valorCentavos = Math.abs(mov.getValor());
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);

        batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_SALDO_ATUAL,
                FieldValue.increment(isReceita ? valorCentavos * fator : -valorCentavos * fator));

        if (mov.getData_movimentacao() != null && isMesmoMesEAno(mov.getData_movimentacao().toDate(), new Date())) {

            int deltaBalanco = (isReceita ? valorCentavos : -valorCentavos) * fator;
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, FieldValue.increment(deltaBalanco));

            if (isReceita) {
                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, FieldValue.increment(valorCentavos * fator));
            } else {
                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, FieldValue.increment(valorCentavos * fator));

                if (mov.getCategoria_id() != null && !mov.getCategoria_id().isEmpty()) {
                    DocumentReference catRef = FirestoreSchema.contasCategoriaDoc(mov.getCategoria_id());
                    batch.update(catRef, ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, FieldValue.increment(valorCentavos * fator));
                }
            }
        }
    }

    private void impactoPendente(WriteBatch batch, MovimentacaoModel mov, int fator) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);

        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(resumoRef, campoPendencia, FieldValue.increment(fator));
    }

    // =========================================================================
    // 4. VIRADA DE MÊS NO APP E UTILS
    // =========================================================================

    public void zerarEstatisticasMensais(Callback callback) {
        FirestoreSchema.contasCategoriasCol().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, 0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, 0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, 0);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        DocumentReference catRef = doc.getReference();
                        batch.update(catRef, ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, 0);
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSucesso("Estatísticas do mês zeradas com sucesso!"))
                            .addOnFailureListener(e -> callback.onErro(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao buscar categorias: " + e.getMessage()));
    }

    private boolean isMesmoMesEAno(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }
}