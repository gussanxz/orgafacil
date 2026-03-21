package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private MaterialButton btnGerarRelatorio;

    // Componentes de Layout e Seleção
    private LinearLayout layoutResumoPrevia;
    private View layoutEmptyStateRelatorio;
    private MaterialCardView cardFormatoPdf, cardFormatoCsv;
    private ImageView iconFormatoPdf, iconFormatoCsv, btnLimparDatas;
    private TextView textFormatoPdf, textFormatoCsv;

    private boolean isPdfSelected = true;

    private MovimentacaoRepository repository;
    private List<MovimentacaoModel> lista = new ArrayList<>();
    private String periodoSelecionado = "";
    private com.google.android.material.chip.ChipGroup chipGroupPeriodo;
    private com.google.android.material.chip.Chip chip7dias;
    private com.google.android.material.chip.Chip chip30dias;
    private com.google.android.material.chip.Chip chipMesAtual;

    private AdapterMovimentacaoLista adapterPrevia;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        btnGerarRelatorio = view.findViewById(R.id.btnGerarRelatorio);

        layoutResumoPrevia = view.findViewById(R.id.layoutResumoPrevia);
        layoutEmptyStateRelatorio = view.findViewById(R.id.layoutEmptyStateRelatorio);
        btnLimparDatas = view.findViewById(R.id.btnLimparDatas);

        chipGroupPeriodo = view.findViewById(R.id.chipGroupPeriodo);
        chip7dias = view.findViewById(R.id.chip7dias);
        chip30dias = view.findViewById(R.id.chip30dias);
        chipMesAtual = view.findViewById(R.id.chipMesAtual);

        cardFormatoPdf = view.findViewById(R.id.cardFormatoPdf);
        cardFormatoCsv = view.findViewById(R.id.cardFormatoCsv);
        iconFormatoPdf = view.findViewById(R.id.iconFormatoPdf);
        iconFormatoCsv = view.findViewById(R.id.iconFormatoCsv);
        textFormatoPdf = view.findViewById(R.id.textFormatoPdf);
        textFormatoCsv = view.findViewById(R.id.textFormatoCsv);

        repository = new MovimentacaoRepository();

        // BUG 2: Limpa tudo ao clicar no X
        btnLimparDatas.setOnClickListener(v -> configurarEstadoInicialLimpo());

        configurarEventosFormatos();
        configurarAdapterPrevia();
        configurarCalendarios();
        configurarChipsPeriodo();

        configurarEstadoInicialLimpo();

        btnGerarRelatorio.setOnClickListener(v -> gerarRelatorio());
    }

    private void configurarEstadoInicialLimpo() {
        editDataInicial.setText("");
        editDataFinal.setText("");
        periodoSelecionado = "";

        if (chipGroupPeriodo != null) chipGroupPeriodo.clearCheck();
        if (layoutResumoPrevia != null) layoutResumoPrevia.setVisibility(View.GONE);

        lista.clear();
        btnLimparDatas.setVisibility(View.GONE);
    }

    private void configurarEventosFormatos() {
        cardFormatoPdf.setOnClickListener(v -> atualizarSelecaoFormato(true));
        cardFormatoCsv.setOnClickListener(v -> atualizarSelecaoFormato(false));
        atualizarSelecaoFormato(true);
    }

    private void atualizarSelecaoFormato(boolean isPdf) {
        this.isPdfSelected = isPdf;

        int corAtivaFundo = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int corInativaFundo = Color.parseColor("#252A40");
        int corInativaTexto = Color.parseColor("#9E9E9E");

        // Atualiza PDF
        cardFormatoPdf.setCardBackgroundColor(isPdf ? corAtivaFundo : corInativaFundo);
        textFormatoPdf.setTextColor(isPdf ? Color.WHITE : corInativaTexto);
        if (isPdf) {
            iconFormatoPdf.clearColorFilter(); // Deixa brilhar a cor do seu XML!
        } else {
            iconFormatoPdf.setColorFilter(corInativaTexto); // Deixa cinza se inativo
        }

        // Atualiza CSV
        cardFormatoCsv.setCardBackgroundColor(!isPdf ? corAtivaFundo : corInativaFundo);
        textFormatoCsv.setTextColor(!isPdf ? Color.WHITE : corInativaTexto);
        if (!isPdf) {
            iconFormatoCsv.clearColorFilter(); // Deixa brilhar a cor do seu XML!
        } else {
            iconFormatoCsv.setColorFilter(corInativaTexto); // Deixa cinza se inativo
        }
    }

    private void configurarAdapterPrevia() {
        adapterPrevia = new AdapterMovimentacaoLista(requireContext(),
                new AdapterMovimentacaoLista.OnItemActionListener() {
                    @Override public void onDeleteClick(MovimentacaoModel m) {}
                    @Override public void onLongClick(MovimentacaoModel m) {}
                    @Override public void onCheckClick(MovimentacaoModel m) {}
                    @Override public void onHeaderSwipeDelete(String d, List<MovimentacaoModel> l) {}
                    @Override public void onHeaderClick(String t, List<MovimentacaoModel> l) {}
                });
        recyclerPrevia.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPrevia.setAdapter(adapterPrevia);
        recyclerPrevia.setNestedScrollingEnabled(false);
    }

    private void gerarRelatorio() {
        if (editDataInicial.getText().toString().isEmpty() || editDataFinal.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Selecione o período inicial e final", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lista.isEmpty()) {
            Toast.makeText(requireContext(), "Não há dados para exportar neste período", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPdfSelected) {
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
        } else {
            Toast.makeText(requireContext(), "Exportação em CSV será implementada em breve!", Toast.LENGTH_SHORT).show();
        }
    }

    private void buscarDados() {
        String inicioStr = editDataInicial.getText().toString();
        String fimStr    = editDataFinal.getText().toString();

        if (inicioStr.isEmpty() || fimStr.isEmpty()) {
            layoutResumoPrevia.setVisibility(View.GONE);
            lista.clear();
            return;
        }

        Date dataInicio = com.gussanxz.orgafacil.util_helper.DateHelper.parsearData(inicioStr);
        Date dataFim    = com.gussanxz.orgafacil.util_helper.DateHelper.parsearData(fimStr);

        if (dataInicio != null && dataFim != null && dataFim.before(dataInicio)) {
            editDataFinal.setError("Data final inválida");
            editDataFinal.requestFocus();
            layoutResumoPrevia.setVisibility(View.GONE);
            return;
        }

        progressBarLoading.setVisibility(View.VISIBLE);
        setChipsEnabled(false);
        periodoSelecionado = inicioStr + " a " + fimStr;
        Date dataFimDia = com.gussanxz.orgafacil.util_helper.DateHelper.parsearDataFim(fimStr);

        repository.buscarMovimentacoesParaExportacao(dataInicio, dataFimDia,
                new MovimentacaoRepository.DadosCallback() {
                    @Override
                    public void onSucesso(List<MovimentacaoModel> dados) {
                        if (!isAdded()) return;
                        progressBarLoading.setVisibility(View.GONE);
                        setChipsEnabled(true);

                        // BUG 3 CORRIGIDO: Limpamos a lista global e adicionamos apenas as movimentações "OK"
                        lista.clear();
                        for (MovimentacaoModel m : dados) {

                            if (m.isPago()) {
                                lista.add(m);
                            }
                        }

                        layoutResumoPrevia.setVisibility(View.VISIBLE);
                        atualizarResumo();
                        atualizarPrevia();

                        if (lista.isEmpty()) {
                            Toast.makeText(requireContext(), "Nenhuma movimentação efetivada neste período.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onErro(String erro) {
                        if (!isAdded()) return;
                        progressBarLoading.setVisibility(View.GONE);
                        setChipsEnabled(true);
                        layoutResumoPrevia.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Erro ao buscar dados: " + erro, Toast.LENGTH_LONG).show();
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

        textTotalReceitas.setText(com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(receitas)));

        textTotalDespesas.setText(com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(despesas)));

        String saldoFmt = com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(Math.abs(saldo)));
        textSaldoRelatorio.setText(saldoFmt);

        if (saldo > 0) {
            textSaldoRelatorio.setTextColor(Color.parseColor("#4CAF50")); // verde
        } else if (saldo < 0) {
            textSaldoRelatorio.setTextColor(Color.parseColor("#E53935")); // vermelho
        } else {
            textSaldoRelatorio.setTextColor(Color.parseColor("#757575")); // cinza
        }
    }

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
            List<AdapterItemListaMovimentacao> itens = HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, false);
            adapterPrevia.submitList(itens);
        }
    }

    private void configurarCalendarios() {
        final boolean[] atualizandoPorChip = {false};

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!atualizandoPorChip[0] && chipGroupPeriodo != null) {
                    chipGroupPeriodo.clearCheck();
                }

                boolean isEmpty = editDataInicial.getText().toString().isEmpty() || editDataFinal.getText().toString().isEmpty();

                // BUG 2: Mostra ou oculta o "X" dinamicamente
                btnLimparDatas.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                if (isEmpty) {
                    if (layoutResumoPrevia != null) layoutResumoPrevia.setVisibility(View.GONE);
                    lista.clear();
                }
            }
        };

        editDataInicial.addTextChangedListener(watcher);
        editDataFinal.addTextChangedListener(watcher);

        editDataInicial.setOnClickListener(v -> abrirCalendario(editDataInicial));
        editDataFinal.setOnClickListener(v -> abrirCalendario(editDataFinal));

        chip7dias.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Date fim = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date inicio = cal.getTime();
            aplicarPeriodoComFlag(inicio, fim, atualizandoPorChip);
        });

        chip30dias.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            Date fim = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date inicio = cal.getTime();
            aplicarPeriodoComFlag(inicio, fim, atualizandoPorChip);
        });

        chipMesAtual.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date inicio = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date fim = cal.getTime();
            aplicarPeriodoComFlag(inicio, fim, atualizandoPorChip);
        });
    }

    private void aplicarPeriodoComFlag(Date inicio, Date fim, boolean[] flag) {
        flag[0] = true;
        try {
            aplicarPeriodo(inicio, fim);
        } finally {
            flag[0] = false;
        }
    }

    private void abrirCalendario(EditText campo) {
        Calendar cal = Calendar.getInstance();
        try {
            Date dataAtual = com.gussanxz.orgafacil.util_helper.DateHelper.parsearData(campo.getText().toString());
            cal.setTime(dataAtual);
        } catch (Exception ignored) {}

        new android.app.DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    cal.set(year, month, day);
                    campo.setText(com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(cal.getTime()));
                    if (chipGroupPeriodo != null) chipGroupPeriodo.clearCheck();
                    buscarDados();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void configurarChipsPeriodo() {
    }

    private void aplicarPeriodo(Date inicio, Date fim) {
        String inicioFmt = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(inicio);
        String fimFmt = com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(fim);
        editDataInicial.setText(inicioFmt);
        editDataFinal.setText(fimFmt);
        buscarDados();
    }
}