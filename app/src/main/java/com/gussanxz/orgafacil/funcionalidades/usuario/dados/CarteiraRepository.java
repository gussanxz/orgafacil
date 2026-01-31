package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.usuario.r_negocio.modelos.CarteiraUsuarioModel;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * REPOSITORY: CARTEIRA E FINANCEIRO
 * Gerencia o saldo e a estrutura de contas/categorias do usuário.
 * Caminho: teste > UID > moduloSistema > contas [cite: 2026-01-26]
 */
public class CarteiraRepository {

    private static final String TAG = "CarteiraRepository";

    /**
     * Inicializa o resumo financeiro diretamente no documento 'contas'.
     * Agora retorna a Task para que possamos encadear processos no Service. [cite: 2025-11-10]
     */
    public Task<Void> inicializarSaldosContas(String uid) {
        CarteiraUsuarioModel carteira = new CarteiraUsuarioModel();

        return FirestoreSchema.userDoc(uid)
                .collection("moduloSistema")
                .document("contas")
                .set(carteira, SetOptions.merge());
    } // <-- A CHAVE QUE FALTA ESTÁ AQUI

    /**
     * Cria as categorias padrão dentro da estrutura: moduloSistema > contas > contas_categorias
     * Retorna a Task do commit do batch. [cite: 2026-01-26]
     */
    public Task<Void> inicializarCategoriasPadrao(String uid) {
        List<String> categorias = Arrays.asList(
                "Alimentação", "Aluguel", "Educação", "Lazer", "Mercado", "Saúde"
        );

        FirebaseFirestore fs = FirestoreSchema.db();
        WriteBatch batch = fs.batch();

        int ordem = 0;
        for (String nome : categorias) {
            String slugId = gerarSlug(nome);

            // Caminho conforme validado no console: moduloSistema > contas > contas_categorias [cite: 2026-01-26]
            DocumentReference ref = FirestoreSchema.userDoc(uid)
                    .collection("moduloSistema")
                    .document("contas")
                    .collection("contas_categorias")
                    .document(slugId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("tipo", "Despesa");
            doc.put("ordem", ordem++);
            doc.put("createdAt", FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        return batch.commit();
    }

    /**
     * Transforma nomes em IDs amigáveis (slugs). [cite: 2026-01-26]
     */
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