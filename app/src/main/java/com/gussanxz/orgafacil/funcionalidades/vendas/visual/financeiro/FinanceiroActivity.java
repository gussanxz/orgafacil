package com.gussanxz.orgafacil.funcionalidades.vendas.visual.financeiro;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HeaderDiaVenda;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinanceiroActivity extends AppCompatActivity {

    // Views
    private ImageButton btnVoltar;
    private TextView txtTotalVendido;
    private TextView chipTodasF, chipFinalizadas, chipCanceladas;
    private Spinner spinnerPagamento, spinnerTipoItem;
    private TextView btnDataInicial, btnDataFinal;
    private ImageButton btnLimparFiltros;
    private androidx.cardview.widget.CardView cardHeaderDiaCongelado;
    private TextView txtHeaderDiaCongelado, txtTotalDiaCongelado;
    private RecyclerView rvFinanceiro;
    private View layoutEstadoVazio;

    // Estado
    private final List<VendaModel> listaCompleta = new ArrayList<>();
    private final List<Object> listaItens = new ArrayList<>();
    private String filtroStatus = null;
    private String filtroPagamento = null; // null = todos
    private String filtroTipoItem = null;  // null = todos
    private Long dataInicialMs = null;
    private Long dataFinalMs = null;

    private AdapterFinanceiro adapter;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;

    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat fmtDia  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat fmtExib = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final SimpleDateFormat fmtLabel = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_financeiro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootFinanceiro), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        vendaRepository = new VendaRepository();
        inicializarComponentes();
        configurarSpinners();
        configurarRecyclerView();
        configurarAcoes();
        configurarScrollSticky();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenerRegistration = vendaRepository.listarTempoReal(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaCompleta.clear();
                if (lista != null) listaCompleta.addAll(lista);
                aplicarFiltros();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(FinanceiroActivity.this,
                        "Erro: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void inicializarComponentes() {
        btnVoltar               = findViewById(R.id.btnVoltarFinanceiro);
        txtTotalVendido         = findViewById(R.id.txtTotalVendido);
        chipTodasF              = findViewById(R.id.chipFinTodasF);
        chipFinalizadas         = findViewById(R.id.chipFinFinalizadas);
        chipCanceladas          = findViewById(R.id.chipFinCanceladas);
        spinnerPagamento        = findViewById(R.id.spinnerPagamento);
        spinnerTipoItem         = findViewById(R.id.spinnerTipoItem);
        btnDataInicial          = findViewById(R.id.btnDataInicial);
        btnDataFinal            = findViewById(R.id.btnDataFinal);
        btnLimparFiltros        = findViewById(R.id.btnLimparFiltros);
        cardHeaderDiaCongelado  = findViewById(R.id.cardHeaderDiaCongelado);
        txtHeaderDiaCongelado   = findViewById(R.id.txtHeaderDiaCongelado);
        txtTotalDiaCongelado    = findViewById(R.id.txtTotalDiaCongelado);
        rvFinanceiro            = findViewById(R.id.rvFinanceiro);
        layoutEstadoVazio       = findViewById(R.id.layoutEstadoVazioFinanceiro);
    }

    private void configurarSpinners() {
        // Forma de pagamento
        List<String> pagamentos = new ArrayList<>();
        pagamentos.add("Pagamento: Todos");
        pagamentos.add(VendaModel.PAGAMENTO_PIX);
        pagamentos.add(VendaModel.PAGAMENTO_DINHEIRO);
        pagamentos.add(VendaModel.PAGAMENTO_CREDITO);
        pagamentos.add(VendaModel.PAGAMENTO_DEBITO);

        ArrayAdapter<String> adPag = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, pagamentos);
        adPag.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPagamento.setAdapter(adPag);
        spinnerPagamento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filtroPagamento = pos == 0 ? null : pagamentos.get(pos);
                aplicarFiltros();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Tipo de item
        List<String> tipos = new ArrayList<>();
        tipos.add("Tipo: Todos");
        tipos.add("Produto");
        tipos.add("Serviço");

        ArrayAdapter<String> adTipo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tipos);
        adTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoItem.setAdapter(adTipo);
        spinnerTipoItem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filtroTipoItem = pos == 0 ? null : tipos.get(pos);
                aplicarFiltros();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void configurarRecyclerView() {
        rvFinanceiro.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterFinanceiro(listaItens);
        rvFinanceiro.setAdapter(adapter);
    }

    private void configurarAcoes() {
        btnVoltar.setOnClickListener(v -> finish());

        chipTodasF.setOnClickListener(v    -> { filtroStatus = null; atualizarChips(); aplicarFiltros(); });
        chipFinalizadas.setOnClickListener(v -> { filtroStatus = VendaModel.STATUS_FINALIZADA; atualizarChips(); aplicarFiltros(); });
        chipCanceladas.setOnClickListener(v  -> { filtroStatus = VendaModel.STATUS_CANCELADA;  atualizarChips(); aplicarFiltros(); });

        btnDataInicial.setOnClickListener(v -> abrirDatePicker(true));
        btnDataFinal.setOnClickListener(v   -> abrirDatePicker(false));

        btnLimparFiltros.setOnClickListener(v -> limparTodosFiltros());
    }

    private void configurarScrollSticky() {
        rvFinanceiro.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                atualizarHeaderCongelado();
            }
        });
    }

    private void atualizarHeaderCongelado() {
        LinearLayoutManager lm = (LinearLayoutManager) rvFinanceiro.getLayoutManager();
        if (lm == null) return;

        int firstVisible = lm.findFirstVisibleItemPosition();
        if (firstVisible < 0 || listaItens.isEmpty()) {
            cardHeaderDiaCongelado.setVisibility(View.GONE);
            return;
        }

        // Se o próprio item no topo já é um header, o congelado não precisa aparecer
        if (listaItens.get(firstVisible) instanceof HeaderDiaVenda) {
            cardHeaderDiaCongelado.setVisibility(View.GONE);
            return;
        }

        // Busca o header do grupo que cobre os itens visíveis
        for (int i = firstVisible - 1; i >= 0; i--) {
            Object item = listaItens.get(i);
            if (item instanceof HeaderDiaVenda) {
                HeaderDiaVenda h = (HeaderDiaVenda) item;
                cardHeaderDiaCongelado.setVisibility(View.VISIBLE);
                txtHeaderDiaCongelado.setText(h.titulo);
                txtTotalDiaCongelado.setText(fmt.format(h.totalDia));
                return;
            }
        }

        cardHeaderDiaCongelado.setVisibility(View.GONE);
    }

    private void abrirDatePicker(boolean isInicial) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day);
                    if (isInicial) {
                        sel.set(Calendar.HOUR_OF_DAY, 0);
                        sel.set(Calendar.MINUTE, 0);
                        sel.set(Calendar.SECOND, 0);
                        dataInicialMs = sel.getTimeInMillis();
                        btnDataInicial.setText(fmtExib.format(sel.getTime()));
                        btnDataInicial.setTextColor(getColor(R.color.colorPrimary));
                    } else {
                        sel.set(Calendar.HOUR_OF_DAY, 23);
                        sel.set(Calendar.MINUTE, 59);
                        sel.set(Calendar.SECOND, 59);
                        dataFinalMs = sel.getTimeInMillis();
                        btnDataFinal.setText(fmtExib.format(sel.getTime()));
                        btnDataFinal.setTextColor(getColor(R.color.colorPrimary));
                    }
                    aplicarFiltros();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void limparTodosFiltros() {
        filtroStatus    = null;
        filtroPagamento = null;
        filtroTipoItem  = null;
        dataInicialMs   = null;
        dataFinalMs     = null;

        spinnerPagamento.setSelection(0);
        spinnerTipoItem.setSelection(0);
        btnDataInicial.setText("Data inicial");
        btnDataInicial.setTextColor(getColor(android.R.color.darker_gray));
        btnDataFinal.setText("Data final");
        btnDataFinal.setTextColor(getColor(android.R.color.darker_gray));

        atualizarChips();
        aplicarFiltros();
    }

    private boolean temFiltroAtivo() {
        return filtroStatus    != null
                || filtroPagamento != null
                || filtroTipoItem  != null
                || dataInicialMs   != null
                || dataFinalMs     != null;
    }

    private void aplicarFiltros() {
        btnLimparFiltros.setVisibility(temFiltroAtivo() ? View.VISIBLE : View.GONE);

        // 1. Filtra vendas
        List<VendaModel> filtradas = new ArrayList<>();
        for (VendaModel v : listaCompleta) {
            if (VendaModel.STATUS_EM_ABERTO.equals(v.getStatus())) continue;

            // Filtro status
            if (filtroStatus != null && !filtroStatus.equals(v.getStatus())) continue;

            // Filtro pagamento
            if (filtroPagamento != null && !filtroPagamento.equals(v.getFormaPagamento())) continue;

            // Filtro tipo de item (produto/serviço)
            if (filtroTipoItem != null) {
                int tipoEsperado = "Produto".equals(filtroTipoItem)
                        ? ItemVendaRegistradaModel.TIPO_PRODUTO
                        : ItemVendaRegistradaModel.TIPO_SERVICO;
                boolean temTipo = false;
                if (v.getItens() != null) {
                    for (ItemVendaRegistradaModel item : v.getItens()) {
                        if (item.getTipo() == tipoEsperado) { temTipo = true; break; }
                    }
                }
                if (!temTipo) continue;
            }

            // Filtro datas
            long ts = v.getDataHoraFechamentoMillis() > 0
                    ? v.getDataHoraFechamentoMillis()
                    : v.getDataHoraAberturaMillis();
            if (dataInicialMs != null && ts < dataInicialMs) continue;
            if (dataFinalMs   != null && ts > dataFinalMs)   continue;

            filtradas.add(v);
        }

        // 2. Calcula total geral
        double totalGeral = 0;
        for (VendaModel v : filtradas) {
            if (VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) {
                totalGeral += v.getValorTotal();
            }
        }
        txtTotalVendido.setText("Total: " + fmt.format(totalGeral));

        // 3. Agrupa por dia
        listaItens.clear();
        String diaHoje  = fmtDia.format(new Date());
        String diaOntem = fmtDia.format(new Date(System.currentTimeMillis() - 86_400_000L));

        LinkedHashMap<String, List<VendaModel>> porDia = new LinkedHashMap<>();
        for (VendaModel v : filtradas) {
            long ts = v.getDataHoraFechamentoMillis() > 0
                    ? v.getDataHoraFechamentoMillis()
                    : v.getDataHoraAberturaMillis();
            String chave = fmtDia.format(new Date(ts));
            if (!porDia.containsKey(chave)) porDia.put(chave, new ArrayList<>());
            porDia.get(chave).add(v);
        }

        for (Map.Entry<String, List<VendaModel>> entrada : porDia.entrySet()) {
            String chave = entrada.getKey();
            List<VendaModel> vendasDoDia = entrada.getValue();

            double totalDia = 0;
            int qtdFin = 0, qtdCan = 0;
            for (VendaModel v : vendasDoDia) {
                if (VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) { totalDia += v.getValorTotal(); qtdFin++; }
                else if (VendaModel.STATUS_CANCELADA.equals(v.getStatus())) qtdCan++;
            }

            String label;
            try {
                Date dataRef = fmtDia.parse(chave);
                if (chave.equals(diaHoje))       label = "Hoje";
                else if (chave.equals(diaOntem)) label = "Ontem";
                else                             label = fmtLabel.format(dataRef);
            } catch (ParseException e) { label = chave; }

            listaItens.add(new HeaderDiaVenda(label, totalDia, qtdFin, qtdCan));
            listaItens.addAll(vendasDoDia);
        }

        adapter.atualizarLista(listaItens);
        atualizarEstadoTela();
        atualizarHeaderCongelado();
    }

    private void atualizarEstadoTela() {
        long qtd = listaItens.stream().filter(o -> o instanceof VendaModel).count();
        layoutEstadoVazio.setVisibility(qtd == 0 ? View.VISIBLE : View.GONE);
        rvFinanceiro.setVisibility(qtd == 0 ? View.GONE : View.VISIBLE);
    }

    private void atualizarChips() {
        atualizarEstiloChip(chipTodasF,      filtroStatus == null);
        atualizarEstiloChip(chipFinalizadas, VendaModel.STATUS_FINALIZADA.equals(filtroStatus));
        atualizarEstiloChip(chipCanceladas,  VendaModel.STATUS_CANCELADA.equals(filtroStatus));
    }

    private void atualizarEstiloChip(TextView chip, boolean selecionado) {
        if (chip == null) return;
        chip.setBackgroundTintList(getColorStateList(
                selecionado ? R.color.colorPrimary : android.R.color.white));
        chip.setTextColor(selecionado
                ? android.graphics.Color.WHITE
                : android.graphics.Color.parseColor("#757575"));
    }
}