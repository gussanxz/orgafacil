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
import java.util.Calendar;
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

                // Definir "Dia depois de hoje" (Amanhã 00:00)
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 1); // Avança para amanhã
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date dataAmanha = cal.getTime();

                for (MovimentacaoModel mov : listaCompleta) {
                    // Segurança contra nulos
                    if (mov.getData_movimentacao() == null) continue;

                    Date dataMov = mov.getData_movimentacao().toDate();

                    // LÓGICA:
                    // 1. Ser do tipo DESPESA
                    // 2. Data ser igual ou posterior a Amanhã (!before significa >=)
                    if (mov.getTipo() == TipoCategoriaContas.DESPESA.getId() &&
                            !dataMov.before(dataAmanha)) {

                        listaFiltrada.add(mov);
                    }
                }

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

    // --- Métodos de Click (Mantidos iguais) ---
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

        textMensagem.setText("Dar baixa/Pagar '" + mov.getDescricao() + "'?");
        btnConfirmar.setText("Pagar");

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