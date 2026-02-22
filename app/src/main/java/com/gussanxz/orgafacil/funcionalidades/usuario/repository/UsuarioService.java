package com.gussanxz.orgafacil.funcionalidades.usuario.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.BuildConfig;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.modelos.UsuarioModel;

import java.util.ArrayList;
import java.util.List;

public class UsuarioService {

    private final FirebaseFirestore db;

    public UsuarioService() {
        this.db = ConfiguracaoFirestore.getFirestore();
    }

    public interface CriacaoCallback {
        void onResultado(Task<Void> task);
    }

    public void inicializarNovoUsuario(FirebaseUser user, CriacaoCallback callback) {
        if (user == null) return;

        String uid = user.getUid();
        WriteBatch batch = db.batch();

        UsuarioModel novoUsuario = new UsuarioModel(
                uid,
                obterNome(user),
                user.getEmail(),
                obterProvedor(user)
        );

        Timestamp agora = FirestoreSchema.nowTs();

        // Dados do Usuário
        if (user.getPhotoUrl() != null) {
            novoUsuario.getDadosPessoais().setFotoUrl(user.getPhotoUrl().toString());
        }
        novoUsuario.getTermosUso().setAceitouTermos(true);
        novoUsuario.getTermosUso().setDataAceite(agora.toDate());
        novoUsuario.getTermosUso().setVersaoTermos("1.0");
        novoUsuario.getDadosConta().setDataCriacao(agora.toDate());
        novoUsuario.getDadosApp().setUltimaAtividade(agora.toDate());
        novoUsuario.getDadosApp().setVersaoApp(BuildConfig.VERSION_NAME);

        // 1. Salva o documento principal usando o ROOT ("teste") do Schema
        DocumentReference userRef = FirestoreSchema.userDoc(uid);
        batch.set(userRef, novoUsuario);

        // 2. Prepara o Resumo Financeiro (Subcoleção contas)
        criarResumoFinanceiroInicial(batch);

        // 3. Cria as Categorias Padrão
        criarCategoriasPadrao(batch);

        // 4. Executa a operação atômica
        batch.commit().addOnCompleteListener(task -> {
            if (callback != null) callback.onResultado(task);
        });
    }

    private void criarResumoFinanceiroInicial(WriteBatch batch) {
        ResumoFinanceiroModel resumo = new ResumoFinanceiroModel();
        // USANDO O SCHEMA: Caminho: teste/{uid}/contas/resumo_geral
        DocumentReference resumoRef = FirestoreSchema.contasResumoDoc();
        batch.set(resumoRef, resumo);
    }

    private void criarCategoriasPadrao(WriteBatch batch) {
        List<ContasCategoriaModel> padroes = new ArrayList<>();

        // RECEITAS
        padroes.add(novaCat("Salário", "ic_money", "#4CAF50", TipoCategoriaContas.RECEITA));
        padroes.add(novaCat("Investimentos", "ic_trending_up", "#2E7D32", TipoCategoriaContas.RECEITA));

        // DESPESAS
        padroes.add(novaCat("Alimentação", "ic_restaurant", "#E53935", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Transporte", "ic_directions_car", "#1E88E5", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Moradia", "ic_home", "#FB8C00", TipoCategoriaContas.DESPESA));

        for (ContasCategoriaModel cat : padroes) {

            // 1. Geramos o ID amigável (Slug)
            String slug = gerarSlug(cat.getVisual().getNome());

            // 2. Usamos o slug como ID no documento do Firestore via Schema
            DocumentReference catRef = FirestoreSchema.contasCategoriasCol().document(slug);

            // 3. Sincronizamos o ID no modelo de dados
            cat.setId(catRef.getId()); // O ID do modelo será o próprio slug
            batch.set(catRef, cat);
        }
    }

    private ContasCategoriaModel novaCat(String nome, String icone, String cor, TipoCategoriaContas tipo) {
        ContasCategoriaModel c = new ContasCategoriaModel();
        c.getVisual().setNome(nome);
        c.getVisual().setIcone(icone);
        c.getVisual().setCor(cor);
        c.getFinanceiro().setTotalGastoMesAtual(0); // Inteiro conforme sua regra
        c.getFinanceiro().setLimiteMensal(0);
        c.setTipo(tipo.getId());
        c.setAtiva(true);
        return c;
    }

    /**
     * Transforma nomes com acentos e espaços em IDs limpos (ex: "Educação" -> "educacao")
     */
    private String gerarSlug(String nome) {
        if (nome == null) return "categoria_" + System.currentTimeMillis();
        return nome.toLowerCase()
                .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u")
                .replace("ç", "c")
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private String obterNome(FirebaseUser user) {
        return (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : "Usuário";
    }

    private String obterProvedor(FirebaseUser user) {
        if (user.getProviderData() == null || user.getProviderData().isEmpty()) return "password";
        return (user.getProviderData().size() > 1) ? "google.com" : "password";
    }
}