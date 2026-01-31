package com.gussanxz.orgafacil.funcionalidades.configuracoes.visual;

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
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.configuracoes.dados.UsuarioRepository; // Repositório unificado de dados

/**
 * Activity responsável pelas configurações de segurança do usuário.
 * Gerencia o bloqueio por PIN (local), recuperação de senha e exclusão de conta.
 */
public class SegurancaActivity extends AppCompatActivity {

    // Componentes de Interface (UI)
    private SwitchCompat switchAtivarPin;
    private LinearLayout itemAlterarSenha, itemRecuperarSenha, itemExcluirConta;

    // Gerenciador de preferências locais (armazenamento simples no dispositivo)
    private SharedPreferences prefs;

    // Firebase e Repositório
    private FirebaseUser usuarioAtual;
    private UsuarioRepository usuarioRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Habilita o layout de ponta a ponta (sob barras de status/navegação)
        setContentView(R.layout.ac_main_configs_seguranca);

        // Ajusta o padding do layout principal para não ficar escondido sob as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // 1. Inicializa o repositório centralizado de usuários
        usuarioRepository = new UsuarioRepository();

        // 2. Obtém a instância do usuário logado no Firebase Auth
        usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();

        // 3. Inicializa o arquivo de preferências locais para configurações rápidas
        prefs = getSharedPreferences("OrgaFacilPrefs", MODE_PRIVATE);

        // 4. Vincula os IDs do XML aos objetos Java
        bindViews();

        // 5. Aplica o estado salvo nas SharedPreferences aos componentes
        carregarPreferencias();

        // 6. Define as ações de clique e mudança de estado
        configurarListeners();
    }

    /**
     * Associa as variáveis aos componentes do arquivo de layout.
     */
    private void bindViews() {
        switchAtivarPin = findViewById(R.id.switchAtivarPin);
        itemAlterarSenha = findViewById(R.id.itemAlterarSenha);
        itemRecuperarSenha = findViewById(R.id.itemRecuperarSenha);
        itemExcluirConta = findViewById(R.id.itemExcluirConta);
    }

    /**
     * Recupera configurações locais, como a obrigatoriedade do PIN ao abrir o app.
     */
    private void carregarPreferencias() {
        // Busca o valor de 'pin_obrigatorio', assumindo 'true' como padrão caso nunca tenha sido salvo
        boolean pinObrigatorio = prefs.getBoolean("pin_obrigatorio", true);
        switchAtivarPin.setChecked(pinObrigatorio);
    }

    /**
     * Define o comportamento dos botões e switches da tela.
     */
    private void configurarListeners() {
        // Monitora a mudança no Switch de PIN e salva imediatamente no dispositivo
        switchAtivarPin.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("pin_obrigatorio", isChecked).apply()
        );

        // Navega para a tela de alteração de senha (fluxo interno do app)
        itemAlterarSenha.setOnClickListener(v ->
                startActivity(new Intent(this, AlterarSenhaActivity.class))
        );

        // Aciona o envio de e-mail de reset de senha delegando para o Repositório
        itemRecuperarSenha.setOnClickListener(v -> {
            if (usuarioAtual != null && usuarioAtual.getEmail() != null) {
                usuarioRepository.enviarEmailRecuperacao(usuarioAtual.getEmail(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Exibe o diálogo de confirmação antes de excluir a conta
        itemExcluirConta.setOnClickListener(v -> confirmarExclusaoConta());
    }

    /**
     * Exibe um alerta de confirmação para evitar exclusões acidentais.
     */
    private void confirmarExclusaoConta() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir conta definitivamente")
                .setMessage("Todos os seus dados de vendas e contas serão apagados. Deseja continuar?")
                .setPositiveButton("Excluir", (dialog, which) -> excluirConta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Processa a exclusão da conta chamando o repositório.
     * Este processo envolve apagar dados no Firestore e deletar o usuário no Auth.
     */
    private void excluirConta() {
        if (usuarioAtual == null) return;

        // Delegamos a exclusão completa para o Repository para manter a Activity focada na UI
        usuarioRepository.excluirContaCompleta(usuarioAtual, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Conta excluída com sucesso.", Toast.LENGTH_SHORT).show();
                finish(); // Encerra a Activity e retorna à tela anterior (ou login)
            } else {
                // O Firebase exige que o usuário tenha feito login recentemente para deletar a conta
                Toast.makeText(this, "Falha: Re-autentique-se para realizar esta operação sensível.", Toast.LENGTH_LONG).show();
            }
        });
    }
}