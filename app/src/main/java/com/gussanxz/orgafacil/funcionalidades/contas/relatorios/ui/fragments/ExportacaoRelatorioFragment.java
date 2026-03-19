package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.HelperExibirDatasMovimentacao;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExportacaoRelatorioFragment extends Fragment {

    private EditText editDataInicial, editDataFinal;
    private TextView textTotalReceitas, textTotalDespesas, textSaldoRelatorio;
    private RecyclerView recyclerPrevia;
    private ProgressBar progressBarLoading;
    private MaterialButton btnGerarPdf;
    private View layoutEmptyStateRelatorio;

    private MovimentacaoRepository repository;
    private List<MovimentacaoModel> lista = new ArrayList<>();
    private String periodoSelecionado = "";
    private com.google.android.material.chip.ChipGroup chipGroupPeriodo;
    private com.google.android.material.chip.Chip chip7dias;
    private com.google.android.material.chip.Chip chip30dias;
    private com.google.android.material.chip.Chip chipMesAtual;

    // CORREÇÃO: adapter tipado para a prévia — antes o recyclerPrevia nunca tinha adapter
    private AdapterMovimentacaoLista adapterPrevia;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_relatorio_exportacao, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editDataInicial = view.findViewById(R.id.editDataInicial);
        editDataFinal = view.findViewById(R.id.editDataFinal);
        textTotalReceitas = view.findViewById(R.id.textTotalReceitas);
        textTotalDespesas = view.findViewById(R.id.textTotalDespesas);
        textSaldoRelatorio = view.findViewById(R.id.textSaldoRelatorio);
        recyclerPrevia = view.findViewById(R.id.recyclerPrevia);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        btnGerarPdf = view.findViewById(R.id.btnGerarPdf);
        layoutEmptyStateRelatorio = view.findViewById(R.id.layoutEmptyStateRelatorio);

        chipGroupPeriodo = view.findViewById(R.id.chipGroupPeriodo);
        chip7dias = view.findViewById(R.id.chip7dias);
        chip30dias = view.findViewById(R.id.chip30dias);
        chipMesAtual = view.findViewById(R.id.chipMesAtual);

        repository = new MovimentacaoRepository();

        // CORREÇÃO: configura o adapter da prévia antes de buscar dados
        configurarAdapterPrevia();
        configurarCalendarios();
        configurarDatasPadrao();
        configurarChipsPeriodo();

        btnGerarPdf.setOnClickListener(v -> gerarPdf());
    }

    // CORREÇÃO: RecyclerView da prévia agora tem adapter configurado corretamente
    private void configurarAdapterPrevia() {
        adapterPrevia = new AdapterMovimentacaoLista(requireContext(),
                new AdapterMovimentacaoLista.OnItemActionListener() {
                    // Prévia é somente leitura — nenhuma ação exposta
                    @Override public void onDeleteClick(MovimentacaoModel m) {}
                    @Override public void onLongClick(MovimentacaoModel m) {}
                    @Override public void onCheckClick(MovimentacaoModel m) {}
                    @Override public void onHeaderSwipeDelete(String d, List<MovimentacaoModel> l) {}
                    @Override public void onHeaderClick(String t, List<MovimentacaoModel> l) {}
                });
        recyclerPrevia.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPrevia.setAdapter(adapterPrevia);
        // Desabilita scroll aninhado para não conflitar com NestedScrollView
        recyclerPrevia.setNestedScrollingEnabled(false);
    }

    private void gerarPdf() {
        if (lista.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Não há dados para exportar neste período",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        com.gussanxz.orgafacil.funcionalidades.contas.exportacao.GeradorPDF.exportar(
                requireContext(),
                periodoSelecionado,
                lista,
                new com.gussanxz.orgafacil.funcionalidades.contas.exportacao.GeradorPDF.GeradorPdfCallback() {
                    @Override
                    public void onSucesso(File file) {
                        Uri uri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".fileprovider",
                                file);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("application/pdf");
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Compartilhar PDF"));
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(requireContext(), erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void configurarDatasPadrao() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        String inicio = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(cal.getTime());
        cal.set(java.util.Calendar.DAY_OF_MONTH,
                cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        String fim = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(cal.getTime());
        editDataInicial.setText(inicio);
        editDataFinal.setText(fim);
        periodoSelecionado = inicio + " a " + fim;
        buscarDados();
    }

    private void buscarDados() {
        progressBarLoading.setVisibility(View.VISIBLE);
        // CORREÇÃO: desabilita chips durante carregamento para evitar dupla busca
        setChipsEnabled(false);

        String inicioStr = editDataInicial.getText().toString();
        String fimStr = editDataFinal.getText().toString();
        periodoSelecionado = inicioStr + " a " + fimStr;

        java.util.Date dataInicio =
                com.gussanxz.orgafacil.util_helper.DateHelper.parsearData(inicioStr);
        java.util.Date dataFim =
                com.gussanxz.orgafacil.util_helper.DateHelper.parsearDataFim(fimStr);

        repository.buscarMovimentacoesParaExportacao(dataInicio, dataFim,
                new MovimentacaoRepository.DadosCallback() {
                    @Override
                    public void onSucesso(List<MovimentacaoModel> dados) {
                        if (!isAdded()) return;
                        progressBarLoading.setVisibility(View.GONE);
                        setChipsEnabled(true);
                        lista = dados;
                        atualizarResumo();
                        atualizarPrevia();

                        if (lista.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "Nenhuma movimentação neste período.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        progressBarLoading.setVisibility(View.GONE);
                        setChipsEnabled(true);
                        Toast.makeText(requireContext(),
                                "Erro ao buscar dados: " + erro,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setChipsEnabled(boolean enabled) {
        if (chip7dias != null) chip7dias.setEnabled(enabled);
        if (chip30dias != null) chip30dias.setEnabled(enabled);
        if (chipMesAtual != null) chipMesAtual.setEnabled(enabled);
    }

    private void atualizarResumo() {
        long receitas = 0;
        long despesas = 0;

        for (MovimentacaoModel m : lista) {
            if (m.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                receitas += m.getValor();
            } else {
                despesas += m.getValor();
            }
        }

        long saldo = receitas - despesas;

        textTotalReceitas.setText(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                        com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(receitas)));

        textTotalDespesas.setText(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                        com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(despesas)));

        // CORREÇÃO: saldo com cor — antes sempre ficava na cor padrão do XML
        String saldoFmt = com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(Math.abs(saldo)));
        textSaldoRelatorio.setText(saldoFmt);

        if (saldo > 0) {
            textSaldoRelatorio.setTextColor(Color.parseColor("#43A047")); // verde
        } else if (saldo < 0) {
            textSaldoRelatorio.setTextColor(Color.parseColor("#E53935")); // vermelho
        } else {
            textSaldoRelatorio.setTextColor(Color.parseColor("#757575")); // cinza
        }
    }

    // CORREÇÃO: prévia agora exibe de fato as movimentações agrupadas por dia
    private void atualizarPrevia() {
        if (lista.isEmpty()) {
            recyclerPrevia.setVisibility(View.GONE);
            if (layoutEmptyStateRelatorio != null) {
                layoutEmptyStateRelatorio.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerPrevia.setVisibility(View.VISIBLE);
            if (layoutEmptyStateRelatorio != null) {
                layoutEmptyStateRelatorio.setVisibility(View.GONE);
            }
            // Agrupa por dia exatamente como a tela principal de contas — modo histórico (false)
            List<AdapterItemListaMovimentacao> itens =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, false);
            adapterPrevia.submitList(itens);
        }
    }

    private void configurarCalendarios() {
        editDataInicial.setOnClickListener(v -> abrirCalendario(editDataInicial));
        editDataFinal.setOnClickListener(v -> abrirCalendario(editDataFinal));
    }

    private void abrirCalendario(EditText campo) {
        Calendar cal = Calendar.getInstance();
        try {
            Date dataAtual = com.gussanxz.orgafacil.util_helper.DateHelper
                    .parsearData(campo.getText().toString());
            cal.setTime(dataAtual);
        } catch (Exception ignored) {}

        new android.app.DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    cal.set(year, month, day);
                    campo.setText(com.gussanxz.orgafacil.util_helper.DateHelper
                            .formatarData(cal.getTime()));
                    // CORREÇÃO: desmarca chips ao editar data manualmente
                    // (evita chip ativo com datas inconsistentes)
                    if (chipGroupPeriodo != null) chipGroupPeriodo.clearCheck();
                    buscarDados();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void configurarChipsPeriodo() {
        chip7dias.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Date fim = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date inicio = cal.getTime();
            aplicarPeriodo(inicio, fim);
        });

        chip30dias.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Date fim = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date inicio = cal.getTime();
            aplicarPeriodo(inicio, fim);
        });

        chipMesAtual.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date inicio = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date fim = cal.getTime();
            aplicarPeriodo(inicio, fim);
        });
    }

    private void aplicarPeriodo(Date inicio, Date fim) {
        String inicioFmt = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(inicio);
        String fimFmt = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(fim);
        editDataInicial.setText(inicioFmt);
        editDataFinal.setText(fimFmt);
        buscarDados();
    }
}