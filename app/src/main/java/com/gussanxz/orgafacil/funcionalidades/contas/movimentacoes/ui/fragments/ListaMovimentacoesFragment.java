package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.fragments;

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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasViewModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.HelperExibirDatasMovimentacao;

import java.util.ArrayList;
import java.util.List;

/**
 * ListaMovimentacoesFragment (VITRINE DO DASHBOARD)
 * Este fragmento resolve o conflito do ViewPager observando fontes de dados distintas
 * (Histórico vs Futuro) baseadas no modo de operação.
 * [NOVO]: Ele limita a exibição a no máximo 10 itens por aba, para não sobrecarregar
 * a tela inicial, agindo como um resumo rápido.
 */
public class ListaMovimentacoesFragment extends Fragment implements AdapterMovimentacaoLista.OnItemActionListener {

    private static final String ARG_MODO_FUTURO = "modo_futuro";

    private RecyclerView recyclerView;
    private AdapterMovimentacaoLista adapter;
    private MovimentacaoRepository repository;
    private ContasViewModel viewModel;
    private boolean ehModoFuturo;
    private View layoutEmptyState;
    private TextView textEmptyState;

    // Construtor estático para facilitar a criação correta das instâncias
    public static ListaMovimentacoesFragment newInstance(boolean exibirFuturas) {
        ListaMovimentacoesFragment fragment = new ListaMovimentacoesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_MODO_FUTURO, exibirFuturas);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ehModoFuturo = getArguments().getBoolean(ARG_MODO_FUTURO);
        }
        repository = new MovimentacaoRepository();
        // Compartilha o ViewModel com a Activity para ter acesso aos filtros globais
        viewModel = new ViewModelProvider(requireActivity()).get(ContasViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);

        recyclerView = view.findViewById(R.id.recyclerInterno);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // [NOVO] Vinculando a tela vazia
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        textEmptyState = view.findViewById(R.id.textEmptyState);

        // Ajusta a frase para fazer sentido com a aba atual
        if (ehModoFuturo) {
            textEmptyState.setText("Ufa! Nenhuma conta pendente. 🎉");
        } else {
            textEmptyState.setText("Nenhum histórico no momento.");
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupObservers();
        carregarDados();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Garante que, ao voltar para esta aba (slide), os dados sejam recarregados
        // corretamente para este contexto específico.
        carregarDados();
    }

    /**
     * Decide qual LiveData observar e corta a lista para exibir apenas o resumo.
     */
    private void setupObservers() {
        Observer<List<MovimentacaoModel>> observerUI = lista -> {

            // [NOVO] Controle do Estado Vazio
            if (lista == null || lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                return; // Sai da função, não precisa desenhar a lista
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
            }

            // A mágica da vitrine continua igual...
            List<MovimentacaoModel> listaResumo = new ArrayList<>();
            int limiteVitrine = Math.min(lista.size(), 10);

            for (int i = 0; i < limiteVitrine; i++) {
                listaResumo.add(lista.get(i));
            }

            // Helper organiza por dia (Crescente ou Decrescente dependendo do modo)
            // Agora passando a lista já cortada
            List<AdapterItemListaMovimentacao> listaProcessada =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(listaResumo, ehModoFuturo);

            adapter = new AdapterMovimentacaoLista(getContext(), listaProcessada, this);
            recyclerView.setAdapter(adapter);
        };

        // Remove observadores antigos para evitar vazamento de memória ou duplicação
        viewModel.listaHistorico.removeObservers(getViewLifecycleOwner());
        viewModel.listaFutura.removeObservers(getViewLifecycleOwner());

        // Conecta ao LiveData correto
        if (ehModoFuturo) {
            viewModel.listaFutura.observe(getViewLifecycleOwner(), observerUI);
        } else {
            viewModel.listaHistorico.observe(getViewLifecycleOwner(), observerUI);
        }
    }

    private void carregarDados() {
        // Envia 'ehModoFuturo' para o ViewModel saber qual cache atualizar
        viewModel.fetchDados(repository, ehModoFuturo, new MovimentacaoRepository.DadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> lista) { /* A UI é atualizada pelo Observer */ }
            @Override public void onErro(String erro) {
                if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- INTERAÇÕES DO ADAPTER ---

    @Override
    public void onDeleteClick(MovimentacaoModel mov) {
        exibirDialogExclusao(mov);
    }

    @Override
    public void onLongClick(MovimentacaoModel mov) {
        // Implementar edição se desejar
    }

    /**
     * Implementação da lógica de confirmação (Check).
     * Quando o usuário clica no check, o item deixa de ser "pendente/futuro"
     * e passa a ser uma movimentação confirmada (pago = true).
     */
    @Override
    public void onCheckClick(MovimentacaoModel mov) {
        String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar " + acao)
                .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    repository.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
                        @Override
                        public void onSucesso(String msg) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Concluído!", Toast.LENGTH_SHORT).show();

                                // [ATUALIZAÇÃO DUPLA]: O item saiu de uma lista e foi para outra.
                                // Precisamos atualizar ambos os contextos no ViewModel.
                                viewModel.fetchDados(repository, true, null);  // Atualiza Futuros (Remove o item)
                                viewModel.fetchDados(repository, false, null); // Atualiza Histórico (Adiciona o item)
                            }
                        }

                        @Override
                        public void onErro(String erro) {
                            if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exibirDialogExclusao(MovimentacaoModel mov) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirmar_exclusao, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        Button btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        // Texto inteligente baseado no contexto
        if (ehModoFuturo) {
            String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "Pagar" : "Receber";
            textMensagem.setText(acao + " '" + mov.getDescricao() + "'?");
            btnConfirmar.setText(acao);
        } else {
            textMensagem.setText("Excluir '" + mov.getDescricao() + "'?");
            btnConfirmar.setText("Excluir");
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            repository.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) {
                    // Recarrega apenas a lista atual
                    carregarDados();
                }
                @Override public void onErro(String erro) { }
            });
        });
        dialog.show();
    }
}