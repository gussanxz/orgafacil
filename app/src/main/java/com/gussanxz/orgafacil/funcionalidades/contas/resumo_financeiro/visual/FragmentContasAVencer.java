package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.HelperExibirDatasMovimentacao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections; // Importante para ordenação
import java.util.Date;
import java.util.List;

public class FragmentContasAVencer extends Fragment implements AdapterExibeListaMovimentacaoContas.OnItemActionListener {

    private RecyclerView recyclerView;
    private AdapterExibeListaMovimentacaoContas adapter;
    private MovimentacaoRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);
        repository = new MovimentacaoRepository();
        recyclerView = view.findViewById(R.id.recyclerInterno);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        carregarDadosReais();
    }

    private void carregarDadosReais() {
        repository.recuperarMovimentacoes(new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> listaCompleta) {
                if (listaCompleta == null) return;

                List<MovimentacaoModel> listaFiltrada = new ArrayList<>();

                // Definir "Amanhã 00:00"
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date dataAmanha = cal.getTime();

                for (MovimentacaoModel mov : listaCompleta) {
                    if (mov.getData_movimentacao() == null) continue;

                    Date dataMov = mov.getData_movimentacao().toDate();

                    // --- LÓGICA ATUALIZADA ---
                    // 1. Deve ser do Futuro (Data >= Amanhã)
                    boolean ehFuturo = !dataMov.before(dataAmanha);

                    // 2. Aceita RECEITA ou DESPESA
                    boolean tipoValido = (mov.getTipo() == TipoCategoriaContas.DESPESA.getId()) ||
                            (mov.getTipo() == TipoCategoriaContas.RECEITA.getId());

                    if (ehFuturo && tipoValido) {
                        listaFiltrada.add(mov);
                    }
                }

                // DICA DE OURO: Para contas a vencer, o ideal é ordenar ASCENDENTE (a que vence primeiro aparece no topo)
                // Se preferir ver a mais distante primeiro, mantenha o2.compareTo(o1)
                Collections.sort(listaFiltrada, (o1, o2) -> {
                    if (o1.getData_movimentacao() == null || o2.getData_movimentacao() == null) return 0;
                    return o1.getData_movimentacao().compareTo(o2.getData_movimentacao());
                });

                List<ExibirItemListaMovimentacaoContas> listaProcessada =
                        HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(listaFiltrada);

                adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, FragmentContasAVencer.this);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Métodos de Click ---
    @Override public void onDeleteClick(MovimentacaoModel mov) { confirmingExclusao(mov); }
    @Override public void onLongClick(MovimentacaoModel mov) {}

    private void confirmingExclusao(MovimentacaoModel mov) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirmar_exclusao, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        // --- LÓGICA DO TEXTO INTELIGENTE ---
        // Se for Despesa: "Pagar"
        // Se for Receita: "Receber"
        if (mov.getTipo() == TipoCategoriaContas.DESPESA.getId()) {
            textMensagem.setText("Pagar '" + mov.getDescricao() + "'?");
            btnConfirmar.setText("Pagar");
        } else {
            textMensagem.setText("Receber '" + mov.getDescricao() + "'?");
            btnConfirmar.setText("Receber");
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            repository.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) { carregarDadosReais(); }
                @Override public void onErro(String erro) { }
            });
        });
        dialog.show();
    }
}