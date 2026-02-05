package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioRepository;

/**
 * SegurancaActivity
 * Gerencia PIN, Troca de Senha e Navegação para Desativação de Conta.
 */
public class SegurancaActivity extends AppCompatActivity {

    private SwitchCompat switchAtivarPin;
    private LinearLayout itemAlterarSenha, itemRecuperarSenha, itemEncerrarConta;
    private SharedPreferences prefs;

    private FirebaseUser usuarioAtual;
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_configs_seguranca);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        usuarioRepository = new UsuarioRepository();
        usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        prefs = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE);

        bindViews();
        carregarPreferencias();
        configurarListeners();
    }

    private void bindViews() {
        switchAtivarPin = findViewById(R.id.switchAtivarPin);
        itemAlterarSenha = findViewById(R.id.itemAlterarSenha);
        itemRecuperarSenha = findViewById(R.id.itemRecuperarSenha);
        itemEncerrarConta = findViewById(R.id.itemExcluirConta);
    }

    private void carregarPreferencias() {
        boolean pinObrigatorio = prefs.getBoolean("pin_obrigatorio", false);
        switchAtivarPin.setChecked(pinObrigatorio);
    }

    private void configurarListeners() {
        // Toggle de bloqueio por PIN (Armazenamento Local)
        switchAtivarPin.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("pin_obrigatorio", isChecked).apply()
        );

        // Alterar Senha (Placeholder)
        itemAlterarSenha.setOnClickListener(v ->
                Toast.makeText(this, "Para alterar a senha, use a opção 'Recuperar Senha' por enquanto.", Toast.LENGTH_LONG).show()
        );

        // Disparo de E-mail de Recuperação
        itemRecuperarSenha.setOnClickListener(v -> dispararEmailRecuperacao());

        // Navegação: Desativação de Conta (Soft Delete)
        // [CORREÇÃO] Aponta para a nova Activity renomeada
        itemEncerrarConta.setOnClickListener(v ->
                startActivity(new Intent(this, ConfirmacaoDesativacaoContaActivity.class))
        );
    }

    private void dispararEmailRecuperacao() {
        if (usuarioAtual != null && usuarioAtual.getEmail() != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(usuarioAtual.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show();
                        } else {
                            // Usa o mapeamento de erro do novo Repository
                            String erro = usuarioRepository.mapearErroAutenticacao(task.getException());
                            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}