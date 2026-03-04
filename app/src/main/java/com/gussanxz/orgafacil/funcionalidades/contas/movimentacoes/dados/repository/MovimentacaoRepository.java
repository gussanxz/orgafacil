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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MovimentacaoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ── interfaces de callback ───────────────────────────────────────────────

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

    // ── SMART BATCH (SOLUÇÃO #8) ─────────────────────────────────────────────

    /**
     * O Firestore aceita no máximo 500 operações por WriteBatch.
     * O SmartBatch gerencia isso automaticamente, criando novos lotes
     * quando atinge 400 operações e comitando todos em sequência.
     */
    private class SmartBatch {
        private final FirebaseFirestore firestore;
        private WriteBatch loteAtual;
        private int contadorOperacoes = 0;
        private final List<WriteBatch> todosOsLotes = new ArrayList<>();

        public SmartBatch(FirebaseFirestore firestore) {
            this.firestore = firestore;
            novoLote();
        }

        private void novoLote() {
            loteAtual = firestore.batch();
            todosOsLotes.add(loteAtual);
            contadorOperacoes = 0;
        }

        private void checarLimite() {
            // Limite conservador de 400 para evitar margem de erro
            if (contadorOperacoes >= 400) {
                novoLote();
            }
            contadorOperacoes++;
        }

        public void set(DocumentReference ref, Object data) {
            checarLimite();
            loteAtual.set(ref, data);
        }

        public void update(DocumentReference ref, String field, Object value) {
            checarLimite();
            loteAtual.update(ref, field, value);
        }

        public void delete(DocumentReference ref) {
            checarLimite();
            loteAtual.delete(ref);
        }

        public void commit(Callback callback, String mensagemSucesso) {
            commitRecursivo(0, callback, mensagemSucesso);
        }

        private void commitRecursivo(int index, Callback callback, String mensagemSucesso) {
            if (index >= todosOsLotes.size()) {
                callback.onSucesso(mensagemSucesso);
                return;
            }
            todosOsLotes.get(index).commit()
                    .addOnSuccessListener(v -> commitRecursivo(index + 1, callback, mensagemSucesso))
                    .addOnFailureListener(e -> callback.onErro("Falha no lote " + (index + 1) + ": " + e.getMessage()));
        }
    }

    // ── leitura ──────────────────────────────────────────────────────────────

    public void recuperarHistorico(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, true)
                .orderBy("data_pagamento", Query.Direction.ASCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(s -> callback.onSucesso(processarSnapshots(s)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarHistoricoPaginado(Date dataReferencia, DocumentSnapshot ultimoDoc,
                                           DadosPaginadosCallback callback) {
        Query q = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, true)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .limit(100);
        if (ultimoDoc != null) q = q.startAfter(ultimoDoc);
        q.get()
                .addOnSuccessListener(s -> {
                    DocumentSnapshot novoUltimo = s.isEmpty() ? null
                            : s.getDocuments().get(s.size() - 1);
                    callback.onSucesso(processarSnapshots(s), novoUltimo);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarContasFuturas(Date dataReferencia, DadosCallback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy("data_vencimento", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(s -> callback.onSucesso(processarSnapshots(s)))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void recuperarContasFuturasPaginado(Date dataReferencia, DocumentSnapshot ultimoDoc,
                                               DadosPaginadosCallback callback) {
        Query q = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .orderBy(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, Query.Direction.ASCENDING)
                .limit(100);
        if (ultimoDoc != null) q = q.startAfter(ultimoDoc);
        q.get()
                .addOnSuccessListener(s -> {
                    DocumentSnapshot novoUltimo = s.isEmpty() ? null
                            : s.getDocuments().get(s.size() - 1);
                    callback.onSucesso(processarSnapshots(s), novoUltimo);
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    private List<MovimentacaoModel> processarSnapshots(Iterable<QueryDocumentSnapshot> snaps) {
        List<MovimentacaoModel> lista = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snaps) {
            MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
            m.setId(doc.getId());
            lista.add(m);
        }
        return lista;
    }

    // ── escrita simples ──────────────────────────────────────────────────────

    public void salvar(MovimentacaoModel mov, Callback callback) {
        SmartBatch batch = new SmartBatch(db);
        DocumentReference ref = FirestoreSchema.contasMovimentacoesCol().document();
        mov.setId(ref.getId());

        if (mov.getData_vencimento() == null) {
            if (mov.getData_movimentacao() != null) {
                mov.setData_vencimento(mov.getData_movimentacao());
            } else {
                mov.setData_vencimento(Timestamp.now());
                mov.setData_movimentacao(mov.getData_vencimento());
            }
        }

        batch.set(ref, mov);
        aplicarImpacto(batch, mov, 1);
        batch.commit(callback, "Lançado com sucesso!");
    }

    public void excluir(MovimentacaoModel mov, Callback callback) {
        SmartBatch batch = new SmartBatch(db);
        batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
        aplicarImpacto(batch, mov, -1);
        batch.commit(callback, "Excluído com sucesso!");
    }

    public void editar(MovimentacaoModel movAntigo, MovimentacaoModel movNovo, Callback callback) {
        SmartBatch batch = new SmartBatch(db);
        batch.set(FirestoreSchema.contasMovimentacaoDoc(movNovo.getId()), movNovo);
        aplicarImpacto(batch, movAntigo, -1);
        aplicarImpacto(batch, movNovo, 1);
        batch.commit(callback, "Editado com sucesso!");
    }

    // ── confirmação ──────────────────────────────────────────────────────────

    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        if (mov.isPago()) {
            callback.onErro("Esta movimentação já consta como paga.");
            return;
        }

        SmartBatch batch = new SmartBatch(db);
        DocumentReference ref = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
        Timestamp agora = Timestamp.now();

        impactoPendente(batch, mov, -1);

        Timestamp vencAtual = mov.getData_vencimento();

        if (vencAtual == null) {
            vencAtual = mov.getData_movimentacao();
            if (vencAtual != null) {
                batch.update(ref, "data_vencimento", vencAtual);
                mov.setData_vencimento(vencAtual);
            }
        }

        batch.update(ref, MovimentacaoModel.CAMPO_PAGO, true);
        batch.update(ref, "data_pagamento", agora);

        mov.setPago(true);
        mov.setData_pagamento(agora);

        impactoRealizado(batch, mov, 1);

        batch.commit(callback, "Confirmado e registrado corretamente!");
    }

    // ── impactos no resumo ───────────────────────────────────────────────────

    private void aplicarImpacto(SmartBatch batch, MovimentacaoModel mov, int fator) {
        if (mov.isPago()) impactoRealizado(batch, mov, fator);
        else impactoPendente(batch, mov, fator);
    }

    private void impactoRealizado(SmartBatch batch, MovimentacaoModel mov, int fator) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        long centavos = Math.abs(mov.getValor());
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);

        batch.update(resumoRef,
                ResumoFinanceiroModel.CAMPO_SALDO_ATUAL,
                FieldValue.increment(isReceita ? centavos * fator : -centavos * fator));

        if (mov.getData_pagamento() != null &&
                isMesmoMesEAno(mov.getData_pagamento().toDate(), new Date())) {

            long delta = (isReceita ? centavos : -centavos) * fator;

            batch.update(resumoRef,
                    ResumoFinanceiroModel.CAMPO_BALANCO_MES,
                    FieldValue.increment(delta));

            if (isReceita) {
                batch.update(resumoRef,
                        ResumoFinanceiroModel.CAMPO_RECEITAS_MES,
                        FieldValue.increment(centavos * fator));
            } else {
                batch.update(resumoRef,
                        ResumoFinanceiroModel.CAMPO_DESPESAS_MES,
                        FieldValue.increment(centavos * fator));
            }
        }
    }

    private void impactoPendente(SmartBatch batch, MovimentacaoModel mov, int fator) {
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);
        String campo = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(FirestoreSchema.contasResumoDoc(),
                campo,
                FieldValue.increment(fator));
    }

    // ── salvar recorrente ───────────────────────────────────────

    public void salvarRecorrente(MovimentacaoModel movBase, int quantidade, Callback callback) {

        SmartBatch batch = new SmartBatch(db);
        String grupoId = java.util.UUID.randomUUID().toString();

        TipoRecorrencia tipoRec = movBase.getTipoRecorrenciaEnum();
        boolean isParcelado = (tipoRec == TipoRecorrencia.PARCELADO);

        if (quantidade <= 0) {
            callback.onErro("Quantidade inválida para recorrência.");
            return;
        }

        long valorParcela = isParcelado
                ? movBase.getValor() / quantidade
                : movBase.getValor();

        Timestamp dataBase = movBase.getData_vencimento();
        if (dataBase == null) dataBase = movBase.getData_movimentacao();
        if (dataBase == null) dataBase = Timestamp.now();

        for (int i = 0; i < quantidade; i++) {
            MovimentacaoModel parcela = new MovimentacaoModel();

            String descBase = movBase.getDescricao().replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();

            parcela.setDescricao(quantidade > 1
                    ? descBase + " (" + (i + 1) + "/" + quantidade + ")"
                    : descBase);

            parcela.setValor(valorParcela);
            parcela.setCategoria_id(movBase.getCategoria_id());
            parcela.setCategoria_nome(movBase.getCategoria_nome());
            parcela.setTipoEnum(movBase.getTipoEnum());

            parcela.setRecorrencia_id(grupoId);
            parcela.setParcela_atual(i + 1);
            parcela.setTotal_parcelas(quantidade);
            parcela.setRecorrencia_tipo(tipoRec.name());
            parcela.setRecorrencia_intervalo(movBase.getRecorrencia_intervalo());

            parcela.setPago(i == 0 && movBase.isPago());

            Timestamp vencimento;
            if (i > 0) {
                Calendar base = Calendar.getInstance();
                base.setTime(dataBase.toDate());
                calcularProximaData(base, tipoRec, movBase.getRecorrencia_intervalo(), i);
                vencimento = new Timestamp(base.getTime());
            } else {
                vencimento = dataBase;
            }

            parcela.setData_vencimento(vencimento);
            parcela.setData_movimentacao(vencimento);

            if (parcela.isPago()) parcela.setData_pagamento(Timestamp.now());
            else parcela.setData_pagamento(null);

            DocumentReference ref = FirestoreSchema.contasMovimentacoesCol().document();
            parcela.setId(ref.getId());
            batch.set(ref, parcela);

            aplicarImpacto(batch, parcela, 1);
        }

        String msgFinal = tipoRec == TipoRecorrencia.PARCELADO
                ? quantidade + " parcelas criadas!"
                : "Série recorrente agendada!";

        batch.commit(callback, msgFinal);
    }

    private void calcularProximaData(Calendar cal, TipoRecorrencia tipo, int intervalo, int passos) {
        switch (tipo) {
            case PARCELADO:
            case MENSAL:
                cal.add(Calendar.MONTH, passos);
                break;
            case SEMANAL:
                cal.add(Calendar.DAY_OF_YEAR, 7 * passos);
                break;
            case QUINZENAL:
                cal.add(Calendar.DAY_OF_YEAR, 15 * passos);
                break;
            case CADA_X_DIAS:
                int dias = (intervalo > 0) ? intervalo : 1;
                cal.add(Calendar.DAY_OF_YEAR, dias * passos);
                break;
            case CADA_X_MESES:
                int meses = (intervalo > 0) ? intervalo : 1;
                cal.add(Calendar.MONTH, meses * passos);
                break;
        }
    }

    // ── edição e exclusão em massa ───────────────────────────────────────────

    public void editarMultiplos(MovimentacaoModel movOriginal,
                                MovimentacaoModel movNova,
                                boolean todasSeguintes,
                                Callback callback) {

        if (!todasSeguintes) {
            editar(movOriginal, movNova, callback);
            return;
        }

        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", movOriginal.getRecorrencia_id())
                .whereGreaterThanOrEqualTo("parcela_atual", movOriginal.getParcela_atual())
                .get()
                .addOnSuccessListener(snap -> {
                    SmartBatch batch = new SmartBatch(db);
                    String novaDescBase = movNova.getDescricao().replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();

                    for (QueryDocumentSnapshot doc : snap) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        aplicarImpacto(batch, m, -1);

                        m.setValor(movNova.getValor());
                        m.setCategoria_id(movNova.getCategoria_id());
                        m.setCategoria_nome(movNova.getCategoria_nome());
                        m.setDescricao(novaDescBase + " (" + m.getParcela_atual() + "/" + m.getTotal_parcelas() + ")");

                        if (m.getId().equals(movOriginal.getId())) {
                            Timestamp novoVenc = movNova.getData_vencimento();
                            if (novoVenc == null) novoVenc = movNova.getData_movimentacao();
                            if (novoVenc == null) novoVenc = Timestamp.now();

                            m.setData_vencimento(novoVenc);
                            m.setData_movimentacao(novoVenc);

                            boolean novoPago = movNova.isPago();
                            m.setPago(novoPago);

                            if (novoPago) {
                                if (m.getData_pagamento() == null) m.setData_pagamento(Timestamp.now());
                            } else {
                                m.setData_pagamento(null);
                            }
                        }

                        batch.set(doc.getReference(), m);
                        aplicarImpacto(batch, m, 1);
                    }

                    batch.commit(callback, "Série atualizada com sucesso!");
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void excluirMultiplos(MovimentacaoModel movBase,
                                 boolean todasSeguintes,
                                 Callback callback) {

        if (!todasSeguintes) {
            excluir(movBase, callback);
            return;
        }

        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", movBase.getRecorrencia_id())
                .whereGreaterThanOrEqualTo("parcela_atual", movBase.getParcela_atual())
                .get()
                .addOnSuccessListener(snap -> {
                    SmartBatch batch = new SmartBatch(db);

                    for (QueryDocumentSnapshot doc : snap) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        if (m.isPago() && m.getData_pagamento() == null) {
                            m.setData_pagamento(m.getData_vencimento());
                        }

                        aplicarImpacto(batch, m, -1);
                        batch.delete(doc.getReference());
                    }

                    batch.commit(callback, "Série excluída com sucesso!");
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void confirmarMovimentacaoEmMassa(MovimentacaoModel movBase, Callback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", movBase.getRecorrencia_id())
                .whereGreaterThanOrEqualTo("parcela_atual", movBase.getParcela_atual())
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .get()
                .addOnSuccessListener(snap -> {
                    SmartBatch batch = new SmartBatch(db);
                    Timestamp agora = Timestamp.now();

                    for (QueryDocumentSnapshot doc : snap) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        impactoPendente(batch, m, -1);

                        Timestamp vencAtual = m.getData_vencimento();
                        if (vencAtual == null) {
                            vencAtual = m.getData_movimentacao();
                            if (vencAtual != null) {
                                batch.update(doc.getReference(), "data_vencimento", vencAtual);
                                m.setData_vencimento(vencAtual);
                            }
                        }

                        batch.update(doc.getReference(), MovimentacaoModel.CAMPO_PAGO, true);
                        batch.update(doc.getReference(), "data_pagamento", agora);

                        m.setPago(true);
                        m.setData_pagamento(agora);

                        impactoRealizado(batch, m, 1);
                    }

                    batch.commit(callback, "Todas as parcelas confirmadas!");
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── lote ─────────────────────────────────────────────────────────────────

    public void excluirEmLote(List<MovimentacaoModel> lista, Callback callback) {
        if (lista == null || lista.isEmpty()) {
            callback.onSucesso("Nenhuma movimentação para excluir.");
            return;
        }

        SmartBatch batch = new SmartBatch(db);

        for (MovimentacaoModel mov : lista) {
            if (mov.getId() == null || mov.getId().isEmpty()) continue;
            batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
            aplicarImpacto(batch, mov, -1);
        }

        batch.commit(callback, "Dia excluído com sucesso!");
    }

    public void zerarEstatisticasMensais(Callback callback) {
        FirestoreSchema.contasCategoriasCol().get()
                .addOnSuccessListener(snap -> {
                    SmartBatch batch = new SmartBatch(db);
                    DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, 0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, 0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, 0);

                    for (QueryDocumentSnapshot doc : snap) {
                        batch.update(doc.getReference(), ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, 0);
                    }

                    batch.commit(callback, "Estatísticas do mês zeradas com sucesso!");
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao buscar categorias: " + e.getMessage()));
    }

    // ── utilitário ───────────────────────────────────────────────────────────

    private boolean isMesmoMesEAno(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance(); c1.setTime(d1);
        Calendar c2 = Calendar.getInstance(); c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }
}