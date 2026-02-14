package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.ListaMovimentacoesFragment;

/**
 * DashboardPagerAdapter (Corrigido para ViewPager2)
 * Garante a criação correta e única dos fragmentos de Histórico e Futuro.
 */
public class DashboardPagerAdapter extends FragmentStateAdapter {

    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Posição 0: Aba "Últimas Movimentações" (Histórico -> ehModoFuturo = false)
        // Posição 1: Aba "Contas a Vencer" (Futuras -> ehModoFuturo = true)

        boolean ehModoFuturo = (position == 1);
        return ListaMovimentacoesFragment.newInstance(ehModoFuturo);
    }

    @Override
    public int getItemCount() {
        // Sempre temos exatamente 2 abas fixas
        return 2;
    }
}