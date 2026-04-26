package com.gussanxz.orgafacil.funcionalidades.vendas.dados;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CategoriaCatalogoRepository
 * * Gerencia os dados de categorias do catálogo de vendas.
 * Agora utiliza FirebaseSession para garantir a identidade do usuário.
 */
public class CategoriaCatalogoRepository {
    public static final String ID_CATEGORIA_PADRAO = "nao_alocado";
    public static final String EXIBICAO_ALFABETICA = "alfabetica";
    public static final String EXIBICAO_PERSONALIZADA_1 = "personalizada_1";
    public static final String EXIBICAO_PERSONALIZADA_2 = "personalizada_2";
    public static final String EXIBICAO_PERSONALIZADA_3 = "personalizada_3";
    public static final String EXIBICAO_PERSONALIZADA_4 = "personalizada_4";
    public static final String EXIBICAO_PERSONALIZADA_5 = "personalizada_5";

    private static final String CAMPO_EXIBICAO_ATIVA = "catalogoExibicaoCategoriasAtiva";
    private static final String CAMPO_ORDENS_EXIBICAO = "catalogoOrdensCategorias";
    private static final String CAMPO_ORDENS_ITENS_EXIBICAO = "catalogoOrdensItens";
    public static final String NOME_CATEGORIA_PADRAO = "Não alocado";
    public static final String DESCRICAO_CATEGORIA_PADRAO = "Categoria padrão para produtos sem categoria definida";
    private final StorageReference storageRef;

    public CategoriaCatalogoRepository() {
        this.storageRef = FirebaseStorage.getInstance().getReference();
    }

