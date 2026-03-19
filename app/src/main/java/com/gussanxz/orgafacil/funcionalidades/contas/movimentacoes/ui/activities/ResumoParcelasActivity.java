package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.ResumoParcelamentoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.ParcelamentoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResumoParcelasActivity extends AppCompatActivity {

    private ParcelamentoRepository repo;
    private MovimentacaoRepository movRepo;
    private String recorrenciaId;

    // Views - card de resumo
    private TextView textTitulo;
    private TextView textProgresso;
    private TextView textValorPago;
    private TextView textValorRestante;
    private TextView textProximaData;
    private ProgressBar progressParcelas;
    private ProgressBar progressBarLoading;

    // Views - lista
    private RecyclerView recyclerParcelas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_parcelas);

        recorrenciaId = getIntent().getStringExtra("recorrencia_id");

        if (recorrenciaId == null || recorrenciaId.isEmpty()) {
            Toast.makeText(this, "Série não encontrada.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repo = new ParcelamentoRepository();
        movRepo = new MovimentacaoRepository();

        vincularViews();
        carregarSerie();
    }

    private void vincularViews() {
        textTitulo       = findViewById(R.id.textTitulo);
        textProgresso    = findViewById(R.id.textProgresso);
        textValorPago    = findViewById(R.id.textValorPago);
        textValorRestante= findViewById(R.id.textValorRestante);
        textProximaData  = findViewById(R.id.textProximaData);
        progressParcelas = findViewById(R.id.progressParcelas);
        progressBarLoading = findViewById(R.id.progressBarLoading);
        recyclerParcelas = findViewById(R.id.recyclerParcelas);

        recyclerParcelas.setLayoutManager(new LinearLayoutManager(this));
    }

    private void carregarSerie() {
        progressBarLoading.setVisibility(View.VISIBLE);

        repo.buscarParcelas(recorrenciaId, new ParcelamentoRepository.ParcelasCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> parcelas) {
                progressBarLoading.setVisibility(View.GONE);

                if (parcelas.isEmpty()) {
                    Toast.makeText(ResumoParcelasActivity.this,
                            "Nenhuma parcela encontrada para esta série.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                ResumoParcelamentoModel resumo = ResumoParcelamentoModel.calcular(parcelas);
                exibirResumo(resumo, parcelas);
                exibirListaParcelas(parcelas);
            }

            @Override
            public void onErro(String erro) {
                progressBarLoading.setVisibility(View.GONE);
                Toast.makeText(ResumoParcelasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void exibirResumo(ResumoParcelamentoModel r, List<MovimentacaoModel> parcelas) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        // Título: usa a descrição base da primeira parcela (sem a tag "(x/y)")
        if (!parcelas.isEmpty() && parcelas.get(0).getDescricao() != null) {
            String descBase = parcelas.get(0).getDescricao()
                    .replaceAll("\\s*\\(\\d+/\\d+\\)$", "").trim();
            textTitulo.setText(descBase);
        }

        // Progresso
        textProgresso.setText(r.parcelasPagas + "/" + r.totalParcelas + " parcelas pagas");

        // Converte para percentual (0–100) para o ProgressBar
        int percentual = r.totalParcelas > 0
                ? (int) ((double) r.parcelasPagas / r.totalParcelas * 100)
                : 0;
        progressParcelas.setMax(100);
        progressParcelas.setProgress(percentual);

        // Valores
        textValorPago.setText(fmt.format(r.valorPagoCentavos / 100.0));
        textValorRestante.setText(fmt.format(r.valorRestanteCentavos / 100.0));

        // Próximo vencimento
        if (r.proximaPendente != null && r.proximaPendente.getData_movimentacao() != null) {
            String dataStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(r.proximaPendente.getData_movimentacao().toDate());
            textProximaData.setText("Próximo vencimento: " + dataStr);
        } else {
            textProximaData.setText("Todas as parcelas pagas ✓");
            textProximaData.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        }
    }

    private void exibirListaParcelas(List<MovimentacaoModel> parcelas) {
        List<AdapterItemListaMovimentacao> itens = new ArrayList<>();
        for (MovimentacaoModel p : parcelas) {
            itens.add(AdapterItemListaMovimentacao.linha(p));
        }

        // 1. Instanciamos sem a lista
        AdapterMovimentacaoLista adapter = new AdapterMovimentacaoLista(
                this,
                new AdapterMovimentacaoLista.OnItemActionListener() {
                    @Override
                    public void onDeleteClick(MovimentacaoModel mov) {
                        // Não exposto nesta tela — o usuário edita pela tela de detalhes
                    }

                    @Override
                    public void onLongClick(MovimentacaoModel mov) {
                        // Não exposto nesta tela
                    }

                    @Override
                    public void onCheckClick(MovimentacaoModel mov) {
                        confirmarParcela(mov);
                    }

                    @Override
                    public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) {
                        // Sem swipe de header nesta tela
                    }
                    @Override
                    public void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia) {
                        // Como essa tela não tem headers de dia,
                        // esse clique nunca vai acontecer. Pode ficar vazio!
                    }
                }
        );

        // 2. Enviamos a lista para o background calcular e desenhar
        adapter.submitList(itens);

        recyclerParcelas.setAdapter(adapter);
    }
    /**
     * Confirma o pagamento de uma parcela individual diretamente desta tela.
     */
    private void confirmarParcela(MovimentacaoModel mov) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar parcela")
                .setMessage("Deseja marcar a parcela " + mov.getParcela_atual() +
                        "/" + mov.getTotal_parcelas() + " como paga?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    movRepo.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            Toast.makeText(ResumoParcelasActivity.this,
                                    "Parcela confirmada!", Toast.LENGTH_SHORT).show();
                            // Recarrega para refletir o novo estado
                            carregarSerie();
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ResumoParcelasActivity.this,
                                    "Erro: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void retornarPrincipal(View view) {
        finish();
    }
}