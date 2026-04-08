package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments.EvolucaoVendasFragment;
import com.gussanxz.orgafacil.funcionalidades.vendas.relatorios.fragments.ResumoVendasRelatorioFragment;

public class RelatoriosVendasPagerAdapter extends FragmentStateAdapter {

    public RelatoriosVendasPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new EvolucaoVendasFragment();
            default: return new ResumoVendasRelatorioFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}