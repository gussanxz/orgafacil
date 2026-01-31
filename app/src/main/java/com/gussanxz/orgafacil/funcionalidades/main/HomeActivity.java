package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.visual.ConfigsActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.ResumoContasActivity;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private TextView textoContas, textoVendas, textoMercado, textoAtividades, textoConfigs;
    private PreferenciasRepository prefsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. APLICAÇÃO IMEDIATA: Deve vir antes do super.onCreate para evitar o "flash" branco
        TemaHelper.aplicarTemaDoCache(this);

        super.onCreate(savedInstanceState);

        // 2. CONFIGURAÇÃO DE INTERFACE
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);

        // 3. INICIALIZAÇÃO DE DADOS
        prefsRepository = new PreferenciasRepository();
        carregarPreferenciasUsuario();

        // 4. CONFIGURAÇÃO DE VIEWS
        inicializarComponentes();
        configurarBotoesBloqueados();
    }

    private void inicializarComponentes() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textoContas = findViewById(R.id.textViewContas);
        textoVendas = findViewById(R.id.textViewVendas);
        textoMercado = findViewById(R.id.textViewMercado);
        textoAtividades = findViewById(R.id.textViewAtividades);
        textoConfigs = findViewById(R.id.textViewConfigs);
    }

    private void carregarPreferenciasUsuario() {
        // CORREÇÃO: Passando 'this' como primeiro argumento para o repositório.
        // Isso resolve o erro "Required type: Context, Provided: Callback" [cite: 2026-01-31]
        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                if (prefs != null) {
                    // Como o Repositório agora já salva no cache internamente,
                    // você só precisa aplicar o tema visualmente aqui.
                    TemaHelper.aplicarTema(prefs.getTema());

                    Log.i(TAG, "Preferências sincronizadas e cache atualizado via Repository.");
                }
            }
            @Override
            public void onErro(String erro) {
                Log.e(TAG, "Erro ao sincronizar: " + erro);
            }
        });
    }
    private void configurarBotoesBloqueados() {
        View.OnClickListener listenerBloqueio = view ->
                Toast.makeText(HomeActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();

        int[] idsBloqueados = {
                R.id.imageViewMercado,
                R.id.imageViewTodo,
                R.id.imageViewBoletoCPF
        };

        for (int id : idsBloqueados) {
            View overlay = findViewById(id);
            if (overlay != null) overlay.setOnClickListener(listenerBloqueio);
        }
    }

    // --- Métodos de Navegação ---
    public void acessarResumoContasActivity(View view) {
        startActivity(new Intent(this, ResumoContasActivity.class));
    }

    public void acessarResumoVendasAcitivity(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
    }

    public void acessarConfigs(View view) {
        startActivity(new Intent(this, ConfigsActivity.class));
    }
}