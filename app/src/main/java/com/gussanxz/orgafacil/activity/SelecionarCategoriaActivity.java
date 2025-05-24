package com.gussanxz.orgafacil.activity;

import android.app.Activity;
import android.content.Intent;
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
import com.gussanxz.orgafacil.adapter.AdapterCategoria;
import com.gussanxz.orgafacil.helper.Base64Custom;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelecionarCategoriaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterCategoria adapter;
    private final List<String> categorias = new ArrayList<>();
    private DatabaseReference firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecionar_categoria);

        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new AdapterCategoria(categorias, this);
        recyclerView.setAdapter(adapter);

        firebaseRef = FirebaseDatabase.getInstance().getReference();
        carregarCategoriasDoFirebase();

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
                salvarCategoriaNoFirebase(novaCategoria);
            } else {
                Toast.makeText(this, "Digite uma categoria válida", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void salvarCategoriaNoFirebase(String nomeCategoria) {
        String emailUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);

        DatabaseReference categoriasRef = firebaseRef.child("usuarios").child(idUsuario).child("categorias");
        String novaChave = categoriasRef.push().getKey();
        categoriasRef.child(novaChave).setValue(nomeCategoria).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Categoria adicionada!", Toast.LENGTH_SHORT).show();
                categorias.add(nomeCategoria);
                adapter.notifyItemInserted(categorias.size() - 1);
            } else {
                Toast.makeText(this, "Erro ao salvar categoria", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inicializarCategoriasPadrao() {
        List<String> padroes = Arrays.asList(
                "Alimentação", "Aluguel", "Pets", "Contas", "Doações e caridades",
                "Educação", "Investimento", "Lazer", "Mercado", "Moradia"
        );

        for (String cat : padroes) {
            salvarCategoriaNoFirebase(cat);
        }
    }

    private void carregarCategoriasDoFirebase() {
        String emailUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);

        DatabaseReference categoriasRef = firebaseRef.child("usuarios").child(idUsuario).child("categorias");

        categoriasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categorias.clear();
                for (DataSnapshot catSnap : snapshot.getChildren()) {
                    String categoria = catSnap.getValue(String.class);
                    if (categoria != null && !categorias.contains(categoria)) {
                        categorias.add(categoria);
                    }
                }

                if (categorias.isEmpty()) {
                    inicializarCategoriasPadrao();
                } else {
                    categorias.sort(String::compareToIgnoreCase);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

                verificarEExcluirCategoria(categoria, pos);
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

    private void verificarEExcluirCategoria(String categoria, int posicaoNaLista) {
        String emailUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        DatabaseReference movimentacoesRef = firebaseRef.child("movimentacao").child(idUsuario);

        movimentacoesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean categoriaEmUso = false;

                for (DataSnapshot mesAnoSnap : snapshot.getChildren()) {
                    for (DataSnapshot movSnap : mesAnoSnap.getChildren()) {
                        String catMov = movSnap.child("categoria").getValue(String.class);
                        if (categoria.equalsIgnoreCase(catMov)) {
                            categoriaEmUso = true;
                            break;
                        }
                    }
                    if (categoriaEmUso) break;
                }

                if (categoriaEmUso) {
                    Toast.makeText(SelecionarCategoriaActivity.this,
                            "Categoria em uso — não pode ser excluída.", Toast.LENGTH_SHORT).show();
                    // Restaurar visual na lista (recoloca o item visualmente)
                    adapter.notifyItemChanged(posicaoNaLista);
                } else {
                    excluirDoFirebaseELista(categoria, posicaoNaLista);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void excluirDoFirebaseELista(String categoria, int posicaoNaLista) {
        String emailUsuario = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        DatabaseReference categoriasRef = firebaseRef.child("usuarios").child(idUsuario).child("categorias");

        categoriasRef.orderByValue().equalTo(categoria)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot catSnap : snapshot.getChildren()) {
                            catSnap.getRef().removeValue();
                        }

                        // Agora sim: remover da lista e atualizar tela
                        categorias.remove(posicaoNaLista);
                        adapter.notifyItemRemoved(posicaoNaLista);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

}
