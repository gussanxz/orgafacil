package com.gussanxz.orgafacil.funcionalidades.vendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.VendasCadastrosActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.ListaProdutosEServicosActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.gestaoerelatorios.VendasEmAbertoActivity;

public class OperacoesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Conecta o arquivo XML que acabamos de criar
        return inflater.inflate(R.layout.fragment_operacoes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Mapeando os botões do layout
        View cardCadastros     = view.findViewById(R.id.cardCadastros);
        View cardPedidosAbertos = view.findViewById(R.id.cardPedidosAbertos);
        View cardCatalogo      = view.findViewById(R.id.cardCatalogo);
        View overlayCatalogo   = view.findViewById(R.id.overlayCatalogo);

        if (cardCadastros != null) {
            cardCadastros.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), VendasCadastrosActivity.class)));
        }

        if (cardPedidosAbertos != null) {
            cardPedidosAbertos.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), VendasEmAbertoActivity.class)));
        }

        // Catálogo agora disponível — remove o overlay e habilita navegação
        if (overlayCatalogo != null) overlayCatalogo.setVisibility(View.GONE);
        if (cardCatalogo != null) {
            cardCatalogo.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), ListaProdutosEServicosActivity.class)));
        }
    }
}