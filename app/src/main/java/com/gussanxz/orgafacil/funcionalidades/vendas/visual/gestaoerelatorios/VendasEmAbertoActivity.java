package com.gussanxz.orgafacil.funcionalidades.vendas.visual.gestaoerelatorios;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.FechamentoVendaActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.RegistrarVendasActivity;

import java.util.ArrayList;
import java.util.List;

public class VendasEmAbertoActivity extends AppCompatActivity {

    private ImageButton btnVoltar;
    private RecyclerView rvVendasEmAberto;
    private View layoutEstadoVazio;
    private TextView txtQuantidade;

    private final List<VendaModel> listaVendas = new ArrayList<>();
    private AdapterVendasEmAberto adapter;
    private VendaRepository vendaRepository;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_em_aberto);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootVendasEmAberto), (v, insets) -> {
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
        carregarVendasEmAberto();
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
        btnVoltar        = findViewById(R.id.btnVoltarEmAberto);
        rvVendasEmAberto = findViewById(R.id.rvVendasEmAberto);
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazioEmAberto);
        txtQuantidade    = findViewById(R.id.txtQuantidadeEmAberto);
    }

    private void configurarRecyclerView() {
        rvVendasEmAberto.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterVendasEmAberto(listaVendas, new AdapterVendasEmAberto.OnVendaActionListener() {
            @Override
            public void onEditar(VendaModel venda) {
                abrirEdicao(venda);
            }

            @Override
            public void onCancelar(VendaModel venda) {
                confirmarCancelamento(venda);
            }
        });
        rvVendasEmAberto.setAdapter(adapter);
    }

    private void abrirEdicao(VendaModel venda) {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        intent.putExtra("itensSacola", new ArrayList<>(
                converterItensRegistradosParaSacola(venda.getItens())));
        intent.putExtra("vendaId", venda.getId());
        intent.putExtra("numeroVenda", venda.getNumeroVenda());
        startActivity(intent);
    }

    private List<ItemSacolaVendaModel> converterItensRegistradosParaSacola(
            List<ItemVendaRegistradaModel> itens) {
        // ItemVendaRegistradaModel → ItemSacolaVendaModel
        // precisa de um construtor auxiliar — ver abaixo
        List<ItemSacolaVendaModel> sacola = new ArrayList<>();
        if (itens != null) {
            for (ItemVendaRegistradaModel item : itens) {
                sacola.add(new ItemSacolaVendaModel((ItemVendaModel) item));
            }
        }
        return sacola;
    }

    private void configurarAcoes() {
        if (btnVoltar != null) {
            btnVoltar.setOnClickListener(v -> finish());
        }
    }

    private void carregarVendasEmAberto() {
        listenerRegistration = vendaRepository.listarEmAberto(new VendaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<VendaModel> lista) {
                listaVendas.clear();
                if (lista != null) listaVendas.addAll(lista);
                adapter.atualizarLista(listaVendas);
                atualizarEstadoTela();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(VendasEmAbertoActivity.this,
                        "Erro ao carregar vendas: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmarCancelamento(VendaModel venda) {
        String numero = venda.getNumeroVenda() > 0
                ? String.format(java.util.Locale.ROOT, "#%07d", venda.getNumeroVenda())
                : venda.getId().substring(0, 8).toUpperCase();

        new AlertDialog.Builder(this)
                .setTitle("Cancelar venda")
                .setMessage("Deseja cancelar a venda " + numero + "? Essa ação não pode ser desfeita.")
                .setPositiveButton("Cancelar venda", (dialog, which) -> cancelarVenda(venda))
                .setNegativeButton("Voltar", null)
                .show();
    }

    private void cancelarVenda(VendaModel venda) {
        vendaRepository.atualizarStatus(venda.getId(), VendaModel.STATUS_CANCELADA,
                new VendaRepository.Callback() {
                    @Override
                    public void onSucesso(String vendaId) {
                        Toast.makeText(VendasEmAbertoActivity.this,
                                "Venda cancelada.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(VendasEmAbertoActivity.this,
                                "Erro ao cancelar: " + erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void atualizarEstadoTela() {
        boolean vazio = listaVendas.isEmpty();

        if (layoutEstadoVazio != null)
            layoutEstadoVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);

        if (rvVendasEmAberto != null)
            rvVendasEmAberto.setVisibility(vazio ? View.GONE : View.VISIBLE);

        if (txtQuantidade != null) {
            txtQuantidade.setText(vazio
                    ? "Nenhuma venda em aberto"
                    : listaVendas.size() + (listaVendas.size() == 1 ? " venda em aberto" : " vendas em aberto"));
        }
    }
}