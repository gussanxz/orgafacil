package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

/**
 * PerfilActivity
 * Atua na camada Visual para gerenciar a exibição e edição dos dados do usuário.
 * Implementa feedback de carregamento (ProgressBar) e isolamento via Repositório.
 */
public class PerfilActivity extends AppCompatActivity {

    // Componentes de Visualização (Modo Leitura)
    private TextView textNomePerfil, textEmailPerfil;
    private LinearLayout layoutVisualizacao, btnEditarDados;
    private ImageView imagePerfilUsuario;

    // Componentes de Edição (Modo Escrita)
    private LinearLayout layoutEdicao;
    private TextInputEditText editNomePerfil;
    private Button btnSalvarEdicao, btnCancelarEdicao;

    // Feedback Visual
    private ProgressBar progressBar;

    // Firebase e Persistência
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;
    private ConfigPerfilUsuarioRepository perfilRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_meu_perfil);

        perfilRepository = new ConfigPerfilUsuarioRepository(); // Nome novo
        inicializarComponentes();

        // 2. Recupera a instância do usuário logado
        autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();
        usuarioAtual = autenticacao.getCurrentUser();

        // 3. Preenche a tela com os dados atuais do Firebase
        carregarDados();

        // --- CLIQUES ---
        btnEditarDados.setOnClickListener(v -> alternarModo(true));
        btnCancelarEdicao.setOnClickListener(v -> alternarModo(false));
        btnSalvarEdicao.setOnClickListener(v -> salvarAlteracoes());
    }

    /**
     * Alterna a visibilidade entre o modo de leitura e o formulário de edição.
     */
    private void alternarModo(boolean modoEdicao) {
        if (modoEdicao) {
            // Entrando no modo edição: preenche o campo com o nome atual
            editNomePerfil.setText(textNomePerfil.getText().toString());
            layoutVisualizacao.setVisibility(View.GONE);
            layoutEdicao.setVisibility(View.VISIBLE);
        } else {
            // Saindo do modo edição
            layoutEdicao.setVisibility(View.GONE);
            layoutVisualizacao.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Salva as alterações de perfil.
     * Implementa o bloqueio de UI e exibição de ProgressBar para evitar bugs de estado.
     */
    private void salvarAlteracoes() {
        String novoNome = editNomePerfil.getText().toString().trim();
        if (novoNome.isEmpty()) {
            editNomePerfil.setError("Nome não pode ser vazio");
            return;
        }

        if (usuarioAtual == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnSalvarEdicao.setEnabled(false);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(novoNome)
                .build();

        usuarioAtual.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    // Feedback visual encerrado apenas se a primeira etapa falhar;
                    // caso contrário, aguarda o Firestore. [cite: 2025-11-10]
                    if (!task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        btnSalvarEdicao.setEnabled(true);
                        Toast.makeText(this, "Erro ao atualizar autenticação.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // USA O NOVO MODELO E REPOSITÓRIO [cite: 2026-01-22]
                    ConfigPerfilUsuarioModel perfilUpdate = new ConfigPerfilUsuarioModel();
                    perfilUpdate.setIdUsuario(usuarioAtual.getUid());
                    perfilUpdate.setNome(novoNome);
                    perfilUpdate.setEmail(usuarioAtual.getEmail());

                    // TRATATIVA ADICIONAL: Garante que o banco também foi atualizado antes de confirmar ao usuário. [cite: 2025-11-10]
                    perfilRepository.atualizarPerfil(perfilUpdate)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                btnSalvarEdicao.setEnabled(true);
                                textNomePerfil.setText(novoNome);
                                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                                alternarModo(false);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnSalvarEdicao.setEnabled(true);
                                // Utiliza o mapeador de erros do Repository para feedback preciso. [cite: 2025-11-10]
                                String mensagemErro = perfilRepository.mapearErroAutenticacao(e);
                                Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show();
                            });
                });
    }

    /**
     * Preenche os campos da tela usando os dados do FirebaseUser ou Intents.
     */
    private void carregarDados() {
        if (usuarioAtual != null) {
            textEmailPerfil.setText(usuarioAtual.getEmail());

            // Carregamento de imagem com Glide (otimizado com crop circular)
            Uri urlFoto = usuarioAtual.getPhotoUrl();
            if (urlFoto != null) {
                Glide.with(this).load(urlFoto).circleCrop().into(imagePerfilUsuario);
            }

            // Prioriza o nome vindo do Bundle (se houver) ou do DisplayName do Auth
            Bundle bundle = getIntent().getExtras();
            if (bundle != null && bundle.getString("nomeUsuario") != null) {
                textNomePerfil.setText(bundle.getString("nomeUsuario"));
            } else {
                textNomePerfil.setText(usuarioAtual.getDisplayName());
            }
        }
    }

    /**
     * Vincula as variáveis Java aos IDs definidos no arquivo XML.
     */
    private void inicializarComponentes() {
        // Componentes de Visualização
        textNomePerfil = findViewById(R.id.textNomePerfil);
        textEmailPerfil = findViewById(R.id.textEmailPerfil);
        imagePerfilUsuario = findViewById(R.id.imagePerfilUsuario);
        layoutVisualizacao = findViewById(R.id.layoutVisualizacao);
        btnEditarDados = findViewById(R.id.btnEditarDados);

        // Componentes de Edição
        layoutEdicao = findViewById(R.id.layoutEdicao);
        editNomePerfil = findViewById(R.id.editNomePerfil);
        btnSalvarEdicao = findViewById(R.id.btnSalvarEdicao);
        btnCancelarEdicao = findViewById(R.id.btnCancelarEdicao);

        // Progress Bar
        progressBar = findViewById(R.id.progressBarPerfil);
    }
}