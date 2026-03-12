package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter.RelatoriosPagerAdapter;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.EvolucaoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.ExportacaoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.PlanejamentoRelatorioFragment;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.fragments.ResumoRelatorioFragment;

public class RelatoriosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_relatorios);

        carregarFragment(new ResumoRelatorioFragment());

        findViewById(R.id.btnResumo)
                .setOnClickListener(v ->
                        carregarFragment(new ResumoRelatorioFragment()));

        findViewById(R.id.btnEvolucao)
                .setOnClickListener(v ->
                        carregarFragment(new EvolucaoRelatorioFragment()));

        findViewById(R.id.btnPlanejamento)
                .setOnClickListener(v ->
                        carregarFragment(new PlanejamentoRelatorioFragment()));

        findViewById(R.id.btnExportar)
                .setOnClickListener(v ->
                        carregarFragment(new ExportacaoRelatorioFragment()));
    }

    private void carregarFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerRelatorios, fragment)
                .commit();
    }
}