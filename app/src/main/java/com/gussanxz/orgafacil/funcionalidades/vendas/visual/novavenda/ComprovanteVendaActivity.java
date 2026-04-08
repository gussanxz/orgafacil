package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComprovanteVendaActivity extends AppCompatActivity {

    private TextView txtNumeroVenda;
    private TextView txtDataVenda;
    private TextView txtFormaPagamento;
    private RecyclerView rvItensComprovante;
    private TextView txtQtdTotalItens;
    private TextView txtSubtotal;
    private TextView txtAcrescimo;
    private TextView txtDesconto;
    private TextView txtValorTotal;
    private LinearLayout rowAcrescimo;
    private LinearLayout rowDesconto;
    private LinearLayout btnFecharComprovante;

    private AdapterItensComprovante adapter;
    private final List<ItemVendaRegistradaModel> listaItens = new ArrayList<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat formatadorData = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));

    private VendaRepository vendaRepository;
    private LinearLayout layoutAcoesFinanceiro;
    private com.google.android.material.button.MaterialButton btnEditarDoFinanceiro;
    private com.google.android.material.button.MaterialButton btnAlternarStatusDoFinanceiro;
    private VendaModel vendaAtual; // guarda a venda carregada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_comprovante);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootComprovante), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vendaRepository = new VendaRepository();

        inicializarComponentes();
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                irParaResumoVendas();
            }
        });
        configurarRecyclerView();
        configurarAcoes();

        String vendaId = getIntent().getStringExtra("vendaId");
        if (vendaId != null) {
            carregarVenda(vendaId);
        } else {
            Toast.makeText(this, "Erro: venda não encontrada.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void inicializarComponentes() {
        txtNumeroVenda      = findViewById(R.id.txtNumeroVenda);
        txtDataVenda        = findViewById(R.id.txtDataVenda);
        txtFormaPagamento   = findViewById(R.id.txtFormaPagamentoComprovante);
        rvItensComprovante  = findViewById(R.id.rvItensComprovante);
        txtQtdTotalItens    = findViewById(R.id.txtQtdTotalItensComprovante);
        txtSubtotal         = findViewById(R.id.txtSubtotalComprovante);
        txtAcrescimo        = findViewById(R.id.txtAcrescimoComprovante);
        txtDesconto         = findViewById(R.id.txtDescontoComprovante);
        txtValorTotal       = findViewById(R.id.txtValorTotalComprovante);
        rowAcrescimo        = findViewById(R.id.rowAcrescimo);
        rowDesconto         = findViewById(R.id.rowDesconto);
        btnFecharComprovante = findViewById(R.id.btnFecharComprovante);
        layoutAcoesFinanceiro       = findViewById(R.id.layoutAcoesFinanceiro);
        btnEditarDoFinanceiro       = findViewById(R.id.btnEditarDoFinanceiro);
        btnAlternarStatusDoFinanceiro = findViewById(R.id.btnAlternarStatusDoFinanceiro);
    }

    private void configurarRecyclerView() {
        rvItensComprovante.setLayoutManager(new LinearLayoutManager(this));
        rvItensComprovante.setNestedScrollingEnabled(false);
        adapter = new AdapterItensComprovante(listaItens);
        rvItensComprovante.setAdapter(adapter);
    }

    private void configurarAcoes() {
        if (btnFecharComprovante != null) {
            btnFecharComprovante.setOnClickListener(v -> voltarParaNovaVenda());
        }
    }

    private void carregarVenda(String vendaId) {
        vendaRepository.buscarVendaPorId(vendaId, new VendaRepository.VendaCallback() {
            @Override
            public void onVenda(VendaModel venda) {
                preencherComprovante(venda);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ComprovanteVendaActivity.this,
                        "Erro ao carregar venda: " + erro, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void preencherComprovante(VendaModel venda) {
        // Número da venda — usa os 8 primeiros caracteres do ID
        String numeroVenda = venda.getNumeroVenda() > 0
                ? String.format(Locale.ROOT, "%07d", venda.getNumeroVenda())
                : "---";
        txtNumeroVenda.setText("Venda #" + numeroVenda);

        // Data
        long dataExibir = venda.getDataHoraFechamentoMillis() > 0
                ? venda.getDataHoraFechamentoMillis()
                : venda.getDataHoraAberturaMillis();
        txtDataVenda.setText(formatadorData.format(new Date(dataExibir)));

        // Forma de pagamento
        txtFormaPagamento.setText(venda.getFormaPagamento() != null
                ? venda.getFormaPagamento() : "—");

        // Itens
        listaItens.clear();
        if (venda.getItens() != null) {
            listaItens.addAll(venda.getItens());
        }
        adapter.notifyDataSetChanged();

        // Totalizadores
        txtQtdTotalItens.setText(venda.getQuantidadeTotal() + " " +
                (venda.getQuantidadeTotal() == 1 ? "item" : "itens"));

        // subtotal = soma bruta dos itens (valorTotal já carrega esse valor quando não há ajustes)
        // valorTotal = subtotal + acrescimo - desconto
        double subtotal = venda.getValorTotal();
        double valorFinal = subtotal + venda.getAcrescimo() - venda.getDesconto();
        txtSubtotal.setText(formatadorMoeda.format(subtotal));
        txtValorTotal.setText(formatadorMoeda.format(valorFinal));

        // Acréscimo — só exibe a linha se for > 0
        if (venda.getAcrescimo() > 0) {
            rowAcrescimo.setVisibility(View.VISIBLE);
            txtAcrescimo.setText("+ " + formatadorMoeda.format(venda.getAcrescimo()));
        } else {
            rowAcrescimo.setVisibility(View.GONE);
        }

        // Desconto — só exibe a linha se for > 0
        if (venda.getDesconto() > 0) {
            rowDesconto.setVisibility(View.VISIBLE);
            txtDesconto.setText("- " + formatadorMoeda.format(venda.getDesconto()));
        } else {
            rowDesconto.setVisibility(View.GONE);
        }

        vendaAtual = venda;
        boolean origemFinanceiro = getIntent().getBooleanExtra("origemFinanceiro", false);
        if (origemFinanceiro && layoutAcoesFinanceiro != null) {
            layoutAcoesFinanceiro.setVisibility(View.VISIBLE);
            boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
            btnAlternarStatusDoFinanceiro.setText(finalizada ? "Cancelar venda" : "Recuperar venda");
            btnEditarDoFinanceiro.setOnClickListener(v -> abrirEdicao(venda));
            btnAlternarStatusDoFinanceiro.setOnClickListener(v -> confirmarAlteracaoStatus(venda));
        }
    }

    private void voltarParaNovaVenda() {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void irParaResumoVendas() {
        Intent intent = new Intent(this, ResumoVendasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void abrirEdicao(VendaModel venda) {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        ArrayList<ItemSacolaVendaModel> sacola = new ArrayList<>();
        if (venda.getItens() != null) {
            for (ItemVendaRegistradaModel item : venda.getItens()) {
                sacola.add(new ItemSacolaVendaModel(item));
            }
        }
        intent.putExtra("itensSacola", sacola);
        intent.putExtra("vendaId", venda.getId());
        intent.putExtra("dataHoraOriginal",
                venda.getDataHoraFechamentoMillis() > 0
                        ? venda.getDataHoraFechamentoMillis()
                        : venda.getDataHoraAberturaMillis());
        intent.putExtra("formaPagamentoOriginal", venda.getFormaPagamento());
        startActivity(intent);
    }

    private void confirmarAlteracaoStatus(VendaModel venda) {
        boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(finalizada ? "Cancelar venda" : "Recuperar venda")
                .setMessage(finalizada
                        ? "Deseja cancelar esta venda? Ela continuará no histórico."
                        : "Deseja marcar esta venda como finalizada novamente?")
                .setPositiveButton(finalizada ? "Cancelar venda" : "Recuperar", (d, w) -> {
                    vendaRepository.alternarStatus(venda, new VendaRepository.Callback() {
                        @Override public void onSucesso(String id) { finish(); }
                        @Override public void onErro(String erro) {
                            Toast.makeText(ComprovanteVendaActivity.this,
                                    "Erro: " + erro, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Voltar", null)
                .show();
    }
}