package com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.visual.activity.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.ContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.visual.ResumoGeralViewModel;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.NumberFormat;
import java.util.Locale;

public class ResumoContasActivity extends AppCompatActivity {

    private static final String TAG = "ResumoContasActivity";

    // ViewModel e Dados
    private ResumoGeralViewModel viewModel;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // UI Dashboard (IDs mapeados do XML)
    private TextView textSaldoGeral; // Mapeado para R.id.textSaldo
    private TextView textReceitasMes; // Mapeado para R.id.textReceitasDashboard
    private TextView textDespesasMes; // Mapeado para R.id.textDespesasDashboard

    // Layout Components
    private LinearLayout btnFooterContas, btnFooterMovimentacoes;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomAppBar bottomAppBar;

    // Menu Radial
    private FloatingActionButton fabMain, fabDespesaFutura, fabNovaDespesa, fabNovaReceita, fabReceitaFutura;
    private boolean isMenuOpen = false;
    private final OvershootInterpolator interpolator = new OvershootInterpolator();
    private View overlayBackground;
    private View radialSpotlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumo_contas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializa componentes visuais
        inicializarComponentes();

        // 2. Inicializa e conecta o ViewModel
        viewModel = new ViewModelProvider(this).get(ResumoGeralViewModel.class);
        setupDashboardObserver();

        // 3. Configurações de layout (Tabs, Menu, Footer)
        setupSlideView();
        configurarBottomAppBarCustomizada();
        setupMenuRadial();

