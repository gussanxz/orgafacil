package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoCallback;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoVoidCallback;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CaixaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendasRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CaixaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.util_helper.MascaraMoedaWatcher;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FechamentoVendaActivity extends AppCompatActivity {

    public static final String EXTRA_CAIXA_ID   = "caixaId";
    public static final String EXTRA_NOME_CAIXA = "nomeCaixa";

    private ImageButton  btnVoltarFechamento;
    private TextView     txtQuantidadeResumo;
    private TextView     txtTotalResumo;
    private RecyclerView rvResumoItens;

    private LinearLayout cardPagamentoPix;
    private LinearLayout cardPagamentoDebito;
    private LinearLayout cardPagamentoCredito;
    private LinearLayout cardPagamentoDinheiro;
    private LinearLayout layoutPagamentoDinheiro;
    private EditText     editValorRecebidoDinheiro;
    private TextView     txtTrocoDinheiro;
    private TextView     txtErroValorRecebido;
    private TextView     txtFormaPagamentoSelecionada;
    private LinearLayout btnFinalizarVenda;
    private TextView     txtLabelBtnFinalizar;
    private TextView     txtCaixaSelecionado;
    private ImageButton  btnAlterarCaixa;
    private LinearLayout btnSalvarEmAberto;
    private LinearLayout rowNumeroVendaFechamento;
    private TextView     txtNumeroVendaFechamento;
    private TextView     txtClienteSelecionado;
    private TextView     txtVendedorResponsavel;
    private LinearLayout btnSelecionarCliente;
    private LinearLayout btnClienteAvulso;
    private LinearLayout btnCadastrarClienteRapido;

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
    private boolean pagamentosRecolhidos      = false;
    private MascaraMoedaWatcher valorRecebidoWatcher;
    private int     quantidadeTotal           = 0;
    private double  valorTotal                = 0.0;
    private String  vendaIdEdicao             = null;
    private boolean modoEdicao                = false;
    private int     numeroVendaEdicao         = 0;
    /** ID do caixa ao qual esta venda será associada. */
    private String  caixaId                   = null;
    /** Nome legível do caixa (ex.: "20260420_1"), desnormalizado na venda. */
    private String  nomeCaixa                 = null;
    private String  clienteId                 = null;
    private String  clienteNome               = null;
    private String  clienteTelefone           = null;
    private String  vendedorId                = null;
    private String  vendedorNome              = null;
    private String  vendedorEmail             = null;
    private VendaRepository  vendaRepository;
    private VendasRepository vendasRepository;
    private CaixaRepository  caixaRepository;
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
        vendasRepository = new VendasRepository();
        caixaRepository = new CaixaRepository();

        inicializarComponentes();
        configurarRecyclerView();
        configurarAcoesPagamento();
        carregarDadosRecebidos();
        configurarClienteEVendedor();
        configurarSeletorCaixa();
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
        layoutPagamentoDinheiro      = findViewById(R.id.layoutPagamentoDinheiro);
        editValorRecebidoDinheiro    = findViewById(R.id.editValorRecebidoDinheiro);
        txtTrocoDinheiro             = findViewById(R.id.txtTrocoDinheiro);
        txtErroValorRecebido         = findViewById(R.id.txtErroValorRecebido);
        txtFormaPagamentoSelecionada = findViewById(R.id.txtFormaPagamentoSelecionada);
        btnFinalizarVenda            = findViewById(R.id.btnFinalizarVenda);
        txtLabelBtnFinalizar         = findViewById(R.id.txtLabelBtnFinalizar);
        btnSalvarEmAberto            = findViewById(R.id.btnSalvarEmAberto);
        rowNumeroVendaFechamento     = findViewById(R.id.rowNumeroVendaFechamento);
        txtNumeroVendaFechamento     = findViewById(R.id.txtNumeroVendaFechamento);
        txtClienteSelecionado        = findViewById(R.id.txtClienteSelecionado);
        txtVendedorResponsavel       = findViewById(R.id.txtVendedorResponsavel);
        btnSelecionarCliente         = findViewById(R.id.btnSelecionarCliente);
        btnClienteAvulso             = findViewById(R.id.btnClienteAvulso);
        btnCadastrarClienteRapido    = findViewById(R.id.btnCadastrarClienteRapido);

        txtCaixaSelecionado  = findViewById(R.id.txtCaixaSelecionado);
        btnAlterarCaixa      = findViewById(R.id.btnAlterarCaixa);

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

        configurarCamposDinheiro();

        if (btnSalvarEmAberto != null) btnSalvarEmAberto.setOnClickListener(v -> salvarEmAberto());
        if (btnFinalizarVenda != null) btnFinalizarVenda.setOnClickListener(v -> finalizarVenda());
    }

    private void configurarCamposDinheiro() {
        if (editValorRecebidoDinheiro == null) return;

        valorRecebidoWatcher = new MascaraMoedaWatcher(editValorRecebidoDinheiro);
        editValorRecebidoDinheiro.addTextChangedListener(valorRecebidoWatcher);
        valorRecebidoWatcher.setValorInicial(0.0);
        editValorRecebidoDinheiro.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                atualizarTrocoDinheiro();
                atualizarBotaoFinalizar();
            }
        });
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
        nomeCaixa       = getIntent().getStringExtra(EXTRA_NOME_CAIXA);
        clienteId       = getIntent().getStringExtra("clienteId");
        clienteNome     = getIntent().getStringExtra("clienteNome");
        clienteTelefone = getIntent().getStringExtra("clienteTelefone");
        quantidadeTotal = getIntent().getIntExtra("quantidadeTotal", 0);
        valorTotal      = getIntent().getDoubleExtra("valorTotal", 0.0);

        long   dataHoraOriginal       = getIntent().getLongExtra("dataHoraOriginal", 0L);
        String formaPagamentoOriginal = getIntent().getStringExtra("formaPagamentoOriginal");
        double valorRecebidoOriginal  = getIntent().getDoubleExtra("valorRecebidoDinheiroOriginal", 0.0);

        modoEdicao = vendaIdEdicao != null && dataHoraOriginal > 0;
        numeroVendaEdicao   = getIntent().getIntExtra("numeroVenda", 0);

        listaItens.clear();
        if (itensRecebidos != null) listaItens.addAll(itensRecebidos);
        adapter.atualizarLista(listaItens);

        if (txtQuantidadeResumo != null)
            txtQuantidadeResumo.setText(quantidadeTotal + (quantidadeTotal == 1 ? " item" : " itens"));
        if (txtTotalResumo != null)
            txtTotalResumo.setText(formatadorMoeda.format(valorTotal));

        if (formaPagamentoOriginal != null) {
            formaPagamentoSelecionada = formaPagamentoOriginal;
            pagamentosRecolhidos = true;
        }

        if (valorRecebidoWatcher != null && valorRecebidoOriginal > 0)
            valorRecebidoWatcher.setValorInicial(valorRecebidoOriginal);

        dataEscolhida = Calendar.getInstance();
        if (modoEdicao && dataHoraOriginal > 0)
            dataEscolhida.setTimeInMillis(dataHoraOriginal);

        if (modoEdicao) {
            // Oculta "Salvar em Aberto" — venda já foi finalizada
            if (btnSalvarEmAberto != null)
                btnSalvarEmAberto.setVisibility(android.view.View.GONE);

            // Renomeia botão principal para "Atualizar Venda"
            if (txtLabelBtnFinalizar != null)
                txtLabelBtnFinalizar.setText("ATUALIZAR VENDA");

            // Exibe número da venda no card de caixa
            if (rowNumeroVendaFechamento != null)
                rowNumeroVendaFechamento.setVisibility(android.view.View.VISIBLE);
            if (txtNumeroVendaFechamento != null) {
                txtNumeroVendaFechamento.setText(numeroVendaEdicao > 0
                        ? String.format(Locale.ROOT, "#%07d", numeroVendaEdicao)
                        : "—");
            }
        }

        configurarSeletorDataHora();

    }

    // ----------------------------------------------------------------
    // Cliente e vendedor
    // ----------------------------------------------------------------

    private void configurarClienteEVendedor() {
        carregarVendedorResponsavel();
        atualizarExibicaoCliente();

        if (btnSelecionarCliente != null)
            btnSelecionarCliente.setOnClickListener(v -> abrirDialogSelecionarCliente());
        if (btnClienteAvulso != null)
            btnClienteAvulso.setOnClickListener(v -> definirClienteAvulso());
        if (btnCadastrarClienteRapido != null)
            btnCadastrarClienteRapido.setOnClickListener(v -> abrirDialogCadastroClienteRapido());
    }

    private void carregarVendedorResponsavel() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            vendedorId = user.getUid();
            vendedorNome = user.getDisplayName();
            vendedorEmail = user.getEmail();
        }
        if (vendedorNome == null || vendedorNome.trim().isEmpty()) {
            vendedorNome = vendedorEmail != null && !vendedorEmail.trim().isEmpty()
                    ? vendedorEmail
                    : "Usuario responsavel";
        }
        if (txtVendedorResponsavel != null) {
            txtVendedorResponsavel.setText(vendedorNome);
        }
    }

    private void atualizarExibicaoCliente() {
        if (txtClienteSelecionado == null) return;
        boolean temCliente = clienteId != null && !clienteId.trim().isEmpty()
                && clienteNome != null && !clienteNome.trim().isEmpty();
        txtClienteSelecionado.setText(temCliente ? clienteNome : "Venda avulsa");
    }

    private void definirClienteAvulso() {
        clienteId = null;
        clienteNome = null;
        clienteTelefone = null;
        atualizarExibicaoCliente();
    }

    private void abrirDialogSelecionarCliente() {
        vendasRepository.listarClientes(new RepoCallback<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot snap) {
                List<DocumentSnapshot> docs = snap != null ? snap.getDocuments() : new ArrayList<>();
                if (docs.isEmpty()) {
                    Toast.makeText(FechamentoVendaActivity.this,
                            "Nenhum cliente cadastrado.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] opcoes = new String[docs.size()];
                for (int i = 0; i < docs.size(); i++) {
                    String nome = docs.get(i).getString("nome");
                    String telefone = docs.get(i).getString("telefone");
                    opcoes[i] = nome != null && !nome.trim().isEmpty() ? nome : "Cliente sem nome";
                    if (telefone != null && !telefone.trim().isEmpty()) {
                        opcoes[i] += " - " + telefone;
                    }
                }

                new AlertDialog.Builder(FechamentoVendaActivity.this)
                        .setTitle("Selecionar cliente")
                        .setItems(opcoes, (dialog, which) -> {
                            DocumentSnapshot doc = docs.get(which);
                            clienteId = doc.getId();
                            clienteNome = doc.getString("nome");
                            clienteTelefone = doc.getString("telefone");
                            atualizarExibicaoCliente();
                        })
                        .setNeutralButton("Venda avulsa", (dialog, which) -> definirClienteAvulso())
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao carregar clientes: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirDialogCadastroClienteRapido() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 8, padding, 0);

        EditText inputNome = new EditText(this);
        inputNome.setHint("Nome do cliente");
        inputNome.setSingleLine(true);
        container.addView(inputNome);

        EditText inputTelefone = new EditText(this);
        inputTelefone.setHint("Telefone (opcional)");
        inputTelefone.setSingleLine(true);
        inputTelefone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        container.addView(inputTelefone);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cliente rapido")
                .setView(container)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nome = inputNome.getText().toString().trim();
            String telefone = inputTelefone.getText().toString().trim();
            if (nome.isEmpty()) {
                inputNome.setError("Informe o nome");
                return;
            }
            salvarClienteRapido(nome, telefone, dialog);
        }));
        dialog.show();
    }

    private void salvarClienteRapido(String nome, String telefone, AlertDialog dialog) {
        String novoClienteId = FirestoreSchema.vendasClientesCol().document().getId();
        Map<String, Object> data = new HashMap<>();
        data.put("nome", nome);
        data.put("telefone", telefone);
        data.put("statusAtivo", true);

        vendasRepository.salvarCliente(novoClienteId, data, new RepoVoidCallback() {
            @Override
            public void onSuccess() {
                clienteId = novoClienteId;
                clienteNome = nome;
                clienteTelefone = telefone;
                atualizarExibicaoCliente();
                dialog.dismiss();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao salvar cliente: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ----------------------------------------------------------------
    // Seletor de data e hora
    //
    // O bind dos filhos e feito AQUI, apos setVisibility(VISIBLE),
    // porque views dentro de layouts GONE podem nao ser encontradas
    // pelo findViewById da Activity em algumas versoes do Android.
    // Usar layoutSeletorData.findViewById() como raiz e o mais seguro.
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
    // Caixa
    // ----------------------------------------------------------------

    private void configurarSeletorCaixa() {
        atualizarExibicaoCaixa();

        if (btnAlterarCaixa == null) return;

        // Permite vincular venda nova ou edicao a um caixa antigo compativel.
        btnAlterarCaixa.setVisibility(android.view.View.VISIBLE);
        btnAlterarCaixa.setOnClickListener(v -> abrirDialogAlterarCaixa());
    }

    private void atualizarExibicaoCaixa() {
        if (txtCaixaSelecionado == null) return;
        txtCaixaSelecionado.setText(
                nomeCaixa != null && !nomeCaixa.isEmpty() ? nomeCaixa : "—");
    }

    private void abrirDialogAlterarCaixa() {
        if (salvandoVenda) return;
        long saleMillis = dataEscolhida != null
                ? dataEscolhida.getTimeInMillis()
                : System.currentTimeMillis();

        caixaRepository.listarTodosCaixasHistorico(new CaixaRepository.ListaCaixaCallback() {
            @Override
            public void onCaixas(java.util.List<CaixaModel> lista) {
                // Filtra caixas compatíveis com o horário da venda
                java.util.List<CaixaModel> compativeis = new java.util.ArrayList<>();
                for (CaixaModel c : lista) {
                    if (caixaEhCompativel(c, saleMillis)) compativeis.add(c);
                }

                if (compativeis.isEmpty()) {
                    Toast.makeText(FechamentoVendaActivity.this,
                            "Nenhum caixa compatível com o horário desta venda.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String[] opcoes = new String[compativeis.size()];
                for (int i = 0; i < compativeis.size(); i++) {
                    CaixaModel c = compativeis.get(i);
                    opcoes[i] = c.getNomeCaixa()
                            + (c.isAberto() ? "  (aberto)" : "  (fechado)");
                }

                new AlertDialog.Builder(FechamentoVendaActivity.this)
                        .setTitle("Selecionar caixa")
                        .setItems(opcoes, (dialog, which) -> {
                            CaixaModel escolhido = compativeis.get(which);
                            caixaId   = escolhido.getId();
                            nomeCaixa = escolhido.getNomeCaixa();
                            atualizarExibicaoCaixa();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao carregar caixas: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Um caixa é compatível se o horário da venda estiver dentro do intervalo de operação:
     * abertura ≤ saleMillis ≤ fechamento (ou sem limite superior se ainda aberto).
     */
    private boolean caixaEhCompativel(CaixaModel caixa, long saleMillis) {
        if (caixa.isLegado()) return false;
        if (saleMillis < caixa.getAbertoEmMillis()) return false;
        if (caixa.isFechado() && caixa.getFechadoEmMillis() > 0
                && saleMillis > caixa.getFechadoEmMillis()) return false;
        return true;
    }

    // ----------------------------------------------------------------
    // Pagamento
    // ----------------------------------------------------------------

    private void selecionarFormaPagamento(String formaPagamento) {
        if (salvandoVenda) return;
        if (formaPagamento.equals(formaPagamentoSelecionada) && pagamentosRecolhidos) {
            pagamentosRecolhidos = false;
        } else {
            formaPagamentoSelecionada = formaPagamento;
            pagamentosRecolhidos = true;
        }
        atualizarEstadoPagamento();
        atualizarBotaoFinalizar();
    }

    private void atualizarEstadoPagamento() {
        atualizarCardPagamento(cardPagamentoPix,      VendaModel.PAGAMENTO_PIX.equals(formaPagamentoSelecionada), pagamentosRecolhidos);
        atualizarCardPagamento(cardPagamentoDinheiro, VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada), pagamentosRecolhidos);
        atualizarCardPagamento(cardPagamentoDebito,   VendaModel.PAGAMENTO_DEBITO.equals(formaPagamentoSelecionada), pagamentosRecolhidos);
        atualizarCardPagamento(cardPagamentoCredito,  VendaModel.PAGAMENTO_CREDITO.equals(formaPagamentoSelecionada), pagamentosRecolhidos);

        boolean pagamentoEmDinheiro = VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada);
        if (layoutPagamentoDinheiro != null) {
            layoutPagamentoDinheiro.setVisibility(pagamentoEmDinheiro ? View.VISIBLE : View.GONE);
        }
        atualizarTrocoDinheiro();

        if (txtFormaPagamentoSelecionada != null) {
            txtFormaPagamentoSelecionada.setText(formaPagamentoSelecionada == null
                    ? "Selecione uma forma de pagamento"
                    : pagamentosRecolhidos
                    ? "Pagamento selecionado: " + formaPagamentoSelecionada + " (toque para alterar)"
                    : "Escolha outra forma de pagamento ou toque novamente na atual");
        }
    }

    private void atualizarCardPagamento(LinearLayout card, boolean selecionado, boolean recolhido) {
        if (card == null) return;
        card.setVisibility(recolhido && !selecionado ? View.GONE : View.VISIBLE);
        card.setAlpha(selecionado ? 1f : 0.75f);
        card.setBackgroundResource(selecionado
                ? R.drawable.bg_pagamento_selecionado
                : R.drawable.fundo_arredondado);
    }

    private void atualizarTrocoDinheiro() {
        if (txtTrocoDinheiro == null && txtErroValorRecebido == null) return;

        boolean pagamentoEmDinheiro = VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada);
        double valorRecebido = obterValorRecebidoDinheiro();
        boolean valorRecebidoInformado = valorRecebidoDinheiroInformado();
        double troco = valorRecebidoInformado ? Math.max(0.0, valorRecebido - valorTotal) : 0.0;
        boolean insuficiente = pagamentoEmDinheiro
                && valorRecebidoInformado
                && valorRecebido + 0.001 < valorTotal;

        if (txtTrocoDinheiro != null) {
            txtTrocoDinheiro.setText(formatadorMoeda.format(troco));
        }
        if (txtErroValorRecebido != null) {
            txtErroValorRecebido.setVisibility(insuficiente ? View.VISIBLE : View.GONE);
        }
    }

    private double obterValorRecebidoDinheiro() {
        return valorRecebidoWatcher != null ? valorRecebidoWatcher.getValorDouble() : 0.0;
    }

    private boolean valorRecebidoDinheiroInformado() {
        return obterValorRecebidoDinheiro() > 0.001;
    }

    private boolean dinheiroValido() {
        if (!VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada)) return true;
        if (!valorRecebidoDinheiroInformado()) return true;
        return obterValorRecebidoDinheiro() + 0.001 >= valorTotal;
    }

    private void atualizarBotaoFinalizar() {
        if (btnFinalizarVenda == null) return;
        boolean habilitado = !listaItens.isEmpty()
                && formaPagamentoSelecionada != null
                && dinheiroValido()
                && !salvandoVenda;
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
        if (!dinheiroValido()) {
            atualizarTrocoDinheiro();
            Toast.makeText(this, "Valor recebido em dinheiro insuficiente.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dataEscolhida != null && dataEscolhida.getTimeInMillis() > System.currentTimeMillis()) {
            Toast.makeText(this, "A data e hora da venda nao podem ficar no futuro.", Toast.LENGTH_SHORT).show();
            return;
        }

        salvandoVenda = true;
        atualizarBotaoFinalizar();

        garantirCaixaAntesDeSalvar(new Runnable() {
            @Override
            public void run() {
                validarCaixaSelecionadoAntesDeSalvar(() ->
                        vendaRepository.salvar(montarVendaParaSalvar(), new VendaRepository.Callback() {
                            @Override
                            public void onSucesso(String vendaId) {
                                salvandoVenda = false;
                                atualizarBotaoFinalizar();
                                atualizarUltimaCompraCliente();
                                abrirComprovante(vendaId);
                            }

                            @Override
                            public void onErro(String erro) {
                                salvandoVenda = false;
                                atualizarBotaoFinalizar();
                                Toast.makeText(FechamentoVendaActivity.this,
                                        "Erro ao salvar venda: " + erro, Toast.LENGTH_LONG).show();
                            }
                        }));
            }
        });
    }

    private void validarCaixaSelecionadoAntesDeSalvar(@NonNull Runnable onValido) {
        if (caixaId == null || caixaId.trim().isEmpty()) {
            onValido.run();
            return;
        }

        long vendaMillis = dataEscolhida != null
                ? dataEscolhida.getTimeInMillis()
                : System.currentTimeMillis();

        caixaRepository.buscarCaixaPorId(caixaId, new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel caixa) {
                if (caixa == null || !caixaEhCompativel(caixa, vendaMillis)) {
                    salvandoVenda = false;
                    atualizarBotaoFinalizar();
                    Toast.makeText(FechamentoVendaActivity.this,
                            "Selecione um caixa compatível com a data e hora da venda.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                nomeCaixa = caixa.getNomeCaixa();
                atualizarExibicaoCaixa();
                onValido.run();
            }

            @Override
            public void onErro(String erro) {
                salvandoVenda = false;
                atualizarBotaoFinalizar();
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao validar caixa da venda: " + erro,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void garantirCaixaAntesDeSalvar(Runnable onCaixaResolvido) {
        if (modoEdicao) {
            onCaixaResolvido.run();
            return;
        }

        if (caixaId != null && !caixaId.trim().isEmpty()) {
            onCaixaResolvido.run();
            return;
        }

        caixaRepository.buscarCaixaAberto(new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel caixa) {
                if (caixa == null || !caixa.isAberto()) {
                    salvandoVenda = false;
                    atualizarBotaoFinalizar();
                    Toast.makeText(FechamentoVendaActivity.this,
                            "Nenhum caixa aberto encontrado para vincular esta venda.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                caixaId = caixa.getId();
                nomeCaixa = caixa.getNomeCaixa();
                atualizarExibicaoCaixa();

                onCaixaResolvido.run();
            }

            @Override
            public void onErro(String erro) {
                salvandoVenda = false;
                atualizarBotaoFinalizar();
                Toast.makeText(FechamentoVendaActivity.this,
                        "Erro ao identificar caixa da venda: " + erro,
                        Toast.LENGTH_LONG).show();
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
        venda.setQuantidadeTotal(quantidadeTotal);
        venda.setValorTotal(valorTotal);
        if (VendaModel.PAGAMENTO_DINHEIRO.equals(formaPagamentoSelecionada)) {
            double valorRecebido = obterValorRecebidoDinheiro();
            if (valorRecebidoDinheiroInformado()) {
                venda.setValorRecebidoDinheiro(valorRecebido);
                venda.setTrocoDinheiro(Math.max(0.0, valorRecebido - valorTotal));
            } else {
                venda.setValorRecebidoDinheiro(0.0);
                venda.setTrocoDinheiro(0.0);
            }
        } else {
            venda.setValorRecebidoDinheiro(0.0);
            venda.setTrocoDinheiro(0.0);
        }
        venda.setStatus(VendaModel.STATUS_FINALIZADA);
        venda.setItens(converterItensParaVenda(listaItens));
        venda.setCaixaId(caixaId);     // associa ao caixa aberto (null = legado)
        venda.setNomeCaixa(nomeCaixa); // nome legível desnormalizado
        aplicarClienteEVendedor(venda);

        return venda;
    }

    private void abrirComprovante(String vendaId) {
        Intent intent = new Intent(this, ComprovanteVendaActivity.class);
        intent.putExtra("vendaId", vendaId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void aplicarClienteEVendedor(VendaModel venda) {
        venda.setClienteId(clienteId);
        venda.setClienteNome(clienteNome);
        venda.setClienteTelefone(clienteTelefone);
        venda.setVendedorId(vendedorId);
        venda.setVendedorNome(vendedorNome);
        venda.setVendedorEmail(vendedorEmail);
    }

    private void atualizarUltimaCompraCliente() {
        if (clienteId == null || clienteId.trim().isEmpty()) return;
        vendasRepository.atualizarUltimaCompraCliente(clienteId, Timestamp.now(), new RepoVoidCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });
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
        if (numeroVendaEdicao > 0) venda.setNumeroVenda(numeroVendaEdicao);
        venda.setDataHoraAberturaMillis(System.currentTimeMillis());
        venda.setDataHoraFechamentoMillis(0);
        venda.setFormaPagamento(null);
        venda.setQuantidadeTotal(quantidadeTotal);
        venda.setValorTotal(valorTotal);
        venda.setStatus(VendaModel.STATUS_EM_ABERTO);
        venda.setItens(converterItensParaVenda(listaItens));
        venda.setCaixaId(caixaId);
        venda.setNomeCaixa(nomeCaixa);
        aplicarClienteEVendedor(venda);

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

    private void voltarParaNovaVenda() {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
