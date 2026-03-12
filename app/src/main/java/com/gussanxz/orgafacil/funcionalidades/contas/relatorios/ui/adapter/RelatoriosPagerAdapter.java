package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.EvolucaoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.PlanejamentoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.ResumoRelatorioFragment;

public class RelatoriosPagerAdapter extends FragmentStateAdapter {

    public RelatoriosPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ResumoRelatorioFragment(); // Nossa Aba 1
            case 1:
                return new EvolucaoRelatorioFragment(); // Placeholder Aba 2 (Evolução)
            case 2:
                return new PlanejamentoRelatorioFragment();
            default:
                return new ResumoRelatorioFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Nossas 3 abas planejadas
    }
}