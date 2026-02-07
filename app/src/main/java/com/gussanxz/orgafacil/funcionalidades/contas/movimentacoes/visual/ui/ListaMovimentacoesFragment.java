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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.ContasViewModel;

import java.util.List;

/**
 * ListaMovimentacoesFragment (UNIFICADO)
 * Este fragmento substitui o FragmentMovimentacoes e FragmentContasAVencer.
 * Ele decide o que exibir com base no argumento 'MODO_FUTURO'.
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
        // Usamos requireActivity() para que todos os fragmentos compartilhem o MESMO ViewModel da Activity principal
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

    /**
     * Observa o ViewModel. Quando os dados mudarem no banco, o ViewModel filtra
     * e o fragmento apenas "reage" atualizando a lista.
     */
    private void setupObservers() {
        viewModel.listaFiltrada.observe(getViewLifecycleOwner(), lista -> {
            // [CORREÇÃO]: Passando 'ehModoFuturo' para o Helper decidir a ordem
            List<ExibirItemListaMovimentacaoContas> listaProcessada =
                    HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(lista, ehModoFuturo);

            adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, this);
            recyclerView.setAdapter(adapter);
        });
    }

    private void carregarDados() {
        // Configura o "modo" no ViewModel antes de disparar a busca
        viewModel.setModoContasFuturas(ehModoFuturo);

        viewModel.fetchDados(repository, new MovimentacaoRepository.DadosCallback() {
            @Override public void onSucesso(List<MovimentacaoModel> lista) { /* Observado pelo LiveData */ }
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
            String acao = (mov.getTipo() == TipoCategoriaContas.DESPESA.getId()) ? "Pagar" : "Receber";
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
                    // Após excluir, pedimos ao ViewModel para recarregar
                    carregarDados();
                }
                @Override public void onErro(String erro) { }
            });
        });
        dialog.show();
    }
}