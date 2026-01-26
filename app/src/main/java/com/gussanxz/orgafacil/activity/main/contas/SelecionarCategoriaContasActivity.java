package com.gussanxz.orgafacil.activity.main.contas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.ui.contas.categorias.AdapterExibirCategoriasContas;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;

import com.gussanxz.orgafacil.data.config.FirestoreSchema;
import com.gussanxz.orgafacil.helper.CategoriaIdHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SelecionarCategoriaContasActivity (schema novo)
 *
 * Categorias agora em:
 *  {ROOT}/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
 *
 * Verificação de uso:
 *  consulta contasMovimentacoes onde categoriaNome == {nome}
 *
 * Impacto:
 * - remove "contas/main/categorias" antigo
 * - caminho único, fácil de indexar e manter
 */
public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterExibirCategoriasContas adapter;
    private final List<String> categorias = new ArrayList<>();

    private FirebaseFirestore fs;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_contas_selecao_categoria);

        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new AdapterExibirCategoriasContas(categorias, this);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fs = FirestoreSchema.db();

        carregarCategoriasDoFirestore();

        findViewById(R.id.btnNovaCategoria).setOnClickListener(v -> mostrarDialogNovaCategoria());
        configurarSwipeParaExcluir();
    }

    private DocumentReference categoriaDocRef(String catId) {
        // {ROOT}/{uid}/moduloSistema/contas/contasCategorias/{categoriaId}
        return FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_CATEGORIAS).document(catId);
    }

    private void mostrarDialogNovaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Categoria");

        final EditText input = new EditText(this);
        input.setHint("Digite o nome da categoria");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novaCategoria = input.getText().toString().trim();
            if (!TextUtils.isEmpty(novaCategoria)) {
                salvarCategoriaNoFirestore(novaCategoria);
            } else {
                Toast.makeText(this, "Digite uma categoria válida", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void salvarCategoriaNoFirestore(String nomeCategoria) {
        String catId = CategoriaIdHelper.slugify(nomeCategoria);

        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nomeCategoria);
        doc.put("tipo", "Despesa"); // padrão (ajuste se quiser separar Receita/Despesa por tela)
        doc.put("ativo", true);
        doc.put("ordem", 0);
        doc.put("createdAt", FieldValue.serverTimestamp());

        categoriaDocRef(catId)
                .set(doc, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Categoria adicionada!", Toast.LENGTH_SHORT).show();

                    if (!categorias.contains(nomeCategoria)) {
                        categorias.add(nomeCategoria);
                        categorias.sort(String::compareToIgnoreCase);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao salvar categoria", Toast.LENGTH_SHORT).show()
                );
    }

    private void carregarCategoriasDoFirestore() {
        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_CATEGORIAS)
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    categorias.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String nome = doc.getString("nome");
                        if (!TextUtils.isEmpty(nome) && !categorias.contains(nome)) {
                            categorias.add(nome);
                        }
                    }

                    if (categorias.isEmpty()) {
                        inicializarCategoriasPadraoNoFirestore();
                    } else {
                        categorias.sort(String::compareToIgnoreCase);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar categorias", Toast.LENGTH_SHORT).show()
                );
    }

    private void inicializarCategoriasPadraoNoFirestore() {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        final int total = padroes.size();
        final int[] ok = {0};

        for (String nome : padroes) {
            String catId = CategoriaIdHelper.slugify(nome);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("tipo", "Despesa");
            doc.put("ativo", true);
            doc.put("ordem", 0);
            doc.put("createdAt", FieldValue.serverTimestamp());

            categoriaDocRef(catId).set(doc, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        ok[0]++;
                        if (ok[0] == total) carregarCategoriasDoFirestore();
                    })
                    .addOnFailureListener(e -> {
                        ok[0]++;
                        if (ok[0] == total) carregarCategoriasDoFirestore();
                    });
        }
    }

    private void configurarSwipeParaExcluir() {
        ItemTouchHelper.SimpleCallback itemTouch = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                String categoria = categorias.get(pos);

                // Versão segura: verifica se está em uso antes de excluir
                verificarEExcluirCategoriaFirestore(categoria, pos);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                Paint paint = new Paint();
                paint.setColor(Color.RED);

                c.drawRect(viewHolder.itemView.getRight() + dX, viewHolder.itemView.getTop(),
                        viewHolder.itemView.getRight(), viewHolder.itemView.getBottom(), paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(40);
                c.drawText("Excluir", viewHolder.itemView.getRight() - 150,
                        viewHolder.itemView.getTop() + viewHolder.itemView.getHeight() / 2f + 15, paint);
            }
        };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }

    /**
     * Bloqueia exclusão se existir qualquer movimentação usando a categoria.
     *
     * Agora (schema novo):
     * - Consulta direta em contasMovimentacoes onde categoriaNome == categoria
     *
     * Impacto:
     * - Muito mais barato que varrer meses/subcoleções
     */
    private void verificarEExcluirCategoriaFirestore(String categoriaNome, int posicaoNaLista) {
        FirestoreSchema.userDoc(uid)
                .collection(FirestoreSchema.MODULO).document(FirestoreSchema.CONTAS)
                .collection(FirestoreSchema.CONTAS_MOV)
                .whereEqualTo("categoriaNome", categoriaNome)
                .limit(1)
                .get()
                .addOnSuccessListener(itensSnap -> {
                    if (itensSnap != null && !itensSnap.isEmpty()) {
                        Toast.makeText(this,
                                "Categoria em uso — não pode ser excluída.", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(posicaoNaLista);
                    } else {
                        excluirCategoriaFirestore(CategoriaIdHelper.slugify(categoriaNome), posicaoNaLista);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Não foi possível verificar uso da categoria.", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(posicaoNaLista);
                });
    }

    private void excluirCategoriaFirestore(String catId, int posicaoNaLista) {
        categoriaDocRef(catId)
                .delete()
                .addOnSuccessListener(unused -> {
                    categorias.remove(posicaoNaLista);
                    adapter.notifyItemRemoved(posicaoNaLista);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao excluir categoria", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(posicaoNaLista);
                });
    }
}
