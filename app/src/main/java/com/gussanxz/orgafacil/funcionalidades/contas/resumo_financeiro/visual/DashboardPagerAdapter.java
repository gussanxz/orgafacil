package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ListaMovimentacoesFragment;

/**
 * DashboardPagerAdapter (Refatorado)
 * Utiliza o Fragmento Unificado ListaMovimentacoesFragment para exibir
 * diferentes conjuntos de dados (Histórico e Futuras) no ViewPager.
 */
public class DashboardPagerAdapter extends FragmentStateAdapter {

    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // [CLEAN CODE]: Reutilizamos o mesmo Fragmento com configurações diferentes.
        // Posição 0 = Esquerda (Histórico de Movimentações -> ehModoFuturo = false)
        // Posição 1 = Direita (Contas a Vencer/Futuras -> ehModoFuturo = true)

        if (position == 0) {
            // Cria instância para o Histórico
            return ListaMovimentacoesFragment.newInstance(false);
        } else {
            // Cria instância para as Contas Futuras
            return ListaMovimentacoesFragment.newInstance(true);
        }
    }

    @Override
    public int getItemCount() {
        // Mantemos as 2 abas para o efeito de slide
        return 2;
    }
}