package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoRecorrencia;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.dados.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSmartBatch;

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
        com.google.firebase.Timestamp limite = dataReferencia != null
                ? new com.google.firebase.Timestamp(dataReferencia)
                : com.google.firebase.Timestamp.now();

        Query q = FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, true)
                .whereLessThanOrEqualTo(MovimentacaoModel.CAMPO_DATA_MOVIMENTACAO, limite)
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
        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
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
        batch.commit(criarBatchCallback(callback, "Lançado com sucesso!"));
    }

    public void excluir(MovimentacaoModel mov, Callback callback) {
        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
        batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
        aplicarImpacto(batch, mov, -1);
        batch.commit(criarBatchCallback(callback, "Excluído com sucesso!"));
    }

    public void editar(MovimentacaoModel movAntigo, MovimentacaoModel movNovo, Callback callback) {
        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
        batch.set(FirestoreSchema.contasMovimentacaoDoc(movNovo.getId()), movNovo);
        aplicarImpacto(batch, movAntigo, -1);
        aplicarImpacto(batch, movNovo, 1);
        batch.commit(criarBatchCallback(callback, "Editado com sucesso!"));
    }

    // ── confirmação ──────────────────────────────────────────────────────────

    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        if (mov.isPago()) {
            callback.onErro("Esta movimentação já consta como paga.");
            return;
        }

        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        resumoRef.get().addOnSuccessListener(resumoSnap -> {

            boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);
            String campoPendencia = isReceita
                    ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                    : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

            long contadorAtual = 0;
            if (resumoSnap.exists()) {
                Long valor = resumoSnap.getLong(campoPendencia);
                contadorAtual = (valor != null) ? valor : 0;
            }

            FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
            DocumentReference ref = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
            Timestamp agora = Timestamp.now();

            Timestamp vencResolvido = mov.getData_vencimento();
            if (vencResolvido == null) vencResolvido = mov.getData_movimentacao();
            if (vencResolvido == null) vencResolvido = agora;

            mov.setData_vencimento(vencResolvido);
            batch.update(ref, "data_vencimento", vencResolvido);

            if (contadorAtual > 0) {
                batch.update(resumoRef, campoPendencia, FieldValue.increment(-1));
            }

            batch.update(ref, MovimentacaoModel.CAMPO_PAGO, true);
            batch.update(ref, "data_pagamento", agora);

            mov.setPago(true);
            mov.setData_pagamento(agora);

            impactoRealizado(batch, mov, 1);

            batch.commit(criarBatchCallback(callback, "Confirmado e registrado corretamente!"));

        }).addOnFailureListener(e ->
                callback.onErro("Erro ao verificar pendências: " + e.getMessage())
        );
    }

    // ── impactos no resumo ───────────────────────────────────────────────────

    private void aplicarImpacto(FirestoreSmartBatch batch, MovimentacaoModel mov, int fator) {
        if (mov.isPago()) impactoRealizado(batch, mov, fator);
        else impactoPendente(batch, mov, fator);
    }

    private void impactoRealizado(FirestoreSmartBatch batch, MovimentacaoModel mov, int fator) {
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

    private void impactoPendente(FirestoreSmartBatch batch, MovimentacaoModel mov, int fator) {
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);
        String campo = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(FirestoreSchema.contasResumoDoc(),
                campo,
                FieldValue.increment(fator));
    }

    // ── salvar recorrente ────────────────────────────────────────────────────

    public void salvarRecorrente(MovimentacaoModel movBase, int quantidade, Callback callback) {

        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
        String grupoId = java.util.UUID.randomUUID().toString();

        TipoRecorrencia tipoRec = movBase.getTipoRecorrenciaEnum();
        boolean isParcelado = (tipoRec == TipoRecorrencia.PARCELADO);

        if (quantidade <= 0) {
            callback.onErro("Quantidade inválida para recorrência.");
            return;
        }

        long valorBase     = isParcelado ? movBase.getValor() / quantidade : movBase.getValor();
        long restoCentavos = isParcelado ? movBase.getValor() % quantidade : 0;

        Timestamp dataBase = movBase.getData_vencimento();
        if (dataBase == null) dataBase = movBase.getData_movimentacao();
        if (dataBase == null) dataBase = Timestamp.now();

        for (int i = 0; i < quantidade; i++) {
            MovimentacaoModel parcela = new MovimentacaoModel();

            String descBase = movBase.getDescricao().replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();

            parcela.setDescricao(quantidade > 1
                    ? descBase + " (" + (i + 1) + "/" + quantidade + ")"
                    : descBase);

            long valorParcela = (i == 0) ? valorBase + restoCentavos : valorBase;
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

        batch.commit(criarBatchCallback(callback, msgFinal));
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
                    FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
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

                    batch.commit(criarBatchCallback(callback, "Série atualizada com sucesso!"));
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
                    FirestoreSmartBatch batch = new FirestoreSmartBatch(db);

                    for (QueryDocumentSnapshot doc : snap) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        if (m.isPago() && m.getData_pagamento() == null) {
                            m.setData_pagamento(m.getData_vencimento());
                        }

                        aplicarImpacto(batch, m, -1);
                        batch.delete(doc.getReference());
                    }

                    batch.commit(criarBatchCallback(callback, "Série excluída com sucesso!"));
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void confirmarMovimentacaoEmMassa(MovimentacaoModel movBase, Callback callback) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        boolean isReceita = (movBase.getTipoEnum() == TipoCategoriaContas.RECEITA);
        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", movBase.getRecorrencia_id())
                .whereGreaterThanOrEqualTo("parcela_atual", movBase.getParcela_atual())
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        callback.onSucesso("Nenhuma parcela pendente encontrada.");
                        return;
                    }

                    int totalParcelasPendentes = snap.size();

                    resumoRef.get().addOnSuccessListener(resumoSnap -> {

                        long contadorAtual = 0;
                        if (resumoSnap.exists()) {
                            Long valor = resumoSnap.getLong(campoPendencia);
                            contadorAtual = (valor != null) ? valor : 0;
                        }

                        long decrementoSeguro = Math.min(totalParcelasPendentes, contadorAtual);

                        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
                        Timestamp agora = Timestamp.now();

                        for (QueryDocumentSnapshot doc : snap) {
                            MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                            m.setId(doc.getId());

                            Timestamp vencResolvido = m.getData_vencimento();
                            if (vencResolvido == null) vencResolvido = m.getData_movimentacao();
                            if (vencResolvido == null) vencResolvido = agora;

                            m.setData_vencimento(vencResolvido);
                            batch.update(doc.getReference(), "data_vencimento", vencResolvido);

                            batch.update(doc.getReference(), MovimentacaoModel.CAMPO_PAGO, true);
                            batch.update(doc.getReference(), "data_pagamento", agora);

                            m.setPago(true);
                            m.setData_pagamento(agora);

                            impactoRealizado(batch, m, 1);
                        }

                        if (decrementoSeguro > 0) {
                            batch.update(resumoRef, campoPendencia,
                                    FieldValue.increment(-decrementoSeguro));
                        }

                        batch.commit(criarBatchCallback(callback, "Todas as parcelas confirmadas!"));

                    }).addOnFailureListener(e ->
                            callback.onErro("Erro ao verificar pendências: " + e.getMessage())
                    );

                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── lote ─────────────────────────────────────────────────────────────────

    public void excluirEmLote(List<MovimentacaoModel> lista, Callback callback) {
        if (lista == null || lista.isEmpty()) {
            callback.onSucesso("Nenhuma movimentação para excluir.");
            return;
        }

        long pendentesPagar = 0, pendentesReceber = 0;
        for (MovimentacaoModel mov : lista) {
            if (!mov.isPago()) {
                if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) pendentesReceber++;
                else pendentesPagar++;
            }
        }

        if (pendentesPagar == 0 && pendentesReceber == 0) {
            commitExcluirEmLote(lista, 0, 0, callback);
            return;
        }

        final long finalPendentesPagar   = pendentesPagar;
        final long finalPendentesReceber = pendentesReceber;

        FirestoreSchema.contasResumoDoc().get()
                .addOnSuccessListener(snap -> {
                    long contPagar   = 0, contReceber = 0;
                    if (snap.exists()) {
                        Long vp = snap.getLong(ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR);
                        Long vr = snap.getLong(ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER);
                        contPagar   = (vp != null) ? vp : 0;
                        contReceber = (vr != null) ? vr : 0;
                    }
                    long decPagar   = Math.min(finalPendentesPagar,   contPagar);
                    long decReceber = Math.min(finalPendentesReceber, contReceber);
                    commitExcluirEmLote(lista, decPagar, decReceber, callback);
                })
                .addOnFailureListener(e -> callback.onErro("Erro ao verificar pendências: " + e.getMessage()));
    }

    private void commitExcluirEmLote(List<MovimentacaoModel> lista,
                                     long decPagar, long decReceber,
                                     Callback callback) {
        FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        for (MovimentacaoModel mov : lista) {
            if (mov.getId() == null || mov.getId().isEmpty()) continue;
            batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
            if (mov.isPago()) impactoRealizado(batch, mov, -1);
        }

        if (decPagar > 0)
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR,
                    FieldValue.increment(-decPagar));
        if (decReceber > 0)
            batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER,
                    FieldValue.increment(-decReceber));

        batch.commit(criarBatchCallback(callback, "Dia excluído com sucesso!"));
    }

    public void zerarEstatisticasMensais(Callback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .get()
                .addOnSuccessListener(snapPendentes -> {

                    long totalPagar = 0, totalReceber = 0;
                    for (QueryDocumentSnapshot doc : snapPendentes) {
                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) totalReceber++;
                        else totalPagar++;
                    }

                    final long finalPagar   = totalPagar;
                    final long finalReceber = totalReceber;

                    FirestoreSchema.contasCategoriasCol().get()
                            .addOnSuccessListener(snapCategorias -> {
                                FirestoreSmartBatch batch = new FirestoreSmartBatch(db);
                                DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

                                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES, 0);
                                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES, 0);
                                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES, 0);

                                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR,   finalPagar);
                                batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER, finalReceber);

                                for (QueryDocumentSnapshot doc : snapCategorias) {
                                    batch.update(doc.getReference(),
                                            ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, 0);
                                }

                                batch.commit(criarBatchCallback(callback, "Estatísticas do mês zeradas com sucesso!"));
                            })
                            .addOnFailureListener(e -> callback.onErro("Erro ao buscar categorias: " + e.getMessage()));

                })
                .addOnFailureListener(e -> callback.onErro("Erro ao recontar pendências: " + e.getMessage()));
    }

    // ── utilitário ───────────────────────────────────────────────────────────

    private boolean isMesmoMesEAno(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance(); c1.setTime(d1);
        Calendar c2 = Calendar.getInstance(); c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    /**
     * Utilitário para converter o Callback interno do repositório no Callback do FirestoreSmartBatch
     */
    private FirestoreSmartBatch.Callback criarBatchCallback(Callback callbackOriginal, String mensagemSucesso) {
        return new FirestoreSmartBatch.Callback() {
            @Override
            public void onSucesso() {
                callbackOriginal.onSucesso(mensagemSucesso);
            }

            @Override
            public void onErro(String erro) {
                callbackOriginal.onErro(erro);
            }
        };
    }
}