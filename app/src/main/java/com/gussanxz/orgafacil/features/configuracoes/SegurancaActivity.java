package com.gussanxz.orgafacil.features.configuracoes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.gussanxz.orgafacil.R;

public class SegurancaActivity extends AppCompatActivity {

    // Itens de UI
    private SwitchCompat switchAtivarPin;
    private LinearLayout itemAlterarSenha, itemRecuperarSenha, itemExcluirConta;

    // Preferências locais
    private SharedPreferences prefs;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userRef;

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

        // Firebase
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid());
        }

        // SharedPreferences
        prefs = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE);

        bindViews();
        carregarPreferencias();
        configurarListeners();
    }

    private void bindViews() {
        switchAtivarPin   = findViewById(R.id.switchAtivarPin);
        itemAlterarSenha  = findViewById(R.id.itemAlterarSenha);
        itemRecuperarSenha= findViewById(R.id.itemRecuperarSenha);
        itemExcluirConta  = findViewById(R.id.itemExcluirConta);
    }

    private void carregarPreferencias() {
        // Padrão = true (exigir PIN ao abrir o app)
        boolean pinObrigatorio = prefs.getBoolean("pin_obrigatorio", true);
        switchAtivarPin.setChecked(pinObrigatorio);
    }

    private void configurarListeners() {
        // Salva ON/OFF do PIN obrigatório
        switchAtivarPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pin_obrigatorio", isChecked).apply();
            // feedback opcional
            // Toast.makeText(this, isChecked ? "PIN obrigatório ativado" : "PIN ao abrir desativado", Toast.LENGTH_SHORT).show();
        });

        // Abre a tela dedicada de alterar senha (com senha atual + nova + confirmar)
        itemAlterarSenha.setOnClickListener(v ->
                startActivity(new Intent(this, AlterarSenhaActivity.class))
        );

        // Envia e-mail de recuperação (reset) pelo Firebase
        itemRecuperarSenha.setOnClickListener(v -> {
            if (user == null || user.getEmail() == null) {
                Toast.makeText(this, "Usuário/e-mail não disponível.", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Link de recuperação enviado para " + user.getEmail(), Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Falha: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Excluir conta (confirmação + remoção DB + Auth)
        itemExcluirConta.setOnClickListener(v -> confirmarExclusaoConta());
    }

    private void confirmarExclusaoConta() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir conta")
                .setMessage("Tem certeza? Esta ação não pode ser desfeita.")
                .setPositiveButton("Excluir", (dialog, which) -> excluirConta())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void excluirConta() {
        if (user == null) {
            Toast.makeText(this, "Nenhum usuário autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove dados no Realtime Database
        if (userRef != null) {
            userRef.removeValue();
        }

        // Remove conta no FirebaseAuth
        user.delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                    // Volta pra tela de Login (ajuste o destino conforme sua navegação)
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir conta: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
