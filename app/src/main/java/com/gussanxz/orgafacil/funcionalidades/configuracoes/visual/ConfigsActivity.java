package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.UsuarioRepository; // Import necessário


public class ConfigsActivity extends AppCompatActivity {

    private LinearLayout itemPerfil, itemPreferencias, itemSeguranca, itemSair;
    private FirebaseUser user;

    // 1. Declarar o repositório para usar o logout centralizado
    private UsuarioRepository usuarioRepository;

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

        // 2. Inicializar o repositório
        usuarioRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            abrirTelaLogin();
            return;
        }

        inicializarComponentes();
        configurarCliques();
    }

    private void inicializarComponentes() {
        itemPerfil       = findViewById(R.id.itemPerfil);
        itemPreferencias = findViewById(R.id.itemPreferencias);
        itemSeguranca    = findViewById(R.id.itemSeguranca);
        itemSair         = findViewById(R.id.itemSair);
    }

    private void configurarCliques() {
        itemPerfil.setOnClickListener(v -> navegarParaPerfil());

        itemPreferencias.setOnClickListener(v ->
                Toast.makeText(this, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show()
        );

        itemSeguranca.setOnClickListener(v ->
                startActivity(new Intent(this, SegurancaActivity.class))
        );

        // 3. Unificado: Usa o método que utiliza o repositório
        itemSair.setOnClickListener(v -> executarLogout());
    }

    private void navegarParaPerfil() {
        if (user != null) {
            System.out.println("--- DEBUG PERFIL: " + user.getDisplayName() + " ---");
            Intent intent = new Intent(this, PerfilActivity.class);
            intent.putExtra("nomeUsuario", user.getDisplayName());
            intent.putExtra("emailUsuario", user.getEmail());

            Uri fotoUrl = user.getPhotoUrl();
            if (fotoUrl != null) {
                intent.putExtra("fotoUsuario", fotoUrl.toString());
            }
            startActivity(intent);
        }
    }

    /**
     * Executa o logout através do repositório centralizado.
     */
    private void executarLogout() {
        // 4. Usa o método centralizado conforme planejado
        usuarioRepository.deslogar();
        abrirTelaLogin();
    }

    private void abrirTelaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}