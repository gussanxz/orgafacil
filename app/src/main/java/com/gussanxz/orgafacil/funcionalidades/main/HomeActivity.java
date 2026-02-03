package com.gussanxz.orgafacil.funcionalidades.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.visual.ConfigsActivity;
import com.gussanxz.orgafacil.funcionalidades.contas.comum.visual.ui.ResumoContasActivity;
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
        TemaHelper.aplicarTemaDoCache(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);

        prefsRepository = new PreferenciasRepository();
        inicializarComponentes();
        configurarBotoesBloqueados();

        // [NOVO] Captura o botão voltar do Android para exibir o Logout
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmarSairDoApp();
            }
        });

        carregarPreferenciasUsuario();
    }

    private void inicializarComponentes() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        textoContas = findViewById(R.id.textViewContas);
        textoVendas = findViewById(R.id.textViewVendas);
        textoMercado = findViewById(R.id.textViewMercado);
        textoAtividades = findViewById(R.id.textViewAtividades);
        textoConfigs = findViewById(R.id.textViewConfigs);
    }

    private void carregarPreferenciasUsuario() {
        prefsRepository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                if (prefs != null) {
                    TemaHelper.aplicarTema(prefs.getTema());
                }
            }
            @Override
            public void onErro(String erro) {
                Log.e(TAG, "Erro ao sincronizar preferências: " + erro);
            }
        });
    }

    private void configurarBotoesBloqueados() {
        View.OnClickListener listenerBloqueio = view ->
                Toast.makeText(this, "Funcionalidade disponível em breve!", Toast.LENGTH_SHORT).show();

        int[] idsBloqueados = {R.id.imageViewMercado, R.id.imageViewTodo, R.id.imageViewBoletoCPF};
        for (int id : idsBloqueados) {
            View v = findViewById(id);
            if (v != null) v.setOnClickListener(listenerBloqueio);
        }
    }

    public void acessarResumoContasActivity(View view) {
        startActivity(new Intent(this, ResumoContasActivity.class));
    }

    public void acessarResumoVendasAcitivity(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
    }

    public void acessarConfigs(View view) {
        startActivity(new Intent(this, ConfigsActivity.class));
    }

    private void confirmarSairDoApp() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        bottomSheet.setContentView(viewDialog);

        // ESSA PARTE É ESSENCIAL:
        // Remove o fundo padrão para o seu @drawable/bg_dialog_top_rounded aparecer
        View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        Button btnSair = viewDialog.findViewById(R.id.btnConfirmarSair);
        Button btnCancelar = viewDialog.findViewById(R.id.btnCancelarSair);

        btnSair.setOnClickListener(v -> {
            bottomSheet.dismiss();
            executarLogoutReal(); // Ou perfilRepository.deslogar() na Configs
        });

        btnCancelar.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void executarLogoutReal() {
        com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();
        Intent intent = new Intent(this, com.gussanxz.orgafacil.funcionalidades.main.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}