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
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;

/**
 * SegurancaActivity
 * Atua como um HUB de navegação para as opções de proteção da conta.
 * Segue a melhor prática de desacoplamento, delegando fluxos complexos para Activities dedicadas. [cite: 2025-10-26]
 */
public class SegurancaActivity extends AppCompatActivity {

    private SwitchCompat switchAtivarPin;
    private LinearLayout itemAlterarSenha, itemRecuperarSenha, itemEncerrarConta;
    private SharedPreferences prefs;

    private FirebaseUser usuarioAtual;
    private ConfigPerfilUsuarioRepository perfilRepository; // Atualizado para o novo nome [cite: 2026-01-22]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_configs_seguranca);

        // Ajuste de padding para barras do sistema (Status e Navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // Inicialização de Repositório e Instâncias
        perfilRepository = new ConfigPerfilUsuarioRepository(); // Atualizado para o novo nome [cite: 2026-01-22]
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
        // Lembre-se de atualizar o ID no XML para algo como itemEncerrarConta, se desejar
        itemEncerrarConta = findViewById(R.id.itemExcluirConta);
    }

    private void carregarPreferencias() {
        boolean pinObrigatorio = prefs.getBoolean("pin_obrigatorio", true);
        switchAtivarPin.setChecked(pinObrigatorio);
    }

    private void configurarListeners() {
        // Toggle de bloqueio por PIN (Armazenamento Local)
        switchAtivarPin.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("pin_obrigatorio", isChecked).apply()
        );

        // Navegação: Alterar Senha (Fluxo para usuário logado)
        itemAlterarSenha.setOnClickListener(v ->
                startActivity(new Intent(this, AlterarSenhaActivity.class))
        );

        // Ação: Disparo de E-mail de Recuperação (Ação atômica, delegada ao Repository)
        itemRecuperarSenha.setOnClickListener(v -> dispararEmailRecuperacao());

        // Navegação: Encerrar Conta (Redireciona para tela de confirmação e reautenticação) [cite: 2025-11-10]
        itemEncerrarConta.setOnClickListener(v ->
                startActivity(new Intent(this, ConfirmacaoEncerrarContaActivity.class))
        );
    }

    /**
     * Solicita o envio de e-mail de reset.
     * Mantido aqui por não exigir inputs adicionais do usuário nesta tela.
     */
    private void dispararEmailRecuperacao() {
        if (usuarioAtual != null && usuarioAtual.getEmail() != null) {
            // Nota: Certifique-se de que o método enviarEmailRecuperacao esteja descomentado no perfilRepository [cite: 2025-11-10]
            FirebaseAuth.getInstance().sendPasswordResetEmail(usuarioAtual.getEmail()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show();
                } else {
                    // Delegamos o mapeamento de erro para o novo Repositório [cite: 2025-11-10]
                    String erro = perfilRepository.mapearErroAutenticacao(task.getException());
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}