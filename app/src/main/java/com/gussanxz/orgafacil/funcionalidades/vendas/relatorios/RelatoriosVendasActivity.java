package com.gussanxz.orgafacil.funcionalidades.vendas.relatorios;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gussanxz.orgafacil.R;

public class RelatoriosVendasActivity extends AppCompatActivity {

    private ViewPager2 viewPagerRelatorios;
    private View btnResumo, btnEvolucao;
    private final int[] botaoIds = { R.id.btnRelVendasResumo, R.id.btnRelVendasEvolucao };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_relatorios);

        viewPagerRelatorios = findViewById(R.id.viewPagerRelVendas);
        btnResumo   = findViewById(R.id.btnRelVendasResumo);
        btnEvolucao = findViewById(R.id.btnRelVendasEvolucao);

        RelatoriosVendasPagerAdapter adapter = new RelatoriosVendasPagerAdapter(this);
        viewPagerRelatorios.setAdapter(adapter);
        viewPagerRelatorios.setOffscreenPageLimit(2);

        btnResumo.setOnClickListener(v   -> mudarAba(0));
        btnEvolucao.setOnClickListener(v -> mudarAba(1));

        viewPagerRelatorios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                atualizarBotaoAtivo(position);
            }
        });

        TabLayout tabPontinhos = findViewById(R.id.tabPontinhosRelVendas);
        new TabLayoutMediator(tabPontinhos, viewPagerRelatorios, (tab, position) -> {}).attach();

        atualizarBotaoAtivo(0);
    }

    private void mudarAba(int posicao) {
        viewPagerRelatorios.setCurrentItem(posicao, false);
    }

    private void atualizarBotaoAtivo(int posicaoAtiva) {
        View[] botoes = { btnResumo, btnEvolucao };
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i] == null) continue;
            botoes[i].setAlpha(i == posicaoAtiva ? 1.0f : 0.45f);
        }
    }

    public void retornarPrincipal(View view) {
        finish();
    }
}