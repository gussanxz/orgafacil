package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager; // [NOVO] Segurança
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

public class PerfilActivity extends AppCompatActivity {

    private TextView textNomePerfil, textEmailPerfil;
    private LinearLayout layoutVisualizacao, btnEditarDados, layoutEdicao;
    private ImageView imagePerfilUsuario;
    private TextInputEditText editNomePerfil;
    private Button btnSalvarEdicao, btnCancelarEdicao;
    private ProgressBar progressBar;

    private FirebaseUser usuarioAtual;
    private ConfigPerfilUsuarioRepository perfilRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_meu_perfil);

        // [SEGURANÇA] Impede prints em tela de dados sensíveis
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        perfilRepository = new ConfigPerfilUsuarioRepository();
        usuarioAtual = ConfiguracaoFirestore.getFirebaseAutenticacao().getCurrentUser();

        if (usuarioAtual == null) {
            finish();
            return;
        }

        inicializarComponentes();
        carregarDados();

        btnEditarDados.setOnClickListener(v -> alternarModo(true));
        btnCancelarEdicao.setOnClickListener(v -> alternarModo(false));
        btnSalvarEdicao.setOnClickListener(v -> salvarAlteracoes());
    }

    private void alternarModo(boolean modoEdicao) {
        if (modoEdicao) {
            editNomePerfil.setText(textNomePerfil.getText().toString());
            layoutVisualizacao.setVisibility(View.GONE);
            layoutEdicao.setVisibility(View.VISIBLE);
        } else {
            layoutEdicao.setVisibility(View.GONE);
            layoutVisualizacao.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sincroniza a alteração do nome no Auth e no Firestore.
     */
    private void salvarAlteracoes() {
        String novoNome = editNomePerfil.getText().toString().trim();
        if (novoNome.isEmpty()) {
            editNomePerfil.setError("Nome não pode ser vazio");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSalvarEdicao.setEnabled(false);

        // 1. Atualiza no Firebase Auth (Identidade da Sessão)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(novoNome)
                .build();

        usuarioAtual.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // 2. [CORREÇÃO] Criamos um modelo apenas com os campos que DEVEM ser alterados.
                // O Repository usará o merge para não apagar fusoHorario, versaoApp, etc.
                ConfigPerfilUsuarioModel perfilUpdate = new ConfigPerfilUsuarioModel();
                perfilUpdate.setNome(novoNome);
                perfilUpdate.setEmail(usuarioAtual.getEmail());

                perfilRepository.atualizarPerfil(perfilUpdate)
                        .addOnSuccessListener(aVoid -> {
                            // 3. [AUDITORIA] Registra que o usuário esteve ativo hoje
                            perfilRepository.atualizarUltimaAtividade(usuarioAtual.getUid());

                            progressBar.setVisibility(View.GONE);
                            btnSalvarEdicao.setEnabled(true);
                            textNomePerfil.setText(novoNome);
                            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            alternarModo(false);
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnSalvarEdicao.setEnabled(true);
                            String msg = perfilRepository.mapearErroAutenticacao(e);
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        });
            } else {
                progressBar.setVisibility(View.GONE);
                btnSalvarEdicao.setEnabled(true);
                Toast.makeText(this, "Falha ao sincronizar Auth.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarDados() {
        textEmailPerfil.setText(usuarioAtual.getEmail());

        // Carrega foto com placeholder para evitar espaços vazios
        Uri urlFoto = usuarioAtual.getPhotoUrl();
        Glide.with(this)
                .load(urlFoto)
                .placeholder(R.drawable.ic_account_circle_24)
                .circleCrop()
                .into(imagePerfilUsuario);

        // Prioridade de exibição do nome
        String nome = usuarioAtual.getDisplayName();
        if (nome == null || nome.isEmpty()) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) nome = bundle.getString("nomeUsuario");
        }

        textNomePerfil.setText(nome != null ? nome : "Usuário OrgaFácil");
    }

    private void inicializarComponentes() {
        textNomePerfil = findViewById(R.id.textNomePerfil);
        textEmailPerfil = findViewById(R.id.textEmailPerfil);
        imagePerfilUsuario = findViewById(R.id.imagePerfilUsuario);
        layoutVisualizacao = findViewById(R.id.layoutVisualizacao);
        btnEditarDados = findViewById(R.id.btnEditarDados);
        layoutEdicao = findViewById(R.id.layoutEdicao);
        editNomePerfil = findViewById(R.id.editNomePerfil);
        btnSalvarEdicao = findViewById(R.id.btnSalvarEdicao);
        btnCancelarEdicao = findViewById(R.id.btnCancelarEdicao);
        progressBar = findViewById(R.id.progressBarPerfil);
    }
}