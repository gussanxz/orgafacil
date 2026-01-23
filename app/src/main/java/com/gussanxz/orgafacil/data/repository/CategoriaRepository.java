package com.gussanxz.orgafacil.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.gussanxz.orgafacil.data.model.Categoria;

import java.util.ArrayList;
import java.util.List;

public class CategoriaRepository {

    private final FirebaseFirestore db;
    private final String uid;

    // Constantes para não errar o nome das coleções
    private static final String COLECAO_RAIZ = "users";
    private static final String COLECAO_MODULO = "vendas_dados"; // Mudamos aqui!
    private static final String COLECAO_CATEGORIAS = "categorias";

    public CategoriaRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    public interface CategoriaCallback {
        void onSucesso(String mensagem);
        void onErro(String erro);
    }

    public interface ListaCallback {
        void onNovosDados(List<Categoria> lista);
        void onErro(String erro);
    }

    // --- SALVAR / EDITAR ---
    public void salvar(Categoria categoria, CategoriaCallback callback) {
        if (uid == null) { callback.onErro("Usuário não logado"); return; }

        DocumentReference docRef;
        boolean isEdicao = (categoria.getId() != null && !categoria.getId().isEmpty());

        // CAMINHO NOVO: users/{uid}/vendas_dados/categorias/{id}
        if (isEdicao) {
            docRef = db.collection(COLECAO_RAIZ).document(uid)
                    .collection(COLECAO_MODULO).document(COLECAO_CATEGORIAS)
                    .collection("itens").document(categoria.getId());
            // Nota: Firestore exige padrão col/doc/col/doc.
            // Então fazemos: vendas_dados (col) -> categorias (doc) -> itens (col) -> ID (doc)
            // OU simplificamos para: users/{uid}/categorias_vendas/{id}

            // OPÇÃO MAIS LIMPA E RECOMENDADA PARA SEU CASO:
            docRef = db.collection("users").document(uid)
                    .collection("vendas_categorias").document(categoria.getId());
        } else {
            docRef = db.collection("users").document(uid)
                    .collection("vendas_categorias").document();
            categoria.setId(docRef.getId());
        }

        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> callback.onSucesso(isEdicao ? "Atualizado!" : "Criado!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }

    // --- LISTAR EM TEMPO REAL ---
    public ListenerRegistration listarTempoReal(ListaCallback callback) {
        if (uid == null) return null;

        // CAMINHO NOVO DE LEITURA
        return db.collection("users").document(uid)
                .collection("vendas_categorias") // Mesmo nome usado no salvar
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onErro(error.getMessage());
                        return;
                    }
                    List<Categoria> lista = new ArrayList<>();
                    if (snapshots != null) {
                        lista = snapshots.toObjects(Categoria.class);
                        // Garante IDs caso o toObjects falhe nisso
                        int i = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            if(i < lista.size()) lista.get(i).setId(doc.getId());
                            i++;
                        }
                    }
                    callback.onNovosDados(lista);
                });
    }

    // --- EXCLUIR ---
    public void excluir(String idCategoria, CategoriaCallback callback) {
        if (uid == null) return;

        db.collection("users").document(uid)
                .collection("vendas_categorias").document(idCategoria)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSucesso("Excluído com sucesso!"))
                .addOnFailureListener(e -> callback.onErro(e.getMessage()));
    }
}