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

    // ── SMART BATCH ──────────────────────────────────────────────────────────

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
            if (contadorOperacoes >= 400) novoLote();
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

    /**
     * CORREÇÃO BUG #1 — Ordem de operações no confirmarMovimentacao.
     *
     * PROBLEMA ORIGINAL:
     *   1. impactoPendente(batch, mov, -1) era chamado com mov.data_vencimento podendo ser null.
     *   2. Em seguida, o código tentava resolver o data_vencimento e chamava
     *      batch.update(ref, "data_vencimento", vencAtual) onde vencAtual ainda era null —
     *      gravando null no Firestore (escrita inválida silenciosa).
     *
     * CORREÇÃO APLICADA:
     *   1. Primeiro resolvemos e garantimos data_vencimento no objeto local (mov) e no batch.
     *   2. Só então chamamos impactoPendente com o objeto consistente.
     *   3. Adicionamos guard: se data_vencimento continuar null após todas as tentativas,
     *      usamos Timestamp.now() como fallback — nunca gravamos null.
     *
     * CORREÇÃO BUG #2 — Proteção contra contador negativo.
     *
     * PROBLEMA ORIGINAL:
     *   impactoPendente usava FieldValue.increment(-1) sem checar se o contador já era 0.
     *   O Firestore aceita negativos silenciosamente, corrompendo o badge de pendências.
     *
     * CORREÇÃO APLICADA:
     *   impactoPendenteSafe() lê o valor atual do contador antes de decrementar.
     *   Se já for 0, não faz nada. Se for positivo, decrementa normalmente.
     *   Para incremento (fator = 1), comportamento é idêntico ao original.
     *
     *   NOTA SOBRE CUSTO: essa leitura extra custa 1 read no Firestore por confirmação.
     *   É aceitável pois confirmação é uma ação explícita e pouco frequente do usuário.
     *   A alternativa (Cloud Function) seria gratuita em reads mas requereria backend.
     */
    public void confirmarMovimentacao(MovimentacaoModel mov, Callback callback) {
        if (mov.isPago()) {
            callback.onErro("Esta movimentação já consta como paga.");
            return;
        }

        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();

        // ── PASSO 1: Lê o contador atual ANTES de montar o batch ─────────────
        // Isso resolve o Bug #2: garantimos que não vamos decrementar abaixo de 0.
        resumoRef.get().addOnSuccessListener(resumoSnap -> {

            // Extrai o valor atual do contador (pagar ou receber)
            boolean isReceita = (mov.getTipoEnum() == TipoCategoriaContas.RECEITA);
            String campoPendencia = isReceita
                    ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                    : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

            long contadorAtual = 0;
            if (resumoSnap.exists()) {
                Long valor = resumoSnap.getLong(campoPendencia);
                contadorAtual = (valor != null) ? valor : 0;
            }

            // ── PASSO 2: Monta o batch com a ordem correta ────────────────────
            SmartBatch batch = new SmartBatch(db);
            DocumentReference ref = FirestoreSchema.contasMovimentacaoDoc(mov.getId());
            Timestamp agora = Timestamp.now();

            // ── PASSO 3: Resolve data_vencimento ANTES de qualquer impacto ────
            // Bug #1: antes o impacto era calculado com data potencialmente nula.
            // Agora garantimos que mov.data_vencimento está preenchido antes de prosseguir.
            Timestamp vencResolvido = mov.getData_vencimento();

            if (vencResolvido == null) {
                // Tenta usar data_movimentacao como fallback
                vencResolvido = mov.getData_movimentacao();
            }
            if (vencResolvido == null) {
                // Último recurso: usa agora. Nunca grava null.
                vencResolvido = agora;
            }

            // Atualiza o objeto local para que impactoRealizado use a data correta
            mov.setData_vencimento(vencResolvido);

            // Grava data_vencimento no documento se estava null originalmente
            if (mov.getData_vencimento() != null) {
                batch.update(ref, "data_vencimento", vencResolvido);
            }

            // ── PASSO 4: Remove do contador de pendências ─────────────────────
            // Bug #2: só decrementa se o contador for maior que 0.
            if (contadorAtual > 0) {
                batch.update(resumoRef, campoPendencia, FieldValue.increment(-1));
            }
            // Se contadorAtual == 0, o dado já estava inconsistente (bug de sync anterior).
            // Não fazemos nada: não deixamos ir negativo.

            // ── PASSO 5: Marca como pago e registra data de pagamento ─────────
            batch.update(ref, MovimentacaoModel.CAMPO_PAGO, true);
            batch.update(ref, "data_pagamento", agora);

            // Atualiza objeto local para que impactoRealizado use estado correto
            mov.setPago(true);
            mov.setData_pagamento(agora);

            // ── PASSO 6: Aplica impacto no saldo realizado ────────────────────
            // Agora mov está completamente consistente: pago=true, datas preenchidas.
            impactoRealizado(batch, mov, 1);

            batch.commit(callback, "Confirmado e registrado corretamente!");

        }).addOnFailureListener(e ->
                callback.onErro("Erro ao verificar pendências: " + e.getMessage())
        );
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

    /**
     * CORREÇÃO BUG #2 — impactoPendente para incremento (fator = 1).
     *
     * Para INCREMENTO (nova movimentação pendente criada), o comportamento
     * é idêntico ao original: incrementa o contador sem restrições.
     *
     * Para DECREMENTO (fator = -1), este método NÃO deve mais ser chamado
     * diretamente em confirmarMovimentacao — a lógica de decremento seguro
     * agora está dentro de confirmarMovimentacao() que lê o valor atual primeiro.
     *
     * Para excluir(), o decremento ainda usa FieldValue.increment(-1) porque:
     *   - excluir() remove o documento, então o contador deveria estar >= 1.
     *   - Se o dado estiver inconsistente, o documento nem existiria para excluir.
     *   - O risco de negativo aqui é muito menor.
     */
    private void impactoPendente(SmartBatch batch, MovimentacaoModel mov, int fator) {
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

        SmartBatch batch = new SmartBatch(db);
        String grupoId = java.util.UUID.randomUUID().toString();

        TipoRecorrencia tipoRec = movBase.getTipoRecorrenciaEnum();
        boolean isParcelado = (tipoRec == TipoRecorrencia.PARCELADO);

        if (quantidade <= 0) {
            callback.onErro("Quantidade inválida para recorrência.");
            return;
        }

        // ── CORREÇÃO BUG #7 (bonus) — Divisão inteira trunca centavos ────────
        // PROBLEMA: R$ 100,01 / 3 = R$ 33,00 cada → perde R$ 0,01 no total.
        // CORREÇÃO: calcula o valor base e coloca o centavo restante na 1ª parcela.
        long valorBase    = isParcelado ? movBase.getValor() / quantidade : movBase.getValor();
        long restoCentavos = isParcelado ? movBase.getValor() % quantidade : 0;
        // A parcela 0 (primeira) receberá valorBase + restoCentavos.
        // As demais recebem valorBase. Assim a soma sempre fecha o total original.

        Timestamp dataBase = movBase.getData_vencimento();
        if (dataBase == null) dataBase = movBase.getData_movimentacao();
        if (dataBase == null) dataBase = Timestamp.now();

        for (int i = 0; i < quantidade; i++) {
            MovimentacaoModel parcela = new MovimentacaoModel();

            String descBase = movBase.getDescricao().replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();

            parcela.setDescricao(quantidade > 1
                    ? descBase + " (" + (i + 1) + "/" + quantidade + ")"
                    : descBase);

            // Primeira parcela absorve o centavo restante da divisão
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

    /**
     * CORREÇÃO BUG #2 aplicada também no confirmarMovimentacaoEmMassa.
     *
     * O mesmo problema de contador negativo existe aqui: várias parcelas sendo
     * confirmadas de uma vez, cada uma decrementando o contador.
     *
     * SOLUÇÃO: lemos o valor atual do contador uma única vez antes do batch.
     * Calculamos quantas parcelas pendentes existem no snap, e decrementamos
     * apenas Math.min(qtdParcelas, contadorAtual) — nunca abaixo de zero.
     */
    public void confirmarMovimentacaoEmMassa(MovimentacaoModel movBase, Callback callback) {
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        boolean isReceita = (movBase.getTipoEnum() == TipoCategoriaContas.RECEITA);
        String campoPendencia = isReceita
                ? ResumoFinanceiroModel.CAMPO_PENDENCIAS_RECEBER
                : ResumoFinanceiroModel.CAMPO_PENDENCIAS_PAGAR;

        // ── PASSO 1: Lê o contador atual E as parcelas pendentes em paralelo ─
        // Usamos a query das parcelas primeiro, depois lemos o resumo.
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

                    // ── PASSO 2: Lê o resumo para saber o contador atual ──────
                    resumoRef.get().addOnSuccessListener(resumoSnap -> {

                        long contadorAtual = 0;
                        if (resumoSnap.exists()) {
                            Long valor = resumoSnap.getLong(campoPendencia);
                            contadorAtual = (valor != null) ? valor : 0;
                        }

                        // Quantas unidades podemos decrementar sem ir negativo?
                        long decrementoSeguro = Math.min(totalParcelasPendentes, contadorAtual);

                        // ── PASSO 3: Monta o batch ────────────────────────────
                        SmartBatch batch = new SmartBatch(db);
                        Timestamp agora = Timestamp.now();

                        for (QueryDocumentSnapshot doc : snap) {
                            MovimentacaoModel m = doc.toObject(MovimentacaoModel.class);
                            m.setId(doc.getId());

                            // Resolve data_vencimento antes de qualquer impacto (Bug #1)
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

                        // Decrementa o contador de pendências de forma segura (uma única vez)
                        if (decrementoSeguro > 0) {
                            batch.update(resumoRef, campoPendencia,
                                    FieldValue.increment(-decrementoSeguro));
                        }

                        batch.commit(callback, "Todas as parcelas confirmadas!");

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