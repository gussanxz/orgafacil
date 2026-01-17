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
import com.gussanxz.orgafacil.adapter.AdapterCategoriaContas;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.gussanxz.orgafacil.model.CategoriaIdHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterCategoriaContas adapter;
    private final List<String> categorias = new ArrayList<>();

    private FirebaseFirestore fs;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_contas_selecionar_categoria);

        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new AdapterCategoriaContas(categorias, this);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fs = ConfiguracaoFirebase.getFirestore();

        carregarCategoriasDoFirestore();

        findViewById(R.id.btnNovaCategoria).setOnClickListener(v -> mostrarDialogNovaCategoria());
        configurarSwipeParaExcluir();
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

    // users/{uid}/contas/main/categorias/{catId}
    private DocumentReference categoriaDocRef(String catId) {
        return fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("categorias").document(catId);
    }

    private void salvarCategoriaNoFirestore(String nomeCategoria) {
        String catId = CategoriaIdHelper.slugify(nomeCategoria);

        Map<String, Object> doc = new HashMap<>();
        doc.put("nome", nomeCategoria);
        doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

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
        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("categorias")
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
                        // fallback (caso seed no cadastro falhe)
                        inicializarCategoriasPadraoNoFirestore();
                    } else {
                        categorias.sort(String::compareToIgnoreCase);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar categorias", Toast.LENGTH_SHORT).show();
                });
    }

    private void inicializarCategoriasPadraoNoFirestore() {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        // cria todas e depois recarrega
        final int total = padroes.size();
        final int[] ok = {0};

        for (String nome : padroes) {
            String catId = CategoriaIdHelper.slugify(nome);

            Map<String, Object> doc = new HashMap<>();
            doc.put("nome", nome);
            doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            categoriaDocRef(catId).set(doc, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        ok[0]++;
                        if (ok[0] == total) {
                            carregarCategoriasDoFirestore();
                        }
                    })
                    .addOnFailureListener(e -> {
                        ok[0]++;
                        if (ok[0] == total) {
                            carregarCategoriasDoFirestore();
                        }
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
                excluirCategoriaFirestore(CategoriaIdHelper.slugify(categoria), pos);
//                verificarEExcluirCategoriaFirestore(categoria, pos);
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
     * Implementação simples: varre meses e consulta "itens" de cada mês.
     * (Depois podemos otimizar com índice/coleção única por mês ou campo agregado.)
     */
    private void verificarEExcluirCategoriaFirestore(String categoria, int posicaoNaLista) {
        String cat = categoria;
        String catId = CategoriaIdHelper.slugify(cat);

        fs.collection("users").document(uid)
                .collection("contas").document("main")
                .collection("movimentacoes")
                .get()
                .addOnSuccessListener(mesesSnap -> {
                    if (mesesSnap.isEmpty()) {
                        excluirCategoriaFirestore(catId, posicaoNaLista);
                        return;
                    }

                    final boolean[] emUso = {false};
                    final int totalMeses = mesesSnap.size();
                    final int[] verificados = {0};

                    for (QueryDocumentSnapshot mesDoc : mesesSnap) {
                        mesDoc.getReference()
                                .collection("itens")
                                .whereEqualTo("categoria", cat)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(itensSnap -> {
                                    if (!itensSnap.isEmpty()) emUso[0] = true;

                                    verificados[0]++;
                                    if (verificados[0] == totalMeses) {
                                        if (emUso[0]) {
                                            Toast.makeText(this,
                                                    "Categoria em uso — não pode ser excluída.", Toast.LENGTH_SHORT).show();
                                            adapter.notifyItemChanged(posicaoNaLista);
                                        } else {
                                            excluirCategoriaFirestore(catId, posicaoNaLista);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    verificados[0]++;
                                    if (verificados[0] == totalMeses) {
                                        // se falhou a verificação, por segurança não exclui
                                        Toast.makeText(this,
                                                "Não foi possível verificar uso da categoria.", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(posicaoNaLista);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao verificar movimentações", Toast.LENGTH_SHORT).show();
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
