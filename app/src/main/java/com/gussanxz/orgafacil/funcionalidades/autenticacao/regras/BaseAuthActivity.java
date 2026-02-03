package com.gussanxz.orgafacil.funcionalidades.autenticacao.regras;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        // [SEGURANÇA] Bloqueio contra prints e gravação em todas as telas que herdam daqui
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * SAFETY NET UNIFICADA: Verifica existência do perfil e termos.
     */
    protected void iniciarFluxoSegurancaDados() {
        if (!FirebaseSession.isUserLogged()) return;
        if (getLoadingHelper() != null) getLoadingHelper().exibir();

        perfilRepository.verificarExistenciaPerfil().addOnCompleteListener(task -> {
            // [BOA PRÁTICA] Null check na task antes de verificar sucesso
            if (task != null && task.isSuccessful() && task.getResult() != null) {
                if (task.getResult().exists()) {

                    Boolean aceitouTermos = task.getResult().getBoolean("aceitouTermos");

                    if (aceitouTermos != null && aceitouTermos) {
                        navegarParaHome("Bem-vindo de volta!");
                    } else {
                        tratarNovoUsuario(autenticacao.getCurrentUser());
                    }

                } else {
                    tratarNovoUsuario(autenticacao.getCurrentUser());
                }
            } else {
                if (getLoadingHelper() != null) getLoadingHelper().ocultar();
                Toast.makeText(this, "Erro de sincronização com o servidor.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void tratarNovoUsuario(FirebaseUser user) {
        if (user == null) return;

        usuarioService.inicializarNovoUsuario(user, task -> {
            if (getLoadingHelper() != null) getLoadingHelper().ocultar();

            // [CORREÇÃO CRITICAL] Verificamos se a task não é nula antes de isSuccessful()
            // Isso evita o erro fatal caso o service falhe em retornar a task.
            if (task != null && task.isSuccessful()) {
                navegarParaHome("Conta configurada com sucesso!");
            } else {
                Toast.makeText(this, "Erro ao configurar perfil de usuário.", Toast.LENGTH_SHORT).show();
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