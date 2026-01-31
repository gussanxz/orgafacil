package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

public class PreferenciasActivity extends AppCompatActivity {

    private RadioGroup radioGroupTema;
    private PreferenciasRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_preferencias);

        repository = new PreferenciasRepository();
        radioGroupTema = findViewById(R.id.radioGroupTema);

        // 1. Executa o Check Automático ao abrir
        marcarOpcaoAtual();

        // 2. Configura o Salvamento Silencioso
        configurarListeners();
    }

    /**
     * Etapa 1: Check Automático.
     * Agora utiliza a FirebaseSession para ler o cache centralizado. [cite: 2026-01-31]
     */
    private void marcarOpcaoAtual() {
        String temaSalvo = FirebaseSession.getString(this, TemaHelper.KEY_TEMA, "SISTEMA");

        if (temaSalvo.equals("CLARO")) {
            ((RadioButton) findViewById(R.id.rbClaro)).setChecked(true);
        } else if (temaSalvo.equals("ESCURO")) {
            ((RadioButton) findViewById(R.id.rbEscuro)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbSistema)).setChecked(true);
        }
    }

    /**
     * Etapa 2: Salvamento Silencioso.
     */
    private void configurarListeners() {
        radioGroupTema.setOnCheckedChangeListener((group, checkedId) -> {
            String novoTema = "SISTEMA";
            if (checkedId == R.id.rbClaro) novoTema = "CLARO";
            else if (checkedId == R.id.rbEscuro) novoTema = "ESCURO";

            aplicarESalvar(novoTema);
        });
    }

    private void aplicarESalvar(String tema) {
        // Ação visual imediata (UX)
        TemaHelper.aplicarTema(tema);

        // Sincronização via Repository
        // Agora passando o Context (this) como 1º argumento para resolver o erro do print [cite: 2026-01-31]
        PreferenciasModel pref = new PreferenciasModel(tema, "BRL", false);

        repository.salvar(this, pref, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                // Sincronização silenciosa concluída com sucesso
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(PreferenciasActivity.this, "Erro ao sincronizar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}