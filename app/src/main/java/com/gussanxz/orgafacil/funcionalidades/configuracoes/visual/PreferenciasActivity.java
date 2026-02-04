package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.PreferenciasRepository;
// CORREÇÃO 1: Importando do local correto agora
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.PreferenciasModel;
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

        marcarOpcaoAtual();
        configurarListeners();
    }

    private void marcarOpcaoAtual() {
        // CORREÇÃO 2: Usando constantes em vez de "string solta"
        String temaSalvo = FirebaseSession.getString(this, TemaHelper.KEY_TEMA, PreferenciasModel.TEMA_SISTEMA);

        if (temaSalvo.equals(PreferenciasModel.TEMA_CLARO)) {
            ((RadioButton) findViewById(R.id.rbClaro)).setChecked(true);
        } else if (temaSalvo.equals(PreferenciasModel.TEMA_ESCURO)) {
            ((RadioButton) findViewById(R.id.rbEscuro)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbSistema)).setChecked(true);
        }
    }

    private void configurarListeners() {
        radioGroupTema.setOnCheckedChangeListener((group, checkedId) -> {
            String novoTema = PreferenciasModel.TEMA_SISTEMA;

            if (checkedId == R.id.rbClaro) {
                novoTema = PreferenciasModel.TEMA_CLARO;
            } else if (checkedId == R.id.rbEscuro) {
                novoTema = PreferenciasModel.TEMA_ESCURO;
            }

            aplicarESalvar(novoTema);
        });
    }

    private void aplicarESalvar(String tema) {
        // Ação visual imediata (UX)
        TemaHelper.aplicarTema(tema);

        // CORREÇÃO 3: Usando construtor vazio + Setter
        // O construtor vazio já define BRL, false, etc.
        PreferenciasModel pref = new PreferenciasModel();
        pref.setTema(tema);

        // Sincronização via Repository
        repository.salvar(this, pref, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                // Sincronização silenciosa concluída
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(PreferenciasActivity.this, "Erro ao sincronizar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}