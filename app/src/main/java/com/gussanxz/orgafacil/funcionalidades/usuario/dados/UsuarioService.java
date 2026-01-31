package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

/**
 * SERVICE: USUÁRIO
 * Orquestra a criação coordenada entre Perfil e Carteira.
 */
public class UsuarioService {

    private final ConfigPerfilUsuarioRepository perfilRepository;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService() {
        this.perfilRepository = new ConfigPerfilUsuarioRepository();
        this.carteiraRepository = new CarteiraRepository();
    }

    /**
     * Inicializa toda a estrutura do usuário de forma sequencial e segura.
     * Garante que cada etapa seja concluída antes de iniciar a próxima.
     */
    public void inicializarNovoUsuario(FirebaseUser user, OnCompleteListener<Void> listener) {
        String uid = user.getUid();

        // 1. Etapa Inicial: Metadados na Raiz
        perfilRepository.inicializarMetadadosRaiz(user).addOnSuccessListener(aVoid -> {

            // 2. Criar Perfil
            ConfigPerfilUsuarioModel perfil = new ConfigPerfilUsuarioModel();
            perfil.setIdUsuario(uid);
            perfil.setNome(user.getDisplayName() != null ? user.getDisplayName() : "Usuário");
            perfil.setEmail(user.getEmail());

            perfilRepository.salvarPerfil(perfil).addOnSuccessListener(aVoid1 -> {

                // 3. Criar Carteira (Documento 'contas')
                carteiraRepository.inicializarSaldosContas(uid).addOnSuccessListener(aVoid2 -> {

                    // 4. Criar Categorias Padrão
                    carteiraRepository.inicializarCategoriasPadrao(uid).addOnSuccessListener(aVoid3 -> {

                        // SUCESSO TOTAL: Notifica que toda a estrutura foi criada
                        listener.onComplete(null);

                    }).addOnFailureListener(e -> listener.onComplete(null)); // Trate erros conforme sua lógica
                });
            });
        }).addOnFailureListener(e -> listener.onComplete(null));
    }
}