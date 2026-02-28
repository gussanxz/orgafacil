package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.gussanxz.orgafacil.util_helper.SwipeCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ListaMovimentacoesFragment (VITRINE DO DASHBOARD)
 * Este fragmento resolve o conflito do ViewPager observando fontes de dados distintas
 * (Histórico vs Futuro) baseadas no modo de operação.
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
    private ProgressBar progressBarFragment;

    // Controle para evitar o piscar do Empty State na primeira carga
    private boolean isPrimeiroCarregamento = true;

    private ItemTouchHelper swipeHelper;

    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> launcherEdicao;

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

        launcherEdicao = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        viewModel.fetchDados(true, null);
                        viewModel.fetchDados(false, null);
                    }
                });
    }

    private void abrirTelaEdicao(MovimentacaoModel m, boolean direto) {
        android.content.Intent intent =
                new android.content.Intent(requireContext(),
                        com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.EditarMovimentacaoActivity.class);

        intent.putExtra("movimentacaoSelecionada", m);
        intent.putExtra("DIRETO_PRA_EDICAO", direto);

        launcherEdicao.launch(intent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);

        recyclerView = view.findViewById(R.id.recyclerInterno);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Vinculando a tela vazia e ProgressBar
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        textEmptyState = view.findViewById(R.id.textEmptyState);
        progressBarFragment = view.findViewById(R.id.progressBarFragment);

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
        carregarDados();
    }

    /**
     * Decide qual LiveData observar e corta a lista para exibir apenas o resumo.
     */
    private void setupObservers() {

        // Observer para o ProgressBar e controle do "Piscar"
        viewModel.carregandoPaginacao.observe(getViewLifecycleOwner(), isCarregando -> {
            if (isCarregando) {
                // [CORREÇÃO AQUI]: Só exibe a bolinha se for a primeira vez que a aba carrega.
                // Se o usuário só mudou de aba, a atualização dos dados acontece de forma silenciosa e transparente.
                if (isPrimeiroCarregamento) {
                    progressBarFragment.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.GONE);
                }
            } else {
                progressBarFragment.setVisibility(View.GONE);
                isPrimeiroCarregamento = false;
            }
        });


        Observer<List<MovimentacaoModel>> observerUI = lista -> {

            // Se ainda estiver na primeira vez e o loading não acabou, ignora e não desenha a tela vazia
            if (isPrimeiroCarregamento && Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

            // Controle do Estado Vazio
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
            List<AdapterItemListaMovimentacao> listaProcessada =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(listaResumo, ehModoFuturo);

            adapter = new AdapterMovimentacaoLista(getContext(), listaProcessada, new AdapterMovimentacaoLista.OnItemActionListener() {

                @Override
                public void onDeleteClick(MovimentacaoModel mov) {
                    exibirDialogExclusao(mov);
                }
                @Override
                public void onLongClick(MovimentacaoModel mov) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Editar")
                            .setMessage("Deseja editar '" + mov.getDescricao() + "'?")
                            .setPositiveButton("Sim", (d, w) -> abrirTelaEdicao(mov, false))
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
                @Override
                public void onCheckClick(MovimentacaoModel mov) {
                    // mantém o comportamento original
                    String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";
                    if (mov.getTotal_parcelas() > 1 && mov.getParcela_atual() < mov.getTotal_parcelas()) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirmar " + acao)
                                .setMessage("Deseja confirmar apenas '" + mov.getDescricao() +
                                        "' ou também antecipar todas as parcelas seguintes?")
                                .setPositiveButton("Apenas esta", (d, w) -> executarConfirmacao(mov))
                                .setNegativeButton("Esta e seguintes", (d, w) -> executarConfirmacaoEmMassa(mov))
                                .setNeutralButton("Cancelar", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirmar " + acao)
                                .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                                .setPositiveButton("Confirmar", (dialog, which) -> executarConfirmacao(mov))
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                }

                @Override
                public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) {
                    // Não é chamado diretamente aqui — o SwipeCallback aciona direto
                    confirmarExclusaoDoDia(dataDia, movsDoDia);
                }
            });

            recyclerView.setAdapter(adapter);

            // Garante que o SwipeCallback está configurado sempre que o adapter é recriado
            configurarSwipeDoDashboard();

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
        // Agora solicitamos direto à ViewModel, sem passar a dependência
        viewModel.fetchDados(ehModoFuturo, new MovimentacaoRepository.DadosCallback() {
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
     */
    @Override
    public void onCheckClick(MovimentacaoModel mov) {
        String acao = (mov.getTipoEnum() == TipoCategoriaContas.DESPESA) ? "pagamento" : "recebimento";

        // Se for parcela com sequência, perguntar sobre as demais
        if (mov.getTotal_parcelas() > 1 && mov.getParcela_atual() < mov.getTotal_parcelas()) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar apenas '" + mov.getDescricao() + "' ou também antecipar todas as parcelas seguintes?")
                    .setPositiveButton("Apenas esta", (d, w) -> executarConfirmacao(mov))
                    .setNegativeButton("Esta e seguintes", (d, w) -> executarConfirmacaoEmMassa(mov))
                    .setNeutralButton("Cancelar", null)
                    .show();
        } else {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar " + acao)
                    .setMessage("Deseja confirmar que '" + mov.getDescricao() + "' foi concluído?")
                    .setPositiveButton("Confirmar", (dialog, which) -> executarConfirmacao(mov))
                    .setNegativeButton("Cancelar", null)
                    .show();

        }
    }

    private void executarConfirmacao(MovimentacaoModel mov) {
        viewModel.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Concluído!", Toast.LENGTH_SHORT).show();
                    viewModel.fetchDados(true, null);
                    viewModel.fetchDados(false, null);
                }
            }
            @Override
            public void onErro(String erro) {
                if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarConfirmacaoEmMassa(MovimentacaoModel movBase) {
        viewModel.confirmarMovimentacaoEmMassa(movBase, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                if (isAdded()) {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    viewModel.fetchDados(true, null);
                    viewModel.fetchDados(false, null);
                }
            }
            @Override
            public void onErro(String erro) {
                if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
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
            viewModel.excluir(mov, new MovimentacaoRepository.Callback() {
                @Override public void onSucesso(String msg) {
                    carregarDados();
                }
                @Override public void onErro(String erro) { }
            });
        });
        dialog.show();
    }

    private void configurarSwipeDoDashboard() {
        if (swipeHelper != null) swipeHelper.attachToRecyclerView(null); // desanexa o anterior

        swipeHelper = new ItemTouchHelper(new SwipeCallback(requireContext()) {

            @Override
            protected void onHeaderSwipeDelete(String tituloDia, List<MovimentacaoModel> movimentos) {
                confirmarExclusaoDoDia(tituloDia, movimentos);
            }

            @Override
            protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                             int direction, int position) {
                // No dashboard só temos a lista processada (listaProcessada local ao observer).
                // Precisamos acessar pelo adapter.
                if (adapter == null) return;
                AdapterItemListaMovimentacao item = adapter.getItens().get(position);
                if (item.type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {

                    MovimentacaoModel m = item.movimentacaoModel;

                    if (direction == ItemTouchHelper.LEFT) {
                        exibirDialogExclusao(m);
                        adapter.notifyItemChanged(position);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        adapter.notifyItemChanged(position);
                        abrirTelaEdicao(m, true); // <<< AQUI ESTÁ A MÁGICA
                    }

                } else {
                    adapter.notifyItemChanged(position);
                }
            }
        });

        swipeHelper.attachToRecyclerView(recyclerView);
    }

    private void confirmarExclusaoDoDia(String tituloDia, List<MovimentacaoModel> movimentos) {
        if (!isAdded() || movimentos == null || movimentos.isEmpty()) return;

        int total = movimentos.size();

        boolean temRecorrentes = false;
        for (MovimentacaoModel m : movimentos) {
            if (m.getTotal_parcelas() > 1) { temRecorrentes = true; break; }
        }

        String msgPrincipal = "Deseja excluir " + total +
                (total == 1 ? " movimentação" : " movimentações") +
                " de \"" + tituloDia + "\"?\n\nEsta ação não pode ser desfeita.";

        String msgFinal = temRecorrentes
                ? msgPrincipal + "\n\n⚠️ Atenção: há lançamentos recorrentes neste dia. " +
                "Apenas as parcelas deste dia serão removidas — as demais da série serão mantidas."
                : msgPrincipal;

        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir " + tituloDia)
                .setMessage(msgFinal)
                .setPositiveButton("Excluir tudo", (dialog, which) -> executarExclusaoDoDia(movimentos))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarExclusaoDoDia(List<MovimentacaoModel> movimentos) {
        viewModel.excluirEmLote(movimentos, new MovimentacaoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                // Recarrega ambas as abas para manter consistência
                viewModel.fetchDados(true, null);
                viewModel.fetchDados(false, null);
            }

            @Override
            public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) {
        confirmarExclusaoDoDia(dataDia, movsDoDia);
    }

}