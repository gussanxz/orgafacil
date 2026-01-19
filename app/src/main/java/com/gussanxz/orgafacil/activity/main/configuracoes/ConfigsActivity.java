package com.gussanxz.orgafacil.activity.main.configuracoes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.LoginActivity;

public class ConfigsActivity extends AppCompatActivity {

    private LinearLayout itemPerfil, itemPreferencias, itemSeguranca, itemSair;
    private TextView textUserEmail;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_configuracoes);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        inicializarComponentes();

        // Pega o usuário atual do Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Configura todos os cliques
        configurarCliques();
    }

    private void inicializarComponentes() {
        itemPerfil       = findViewById(R.id.itemPerfil);
        itemPreferencias = findViewById(R.id.itemPreferencias);
        itemSeguranca    = findViewById(R.id.itemSeguranca);
        itemSair         = findViewById(R.id.itemSair);
    }

    private void configurarCliques() {
        configurarClickPerfil();
        configurarClickPreferencias();
        configurarClickSeguranca();
        configurarClickSair();
    }

    private void configurarClickPerfil() {
        itemPerfil.setOnClickListener(v -> {
            if (user != null) {
                // 1. Recupera dados para Debug e Envio
                String userEmail = user.getEmail();
                String userUid = user.getUid();
                String userName = user.getDisplayName();

                // 2. SEUS LOGS DE DEBUG (Solicitado)
                System.out.println("--- DEBUG PERFIL ---");
                System.out.println("Status: LOGADO");
                System.out.println("UID (Importante): " + userUid);
                System.out.println("Email: " + userEmail);
                System.out.println("Nome Google: " + userName);
                System.out.println("--------------------");

                // 3. Prepara a navegação enviando os dados
                Intent intent = new Intent(ConfigsActivity.this, PerfilActivity.class);
                intent.putExtra("nomeUsuario", userName);
                intent.putExtra("emailUsuario", userEmail);

                // Verifica se tem foto antes de passar
                Uri fotoUrl = user.getPhotoUrl();
                if (fotoUrl != null) {
                    intent.putExtra("fotoUsuario", fotoUrl.toString());
                }

                startActivity(intent);
            } else {
                System.out.println("--- DEBUG PERFIL: USUARIO NULO ---");
            }
        });
    }

    private void configurarClickPreferencias() {
        itemPreferencias.setOnClickListener(v -> {
            // TODO: Criar PreferenciasActivity e descomentar abaixo
            // startActivity(new Intent(ConfigsActivity.this, PreferenciasActivity.class));
            Toast.makeText(ConfigsActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarClickSeguranca() {
        itemSeguranca.setOnClickListener(v -> {
            startActivity(new Intent(ConfigsActivity.this, SegurancaActivity.class));
        });
    }

    private void configurarClickSair() {
        itemSair.setOnClickListener(v -> {
            // Desloga do Firebase
            FirebaseAuth.getInstance().signOut();

            // Volta para o Login e limpa o histórico de navegação
            Intent i = new Intent(ConfigsActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}