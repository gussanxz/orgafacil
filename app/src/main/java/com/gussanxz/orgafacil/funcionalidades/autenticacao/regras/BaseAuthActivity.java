package com.gussanxz.orgafacil.funcionalidades.autenticacao.regras;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.main.HomeActivity;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.ConfigPerfilUsuarioRepository;
import com.gussanxz.orgafacil.funcionalidades.usuario.dados.UsuarioService;
import com.gussanxz.orgafacil.util_helper.LoadingHelper;

public abstract class BaseAuthActivity extends AppCompatActivity {

    protected ConfigPerfilUsuarioRepository perfilRepository = new ConfigPerfilUsuarioRepository();
    protected UsuarioService usuarioService = new UsuarioService();
    protected FirebaseAuth autenticacao = ConfiguracaoFirestore.getFirebaseAutenticacao();

    protected abstract LoadingHelper getLoadingHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    protected void iniciarFluxoSegurancaDados() {
        if (!FirebaseSession.isUserLogged()) return;
        if (getLoadingHelper() != null) getLoadingHelper().exibir();

        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            // Se der erro na busca, aborta.
            if (task == null || !task.isSuccessful() || task.getResult() == null) {
                if (getLoadingHelper() != null) getLoadingHelper().ocultar();
                Toast.makeText(this, "Erro de conexão. Tente novamente.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (task.getResult().exists()) {
                // [CENÁRIO 1] Documento existe. Verifica o aceite.
                Boolean aceitou = task.getResult().getBoolean("aceitouTermos");

                if (aceitou != null && aceitou) {
                    navegarParaHome("Bem-vindo de volta!");
                } else {
                    // Documento existe, mas termos não aceitos (inconsistência).
                    if (getLoadingHelper() != null) getLoadingHelper().ocultar();
                    exibirDialogoTermos(autenticacao.getCurrentUser());
                }
            } else {
                // [CENÁRIO 2 - CRÍTICO] Documento NÃO existe.
                // O ERRO ESTAVA AQUI: Jamais chame tratarNovoUsuario aqui.
                // Devemos chamar o DIÁLOGO.
                if (getLoadingHelper() != null) getLoadingHelper().ocultar();
                exibirDialogoTermos(autenticacao.getCurrentUser());
            }
        });
    }

    private void exibirDialogoTermos(FirebaseUser user) {
        if (user == null) return;

        View viewDialog = getLayoutInflater().inflate(R.layout.dialog_termos, null);
        CheckBox checkTermos = viewDialog.findViewById(R.id.checkDialogTermos);
        Button btnAceitar = viewDialog.findViewById(R.id.btnAceitarTermos);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(viewDialog)
                .setCancelable(false)
                .setNegativeButton("Cancelar", (d, w) -> realizarLogoutDeLimpeza())
                .create();

        // Trava de saída pelo botão "Voltar" do celular
        dialog.setOnCancelListener(d -> realizarLogoutDeLimpeza());

        if (checkTermos != null && btnAceitar != null) {
            checkTermos.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btnAceitar.setEnabled(isChecked);
            });

            // [CRÍTICO] O usuário SÓ É CRIADO se clicar AQUI
            btnAceitar.setOnClickListener(v -> {
                dialog.dismiss();
                if (getLoadingHelper() != null) getLoadingHelper().exibir();
                tratarNovoUsuario(user);
            });
        }

        dialog.show();

        // [VISUAL] Configuração de Tamanho e Fundo Transparente
        if (dialog.getWindow() != null) {
            // Remove o fundo branco padrão do Android para respeitar o rounded corners do seu XML
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            // Ocupa 90% da largura e 85% da altura da tela
            int displayWidth = (int) (displayMetrics.widthPixels * 0.90);
            int displayHeight = (int) (displayMetrics.heightPixels * 0.85);
            dialog.getWindow().setLayout(displayWidth, displayHeight);
        }
    }

    private void realizarLogoutDeLimpeza() {
        autenticacao.signOut();
        Toast.makeText(this, "É necessário aceitar os termos.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void tratarNovoUsuario(FirebaseUser user) {
        if (user == null) return;

        // Aqui é onde o documento é realmente criado
        usuarioService.inicializarNovoUsuario(user, task -> {
            if (getLoadingHelper() != null) getLoadingHelper().ocultar();
            if (task != null && task.isSuccessful()) {
                navegarParaHome("Conta configurada com sucesso!");
            } else {
                Toast.makeText(this, "Erro ao configurar conta.", Toast.LENGTH_SHORT).show();
                autenticacao.signOut(); // Se falhar, desloga para tentar de novo depois
            }
        });
    }

    protected void navegarParaHome(String mensagem) {
        if (getLoadingHelper() != null) getLoadingHelper().ocultar();
        if (mensagem != null) Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}