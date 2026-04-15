package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.ComprovanteVendaActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.RegistrarVendasActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoricoVendasActivity extends AppCompatActivity {

    private ImageButton btnVoltarHistorico;
    private RecyclerView rvHistoricoVendas;
    private View layoutEstadoVazioHistorico;
    private TextView txtQuantidadeVendas;
    private TextView chipTodas;
    private TextView chipFinalizadas;
    private TextView chipCanceladas;

    private final List<Object> listaItens = new ArrayList<>();
    private final List<VendaModel> listaCompleta = new ArrayList<>();
    private String filtroAtivo = null;

    private AdapterHistoricoVendas adapter;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_historico_vendas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootHistoricoVendas), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vendaRepository = new VendaRepository();

        inicializarComponentes();
        configurarRecyclerView();
        configurarAcoes();
        atualizarEstadoTela();
    }

    @Override
    protected void onStart() {
        super.onStart();
        carregarHistorico();
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
        btnVoltarHistorico         = findViewById(R.id.btnVoltarHistorico);
        rvHistoricoVendas          = findViewById(R.id.rvHistoricoVendas);
        layoutEstadoVazioHistorico = findViewById(R.id.layoutEstadoVazioHistorico);
        txtQuantidadeVendas        = findViewById(R.id.txtQuantidadeVendas);
        chipTodas                  = findViewById(R.id.chipTodas);
        chipFinalizadas            = findViewById(R.id.chipFinalizadas);
        chipCanceladas             = findViewById(R.id.chipCanceladas);
    }

    private void configurarRecyclerView() {
        rvHistoricoVendas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterHistoricoVendas(
                listaItens,
                venda -> abrirComprovante(venda.getId()),
                venda -> abrirEdicao(venda),
                venda -> confirmarAlteracaoStatus(venda),
                header -> exibirResumoDia(header)
        );
        rvHistoricoVendas.setAdapter(adapter);
    }

    private void configurarAcoes() {
        if (btnVoltarHistorico != null) {
            btnVoltarHistorico.setOnClickListener(v -> finish());
        }
        if (chipTodas != null)       chipTodas.setOnClickListener(v -> aplicarFiltro(null));
        if (chipFinalizadas != null) chipFinalizadas.setOnClickListener(v -> aplicarFiltro(VendaModel.STATUS_FINALIZADA));
        if (chipCanceladas != null)  chipCanceladas.setOnClickListener(v -> aplicarFiltro(VendaModel.STATUS_CANCELADA));
        atualizarEstiloChips();
    }

    private void carregarHistorico() {
        listenerRegistration = vendaRepository.listarTempoReal(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaCompleta.clear();
                if (lista != null) listaCompleta.addAll(lista);
                aplicarFiltro(filtroAtivo);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(HistoricoVendasActivity.this,
                        "Erro ao carregar histórico: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void atualizarEstadoTela() {
        long qtdVendas = listaItens.stream()
                .filter(o -> o instanceof VendaModel)
                .count();
        boolean vazio = qtdVendas == 0;

        if (layoutEstadoVazioHistorico != null)
            layoutEstadoVazioHistorico.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (rvHistoricoVendas != null)
            rvHistoricoVendas.setVisibility(vazio ? View.GONE : View.VISIBLE);
        if (txtQuantidadeVendas != null)
            txtQuantidadeVendas.setText(vazio
                    ? "Nenhuma venda encontrada"
                    : qtdVendas + (qtdVendas == 1 ? " venda" : " vendas"));
    }

    private void aplicarFiltro(String status) {
        filtroAtivo = status;

        // 1. Filtra as vendas
        List<VendaModel> filtradas = new ArrayList<>();
        for (VendaModel v : listaCompleta) {
            if (VendaModel.STATUS_EM_ABERTO.equals(v.getStatus())) continue;
            if (status != null && !status.equals(v.getStatus())) continue;
            filtradas.add(v);
        }

        // 2. Agrupa por dia
        listaItens.clear();
        SimpleDateFormat fmtDia  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat fmtExib = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
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

            int totalDia = 0;
            int qtdDia = 0;
            int qtdCanceladas = 0;

            for (VendaModel v : vendasDoDia) {
                if (VendaModel.STATUS_FINALIZADA.equals(v.getStatus())) {
                    totalDia += v.getValorTotal(); // Agora é INT + INT
                    qtdDia++;
                } else if (VendaModel.STATUS_CANCELADA.equals(v.getStatus())) {
                    qtdCanceladas++;
                }
            }

            String label;
            try {
                Date dataRef = fmtDia.parse(chave);
                if (chave.equals(diaHoje))       label = "Hoje";
                else if (chave.equals(diaOntem)) label = "Ontem";
                else                             label = fmtExib.format(dataRef);
            } catch (Exception e) { label = chave; }

            listaItens.add(new HeaderDiaVenda(label, totalDia, qtdDia, qtdCanceladas));
            listaItens.addAll(vendasDoDia);
        }

        adapter.atualizarLista(listaItens);
        atualizarEstadoTela();
        atualizarEstiloChips();
    }

    private void abrirComprovante(String vendaId) {
        Intent intent = new Intent(this, ComprovanteVendaActivity.class);
        intent.putExtra("vendaId", vendaId);
        startActivity(intent);
    }

    private void abrirEdicao(VendaModel venda) {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        ArrayList<ItemSacolaVendaModel> sacola = new ArrayList<>();
        if (venda.getItens() != null) {
            for (ItemVendaRegistradaModel item : venda.getItens()) {
                sacola.add(new ItemSacolaVendaModel((ItemVendaModel) item));
            }
        }
        intent.putExtra("itensSacola", sacola);
        intent.putExtra("vendaId", venda.getId());
        // Extras adicionais para o FechamentoVendaActivity saber que é edição retroativa
        intent.putExtra("dataHoraOriginal",
                venda.getDataHoraFechamentoMillis() > 0
                        ? venda.getDataHoraFechamentoMillis()
                        : venda.getDataHoraAberturaMillis());
        intent.putExtra("formaPagamentoOriginal", venda.getFormaPagamento());
        startActivity(intent);
    }

    private void confirmarAlteracaoStatus(VendaModel venda) {
        boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
        String titulo  = finalizada ? "Cancelar venda"    : "Recuperar venda";
        String msg     = finalizada
                ? "Deseja cancelar esta venda? Ela continuará no histórico."
                : "Deseja marcar esta venda como finalizada novamente?";
        String btnText = finalizada ? "Cancelar venda" : "Recuperar";

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(msg)
                .setPositiveButton(btnText, (dialog, which) -> alternarStatus(venda))
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void alternarStatus(VendaModel venda) {
        vendaRepository.alternarStatus(venda, new VendaRepository.Callback() {
            @Override
            public void onSucesso(String vendaId) {
                Toast.makeText(HistoricoVendasActivity.this,
                        "Status atualizado.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(HistoricoVendasActivity.this,
                        "Erro ao atualizar: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exibirResumoDia(HeaderDiaVenda header) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_resumo_dia_vendas, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ((TextView) view.findViewById(R.id.txtResumoDiaTitulo))
                .setText("Resumo: " + header.titulo);
        ((TextView) view.findViewById(R.id.txtResumoQtdFinalizadas))
                .setText(String.valueOf(header.qtdVendasFinalizadas));
        ((TextView) view.findViewById(R.id.txtResumoQtdCanceladas))
                .setText(String.valueOf(header.qtdCanceladas));
        ((TextView) view.findViewById(R.id.txtResumoTotalDia))
                .setText(fmt.format(header.totalDia / 100.0));

        view.findViewById(R.id.btnFecharResumoDia).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void atualizarEstiloChips() {
        atualizarEstiloChip(chipTodas,       filtroAtivo == null);
        atualizarEstiloChip(chipFinalizadas, VendaModel.STATUS_FINALIZADA.equals(filtroAtivo));
        atualizarEstiloChip(chipCanceladas,  VendaModel.STATUS_CANCELADA.equals(filtroAtivo));
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