package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository;

import com.google.firebase.Timestamp;
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

public class MovimentacaoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onSucesso(String msg);
        void onErro(String erro);
    }

    public interface DadosCallback {
        void onSucesso(List<MovimentacaoModel> lista);
        void onErro(String erro);
    }

    public interface DadosPaginadosCallback {
        void onSucesso(List<MovimentacaoModel> lista, DocumentSnapshot ultimoDocumento);
        void onErro(String erro);
    }

    // ==========================================
    // HISTÓRICO: DECRESCENTE (Mais recente -> Mais antigo)
    // ==========================================
    public void recuperarHistorico(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, true)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(querySnapshots -> callback.onSucesso(processarSnapshots(querySnapshots)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarHistoricoPaginado(Date dataReferencia, DocumentSnapshot ultimoDocumentoVisivel, DadosPaginadosCallback callback) {
        Query query = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, true)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.DESCENDING)
                .limit(100);

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

    // ==========================================
    // PENDÊNCIAS: CRESCENTE (Urgente/Atrasada -> Futuro)
    // ==========================================
    public void recuperarContasFuturas(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> callback.onSucesso(processarSnapshots(querySnapshots)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarContasFuturasPaginado(Date dataReferencia, DocumentSnapshot ultimoDocumentoVisivel, DadosPaginadosCallback callback) {
        Query query = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .limit(100);

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

    private List<MovimentacaoModel> processarSnapshots(Iterable<QueryDocumentSnapshot> snapshots) {
        List<MovimentacaoModel> lista = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshots) {
            MovimentacaoModel mov = doc.toObject(MovimentacaoModel.class);
            mov.setId(doc.getId());
            lista.add(mov);
        }
        return lista;
    }

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

        Timestamp dataVencimentoAntiga = mov.getData_movimentacao();
        batch.update(movRef, "data_vencimento_original", dataVencimentoAntiga);

        mov.setPago(true);
        Timestamp dataPagamentoReal = Timestamp.now();
        mov.setData_movimentacao(dataPagamentoReal);

        batch.update(movRef, MovimentacaoModel.CAMPO_PAGO, true);
        batch.update(movRef, MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, dataPagamentoReal);

        impactoRealizado(batch, mov, 1);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Confirmado e registrado na data de hoje!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

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

    public void salvarRecorrente(MovimentacaoModel movBase, int qtdMeses, Callback callback) {
        WriteBatch batch = db.batch();
        Calendar calendar = Calendar.getInstance();
        String grupoId = java.util.UUID.randomUUID().toString();

        for (int i = 0; i < qtdMeses; i++) {
            MovimentacaoModel parcela = new MovimentacaoModel();

            parcela.setDescricao(movBase.getDescricao() + (qtdMeses > 1 ? " (" + (i + 1) + "/" + qtdMeses + ")" : ""));
            parcela.setValor(movBase.getValor());
            parcela.setCategoria_id(movBase.getCategoria_id());
            parcela.setCategoria_nome(movBase.getCategoria_nome());
            parcela.setTipoEnum(movBase.getTipoEnum());

            parcela.setRecorrencia_id(grupoId);
            parcela.setParcela_atual(i + 1);
            parcela.setTotal_parcelas(qtdMeses);

            parcela.setPago(i == 0 && movBase.isPago());

            calendar.setTime(movBase.getData_movimentacao().toDate());
            calendar.add(Calendar.MONTH, i);
            parcela.setData_movimentacao(new Timestamp(calendar.getTime()));

            DocumentReference ref = FirestoreSchema.contasMovimentacoesCol().document();
            parcela.setId(ref.getId());
            batch.set(ref, parcela);

            aplicarImpacto(batch, parcela, 1);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Recorrência agendada!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }
}