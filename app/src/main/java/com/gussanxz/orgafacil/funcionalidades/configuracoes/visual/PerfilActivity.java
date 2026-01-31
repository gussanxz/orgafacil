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
import com.gussanxz.orgafacil.funcionalidades.configuracoes.dados.UsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.negocio.modelos.Usuario;

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
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_configs_meu_perfil);

        // 1. Inicializa o repositório e componentes da interface
        usuarioRepository = new UsuarioRepository();
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

        // Validação básica de campo vazio
        if (novoNome.isEmpty()) {
            editNomePerfil.setError("Nome não pode ser vazio");
            return;
        }

        if (usuarioAtual == null) return;

        // INÍCIO DO FEEDBACK DE CARREGAMENTO
        // Exibe a barra de progresso e desativa o botão para evitar cliques múltiplos
        progressBar.setVisibility(View.VISIBLE);
        btnSalvarEdicao.setEnabled(false);

        // 1. Atualiza o Perfil no Firebase Authentication (Sessão local)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(novoNome)
                .build();

        usuarioAtual.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    // Independente de sucesso ou falha, encerra o feedback visual
                    progressBar.setVisibility(View.GONE);
                    btnSalvarEdicao.setEnabled(true);

                    if (task.isSuccessful()) {
                        // 2. Atualiza no Firestore através do Repositório (Persistência remota)
                        Usuario usuarioUpdate = new Usuario();
                        usuarioUpdate.setIdUsuario(usuarioAtual.getUid());
                        usuarioUpdate.setNome(novoNome);

                        // Chamada delegada ao Repository para isolar a lógica de rede
                        usuarioRepository.atualizarPerfil(usuarioUpdate);

                        // 3. Sucesso: Atualiza a interface e volta ao modo leitura
                        textNomePerfil.setText(novoNome);
                        Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        alternarModo(false);
                    } else {
                        Toast.makeText(this, "Erro ao atualizar autenticação.", Toast.LENGTH_SHORT).show();
                    }
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