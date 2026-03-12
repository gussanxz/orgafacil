package com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter.RelatoriosPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class RelatoriosActivity extends AppCompatActivity {

    private ViewPager2 viewPagerRelatorios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_relatorios);

        // 1. Inicializando o ViewPager2
        // IMPORTANTE: Certifique-se de que no seu XML (tela_relatorios.xml) o ID está como viewPagerRelatorios
        viewPagerRelatorios = findViewById(R.id.viewPagerRelatorios);
        RelatoriosPagerAdapter pagerAdapter = new RelatoriosPagerAdapter(this);
        viewPagerRelatorios.setAdapter(pagerAdapter);

        // O pulo do gato para a performance:
        // O limite '3' garante que todas as nossas 4 abas fiquem "congeladas" na memória prontas para uso.
        viewPagerRelatorios.setOffscreenPageLimit(3);

        // 2. Conectando os botões do Menu Inferior ao ViewPager2
        findViewById(R.id.btnResumo).setOnClickListener(v -> mudarAba(0));
        findViewById(R.id.btnEvolucao).setOnClickListener(v -> mudarAba(1));
        findViewById(R.id.btnPlanejamento).setOnClickListener(v -> mudarAba(2));
        findViewById(R.id.btnExportar).setOnClickListener(v -> mudarAba(3));

        // 3. (Opcional) Sincronizar deslize com a UI
        // Se o usuário arrastar a tela com o dedo, você pode querer mudar a cor do botão ativo no rodapé.
        viewPagerRelatorios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // No futuro, podemos adicionar a lógica de "pintar" o botão ativo de branco e os inativos de cinza aqui!
            }
        });

        TabLayout tabPontinhos = findViewById(R.id.tabPontinhos);
        new TabLayoutMediator(tabPontinhos, viewPagerRelatorios, (tab, position) -> {
            // Deixamos vazio porque queremos apenas as bolinhas, sem nenhum texto escrito nelas.
        }).attach();
    }

    private void mudarAba(int posicao) {
        // O 'false' faz a troca de tela ser instantânea ao clicar no botão.
        // Se colocar 'true', ele vai deslizar por todas as telas até chegar no destino.
        viewPagerRelatorios.setCurrentItem(posicao, false);
    }
}