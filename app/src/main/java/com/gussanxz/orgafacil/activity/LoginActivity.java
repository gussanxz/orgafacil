package com.gussanxz.orgafacil.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.contas.ContasActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.helper.SenhaVisibilidadeHelper;
import com.gussanxz.orgafacil.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private EditText campoEmail, campoSenha;
    private Button botaoEntrar;
    private TextView recuperarSenha;
    private Usuario usuario;
    private FirebaseAuth autenticacao;
    private RadioButton acessarTelaCadastro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        botaoEntrar = findViewById(R.id.buttonEntrar);
        acessarTelaCadastro = findViewById(R.id.radioButtonCadastreSe);
        recuperarSenha = findViewById(R.id.textViewRecuperarSenha);

        botaoEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String textoEmail = campoEmail.getText().toString();
                String textoSenha = campoSenha.getText().toString();

                if (!textoEmail.isEmpty()) {
                    if (!textoSenha.isEmpty()) {

                        usuario = new Usuario();
                        usuario.setEmail( textoEmail );
                        usuario.setSenha( textoSenha );
                        validarLogin();

                    } else {
                        Toast.makeText(LoginActivity.this, "Preencha a senha", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Preencha o email", Toast.LENGTH_SHORT).show();
                }
            }
        });

        acessarTelaCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                abrirTelaCadastro();

            }
        });

        recuperarSenha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recuperarSenha();
            }
        });

        EditText campoSenha = findViewById(R.id.editSenha);
        SenhaVisibilidadeHelper.ativarAlternancia(campoSenha);

    }

    public void validarLogin(){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if ( task.isSuccessful() ) {

                    abrirTelaHome();
                    
                }else {

                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthInvalidUserException e) {
                        excecao = "Usuario nao esta cadastrado";
                    }catch (FirebaseAuthInvalidCredentialsException e) {
                        excecao = "E-mail e/ou senha nao correspondem a um usuario cadastrado";
                    }catch (Exception e) {
                        excecao = "Erro ao cadastrar usuario: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this, excecao, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void abrirTelaHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    public void abrirTelaCadastro(){
        startActivity(new Intent(this, CadastroActivity.class));
    }

    public void recuperarSenha() {
        EditText campoEmail = findViewById(R.id.editEmail);
        String email = campoEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Digite o e-mail", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Link de redefinição enviado para " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erro ao enviar o link. Verifique o e-mail.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}