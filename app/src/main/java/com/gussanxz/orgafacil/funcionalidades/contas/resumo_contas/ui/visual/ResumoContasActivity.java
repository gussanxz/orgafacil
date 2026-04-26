package com.gussanxz.orgafacil.funcionalidades.contas.resumo_contas.ui.visual;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.ContasViewModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.DespesasActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities.ReceitasActivity;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;
import com.google.android.material.bottomappbar.BottomAppBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ResumoContasActivity extends AppCompatActivity {

    private static final String TAG = "ResumoContasActivity";

    // ─────────────────────────────────────────────────────────────
    // ViewModels
    // ─────────────────────────────────────────────────────────────
    private ResumoGeralViewModel viewModel;
    private ContasViewModel contasViewModel;

    // ─────────────────────────────────────────────────────────────
    // Views — Dashboard / Header
    // ─────────────────────────────────────────────────────────────
    private TextView textSaudacao;
    private TextView textSaldoGeral;
    private TextView textLegendaSaldo;
    private ImageView imgOlhoSaldo;

    // ─────────────────────────────────────────────────────────────
    // Views — Corpo
    // ─────────────────────────────────────────────────────────────
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private EditText editTextBusca;

    // ─────────────────────────────────────────────────────────────
    // Views — Footer / Navegação
    // ─────────────────────────────────────────────────────────────
    private LinearLayout btnFooterContas, btnFooterMovimentacoes;
    private BottomAppBar bottomAppBar;
    private RadioGroup radioGroupFiltroTipo;
    private View btnRelatoriosTop;

    // ─────────────────────────────────────────────────────────────
    // Views — Menu Radial
    // ─────────────────────────────────────────────────────────────
    private FloatingActionButton fabMain, fabDespesaFutura, fabNovaDespesa,
            fabNovaReceita, fabReceitaFutura;
    private TextView labelDespesaFutura, labelReceitaFutura,
            labelNovaDespesa, labelNovaReceita;
    private View overlayBackground;
    private View radialSpotlight;
    private boolean isMenuOpen = false;
    private final OvershootInterpolator interpolator = new OvershootInterpolator();

    // ─────────────────────────────────────────────────────────────
    // Estado
    // ─────────────────────────────────────────────────────────────
    private boolean aguardandoPrimeiroFetch = true;
    private androidx.activity.result.ActivityResultLauncher<Intent> launcherMovimentacao;

    // ─────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_resumo_contas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        inicializarComponentes();

        viewModel = new ViewModelProvider(this).get(ResumoGeralViewModel.class);
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        viewModel.nomeUsuario.observe(this, nome -> {
            if (textSaudacao != null) {
                textSaudacao.setText("Olá, " + nome + " 👋");
            }
        });

        setupSaldoListaObserver();
        viewModel.verificarViradaDeMes(this);

        launcherMovimentacao = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    contasViewModel.invalidarDados();
                    contasViewModel.fetchDados(true, null);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> contasViewModel.fetchDados(false, null), 300);
                });

        setupSlideView();
        contasViewModel.fetchDados(true, null);
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> contasViewModel.fetchDados(false, null), 300);

        configurarBottomAppBarCustomizada();
        setupMenuRadial();
        configurarChipsFiltro();
        configurarBusca();

        overlayBackground.setOnClickListener(v -> fecharMenu());
    }

    // ─────────────────────────────────────────────────────────────
    // Inicialização
    // ─────────────────────────────────────────────────────────────

    private void inicializarComponentes() {
        inicializarViewsDashboard();

        tabLayout           = findViewById(R.id.tabLayoutDashboard);
        viewPager           = findViewById(R.id.viewPagerDashboard);
        overlayBackground   = findViewById(R.id.overlay_background);
        bottomAppBar        = findViewById(R.id.bottomAppBar);
        radioGroupFiltroTipo = findViewById(R.id.radioGroupFiltroTipo);
        btnRelatoriosTop    = findViewById(R.id.btnRelatoriosTop);
        editTextBusca       = findViewById(R.id.editTextBusca);

        // Botão voltar
        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> finish());

        if (btnRelatoriosTop != null) {
            btnRelatoriosTop.setOnClickListener(v -> acessarRelatorios(v));
        }
    }

    private void inicializarViewsDashboard() {
        imgOlhoSaldo     = findViewById(R.id.imgOlhoSaldo);
        textLegendaSaldo = findViewById(R.id.textLegendaSaldo);
        textSaudacao     = findViewById(R.id.textSaudacao);
        textSaldoGeral   = findViewById(R.id.textSaldo);

        if (textSaldoGeral != null) {
            textSaldoGeral.setText("Carregando...");
            textSaldoGeral.setTextColor(Color.WHITE);
        }
        if (textLegendaSaldo != null) textLegendaSaldo.setText("Calculando...");
        if (imgOlhoSaldo != null) imgOlhoSaldo.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────
    // Busca
    // ─────────────────────────────────────────────────────────────

    /**
     * Conecta o campo de busca ao ContasViewModel.
     * Filtro aplicado a cada caractere digitado.
     */
    private void configurarBusca() {
        if (editTextBusca == null) return;
        editTextBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // aplicarFiltros mantém os outros filtros ativos (datas e categorias = null = sem filtro)
                contasViewModel.aplicarFiltros(s.toString().trim(), null, null, null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Saldo
    // ─────────────────────────────────────────────────────────────

    private void setupSaldoListaObserver() {
        contasViewModel.carregandoPaginacao.observe(this, isCarregando -> {
            if (!isCarregando) aguardandoPrimeiroFetch = false;
        });

        contasViewModel.saldoListaAtual.observe(this, saldoCentavos -> {
            if (saldoCentavos == null) return;
            if (Boolean.TRUE.equals(contasViewModel.carregandoPaginacao.getValue())) return;
            if (aguardandoPrimeiroFetch && saldoCentavos == 0L) return;
            desenharSaldoNaTela(saldoCentavos);
        });
    }

    private void desenharSaldoNaTela(long saldoCentavos) {
        String valorFormatado = com.gussanxz.orgafacil.util_helper.MoedaHelper
                .formatarCentavosParaBRL(saldoCentavos);

        // Saldo no header sempre escuro (#1A1A1A) para contrastar com o teal.
        // A cor semântica (verde/vermelho) fica nos itens da lista, não aqui.
        int corSaldo = androidx.core.content.ContextCompat.getColor(this, R.color.cor_saldo_header);

        if (imgOlhoSaldo != null) imgOlhoSaldo.setVisibility(View.VISIBLE);

        View containerSaldo = findViewById(R.id.containerSaldo);
        if (containerSaldo != null) {
            VisibilidadeHelper.configurarVisibilidadeSaldo(
                    containerSaldo, textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);
        }
        VisibilidadeHelper.atualizarValorSaldo(
                textSaldoGeral, imgOlhoSaldo, valorFormatado, corSaldo);

        atualizarLegendaSaldo(saldoCentavos);
    }

    private void atualizarLegendaSaldo(long saldoCentavos) {
        if (textLegendaSaldo == null) return;

        int abaAtual = viewPager.getCurrentItem();
        String novoTexto;

        if (abaAtual == 0) {
            if (saldoCentavos < 0)      novoTexto = "Total a pagar";
            else if (saldoCentavos > 0) novoTexto = "Total a receber";
            else                        novoTexto = "Nenhum pendente";
        } else {
            novoTexto = "Saldo das movimentações";
        }

        if (textLegendaSaldo.getText().toString().equals(novoTexto)) return;

        final String textoFinal = novoTexto;
        textLegendaSaldo.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            textLegendaSaldo.setText(textoFinal);
            textLegendaSaldo.animate().alpha(0.8f).setDuration(150).start();
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // ViewPager / Tabs
    // ─────────────────────────────────────────────────────────────

    private void setupSlideView() {
        DashboardPagerAdapter adapter = new DashboardPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText(R.string.tab_titulo_contas_pendentes);
            else               tab.setText(R.string.tab_titulo_ultimas_mov);
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                contasViewModel.notificarAbaAtiva(position == 0);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Chips de filtro
    // ─────────────────────────────────────────────────────────────

    private void configurarChipsFiltro() {
        RadioButton radioTodos     = radioGroupFiltroTipo.findViewById(R.id.radioTodos);
        RadioButton radioReceitas  = radioGroupFiltroTipo.findViewById(R.id.radioReceitas);
        RadioButton radioDespesas  = radioGroupFiltroTipo.findViewById(R.id.radioDespesas);

        radioGroupFiltroTipo.setOnCheckedChangeListener((group, checkedId) -> {
            // Força o redesenho de todos os botões para garantir que o selector
            // de background e cor de texto sejam aplicados corretamente no tema escuro.
            atualizarEstadoVisualFiltro(radioTodos, radioReceitas, radioDespesas);

            if (checkedId == R.id.radioTodos) {
                contasViewModel.setFiltroTipo(null);
            } else if (checkedId == R.id.radioReceitas) {
                contasViewModel.setFiltroTipo(TipoCategoriaContas.RECEITA);
            } else if (checkedId == R.id.radioDespesas) {
                contasViewModel.setFiltroTipo(TipoCategoriaContas.DESPESA);
            }
        });

        // Estado inicial correto
        atualizarEstadoVisualFiltro(radioTodos, radioReceitas, radioDespesas);
    }

    /**
     * Força o redesenho dos RadioButtons após mudança de estado.
     * Necessário no tema escuro (Material3) porque o sistema não redesenha
     * automaticamente o background/textColor baseado em state_checked.
     */
    private void atualizarEstadoVisualFiltro(RadioButton... botoes) {
        for (RadioButton btn : botoes) {
            btn.refreshDrawableState();
            btn.invalidate();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Footer / Bottom bar
    // ─────────────────────────────────────────────────────────────

    private void configurarBottomAppBarCustomizada() {
        bottomAppBar.setContentInsetsAbsolute(0, 0);
        setupMenuFooter();
    }

    private void setupMenuFooter() {
        btnFooterMovimentacoes = findViewById(R.id.btn_footer_movimentacoes);
        btnFooterContas        = findViewById(R.id.btn_footer_contas);

        btnFooterMovimentacoes.setOnClickListener(v -> acessarContasActivity(v));
        btnFooterContas.setOnClickListener(v -> acessarContasFuturas(v));
    }

    // ─────────────────────────────────────────────────────────────
    // Navegação
    // ─────────────────────────────────────────────────────────────

    public void acessarContasFuturas(View v) {
        Intent intent = new Intent(this, ContasActivity.class);
        intent.putExtra("EH_ATALHO", true);
        startActivity(intent);
    }

    public void acessarContasActivity(View view) {
        startActivity(new Intent(this, ContasActivity.class));
    }

    public void adicionarReceita(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("TITULO_TELA", "Adicionar Receita");
        launcherMovimentacao.launch(intent);
    }

    public void adicionarDespesa(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        intent.putExtra("TITULO_TELA", "Adicionar Despesa");
        launcherMovimentacao.launch(intent);
    }

    public void adicionarReceitaFutura(View v) {
        Intent intent = new Intent(this, ReceitasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Receita");
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("EH_CONTA_FUTURA", true);
        launcherMovimentacao.launch(intent);
    }

    public void adicionarDespesaFutura(View v) {
        Intent intent = new Intent(this, DespesasActivity.class);
        intent.putExtra("TITULO_TELA", "Agendar Despesa");
        intent.putExtra("EH_ATALHO", true);
        intent.putExtra("EH_CONTA_FUTURA", true);
        launcherMovimentacao.launch(intent);
    }

    public void acessarRelatorios(View v) {
        startActivity(new Intent(this,
                com.gussanxz.orgafacil.funcionalidades.contas.relatorios.ui.activities.RelatoriosActivity.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Menu Radial
    // ─────────────────────────────────────────────────────────────

    private void setupMenuRadial() {
        fabMain           = findViewById(R.id.fab_main);
        fabDespesaFutura  = findViewById(R.id.fab_despesa_futura);
        fabNovaDespesa    = findViewById(R.id.fab_nova_despesa);
        fabNovaReceita    = findViewById(R.id.fab_nova_receita);
        fabReceitaFutura  = findViewById(R.id.fab_receita_futura);
        radialSpotlight   = findViewById(R.id.radial_spotlight);

        labelDespesaFutura = findViewById(R.id.label_fab_despesa_futura);
        labelReceitaFutura = findViewById(R.id.label_fab_receita_futura);
        labelNovaDespesa   = findViewById(R.id.label_fab_nova_despesa);
        labelNovaReceita   = findViewById(R.id.label_fab_nova_receita);

        fabMain.setOnClickListener(v -> { if (!isMenuOpen) abrirMenu(); else fecharMenu(); });
        fabDespesaFutura.setOnClickListener(v -> { fecharMenu(); adicionarDespesaFutura(v); });
        fabNovaDespesa.setOnClickListener(v   -> { fecharMenu(); adicionarDespesa(v); });
        fabNovaReceita.setOnClickListener(v   -> { fecharMenu(); adicionarReceita(v); });
        fabReceitaFutura.setOnClickListener(v -> { fecharMenu(); adicionarReceitaFutura(v); });
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
        animarBotao(fabNovaDespesa,    150f, -420f);
        animarBotao(fabNovaReceita,    320f, -200f);

        animarLabelAcimaDoFab(labelDespesaFutura, -320f, -200f);
        animarLabelAcimaDoFab(labelReceitaFutura, -150f, -420f);
        animarLabelAcimaDoFab(labelNovaDespesa,    150f, -420f);
        animarLabelAcimaDoFab(labelNovaReceita,    320f, -200f);
    }

    private void fecharMenu() {
        isMenuOpen = false;
        overlayBackground.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> overlayBackground.setVisibility(View.GONE)).start();
        radialSpotlight.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(300)
                .withEndAction(() -> radialSpotlight.setVisibility(View.INVISIBLE)).start();
        fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start();

        recolherBotao(fabDespesaFutura); recolherBotao(fabNovaDespesa);
        recolherBotao(fabNovaReceita);   recolherBotao(fabReceitaFutura);
        recolherBotao(labelDespesaFutura); recolherBotao(labelReceitaFutura);
        recolherBotao(labelNovaDespesa);   recolherBotao(labelNovaReceita);
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
        label.animate().translationX(fabX).translationY(fabY - dp(44))
                .alpha(1f).setInterpolator(interpolator).setDuration(250).start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}