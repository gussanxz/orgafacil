package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FechamentoVendaActivity extends AppCompatActivity {

    private ImageButton btnVoltarFechamento;
    private TextView txtQuantidadeResumo;
    private TextView txtTotalResumo;
    private RecyclerView rvResumoItens;

    private AdapterResumoFechamentoVenda adapter;
    private final List<ItemSacolaVendaModel> listaItens = new ArrayList<>();
    private final NumberFormat formatadorMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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

        inicializarComponentes();
        configurarRecyclerView();
        configurarAcoes();
        carregarDadosRecebidos();
    }

    private void inicializarComponentes() {
        btnVoltarFechamento = findViewById(R.id.btnVoltarFechamento);
        txtQuantidadeResumo = findViewById(R.id.txtQuantidadeResumo);
        txtTotalResumo = findViewById(R.id.txtTotalResumo);
        rvResumoItens = findViewById(R.id.rvResumoItens);
    }

    private void configurarRecyclerView() {
        rvResumoItens.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterResumoFechamentoVenda(listaItens);
        rvResumoItens.setAdapter(adapter);
    }

    private void configurarAcoes() {
        if (btnVoltarFechamento != null) {
            btnVoltarFechamento.setOnClickListener(v -> finish());
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarDadosRecebidos() {
        ArrayList<ItemSacolaVendaModel> itensRecebidos =
                (ArrayList<ItemSacolaVendaModel>) getIntent().getSerializableExtra("itensSacola");

        int quantidadeTotal = getIntent().getIntExtra("quantidadeTotal", 0);
        double valorTotal = getIntent().getDoubleExtra("valorTotal", 0.0);

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
}