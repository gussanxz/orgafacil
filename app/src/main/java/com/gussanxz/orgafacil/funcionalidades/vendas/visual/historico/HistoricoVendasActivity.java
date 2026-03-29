package com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico;

import static com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasDialogHelper.confirmarExclusao;

import android.content.Intent;
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
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaRegistradaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.ComprovanteVendaActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.RegistrarVendasActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoricoVendasActivity extends AppCompatActivity {

    private ImageButton btnVoltarHistorico;
    private RecyclerView rvHistoricoVendas;
    private View layoutEstadoVazioHistorico;
    private TextView txtQuantidadeVendas;

    private final List<Object> listaItens = new ArrayList<>();
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
        adapter = new AdapterHistoricoVendas(
                listaItens,
                venda -> abrirComprovante(venda.getId()),
                venda -> abrirEdicao(venda),
                venda -> confirmarAlteracaoStatus(venda)
        );
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

        // 2. Agrupa por dia montando lista mista com headers
        listaItens.clear();
        SimpleDateFormat fmtDia  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat fmtExib = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
        String diaHoje = fmtDia.format(new Date());
        String diaOntem = fmtDia.format(new Date(System.currentTimeMillis() - 86400000L));

        String ultimoDia = null;
        for (VendaModel v : filtradas) {
            long ts = v.getDataHoraFechamentoMillis() > 0
                    ? v.getDataHoraFechamentoMillis()
                    : v.getDataHoraAberturaMillis();
            String diaVenda = fmtDia.format(new Date(ts));

            if (!diaVenda.equals(ultimoDia)) {
                String label;
                if (diaVenda.equals(diaHoje))   label = "Hoje";
                else if (diaVenda.equals(diaOntem)) label = "Ontem";
                else label = fmtExib.format(new Date(ts));

                listaItens.add(label);
                ultimoDia = diaVenda;
            }
            listaItens.add(v);
        }

        adapter.atualizarLista(listaItens);
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

    private void mostrarOpcoesStatus(VendaModel venda) {
        boolean isFinalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
        boolean isCancelada  = VendaModel.STATUS_CANCELADA.equals(venda.getStatus());

        String numero = venda.getNumeroVenda() > 0
                ? String.format(java.util.Locale.ROOT, "#%07d", venda.getNumeroVenda())
                : venda.getId().substring(0, 8).toUpperCase();

        // Monta as opções disponíveis dinamicamente
        java.util.List<String> opcoes = new java.util.ArrayList<>();
        if (!isFinalizada) opcoes.add("Marcar como Finalizada");
        if (!isCancelada)  opcoes.add("Marcar como Cancelada");

        if (opcoes.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alterar status — Venda " + numero)
                .setItems(opcoes.toArray(new String[0]), (dialog, which) -> {
                    String novoStatus = opcoes.get(which).contains("Finalizada")
                            ? VendaModel.STATUS_FINALIZADA
                            : VendaModel.STATUS_CANCELADA;
                    confirmarAlteracaoStatus(venda, novoStatus);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarAlteracaoStatus(VendaModel venda, String novoStatus) {
        String labelStatus = VendaModel.STATUS_FINALIZADA.equals(novoStatus)
                ? "finalizada" : "cancelada";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar alteração")
                .setMessage("Deseja marcar esta venda como " + labelStatus + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    vendaRepository.atualizarStatus(venda.getId(), novoStatus,
                            new VendaRepository.Callback() {
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
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
        startActivity(intent);
    }

    private void confirmarAlteracaoStatus(VendaModel venda) {
        boolean finalizada = VendaModel.STATUS_FINALIZADA.equals(venda.getStatus());
        String titulo  = finalizada ? "Cancelar venda"    : "Recuperar venda";
        String msg     = finalizada
                ? "Deseja cancelar esta venda? Ela continuará no histórico."
                : "Deseja marcar esta venda como finalizada novamente?";
        String btnText = finalizada ? "Cancelar venda" : "Recuperar";

        new androidx.appcompat.app.AlertDialog.Builder(this)
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
                // Listener em tempo real já atualiza a lista e o ResumoVendas automaticamente
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
}