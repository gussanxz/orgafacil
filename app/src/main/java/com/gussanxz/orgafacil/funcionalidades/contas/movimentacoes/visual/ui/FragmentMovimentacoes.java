package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.r_negocio.modelos.MovimentacaoModel;

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

        // 1. Inicializa Componentes
        repository = new MovimentacaoRepository();
        recyclerView = view.findViewById(R.id.recyclerInterno);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Carrega os dados sempre que a tela aparece (garante atualização após adicionar/editar)
        carregarDadosReais();
    }

    private void carregarDadosReais() {
        // Busca TODAS as movimentações do banco
        // Nota: Idealmente, no futuro, criaríamos um método no Repository "listarUltimas(int limit)"
        // para não baixar o banco todo. Por enquanto, filtramos aqui.
        repository.recuperarMovimentacoes(new MovimentacaoRepository.DadosCallback() {
            @Override
            public void onSucesso(List<MovimentacaoModel> listaCompleta) {
                if (listaCompleta == null || listaCompleta.isEmpty()) {
                    // Se quiser, mostre uma View de "Nenhuma movimentação" aqui
                    return;
                }

                // A. Ordenar por Data (Mais recente primeiro) usando o Timestamp do Model
                // O Helper já faz isso, mas garantimos aqui para o corte dos 5 primeiros
                Collections.sort(listaCompleta, (o1, o2) -> {
                    if (o1.getData_movimentacao() == null || o2.getData_movimentacao() == null) return 0;
                    return o2.getData_movimentacao().compareTo(o1.getData_movimentacao());
                });

                // B. Pegar apenas as 5 mais recentes (Lógica de Resumo)
                int limite = 5;
                List<MovimentacaoModel> ultimasMovimentacoes = listaCompleta.subList(0, Math.min(listaCompleta.size(), limite));

                // C. Processar para exibir (Agrupar datas, calcular saldos do dia)
                List<ExibirItemListaMovimentacaoContas> listaProcessada =
                        HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(ultimasMovimentacoes);

                // D. Atualizar Adapter
                adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, FragmentMovimentacoes.this);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(getContext(), "Erro ao carregar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Ações do Adapter ---

    @Override
    public void onDeleteClick(MovimentacaoModel mov) {
        confirmarExclusao(mov);
    }

    @Override
    public void onLongClick(MovimentacaoModel mov) {
        // Opcional: Abrir detalhes ou menu de edição rápida
        Toast.makeText(getContext(), "Detalhes: " + mov.getDescricao(), Toast.LENGTH_SHORT).show();
    }

    // --- Diálogo de Confirmação ---

    private void confirmarExclusao(MovimentacaoModel mov) {
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
            // Chama o Repository para excluir e estornar o saldo
            repository.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override
                public void onSucesso(String msg) {
                    Toast.makeText(getContext(), "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarDadosReais(); // Recarrega a lista
                }

                @Override
                public void onErro(String erro) {
                    Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}