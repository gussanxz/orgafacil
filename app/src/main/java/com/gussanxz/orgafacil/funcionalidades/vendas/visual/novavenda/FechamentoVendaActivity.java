package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
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
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FechamentoVendaActivity extends AppCompatActivity {

    private static final String PAGAMENTO_PIX = "PIX";
    private static final String PAGAMENTO_CARTAO = "Cartão";
    private static final String PAGAMENTO_DINHEIRO = "Dinheiro";

    private ImageButton btnVoltarFechamento;
    private TextView txtQuantidadeResumo;
    private TextView txtTotalResumo;
    private RecyclerView rvResumoItens;

    private LinearLayout cardPagamentoPix;
    private LinearLayout cardPagamentoCartao;
    private LinearLayout cardPagamentoDinheiro;
    private TextView txtFormaPagamentoSelecionada;
    private LinearLayout btnFinalizarVenda;

    private AdapterResumoFechamentoVenda adapter;
    private final List<ItemSacolaVendaModel> listaItens = new ArrayList<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private String formaPagamentoSelecionada = null;
    private int quantidadeTotal = 0;
    private double valorTotal = 0.0;

    private VendaRepository vendaRepository;
    private boolean salvandoVenda = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_fechamento_venda);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootFechamentoVenda), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vendaRepository = new VendaRepository();

        inicializarComponentes();
        configurarRecyclerView();
        configurarAcoes();
        carregarDadosRecebidos();
        atualizarEstadoPagamento();
        atualizarBotaoFinalizar();
    }

    private void inicializarComponentes() {
        btnVoltarFechamento = findViewById(R.id.btnVoltarFechamento);
        txtQuantidadeResumo = findViewById(R.id.txtQuantidadeResumo);
        txtTotalResumo = findViewById(R.id.txtTotalResumo);
        rvResumoItens = findViewById(R.id.rvResumoItens);

        cardPagamentoPix = findViewById(R.id.cardPagamentoPix);
        cardPagamentoCartao = findViewById(R.id.cardPagamentoCartao);
        cardPagamentoDinheiro = findViewById(R.id.cardPagamentoDinheiro);
        txtFormaPagamentoSelecionada = findViewById(R.id.txtFormaPagamentoSelecionada);
        btnFinalizarVenda = findViewById(R.id.btnFinalizarVenda);
    }

    private void configurarRecyclerView() {
        rvResumoItens.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterResumoFechamentoVenda(listaItens);
        rvResumoItens.setAdapter(adapter);
    }

    private void configurarAcoes() {
        if (btnVoltarFechamento != null) {
            btnVoltarFechamento.setOnClickListener(v -> {
                if (!salvandoVenda) {
                    finish();
                }
            });
        }

        if (cardPagamentoPix != null) {
            cardPagamentoPix.setOnClickListener(v -> selecionarFormaPagamento(PAGAMENTO_PIX));
        }

        if (cardPagamentoCartao != null) {
            cardPagamentoCartao.setOnClickListener(v -> selecionarFormaPagamento(PAGAMENTO_CARTAO));
        }

        if (cardPagamentoDinheiro != null) {
            cardPagamentoDinheiro.setOnClickListener(v -> selecionarFormaPagamento(PAGAMENTO_DINHEIRO));
        }

        if (btnFinalizarVenda != null) {
            btnFinalizarVenda.setOnClickListener(v -> finalizarVenda());
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarDadosRecebidos() {
        ArrayList<ItemSacolaVendaModel> itensRecebidos =
                (ArrayList<ItemSacolaVendaModel>) getIntent().getSerializableExtra("itensSacola");

        quantidadeTotal = getIntent().getIntExtra("quantidadeTotal", 0);
        valorTotal = getIntent().getDoubleExtra("valorTotal", 0.0);

        listaItens.clear();
        if (itensRecebidos != null) {
            listaItens.addAll(itensRecebidos);
        }

        adapter.atualizarLista(listaItens);

        if (txtQuantidadeResumo != null) {
            txtQuantidadeResumo.setText(
                    quantidadeTotal + (quantidadeTotal == 1 ? " item" : " itens")
            );
        }

        if (txtTotalResumo != null) {
            txtTotalResumo.setText(formatadorMoeda.format(valorTotal));
        }
    }

    private void selecionarFormaPagamento(String formaPagamento) {
        if (salvandoVenda) return;

        formaPagamentoSelecionada = formaPagamento;
        atualizarEstadoPagamento();
        atualizarBotaoFinalizar();
    }

    private void atualizarEstadoPagamento() {
        atualizarCardPagamento(cardPagamentoPix, PAGAMENTO_PIX.equals(formaPagamentoSelecionada));
        atualizarCardPagamento(cardPagamentoCartao, PAGAMENTO_CARTAO.equals(formaPagamentoSelecionada));
        atualizarCardPagamento(cardPagamentoDinheiro, PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada));

        if (txtFormaPagamentoSelecionada != null) {
            txtFormaPagamentoSelecionada.setText(
                    formaPagamentoSelecionada == null
                            ? "Selecione uma forma de pagamento"
                            : "Pagamento selecionado: " + formaPagamentoSelecionada
            );
        }
    }

    private void atualizarCardPagamento(LinearLayout card, boolean selecionado) {
        if (card == null) return;

        card.setAlpha(selecionado ? 1f : 0.75f);
        card.setBackgroundResource(
                selecionado ? R.drawable.bg_pagamento_selecionado : R.drawable.fundo_arredondado
        );
    }

    private void atualizarBotaoFinalizar() {
        if (btnFinalizarVenda == null) return;

        boolean habilitado = !listaItens.isEmpty() && formaPagamentoSelecionada != null && !salvandoVenda;
        btnFinalizarVenda.setEnabled(habilitado);
        btnFinalizarVenda.setAlpha(habilitado ? 1f : 0.5f);
    }

    private void finalizarVenda() {
        if (salvandoVenda) {
            return;
        }

        if (listaItens.isEmpty()) {
            Toast.makeText(this, "Nenhum item encontrado na venda.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (formaPagamentoSelecionada == null) {
            Toast.makeText(this, "Selecione uma forma de pagamento.", Toast.LENGTH_SHORT).show();
            return;
        }

        salvandoVenda = true;
        atualizarBotaoFinalizar();

        VendaModel venda = montarVendaParaSalvar();

        vendaRepository.salvar(venda, new VendaRepository.Callback() {
            @Override
            public void onSucesso(String vendaId) {
                salvandoVenda = false;
                atualizarBotaoFinalizar();
                abrirComprovante(vendaId);
            }

            @Override
            public void onErro(String erro) {
                salvandoVenda = false;
                atualizarBotaoFinalizar();

                Toast.makeText(
                        FechamentoVendaActivity.this,
                        "Erro ao salvar venda: " + erro,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void abrirComprovante(String vendaId) {
        Intent intent = new Intent(this, ComprovanteVendaActivity.class);
        intent.putExtra("vendaId", vendaId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private VendaModel montarVendaParaSalvar() {
        VendaModel venda = new VendaModel();
        venda.setDataHoraMillis(System.currentTimeMillis());
        venda.setFormaPagamento(formaPagamentoSelecionada);
        venda.setQuantidadeTotal(quantidadeTotal);
        venda.setValorTotal(valorTotal);
        venda.setStatus(VendaModel.STATUS_FINALIZADA);
        venda.setItens(converterItensParaVenda(listaItens));
        return venda;
    }

    private List<ItemVendaRegistradaModel> converterItensParaVenda(List<ItemSacolaVendaModel> itensSacola) {
        List<ItemVendaRegistradaModel> itensVenda = new ArrayList<>();

        for (ItemSacolaVendaModel itemSacola : itensSacola) {
            itensVenda.add(new ItemVendaRegistradaModel(itemSacola));
        }

        return itensVenda;
    }
}