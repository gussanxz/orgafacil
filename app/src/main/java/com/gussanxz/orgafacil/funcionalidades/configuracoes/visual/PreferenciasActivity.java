package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.usuario.repository.PreferenciasRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.PreferenciasModel;
import com.gussanxz.orgafacil.util_helper.TemaHelper;

public class PreferenciasActivity extends AppCompatActivity {

    private RadioGroup radioGroupTema;
    private PreferenciasRepository repository;
    // Adicionado para manter o estado atual das preferências
    private PreferenciasModel preferenciasAtuais;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_preferencias);

        repository = new PreferenciasRepository();
        radioGroupTema = findViewById(R.id.radioGroupTema);

        // Carregar as preferências reais do banco/cache ao abrir
        carregarPreferencias();

        marcarOpcaoAtual();
        configurarListeners();
    }

    private void carregarPreferencias() {
        repository.obter(this, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                preferenciasAtuais = prefs;
            }

            @Override
            public void onErro(String erro) {
                // Se falhar, inicializamos com o padrão para evitar NullPointerException
                preferenciasAtuais = new PreferenciasModel();
            }
        });
    }

    private void marcarOpcaoAtual() {
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
        TemaHelper.aplicarTema(tema);

        // Se ainda não carregou do repositório, garantimos que não esteja nulo
        if (preferenciasAtuais == null) {
            preferenciasAtuais = new PreferenciasModel();
        }

        // Atualizamos apenas o campo de tema dentro do objeto existente
        // Isso evita que 'esconderSaldo' ou 'moeda' voltem ao padrão de fábrica
        preferenciasAtuais.getVisual().setTema(tema);

        repository.salvar(this, preferenciasAtuais, new PreferenciasRepository.Callback() {
            @Override
            public void onSucesso(PreferenciasModel prefs) {
                // Sucesso
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(PreferenciasActivity.this, "Erro ao sincronizar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}