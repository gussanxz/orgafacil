package com.gussanxz.orgafacil.funcionalidades.autenticacao.visual;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.gussanxz.orgafacil.R;

/**
 * TermosActivity
 * Exibe os termos de uso e políticas de privacidade do OrgaFacil.
 */
public class TermosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_termos_privacidade);

        // Botão simples para retornar à tela de cadastro
        Button btnEntendi = findViewById(R.id.btnEntendi);
        btnEntendi.setOnClickListener(v -> finish());
    }
}