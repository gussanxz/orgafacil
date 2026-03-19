package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter.RelatoriosPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class RelatoriosActivity extends AppCompatActivity {

    private ViewPager2 viewPagerRelatorios;

    // Botões do menu inferior
    private View btnResumo, btnEvolucao, btnPlanejamento, btnExportar;

    // IDs dos botões na ordem das abas
    private final int[] botaoIds = {
            R.id.btnResumo, R.id.btnEvolucao, R.id.btnPlanejamento, R.id.btnExportar
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_relatorios);

        viewPagerRelatorios = findViewById(R.id.viewPagerRelatorios);

        btnResumo       = findViewById(R.id.btnResumo);
        btnEvolucao     = findViewById(R.id.btnEvolucao);
        btnPlanejamento = findViewById(R.id.btnPlanejamento);
        btnExportar     = findViewById(R.id.btnExportar);

        RelatoriosPagerAdapter pagerAdapter = new RelatoriosPagerAdapter(this);
        viewPagerRelatorios.setAdapter(pagerAdapter);

        // CORREÇÃO: offscreenPageLimit reduzido para 2 — mantém apenas as
        // abas adjacentes em memória, não todas as 4 simultaneamente.
        // O Fragment da aba ativa + 1 de cada lado é suficiente para troca suave.
        viewPagerRelatorios.setOffscreenPageLimit(2);

        // Botões do menu inferior
        btnResumo.setOnClickListener(v -> mudarAba(0));
        btnEvolucao.setOnClickListener(v -> mudarAba(1));
        btnPlanejamento.setOnClickListener(v -> mudarAba(2));
        btnExportar.setOnClickListener(v -> mudarAba(3));

        // CORREÇÃO: sincroniza o estado visual do botão ativo com o swipe
        viewPagerRelatorios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                atualizarBotaoAtivo(position);
            }
        });

        TabLayout tabPontinhos = findViewById(R.id.tabPontinhos);
        new TabLayoutMediator(tabPontinhos, viewPagerRelatorios, (tab, position) -> {
            // Apenas bolinhas, sem texto
        }).attach();

        // Marca o primeiro botão como ativo ao abrir
        atualizarBotaoAtivo(0);
    }

    private void mudarAba(int posicao) {
        viewPagerRelatorios.setCurrentItem(posicao, false);
        // atualizarBotaoAtivo() é chamado pelo onPageSelected acima
    }

    /**
     * CORREÇÃO: atualiza visualmente qual botão do menu inferior está ativo.
     *
     * Antes: nenhum botão indicava qual aba estava selecionada — o usuário
     * não sabia em qual tela estava após navegar pelo swipe.
     *
     * Agora: o botão da aba ativa fica com alpha 1.0 (destacado) e os
     * demais ficam com alpha 0.45 (esmaecidos), criando contraste claro.
     */
    private void atualizarBotaoAtivo(int posicaoAtiva) {
        View[] botoes = {btnResumo, btnEvolucao, btnPlanejamento, btnExportar};
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i] == null) continue;
            if (i == posicaoAtiva) {
                // Ativo: totalmente visível
                botoes[i].setAlpha(1.0f);
            } else {
                // Inativo: esmaecido para criar contraste
                botoes[i].setAlpha(0.45f);
            }
        }
    }

    public void retornarPrincipal(View view) {
        finish();
    }
}