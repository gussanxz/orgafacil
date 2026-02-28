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

    // [CORREÇÃO 1.5] TextView de legenda do saldo (ex: "Total a pagar", "Saldo das movimentações")
    // Certifique-se que este ID existe no XML, ou remova se não houver
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_contas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        viewModel = new ViewModelProvider(this).get(ResumoGeralViewModel.class);
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        setupDashboardObserver();
        setupSaldoListaObserver(); // [CORREÇÃO 1.5]

        viewModel.verificarViradaDeMes(this);

        setupSlideView();
        configurarBottomAppBarCustomizada();
        setupMenuRadial();
        configurarChipsFiltro();

        overlayBackground.setOnClickListener(v -> fecharMenu());
    }

    private void inicializarViewsDashboard() {
        textSaldoGeral = findViewById(R.id.textSaldo);
        imgOlhoSaldo = findViewById(R.id.imgOlhoSaldo);
        textLegendaSaldo = findViewById(R.id.textLegendaSaldo);
    }

    private void inicializarComponentes() {
        inicializarViewsDashboard();

        tabLayout = findViewById(R.id.tabLayoutDashboard);
        viewPager = findViewById(R.id.viewPagerDashboard);
        overlayBackground = findViewById(R.id.overlay_background);
        bottomAppBar = findViewById(R.id.bottomAppBar);
        chipGroupFiltroTipo = findViewById(R.id.chipGroupFiltroTipo);
    }

    // Observer original — mantém o saldo real do Firestore como base inicial
    // antes de qualquer interação do usuário
    private void setupDashboardObserver() {
        viewModel.resumoDados.observe(this, resumo -> {
            if (resumo == null || textSaldoGeral == null) return;

            long saldoCentavos = resumo.getBalanco().getSaldoAtual();
            double saldoDouble = saldoCentavos / 100.0;
            String valorFormatado = currencyFormat.format(saldoDouble);

            int corSaldo;
            if (saldoCentavos > 0)      corSaldo = Color.parseColor("#4CAF50");
            else if (saldoCentavos < 0) corSaldo = Color.parseColor("#00D39E");
            else                         corSaldo = Color.WHITE;

            View containerSaldo = findViewById(R.id.containerSaldo);
            VisibilidadeHelper.configurarVisibilidadeSaldo(
                    containerSaldo, textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);

            VisibilidadeHelper.atualizarValorSaldo(textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);
        });
    }

    /**
     * [CORREÇÃO 1.5] Observer do saldo calculado localmente.
     * Atualiza o textSaldoGeral sempre que a lista visível muda —
     * por filtro de tipo (só receitas/só despesas) ou troca de aba.
     * Este observer sobrescreve o valor do setupDashboardObserver() quando ativo.
     */
    private void setupSaldoListaObserver() {
        contasViewModel.saldoListaAtual.observe(this, saldoCentavos -> {
            if (saldoCentavos == null || textSaldoGeral == null) return;

            double saldoDouble = saldoCentavos / 100.0;
            String valorFormatado = currencyFormat.format(Math.abs(saldoDouble));

            int corSaldo;
            if (saldoCentavos > 0)      corSaldo = Color.parseColor("#4CAF50");
            else if (saldoCentavos < 0) corSaldo = Color.parseColor("#E53935");
            else                         corSaldo = Color.WHITE;

            VisibilidadeHelper.atualizarValorSaldo(textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);

            // Atualiza legenda contextual se a view existir no XML
            atualizarLegendaSaldo(saldoCentavos);
        });
    }

    /**
     * [CORREÇÃO 1.5] Atualiza o texto de contexto do saldo conforme aba e valor.
     */
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

        // Animação suave: esmaece (alpha 0), troca o texto, e volta (alpha 0.8)
        final String textoFinal = novoTexto;
        textLegendaSaldo.animate()
                .alpha(0f)
                .setDuration(150) // 150 milissegundos
                .withEndAction(() -> {
                    textLegendaSaldo.setText(textoFinal);
                    textLegendaSaldo.animate()
                            .alpha(0.8f) // Volta para 80% de opacidade (conforme definimos no XML)
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
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("TITULO_TELA", "Agendar Receita");
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

        // [CORREÇÃO 1.5] Notifica o ViewModel quando o usuário troca de aba,
        // para que saldoListaAtual seja publicado com o saldo correto (pendentes ou histórico)
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
}