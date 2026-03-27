package com.gussanxz.orgafacil.funcionalidades.vendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HistoricoVendasActivity;

public class GestaoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Conecta o arquivo XML correspondente
        return inflater.inflate(R.layout.fragment_gestao, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurar botão de Vendas (O único que não está bloqueado na sua imagem/código base)
        View cardVendas = view.findViewById(R.id.cardVendas);
        if (cardVendas != null) {
            cardVendas.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), HistoricoVendasActivity.class));
            });
        }

        // 2. Configurar os botões com cadeado (Funcionalidade futura)
        configurarBotoesBloqueados(view);
    }

    private void configurarBotoesBloqueados(View parentView) {
        // Listener único para otimizar memória (mesma lógica excelente do seu código antigo)
        View.OnClickListener listenerBloqueio = v -> {
            Toast.makeText(requireContext(), "Funcionalidade futura", Toast.LENGTH_SHORT).show();
        };

        // Lista de IDs dos overlays bloqueados nesta tela
        int[] idsBloqueados = {
                R.id.overlayEstoque,
                R.id.overlayDevolucoes,
                R.id.overlayRelatorios,
                R.id.overlayControleCaixa,
                R.id.overlayFinanceiro
        };

        // Aplica o clique em cada um
        for (int id : idsBloqueados) {
            View overlay = parentView.findViewById(id);
            if (overlay != null) {
                overlay.setOnClickListener(listenerBloqueio);
            }
        }
    }
}