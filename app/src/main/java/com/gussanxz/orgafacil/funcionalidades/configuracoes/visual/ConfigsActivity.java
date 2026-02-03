package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class ConfigsActivity extends AppCompatActivity {

    private LinearLayout itemPerfil, itemPreferencias, itemSeguranca, itemSair;
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
        itemPerfil = findViewById(R.id.itemPerfil);
        itemPreferencias = findViewById(R.id.itemPreferencias);
        itemSeguranca = findViewById(R.id.itemSeguranca);
        itemSair = findViewById(R.id.itemSair);
    }

    private void configurarCliques() {
        itemPerfil.setOnClickListener(v -> navegarParaPerfil());
        itemPreferencias.setOnClickListener(v -> startActivity(new Intent(this, PreferenciasActivity.class)));
        itemSeguranca.setOnClickListener(v -> startActivity(new Intent(this, SegurancaActivity.class)));

        // Chamada do método corrigida para coincidir com o nome abaixo
        itemSair.setOnClickListener(v -> confirmarSairDoApp());
    }

    private void confirmarSairDoApp() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        bottomSheet.setContentView(viewDialog);

        // Remove o fundo padrão para o arredondado aparecer
        View bottomSheetInternal = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        Button btnSair = viewDialog.findViewById(R.id.btnConfirmarSair);
        Button btnCancelar = viewDialog.findViewById(R.id.btnCancelarSair);

        btnSair.setOnClickListener(v -> {
            bottomSheet.dismiss();
            // CORREÇÃO: Agora chama o método correto existente na classe
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

    private void executarLogout() {
        if (user == null) return;

        String uid = user.getUid();

        // 1. Alteramos o status no Firestore primeiro
        perfilRepository.desativarContaLogica(uid).addOnCompleteListener(task -> {

            // 2. Independente se o banco atualizou ou deu erro de rede,
            // nós limpamos a sessão local por segurança.
            perfilRepository.deslogar();

            // 3. Redirecionamos para a tela de Login/Intro
            abrirTelaLogin();

            if (task.isSuccessful()) {
                Toast.makeText(this, "Conta desativada com sucesso.", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("ConfigsActivity", "Erro ao atualizar status no banco: " + task.getException());
            }
        });
    }
    private void abrirTelaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        // Limpa a pilha de atividades para segurança
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}