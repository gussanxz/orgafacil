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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasViewModel;

import java.util.List;

/**
 * ListaMovimentacoesFragment (UNIFICADO E CORRIGIDO)
 * Este fragmento resolve o conflito do ViewPager observando fontes de dados distintas
 * (Histórico vs Futuro) baseadas no modo de operação.
 */
public class ListaMovimentacoesFragment extends Fragment implements AdapterExibeListaMovimentacaoContas.OnItemActionListener {

    private static final String ARG_MODO_FUTURO = "modo_futuro";

    private RecyclerView recyclerView;
    private AdapterExibeListaMovimentacaoContas adapter;
    private MovimentacaoRepository repository;
    private ContasViewModel viewModel;
    private boolean ehModoFuturo;

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
        // [FIX]: Garante que, ao voltar para esta aba (slide), os dados sejam recarregados
        // corretamente para este contexto específico.
        carregarDados();
    }

    /**
     * [CORREÇÃO PRINCIPAL]: Decide qual LiveData observar.
     * Isso isola a UI: A aba de Histórico só reage a mudanças na lista de Histórico,
     * e a aba de Futuro só reage a mudanças na lista Futura.
     */
    private void setupObservers() {
        // Define o comportamento comum de atualização da UI
        Observer<List<MovimentacaoModel>> observerUI = lista -> {
            // Helper organiza por dia (Crescente ou Decrescente dependendo do modo)
            List<ExibirItemListaMovimentacaoContas> listaProcessada =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehModoFuturo);

            adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, this);
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
        // [CORREÇÃO]: Envia 'ehModoFuturo' para o ViewModel saber qual cache atualizar
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

    @Override public void onLongClick(MovimentacaoModel mov) {
        // Implementar edição se desejar
    }

    /**
     * [NOVO]: Implementação da lógica de confirmação (Check).
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

        // [CLEAN CODE] Texto inteligente baseado no contexto
        if (ehModoFuturo) {
            // [CORREÇÃO APLICADA]: Uso de Enum em vez de ID inteiro/legado
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