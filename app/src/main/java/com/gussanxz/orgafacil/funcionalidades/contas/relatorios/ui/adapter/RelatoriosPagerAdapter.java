package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.EvolucaoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.ExportacaoRelatorioFragment;
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
                return new ResumoRelatorioFragment();
            case 1:
                return new EvolucaoRelatorioFragment();
            case 2:
                return new PlanejamentoRelatorioFragment();
            case 3:
                return new ExportacaoRelatorioFragment();
            default:
                return new ResumoRelatorioFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Agora temos 4 abas exatas
    }
}