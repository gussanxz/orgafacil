package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterItemListaMovimentacao;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.adapter.AdapterMovimentacaoLista;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.ContasDialogHelper;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.helper.HelperExibirDatasMovimentacao;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ListaMovimentacoesFragment (VITRINE DO DASHBOARD)
 *
 * Exibe as últimas movimentações (histórico) ou as contas a vencer (futuras)
 * dependendo de como foi instanciado via newInstance(ehModoFuturo).
 *
 * REFATORAÇÃO — todos os dialogs inline foram removidos.
 * Esta classe não contém mais nenhuma lógica de AlertDialog.
 * Tudo é delegado ao ContasDialogHelper, igual ao que ContasActivity já fazia.
 *
 * MÉTODOS REMOVIDOS:
 *   - exibirDialogExclusao()       → ContasDialogHelper.confirmarOuExcluirMovimento()
 *   - confirmarExclusaoDoDia()     → ContasDialogHelper.confirmarExclusaoDoDia()
 *   - onCheckClick() inline (listener anônimo do adapter) → removido; só existe o @Override
 *
 * MÉTODOS MANTIDOS (lógica é deste Fragment, não do helper):
 *   - executarConfirmacao()        — chama o ViewModel e recarrega as abas
 *   - executarConfirmacaoEmMassa() — chama o ViewModel e recarrega as abas
 *   - executarExclusao()           — chama o ViewModel e recarrega as abas
 *   - executarExclusaoDoDia()      — chama o ViewModel e recarrega as abas
 */
