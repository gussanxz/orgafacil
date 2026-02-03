package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.visual.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;

import java.util.TimeZone;

public class ConfigsActivity extends AppCompatActivity {

    private LinearLayout itemPerfil, itemPreferencias, itemSeguranca, itemSobre, itemSair;
    private TextView textVersaoRodape;
    private FirebaseUser user;
    private ConfigPerfilUsuarioRepository perfilRepository;

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

        perfilRepository = new ConfigPerfilUsuarioRepository();
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
        itemSobre        = findViewById(R.id.itemSobre);
        itemSair         = findViewById(R.id.itemSair);
        textVersaoRodape = findViewById(R.id.textVersaoRodape);

        configurarVersaoApp();
    }

    private void configurarCliques() {
        itemPerfil.setOnClickListener(v -> navegarParaPerfil());

        itemPreferencias.setOnClickListener(v ->
                startActivity(new Intent(this, PreferenciasActivity.class)));

        itemSeguranca.setOnClickListener(v ->
                startActivity(new Intent(this, SegurancaActivity.class)));

        itemSobre.setOnClickListener(v -> exibirDialogoSobre());

        // Aciona o diálogo de confirmação antes de qualquer ação destrutiva
        itemSair.setOnClickListener(v -> confirmarSairDoApp());
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
                        "Organize sua vida financeira com simplicidade.\n\n" +
                        "Fuso Horário: " + TimeZone.getDefault().getID() + "\n" +
                        textVersaoRodape.getText().toString())
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void confirmarSairDoApp() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        bottomSheet.setContentView(viewDialog);

        // Remove o container cinza padrão para respeitar o bg_dialog_top_rounded
        View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        Button btnSair = viewDialog.findViewById(R.id.btnConfirmarSair);
        Button btnCancelar = viewDialog.findViewById(R.id.btnCancelarSair);

        btnSair.setOnClickListener(v -> {
            bottomSheet.dismiss();
            executarLogout();
        });

        btnCancelar.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void navegarParaPerfil() {
        if (user != null) {
            Intent intent = new Intent(this, PerfilActivity.class);
            intent.putExtra("nomeUsuario", user.getDisplayName());
            intent.putExtra("emailUsuario", user.getEmail());
            Uri fotoUrl = user.getPhotoUrl();
            if (fotoUrl != null) intent.putExtra("fotoUsuario", fotoUrl.toString());
            startActivity(intent);
        }
    }

    /**
     * Realiza o Soft Delete (Desativação Lógica) e encerra a sessão local.
     */
    private void executarLogout() {
        if (user == null) return;

        String uid = user.getUid();

        // 1. Alteramos o status no Firestore primeiro (Garante auditoria)
        perfilRepository.desativarContaLogica(uid).addOnCompleteListener(task -> {

            // 2. Limpamos a sessão do Firebase Auth (Segurança local)
            perfilRepository.deslogar();

            // 3. Redirecionamos para a tela de Login/Intro
            abrirTelaLogin();

            if (task.isSuccessful()) {
                Toast.makeText(this, "Sessão encerrada com sucesso.", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("ConfigsActivity", "Falha na sincronização de status: " + task.getException());
            }
        });
    }

    private void abrirTelaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}