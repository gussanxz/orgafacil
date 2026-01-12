package com.gussanxz.orgafacil.activity.main.configuracoes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.LoginActivity;

public class ConfigsActivity extends AppCompatActivity {

    private LinearLayout itemPerfil, itemPreferencias, itemSeguranca, itemSair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_configuracoes_configs);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        itemPerfil       = findViewById(R.id.itemPerfil);
        itemPreferencias = findViewById(R.id.itemPreferencias);
        itemSeguranca    = findViewById(R.id.itemSeguranca);
        itemSair         = findViewById(R.id.itemSair);

        itemPerfil.setOnClickListener(v -> {
            // TODO startActivity(new Intent(this, PerfilActivity.class));
        });
        itemPreferencias.setOnClickListener(v -> {
            // TODO startActivity(new Intent(this, PreferenciasActivity.class));
        });
        itemSeguranca.setOnClickListener(v -> {
            startActivity(new Intent(ConfigsActivity.this, SegurancaActivity.class));
        });
        itemSair.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(ConfigsActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
