package com.gussanxz.orgafacil.funcionalidades.usuario.repository; // Ajuste o package se necessário

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.resumo_financeiro.modelos.ResumoFinanceiroModel;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.UsuarioModel;

import java.util.ArrayList;
import java.util.List;

/**
 * UsuarioService
 * Responsável pela orquestração da CRIAÇÃO da conta.
 * Garante que Usuário + Resumo Financeiro + Categorias Padrão sejam criados atomicamente.
 */
public class UsuarioService {

    private final FirebaseFirestore db;

    public UsuarioService() {
        this.db = ConfiguracaoFirestore.getFirestore();
    }

    // Interface para retorno simples para a Activity
    public interface CriacaoCallback {
        void onResultado(Task<Void> task);
    }

    /**
     * Inicializa toda a estrutura do novo usuário no Firestore.
     */
    public void inicializarNovoUsuario(FirebaseUser user, CriacaoCallback callback) {
        if (user == null) return;

        String uid = user.getUid();
        WriteBatch batch = db.batch(); // Inicia transação atômica

        // 1. Prepara Identidade (UsuarioModel)
        UsuarioModel novoUsuario = new UsuarioModel(
                uid,
                obterNome(user),
                user.getEmail(),
                obterProvedor(user)
        );
        if (user.getPhotoUrl() != null) {
            novoUsuario.setFotoUrl(user.getPhotoUrl().toString());
        }

        DocumentReference userRef = FirestoreSchema.userDoc(uid);
        batch.set(userRef, novoUsuario);

        // 2. Prepara o Resumo Financeiro (O "Documento Mágico") Zerado
        criarResumoFinanceiroInicial(batch, uid);

        // 3. Cria as Categorias Padrão (Receitas e Despesas)
        criarCategoriasPadrao(batch, uid);

        // 4. Comita tudo de uma vez
        batch.commit().addOnCompleteListener(task -> {
            if (callback != null) callback.onResultado(task);
        });
    }

    // --- Helpers de Inicialização ---

    private void criarResumoFinanceiroInicial(WriteBatch batch, String uid) {
        // Cria o model zerado (saldos = 0)
        ResumoFinanceiroModel resumo = new ResumoFinanceiroModel();

        // Caminho: users/{uid}/contas/resumo_geral
        // Nota: Ajuste se seu FirestoreSchema não tiver método estático com UID explícito
        DocumentReference resumoRef = db.collection("usuarios").document(uid)
                .collection("contas").document("resumo_geral");

        batch.set(resumoRef, resumo);
    }

    private void criarCategoriasPadrao(WriteBatch batch, String uid) {
        List<ContasCategoriaModel> padroes = new ArrayList<>();

        // --- RECEITAS ---
        padroes.add(novaCat("Salário", "ic_money", "#4CAF50", TipoCategoriaContas.RECEITA));
        padroes.add(novaCat("Investimentos", "ic_trending_up", "#2E7D32", TipoCategoriaContas.RECEITA));
        padroes.add(novaCat("Outras Receitas", "ic_add_circle", "#81C784", TipoCategoriaContas.RECEITA));

        // --- DESPESAS ---
        padroes.add(novaCat("Alimentação", "ic_restaurant", "#E53935", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Transporte", "ic_directions_car", "#1E88E5", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Moradia", "ic_home", "#FB8C00", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Lazer", "ic_movie", "#8E24AA", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Saúde", "ic_local_hospital", "#D81B60", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Educação", "ic_school", "#3949AB", TipoCategoriaContas.DESPESA));
        padroes.add(novaCat("Outras Despesas", "ic_remove_circle", "#757575", TipoCategoriaContas.DESPESA));

        // Adiciona todas ao Batch
        for (ContasCategoriaModel cat : padroes) {
            DocumentReference catRef = db.collection("usuarios").document(uid)
                    .collection("contas").document("categorias").collection("lista").document();
            cat.setId(catRef.getId()); // Define o ID gerado no objeto
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
        // Regra de Ouro: Valores monetários em INT (0 centavos)
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