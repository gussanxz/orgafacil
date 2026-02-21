package com.gussanxz.orgafacil.funcionalidades.vendas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.RegistrarVendasActivity;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

import java.util.Locale;

public class ResumoVendasActivity extends AppCompatActivity {
    private final String TAG = "ResumoVendasActivity";

    // Mantendo a precisão monetária (inteiro = centavos)
    private int saldoVendasHojeCentavos = 15000;

    // Componentes de UI
    private TabLayout tabLayoutDashboard;
    private ViewPager2 viewPagerDashboard;

    // Componentes do Menu Flutuante e Efeitos
    private FloatingActionButton fabMain, fabNovaVenda, fabNovoOrcamento, fabNovoCliente;
    private View overlayBackground, radialSpotlight;

    // Controle de animação igual ao de Contas
    private boolean isMenuOpen = false;
    private final OvershootInterpolator interpolator = new OvershootInterpolator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();
        configurarSaldo();
        configurarAbas();
        configurarFabMenu();
    }

    private void inicializarComponentes() {
        tabLayoutDashboard = findViewById(R.id.tabLayoutDashboard);
        viewPagerDashboard = findViewById(R.id.viewPagerDashboard);

        fabMain = findViewById(R.id.fab_main);
        fabNovaVenda = findViewById(R.id.fab_nova_venda);
        fabNovoOrcamento = findViewById(R.id.fab_novo_orcamento);
        fabNovoCliente = findViewById(R.id.fab_novo_cliente);

        overlayBackground = findViewById(R.id.overlay_background);
        radialSpotlight = findViewById(R.id.radial_spotlight);
    }

    private void configurarSaldo() {
        TextView textVendasHoje = findViewById(R.id.textVendasHoje);
        ImageView imgOlhoSaldo = findViewById(R.id.imgOlhoSaldo);

        double saldoExibicao = saldoVendasHojeCentavos / 100.0;
        String saldoFormatado = String.format(Locale.getDefault(), "R$ %.2f", saldoExibicao);

        if (textVendasHoje != null) textVendasHoje.setText(saldoFormatado);

        if (imgOlhoSaldo != null && textVendasHoje != null) {
            imgOlhoSaldo.setTag(true);
            imgOlhoSaldo.setOnClickListener(view -> {
                VisibilidadeHelper.alternarVisibilidadeSaldo(textVendasHoje, imgOlhoSaldo, saldoFormatado);
            });
        }
    }

    private void configurarAbas() {
        VendasPagerAdapter adapter = new VendasPagerAdapter(this);
        viewPagerDashboard.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutDashboard, viewPagerDashboard, (tab, position) -> {
            if (position == 0) {
                tab.setText("Operações");
            } else {
                tab.setText("Gestão");
            }
        }).attach();
    }

    // --- LÓGICA DO MENU EXPANSÍVEL (SPEED DIAL) ---

    private void configurarFabMenu() {
        if (fabMain != null) {
            fabMain.setOnClickListener(v -> {
                if (!isMenuOpen) abrirMenu();
                else fecharMenu();
            });
        }

        if (overlayBackground != null) {
            overlayBackground.setOnClickListener(v -> {
                if (isMenuOpen) fecharMenu();
            });
        }

        if (fabNovaVenda != null) {
            fabNovaVenda.setOnClickListener(v -> {
                acaoRapida("Abrindo Nova Venda");
                acessarRegistrarVendasActivity(v);
            });
        }

        if (fabNovoOrcamento != null) {
            fabNovoOrcamento.setOnClickListener(v -> {
                acaoRapida("Acesso a Novo Orçamento (Futuro)");
            });
        }

        if (fabNovoCliente != null) {
            fabNovoCliente.setOnClickListener(v -> {
                acaoRapida("Acesso a Novo Cliente (Futuro)");
            });
        }
    }

    private void abrirMenu() {
        isMenuOpen = true;

        // Escurecer fundo
        overlayBackground.setVisibility(View.VISIBLE);
        overlayBackground.animate().alpha(1f).setDuration(300).start();

        // Expandir o Spotlight
        if (radialSpotlight != null) {
            radialSpotlight.setVisibility(View.VISIBLE);
            radialSpotlight.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(180f)
                    .setInterpolator(interpolator)
                    .setDuration(400)
                    .start();
        }

        // GARANTE QUE O BOTÃO ROSA PRINCIPAL FIQUE POR CIMA DO BRILHO
        fabMain.bringToFront();

        // Girar o botão principal em 45 graus
        fabMain.animate().setInterpolator(interpolator).rotation(45f).setDuration(300).start();

        // ... (o resto das animações continua igual)
        animarBotao(fabNovaVenda, -300f, -280f);
        animarBotao(fabNovoOrcamento, 0f, -420f);
        animarBotao(fabNovoCliente, 300f, -280f);
    }

    private void fecharMenu() {
        isMenuOpen = false;

        // Esconder fundo
        overlayBackground.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> overlayBackground.setVisibility(View.GONE)).start();

        // Recolher o Spotlight
        if (radialSpotlight != null) {
            radialSpotlight.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(300)
                    .withEndAction(() -> radialSpotlight.setVisibility(View.INVISIBLE)).start();
        }

        // Voltar botão principal
        fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start();

        // Recolher botões secundários
        recolherBotao(fabNovaVenda);
        recolherBotao(fabNovoOrcamento);
        recolherBotao(fabNovoCliente);
    }

    private void animarBotao(View view, float x, float y) {
        // 1. Torna visível e joga o botão para a camada mais alta
        view.setVisibility(View.VISIBLE);
        view.bringToFront();

        // 2. Pega a posição exata em que o botão rosa está parado (devido ao -40dp do XML)
        float centroX = fabMain.getTranslationX();
        float centroY = fabMain.getTranslationY();

        // 3. Força o botão menor a sair de trás do botão rosa
        view.setTranslationX(centroX);
        view.setTranslationY(centroY);
        view.setAlpha(0f);

        // 4. Dispara a animação
        view.animate()
                .translationX(x)
                .translationY(y)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start();
    }

    private void recolherBotao(View view) {
        // Pega a posição exata do botão rosa novamente
        float centroX = fabMain.getTranslationX();
        float centroY = fabMain.getTranslationY();

        // Retorna a animação escondendo exatamente atrás do botão rosa
        view.animate()
                .translationX(centroX)
                .translationY(centroY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .withEndAction(() -> view.setVisibility(View.INVISIBLE))
                .start();
    }

    private void acaoRapida(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        fecharMenu();
    }

    public void acessarRegistrarVendasActivity(View view) {
        startActivity(new Intent(this, RegistrarVendasActivity.class));
        Log.i(TAG, "Acessou RegistrarVendasActivity");
    }
}