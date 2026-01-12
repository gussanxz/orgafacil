package com.gussanxz.orgafacil.activity.main.configuracoes;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;

public class AlterarSenhaActivity extends AppCompatActivity {

    private TextInputEditText editSenhaAtual, editNovaSenha, editConfirmarSenha;
    private Button btnSalvarSenha;
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_configuracoes_alterar_senha);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        editSenhaAtual = findViewById(R.id.editSenhaAtual);
        editNovaSenha = findViewById(R.id.editNovaSenha);
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha);
        btnSalvarSenha = findViewById(R.id.btnSalvarSenha);

        btnSalvarSenha.setOnClickListener(v -> alterarSenha());
    }

    private void alterarSenha() {
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String senhaAtual = String.valueOf(editSenhaAtual.getText()).trim();
        String nova = String.valueOf(editNovaSenha.getText()).trim();
        String conf = String.valueOf(editConfirmarSenha.getText()).trim();

        if (senhaAtual.isEmpty() || nova.isEmpty() || conf.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!nova.equals(conf)) {
            Toast.makeText(this, "As senhas novas não coincidem.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nova.length() < 6) {
            Toast.makeText(this, "A nova senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "E-mail do usuário não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.reauthenticate(EmailAuthProvider.getCredential(email, senhaAtual))
                .addOnSuccessListener(unused -> user.updatePassword(nova)
                        .addOnSuccessListener(aVoid -> { Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show(); finish(); })
                        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao alterar senha: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e -> Toast.makeText(this, "Senha atual incorreta.", Toast.LENGTH_SHORT).show());
    }
}
