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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.HelperExibirDatasMovimentacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FragmentMovimentacoes extends Fragment implements AdapterExibeListaMovimentacaoContas.OnItemActionListener {

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

                // FILTRO POR TIPO: Apenas RECEITA ou DESPESA
                List<MovimentacaoModel> listaFiltrada = new ArrayList<>();
                for (MovimentacaoModel mov : listaCompleta) {
                    if (mov.getTipo() == TipoCategoriaContas.RECEITA.getId() ||
                            mov.getTipo() == TipoCategoriaContas.DESPESA.getId()) {
                        listaFiltrada.add(mov);
                    }
                }

                // Ordenação padrão (mais recente primeiro)
                Collections.sort(listaFiltrada, (o1, o2) -> {
                    if (o1.getData_movimentacao() == null || o2.getData_movimentacao() == null) return 0;
                    return o2.getData_movimentacao().compareTo(o1.getData_movimentacao());
                });

                // Limite de 5 itens
                int limite = 5;
                List<MovimentacaoModel> ultimas = listaFiltrada.subList(0, Math.min(listaFiltrada.size(), limite));

                List<ExibirItemListaMovimentacaoContas> listaProcessada =
                        HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(ultimas);

                adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, FragmentMovimentacoes.this);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Métodos de Click (Mantidos iguais) ---
    @Override public void onDeleteClick(MovimentacaoModel mov) { confirmingExclusao(mov); }
    @Override public void onLongClick(MovimentacaoModel mov) {}

    private void confirmingExclusao(MovimentacaoModel mov) {
        // ... (Seu código de dialog existente)
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirmar_exclusao, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        textMensagem.setText("Excluir '" + mov.getDescricao() + "'?");
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