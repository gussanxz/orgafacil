package com.gussanxz.orgafacil.features.configuracoes;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.config.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.data.model.Usuario;

public class PerfilActivity extends AppCompatActivity {

    // Componentes de Visualização
    private TextView textNomePerfil, textEmailPerfil;
    private LinearLayout layoutVisualizacao, btnEditarDados;
    private ImageView imagePerfilUsuario;

    // Componentes de Edição
    private LinearLayout layoutEdicao;
    private TextInputEditText editNomePerfil;
    private Button btnSalvarEdicao, btnCancelarEdicao;

    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_meu_perfil);

        inicializarComponentes();
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        usuarioAtual = autenticacao.getCurrentUser();

        // Carregar dados iniciais
        carregarDados();

        // --- CLIQUES ---

        // 1. Botão "Editar dados" (Abre o modo edição)
        btnEditarDados.setOnClickListener(v -> {
            alternarModo(true); // true = Modo Edição
        });

        // 2. Botão "Cancelar" (Volta sem salvar)
        btnCancelarEdicao.setOnClickListener(v -> {
            alternarModo(false); // false = Modo Visualização
        });

        // 3. Botão "Salvar"
        btnSalvarEdicao.setOnClickListener(v -> {
            salvarAlteracoes();
        });
    }

    private void alternarModo(boolean modoEdicao) {
        if (modoEdicao) {
            // Entrar no modo edição: Preenche o EditText com o nome atual
            editNomePerfil.setText(textNomePerfil.getText().toString());

            layoutVisualizacao.setVisibility(View.GONE);
            layoutEdicao.setVisibility(View.VISIBLE);
        } else {
            // Voltar para visualização
            layoutEdicao.setVisibility(View.GONE);
            layoutVisualizacao.setVisibility(View.VISIBLE);
        }
    }

    private void salvarAlteracoes() {
        String novoNome = editNomePerfil.getText().toString();

        if (novoNome.isEmpty()) {
            editNomePerfil.setError("Nome não pode ser vazio");
            return;
        }

        if (usuarioAtual == null) return;

        // 1. Atualiza no Auth (Login Google etc)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(novoNome)
                .build();

        usuarioAtual.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // 2. Atualiza no Firestore (Banco)
                        Usuario usuarioUpdate = new Usuario();
                        usuarioUpdate.setIdUsuario(usuarioAtual.getUid());
                        usuarioUpdate.setNome(novoNome);
                        usuarioUpdate.atualizar(); // Aquele metodo que criamos antes no Model

                        // 3. Atualiza a tela e fecha o modo edição
                        textNomePerfil.setText(novoNome);
                        Toast.makeText(this, "Perfil atualizado!", Toast.LENGTH_SHORT).show();
                        alternarModo(false);

                    } else {
                        Toast.makeText(this, "Erro ao atualizar.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void carregarDados() {
        if (usuarioAtual != null) {
            textEmailPerfil.setText(usuarioAtual.getEmail());

            Uri urlFoto = usuarioAtual.getPhotoUrl();
            if (urlFoto != null) {
                Glide.with(this).load(urlFoto).circleCrop().into(imagePerfilUsuario);
            }

            // Tenta pegar nome do bundle ou do Auth
            Bundle bundle = getIntent().getExtras();
            if (bundle != null && bundle.getString("nomeUsuario") != null) {
                textNomePerfil.setText(bundle.getString("nomeUsuario"));
            } else {
                textNomePerfil.setText(usuarioAtual.getDisplayName());
            }
        }
    }

    private void inicializarComponentes() {
        // Visualização
        textNomePerfil = findViewById(R.id.textNomePerfil);
        textEmailPerfil = findViewById(R.id.textEmailPerfil);
        imagePerfilUsuario = findViewById(R.id.imagePerfilUsuario);
        layoutVisualizacao = findViewById(R.id.layoutVisualizacao);
        btnEditarDados = findViewById(R.id.btnEditarDados);

        // Edição
        layoutEdicao = findViewById(R.id.layoutEdicao);
        editNomePerfil = findViewById(R.id.editNomePerfil);
        btnSalvarEdicao = findViewById(R.id.btnSalvarEdicao);
        btnCancelarEdicao = findViewById(R.id.btnCancelarEdicao);
    }
}