package com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa;

import android.os.Bundle;
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
    private TextView     txtFechamentoInfo;
    private LinearLayout btnFecharCaixa;

    // Seção FECHADO
    private LinearLayout secaoCaixaFechado;
    private EditText     etObservacaoCaixa;
    private CheckBox     cbPermiteLancamentoTardio;
    private LinearLayout btnAbrirCaixa;

    // Seção HISTÓRICO
    private LinearLayout          secaoHistoricoCaixas;
    private RecyclerView          rvHistoricoCaixas;
    private TextView              txtHistoricoVazio;
    private AdapterHistoricoCaixas adapterHistorico;

    // ── Dados ───────────────────────────────────────────────────────────

    private final CaixaRepository caixaRepository = new CaixaRepository();
    private final VendaRepository vendaRepository  = new VendaRepository();

    private ListenerRegistration listenerCaixa;
    private ListenerRegistration listenerVendas;

    private CaixaModel caixaAtual  = null;
    private boolean    operando    = false; // evita duplo clique

    /** Totais atuais das vendas (usados para snapshot ao fechar). */
    private int    qtdVendasAtual  = 0;
    private double totalVendasAtual = 0.0;

    private final SimpleDateFormat fmtHora  = new SimpleDateFormat(
            "HH:mm 'de' dd/MM/yyyy", new Locale("pt", "BR"));
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
        txtFechamentoInfo        = findViewById(R.id.txtFechamentoInfo);
        btnFecharCaixa           = findViewById(R.id.btnFecharCaixa);

        secaoCaixaFechado        = findViewById(R.id.secaoCaixaFechado);
        etObservacaoCaixa        = findViewById(R.id.etObservacaoCaixa);
        cbPermiteLancamentoTardio = findViewById(R.id.cbPermiteLancamentoTardio);
        btnAbrirCaixa            = findViewById(R.id.btnAbrirCaixa);

        if (btnAbrirCaixa  != null) btnAbrirCaixa.setOnClickListener(v  -> executarAbertura());
        if (btnFecharCaixa != null) btnFecharCaixa.setOnClickListener(v -> confirmarFechamento());

        secaoHistoricoCaixas = findViewById(R.id.secaoHistoricoCaixas);
        rvHistoricoCaixas    = findViewById(R.id.rvHistoricoCaixas);
        txtHistoricoVazio    = findViewById(R.id.txtHistoricoVazio);
    }

    // ── Histórico de caixas ─────────────────────────────────────────────

    private void configurarHistorico() {
        if (rvHistoricoCaixas == null) return;
        adapterHistorico = new AdapterHistoricoCaixas(new ArrayList<>(), this::abrirDetalhesCaixa);
        rvHistoricoCaixas.setLayoutManager(new LinearLayoutManager(this));
        rvHistoricoCaixas.setAdapter(adapterHistorico);
        carregarHistorico();
    }

    private void carregarHistorico() {
        // 1. Carrega os caixas recentes (excluindo o legado)
        caixaRepository.listarCaixasRecentes(10, new CaixaRepository.ListaCaixaCallback() {
            @Override
            public void onCaixas(java.util.List<CaixaModel> recentes) {
                // 2. Busca o caixa_0 e anexa ao final
                caixaRepository.buscarCaixaLegado(new CaixaRepository.CaixaCallback() {
                    @Override
                    public void onCaixa(CaixaModel legado) {
                        java.util.List<CaixaModel> lista = new ArrayList<>(recentes);
                        if (legado != null) lista.add(legado);

                        if (secaoHistoricoCaixas != null)
                            secaoHistoricoCaixas.setVisibility(View.VISIBLE);
                        if (lista.isEmpty()) {
                            if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.GONE);
                            if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.VISIBLE);
                        } else {
                            if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.VISIBLE);
                            if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.GONE);
                            if (adapterHistorico  != null) adapterHistorico.atualizar(lista);
                        }
                    }

                    @Override
                    public void onErro(String erro) {
                        // caixa_0 não carregou — exibe apenas os recentes
                        exibirListaHistorico(recentes);
                    }
                });
            }

            @Override
            public void onErro(String erro) {
                // falha silenciosa — a seção fica oculta
            }
        });
    }

    private void exibirListaHistorico(java.util.List<CaixaModel> lista) {
        if (secaoHistoricoCaixas != null)
            secaoHistoricoCaixas.setVisibility(View.VISIBLE);
        if (lista.isEmpty()) {
            if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.GONE);
            if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.VISIBLE);
        } else {
            if (rvHistoricoCaixas != null) rvHistoricoCaixas.setVisibility(View.VISIBLE);
            if (txtHistoricoVazio != null) txtHistoricoVazio.setVisibility(View.GONE);
            if (adapterHistorico  != null) adapterHistorico.atualizar(lista);
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
        if (txtStatusCaixaControle != null) txtStatusCaixaControle.setText("Aberto");
        if (txtAberturaInfo != null)
            txtAberturaInfo.setText("Abertura: " +
                    fmtHora.format(new Date(caixa.getAbertoEmMillis())));
        if (imgStatusCaixaControle != null) {
            imgStatusCaixaControle.setImageResource(R.drawable.ic_lock_open_28);
            imgStatusCaixaControle.setColorFilter(0xFF4CAF50); // verde
        }

        // Visibilidade de seções
        if (secaoCaixaAberto  != null) secaoCaixaAberto.setVisibility(View.VISIBLE);
        if (secaoCaixaFechado != null) secaoCaixaFechado.setVisibility(View.GONE);

        // Atualiza hora de fechamento prevista (agora)
        if (txtFechamentoInfo != null)
            txtFechamentoInfo.setText("Fechamento registrado em: " + fmtHora.format(new Date()));

        // Escuta vendas do caixa
        escutarVendas(caixa.getId());
    }

    private void mostrarEstadoFechado() {
        // Status card
        if (txtStatusCaixaControle != null) txtStatusCaixaControle.setText("Fechado");
        if (txtAberturaInfo != null) txtAberturaInfo.setText("Nenhum caixa aberto");
        if (imgStatusCaixaControle != null) {
            imgStatusCaixaControle.setImageResource(R.drawable.ic_lock_24);
            imgStatusCaixaControle.setColorFilter(0xFFFFD54F); // âmbar
        }

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

        View btnFecharDetalhesCaixa = view.findViewById(R.id.btnFecharDetalhesCaixa);
        btnFecharDetalhesCaixa.setOnClickListener(v -> dialog.dismiss());
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

        new AlertDialog.Builder(this)
                .setTitle("Fechar Caixa")
                .setMessage("Deseja fechar o caixa agora?\n\nO horário de fechamento será registrado.")
                .setPositiveButton("Fechar", (d, w) -> executarFechamento())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarFechamento() {
        if (operando || caixaAtual == null) return;
        operando = true;
        habilitarBotaoFechar(false);

        // Atualiza hora na UI antes de confirmar
        if (txtFechamentoInfo != null)
            txtFechamentoInfo.setText("Fechamento registrado em: " + fmtHora.format(new Date()));

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
