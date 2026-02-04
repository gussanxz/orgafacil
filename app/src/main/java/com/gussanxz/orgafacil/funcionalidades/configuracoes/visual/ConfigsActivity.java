package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.util_helper.DialogLogoutHelper;

import java.util.TimeZone;

public class ConfigsActivity extends AppCompatActivity {

    private TextView textVersaoRodape;
    private FirebaseUser user;
    // [REFATORAÇÃO] Removido UsuarioRepository pois não é mais usado aqui

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

        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            navegarParaLogin();
            return;
        }

        inicializarViews();
    }

    private void inicializarViews() {
        // --- NAVEGAÇÃO ---

        // Perfil
        findViewById(R.id.itemPerfil).setOnClickListener(v -> navegarParaPerfil());

        // Preferências
        findViewById(R.id.itemPreferencias).setOnClickListener(v ->
                startActivity(new Intent(this, PreferenciasActivity.class)));

        // Segurança
        findViewById(R.id.itemSeguranca).setOnClickListener(v ->
                startActivity(new Intent(this, SegurancaActivity.class)));

        // Sobre
        findViewById(R.id.itemSobre).setOnClickListener(v -> exibirDialogoSobre());

        // --- AÇÃO DE SAIR ---
        // [CORREÇÃO] Passamos apenas o contexto 'this'
        findViewById(R.id.itemSair).setOnClickListener(v ->
                DialogLogoutHelper.mostrarDialogo(this));

        // --- VERSÃO DO APP ---
        textVersaoRodape = findViewById(R.id.textVersaoRodape);
        configurarVersaoApp();
    }

    private void configurarVersaoApp() {
        try {
            String versao = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            textVersaoRodape.setText("Versão " + versao);
        } catch (Exception e) {
            textVersaoRodape.setText("Versão 1.0.0");
        }
    }

    private void exibirDialogoSobre() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Sobre o OrgaFácil")
                .setMessage("Desenvolvido por: Gussanxz/Tomeki\n\n" +
                        "Fuso Horário: " + TimeZone.getDefault().getID() + "\n" +
                        textVersaoRodape.getText().toString())
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void navegarParaPerfil() {
        if (user != null) {
            Intent intent = new Intent(this, PerfilActivity.class);
            // Passamos dados básicos para evitar delay de carregamento na próxima tela
            intent.putExtra("nomeUsuario", user.getDisplayName());
            intent.putExtra("emailUsuario", user.getEmail());
            Uri fotoUrl = user.getPhotoUrl();
            if (fotoUrl != null) intent.putExtra("fotoUsuario", fotoUrl.toString());
            startActivity(intent);
        }
    }

    private void navegarParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}