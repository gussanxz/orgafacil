package com.gussanxz.orgafacil.funcionalidades.usuario.dados;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
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
 * Gerencia a inicialização da estrutura financeira do usuário.
 */
public class CarteiraRepository {

    /**
     * Inicializa o documento 'geral' de resumos com saldo zerado.
     * Usa o caminho centralizado: contas_resumos/geral
     */
    public Task<Void> inicializarSaldosContas() {
        CarteiraUsuarioModel carteira = new CarteiraUsuarioModel();

        // Seguindo o padrão de centavos que estabelecemos
        Map<String, Object> dadosIniciais = new HashMap<>();
        dadosIniciais.put("saldoTotalCent", 0);
        dadosIniciais.put("updatedAt", FieldValue.serverTimestamp());

        // Documento central de saldos: usuarios/{uid}/moduloSistema/contas/contas_resumos/geral
        return FirestoreSchema.myUserDoc()
                .collection(FirestoreSchema.MODULO)
                .document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_RESUMOS)
                .document("geral")
                .set(dadosIniciais, SetOptions.merge());
    }

    /**
     * Cria as categorias padrão usando as coleções mapeadas no Schema.
     */
    public Task<Void> inicializarCategoriasPadrao() {
        List<String> categorias = Arrays.asList(
                "Alimentação", "Aluguel", "Educação", "Lazer", "Mercado", "Saúde"
        );

        WriteBatch batch = ConfiguracaoFirestore.getFirestore().batch();

        int ordem = 0;
        for (String nome : categorias) {
            String slugId = gerarSlug(nome);

            // Usa o método do Schema para pegar a coleção correta sem hardcode
            DocumentReference ref = FirestoreSchema.contasCategoriasCol().document(slugId);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("tipo", "Despesa");
            doc.put("ativo", true);
            doc.put("ordem", ordem++);
            doc.put("createdAt", FieldValue.serverTimestamp());

            batch.set(ref, doc, SetOptions.merge());
        }

        return batch.commit();
    }

    /**
     * Transforma nomes em IDs amigáveis (slugs).
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