    public interface Callback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<Categoria> lista);
        void onErro(String erro);
    }

    public interface ExibicaoCallback {
        void onNovosDados(String exibicaoAtiva, List<String> ordemCategoriaIds);
        void onErro(String erro);
    }

    public interface OrganizacaoCallback {
        void onNovosDados(String exibicaoAtiva,
                          List<String> ordemCategoriaIds,
                          Map<String, List<String>> ordemItensPorCategoria);
        void onErro(String erro);
    }

    /**
     * Salva Categoria do Catálogo (Vendas).
     */
    public void salvar(@NonNull Categoria categoria, @Nullable Uri imagemUri, @NonNull Callback callback) {
        try {
            if (imagemUri != null) {
                // Garante que tem ID antes do upload para usar como nome do arquivo
                if (categoria.getId() == null || categoria.getId().isEmpty()) {
                    DocumentReference docRef = FirestoreSchema.vendasCategoriasCol().document();
                    categoria.setId(docRef.getId());
                }
                uploadImagem(imagemUri, categoria.getId(), (url, erro) -> {
                    if (erro != null) { callback.onErro("Erro ao subir imagem: " + erro); return; }
                    categoria.setUrlImagem(url);
                    categoria.setIndexIcone(-1);
                    salvarNoFirestore(categoria, callback);
                });
            } else if (categoria.getIndexIcone() != -1) {
                categoria.setUrlImagem(null);
                salvarNoFirestore(categoria, callback);
            } else {
                salvarNoFirestore(categoria, callback);
            }
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    private void salvarNoFirestore(Categoria categoria, Callback callback) {
        boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());
        DocumentReference docRef;

        if (isEdicao) {
            docRef = FirestoreSchema.vendasCategoriaDoc(categoria.getId());
        } else {
            // Usa o helper centralizado para pegar a coleção correta
            docRef = FirestoreSchema.vendasCategoriasCol().document();
            categoria.setId(docRef.getId());
        }

        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Categoria atualizada!" : "Categoria criada!"))
                .addOnFailureListener(e -> callback.onErro("Erro ao salvar: " + e.getMessage()));
    }

    public ListenerRegistration listarTempoReal(@NonNull ListaCallback callback) {
        try {
            return FirestoreSchema.vendasCategoriasCol()
                    .orderBy("nome", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage());
                            return;
                        }
                        List<Categoria> lista = (snapshots != null) ? snapshots.toObjects(Categoria.class) : new ArrayList<>();
                        callback.onNovosDados(lista);
                    });
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
            return null;
        }
    }

    public ListenerRegistration ouvirExibicaoAtiva(@NonNull ExibicaoCallback callback) {
        return ouvirOrganizacaoCatalogo(new OrganizacaoCallback() {
            @Override
            public void onNovosDados(String exibicaoAtiva,
                                     List<String> ordemCategoriaIds,
                                     Map<String, List<String>> ordemItensPorCategoria) {
                callback.onNovosDados(exibicaoAtiva, ordemCategoriaIds);
            }

            @Override
            public void onErro(String erro) {
                callback.onErro(erro);
            }
        });
    }

    public ListenerRegistration ouvirOrganizacaoCatalogo(@NonNull OrganizacaoCallback callback) {
        try {
            return FirestoreSchema.vendasResumoDoc()
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            callback.onErro(error.getMessage());
                            return;
                        }

                        String exibicaoAtiva = EXIBICAO_ALFABETICA;
                        List<String> ordem = new ArrayList<>();
                        if (snapshot != null && snapshot.exists()) {
                            exibicaoAtiva = normalizarExibicao(snapshot.getString(CAMPO_EXIBICAO_ATIVA));
                            ordem = lerOrdem(snapshot, exibicaoAtiva);
                        }

                        Map<String, List<String>> ordemItens = snapshot != null
                                ? lerOrdensItens(snapshot, exibicaoAtiva)
                                : new HashMap<>();
                        callback.onNovosDados(exibicaoAtiva, ordem, ordemItens);
                    });
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
            return null;
        }
    }

    public void salvarExibicaoAtiva(@NonNull String exibicaoId, @NonNull Callback callback) {
        try {
            Map<String, Object> dados = new HashMap<>();
            dados.put(CAMPO_EXIBICAO_ATIVA, normalizarExibicao(exibicaoId));

            FirestoreSchema.vendasResumoDoc()
                    .set(dados, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Exibicao aplicada"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao aplicar exibicao: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    public void atualizarStatus(@NonNull Categoria categoria,
                                boolean ativa,
                                @NonNull Callback callback) {
        atualizarStatus(categoria, categoria.isAtiva(), ativa, callback);
    }

    public void atualizarStatus(@NonNull Categoria categoria,
                                boolean statusAnterior,
                                boolean ativa,
                                @NonNull Callback callback) {
        String id = categoria.getId();
        if (id == null || id.trim().isEmpty()) {
            callback.onErro("Categoria invalida.");
            return;
        }
        if (ID_CATEGORIA_PADRAO.equals(id)) {
            callback.onErro("A categoria padrao nao pode ser inativada.");
            return;
        }

        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("ativa", ativa);
            if (statusAnterior != ativa) {
                updates.put("historicoAlteracoes", FieldValue.arrayUnion(
                        criarHistoricoAlteracao(
                                "Status da categoria",
                                rotuloStatusCategoria(statusAnterior),
                                rotuloStatusCategoria(ativa),
                                "catalogo")));
            }

            FirestoreSchema.vendasCategoriaDoc(id)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> callback.onSucesso(ativa ? "Categoria ativada" : "Categoria inativada"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao alterar status: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    private Map<String, Object> criarHistoricoAlteracao(@NonNull String campo,
                                                        @NonNull String valorAnterior,
                                                        @NonNull String valorNovo,
                                                        @NonNull String origem) {
        Map<String, Object> historico = new HashMap<>();
        historico.put("campo", campo);
        historico.put("valorAnterior", valorAnterior);
        historico.put("valorNovo", valorNovo);
        historico.put("descricao", campo + ": " + valorAnterior + " -> " + valorNovo);
        historico.put("origem", origem);
        historico.put("alteradoEm", FirestoreSchema.nowTs());
        return historico;
    }

    private String rotuloStatusCategoria(boolean ativa) {
        return ativa ? "Ativa" : "Inativa";
    }

    public void salvarOrdemExibicao(@NonNull String exibicaoId,
                                    @NonNull List<Categoria> categorias,
                                    @NonNull Callback callback) {
        String exibicao = normalizarExibicao(exibicaoId);
        if (EXIBICAO_ALFABETICA.equals(exibicao)) {
            callback.onErro("A exibicao alfabetica nao pode ser reordenada.");
            return;
        }

        try {
            List<String> ids = new ArrayList<>();
            for (Categoria categoria : categorias) {
                if (categoria.getId() != null && !categoria.getId().trim().isEmpty()) {
                    ids.add(categoria.getId());
                }
            }

            Map<String, Object> ordens = new HashMap<>();
            ordens.put(exibicao, ids);

            Map<String, Object> dados = new HashMap<>();
            dados.put(CAMPO_ORDENS_EXIBICAO, ordens);

            FirestoreSchema.vendasResumoDoc()
                    .set(dados, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Ordem salva"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao salvar ordem: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    public void salvarOrdemItensExibicao(@NonNull String exibicaoId,
                                         @NonNull String categoriaId,
                                         @NonNull List<String> itemIds,
                                         @NonNull Callback callback) {
        String exibicao = normalizarExibicao(exibicaoId);
        if (EXIBICAO_ALFABETICA.equals(exibicao)) {
            callback.onErro("A exibicao alfabetica nao pode ser reordenada.");
            return;
        }

        try {
            Map<String, Object> itensPorCategoria = new HashMap<>();
            itensPorCategoria.put(categoriaId, new ArrayList<>(itemIds));

            Map<String, Object> ordensItens = new HashMap<>();
            ordensItens.put(exibicao, itensPorCategoria);

            Map<String, Object> dados = new HashMap<>();
            dados.put(CAMPO_ORDENS_ITENS_EXIBICAO, ordensItens);

            FirestoreSchema.vendasResumoDoc()
                    .set(dados, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Ordem salva"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao salvar ordem: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    public static boolean isExibicaoPersonalizada(@Nullable String exibicaoId) {
        return !EXIBICAO_ALFABETICA.equals(normalizarExibicao(exibicaoId));
    }

    @NonNull
    public static String normalizarExibicao(@Nullable String exibicaoId) {
        if (EXIBICAO_PERSONALIZADA_1.equals(exibicaoId)
                || EXIBICAO_PERSONALIZADA_2.equals(exibicaoId)
                || EXIBICAO_PERSONALIZADA_3.equals(exibicaoId)
                || EXIBICAO_PERSONALIZADA_4.equals(exibicaoId)
                || EXIBICAO_PERSONALIZADA_5.equals(exibicaoId)) {
            return exibicaoId;
        }
        return EXIBICAO_ALFABETICA;
    }

    @NonNull
    public static List<Categoria> ordenarParaExibicao(@NonNull List<Categoria> categorias,
                                                      @Nullable String exibicaoId,
                                                      @Nullable List<String> ordemCategoriaIds) {
        List<Categoria> ordenadas = new ArrayList<>(categorias);
        Collections.sort(ordenadas, (a, b) -> compararNome(a.getNome(), b.getNome()));

        if (!isExibicaoPersonalizada(exibicaoId) || ordemCategoriaIds == null || ordemCategoriaIds.isEmpty()) {
            return ordenadas;
        }

        Map<String, Integer> posicoes = new HashMap<>();
        for (int i = 0; i < ordemCategoriaIds.size(); i++) {
            posicoes.put(ordemCategoriaIds.get(i), i);
        }

        Collections.sort(ordenadas, (a, b) -> {
            Integer posA = posicoes.get(a.getId());
            Integer posB = posicoes.get(b.getId());
            if (posA != null && posB != null) return posA.compareTo(posB);
            if (posA != null) return -1;
            if (posB != null) return 1;
            return compararNome(a.getNome(), b.getNome());
        });
        return ordenadas;
    }

    public void excluir(@NonNull String idCategoria, @NonNull Callback callback) {
        if (ID_CATEGORIA_PADRAO.equals(idCategoria)) {
            callback.onErro("A categoria padrão não pode ser excluída.");
            return;
        }

        try {
            FirestoreSchema.vendasCategoriaDoc(idCategoria)
                    .delete()
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Categoria excluída com sucesso!"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao excluir: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    // --- Helpers Privados ---

    private void uploadImagem(Uri uri, String categoriaId, BiConsumer<String, String> callback) {
        String nomeArquivo = categoriaId + ".jpg";
        StorageReference fotoRef = storageRef
                .child("images")
                .child("users")
                .child(FirebaseSession.getUserId())
                .child("vendas")
                .child("categorias")
                .child(nomeArquivo);

        // Deleta a anterior (se existir) antes de subir a nova
        fotoRef.delete().addOnCompleteListener(deleteTask -> {
            fotoRef.putFile(uri)
                    .addOnSuccessListener(task -> fotoRef.getDownloadUrl()
                            .addOnSuccessListener(url -> callback.accept(url.toString(), null))
                            .addOnFailureListener(e -> callback.accept(null, e.getMessage())))
                    .addOnFailureListener(e -> callback.accept(null, e.getMessage()));
        });
    }

    private interface BiConsumer<T, U> { void accept(T t, U u); }

    public void garantirCategoriaPadrao(@NonNull Callback callback) {
        try {
            Categoria categoriaPadrao = new Categoria();
            categoriaPadrao.setId(ID_CATEGORIA_PADRAO);
            categoriaPadrao.setNome(NOME_CATEGORIA_PADRAO);
            categoriaPadrao.setDescricao(DESCRICAO_CATEGORIA_PADRAO);
            categoriaPadrao.setIndexIcone(-1);
            categoriaPadrao.setUrlImagem(null);
            categoriaPadrao.setAtiva(true);
            categoriaPadrao.setTipo(Categoria.Tipo.PRODUTO.toString());

            FirestoreSchema.vendasCategoriaDoc(ID_CATEGORIA_PADRAO)
                    .set(categoriaPadrao, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onSucesso("Categoria padrão garantida"))
                    .addOnFailureListener(e -> callback.onErro("Erro ao garantir categoria padrão: " + e.getMessage()));
        } catch (IllegalStateException e) {
            callback.onErro(e.getMessage());
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private List<String> lerOrdem(@NonNull DocumentSnapshot snapshot, @NonNull String exibicaoAtiva) {
        Object ordensRaw = snapshot.get(CAMPO_ORDENS_EXIBICAO);
        if (!(ordensRaw instanceof Map)) return new ArrayList<>();

        Object ordemRaw = ((Map<String, Object>) ordensRaw).get(exibicaoAtiva);
        if (!(ordemRaw instanceof List)) return new ArrayList<>();

        List<String> ordem = new ArrayList<>();
        for (Object id : (List<?>) ordemRaw) {
            if (id instanceof String) ordem.add((String) id);
        }
        return ordem;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> lerOrdensItens(@NonNull DocumentSnapshot snapshot,
                                                     @NonNull String exibicaoAtiva) {
        Map<String, List<String>> resultado = new HashMap<>();
        Object ordensRaw = snapshot.get(CAMPO_ORDENS_ITENS_EXIBICAO);
        if (!(ordensRaw instanceof Map)) return resultado;

        Object exibicaoRaw = ((Map<String, Object>) ordensRaw).get(exibicaoAtiva);
        if (!(exibicaoRaw instanceof Map)) return resultado;

        Map<String, Object> categoriasRaw = (Map<String, Object>) exibicaoRaw;
        for (Map.Entry<String, Object> entry : categoriasRaw.entrySet()) {
            if (!(entry.getValue() instanceof List)) continue;
            List<String> ids = new ArrayList<>();
            for (Object id : (List<?>) entry.getValue()) {
                if (id instanceof String) ids.add((String) id);
            }
            resultado.put(entry.getKey(), ids);
        }

        return resultado;
    }

    private static int compararNome(@Nullable String a, @Nullable String b) {
        String nomeA = a != null ? a : "";
        String nomeB = b != null ? b : "";
        return nomeA.compareToIgnoreCase(nomeB);
    }
}
