package com.gussanxz.orgafacil.funcionalidades.vendas;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class VendasPagerAdapter extends FragmentStateAdapter {

    public VendasPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Posição 0 é a primeira aba (Operações)
        if (position == 0) {
            return new OperacoesFragment();
        }
        // Posição 1 é a segunda aba (Gestão)
        return new GestaoFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // Temos 2 abas no total
    }
}