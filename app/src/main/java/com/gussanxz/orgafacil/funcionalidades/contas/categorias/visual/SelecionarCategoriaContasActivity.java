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

        // [CORREÇÃO] Recupera o tipo enviado pela Activity anterior (Receita ou Despesa)
        // Se não vier nada, assume DESPESA por segurança.
        int tipoId = getIntent().getIntExtra("TIPO_CATEGORIA", TipoCategoriaContas.DESPESA.getId());
        TipoCategoriaContas tipoEnum = (tipoId == TipoCategoriaContas.RECEITA.getId())
                ? TipoCategoriaContas.RECEITA
                : TipoCategoriaContas.DESPESA;

        // Filtra no banco apenas as categorias do tipo solicitado
        repository.listarAtivasPorTipo(tipoEnum)
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaCategorias.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                            listaCategorias.add(cat);
                        }
                    }

                    // Se estiver vazio para esse tipo específico, inicializa os padrões
                    if (listaCategorias.isEmpty()) {
                        repository.inicializarPadroes(new ContasCategoriaRepository.Callback() {
                            @Override
                            public void onSucesso() {
                                carregarCategorias(); // Recarrega para mostrar os novos padrões
                            }

                            @Override
                            public void onErro(String erro) {
                                Toast.makeText(SelecionarCategoriaContasActivity.this,
                                        "Erro ao criar padrões: " + erro, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Erro ao carregar banco: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

                // Define o nome dentro do grupo Visual (Mapa aninhado)
                novaCat.getVisual().setNome(nome);
                novaCat.getVisual().setIcone("ic_default");

                // Define os dados da raiz usando o ID do Enum
                novaCat.setTipo(TipoCategoriaContas.DESPESA.getId());
                novaCat.setAtiva(true);

                repository.salvar(novaCat, new ContasCategoriaRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        Toast.makeText(SelecionarCategoriaContasActivity.this, "Categoria criada!", Toast.LENGTH_SHORT).show();
                        carregarCategorias(); // Atualiza a lista
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
     * Implementa o "Deslizar para excluir" com trava de segurança (verificação de uso).
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
                        // Se houver erro (categoria em uso nas movimentações), desfaz o movimento visual do swipe
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                });
            }
        };
        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }
}