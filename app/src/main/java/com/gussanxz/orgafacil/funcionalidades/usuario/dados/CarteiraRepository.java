package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class CarteiraRepository {

    private final FirebaseFirestore db;

    public CarteiraRepository() {
        this.db = ConfiguracaoFirestore.getFirestore();
    }

    /**
     * Adiciona a criação da carteira ao Batch (Não salva ainda, apenas prepara).
     */
    public void prepararCarteiraInicial(WriteBatch batch, String uid) {
        // Caminho: users/{uid}/carteira/geral
        DocumentReference ref = db.collection("users").document(uid)
                .collection("carteira").document("geral");

        Map<String, Object> dadosIniciais = new HashMap<>();
        dadosIniciais.put("nome", "Carteira Principal");
        dadosIniciais.put("saldo", 0.0); // Usando double para facilitar visualização
        dadosIniciais.put("tipo", "Principal");
        dadosIniciais.put("criadoEm", FieldValue.serverTimestamp());
        dadosIniciais.put("atualizadoEm", FieldValue.serverTimestamp());

        batch.set(ref, dadosIniciais);
    }

    /**
     * Adiciona a criação das categorias ao Batch.
     */
    public void prepararCategoriasPadrao(WriteBatch batch, String uid) {
        List<String> despesas = Arrays.asList(
                "Alimentação", "Moradia", "Transporte", "Lazer", "Saúde", "Educação"
        );

        List<String> receitas = Arrays.asList(
                "Salário", "Investimentos", "Extra"
        );

        // Cria Despesas
        int ordem = 0;
        for (String nome : despesas) {
            String slug = gerarSlug(nome);
            DocumentReference ref = db.collection("users").document(uid)
                    .collection("categorias_despesas").document(slug);

            batch.set(ref, montarMapaCategoria(nome, "Despesa", ordem++));
        }

        // Cria Receitas
        ordem = 0;
        for (String nome : receitas) {
            String slug = gerarSlug(nome);
            DocumentReference ref = db.collection("users").document(uid)
                    .collection("categorias_receitas").document(slug);

            batch.set(ref, montarMapaCategoria(nome, "Receita", ordem++));
        }
    }

    private Map<String, Object> montarMapaCategoria(String nome, String tipo, int ordem) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nome);
        doc.put("tipo", tipo);
        doc.put("ativa", true);
        doc.put("ordem", ordem);
        doc.put("icone", "ic_cat_padrao"); // Placeholder para ícone
        return doc;
    }

    private String gerarSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }
}