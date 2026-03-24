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
    }

    private void configurarRecyclerView() {
        rvHistoricoVendas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterHistoricoVendas(listaVendas);
        rvHistoricoVendas.setAdapter(adapter);
    }

    private void configurarAcoes() {
        if (btnVoltarHistorico != null) {
            btnVoltarHistorico.setOnClickListener(v -> finish());
        }
    }

    private void carregarHistorico() {
        listenerRegistration = vendaRepository.listarTempoReal(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaVendas.clear();
                if (lista != null) {
                    listaVendas.addAll(lista);
                }

                adapter.atualizarLista(listaVendas);
                atualizarEstadoTela();
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
}