package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
// IMPORTANTE: O BuildConfig correto é o do seu pacote, não do Firebase
import com.gussanxz.orgafacil.BuildConfig;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.UsuarioModel;

import java.util.ArrayList;
import java.util.List;

/**
 * UsuarioService
 * Responsável pela orquestração da CRIAÇÃO da conta.
 * ATUALIZADO: Preenche a nova estrutura aninhada do UsuarioModel.
 */
public class UsuarioService {

    private final FirebaseFirestore db;

    public UsuarioService() {
        this.db = ConfiguracaoFirestore.getFirestore();
    }

    public interface CriacaoCallback {
        void onResultado(Task<Void> task);
    }

    /**
     * Inicializa toda a estrutura do novo usuário no Firestore.
     */
    public void inicializarNovoUsuario(FirebaseUser user, CriacaoCallback callback) {
        if (user == null) return;

        String uid = user.getUid();
        WriteBatch batch = db.batch();

        // 1. Instancia o Model (O construtor já cria os sub-objetos internos)
        UsuarioModel novoUsuario = new UsuarioModel(
                uid,
                obterNome(user),
                user.getEmail(),
                obterProvedor(user)
        );

        Timestamp agora = Timestamp.now();

        // --- PREENCHIMENTO DOS DADOS ANINHADOS (MAPAS) ---

        // A. Dados Pessoais (Foto)
        if (user.getPhotoUrl() != null) {
            novoUsuario.getDadosPessoais().setFotoUrl(user.getPhotoUrl().toString());
        }

        // B. Termos de Uso
        novoUsuario.getTermosUso().setAceitouTermos(true);
        novoUsuario.getTermosUso().setDataAceite(agora.toDate());
        novoUsuario.getTermosUso().setVersaoTermos("1.0");

        // C. Dados da Conta (Ciclo de Vida)
        // Status e Plano já nascem preenchidos pelo construtor (ATIVO/GRATUITO)
        novoUsuario.getDadosConta().setDataCriacao(agora.toDate());
        novoUsuario.getDadosConta().setDataDesativacao(null);

        // D. Dados do App (Auditoria)
        novoUsuario.getDadosApp().setUltimaAtividade(agora.toDate());
        novoUsuario.getDadosApp().setVersaoApp(BuildConfig.VERSION_NAME);

        // --- FIM DO PREENCHIMENTO ---

        // Salva o usuário
        DocumentReference userRef = FirestoreSchema.userDoc(uid);
        batch.set(userRef, novoUsuario);

        // 2. Prepara o Resumo Financeiro Zerado
        criarResumoFinanceiroInicial(batch, uid);

        // 3. Cria as Categorias Padrão
        criarCategoriasPadrao(batch, uid);

        // 4. Comita tudo
        batch.commit().addOnCompleteListener(task -> {
            if (callback != null) callback.onResultado(task);
        });
    }

    // --- Helpers de Inicialização (Mantidos iguais) ---

    private void criarResumoFinanceiroInicial(WriteBatch batch, String uid) {
        ResumoFinanceiroModel resumo = new ResumoFinanceiroModel();
        DocumentReference resumoRef = db.collection("usuarios").document(uid)
                .collection("contas").document("resumo_geral");
        batch.set(resumoRef, resumo);
    }

    private void criarCategoriasPadrao(WriteBatch batch, String uid) {
        List<ContasCategoriaModel> padroes = new ArrayList<>();

        // RECEITAS
        padroes.add(novaCat("Salário", "ic_money", "#4CAF50", TipoCategoriaContas.RECEITA));
        padroes.add(novaCat("Investimentos", "ic_trending_up", "#2E7D32", TipoCategoriaContas.RECEITA));
        padroes.add(novaCat("Outras Receitas", "ic_add_circle", "#81C784", TipoCategoriaContas.RECEITA));

        // DESPESAS
        padroes.add(novaCat("Alimentação", "ic_restaurant", "#E53935", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Transporte", "ic_directions_car", "#1E88E5", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Moradia", "ic_home", "#FB8C00", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Lazer", "ic_movie", "#8E24AA", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Saúde", "ic_local_hospital", "#D81B60", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Educação", "ic_school", "#3949AB", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Outras Despesas", "ic_remove_circle", "#757575", TipoCategoriaContas.DESPESA));

        for (ContasCategoriaModel cat : padroes) {
            DocumentReference catRef = db.collection("usuarios").document(uid)
                    .collection("contas").document("categorias").collection("lista").document();
            cat.setId(catRef.getId());
            batch.set(catRef, cat);
        }
    }

    private ContasCategoriaModel novaCat(String nome, String icone, String cor, TipoCategoriaContas tipo) {
        ContasCategoriaModel c = new ContasCategoriaModel();
        c.setNome(nome);
        c.setIcone(icone);
        c.setCor(cor);
        c.setTipo(tipo.getId());
        c.setAtiva(true);
        c.setTotalGastoMesAtual(0);
        return c;
    }

    // --- Helpers de Extração ---

    private String obterNome(FirebaseUser user) {
        return (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : "Usuário";
    }

    private String obterProvedor(FirebaseUser user) {
        if (user.getProviderData() == null || user.getProviderData().isEmpty()) return "password";
        return (user.getProviderData().size() > 1) ? "google.com" : "password";
    }
}