package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.VendaModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FechamentoVendaActivity extends AppCompatActivity {

    public static final String EXTRA_CAIXA_ID = "caixaId";

    private ImageButton  btnVoltarFechamento;
    private TextView     txtQuantidadeResumo;
    private TextView     txtTotalResumo;
    private RecyclerView rvResumoItens;

    private LinearLayout cardPagamentoPix;
    private LinearLayout cardPagamentoDebito;
    private LinearLayout cardPagamentoCredito;
    private LinearLayout cardPagamentoDinheiro;
    private TextView     txtFormaPagamentoSelecionada;
    private LinearLayout btnFinalizarVenda;
    private LinearLayout btnSalvarEmAberto;

    // Seletor de data/hora -- so visivel em modo edicao
    private LinearLayout layoutSeletorData;
    private TextView     txtDataSelecionada;
    private TextView     txtHoraSelecionada;
    private ImageButton  btnSelecionarData;
    private ImageButton  btnSelecionarHora;

    private AdapterResumoFechamentoVenda adapter;
    private final List<ItemSacolaVendaModel> listaItens = new ArrayList<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat fmtData  = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final SimpleDateFormat fmtHora  = new SimpleDateFormat("HH:mm",      new Locale("pt", "BR"));
    private final SimpleDateFormat fmtChave = new SimpleDateFormat("yyyyMMdd",   Locale.US);

    private String  formaPagamentoSelecionada = null;
    private int     quantidadeTotal           = 0;

    // Atualizado: Usando int para armazenar os centavos
    private int     valorTotal                = 0;

    private String  vendaIdEdicao             = null;
    private boolean modoEdicao                = false;
    private int     numeroVendaEdicao         = 0;
    /** ID do caixa ao qual esta venda será associada. */
    private String  caixaId                   = null;
    private VendaRepository vendaRepository;
    private boolean salvandoVenda = false;

    // null em nova venda; preenchido na edicao
    private Calendar dataEscolhida = null;

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
        configurarAcoesPagamento();
        carregarDadosRecebidos();
        atualizarEstadoPagamento();
        atualizarBotaoFinalizar();
    }

    // ----------------------------------------------------------------
    // Bind
    // ----------------------------------------------------------------

    private void inicializarComponentes() {
        btnVoltarFechamento          = findViewById(R.id.btnVoltarFechamento);
        txtQuantidadeResumo          = findViewById(R.id.txtQuantidadeResumo);
        txtTotalResumo               = findViewById(R.id.txtTotalResumo);
        rvResumoItens                = findViewById(R.id.rvResumoItens);
        cardPagamentoPix             = findViewById(R.id.cardPagamentoPix);
        cardPagamentoDinheiro        = findViewById(R.id.cardPagamentoDinheiro);
        cardPagamentoDebito          = findViewById(R.id.cardPagamentoDebito);
        cardPagamentoCredito         = findViewById(R.id.cardPagamentoCredito);
        txtFormaPagamentoSelecionada = findViewById(R.id.txtFormaPagamentoSelecionada);
        btnFinalizarVenda            = findViewById(R.id.btnFinalizarVenda);
        btnSalvarEmAberto            = findViewById(R.id.btnSalvarEmAberto);

        // layoutSeletorData comeca GONE no XML.
        // Os filhos (txtDataSelecionada, btnSelecionarHora, etc.) sao vinculados
        // em configurarSeletorDataHora(), DEPOIS de setVisibility(VISIBLE),
        // o que garante que o Android os encontre corretamente.
        layoutSeletorData = findViewById(R.id.layoutSeletorData);
    }

    private void configurarRecyclerView() {
        rvResumoItens.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterResumoFechamentoVenda(listaItens);
        rvResumoItens.setAdapter(adapter);
    }

    private void configurarAcoesPagamento() {
        if (btnVoltarFechamento != null)
            btnVoltarFechamento.setOnClickListener(v -> { if (!salvandoVenda) finish(); });

        if (cardPagamentoPix     != null) cardPagamentoPix.setOnClickListener(v     -> selecionarFormaPagamento(VendaModel.PAGAMENTO_PIX));
        if (cardPagamentoDinheiro!= null) cardPagamentoDinheiro.setOnClickListener(v -> selecionarFormaPagamento(VendaModel.PAGAMENTO_DINHEIRO));
        if (cardPagamentoDebito  != null) cardPagamentoDebito.setOnClickListener(v   -> selecionarFormaPagamento(VendaModel.PAGAMENTO_DEBITO));
        if (cardPagamentoCredito != null) cardPagamentoCredito.setOnClickListener(v  -> selecionarFormaPagamento(VendaModel.PAGAMENTO_CREDITO));

        if (btnSalvarEmAberto != null) btnSalvarEmAberto.setOnClickListener(v -> salvarEmAberto());
        if (btnFinalizarVenda != null) btnFinalizarVenda.setOnClickListener(v -> finalizarVenda());
    }

    // ----------------------------------------------------------------
    // Intent
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void carregarDadosRecebidos() {
        ArrayList<ItemSacolaVendaModel> itensRecebidos =
                (ArrayList<ItemSacolaVendaModel>) getIntent().getSerializableExtra("itensSacola");

        vendaIdEdicao   = getIntent().getStringExtra("vendaId");
        caixaId         = getIntent().getStringExtra(EXTRA_CAIXA_ID);

        long   dataHoraOriginal       = getIntent().getLongExtra("dataHoraOriginal", 0L);
        String formaPagamentoOriginal = getIntent().getStringExtra("formaPagamentoOriginal");

        modoEdicao = vendaIdEdicao != null && dataHoraOriginal > 0;
        numeroVendaEdicao   = getIntent().getIntExtra("numeroVenda", 0);

        listaItens.clear();
        if (itensRecebidos != null) listaItens.addAll(itensRecebidos);
        adapter.atualizarLista(listaItens);

        // Recalcula totais a partir dos itens recebidos — nunca confia nos valores da Intent
        quantidadeTotal = recalcularQuantidadeTotal(listaItens);
        valorTotal      = recalcularValorTotal(listaItens);

        if (txtQuantidadeResumo != null)
            txtQuantidadeResumo.setText(quantidadeTotal + (quantidadeTotal == 1 ? " item" : " itens"));

        // Atualizado: Dividindo por 100.0 APENAS na hora de exibir na tela
        if (txtTotalResumo != null)
            txtTotalResumo.setText(formatadorMoeda.format(valorTotal / 100.0));

        if (formaPagamentoOriginal != null)
            formaPagamentoSelecionada = formaPagamentoOriginal;

        dataEscolhida = Calendar.getInstance();
        if (modoEdicao && dataHoraOriginal > 0)
            dataEscolhida.setTimeInMillis(dataHoraOriginal);
        configurarSeletorDataHora();

    }

    // ----------------------------------------------------------------
    // Seletor de data e hora
    // ----------------------------------------------------------------

    private void configurarSeletorDataHora() {
        if (layoutSeletorData == null) return;

        layoutSeletorData.setVisibility(android.view.View.VISIBLE);

        txtDataSelecionada = layoutSeletorData.findViewById(R.id.txtDataSelecionada);
        txtHoraSelecionada = layoutSeletorData.findViewById(R.id.txtHoraSelecionada);
        btnSelecionarData  = layoutSeletorData.findViewById(R.id.btnSelecionarData);
        btnSelecionarHora  = layoutSeletorData.findViewById(R.id.btnSelecionarHora);

        if (btnSelecionarData != null)
            btnSelecionarData.setOnClickListener(v -> abrirDatePicker());

        if (btnSelecionarHora != null)
            btnSelecionarHora.setOnClickListener(v -> abrirTimePicker());

        atualizarExibicaoDataHora();
    }

    private void abrirDatePicker() {
        if (dataEscolhida == null) dataEscolhida = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    dataEscolhida.set(Calendar.YEAR,         year);
                    dataEscolhida.set(Calendar.MONTH,        month);
                    dataEscolhida.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    atualizarExibicaoDataHora();
                },
                dataEscolhida.get(Calendar.YEAR),
                dataEscolhida.get(Calendar.MONTH),
                dataEscolhida.get(Calendar.DAY_OF_MONTH)
        ) {{
            getDatePicker().setMaxDate(System.currentTimeMillis());
        }}.show();
    }

    private void abrirTimePicker() {
        if (dataEscolhida == null) dataEscolhida = Calendar.getInstance();

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    dataEscolhida.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    dataEscolhida.set(Calendar.MINUTE,      minute);
                    dataEscolhida.set(Calendar.SECOND,      0);
                    dataEscolhida.set(Calendar.MILLISECOND, 0);
                    atualizarExibicaoDataHora();
                },
                dataEscolhida.get(Calendar.HOUR_OF_DAY),
                dataEscolhida.get(Calendar.MINUTE),
                true  // formato 24h
        ).show();
    }

    private void atualizarExibicaoDataHora() {
        if (dataEscolhida == null) return;

        if (txtDataSelecionada != null) {
            String hoje  = fmtChave.format(new Date());
            String ontem = fmtChave.format(new Date(System.currentTimeMillis() - 86_400_000L));
            String chave = fmtChave.format(dataEscolhida.getTime());

            if      (chave.equals(hoje))  txtDataSelecionada.setText("Hoje");
            else if (chave.equals(ontem)) txtDataSelecionada.setText("Ontem");
            else                          txtDataSelecionada.setText(fmtData.format(dataEscolhida.getTime()));
        }

        if (txtHoraSelecionada != null)
            txtHoraSelecionada.setText(fmtHora.format(dataEscolhida.getTime()));
    }

    // ----------------------------------------------------------------
    // Pagamento
    // ----------------------------------------------------------------

    private void selecionarFormaPagamento(String formaPagamento) {
        if (salvandoVenda) return;
        formaPagamentoSelecionada = formaPagamento;
        atualizarEstadoPagamento();
        atualizarBotaoFinalizar();
    }

    private void atualizarEstadoPagamento() {
        atualizarCardPagamento(cardPagamentoPix,      VendaModel.PAGAMENTO_PIX.equals(formaPagamentoSelecionada));
        atualizarCardPagamento(cardPagamentoDinheiro, VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada));
        atualizarCardPagamento(cardPagamentoDebito,   VendaModel.PAGAMENTO_DEBITO.equals(formaPagamentoSelecionada));
        atualizarCardPagamento(cardPagamentoCredito,  VendaModel.PAGAMENTO_CREDITO.equals(formaPagamentoSelecionada));

        if (txtFormaPagamentoSelecionada != null) {
            txtFormaPagamentoSelecionada.setText(formaPagamentoSelecionada == null
                    ? "Selecione uma forma de pagamento"
                    : "Pagamento selecionado: " + formaPagamentoSelecionada);
        }
    }

    private void atualizarCardPagamento(LinearLayout card, boolean selecionado) {
        if (card == null) return;
        card.setAlpha(selecionado ? 1f : 0.75f);
        card.setBackgroundResource(selecionado
                ? R.drawable.bg_pagamento_selecionado
                : R.drawable.fundo_arredondado);
    }

    private void atualizarBotaoFinalizar() {
        if (btnFinalizarVenda == null) return;
        boolean habilitado = !listaItens.isEmpty() && formaPagamentoSelecionada != null && !salvandoVenda;
        btnFinalizarVenda.setEnabled(habilitado);
        btnFinalizarVenda.setAlpha(habilitado ? 1f : 0.5f);

        if (btnSalvarEmAberto != null) {
            boolean podeAbrir = !listaItens.isEmpty() && !salvandoVenda;
            btnSalvarEmAberto.setEnabled(podeAbrir);
            btnSalvarEmAberto.setAlpha(podeAbrir ? 1f : 0.5f);
        }
    }

    // ----------------------------------------------------------------
    // Salvar
    // ----------------------------------------------------------------

    private void finalizarVenda() {
        if (salvandoVenda) return;
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

        vendaRepository.salvar(montarVendaParaSalvar(), new VendaRepository.Callback() {
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
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao salvar venda: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private VendaModel montarVendaParaSalvar() {
        VendaModel venda = new VendaModel();
        if (vendaIdEdicao != null) venda.setId(vendaIdEdicao);
        if (numeroVendaEdicao > 0) venda.setNumeroVenda(numeroVendaEdicao);

        long dataFechamento = dataEscolhida != null
                ? dataEscolhida.getTimeInMillis()
                : System.currentTimeMillis();

        venda.setDataHoraAberturaMillis(dataFechamento);
        venda.setDataHoraFechamentoMillis(dataFechamento);
        venda.setDiaKey(FirestoreSchema.diaKey(new Date(dataFechamento)));
        venda.setFormaPagamento(formaPagamentoSelecionada);
        venda.setQuantidadeTotal(recalcularQuantidadeTotal(listaItens));
        venda.setValorTotal(recalcularValorTotal(listaItens));
        venda.setStatus(VendaModel.STATUS_FINALIZADA);
        venda.setItens(converterItensParaVenda(listaItens));
        venda.setCaixaId(caixaId);

        return venda;
    }

    private void abrirComprovante(String vendaId) {
        Intent intent = new Intent(this, ComprovanteVendaActivity.class);
        intent.putExtra("vendaId", vendaId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void salvarEmAberto() {
        if (salvandoVenda) return;
        if (listaItens.isEmpty()) {
            Toast.makeText(this, "Nenhum item encontrado na venda.", Toast.LENGTH_SHORT).show();
            return;
        }

        salvandoVenda = true;
        atualizarBotaoFinalizar();

        VendaModel venda = new VendaModel();
        if (vendaIdEdicao != null) venda.setId(vendaIdEdicao);
        venda.setDataHoraAberturaMillis(System.currentTimeMillis());
        venda.setDataHoraFechamentoMillis(0);
        venda.setFormaPagamento(null);
        venda.setQuantidadeTotal(recalcularQuantidadeTotal(listaItens));
        venda.setValorTotal(recalcularValorTotal(listaItens));
        venda.setStatus(VendaModel.STATUS_EM_ABERTO);
        venda.setItens(converterItensParaVenda(listaItens));
        venda.setCaixaId(caixaId);

        vendaRepository.salvar(venda, new VendaRepository.Callback() {
            @Override
            public void onSucesso(String vendaId) {
                salvandoVenda = false;
                Toast.makeText(FechamentoVendaActivity.this,
                        "Venda salva em aberto.", Toast.LENGTH_SHORT).show();
                voltarParaNovaVenda();
            }

            @Override
            public void onErro(String erro) {
                salvandoVenda = false;
                atualizarBotaoFinalizar();
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao salvar: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private List<ItemVendaRegistradaModel> converterItensParaVenda(List<ItemSacolaVendaModel> itensSacola) {
        List<ItemVendaRegistradaModel> itensVenda = new ArrayList<>();
        for (ItemSacolaVendaModel item : itensSacola)
            itensVenda.add(new ItemVendaRegistradaModel(item));
        return itensVenda;
    }

    /** Atualizado: Retorna int e assume que os itens já trabalham com centavos */
    private int recalcularValorTotal(List<ItemSacolaVendaModel> itens) {
        // Coloquei um cast aqui temporariamente caso o seu ItemSacolaVendaModel
        // ainda esteja retornando double. Assim que mudarmos o modelo, pode tirar o cast!
        int total = 0;
        for (ItemSacolaVendaModel item : itens) {
            total += (int) item.getSubtotal();
        }
        return total;
    }

    private int recalcularQuantidadeTotal(List<ItemSacolaVendaModel> itens) {
        int total = 0;
        for (ItemSacolaVendaModel item : itens) total += item.getQuantidade();
        return total;
    }

    private void voltarParaNovaVenda() {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}