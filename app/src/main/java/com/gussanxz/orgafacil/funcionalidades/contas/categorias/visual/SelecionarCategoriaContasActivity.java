package com.gussanxz.orgafacil.funcionalidades.contas.categorias.visual;

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
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.repository.ContasCategoriaRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.modelos.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * SelecionarCategoriaContasActivity
 * O que esta classe faz:
 * 1. Gerenciamento de Catálogo: Lista as categorias disponíveis.
 * 2. Integração Profissional: Usa o Repository e o Model aninhado (Visual/Financeiro).
 */
public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterExibirCategoriasContas adapter;

    private final List<ContasCategoriaModel> listaCategorias = new ArrayList<>();
    private ContasCategoriaRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_contas_selecao_categoria);

        // Segurança: Impede acesso de usuários deslogados
        if (!FirebaseSession.isUserLogged()) {
            finish();
            return;
        }

        repository = new ContasCategoriaRepository();
        configurarRecycler();
        carregarCategorias();

        findViewById(R.id.btnNovaCategoria).setOnClickListener(v -> mostrarDialogNovaCategoria());
        configurarSwipeParaExcluir();
    }

    private void configurarRecycler() {
        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // O Adapter já foi atualizado para ler o grupo Visual
        adapter = new AdapterExibirCategoriasContas(listaCategorias, this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Busca as categorias no Firestore e inicializa padrões caso o banco esteja vazio.
     */
    private void carregarCategorias() {
        // O Repository ordena por "visual.nome" automaticamente
        repository.listarAtivasPorTipo(TipoCategoriaContas.DESPESA.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaCategorias.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                        listaCategorias.add(cat);
                    }

                    // Se estiver vazio, cria os padrões
                    if (listaCategorias.isEmpty()) {
                        repository.inicializarPadroes(new ContasCategoriaRepository.Callback() {
                            @Override
                            public void onSucesso() {
                                carregarCategorias(); // Recarrega
                            }

                            @Override
                            public void onErro(String erro) {
                                Toast.makeText(SelecionarCategoriaContasActivity.this, "Erro ao criar padrões: " + erro, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar banco", Toast.LENGTH_SHORT).show());
    }

    /**
     * Cria uma nova categoria personalizada.
     */
    private void mostrarDialogNovaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Categoria");
        final EditText input = new EditText(this);
        input.setHint("Nome da categoria");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String nome = input.getText().toString().trim();
            if (!TextUtils.isEmpty(nome)) {

                ContasCategoriaModel novaCat = new ContasCategoriaModel();

                // [ATUALIZADO] Define o nome dentro do grupo Visual
                novaCat.getVisual().setNome(nome);

                // Define os dados da raiz
                novaCat.setTipo(TipoCategoriaContas.DESPESA.getId());
                novaCat.setAtiva(true);

                repository.salvar(novaCat, new ContasCategoriaRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, "Categoria criada!", Toast.LENGTH_SHORT).show();
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

    /**
     * Implementa o "Deslizar para excluir" com trava de segurança.
     */
    private void configurarSwipeParaExcluir() {
        ItemTouchHelper.SimpleCallback itemTouch = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                ContasCategoriaModel categoriaParaExcluir = listaCategorias.get(pos);

                repository.verificarEExcluir(categoriaParaExcluir, new ContasCategoriaRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        listaCategorias.remove(pos);
                        adapter.notifyItemRemoved(pos);
                        Toast.makeText(SelecionarCategoriaContasActivity.this, "Excluída!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String erro) {
                        // Se houver erro (categoria em uso), desfaz o swipe
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                });
            }
        };
        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }
}