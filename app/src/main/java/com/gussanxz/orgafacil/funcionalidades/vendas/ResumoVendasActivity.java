package com.gussanxz.orgafacil.funcionalidades.vendas;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.CaixaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.VendaRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.CaixaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.VendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa.AbrirCaixaActivity; // usado no FAB launcher
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa.FecharCaixaActivity; // controle de caixa
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.RegistrarVendasActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ResumoVendasActivity extends AppCompatActivity {

    private static final String TAG              = "ResumoVendasActivity";
    private static final String PREFS_CAIXA      = "caixa_prefs";
    private static final String KEY_MIGRADO      = "vendas_legadas_migradas_v2";
    private static final String KEY_LEGADO_CRIADO = "caixa_legado_criado";

    private VendaRepository  vendaRepository;
    private CaixaRepository  caixaRepository;

    private ListenerRegistration listenerVendasHoje;
    private ListenerRegistration listenerCaixa;

    private TextView      textVendasHoje;
    private TextView      textQtdVendas;
    private TextView      textStatusCaixa;
    private ImageView     imgStatusCaixa;
    private LinearLayout  layoutStatusCaixa;
    private String        ultimoValorVendas = "R$ 0,00";

    /** Caixa atualmente aberto, ou null se fechado. */
    private CaixaModel caixaAtual = null;

    // Componentes de UI
    private TabLayout  tabLayoutDashboard;
    private ViewPager2 viewPagerDashboard;

    // Speed-dial
    private FloatingActionButton fabMain, fabNovaVenda, fabNovoOrcamento, fabNovoCliente;
    private View overlayBackground, radialSpotlight;
    private final OvershootInterpolator interpolator = new OvershootInterpolator();

    // Launcher para AbrirCaixaActivity — após abertura bem-sucedida, navega direto para venda
    private final ActivityResultLauncher<Intent> abrirCaixaParaVendaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            String caixaId = result.getData()
                                    .getStringExtra(AbrirCaixaActivity.EXTRA_CAIXA_ID);
                            if (caixaId != null) navegarParaNovaVenda(caixaId);
                        }
                    });

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
        inicializarResumo();
        configurarAbas();
        configurarFabMenu();
        garantirCaixaLegadoEMigrar();
    }

    private void inicializarComponentes() {
        tabLayoutDashboard = findViewById(R.id.tabLayoutDashboard);
        viewPagerDashboard = findViewById(R.id.viewPagerDashboard);

        fabMain           = findViewById(R.id.fab_main);
        fabNovaVenda      = findViewById(R.id.fab_nova_venda);
        fabNovoOrcamento  = findViewById(R.id.fab_novo_orcamento);
        fabNovoCliente    = findViewById(R.id.fab_novo_cliente);

        overlayBackground = findViewById(R.id.overlay_background);
        radialSpotlight   = findViewById(R.id.radial_spotlight);
    }

    private void inicializarResumo() {
        textVendasHoje     = findViewById(R.id.textVendasHoje);
        textQtdVendas      = findViewById(R.id.textQtdVendas);
        textStatusCaixa    = findViewById(R.id.textStatusCaixa);
        imgStatusCaixa     = findViewById(R.id.imgStatusCaixa);
        layoutStatusCaixa  = findViewById(R.id.layoutStatusCaixa);

        vendaRepository = new VendaRepository();
        caixaRepository = new CaixaRepository();

        if (textVendasHoje != null) {
            textVendasHoje.setTag(true);
            textVendasHoje.setOnClickListener(v -> alternarVisibilidadeValor());
        }

        if (layoutStatusCaixa != null) {
            layoutStatusCaixa.setOnClickListener(v -> gerenciarCaixa());
        }
    }

    // ── Migração de vendas legadas (roda uma vez) ─────────────────────

    private void garantirCaixaLegadoEMigrar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_CAIXA, MODE_PRIVATE);

        if (prefs.getBoolean(KEY_MIGRADO, false)) return; // já feito

        // 1. Garante que o documento "caixa_0" existe
        caixaRepository.garantirCaixaLegado(new CaixaRepository.VoidCallback() {
            @Override
            public void onSucesso(String caixaId) {
                prefs.edit().putBoolean(KEY_LEGADO_CRIADO, true).apply();
                // 2. Migra as vendas sem caixaId para "caixa_0"
                migrarVendasLegadas(prefs);
            }

            @Override
            public void onErro(String erro) {
                Log.w(TAG, "Não foi possível criar caixa_0: " + erro);
            }
        });
    }

    private void migrarVendasLegadas(SharedPreferences prefs) {
        caixaRepository.migrarVendasLegadas(new CaixaRepository.VoidCallback() {
            @Override
            public void onSucesso(String quantidade) {
                int qtd = Integer.parseInt(quantidade);
                if (qtd > 0) {
                    Log.i(TAG, "Migradas " + qtd + " venda(s) legada(s) para caixa_0.");
                }
                prefs.edit().putBoolean(KEY_MIGRADO, true).apply();
                Log.i(TAG, "Migração de vendas legadas concluída.");
                // Calcula e grava os totais reais do caixa_0 no documento
                calcularTotaisCaixaLegado();
            }

            @Override
            public void onErro(String erro) {
                Log.w(TAG, "Erro na migração de vendas legadas: " + erro);
                // Não persiste o flag — tentará novamente na próxima abertura
            }
        });
    }

    private void calcularTotaisCaixaLegado() {
        vendaRepository.buscarTotaisFinalizadasDoCaixa(
                CaixaModel.ID_LEGADO,
                new VendaRepository.TotaisCallback() {
                    @Override
                    public void onTotais(int qtd, int total) { // Agora recebe int (centavos)
                        caixaRepository.atualizarSnapshotTotais(
                                CaixaModel.ID_LEGADO, qtd, total,
                                new CaixaRepository.VoidCallback() {
                                    @Override
                                    public void onSucesso(String id) { // <-- Corrigido para onSucesso
                                        // Para o log ficar legível, dividimos por 100.0
                                        Log.i(TAG, "Totais do caixa_0 gravados: "
                                                + qtd + " vendas, R$ " + (total / 100.0));
                                    }
                                    @Override
                                    public void onErro(String erro) {
                                        Log.w(TAG, "Falha ao gravar totais do caixa_0: " + erro);
                                    }
                                });
                    }
                    @Override
                    public void onErro(String erro) {
                        Log.w(TAG, "Falha ao calcular totais do caixa_0: " + erro);
                    }
                });
    }

    // ── Listeners ─────────────────────────────────────────────────────

    private void iniciarListeners() {
        escutarVendasHoje();
        escutarCaixa();
    }

    private void pararListeners() {
        if (listenerVendasHoje != null) { listenerVendasHoje.remove(); listenerVendasHoje = null; }
        if (listenerCaixa      != null) { listenerCaixa.remove();      listenerCaixa      = null; }
    }

    private void escutarVendasHoje() {
        listenerVendasHoje = vendaRepository.escutarVendasFinalizadasHoje(
                new VendaRepository.ListaCallback() {
                    @Override
                    public void onNovosDados(List<VendaModel> lista) {
                        // Acumulador em int (centavos)
                        int totalHojeCentavos = 0;
                        for (VendaModel v : lista) {
                            totalHojeCentavos += v.getValorTotal();
                        }

                        // Formata dividindo por 100.0 para exibir os centavos corretamente
                        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                        ultimoValorVendas = fmt.format(totalHojeCentavos / 100.0);

                        if (textQtdVendas != null)
                            textQtdVendas.setText(String.valueOf(lista.size()));

                        if (textVendasHoje != null) {
                            boolean visivel = textVendasHoje.getTag() != null
                                    && (boolean) textVendasHoje.getTag();
                            if (visivel) textVendasHoje.setText(ultimoValorVendas);
                        }
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(ResumoVendasActivity.this,
                                "Erro ao carregar vendas: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void escutarCaixa() {
        listenerCaixa = caixaRepository.escutarCaixaAberto(new CaixaRepository.CaixaCallback() {
            @Override
            public void onCaixa(CaixaModel caixa) {
                caixaAtual = caixa;
                atualizarUiStatusCaixa(caixa);
            }

            @Override
            public void onErro(String erro) { /* não crítico */ }
        });
    }

    private void atualizarUiStatusCaixa(CaixaModel caixa) {
        if (textStatusCaixa == null) return;

        boolean aberto = caixa != null && caixa.isAberto();
        textStatusCaixa.setText(aberto ? "Aberto" : "Fechado");

        if (imgStatusCaixa != null) {
            imgStatusCaixa.setImageResource(aberto
                    ? R.drawable.ic_lock_open_28
                    : R.drawable.ic_lock_24);
            int cor = aberto ? 0xFF4CAF50 : 0xFFFFD54F;
            imgStatusCaixa.setColorFilter(cor);
        }
    }

    // ── Gerenciamento do caixa via toque no status ────────────────────

    private void gerenciarCaixa() {
        // FecharCaixaActivity gerencia abertura e fechamento conforme o estado atual
        startActivity(new Intent(this, FecharCaixaActivity.class));
    }

    // ── Alternância de valor ──────────────────────────────────────────

    private void alternarVisibilidadeValor() {
        if (textVendasHoje == null) return;
        boolean visivel = textVendasHoje.getTag() != null && (boolean) textVendasHoje.getTag();
        if (visivel) {
            textVendasHoje.setText("R$ ••••••");
            textVendasHoje.setTag(false);
        } else {
            textVendasHoje.setText(ultimoValorVendas);
            textVendasHoje.setTag(true);
        }
    }

    // ── Abas ─────────────────────────────────────────────────────────

    private void configurarAbas() {
        VendasPagerAdapter adapter = new VendasPagerAdapter(this);
        viewPagerDashboard.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutDashboard, viewPagerDashboard, (tab, position) ->
                tab.setText(position == 0 ? "Operações" : "Gestão")
        ).attach();
    }

    // ── FAB / Nova Venda ──────────────────────────────────────────────

    private void configurarFabMenu() {
        if (fabMain != null) {
            fabMain.setOnClickListener(v -> verificarCaixaENavegar());
        }
    }

    /** Ponto de entrada para "Nova Venda" (também pode ser chamado do XML via onClick). */
    public void acessarRegistrarVendasActivity(View view) {
        verificarCaixaENavegar();
    }

    private void verificarCaixaENavegar() {
        if (caixaAtual != null && caixaAtual.isAberto()) {
            navegarParaNovaVenda(caixaAtual.getId());
        } else {
            // Caixa fechado: pede para abrir, depois vai direto para a venda
            new AlertDialog.Builder(this)
                    .setTitle("Caixa fechado")
                    .setMessage("É necessário abrir o caixa antes de registrar uma venda.\n\nDeseja abrir o caixa agora?")
                    .setPositiveButton("Abrir Caixa", (d, w) ->
                            abrirCaixaParaVendaLauncher.launch(
                                    new Intent(this, AbrirCaixaActivity.class)))
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void navegarParaNovaVenda(String caixaId) {
        Intent intent = new Intent(this, RegistrarVendasActivity.class);
        intent.putExtra(RegistrarVendasActivity.EXTRA_CAIXA_ID, caixaId);
        startActivity(intent);
        Log.i(TAG, "Nova venda, caixaId=" + caixaId);
    }

    // ── Animações do Speed-dial ───────────────────────────────────────

    private void animarBotao(View view, float x, float y) {
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
        float centroX = fabMain.getTranslationX();
        float centroY = fabMain.getTranslationY();
        view.setTranslationX(centroX);
        view.setTranslationY(centroY);
        view.setAlpha(0f);
        view.animate().translationX(x).translationY(y).alpha(1f)
                .setInterpolator(interpolator).setDuration(300).start();
    }

    private void recolherBotao(View view) {
        float centroX = fabMain.getTranslationX();
        float centroY = fabMain.getTranslationY();
        view.animate().translationX(centroX).translationY(centroY).alpha(0f)
                .setInterpolator(interpolator).setDuration(300)
                .withEndAction(() -> view.setVisibility(View.INVISIBLE)).start();
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        iniciarListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pararListeners();
    }
}
