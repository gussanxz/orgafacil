package com.gussanxz.orgafacil.funcionalidades.contas;

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
import com.gussanxz.orgafacil.funcionalidades.contas.dados.CategoriaContasRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.visual.AdapterExibirCategoriasContas;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterExibirCategoriasContas adapter;
    private final List<String> categorias = new ArrayList<>();
    private CategoriaContasRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_contas_selecao_categoria);

        // 1. Verificação de Sessão (Pensa fora da caixa: segurança primeiro)
        if (!FirebaseSession.isUserLogged()) {
            finish();
            return;
        }

        repository = new CategoriaContasRepository();
        configurarRecycler();

        carregarCategorias();

        findViewById(R.id.btnNovaCategoria).setOnClickListener(v -> mostrarDialogNovaCategoria());
        configurarSwipeParaExcluir();
    }

    private void configurarRecycler() {
        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new AdapterExibirCategoriasContas(categorias, this);
        recyclerView.setAdapter(adapter);
    }

    private void carregarCategorias() {
        repository.listarAtivas().addOnSuccessListener(snapshot -> {
            categorias.clear();
            for (QueryDocumentSnapshot doc : snapshot) {
                String nome = doc.getString("nome");
                if (nome != null) categorias.add(nome);
            }

            // Substitua o trecho com erro por este:
            if (categorias.isEmpty()) {
                repository.inicializarPadroes(new CategoriaContasRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        carregarCategorias(); // Agora sim chama o método após inicializar
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogNovaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Categoria");
        final EditText input = new EditText(this);
        input.setHint("Digite o nome");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String nome = input.getText().toString().trim();
            if (!TextUtils.isEmpty(nome)) {
                repository.salvar(nome, new CategoriaContasRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, "Salvo!", Toast.LENGTH_SHORT).show();
                        carregarCategorias();
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void configurarSwipeParaExcluir() {
        ItemTouchHelper.SimpleCallback itemTouch = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                String categoriaNome = categorias.get(pos);

                repository.verificarEExcluir(categoriaNome, new CategoriaContasRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        categorias.remove(pos);
                        adapter.notifyItemRemoved(pos);
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int state, boolean active) {
                // ... (mantenha sua implementação de desenho do Canvas aqui, ela está correta)
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }
        };
        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }
}