package com.gussanxz.orgafacil.model;

import com.gussanxz.orgafacil.config.ConfiguracaoFirestore;
import android.util.Log;

// Antes era: com.google.firebase.database.Exclude
// Agora é:
import com.google.firebase.firestore.Exclude;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.text.Normalizer;

public class Usuario {

    private String idUsuario;
    private String nome;
    private String email;
    private String senha;


    // Esses campos servem apenas para leitura ou uso local.
    // Não vamos enviá-los no metodo salvar() padrão para não zerar o banco.
    private Double proventosTotal = 0.00;
    private Double despesaTotal = 0.00;

    //Construtor vazio para o Firestore
    public Usuario() {
    }


     // Este metodo deve ser usado para atualizar dados cadastrais (Nome, Email).
     // ELE NÃO MEXE NO SALDO FINANCEIRO.
    public void salvar() {

        // Instancia do Firestore
        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();

        // Id do usuario
        String uid = getIdUsuario();

        // --- VALIDAÇÃO DO ID ---
        // Se o ID estiver vazio, não temos onde salvar os dados.
        if (uid == null || uid.trim().isEmpty()) {
            Log.e("FireStore", "Usuario.salvar(): uid vazio");
            return; // Para a execução do método aqui para evitar crash.
        }

        // 1) users/{uid} com perfil
        // --- ETAPA 1: DADOS PESSOAIS (PERFIL) ---

        // Criamos um mapa (Map) que representa o documento JSON que vai para o banco.
        Map<String, Object> userDoc = new HashMap<>();

        userDoc.put("nome", getNome());
        userDoc.put("email", getEmail());
        // Adicionamos um carimbo de data/hora do servidor (Server Timestamp).
        userDoc.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // ACESSO AO FIRESTORE:
        // Caminho: coleção "users" -> documento com o ID do usuário (uid)
        fs.collection("users").document(uid)

                // O 'merge' é CRUCIAL aqui. Ele diz: "Se o documento já existir,
                // atualize apenas os campos 'perfil' e 'createdAt', mas NÃO apague o resto".
                .set(userDoc, SetOptions.merge())

                // Listener de falha: Avisa no Logcat se der erro (ex: sem internet, sem permissão).
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao salvar perfil", e));
    }

    public void inicializarNovosDados() {

        // Instancia do Firestore
        FirebaseFirestore fs = ConfiguracaoFirestore.getFirestore();

        // Id do usuario
        String uid = getIdUsuario();

        if (uid == null) return;

        // 1) Criar documento da conta (Financeiro) ZERADO
        Map<String, Object> resumo = new HashMap<>();
        resumo.put("proventosTotal", 0.0);
        resumo.put("despesaTotal", 0.0);
        resumo.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .set(resumo, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("FireStore", "Erro ao iniciar conta", e));

        // 2) Criar categorias
        seedCategoriasPadrao(fs, uid);
    }

    // Metodo auxiliar que cria categorias iniciais (Alimentação, Lazer, etc.) automaticamente
    private void seedCategoriasPadrao(FirebaseFirestore fs, String uid) {

        // Lista fixa de nomes que queremos criar
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        // WriteBatch: Ferramenta do Firestore para fazer várias gravações de uma vez só.
        WriteBatch batch = fs.batch();

        for (String nome : padroes) {
            // Gera um ID limpo (ex: "Doações e caridades" -> "doacoes_e_caridades")
            String catId = gerarSlug(nome);
            Log.d("FireStore", "Categoria padrão setadas pro usuário");

            // Define o endereço exato onde a categoria vai morar:
            // users -> {uid} -> contas -> main -> categorias -> {catId}
            DocumentReference ref = fs.collection("users").document(uid)
                    .collection("contas").document("main")
                    .collection("categorias").document(catId);

            // Cria o "pacotinho" de dados da categoria
            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome); // Salva o nome bonito (com acento)

            // Registra quando foi criado
            doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            // Adiciona essa operação na fila de espera do Batch (ainda não enviou para a internet)
            batch.set(ref, doc, SetOptions.merge());
        }

        // commit(): Agora sim! Envia todas as operações acumuladas para o servidor.
        batch.commit()
                .addOnSuccessListener(unused -> Log.d("FireStore", "Seed categorias OK"))
                .addOnFailureListener(e -> Log.e("FireStore", "Erro seed categorias", e));
    }

    // Helper para limpar strings (substitui aquele monte de .replace)
    private String gerarSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace(" ", "_");
    }

    // --- GETTERS E SETTERS ---
    public Double getProventosTotal() {
        return proventosTotal;
    }

    public void setProventosTotal(Double proventosTotal) {
        this.proventosTotal = proventosTotal;
    }

    public Double getDespesaTotal() {
        return despesaTotal;
    }

    public void setDespesaTotal(Double despesaTotal) {
        this.despesaTotal = despesaTotal;
    }

    // Diz ao Firestore: "Não salve o campo 'idUsuario' dentro do documento JSON".
    // Motivo: O ID já é o nome do documento (a chave da gaveta), não precisamos repetir ele dentro.
    @Exclude
    public String getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }


    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    // Garante que a senha nunca seja enviada para o banco de dados aberto.
    // A senha fica apenas no sistema de Autenticação (Auth), nunca no Firestore.
    @Exclude
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
}
