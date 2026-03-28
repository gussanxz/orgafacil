package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.ResumoParcelamentoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.ParcelamentoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.CelebracaoManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResumoParcelasActivity extends AppCompatActivity {

    private enum EmptyEstado { SEM_PARCELAS, ERRO_REDE }

    // Simplificação do Enum de Filtro (1 status por vez - BUG 1)
    private enum StatusFiltro { TODOS, PAGO, PENDENTE, ATRASADO }

    // ─── Estado ──────────────────────────────────────────────────────────────
    private ParcelamentoRepository repo;
    private MovimentacaoRepository movRepo;
    private String recorrenciaId;
    private List<MovimentacaoModel> listaBruta = new ArrayList<>();
    // Filtro inicial padrão
    private StatusFiltro filtroAtivo = StatusFiltro.TODOS;

    // === CACHE DE SESSÃO ===
    // Guarda a última série carregada para abrir a tela instantaneamente
    private static String cacheRecorrenciaId = null;
    private static List<MovimentacaoModel> cacheParcelas = null;

    // ─── Views — Novo card de resumo Donut ───────────────────────────────────
    private TextView textTitulo; // Dinâmico, ex: "Freelance: Design de Logo" (mantendo lógica aplicada)
    private ProgressBar progressParcelasCircular; // O donut
    private TextView textProgressoCentral; // Ex: "1/5"
    private TextView textProgressoDescricao; // Ex: "Recebidas: 1 de 5 parcelas"
    private TextView textValorPago;
    private TextView textValorRestante;
    private TextView textProximaData;
    private ProgressBar progressBarLoading;

    // ─── Views — lista e container ───────────────────────────────────────────
    private RecyclerView recyclerParcelas;
    private View cardResumo;

    // ─── Views — filtro (Mantidos os IDs para compatibilidade com include) ────
    private ChipGroup chipGroupFiltroStatus;
    private Chip chipFiltroTodos;
    private Chip chipFiltroPago;
    private Chip chipFiltroPendente;
    private Chip chipFiltroAtrasado;

    // ─── Views — empty state ─────────────────────────────────────────────────
    private View layoutEmptyState;
    private android.widget.ImageView emptyStateIcon;
    private TextView emptyStateTitulo;
    private TextView emptyStateDescricao;
    private MaterialButton emptyStateBotaoPrimario;
    private MaterialButton emptyStateBotaoSecundario;
    private boolean jaCelebrouQuitacao = false;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_parcelas);

        recorrenciaId = getIntent().getStringExtra("recorrencia_id");

        if (recorrenciaId == null || recorrenciaId.isEmpty()) {
            vincularViews();
            exibirEmptyState(EmptyEstado.SEM_PARCELAS);
            return;
        }

        repo    = new ParcelamentoRepository();
        movRepo = new MovimentacaoRepository();

        vincularViews();
        configurarChipsFiltro();

        // NOVA LÓGICA DE CACHE:
        if (recorrenciaId.equals(cacheRecorrenciaId) && cacheParcelas != null) {
            // Se já temos no cache, monta a tela instantaneamente!
            processarDados(cacheParcelas);
            // E busca novidades no fundo sem mostrar o loading
            carregarSerie(false);
        } else {
            // Primeira vez abrindo essa série, mostra o loading normal
            carregarSerie(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevenção de Bug: Limpa o cache estático ao fechar a tela para não exibir dados velhos
        cacheRecorrenciaId = null;
        cacheParcelas = null;
    }

    private void vincularViews() {
        // Atualizadas para as novas views do resumo Donut
        textTitulo              = findViewById(R.id.textTitulo);
        progressParcelasCircular = findViewById(R.id.progressParcelasCircular);
        textProgressoCentral    = findViewById(R.id.textProgressoCentral);
        textProgressoDescricao   = findViewById(R.id.textProgressoDescricao);
        textValorPago           = findViewById(R.id.textValorPago);
        textValorRestante       = findViewById(R.id.textValorRestante);
        textProximaData         = findViewById(R.id.textProximaData);

        progressBarLoading  = findViewById(R.id.progressBarLoading);
        recyclerParcelas    = findViewById(R.id.recyclerParcelas);
        cardResumo          = findViewById(R.id.cardResumo);

        chipGroupFiltroStatus = findViewById(R.id.chipGroupFiltroStatus);
        chipFiltroTodos       = findViewById(R.id.chipFiltroTodos);
        chipFiltroPago        = findViewById(R.id.chipFiltroPago);
        chipFiltroPendente    = findViewById(R.id.chipFiltroPendente);
        chipFiltroAtrasado    = findViewById(R.id.chipFiltroAtrasado);

        layoutEmptyState          = findViewById(R.id.layoutEmptyState);
        emptyStateIcon            = findViewById(R.id.emptyStateIcon);
        emptyStateTitulo          = findViewById(R.id.emptyStateTitulo);
        emptyStateDescricao       = findViewById(R.id.emptyStateDescricao);
        emptyStateBotaoPrimario   = findViewById(R.id.emptyStateBotaoPrimario);
        emptyStateBotaoSecundario = findViewById(R.id.emptyStateBotaoSecundario);

        recyclerParcelas.setLayoutManager(new LinearLayoutManager(this));
    }

    // ─── Configuração dos chips CORRIGIDA (Seleção Única - BUG 1) ─────────────

    private void configurarChipsFiltro() {
        if (chipGroupFiltroStatus == null) return;

        // Com singleSelection=true e selectionRequired=true no XML,
        // garantimos que exatamente 1 chip esteja sempre selecionado.
        // O listener simplifica drasticamente.
        chipGroupFiltroStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Devido à seleção obrigatória, checkedIds nunca será vazia aqui.
            int checkedId = checkedIds.get(0); // Pega o único ID selecionado

            if (checkedId == R.id.chipFiltroTodos) {
                filtroAtivo = StatusFiltro.TODOS;
            } else if (checkedId == R.id.chipFiltroPago) {
                filtroAtivo = StatusFiltro.PAGO;
            } else if (checkedId == R.id.chipFiltroPendente) {
                filtroAtivo = StatusFiltro.PENDENTE;
            } else if (checkedId == R.id.chipFiltroAtrasado) {
                filtroAtivo = StatusFiltro.ATRASADO;
            } else {
                filtroAtivo = StatusFiltro.TODOS; // Fallback de segurança
            }

            aplicarFiltro();
        });
    }

    // ─── Lógica de Carga e Cache ─────────────────────────────────────────────

    private void carregarSerie(boolean mostrarLoading) {
        if (mostrarLoading && listaBruta.isEmpty()) {
            cardResumo.setVisibility(View.GONE);
            if (chipGroupFiltroStatus != null) chipGroupFiltroStatus.setVisibility(View.GONE);
            recyclerParcelas.setVisibility(View.GONE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);

            progressBarLoading.setVisibility(View.VISIBLE);
        }

        repo.buscarParcelas(recorrenciaId, new ParcelamentoRepository.ParcelasCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> parcelas) {
                if (!estaAtiva()) return;
                progressBarLoading.setVisibility(View.GONE);

                // Salva os dados fresquinhos no Cache para a próxima vez
                cacheRecorrenciaId = recorrenciaId;
                cacheParcelas = parcelas;

                processarDados(parcelas);
            }

            @Override
            public void onErro(String erro) {
                if (!estaAtiva()) return;
                progressBarLoading.setVisibility(View.GONE);

                // Só mostra tela de erro se a tela estiver totalmente vazia (sem cache)
                if (listaBruta.isEmpty()) {
                    exibirEmptyState(EmptyEstado.ERRO_REDE);
                }
            }
        });
    }

    private void processarDados(List<MovimentacaoModel> parcelas) {
        if (parcelas.isEmpty()) {
            exibirEmptyState(EmptyEstado.SEM_PARCELAS);
            return;
        }

        listaBruta = parcelas;
        ResumoParcelamentoModel resumo = ResumoParcelamentoModel.calcular(parcelas);

        cardResumo.setVisibility(View.VISIBLE);
        if (chipGroupFiltroStatus != null) chipGroupFiltroStatus.setVisibility(View.VISIBLE);

        // CORREÇÃO DO FLICKERING:
        if (resumo.parcelasPendentes == 0) {
            if (!jaCelebrouQuitacao) {
                // Primeira vez que percebe que quitou: Dispara a animação!
                jaCelebrouQuitacao = true;
                exibirEmptyStateTodasPagas(resumo);
            } else if (layoutEmptyState.getVisibility() != View.VISIBLE) {
                // Se já celebrou antes (ou já entrou na tela com tudo pago no passado)
                // e a tela estática ainda não está visível, exibe o banner estático direto sem animar.
                exibirBannerTodasPagas(resumo);
            }
        }

        exibirResumo(resumo, parcelas);
        aplicarFiltro();
    }

    // ─── Filtro Lógica CORRIGIDA (Exato - BUG 2) ─────────────────────────────

    private void aplicarFiltro() {
        // Se a lista está vazia, não há o que filtrar (o empty state de carga já lidou com isso)
        if (listaBruta.isEmpty()) return;

        List<MovimentacaoModel> filtradas = new ArrayList<>();

        // Lógica de filtro EXATA baseada no único status ativo (BUG 2 CORRIGIDO)
        for (MovimentacaoModel p : listaBruta) {
            switch (filtroAtivo) {
                case TODOS:
                    filtradas.add(p);
                    break;
                case PAGO:
                    if (p.isPago()) filtradas.add(p);
                    break;
                case PENDENTE:
                    // Não paga E dentro do prazo
                    if (!p.isPago() && !p.estaVencida()) filtradas.add(p);
                    break;
                case ATRASADO:
                    // Não paga E vencida
                    if (!p.isPago() && p.estaVencida()) filtradas.add(p);
                    break;
            }
        }

        exibirListaFiltrada(filtradas);
    }

    private void exibirListaFiltrada(List<MovimentacaoModel> filtradas) {
        if (filtradas.isEmpty()) {
            recyclerParcelas.setVisibility(View.GONE);
            exibirEmptyStateFiltro();
            return;
        }

        // Se há resultados, garante que o empty state de filtro esteja oculto
        // O empty state de filtro usa o layoutEmptyState padrão.
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        recyclerParcelas.setVisibility(View.VISIBLE);
        exibirListaParcelas(filtradas);
    }

    private void exibirEmptyStateFiltro() {
        if (layoutEmptyState == null) return;

        String mensagem;
        // Mensagem contextual exata baseada no status CORRIGIDO
        switch (filtroAtivo) {
            case PAGO: mensagem = "Nenhuma parcela paga encontrada."; break;
            case PENDENTE: mensagem = "Nenhuma parcela pendente. Tudo em dia! ✓"; break;
            case ATRASADO: mensagem = "Nenhuma parcela atrasada. Que ótimo!"; break;
            case TODOS:
            default: mensagem = "Nenhuma parcela encontrada para esta série."; break;
        }

        emptyStateIcon.setImageResource(R.drawable.ic_info_24);
        emptyStateTitulo.setText("Sem resultados");
        emptyStateDescricao.setText(mensagem);
        emptyStateBotaoPrimario.setVisibility(View.GONE);
        emptyStateBotaoSecundario.setVisibility(View.GONE);

        layoutEmptyState.setAlpha(0f);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutEmptyState.animate().alpha(1f).setDuration(220).start();
    }

    // ─── Empty States (SEM_PARCELAS e ERRO_REDE) (Mantidos) ────────────────────

    private void exibirEmptyState(EmptyEstado estado) {
        cardResumo.setVisibility(View.GONE);
        if (chipGroupFiltroStatus != null) chipGroupFiltroStatus.setVisibility(View.GONE);
        recyclerParcelas.setVisibility(View.GONE);

        switch (estado) {
            case SEM_PARCELAS:
                emptyStateIcon.setImageResource(R.drawable.ic_info_24);
                emptyStateTitulo.setText("Nenhuma parcela encontrada");
                emptyStateDescricao.setText("Esta série não possui parcelas registradas.");
                emptyStateBotaoPrimario.setText("Voltar");
                emptyStateBotaoPrimario.setOnClickListener(v -> finish());
                emptyStateBotaoPrimario.setVisibility(View.VISIBLE);
                emptyStateBotaoSecundario.setVisibility(View.GONE);
                break;
            case ERRO_REDE:
                emptyStateIcon.setImageResource(R.drawable.ic_wifi_off_24);
                emptyStateTitulo.setText("Não foi possível carregar");
                emptyStateDescricao.setText("Verifique sua conexão e tente novamente.");
                emptyStateBotaoPrimario.setText("Tentar novamente");
                emptyStateBotaoPrimario.setOnClickListener(v -> carregarSerie(true));
                emptyStateBotaoPrimario.setVisibility(View.VISIBLE);
                emptyStateBotaoSecundario.setVisibility(View.VISIBLE);
                emptyStateBotaoSecundario.setText("Voltar");
                emptyStateBotaoSecundario.setOnClickListener(v -> finish());
                break;
        }

        layoutEmptyState.setAlpha(0f);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutEmptyState.animate().alpha(1f).setDuration(280).start();
    }

    // ─── Empty State QUQUITADA (Mantido) ───────────────────────────────────────

    private void exibirEmptyStateTodasPagas(ResumoParcelamentoModel resumo) {
        ViewGroup root = findViewById(R.id.main);
        // Precisão de Inteiros: Utilizando o MoedaHelper em vez de dividir por 100.0
        String totalPagoStr = com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarCentavosParaBRL(resumo.valorTotalCentavos);
        String subtitulo = "Parabéns! Série quitada em " + resumo.totalParcelas + " parcelas. Total: " + totalPagoStr;

        CelebracaoManager.celebrar(this, root, "Série quitada! 🎉", subtitulo, () -> {
            if (!estaAtiva()) return;
            exibirBannerTodasPagas(resumo);
        });
    }

    private void exibirBannerTodasPagas(ResumoParcelamentoModel resumo) {
        // Precisão de Inteiros: Utilizando o MoedaHelper em vez de dividir por 100.0
        String totalPagoStr = com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarCentavosParaBRL(resumo.valorTotalCentavos);

        emptyStateIcon.setImageResource(R.drawable.ic_check_circle_24);
        emptyStateIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        emptyStateIcon.setAlpha(1f);

        emptyStateTitulo.setText("Série quitada! ✓");
        emptyStateTitulo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

        emptyStateDescricao.setText(resumo.totalParcelas + " parcelas pagas. Total: " + totalPagoStr);

        emptyStateBotaoPrimario.setText("Fechar");
        emptyStateBotaoPrimario.setOnClickListener(v -> finish());
        emptyStateBotaoPrimario.setVisibility(View.VISIBLE);
        emptyStateBotaoSecundario.setVisibility(View.GONE);

        layoutEmptyState.setAlpha(0f);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutEmptyState.animate().alpha(1f).setDuration(400).start();
    }

    // ─── Exibição normal (Atualizada para views donut) ────────────────────────

    private void exibirResumo(ResumoParcelamentoModel r, List<MovimentacaoModel> parcelas) {
        // Lógica Dinâmica Mantida (Sla -> Descrição real)
        if (!parcelas.isEmpty() && parcelas.get(0).getDescricao() != null) {
            String descBase = parcelas.get(0).getDescricao()
                    .replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();
            textTitulo.setText(descBase); // Define ex: "Freelance: Design de Logo"
        }

        // Atualização das novas views Donut
        textProgressoCentral.setText(r.parcelasPagas + "/" + r.totalParcelas);

        textProgressoDescricao.setText("Pagas: " + r.parcelasPagas + " de " + r.totalParcelas + " parcelas");

        int percentual = r.totalParcelas > 0
                ? (int) ((double) r.parcelasPagas / r.totalParcelas * 100)
                : 0;
        progressParcelasCircular.setMax(100);

        android.animation.ObjectAnimator.ofInt(progressParcelasCircular, "progress", percentual)
                .setDuration(400)
                .start();

        // Precisão de Inteiros: Utilizando o MoedaHelper para exatidão e padronização
        textValorPago.setText(com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarCentavosParaBRL(r.valorPagoCentavos));
        textValorRestante.setText(com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarCentavosParaBRL(r.valorRestanteCentavos));

        if (r.proximaPendente != null && r.proximaPendente.getData_movimentacao() != null) {
            String dataStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(r.proximaPendente.getData_movimentacao().toDate());
            textProximaData.setText("Próximo vencimento: " + dataStr);
        } else {
            textProximaData.setText("Todas as parcelas pagas ✓");
            textProximaData.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    // Lógica de Lista (Sem alterações, depende do Adapter) ──────────────────────

    private void exibirListaParcelas(List<MovimentacaoModel> parcelas) {
        List<AdapterItemListaMovimentacao> itens = new ArrayList<>();

        for (MovimentacaoModel p : parcelas) {
            itens.add(AdapterItemListaMovimentacao.linha(p));
        }

        AdapterMovimentacaoLista adapter = new AdapterMovimentacaoLista(this, new AdapterMovimentacaoLista.OnItemActionListener() {
            @Override public void onDeleteClick(MovimentacaoModel mov) { }
            @Override public void onLongClick(MovimentacaoModel mov) { }
            @Override public void onCheckClick(MovimentacaoModel mov) { confirmarParcela(mov); }
            @Override public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) { }
            @Override public void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia) { }
        }, true /* modoParcelasResumidas: pagos exibidos em cinza somente nesta tela */);

        adapter.submitList(itens);
        recyclerParcelas.setAdapter(adapter);
    }

    private void confirmarParcela(MovimentacaoModel mov) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar parcela")
                .setMessage("Deseja marcar a parcela " + mov.getParcela_atual() + "/" + mov.getTotal_parcelas() + " como paga?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    movRepo.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            if (!estaAtiva()) return;
                            Toast.makeText(ResumoParcelasActivity.this, "Parcela confirmada!", Toast.LENGTH_SHORT).show();
                            carregarSerie(false); // Recarrega silenciosamente
                        }
                        @Override
                        public void onErro(String erro) {
                            if (!estaAtiva()) return;
                            Toast.makeText(ResumoParcelasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Helpers ──────────────────────────────────────────────────────────────────
    public void retornarPrincipal(View view) { finish(); }
    private boolean estaAtiva() { return !isFinishing() && !isDestroyed(); }
}