        overlayBackground.setOnClickListener(v -> fecharMenu());
    }

    /**
     * Vincula os IDs do XML às variáveis Java.
     */
    private void inicializarViewsDashboard() {
        // [CORREÇÃO]: Usando os IDs exatos do XML novo
        textSaldoGeral = findViewById(R.id.textSaldo);
        textReceitasMes = findViewById(R.id.textReceitasDashboard);
        textDespesasMes = findViewById(R.id.textDespesasDashboard);
    }

    private void inicializarComponentes() {
        inicializarViewsDashboard();

        tabLayout = findViewById(R.id.tabLayoutDashboard);
        viewPager = findViewById(R.id.viewPagerDashboard);
        overlayBackground = findViewById(R.id.overlay_background);
        bottomAppBar = findViewById(R.id.bottomAppBar);
    }

    /**
     * Escuta as mudanças do Firebase em tempo real e atualiza os textos.
     */
    private void setupDashboardObserver() {
        viewModel.resumoDados.observe(this, resumo -> {
            if (resumo == null) return;

            // Converter centavos para Double
            double saldo = resumo.getBalanco().getSaldoAtual() / 100.0;
            double receitas = resumo.getBalanco().getReceitasMes() / 100.0;
            double despesas = resumo.getBalanco().getDespesasMes() / 100.0;

            // Atualiza Texto Saldo
            if (textSaldoGeral != null) {
                textSaldoGeral.setText(currencyFormat.format(saldo));
                // Cor dinâmica: Verde se positivo, Vermelho se negativo
                if (saldo >= 0) textSaldoGeral.setTextColor(Color.WHITE);
                else textSaldoGeral.setTextColor(Color.parseColor("#FFCDD2")); // Vermelho claro p/ fundo escuro
            }

            // Atualiza Receitas e Despesas
            if (textReceitasMes != null) textReceitasMes.setText(currencyFormat.format(receitas));
            if (textDespesasMes != null) textDespesasMes.setText(currencyFormat.format(despesas));
        });
    }

    // --- Métodos de Configuração Visual (Mantidos) ---

    private void configurarBottomAppBarCustomizada() {
        bottomAppBar.setContentInsetsAbsolute(0, 0);
        View containerInterno = findViewById(R.id.linear_footer_container);
        if (containerInterno == null && bottomAppBar.getChildCount() > 0) {
            for(int i=0; i<bottomAppBar.getChildCount(); i++) {
                if (bottomAppBar.getChildAt(i) instanceof LinearLayout) {
                    containerInterno = bottomAppBar.getChildAt(i);
                    break;
                }
            }
        }
        if (containerInterno != null) {
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.MATCH_PARENT,
                    Toolbar.LayoutParams.MATCH_PARENT
            );
            params.gravity = Gravity.CENTER;
            containerInterno.setLayoutParams(params);
        }
        setupMenuFooter();
    }

    private void setupMenuFooter() {
        btnFooterMovimentacoes = findViewById(R.id.btn_footer_movimentacoes);
        btnFooterContas = findViewById(R.id.btn_footer_contas);

        btnFooterMovimentacoes.setOnClickListener(v -> {
            acaoRapida("Abrindo Contas");
            acessarContasActivity(v);
        });

        btnFooterContas.setOnClickListener(v -> {
            acaoRapida("Abrindo Contas Futuras");
            acessarContasFuturas(v);
        });
    }

    private void setupMenuRadial() {
        fabMain = findViewById(R.id.fab_main);
        fabDespesaFutura = findViewById(R.id.fab_despesa_futura);
        fabNovaDespesa = findViewById(R.id.fab_nova_despesa);
        fabNovaReceita = findViewById(R.id.fab_nova_receita);
        fabReceitaFutura = findViewById(R.id.fab_receita_futura);
        radialSpotlight = findViewById(R.id.radial_spotlight);

        fabMain.setOnClickListener(v -> {
            if (!isMenuOpen) abrirMenu();
            else fecharMenu();
        });

        fabDespesaFutura.setOnClickListener(v -> {
            acaoRapida("Abrindo Despesa Futura");
            adicionarDespesaFutura(v);
        });
        fabNovaDespesa.setOnClickListener(v -> {
            acaoRapida("Abrindo Nova Despesa");
            adicionarDespesa(v);
        });
        fabNovaReceita.setOnClickListener(v -> {
            acaoRapida("Abrindo Nova Receita");
            adicionarReceita(v);
        });
        fabReceitaFutura.setOnClickListener(v -> {
            acaoRapida("Abrindo Receita Futura");
            adicionarReceitaFutura(v);
        });
    }

    // --- Navegação ---

    public void adicionarReceita(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("TITULO_TELA", "Agendar Receita Futura");
        startActivity(intent);
    }

    public void acessarContasFuturas(View v) {
        Intent intent = new Intent(this, ContasActivity.class);
        intent.putExtra("TEXTO_SALDO", "Saldo futuro");
        intent.putExtra("BTN_DESPESA_FUTURA", "DESPESA FUTURA");
        intent.putExtra("BTN_RECEITA_FUTURA", "RECEITA FUTURA");
        intent.putExtra("EH_ATALHO", true);
        startActivity(intent);
    }

    public void adicionarDespesa(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        startActivity(intent);
    }

    public void adicionarReceitaFutura(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Receita Futura");
        startActivity(intent);
    }

    public void adicionarDespesaFutura(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Despesa Futura");
        intent.putExtra("EH_ATALHO", true);
        startActivity(intent);
    }

    public void acessarContasActivity(View view) {
        startActivity(new Intent(this, ContasActivity.class));
        Log.i(TAG, "acessou ContasActivity");
    }

    // --- Animações ---

    private void abrirMenu() {
        isMenuOpen = true;
        overlayBackground.setVisibility(View.VISIBLE);
        overlayBackground.animate().alpha(1f).setDuration(300).start();
        radialSpotlight.setVisibility(View.VISIBLE);
        radialSpotlight.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(200f).setInterpolator(interpolator).setDuration(400).start();
        fabMain.animate().setInterpolator(interpolator).rotation(45f).setDuration(300).start();
        animarBotao(fabDespesaFutura, -320f, -200f);
        animarBotao(fabReceitaFutura, -150f, -420f);
        animarBotao(fabNovaDespesa, 150f, -420f);
        animarBotao(fabNovaReceita, 320f, -200f);
    }

    private void fecharMenu() {
        isMenuOpen = false;
        overlayBackground.animate().alpha(0f).setDuration(300).withEndAction(() -> overlayBackground.setVisibility(View.GONE)).start();
        radialSpotlight.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(300).withEndAction(() -> radialSpotlight.setVisibility(View.INVISIBLE)).start();
        fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start();
        recolherBotao(fabDespesaFutura);
        recolherBotao(fabNovaDespesa);
        recolherBotao(fabNovaReceita);
        recolherBotao(fabReceitaFutura);
    }

    private void animarBotao(View view, float x, float y) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().translationX(x).translationY(y).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
    }

    private void recolherBotao(View view) {
        view.animate().translationX(0f).translationY(0f).alpha(0f).setInterpolator(interpolator).setDuration(300).withEndAction(() -> view.setVisibility(View.INVISIBLE)).start();
    }

    private void acaoRapida(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        fecharMenu();
    }

    private void setupSlideView() {
        DashboardPagerAdapter adapter = new DashboardPagerAdapter(this);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.tab_titulo_ultimas_mov);
            } else {
                tab.setText(R.string.tab_titulo_contas_a_vencer);
            }
        }).attach();
    }
}