public class ListaMovimentacoesFragment extends Fragment
        implements AdapterMovimentacaoLista.OnItemActionListener {

    private static final String ARG_MODO_FUTURO = "modo_futuro";

    private RecyclerView recyclerView;
    private AdapterMovimentacaoLista adapter;
    private ContasViewModel viewModel;
    private boolean ehModoFuturo;

    private View layoutEmptyState;
    private TextView textEmptyState;
    private ProgressBar progressBarFragment;
    private boolean isPrimeiroCarregamento = true;

    private ItemTouchHelper swipeHelper;

    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> launcherEdicao;

    // ── Fábrica ───────────────────────────────────────────────────────────────

    public static ListaMovimentacoesFragment newInstance(boolean exibirFuturas) {
        ListaMovimentacoesFragment fragment = new ListaMovimentacoesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_MODO_FUTURO, exibirFuturas);
        fragment.setArguments(args);
        return fragment;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ehModoFuturo = getArguments().getBoolean(ARG_MODO_FUTURO);
        }
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);

        recyclerView        = view.findViewById(R.id.recyclerInterno);
        layoutEmptyState    = view.findViewById(R.id.layoutEmptyState);
        textEmptyState      = view.findViewById(R.id.textEmptyState);
        progressBarFragment = view.findViewById(R.id.progressBarFragment);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        textEmptyState.setText(ehModoFuturo
                ? "Ufa! Nenhuma conta pendente. \uD83C\uDF89"
                : "Nenhum histórico no momento.");

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
        carregarDados();
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private void setupObservers() {
        viewModel.carregandoPaginacao.observe(getViewLifecycleOwner(), isCarregando -> {
            if (isCarregando) {
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
            if (isPrimeiroCarregamento
                    && Boolean.TRUE.equals(viewModel.carregandoPaginacao.getValue())) return;

            if (lista == null || lista.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                return;
            }

            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

            // Vitrine: exibe no máximo 10 itens
            List<MovimentacaoModel> listaResumo = new ArrayList<>();
            int limite = Math.min(lista.size(), 10);
            for (int i = 0; i < limite; i++) listaResumo.add(lista.get(i));

            List<AdapterItemListaMovimentacao> listaProcessada =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(listaResumo, ehModoFuturo);

            if (adapter == null) {
                adapter = new AdapterMovimentacaoLista(getContext(), this);
                recyclerView.setAdapter(adapter);
                configurarSwipeDoDashboard();
            }

            boolean listaEstavaVazia = adapter.getCurrentList().isEmpty();
            if (listaEstavaVazia && !listaProcessada.isEmpty()) {
                recyclerView.scheduleLayoutAnimation();
            }

            adapter.submitList(listaProcessada);
        };

        viewModel.listaHistorico.removeObservers(getViewLifecycleOwner());
        viewModel.listaFutura.removeObservers(getViewLifecycleOwner());

        if (ehModoFuturo) {
            viewModel.listaFutura.observe(getViewLifecycleOwner(), observerUI);
        } else {
            viewModel.listaHistorico.observe(getViewLifecycleOwner(), observerUI);
        }
    }

    // ── Carregamento ──────────────────────────────────────────────────────────

    private void carregarDados() {
        viewModel.fetchDados(ehModoFuturo, new MovimentacaoRepository.DadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> lista) { /* UI via Observer */ }
            @Override public void onErro(String erro) {
                if (isAdded()) Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recarregarAmbas() {
        viewModel.fetchDados(true, null);
        viewModel.fetchDados(false, null);
    }

    // ── OnItemActionListener — delegação para o helper ────────────────────────

    /**
     * Swipe esquerda ou botão de lixeira.
     * Delegado ao helper com suporte ao modo da aba (futuro = confirmar, histórico = excluir).
     */
    @Override
    public void onDeleteClick(MovimentacaoModel mov) {
        // ANTES: dialog inline com LayoutInflater manual (30 linhas)
        // AGORA: uma linha
        ContasDialogHelper.confirmarOuExcluirMovimento(
                requireContext(), mov, ehModoFuturo,
                new ContasDialogHelper.AcaoUnicaCallback() {
                    @Override public void onConfirmar() {
                        if (ehModoFuturo) {
                            executarConfirmacao(mov);
                        } else {
                            executarExclusao(mov);
                        }
                    }
                });
    }

    /**
     * Long press — abre tela de edição com confirmação.
     */
    @Override
    public void onLongClick(MovimentacaoModel mov) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Editar")
                .setMessage("Deseja editar '" + mov.getDescricao() + "'?")
                .setPositiveButton("Sim", (d, w) -> abrirTelaEdicao(mov, false))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Botão de check (confirmar pagamento/recebimento).
     * ANTES: AlertDialog inline duplicado em dois lugares neste arquivo + duplicado da ContasActivity.
     * AGORA: delegado ao helper, idêntico ao que ContasActivity já faz.
     */
    @Override
    public void onCheckClick(MovimentacaoModel mov) {
        ContasDialogHelper.confirmarPagamentoOuRecebimento(
                requireContext(), mov,
                new ContasDialogHelper.AcaoMultiplaCallback() {
                    @Override public void onApenasEsta()      { executarConfirmacao(mov); }
                    @Override public void onEstaESeguintes()  { executarConfirmacaoEmMassa(mov); }
                });
    }

    /**
     * Swipe no header de dia — exclui todas as movimentações daquele dia.
     * ANTES: AlertDialog inline (20 linhas).
     * AGORA: delegado ao helper.
     */
    @Override
    public void onHeaderSwipeDelete(String dataDia, List<MovimentacaoModel> movsDoDia) {
        if (!isAdded()) return;
        ContasDialogHelper.confirmarExclusaoDoDia(
                requireContext(), dataDia, movsDoDia,
                new ContasDialogHelper.AcaoUnicaCallback() {
                    @Override public void onConfirmar() { executarExclusaoDoDia(movsDoDia); }
                });
    }

    /**
     * Clique simples no header do dia — exibe o resumo financeiro em popup.
     */
    @Override
    public void onHeaderClick(String tituloDia, List<MovimentacaoModel> movsDoDia) {
        if (!isAdded()) return;
        exibirPopupResumoDia(tituloDia, movsDoDia);
    }

    private void exibirPopupResumoDia(String tituloDia, List<MovimentacaoModel> movsDoDia) {
        // 1. Cria o Dialog usando o contexto seguro do Fragment
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_resumo_dia);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // 2. Mapeia os componentes
        TextView txtTitulo = dialog.findViewById(R.id.textTituloDialogResumo);
        TextView txtQtdReceitas = dialog.findViewById(R.id.textQtdReceitas);
        TextView txtValorReceitas = dialog.findViewById(R.id.textValorReceitas);
        TextView txtQtdDespesas = dialog.findViewById(R.id.textQtdDespesas);
        TextView txtValorDespesas = dialog.findViewById(R.id.textValorDespesas);
        TextView txtSaldoFinal = dialog.findViewById(R.id.textSaldoDialog);
        com.google.android.material.button.MaterialButton btnVoltar = dialog.findViewById(R.id.btnVoltarResumo);

        txtTitulo.setText("Resumo: " + tituloDia);

        // 3. Matemática Financeira Exata (Usando LONG para centavos, sem double)
        long totalReceitas = 0;
        long totalDespesas = 0;
        int qtdReceitas = 0;
        int qtdDespesas = 0;

        for (MovimentacaoModel mov : movsDoDia) {
            if (mov.getTipoEnum() == com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas.RECEITA) {
                totalReceitas += mov.getValor();
                qtdReceitas++;
            } else {
                totalDespesas += mov.getValor();
                qtdDespesas++;
            }
        }

        long saldoFinalLong = totalReceitas - totalDespesas;

        // 4. Formatação para Reais (Dividindo por 100.0 apenas na exibição visual)
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));

        txtQtdReceitas.setText(qtdReceitas + (qtdReceitas == 1 ? " Receita" : " Receitas"));
        txtValorReceitas.setText("+ " + currencyFormat.format(totalReceitas / 100.0));

        txtQtdDespesas.setText(qtdDespesas + (qtdDespesas == 1 ? " Despesa" : " Despesas"));
        txtValorDespesas.setText("- " + currencyFormat.format(totalDespesas / 100.0));

        txtSaldoFinal.setText(currencyFormat.format(saldoFinalLong / 100.0));

        // Cor do saldo final
        if (saldoFinalLong > 0) {
            txtSaldoFinal.setTextColor(android.graphics.Color.parseColor("#008000"));
        } else if (saldoFinalLong < 0) {
            txtSaldoFinal.setTextColor(android.graphics.Color.parseColor("#E53935"));
        } else {
            txtSaldoFinal.setTextColor(android.graphics.Color.parseColor("#757575"));
        }

        // 5. Botão de fechar
        btnVoltar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ── Ações sobre o ViewModel (lógica exclusiva deste Fragment) ─────────────

    private void executarConfirmacao(MovimentacaoModel mov) {
        viewModel.confirmarMovimentacao(mov, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Concluído!", Toast.LENGTH_SHORT).show();
                recarregarAmbas();
            }
            @Override public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarConfirmacaoEmMassa(MovimentacaoModel movBase) {
        viewModel.confirmarMovimentacaoEmMassa(movBase, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                recarregarAmbas();
            }
            @Override public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarExclusao(MovimentacaoModel mov) {
        viewModel.excluir(mov, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (!isAdded()) return;
                recarregarAmbas();
            }
            @Override public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executarExclusaoDoDia(List<MovimentacaoModel> movimentos) {
        viewModel.excluirEmLote(movimentos, new MovimentacaoRepository.Callback() {
            @Override public void onSucesso(String msg) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                recarregarAmbas();
            }
            @Override public void onErro(String erro) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    private void abrirTelaEdicao(MovimentacaoModel m, boolean direto) {
        android.content.Intent intent = new android.content.Intent(requireContext(),
                com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities
                        .EditarMovimentacaoActivity.class);
        intent.putExtra("movimentacaoSelecionada", m);
        intent.putExtra("DIRETO_PRA_EDICAO", direto);
        launcherEdicao.launch(intent);
    }

    // ── Swipe ─────────────────────────────────────────────────────────────────

    private void configurarSwipeDoDashboard() {
        if (swipeHelper != null) swipeHelper.attachToRecyclerView(null);

        swipeHelper = new ItemTouchHelper(new SwipeCallback(requireContext()) {

            @Override
            protected void onHeaderSwipeDelete(String tituloDia, List<MovimentacaoModel> movimentos) {
                // Delegado para o @Override do OnItemActionListener acima
                ListaMovimentacoesFragment.this.onHeaderSwipeDelete(tituloDia, movimentos);
            }

            @Override
            protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                             int direction, int position) {
                if (adapter == null) return;
                AdapterItemListaMovimentacao item = adapter.getCurrentList().get(position);

                if (item.type == AdapterItemListaMovimentacao.TYPE_MOVIMENTO) {
                    MovimentacaoModel m = item.movimentacaoModel;

                    if (direction == ItemTouchHelper.LEFT) {
                        // Delegado para o @Override onDeleteClick acima (que usa o helper)
                        onDeleteClick(m);
                        adapter.notifyItemChanged(position);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        adapter.notifyItemChanged(position);
                        abrirTelaEdicao(m, true);
                    }
                } else {
                    adapter.notifyItemChanged(position);
                }
            }
        });

        swipeHelper.attachToRecyclerView(recyclerView);
    }
}