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
        WriteBatch batch = db.batch();
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
        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso("Lançado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void excluir(MovimentacaoModel mov, Callback callback) {
        WriteBatch batch = db.batch();
        batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
        aplicarImpacto(batch, mov, -1);
        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso("Excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void editar(MovimentacaoModel movAntigo, MovimentacaoModel movNovo, Callback callback) {
        WriteBatch batch = db.batch();
        batch.set(FirestoreSchema.contasMovimentacaoDoc(movNovo.getId()), movNovo);
        aplicarImpacto(batch, movAntigo, -1);
        aplicarImpacto(batch, movNovo, 1);
        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso("Editado com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── confirmação ──────────────────────────────────────────────────────────

    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        if (mov.isPago()) {
            callback.onErro("Esta movimentação já consta como paga.");
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference ref = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
        Timestamp agora = Timestamp.now();

        impactoPendente(batch, mov, -1);

        Timestamp vencAtual = mov.getData_vencimento();

        // Compatibilidade com registros antigos que só possuem data_movimentacao
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

        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso("Confirmado e registrado corretamente!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── impactos no resumo ───────────────────────────────────────────────────

    private void aplicarImpacto(WriteBatch batch, MovimentacaoModel mov, int fator) {
        if (mov.isPago()) impactoRealizado(batch, mov, fator);
        else impactoPendente(batch, mov, fator);
    }

    private void impactoRealizado(WriteBatch batch, MovimentacaoModel mov, int fator) {
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

    private void impactoPendente(WriteBatch batch, MovimentacaoModel mov, int fator) {
        boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);
        String campo = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        batch.update(FirestoreSchema.contasResumoDoc(),
                campo,
                FieldValue.increment(fator));
    }

    // ── salvar recorrente (REFATORADO) ───────────────────────────────────────

    /**
     * Gera e salva todas as parcelas de uma série recorrente.
     *
     * @param movBase     movimentação-modelo com data, valor, categoria, tipo e
     *                    recorrencia_tipo / recorrencia_intervalo já preenchidos.
     * @param quantidade  número de repetições (parcelas ou ocorrências).
     * @param callback    resultado da operação em lote.
     */
    public void salvarRecorrente(MovimentacaoModel movBase, int quantidade, Callback callback) {

        WriteBatch batch = db.batch();
        String grupoId = java.util.UUID.randomUUID().toString();

        TipoRecorrencia tipoRec = movBase.getTipoRecorrenciaEnum();
        boolean isParcelado = (tipoRec == TipoRecorrencia.PARCELADO);

        // Segurança básica
        if (quantidade <= 0) {
            callback.onErro("Quantidade inválida para recorrência.");
            return;
        }

        // Valor por parcela
        long valorParcela = isParcelado
                ? movBase.getValor() / quantidade
                : movBase.getValor();

        // ── DATA BASE SEGURA ─────────────────────────────────────────────

        Timestamp dataBase = movBase.getData_vencimento();

        if (dataBase == null) {
            dataBase = movBase.getData_movimentacao();
        }

        if (dataBase == null) {
            dataBase = Timestamp.now();
        }

        // ── LOOP DE CRIAÇÃO ──────────────────────────────────────────────

        for (int i = 0; i < quantidade; i++) {

            MovimentacaoModel parcela = new MovimentacaoModel();

            // ── descrição com tag (x/y)
            String descBase = movBase.getDescricao()
                    .replaceAll("\\s*\\(\\d+/\\d+\\)$", "")
                    .trim();

            parcela.setDescricao(quantidade > 1
                    ? descBase + " (" + (i + 1) + "/" + quantidade + ")"
                    : descBase);

            parcela.setValor(valorParcela);
            parcela.setCategoria_id(movBase.getCategoria_id());
            parcela.setCategoria_nome(movBase.getCategoria_nome());
            parcela.setTipoEnum(movBase.getTipoEnum());

            // ── metadados da série
            parcela.setRecorrencia_id(grupoId);
            parcela.setParcela_atual(i + 1);
            parcela.setTotal_parcelas(quantidade);
            parcela.setRecorrencia_tipo(tipoRec.name());
            parcela.setRecorrencia_intervalo(movBase.getRecorrencia_intervalo());

            // ── status
            parcela.setPago(i == 0 && movBase.isPago());

            // ── cálculo do vencimento
            Timestamp vencimento;

            if (i > 0) {
                Calendar base = Calendar.getInstance();
                base.setTime(dataBase.toDate());
                calcularProximaData(base, tipoRec,
                        movBase.getRecorrencia_intervalo(), i);
                vencimento = new Timestamp(base.getTime());
            } else {
                vencimento = dataBase;
            }

            // ── aplicar datas corretamente
            parcela.setData_vencimento(vencimento);
            parcela.setData_movimentacao(vencimento); // compatibilidade legado

            if (parcela.isPago()) {
                parcela.setData_pagamento(Timestamp.now());
            } else {
                parcela.setData_pagamento(null);
            }

            // ── persistência
            DocumentReference ref =
                    FirestoreSchema.contasMovimentacoesCol().document();

            parcela.setId(ref.getId());
            batch.set(ref, parcela);

            aplicarImpacto(batch, parcela, 1);
        }

        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso(
                        tipoRec == TipoRecorrencia.PARCELADO
                                ? quantidade + " parcelas criadas!"
                                : "Série recorrente agendada!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    /**
     * Avança o Calendar em `passos` unidades conforme o TipoRecorrencia.
     *
     * @param cal       instância a ser modificada in-place
     * @param tipo      tipo de recorrência
     * @param intervalo valor de N para CADA_X_DIAS / CADA_X_MESES (ignorado nos demais)
     * @param passos    quantas vezes avançar (normalmente i, o índice da parcela)
     */
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

                    WriteBatch batch = db.batch();

                    String novaDescBase = movNova.getDescricao()
                            .replaceAll("\\s*\\(\\d+/\\d+\\)$", "")
                            .trim();

                    for (QueryDocumentSnapshot doc : snap) {

                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        // ── Remove impacto antigo ──────────────────────────────
                        aplicarImpacto(batch, m, -1);

                        // ── Atualiza dados básicos ─────────────────────────────
                        m.setValor(movNova.getValor());
                        m.setCategoria_id(movNova.getCategoria_id());
                        m.setCategoria_nome(movNova.getCategoria_nome());

                        m.setDescricao(novaDescBase + " ("
                                + m.getParcela_atual()
                                + "/" + m.getTotal_parcelas() + ")");

                        // ── Se for a parcela originalmente editada ────────────
                        if (m.getId().equals(movOriginal.getId())) {

                            // Atualiza vencimento corretamente
                            Timestamp novoVenc = movNova.getData_vencimento();

                            if (novoVenc == null) {
                                novoVenc = movNova.getData_movimentacao();
                            }

                            if (novoVenc == null) {
                                novoVenc = Timestamp.now();
                            }

                            m.setData_vencimento(novoVenc);
                            m.setData_movimentacao(novoVenc); // compatibilidade

                            // Controle de pagamento
                            boolean novoPago = movNova.isPago();
                            m.setPago(novoPago);

                            if (novoPago) {
                                // Se virou pago agora e não tinha data_pagamento
                                if (m.getData_pagamento() == null) {
                                    m.setData_pagamento(Timestamp.now());
                                }
                            } else {
                                // Se voltou a ser pendente
                                m.setData_pagamento(null);
                            }
                        }

                        // ── Persistência ───────────────────────────────────────
                        batch.set(doc.getReference(), m);

                        // ── Aplica novo impacto ────────────────────────────────
                        aplicarImpacto(batch, m, 1);
                    }

                    batch.commit()
                            .addOnSuccessListener(v ->
                                    callback.onSucesso("Série atualizada com sucesso!"))
                            .addOnFailureListener(e ->
                                    callback.onErro(e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onErro(e.getMessage()));
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

                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot doc : snap) {

                        MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                        m.setId(doc.getId());

                        // ── Blindagem contra inconsistência legado ───────────
                        if (m.isPago() && m.getData_pagamento() == null) {
                            // se por algum motivo estiver pago sem data_pagamento
                            m.setData_pagamento(m.getData_vencimento());
                        }

                        // ── Remove impacto financeiro primeiro ──────────────
                        aplicarImpacto(batch, m, -1);

                        // ── Depois remove documento ─────────────────────────
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(v ->
                                    callback.onSucesso("Série excluída com sucesso!"))
                            .addOnFailureListener(e ->
                                    callback.onErro(e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onErro(e.getMessage()));
    }

    public void confirmarMovimentacaoEmMassa(MovimentacaoModel movBase, Callback callback) {
        FirestoreSchema.contasMovimentacoesCol()
                .whereEqualTo("recorrencia_id", movBase.getRecorrencia_id())
                .whereGreaterThanOrEqualTo("parcela_atual", movBase.getParcela_atual())
                .whereEqualTo(MovimentacaoModel.CAMPO_PAGO, false)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
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

                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSucesso("Todas as parcelas confirmadas!"))
                            .addOnFailureListener(e -> callback.onErro(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // ── lote ─────────────────────────────────────────────────────────────────

    public void excluirEmLote(List<MovimentacaoModel> lista, Callback callback) {
        if (lista == null || lista.isEmpty()) {
            callback.onSucesso("Nenhuma movimentação para excluir.");
            return;
        }
        WriteBatch batch = db.batch();
        for (MovimentacaoModel mov : lista) {
            if (mov.getId() == null || mov.getId().isEmpty()) continue;
            batch.delete(FirestoreSchema.contasMovimentacaoDoc(mov.getId()));
            aplicarImpacto(batch, mov, -1);
        }
        batch.commit()
                .addOnSuccessListener(v -> callback.onSucesso("Dia excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    public void zerarEstatisticasMensais(Callback callback) {
        FirestoreSchema.contasCategoriasCol().get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_BALANCO_MES,   0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_RECEITAS_MES,  0);
                    batch.update(resumoRef, ResumoFinanceiroModel.CAMPO_DESPESAS_MES,  0);
                    for (QueryDocumentSnapshot doc : snap) {
                        batch.update(doc.getReference(), ContasCategoriaModel.CAMPO_TOTAL_GASTO_MES, 0);
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSucesso("Estatísticas do mês zeradas com sucesso!"))
                            .addOnFailureListener(e -> callback.onErro(e.getMessage()));
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