package com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CaixaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CaixaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.gestaoerelatorios.VendasEmAbertoActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Controle de Caixa — abre e fecha o caixa do dia.
 *
 * Estados:
 *  • Caixa FECHADO → exibe formulário de abertura + botão "Abrir Caixa"
 *  • Caixa ABERTO  → exibe resumo de vendas + botão "Fechar Caixa"
 */
public class FecharCaixaActivity extends AppCompatActivity {

    // ── UI ─────────────────────────────────────────────────────────────

    // Status card (sempre visível)
    private ImageView    imgStatusCaixaControle;
    private TextView     txtStatusCaixaControle;
    private TextView     txtAberturaInfo;

    // Seção ABERTO
    private LinearLayout secaoCaixaAberto;
    private TextView     txtQtdVendasCaixa;
    private TextView     txtTotalCaixa;
    private TextView     txtTempoAbertoValor;
    private LinearLayout btnFecharCaixa;

    // Seção FECHADO
    private LinearLayout secaoCaixaFechado;
    private EditText     etObservacaoCaixa;
    private CheckBox     cbPermiteLancamentoTardio;
    private LinearLayout btnAbrirCaixa;

    // Seção HISTÓRICO
    private LinearLayout           secaoHistoricoCaixas;
    private RecyclerView           rvHistoricoCaixas;
    private TextView               txtHistoricoVazio;
    private TextView               txtPaginacaoHistorico;

    private TextView               chipHistorico5;
    private TextView               chipHistorico10;
    private TextView               chipHistorico20;
    private TextView               chipHistorico50;
    private ImageButton            btnPaginaAnteriorHistorico;
    private ImageButton            btnPaginaProximaHistorico;
    private AdapterHistoricoCaixas adapterHistorico;

    private final List<CaixaModel> historicoCompleto = new ArrayList<>();
    private int paginaAtualHistorico = 0;
    private int itensPorPaginaHistorico = 5;

    // ── Dados ───────────────────────────────────────────────────────────

    private final CaixaRepository caixaRepository = new CaixaRepository();
    private final VendaRepository vendaRepository  = new VendaRepository();

    private ListenerRegistration listenerCaixa;
    private ListenerRegistration listenerVendas;

    private CaixaModel caixaAtual  = null;
    private boolean    operando    = false; // evita duplo clique
    private final Handler tempoAbertoHandler = new Handler(Looper.getMainLooper());
    private Runnable atualizacaoTempoAberto;

    /** Totais atuais das vendas (usados para snapshot ao fechar). */
    private int    qtdVendasAtual  = 0;
    private double totalVendasAtual = 0.0;

    private final SimpleDateFormat fmtHoraAbertura  = new SimpleDateFormat(
            "HH:mm", new Locale("pt", "BR"));
    private final SimpleDateFormat fmtDataAbertura  = new SimpleDateFormat(
            "dd/MM/yyyy", new Locale("pt", "BR"));
    private final NumberFormat     fmtMoeda = NumberFormat.getCurrencyInstance(
            new Locale("pt", "BR"));

