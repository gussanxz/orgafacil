package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.contas.contas_a_pagar.visual.FragmentContasAVencer;

public class DashboardPagerAdapter extends FragmentStateAdapter {

    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Posição 0 = Esquerda (Movimentações)
        // Posição 1 = Direita (Contas Futuras)
        if (position == 0) {
            return new FragmentMovimentacoes();
        } else {
            return new FragmentContasAVencer();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Temos 2 abas
    }
}