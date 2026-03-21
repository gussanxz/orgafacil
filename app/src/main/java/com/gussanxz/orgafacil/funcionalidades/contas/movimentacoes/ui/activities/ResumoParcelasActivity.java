package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.ResumoParcelamentoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.ParcelamentoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.CelebracaoManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResumoParcelasActivity extends AppCompatActivity {

    // ─── Enum: define os 3 cenários de empty state ───────────────────────────
    private enum EmptyEstado {
        SEM_PARCELAS,   // Firestore retornou lista vazia
        TODAS_PAGAS,    // Todas as parcelas têm isPago() == true
        ERRO_REDE       // onErro() do repositório foi chamado
    }

    private ParcelamentoRepository repo;
    private MovimentacaoRepository movRepo;
    private String recorrenciaId;

    // Views — card de resumo
    private TextView textTitulo;
    private TextView textProgresso;
    private TextView textValorPago;
    private TextView textValorRestante;
    private TextView textProximaData;
    private ProgressBar progressParcelas;
    private ProgressBar progressBarLoading;

    // Views — lista
    private RecyclerView recyclerParcelas;

    // Views — card de resumo inteiro (para esconder quando não há parcelas)
    private View cardResumo;

    // Views — empty state
    private View layoutEmptyState;
    private android.widget.ImageView emptyStateIcon;
    private TextView emptyStateTitulo;
    private TextView emptyStateDescricao;
    private MaterialButton emptyStateBotaoPrimario;
    private MaterialButton emptyStateBotaoSecundario;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_parcelas);

        recorrenciaId = getIntent().getStringExtra("recorrencia_id");

        if (recorrenciaId == null || recorrenciaId.isEmpty()) {
            // Sem ID: exibe empty state imediatamente, sem ir ao Firestore
            vincularViews();
            exibirEmptyState(EmptyEstado.SEM_PARCELAS);
            return;
        }

        repo    = new ParcelamentoRepository();
        movRepo = new MovimentacaoRepository();

        vincularViews();
        carregarSerie();
    }

    // ─── Binding ─────────────────────────────────────────────────────────────

    private void vincularViews() {
        textTitulo          = findViewById(R.id.textTitulo);
        textProgresso       = findViewById(R.id.textProgresso);
        textValorPago       = findViewById(R.id.textValorPago);
        textValorRestante   = findViewById(R.id.textValorRestante);
        textProximaData     = findViewById(R.id.textProximaData);
        progressParcelas    = findViewById(R.id.progressParcelas);
        progressBarLoading  = findViewById(R.id.progressBarLoading);
        recyclerParcelas    = findViewById(R.id.recyclerParcelas);
        cardResumo          = findViewById(R.id.cardResumo);

        // Empty state
        layoutEmptyState        = findViewById(R.id.layoutEmptyState);
        emptyStateIcon          = findViewById(R.id.emptyStateIcon);
        emptyStateTitulo        = findViewById(R.id.emptyStateTitulo);
        emptyStateDescricao     = findViewById(R.id.emptyStateDescricao);
        emptyStateBotaoPrimario = findViewById(R.id.emptyStateBotaoPrimario);
        emptyStateBotaoSecundario = findViewById(R.id.emptyStateBotaoSecundario);

        recyclerParcelas.setLayoutManager(new LinearLayoutManager(this));
    }

    // ─── Carga ───────────────────────────────────────────────────────────────

    private void carregarSerie() {
        // Garante que o conteúdo normal está visível e o empty state oculto
        // enquanto a requisição está em andamento.
        ocultarEmptyState();
        progressBarLoading.setVisibility(View.VISIBLE);

        repo.buscarParcelas(recorrenciaId, new ParcelamentoRepository.ParcelasCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> parcelas) {
                if (!estaAtiva()) return;
                progressBarLoading.setVisibility(View.GONE);

                if (parcelas.isEmpty()) {
                    // Cenário 1: Firestore não retornou nenhum documento
                    exibirEmptyState(EmptyEstado.SEM_PARCELAS);
                    return;
                }

                ResumoParcelamentoModel resumo = ResumoParcelamentoModel.calcular(parcelas);

                if (resumo.parcelasPendentes == 0) {
                    // Cenário 2: Existem parcelas, mas todas já estão pagas.
                    // Ainda exibimos o card de resumo (é útil!) e a lista,
                    // mas também mostramos um banner de "tudo pago".
                    exibirResumo(resumo, parcelas);
                    exibirListaParcelas(parcelas);
                    exibirEmptyStateTodasPagas(resumo);
                    return;
                }

                // Fluxo normal: há parcelas pendentes
                exibirResumo(resumo, parcelas);
                exibirListaParcelas(parcelas);
            }

            @Override
            public void onErro(String erro) {
                if (!estaAtiva()) return;
                progressBarLoading.setVisibility(View.GONE);
                // Cenário 3: falha de rede ou Firestore
                exibirEmptyState(EmptyEstado.ERRO_REDE);
            }
        });
    }

    // ─── Empty State ─────────────────────────────────────────────────────────

    /**
     * Exibe o empty state completo para os cenários SEM_PARCELAS e ERRO_REDE.
     * Oculta o card de resumo e a lista — não há nada a mostrar nesses casos.
     */
    private void exibirEmptyState(EmptyEstado estado) {
        // Oculta conteúdo normal
        cardResumo.setVisibility(View.GONE);
        recyclerParcelas.setVisibility(View.GONE);

        // Configura textos e ícone conforme o cenário
        switch (estado) {
            case SEM_PARCELAS:
                emptyStateIcon.setImageResource(R.drawable.ic_info_24);
                emptyStateTitulo.setText("Nenhuma parcela encontrada");
                emptyStateDescricao.setText(
                        "Esta série não possui parcelas registradas ou pode ter sido removida.");
                emptyStateBotaoPrimario.setText("Voltar");
                emptyStateBotaoPrimario.setOnClickListener(v -> finish());
                emptyStateBotaoSecundario.setVisibility(View.GONE);
                break;

            case ERRO_REDE:
                emptyStateIcon.setImageResource(R.drawable.ic_wifi_off_24);
                emptyStateTitulo.setText("Não foi possível carregar");
                emptyStateDescricao.setText(
                        "Verifique sua conexão e tente novamente.");
                emptyStateBotaoPrimario.setText("Tentar novamente");
                emptyStateBotaoPrimario.setOnClickListener(v -> carregarSerie());
                // Botão secundário "Voltar" aparece só no cenário de erro
                emptyStateBotaoSecundario.setVisibility(View.VISIBLE);
                emptyStateBotaoSecundario.setText("Voltar");
                emptyStateBotaoSecundario.setOnClickListener(v -> finish());
                break;

            default:
                break;
        }

        // Anima a entrada do empty state
        layoutEmptyState.setAlpha(0f);
        layoutEmptyState.setTranslationY(24f);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutEmptyState.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    /**
     * Cenário especial: todas as parcelas estão pagas.
     * O card de resumo e a lista são mantidos visíveis (o usuário ainda quer
     * ver o histórico). O empty state entra como um banner de celebração
     * abaixo da lista, não substituindo o conteúdo.
     */
    private void exibirEmptyStateTodasPagas(ResumoParcelamentoModel resumo) {

        // Recupera a raiz da Activity para que o overlay cubra tudo
        ViewGroup root = findViewById(R.id.main);

        // Formata o total pago para o subtítulo do card
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String subtitulo  = "Total pago: " + fmt.format(resumo.valorTotalCentavos / 100.0)
                + " em " + resumo.totalParcelas + " parcelas";

        // Dispara celebração — ao fechar, mostra o banner estático do empty state
        CelebracaoManager.celebrar(
                this,
                root,
                "Série quitada!",
                subtitulo,
                () -> {
                    // Callback executado APÓS o card de celebração sumir.
                    // Aqui você pode, por exemplo, mostrar o empty state verde
                    // que já existia, ou simplesmente não fazer nada.
                    if (!estaAtiva()) return;
                    exibirBannerTodasPagas(resumo); // veja método abaixo
                }
        );
    }

    private void exibirBannerTodasPagas(ResumoParcelamentoModel resumo) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        emptyStateIcon.setImageResource(R.drawable.ic_check_circle_24);
        emptyStateIcon.setAlpha(1f);
        emptyStateIcon.setColorFilter(
                getResources().getColor(android.R.color.holo_green_dark, getTheme()));

        emptyStateTitulo.setText("Série quitada! \uD83C\uDF89");
        emptyStateTitulo.setTextColor(
                getResources().getColor(android.R.color.holo_green_dark, getTheme()));

        emptyStateDescricao.setText(
                "Parabéns! " + resumo.totalParcelas + " parcelas pagas. Total: " +
                        fmt.format(resumo.valorTotalCentavos / 100.0));

        emptyStateBotaoPrimario.setText("Fechar");
        emptyStateBotaoPrimario.setOnClickListener(v -> finish());
        emptyStateBotaoSecundario.setVisibility(android.view.View.GONE);

        // Aparece suavemente logo abaixo da lista
        layoutEmptyState.setAlpha(0f);
        layoutEmptyState.setVisibility(android.view.View.VISIBLE);
        layoutEmptyState.animate().alpha(1f).setDuration(400).start();
    }

    /** Garante que o empty state está oculto e o conteúdo normal visível. */
    private void ocultarEmptyState() {
        layoutEmptyState.setVisibility(View.GONE);
        cardResumo.setVisibility(View.VISIBLE);
        recyclerParcelas.setVisibility(View.VISIBLE);
    }

    // ─── Exibição normal (sem mudança na lógica original) ────────────────────

    private void exibirResumo(ResumoParcelamentoModel r, List<MovimentacaoModel> parcelas) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        if (!parcelas.isEmpty() && parcelas.get(0).getDescricao() != null) {
            String descBase = parcelas.get(0).getDescricao()
                    .replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();
            textTitulo.setText(descBase);
        }

        textProgresso.setText(r.parcelasPagas + "/" + r.totalParcelas + " parcelas pagas");

        int percentual = r.totalParcelas > 0
                ? (int) ((double) r.parcelasPagas / r.totalParcelas * 100)
                : 0;
        progressParcelas.setMax(100);
        progressParcelas.setProgress(percentual);

        textValorPago.setText(fmt.format(r.valorPagoCentavos / 100.0));
        textValorRestante.setText(fmt.format(r.valorRestanteCentavos / 100.0));

        if (r.proximaPendente != null && r.proximaPendente.getData_movimentacao() != null) {
            String dataStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(r.proximaPendente.getData_movimentacao().toDate());
            textProximaData.setText("Próximo vencimento: " + dataStr);
        } else {
            textProximaData.setText("Todas as parcelas pagas ✓");
            textProximaData.setTextColor(
                    getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        }
    }

    private void exibirListaParcelas(List<MovimentacaoModel> parcelas) {
        List<AdapterItemListaMovimentacao> itens = new ArrayList<>();
        for (MovimentacaoModel p : parcelas) {
            itens.add(AdapterItemListaMovimentacao.linha(p));
        }

        AdapterMovimentacaoLista adapter = new AdapterMovimentacaoLista(
                this,
                new AdapterMovimentacaoLista.OnItemActionListener() {
                    @Override public void onDeleteClick(MovimentacaoModel mov) { }
                    @Override public void onLongClick(MovimentacaoModel mov) { }

                    @Override
                    public void onCheckClick(MovimentacaoModel mov) {
                        confirmarParcela(mov);
                    }

                    @Override
                    public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) { }

                    @Override
                    public void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia) { }
                }
        );

        adapter.submitList(itens);
        recyclerParcelas.setAdapter(adapter);
    }

    // ─── Confirmação de parcela (sem mudança) ────────────────────────────────

    private void confirmarParcela(MovimentacaoModel mov) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar parcela")
                .setMessage("Deseja marcar a parcela " + mov.getParcela_atual() +
                        "/" + mov.getTotal_parcelas() + " como paga?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    movRepo.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            if (!estaAtiva()) return;
                            Toast.makeText(ResumoParcelasActivity.this,
                                    "Parcela confirmada!", Toast.LENGTH_SHORT).show();
                            carregarSerie(); // recarrega — vai detectar "todas pagas" se for o caso
                        }

                        @Override
                        public void onErro(String erro) {
                            if (!estaAtiva()) return;
                            Toast.makeText(ResumoParcelasActivity.this,
                                    "Erro: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public void retornarPrincipal(View view) {
        finish();
    }

    private boolean estaAtiva() {
        return !isFinishing() && !isDestroyed();
    }
}