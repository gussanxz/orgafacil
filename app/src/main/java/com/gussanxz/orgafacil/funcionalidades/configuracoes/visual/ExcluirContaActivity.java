package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.autenticacao.LoginActivity;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.dados.UsuarioRepository;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

public class ExcluirContaActivity extends AppCompatActivity {

    private TextInputEditText editSenhaExcluir;
    private Button btnConfirmarExclusao, btnCancelar;
    private LoadingHelper loadingHelper;
    private UsuarioRepository usuarioRepository;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_excluir_conta);

        usuarioRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Inicializa o helper usando o include do XML
        loadingHelper = new LoadingHelper(findViewById(R.id.loading_overlay));

        editSenhaExcluir = findViewById(R.id.editSenhaExcluir);
        btnConfirmarExclusao = findViewById(R.id.btnConfirmarExclusao);
        btnCancelar = findViewById(R.id.btnCancelarExclusao);

        btnConfirmarExclusao.setOnClickListener(v -> processarExclusao());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void processarExclusao() {
        String senha = editSenhaExcluir.getText().toString().trim();

        if (senha.isEmpty()) {
            editSenhaExcluir.setError("Senha obrigatória para exclusão");
            return;
        }

        loadingHelper.exibir();
        btnConfirmarExclusao.setEnabled(false);

        usuarioRepository.excluirContaCompleta(user, senha, task -> {
            loadingHelper.ocultar();
            btnConfirmarExclusao.setEnabled(true);

            if (task != null && task.isSuccessful()) {
                Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_LONG).show();

                // Vai para o login e limpa a pilha
                Intent i = new Intent(this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, "Erro: Verifique sua senha.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}