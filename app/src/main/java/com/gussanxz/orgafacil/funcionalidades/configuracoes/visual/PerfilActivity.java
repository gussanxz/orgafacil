package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioRepository;

public class PerfilActivity extends AppCompatActivity {

    private TextView textNomePerfil, textEmailPerfil;
    private LinearLayout layoutVisualizacao, btnEditarDados, layoutEdicao;
    private ImageView imagePerfilUsuario;
    private TextInputEditText editNomePerfil;
    private Button btnSalvarEdicao, btnCancelarEdicao;
    private ProgressBar progressBar;

    private FirebaseUser usuarioAtual;
    private UsuarioRepository usuarioRepository; // Renomeado para consistência

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_meu_perfil);

        // [SEGURANÇA] Impede prints em tela de dados sensíveis
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        usuarioRepository = new UsuarioRepository();
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

        // 1. Atualiza no Firebase Auth (Identidade da Sessão - Local e Nuvem)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(novoNome)
                .build();

        usuarioAtual.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // 2. [CORREÇÃO] Atualiza no Banco de Dados usando o novo método simplificado
                // Passamos null na fotoUrl pois não estamos alterando a foto agora
                usuarioRepository.atualizarDadosPerfil(novoNome, null)
                        .addOnSuccessListener(aVoid -> {

                            // 3. [AUDITORIA] Registra atividade (método sem argumentos agora)
                            usuarioRepository.atualizarUltimaAtividade();

                            progressBar.setVisibility(View.GONE);
                            btnSalvarEdicao.setEnabled(true);

                            // Atualiza a tela
                            textNomePerfil.setText(novoNome);
                            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            alternarModo(false);
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnSalvarEdicao.setEnabled(true);
                            String msg = usuarioRepository.mapearErroAutenticacao(e);
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

        // Carrega foto com placeholder
        Uri urlFoto = usuarioAtual.getPhotoUrl();
        Glide.with(this)
                .load(urlFoto)
                .placeholder(R.drawable.ic_account_circle_24) // Certifique-se que este ícone existe
                .circleCrop()
                .into(imagePerfilUsuario);

        // Prioridade de exibição do nome
        String nome = usuarioAtual.getDisplayName();

        // Fallback: Tenta pegar do Intent se o Auth ainda não tiver carregado (raro, mas possível)
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