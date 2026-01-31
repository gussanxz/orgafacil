package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseUser;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.ConfigPerfilUsuarioModel;

/**
 * SERVICE: USUÁRIO
 * Orquestra a criação coordenada entre Perfil e Carteira.
 * Agora utiliza a nova arquitetura onde os repositórios resolvem o UID internamente.
 */
public class UsuarioService {

    private final ConfigPerfilUsuarioRepository perfilRepository;
    private final CarteiraRepository carteiraRepository;

    public UsuarioService() {
        this.perfilRepository = new ConfigPerfilUsuarioRepository();
        this.carteiraRepository = new CarteiraRepository();
    }

    /**
     * Inicializa toda a estrutura do usuário de forma sequencial.
     * Pensa fora da caixa: Menos parâmetros, mais segurança.
     */
    public void inicializarNovoUsuario(FirebaseUser user, OnCompleteListener<Void> listener) {
        // 1. Etapa Inicial: Metadados na Raiz
        // Note: Passamos 'user' aqui apenas porque o repository extrai o provedor (google/senha)
        perfilRepository.inicializarMetadadosRaiz(user).addOnSuccessListener(aVoid -> {

            // 2. Criar Perfil (O Repository agora já sabe o UID via FirebaseSession)
            ConfigPerfilUsuarioModel perfil = new ConfigPerfilUsuarioModel();
            perfil.setNome(user.getDisplayName() != null ? user.getDisplayName() : "Usuário");
            perfil.setEmail(user.getEmail());

            perfilRepository.salvarPerfil(perfil).addOnSuccessListener(aVoid1 -> {

                // 3. Criar Carteira (Saldo inicial em centavos)
                carteiraRepository.inicializarSaldosContas().addOnSuccessListener(aVoid2 -> {

                    // 4. Criar Categorias Padrão
                    carteiraRepository.inicializarCategoriasPadrao().addOnSuccessListener(aVoid3 -> {

                        // SUCESSO TOTAL
                        listener.onComplete(null);

                    }).addOnFailureListener(e -> listener.onComplete(null));
                });
            });
        }).addOnFailureListener(e -> listener.onComplete(null));
    }
}