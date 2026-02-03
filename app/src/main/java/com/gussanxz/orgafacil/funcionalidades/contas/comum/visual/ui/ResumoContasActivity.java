package com.gussanxz.orgafacil.funcionalidades.contas.comum.visual.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.ui.DashboardPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ResumoContasActivity extends AppCompatActivity {

    private static final String TAG = "ResumoContasActivity";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resumo_contas);

        // Configuração de Insets (Barras do sistema)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializar componentes
        tabLayout = findViewById(R.id.tabLayoutDashboard);
        viewPager = findViewById(R.id.viewPagerDashboard);

        setupSlideView();
    }
    public void acessarContasActivity(View view) {
        startActivity(new Intent(this, ContasActivity.class));
        Log.i(TAG, "acessou ContasActivity");
    }

    private void setupSlideView() {
        // 2. Criar e definir o Adaptador
        DashboardPagerAdapter adapter = new DashboardPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 3. Ligar as Abas ao ViewPager (Define os títulos)
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.tab_titulo_ultimas_mov); // Aba da Esquerda
            } else {
                tab.setText(R.string.tab_titulo_contas_a_vencer); // Aba da Direita
            }
        }).attach();
    }
}