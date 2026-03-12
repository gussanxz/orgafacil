package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
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
import com.gussanxz.orgafacil.funcionalidades.contas.ContasViewModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.ReceitasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.adapter.RelatoriosPagerAdapter;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;
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
    private ContasViewModel contasViewModel;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // UI Dashboard
    private TextView textSaldoGeral;
    private ImageView imgOlhoSaldo;
    private TextView labelDespesaFutura;
    private TextView labelReceitaFutura;
    private TextView labelNovaDespesa;
    private TextView labelNovaReceita;
    private TextView textLegendaSaldo;

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
    private com.google.android.material.chip.ChipGroup chipGroupFiltroTipo;

    // Controle de Carregamento para o Observer
    private boolean aguardandoPrimeiroFetch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_contas);

        // Ajuste de insets (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializa IDs e ViewModels
        inicializarComponentes();
        viewModel = new ViewModelProvider(this).get(ResumoGeralViewModel.class);
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        // 3. Configurações de UI e Comportamento
        setupSaldoListaObserver();
        viewModel.verificarViradaDeMes(this);

        setupSlideView();
        configurarBottomAppBarCustomizada();
        setupMenuRadial();
        configurarChipsFiltro();

        // Listener para fechar menu radial ao clicar fora
        overlayBackground.setOnClickListener(v -> fecharMenu());
    }

    private void inicializarViewsDashboard() {
        textSaldoGeral = findViewById(R.id.textSaldo);
        imgOlhoSaldo = findViewById(R.id.imgOlhoSaldo);
        textLegendaSaldo = findViewById(R.id.textLegendaSaldo);

        // 1. ABRIR A TELA -> MOSTRAR "CARREGANDO" (Estado inicial cravado)
        if (textSaldoGeral != null) {
            textSaldoGeral.setText("Carregando...");
            textSaldoGeral.setTextColor(Color.WHITE);
        }
        if (textLegendaSaldo != null) {
            textLegendaSaldo.setText("Calculando...");
        }
        if (imgOlhoSaldo != null) {
            imgOlhoSaldo.setVisibility(View.GONE);
        }
    }

    private void inicializarComponentes() {
        inicializarViewsDashboard();

        tabLayout = findViewById(R.id.tabLayoutDashboard);
        viewPager = findViewById(R.id.viewPagerDashboard);
        overlayBackground = findViewById(R.id.overlay_background);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        chipGroupFiltroTipo = findViewById(R.id.chipGroupFiltroTipo);
        ImageView btnRelatoriosTop = findViewById(R.id.btnRelatoriosTop);

        if (btnRelatoriosTop != null) {
            btnRelatoriosTop.setOnClickListener(v -> acessarRelatorios(v));
        }
    }

    private void setupSaldoListaObserver() {
        // Escuta o status de carregamento
        contasViewModel.carregandoPaginacao.observe(this, isCarregando -> {
            if (isCarregando) {
                if (textLegendaSaldo != null) textLegendaSaldo.setText("Calculando...");
            } else {
                // Quando o carregamento do Firebase terminar, ele destrava a permissão
                aguardandoPrimeiroFetch = false;
            }
        });

        // 2. Escuta a emissão de saldo do ViewModel e substitui na tela
        contasViewModel.saldoListaAtual.observe(this, saldoCentavos -> {
            if (saldoCentavos == null) return;

            // Se o app ainda tá conectando no banco, não faz nada
            if (Boolean.TRUE.equals(contasViewModel.carregandoPaginacao.getValue())) return;

            // BLOQUEIO DO PISCA: O ViewModel emite "0L" no milissegundo em que abre.
            // A gente ignora esse 0L mentiroso se ainda estiver aguardando o primeiro fetch.
            if (aguardandoPrimeiroFetch && saldoCentavos == 0L) return;

            // O saldo agora é validado e real. Manda desenhar na tela!
            desenharSaldoNaTela(saldoCentavos);
        });
    }

    private void desenharSaldoNaTela(long saldoCentavos) {
        double saldoDouble = saldoCentavos / 100.0;
        String valorFormatado = currencyFormat.format(Math.abs(saldoDouble));

        int corSaldo;
        if (saldoCentavos > 0)      corSaldo = Color.parseColor("#4CAF50");
        else if (saldoCentavos < 0) corSaldo = Color.parseColor("#E53935");
        else                        corSaldo = Color.WHITE;

        if (imgOlhoSaldo != null) imgOlhoSaldo.setVisibility(View.VISIBLE);

        View containerSaldo = findViewById(R.id.containerSaldo);
        if (containerSaldo != null) {
            VisibilidadeHelper.configurarVisibilidadeSaldo(
                    containerSaldo, textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);
        }
        VisibilidadeHelper.atualizarValorSaldo(textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);

        // 3. Atualiza a legenda de forma suave
        atualizarLegendaSaldo(saldoCentavos);
    }

    private void atualizarLegendaSaldo(long saldoCentavos) {
        if (textLegendaSaldo == null) return;

        int abaAtual = viewPager.getCurrentItem(); // 0 = Pendentes, 1 = Últimas Movimentações
        String novoTexto = "";

        if (abaAtual == 0) {
            // Aba "Contas Pendentes"
            if (saldoCentavos < 0) {
                novoTexto = "Total a pagar";
            } else if (saldoCentavos > 0) {
                novoTexto = "Total a receber";
            } else {
                novoTexto = "Nenhum pendente";
            }
        } else {
            // Aba "Últimas Movimentações"
            novoTexto = "Saldo das movimentações";
        }

        // Se o texto for o mesmo, não faz nada
        if (textLegendaSaldo.getText().toString().equals(novoTexto)) return;

        // Animação suave: esmaece, troca e volta
        final String textoFinal = novoTexto;
        textLegendaSaldo.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    textLegendaSaldo.setText(textoFinal);
                    textLegendaSaldo.animate()
                            .alpha(0.8f)
                            .setDuration(150)
                            .start();
                }).start();
    }

    private void configurarBottomAppBarCustomizada() {
        bottomAppBar.setContentInsetsAbsolute(0, 0);
        View containerInterno = findViewById(R.id.linear_footer_container);
        if (containerInterno == null && bottomAppBar.getChildCount() > 0) {
            for (int i = 0; i < bottomAppBar.getChildCount(); i++) {
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

        labelDespesaFutura = findViewById(R.id.label_fab_despesa_futura);
        labelReceitaFutura = findViewById(R.id.label_fab_receita_futura);
        labelNovaDespesa = findViewById(R.id.label_fab_nova_despesa);
        labelNovaReceita = findViewById(R.id.label_fab_nova_receita);
    }

    public void adicionarReceita(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("TITULO_TELA", "Adicionar Receita");
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
        intent.putExtra("TITULO_TELA", "Adicionar Despesa");
        startActivity(intent);
    }

    public void adicionarReceitaFutura(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Receita");
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("EH_CONTA_FUTURA", true);
        startActivity(intent);
    }

    public void adicionarDespesaFutura(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Despesa");
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("EH_CONTA_FUTURA", true);
        startActivity(intent);
    }

    public void acessarContasActivity(View view) {
        startActivity(new Intent(this, ContasActivity.class));
        Log.i(TAG, "acessou ContasActivity");
    }

    private void abrirMenu() {
        isMenuOpen = true;
        overlayBackground.setVisibility(View.VISIBLE);
        overlayBackground.animate().alpha(1f).setDuration(300).start();
        radialSpotlight.setVisibility(View.VISIBLE);
        radialSpotlight.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(200f)
                .setInterpolator(interpolator).setDuration(400).start();
        fabMain.animate().setInterpolator(interpolator).rotation(45f).setDuration(300).start();

        animarBotao(fabDespesaFutura, -320f, -200f);
        animarBotao(fabReceitaFutura, -150f, -420f);
        animarBotao(fabNovaDespesa, 150f, -420f);
        animarBotao(fabNovaReceita, 320f, -200f);

        animarLabelAcimaDoFab(labelDespesaFutura, -320f, -200f);
        animarLabelAcimaDoFab(labelReceitaFutura, -150f, -420f);
        animarLabelAcimaDoFab(labelNovaDespesa, 150f, -420f);
        animarLabelAcimaDoFab(labelNovaReceita, 320f, -200f);
    }

    private void fecharMenu() {
        isMenuOpen = false;
        overlayBackground.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> overlayBackground.setVisibility(View.GONE)).start();
        radialSpotlight.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(300)
                .withEndAction(() -> radialSpotlight.setVisibility(View.INVISIBLE)).start();
        fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start();

        recolherBotao(fabDespesaFutura);
        recolherBotao(fabNovaDespesa);
        recolherBotao(fabNovaReceita);
        recolherBotao(fabReceitaFutura);

        recolherBotao(labelDespesaFutura);
        recolherBotao(labelReceitaFutura);
        recolherBotao(labelNovaDespesa);
        recolherBotao(labelNovaReceita);
    }

    private void animarBotao(View view, float x, float y) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().translationX(x).translationY(y).alpha(1f)
                .setInterpolator(interpolator).setDuration(300).start();
    }

    private void recolherBotao(View view) {
        view.animate().translationX(0f).translationY(0f).alpha(0f)
                .setInterpolator(interpolator).setDuration(300)
                .withEndAction(() -> view.setVisibility(View.INVISIBLE)).start();
    }

    private void animarLabelAcimaDoFab(TextView label, float fabX, float fabY) {
        label.setVisibility(View.VISIBLE);
        label.setAlpha(0f);
        label.animate()
                .translationX(fabX)
                .translationY(fabY - dp(44))
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(250)
                .start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
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
                tab.setText(R.string.tab_titulo_contas_pendentes);
            } else {
                tab.setText(R.string.tab_titulo_ultimas_mov);
            }
        }).attach();

        // Notifica o ViewModel quando o usuário troca de aba
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                boolean ehFuturo = (position == 0); // aba 0 = Pendentes, aba 1 = Últimas
                contasViewModel.notificarAbaAtiva(ehFuturo);
            }
        });
    }

    private void configurarChipsFiltro() {
        chipGroupFiltroTipo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chipTodos)) {
                contasViewModel.setFiltroTipo(null);
            } else if (checkedIds.contains(R.id.chipSoReceitas)) {
                contasViewModel.setFiltroTipo(TipoCategoriaContas.RECEITA);
            } else if (checkedIds.contains(R.id.chipSoDespesas)) {
                contasViewModel.setFiltroTipo(TipoCategoriaContas.DESPESA);
            }
        });
    }

    private void configurarAbasRelatorio() {

        RelatoriosPagerAdapter adapter = new RelatoriosPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Desativa o swipe lateral se quiser que a navegação seja apenas pelos cliques nos Chips/Tabs
        // viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Resumo");
                    break;
                case 1:
                    tab.setText("Evolução");
                    break;
                case 2:
                    tab.setText("Planejamento");
                    break;
            }
        }).attach();
    }

    public void acessarRelatorios(View v) {

        Intent intent = new Intent(this,
                com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.activities.RelatoriosActivity.class);

        startActivity(intent);
    }
}