    // ── Ciclo de vida ───────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_vendas_fechar_caixa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootFecharCaixa), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        bindViews();
        configurarHistorico();
        escutarCaixa();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerCaixa  != null) listenerCaixa.remove();
        if (listenerVendas != null) listenerVendas.remove();
        pararAtualizacaoTempoAberto();
    }

    // ── Bind ────────────────────────────────────────────────────────────

    private void bindViews() {
        ImageButton btnVoltar = findViewById(R.id.btnVoltarFecharCaixa);
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> finish());
        imgStatusCaixaControle   = findViewById(R.id.imgStatusCaixaControle);
        txtStatusCaixaControle   = findViewById(R.id.txtStatusCaixaControle);
        txtAberturaInfo          = findViewById(R.id.txtAberturaInfo);

        secaoCaixaAberto         = findViewById(R.id.secaoCaixaAberto);
        txtQtdVendasCaixa        = findViewById(R.id.txtQtdVendasCaixa);
        txtTotalCaixa            = findViewById(R.id.txtTotalCaixa);
        txtTempoAbertoValor      = findViewById(R.id.txtTempoAbertoValor);
        btnFecharCaixa           = findViewById(R.id.btnFecharCaixa);

        secaoCaixaFechado        = findViewById(R.id.secaoCaixaFechado);
        etObservacaoCaixa        = findViewById(R.id.etObservacaoCaixa);
        cbPermiteLancamentoTardio = findViewById(R.id.cbPermiteLancamentoTardio);
        btnAbrirCaixa            = findViewById(R.id.btnAbrirCaixa);

        if (btnAbrirCaixa  != null) btnAbrirCaixa.setOnClickListener(v  -> executarAbertura());
        if (btnFecharCaixa != null) btnFecharCaixa.setOnClickListener(v -> confirmarFechamento());

        secaoHistoricoCaixas       = findViewById(R.id.secaoHistoricoCaixas);
        rvHistoricoCaixas          = findViewById(R.id.rvHistoricoCaixas);
        txtHistoricoVazio          = findViewById(R.id.txtHistoricoVazio);
        txtPaginacaoHistorico      = findViewById(R.id.txtPaginacaoHistorico);
        chipHistorico5  = findViewById(R.id.chipHistorico5);
        chipHistorico10 = findViewById(R.id.chipHistorico10);
        chipHistorico20 = findViewById(R.id.chipHistorico20);
        chipHistorico50 = findViewById(R.id.chipHistorico50);
        btnPaginaAnteriorHistorico = findViewById(R.id.btnPaginaAnteriorHistorico);
        btnPaginaProximaHistorico  = findViewById(R.id.btnPaginaProximaHistorico);
    }

    // ── Histórico de caixas ─────────────────────────────────────────────

    private void configurarHistorico() {
        if (rvHistoricoCaixas == null) return;
        adapterHistorico = new AdapterHistoricoCaixas(new ArrayList<>(), this::abrirDetalhesCaixa);
        rvHistoricoCaixas.setLayoutManager(new LinearLayoutManager(this));
        rvHistoricoCaixas.setNestedScrollingEnabled(false);
        rvHistoricoCaixas.setAdapter(adapterHistorico);

        if (chipHistorico5  != null) chipHistorico5.setOnClickListener(v -> alterarItensPorPagina(5));
        if (chipHistorico10 != null) chipHistorico10.setOnClickListener(v -> alterarItensPorPagina(10));
        if (chipHistorico20 != null) chipHistorico20.setOnClickListener(v -> alterarItensPorPagina(20));
        if (chipHistorico50 != null) chipHistorico50.setOnClickListener(v -> alterarItensPorPagina(50));

        if (btnPaginaAnteriorHistorico != null) {
            btnPaginaAnteriorHistorico.setOnClickListener(v -> {
                if (paginaAtualHistorico > 0) {
                    paginaAtualHistorico--;
                    atualizarPaginacaoHistorico();
                }
            });
        }

        if (btnPaginaProximaHistorico != null) {
            btnPaginaProximaHistorico.setOnClickListener(v -> {
                int total = historicoCompleto.size();
                int proximoInicio = (paginaAtualHistorico + 1) * itensPorPaginaHistorico;
                if (proximoInicio < total) {
                    paginaAtualHistorico++;
                    atualizarPaginacaoHistorico();
                }
            });
        }

        atualizarEstiloQuantidadeHistorico();
        carregarHistorico();
    }

    private void carregarHistorico() {
        caixaRepository.buscarCaixaLegado(new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel legado) {
                caixaRepository.listarTodosCaixasHistorico(new CaixaRepository.ListaCaixaCallback() {
                    @Override
                    public void onCaixas(List<CaixaModel> recentes) {
                        historicoCompleto.clear();
                        historicoCompleto.addAll(recentes);
                        android.util.Log.d("HistoricoCaixa", "Qtd caixas carregados: " + recentes.size());

                        if (legado != null) {
                            historicoCompleto.add(legado);
                        }

                        paginaAtualHistorico = 0;
                        atualizarPaginacaoHistorico();
                    }

                    @Override
                    public void onErro(String erro) {
                        historicoCompleto.clear();

                        if (legado != null) {
                            historicoCompleto.add(legado);
                        }

                        paginaAtualHistorico = 0;
                        atualizarPaginacaoHistorico();
                    }
                });
            }

            @Override
            public void onErro(String erro) {
                caixaRepository.listarTodosCaixasHistorico(new CaixaRepository.ListaCaixaCallback() {
                    @Override
                    public void onCaixas(List<CaixaModel> recentes) {
                        historicoCompleto.clear();
                        historicoCompleto.addAll(recentes);
                        paginaAtualHistorico = 0;
                        atualizarPaginacaoHistorico();
                    }

                    @Override
                    public void onErro(String erro) {
                        historicoCompleto.clear();
                        paginaAtualHistorico = 0;
                        atualizarPaginacaoHistorico();
                    }
                });
            }
        });
    }

    private void atualizarPaginacaoHistorico() {
        if (secaoHistoricoCaixas != null) {
            secaoHistoricoCaixas.setVisibility(View.VISIBLE);
        }

        int total = historicoCompleto.size();

        if (total == 0) {
            if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.GONE);
            if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.VISIBLE);
            if (txtPaginacaoHistorico != null) txtPaginacaoHistorico.setText("0 de 0");
            atualizarEstadoBotoesPaginacao(0, 0, 0);
            return;
        }

        int inicio = paginaAtualHistorico * itensPorPaginaHistorico;
        if (inicio >= total) {
            paginaAtualHistorico = 0;
            inicio = 0;
        }

        int fim = Math.min(inicio + itensPorPaginaHistorico, total);
        List<CaixaModel> pagina = new ArrayList<>(historicoCompleto.subList(inicio, fim));

        if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.VISIBLE);
        if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.GONE);
        if (adapterHistorico != null) adapterHistorico.atualizar(pagina);

        if (rvHistoricoCaixas != null) {
            rvHistoricoCaixas.post(() -> {
                rvHistoricoCaixas.requestLayout();
                rvHistoricoCaixas.invalidateItemDecorations();
            });
        }

        if (txtPaginacaoHistorico != null) {
            txtPaginacaoHistorico.setText((inicio + 1) + "-" + fim + " de " + total);
        }

        atualizarEstadoBotoesPaginacao(inicio, fim, total);
    }

    private void atualizarEstadoBotoesPaginacao(int inicio, int fim, int total) {
        boolean podeVoltar = inicio > 0;
        boolean podeAvancar = fim < total;

        if (btnPaginaAnteriorHistorico != null) {
            btnPaginaAnteriorHistorico.setEnabled(podeVoltar);
            btnPaginaAnteriorHistorico.setAlpha(podeVoltar ? 1f : 0.35f);
        }

        if (btnPaginaProximaHistorico != null) {
            btnPaginaProximaHistorico.setEnabled(podeAvancar);
            btnPaginaProximaHistorico.setAlpha(podeAvancar ? 1f : 0.35f);
        }
    }

    private void alterarItensPorPagina(int quantidade) {
        if (itensPorPaginaHistorico == quantidade) return;
        itensPorPaginaHistorico = quantidade;
        paginaAtualHistorico = 0;
        atualizarEstiloQuantidadeHistorico();
        atualizarPaginacaoHistorico();
    }

    private void atualizarEstiloQuantidadeHistorico() {
        atualizarEstiloChipQuantidade(chipHistorico5,  itensPorPaginaHistorico == 5);
        atualizarEstiloChipQuantidade(chipHistorico10, itensPorPaginaHistorico == 10);
        atualizarEstiloChipQuantidade(chipHistorico20, itensPorPaginaHistorico == 20);
        atualizarEstiloChipQuantidade(chipHistorico50, itensPorPaginaHistorico == 50);
    }

    private void atualizarEstiloChipQuantidade(TextView chip, boolean selecionado) {
        if (chip == null) return;

        if (selecionado) {
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.vendas_chip_selected)));
            chip.setTextColor(android.graphics.Color.parseColor("#071120"));
        } else {
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.vendas_chip_bg)));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.vendas_chip_text));
        }
    }

    // ── Listener de caixa em tempo real ────────────────────────────────

    private void escutarCaixa() {
        listenerCaixa = caixaRepository.escutarCaixaAberto(new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel caixa) {
                caixaAtual = caixa;
                operando   = false;
                atualizarEstado(caixa);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(FecharCaixaActivity.this,
                        "Erro ao carregar caixa: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Atualização da UI conforme estado ───────────────────────────────

    private void atualizarEstado(CaixaModel caixa) {
        if (caixa != null && caixa.isAberto()) {
            mostrarEstadoAberto(caixa);
        } else {
            mostrarEstadoFechado();
        }
    }

    private void mostrarEstadoAberto(CaixaModel caixa) {
        // Status card
        if (txtStatusCaixaControle != null) txtStatusCaixaControle.setText("Caixa aberto");
        if (txtAberturaInfo != null) {
            Date abertura = new Date(caixa.getAbertoEmMillis());
            txtAberturaInfo.setText("Abertura: " + fmtHoraAbertura.format(abertura)
                    + "\n" + fmtDataAbertura.format(abertura));
        }
        if (imgStatusCaixaControle != null) {
            imgStatusCaixaControle.setImageResource(R.drawable.ic_lock_open_28);
            imgStatusCaixaControle.setColorFilter(0xFF4CAF50); // verde
        }

        // Visibilidade de seções
        if (secaoCaixaAberto  != null) secaoCaixaAberto.setVisibility(View.VISIBLE);
        if (secaoCaixaFechado != null) secaoCaixaFechado.setVisibility(View.GONE);

        iniciarAtualizacaoTempoAberto(caixa.getAbertoEmMillis());

        // Escuta vendas do caixa
        escutarVendas(caixa.getId());
    }

    private void mostrarEstadoFechado() {
        // Status card
        if (txtStatusCaixaControle != null) txtStatusCaixaControle.setText("Caixa fechado");
        if (txtAberturaInfo != null) txtAberturaInfo.setText("Nenhum caixa aberto");
        if (imgStatusCaixaControle != null) {
            imgStatusCaixaControle.setImageResource(R.drawable.ic_lock_24);
            imgStatusCaixaControle.setColorFilter(0xFFFFD54F); // âmbar
        }
        pararAtualizacaoTempoAberto();

        // Visibilidade de seções
        if (secaoCaixaAberto  != null) secaoCaixaAberto.setVisibility(View.GONE);
        if (secaoCaixaFechado != null) secaoCaixaFechado.setVisibility(View.VISIBLE);

        // Remove listener de vendas se havia
        if (listenerVendas != null) {
            listenerVendas.remove();
            listenerVendas = null;
        }
    }

    // ── Vendas do caixa ─────────────────────────────────────────────────

    private void iniciarAtualizacaoTempoAberto(long abertoEmMillis) {
        pararAtualizacaoTempoAberto();
        atualizacaoTempoAberto = new Runnable() {
            @Override
            public void run() {
                if (txtTempoAbertoValor != null) {
                    txtTempoAbertoValor.setText(formatarTempoAberto(abertoEmMillis));
                }
                tempoAbertoHandler.postDelayed(this, 60_000L);
            }
        };
        atualizacaoTempoAberto.run();
    }

    private void pararAtualizacaoTempoAberto() {
        if (atualizacaoTempoAberto != null) {
            tempoAbertoHandler.removeCallbacks(atualizacaoTempoAberto);
            atualizacaoTempoAberto = null;
        }
    }

    private String formatarTempoAberto(long abertoEmMillis) {
        if (abertoEmMillis <= 0L) {
            return "00:00";
        }
        long duracao = Math.max(0L, System.currentTimeMillis() - abertoEmMillis);
        long minutosTotais = duracao / 60_000L;
        long horas = minutosTotais / 60L;
        long minutos = minutosTotais % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", horas, minutos);
    }

    private void escutarVendas(String caixaId) {
        if (listenerVendas != null) listenerVendas.remove();
        listenerVendas = vendaRepository.escutarVendasDoCaixa(caixaId,
                new VendaRepository.ListaCallback() {
                    @Override
                    public void onNovosDados(List<VendaModel> lista) {
                        int    qtd = 0;
                        double tot = 0;
                        for (VendaModel v : lista) {
                            if (VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) {
                                qtd++;
                                tot += v.getValorTotal();
                            }
                        }
                        // Guarda para uso no fechamento
                        qtdVendasAtual  = qtd;
                        totalVendasAtual = tot;

                        if (txtQtdVendasCaixa != null)
                            txtQtdVendasCaixa.setText(String.valueOf(qtd));
                        if (txtTotalCaixa != null)
                            txtTotalCaixa.setText(fmtMoeda.format(tot));
                    }

                    @Override
                    public void onErro(String erro) { /* silencioso */ }
                });
    }

    private void abrirDetalhesCaixa(@NonNull CaixaModel caixa) {
        vendaRepository.buscarVendasPorNomeCaixa(caixa.getNomeCaixa(), new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> vendas) {
                exibirDialogDetalhesCaixa(caixa, vendas);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(FecharCaixaActivity.this,
                        "Erro ao carregar detalhes do caixa: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exibirDialogDetalhesCaixa(@NonNull CaixaModel caixa, List<VendaModel> vendas) {
        View view = getLayoutInflater().inflate(R.layout.dialog_detalhes_caixa, null);

        TextView txtNomeCaixaDetalhe = view.findViewById(R.id.txtNomeCaixaDetalhe);
        TextView txtDataCaixaDetalhe = view.findViewById(R.id.txtDataCaixaDetalhe);
        TextView txtVendasFinalizadasDetalhe = view.findViewById(R.id.txtVendasFinalizadasDetalhe);
        TextView txtVendasCanceladasDetalhe = view.findViewById(R.id.txtVendasCanceladasDetalhe);
        TextView txtTotalCaixaDetalhe = view.findViewById(R.id.txtTotalCaixaDetalhe);
        LinearLayout btnFinalizadasDetalhe = view.findViewById(R.id.btnFinalizadasDetalhe);
        LinearLayout btnCanceladasDetalhe = view.findViewById(R.id.btnCanceladasDetalhe);

        List<VendaModel> finalizadas = new ArrayList<>();
        List<VendaModel> canceladas = new ArrayList<>();
        double totalCaixa = 0.0;

        if (vendas != null) {
            for (VendaModel venda : vendas) {
                totalCaixa += venda.getValorTotal();

                if (VendaModel.STATUS_FINALIZADA.equals(venda.getStatus())) {
                    finalizadas.add(venda);
                } else if (VendaModel.STATUS_CANCELADA.equals(venda.getStatus())) {
                    canceladas.add(venda);
                }
            }
        }

        txtNomeCaixaDetalhe.setText(caixa.getNomeCaixa());
        txtDataCaixaDetalhe.setText(formatarDataCaixa(caixa));
        txtVendasFinalizadasDetalhe.setText(String.valueOf(finalizadas.size()));
        txtVendasCanceladasDetalhe.setText(String.valueOf(canceladas.size()));
        txtTotalCaixaDetalhe.setText(fmtMoeda.format(totalCaixa));

        btnFinalizadasDetalhe.setOnClickListener(v -> {
            if (!finalizadas.isEmpty()) {
                exibirDialogListaVendas("Vendas finalizadas • " + caixa.getNomeCaixa(), finalizadas);
            }
        });
        btnCanceladasDetalhe.setOnClickListener(v -> {
            if (!canceladas.isEmpty()) {
                exibirDialogListaVendas("Vendas canceladas • " + caixa.getNomeCaixa(), canceladas);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnReabrirDetalhesCaixa = view.findViewById(R.id.btnReabrirDetalhesCaixa);
        View btnFecharDetalhesCaixa = view.findViewById(R.id.btnFecharDetalhesCaixa);
        configurarReaberturaNoDialog(caixa, dialog, btnReabrirDetalhesCaixa);
        btnFecharDetalhesCaixa.setOnClickListener(v -> dialog.dismiss());
    }

    private void configurarReaberturaNoDialog(
            @NonNull CaixaModel caixa,
            @NonNull AlertDialog dialog,
            View btnReabrirDetalhesCaixa
    ) {
        if (btnReabrirDetalhesCaixa == null) return;

        btnReabrirDetalhesCaixa.setVisibility(View.GONE);
        btnReabrirDetalhesCaixa.setEnabled(false);

        if (caixa.isLegado() || !caixa.isFechado() || (caixaAtual != null && caixaAtual.isAberto())) {
            return;
        }

        caixaRepository.buscarUltimoCaixaFechado(new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel ultimoFechado) {
                boolean podeReabrir = ultimoFechado != null
                        && caixa.getId() != null
                        && caixa.getId().equals(ultimoFechado.getId())
                        && (caixaAtual == null || !caixaAtual.isAberto());

                btnReabrirDetalhesCaixa.setVisibility(podeReabrir ? View.VISIBLE : View.GONE);
                btnReabrirDetalhesCaixa.setEnabled(podeReabrir);

                if (podeReabrir) {
                    btnReabrirDetalhesCaixa.setOnClickListener(v ->
                            confirmarReaberturaCaixa(caixa, dialog, btnReabrirDetalhesCaixa));
                }
            }

            @Override
            public void onErro(String erro) {
                btnReabrirDetalhesCaixa.setVisibility(View.GONE);
                btnReabrirDetalhesCaixa.setEnabled(false);
            }
        });
    }

    private void confirmarReaberturaCaixa(
            @NonNull CaixaModel caixa,
            @NonNull AlertDialog dialog,
            View btnReabrirDetalhesCaixa
    ) {
        if (operando) return;

        new AlertDialog.Builder(this)
                .setTitle("Reabrir caixa")
                .setMessage("Deseja reabrir o caixa " + caixa.getNomeCaixa() + "?")
                .setPositiveButton("Reabrir", (d, w) ->
                        executarReaberturaCaixa(caixa, dialog, btnReabrirDetalhesCaixa))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarReaberturaCaixa(
            @NonNull CaixaModel caixa,
            @NonNull AlertDialog dialog,
            View btnReabrirDetalhesCaixa
    ) {
        if (operando || caixa.getId() == null) return;
        operando = true;

        if (btnReabrirDetalhesCaixa != null) {
            btnReabrirDetalhesCaixa.setEnabled(false);
            btnReabrirDetalhesCaixa.setAlpha(0.5f);
        }

        caixaRepository.reabrirCaixa(caixa.getId(), new CaixaRepository.VoidCallback() {
            @Override
            public void onSucesso(String caixaId) {
                Toast.makeText(FecharCaixaActivity.this,
                        "Caixa reaberto.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                carregarHistorico();
            }

            @Override
            public void onErro(String erro) {
                operando = false;
                if (btnReabrirDetalhesCaixa != null) {
                    btnReabrirDetalhesCaixa.setEnabled(true);
                    btnReabrirDetalhesCaixa.setAlpha(1f);
                }
                Toast.makeText(FecharCaixaActivity.this,
                        "Erro ao reabrir caixa: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatarDataCaixa(@NonNull CaixaModel caixa) {
        long referencia = caixa.getAbertoEmMillis() > 0
                ? caixa.getAbertoEmMillis()
                : caixa.getFechadoEmMillis();

        if (referencia <= 0) {
            return "—";
        }

        return new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                .format(new Date(referencia));
    }

    private void exibirDialogListaVendas(@NonNull String titulo, @NonNull List<VendaModel> vendas) {
        List<VendaModel> ordenadas = new ArrayList<>(vendas);
        Collections.sort(ordenadas, new Comparator<VendaModel>() {
            @Override
            public int compare(VendaModel a, VendaModel b) {
                long dataA = a.getDataHoraFechamentoMillis() > 0
                        ? a.getDataHoraFechamentoMillis()
                        : a.getDataHoraAberturaMillis();
                long dataB = b.getDataHoraFechamentoMillis() > 0
                        ? b.getDataHoraFechamentoMillis()
                        : b.getDataHoraAberturaMillis();
                return Long.compare(dataB, dataA);
            }
        });

        List<String> itens = new ArrayList<>();
        SimpleDateFormat fmtDataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));
        for (VendaModel venda : ordenadas) {
            long dataRef = venda.getDataHoraFechamentoMillis() > 0
                    ? venda.getDataHoraFechamentoMillis()
                    : venda.getDataHoraAberturaMillis();
            String numero = venda.getNumeroVenda() > 0 ? String.valueOf(venda.getNumeroVenda()) : venda.getId();
            itens.add("Venda #" + numero
                    + " • " + fmtMoeda.format(venda.getValorTotal())
                    + "\n" + fmtDataHora.format(new Date(dataRef)));
        }

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(itens.toArray(new String[0]), null)
                .setPositiveButton("Fechar", null)
                .show();
    }

    // ── Abertura ────────────────────────────────────────────────────────

    private void executarAbertura() {
        if (operando) return;
        operando = true;
        habilitarBotaoAbrir(false);

        String  obs     = etObservacaoCaixa        != null
                ? etObservacaoCaixa.getText().toString().trim() : "";
        boolean tardio  = cbPermiteLancamentoTardio != null
                && cbPermiteLancamentoTardio.isChecked();

        caixaRepository.abrirCaixa(obs.isEmpty() ? null : obs, tardio,
                new CaixaRepository.AbrirCaixaCallback() {
                    @Override
                    public void onSucesso(String caixaId, String nomeCaixa) {
                        // listener atualiza a UI automaticamente
                        Toast.makeText(FecharCaixaActivity.this,
                                "Caixa aberto!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String erro) {
                        operando = false;
                        habilitarBotaoAbrir(true);
                        Toast.makeText(FecharCaixaActivity.this,
                                "Erro ao abrir caixa: " + erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Fechamento ───────────────────────────────────────────────────────

    private void confirmarFechamento() {
        if (operando || caixaAtual == null) return;

        validarVendasEmAbertoAntesDeFechar();
    }

    private void validarVendasEmAbertoAntesDeFechar() {
        vendaRepository.buscarVendasEmAberto(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                List<VendaModel> pendentes = new ArrayList<>();

                if (lista != null) {
                    for (VendaModel venda : lista) {
                        if (venda != null && VendaModel.STATUS_EM_ABERTO.equals(venda.getStatus())) {
                            pendentes.add(venda);
                        }
                    }
                }

                if (pendentes.isEmpty()) {
                    exibirConfirmacaoFechamento();
                    return;
                }

                exibirBloqueioVendasEmAberto(pendentes);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(FecharCaixaActivity.this,
                        "Erro ao validar vendas em aberto: " + erro,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exibirConfirmacaoFechamento() {
        new AlertDialog.Builder(this)
                .setTitle("Fechar Caixa")
                .setMessage("Deseja fechar o caixa agora?\n\nO horário de fechamento será registrado.")
                .setPositiveButton("Fechar", (d, w) -> executarFechamento())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exibirBloqueioVendasEmAberto(@NonNull List<VendaModel> pendentes) {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Existem ")
                .append(pendentes.size())
                .append(pendentes.size() == 1 ? " venda em aberto.\n\n" : " vendas em aberto.\n\n")
                .append("É obrigatório finalizar ou cancelar essas vendas antes de fechar o caixa.");

        int limite = Math.min(pendentes.size(), 5);
        for (int i = 0; i < limite; i++) {
            VendaModel venda = pendentes.get(i);

            String numero = venda.getNumeroVenda() > 0
                    ? String.format(Locale.ROOT, "#%07d", venda.getNumeroVenda())
                    : (venda.getId() != null && venda.getId().length() >= 8
                    ? venda.getId().substring(0, 8).toUpperCase()
                    : "SEM_ID");

            mensagem.append("\n• ").append(numero);
        }

        if (pendentes.size() > limite) {
            mensagem.append("\n• ... e mais ").append(pendentes.size() - limite);
        }

        new AlertDialog.Builder(this)
                .setTitle("Fechamento bloqueado")
                .setMessage(mensagem.toString())
                .setPositiveButton("Ver vendas em aberto", (d, w) -> {
                    Intent intent = new Intent(FecharCaixaActivity.this, VendasEmAbertoActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void executarFechamento() {
        if (operando || caixaAtual == null) return;
        operando = true;
        habilitarBotaoFechar(false);

        caixaRepository.fecharCaixa(caixaAtual.getId(), qtdVendasAtual, totalVendasAtual,
                new CaixaRepository.VoidCallback() {
                    @Override
                    public void onSucesso(String caixaId) {
                        Toast.makeText(FecharCaixaActivity.this,
                                "Caixa fechado.", Toast.LENGTH_SHORT).show();
                        // listener já atualiza a UI para o estado FECHADO;
                        // recarrega o histórico para exibir o caixa recém fechado
                        carregarHistorico();
                    }

                    @Override
                    public void onErro(String erro) {
                        operando = false;
                        habilitarBotaoFechar(true);
                        Toast.makeText(FecharCaixaActivity.this,
                                "Erro ao fechar caixa: " + erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void habilitarBotaoAbrir(boolean on) {
        if (btnAbrirCaixa != null) {
            btnAbrirCaixa.setEnabled(on);
            btnAbrirCaixa.setAlpha(on ? 1f : 0.5f);
        }
    }

    private void habilitarBotaoFechar(boolean on) {
        if (btnFecharCaixa != null) {
            btnFecharCaixa.setEnabled(on);
            btnFecharCaixa.setAlpha(on ? 1f : 0.5f);
        }
    }

}
