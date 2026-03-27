package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;

import java.util.ArrayList;
import java.util.List;

public class HistoricoVendasActivity extends AppCompatActivity {

    private ImageButton btnVoltarHistorico;
    private RecyclerView rvHistoricoVendas;
    private View layoutEstadoVazioHistorico;
    private TextView txtQuantidadeVendas;

    private final List<VendaModel> listaVendas = new ArrayList<>();
    private AdapterHistoricoVendas adapter;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;
    private TextView chipTodas;
    private TextView chipFinalizadas;
    private TextView chipCanceladas;
    private final List<VendaModel> listaCompleta = new ArrayList<>();
    private String filtroAtivo = null;

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
        btnVoltarHistorico = findViewById(R.id.btnVoltarHistorico);
        rvHistoricoVendas = findViewById(R.id.rvHistoricoVendas);
        layoutEstadoVazioHistorico = findViewById(R.id.layoutEstadoVazioHistorico);
        txtQuantidadeVendas = findViewById(R.id.txtQuantidadeVendas);
        chipTodas       = findViewById(R.id.chipTodas);
        chipFinalizadas = findViewById(R.id.chipFinalizadas);
        chipCanceladas  = findViewById(R.id.chipCanceladas);
    }

    private void configurarRecyclerView() {
        rvHistoricoVendas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterHistoricoVendas(listaVendas, venda ->
                abrirComprovante(venda.getId()));
        rvHistoricoVendas.setAdapter(adapter);
    }
    private void abrirComprovante(String vendaId) {
        Intent intent = new Intent(this, ComprovanteVendaActivity.class);
        intent.putExtra("vendaId", vendaId);
        startActivity(intent);
    }

    private void configurarAcoes() {
        if (btnVoltarHistorico != null) {
            btnVoltarHistorico.setOnClickListener(v -> finish());
        }
        if (chipTodas != null) chipTodas.setOnClickListener(v -> aplicarFiltro(null));
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
                Toast.makeText(
                        HistoricoVendasActivity.this,
                        "Erro ao carregar histórico: " + erro,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void atualizarEstadoTela() {
        boolean vazio = listaVendas.isEmpty();

        if (layoutEstadoVazioHistorico != null) {
            layoutEstadoVazioHistorico.setVisibility(vazio ? View.VISIBLE : View.GONE);
        }

        if (rvHistoricoVendas != null) {
            rvHistoricoVendas.setVisibility(vazio ? View.GONE : View.VISIBLE);
        }

        if (txtQuantidadeVendas != null) {
            txtQuantidadeVendas.setText(
                    vazio
                            ? "Nenhuma venda encontrada"
                            : listaVendas.size() + (listaVendas.size() == 1 ? " venda" : " vendas")
            );
        }
    }

    private void aplicarFiltro(String status) {
        filtroAtivo = status;

        listaVendas.clear();
        if (status == null) {
            // "Todas" — exclui apenas EM_ABERTO (essas ficam na tela de Vendas em Aberto)
            for (VendaModel v : listaCompleta) {
                if (!VendaModel.STATUS_EM_ABERTO.equals(v.getStatus())) {
                    listaVendas.add(v);
                }
            }
        } else {
            for (VendaModel v : listaCompleta) {
                if (status.equals(v.getStatus())) {
                    listaVendas.add(v);
                }
            }
        }

        adapter.atualizarLista(listaVendas);
        atualizarEstadoTela();
        atualizarEstiloChips();
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