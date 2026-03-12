package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments;

import android.content.Intent;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;

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

    private MovimentacaoRepository repository;
    private List<MovimentacaoModel> lista = new ArrayList<>();
    private String periodoSelecionado = "";
    private com.google.android.material.chip.ChipGroup chipGroupPeriodo;
    private com.google.android.material.chip.Chip chip7dias;
    private com.google.android.material.chip.Chip chip30dias;
    private com.google.android.material.chip.Chip chipMesAtual;

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

        chipGroupPeriodo = view.findViewById(R.id.chipGroupPeriodo);
        chip7dias = view.findViewById(R.id.chip7dias);
        chip30dias = view.findViewById(R.id.chip30dias);
        chipMesAtual = view.findViewById(R.id.chipMesAtual);

        repository = new MovimentacaoRepository();

        configurarCalendarios();
        configurarDatasPadrao();
        configurarChipsPeriodo();

        btnGerarPdf.setOnClickListener(v -> gerarPdf());
    }

    private void gerarPdf() {

        if(lista.isEmpty()){
            Toast.makeText(requireContext(),
                    "Não há dados para exportar neste período",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        com.gussanxz.orgafacil.funcionalidades.contas.exportacao.GeradorPDF.exportar(requireContext(),
                periodoSelecionado,
                lista,
                new com.gussanxz.orgafacil.funcionalidades.contas.exportacao.GeradorPDF.GeradorPdfCallback() {

                    @Override
                    public void onSucesso(File file) {

                        Uri uri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName()+".fileprovider",
                                file);

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("application/pdf");
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(Intent.createChooser(intent,"Compartilhar PDF"));
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

                        if(!isAdded()) return;

                        progressBarLoading.setVisibility(View.GONE);

                        lista = dados;

                        atualizarResumo();
                    }

                    @Override
                    public void onErro(String erro) {

                        if(!isAdded()) return;

                        progressBarLoading.setVisibility(View.GONE);

                        Toast.makeText(requireContext(),
                                "Erro ao buscar dados",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void atualizarResumo() {

        long receitas = 0;
        long despesas = 0;

        for(MovimentacaoModel m : lista){

            if(m.getTipoEnum() ==
                    com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas.RECEITA){

                receitas += m.getValor();

            }else{

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

        textSaldoRelatorio.setText(
                com.gussanxz.orgafacil.util_helper.MoedaHelper.formatarParaBRL(
                        com.gussanxz.orgafacil.util_helper.MoedaHelper.centavosParaDouble(saldo)));
    }

    private void configurarCalendarios(){

        editDataInicial.setOnClickListener(v ->
                abrirCalendario(editDataInicial));

        editDataFinal.setOnClickListener(v ->
                abrirCalendario(editDataFinal));
    }

    private void abrirCalendario(EditText campo){

        Calendar cal = Calendar.getInstance();

        try{
            Date dataAtual =
                    com.gussanxz.orgafacil.util_helper.DateHelper
                            .parsearData(campo.getText().toString());

            cal.setTime(dataAtual);

        }catch(Exception ignored){}

        new android.app.DatePickerDialog(requireContext(),
                (view, year, month, day) -> {

                    cal.set(year,month,day);

                    campo.setText(
                            com.gussanxz.orgafacil.util_helper.DateHelper
                                    .formatarData(cal.getTime()));

                    buscarDados();

                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH))
                .show();
    }

    private void configurarChipsPeriodo() {

        chip7dias.setOnClickListener(v -> {

            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date fim = cal.getTime();

            cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            java.util.Date inicio = cal.getTime();

            aplicarPeriodo(inicio, fim);
        });

        chip30dias.setOnClickListener(v -> {

            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Date fim = cal.getTime();

            cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
            java.util.Date inicio = cal.getTime();

            aplicarPeriodo(inicio, fim);
        });

        chipMesAtual.setOnClickListener(v -> {

            java.util.Calendar cal = java.util.Calendar.getInstance();

            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            java.util.Date inicio = cal.getTime();

            cal.set(java.util.Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));

            java.util.Date fim = cal.getTime();

            aplicarPeriodo(inicio, fim);
        });
    }

    private void aplicarPeriodo(java.util.Date inicio, java.util.Date fim){

        String inicioFmt =
                com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(inicio);

        String fimFmt =
                com.gussanxz.orgafacil.util_helper.DateHelper.formatarData(fim);

        editDataInicial.setText(inicioFmt);
        editDataFinal.setText(fimFmt);

        buscarDados();
    }
}