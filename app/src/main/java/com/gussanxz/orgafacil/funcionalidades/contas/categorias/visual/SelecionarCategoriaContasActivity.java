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
 * 1. Gerenciamento de Catálogo: Lista as categorias disponíveis para vincular a uma movimentação.
 * 2. Evolução de Dados: Migrou de List<String> para List<ContasCategoriaModel> para suportar limites e ícones.
 * 3. Integração Profissional: Usa o novo ContasCategoriaRepository para salvar e validar exclusões.
 */
public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterExibirCategoriasContas adapter;

    // Agora guardamos a lista de modelos, não apenas Strings
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

        // O Adapter deve ser atualizado para receber List<ContasCategoriaModel>
        adapter = new AdapterExibirCategoriasContas(listaCategorias, this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Busca as categorias no Firestore e inicializa padrões caso o banco esteja vazio.
     */
    private void carregarCategorias() {
        // Por padrão, listamos categorias de Despesa para seleção
        repository.listarAtivasPorTipo(TipoCategoriaContas.DESPESA.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaCategorias.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                        listaCategorias.add(cat);
                    }

                    // Pensa fora da caixa: se o usuário é novo, entrega o app pronto com padrões
                    if (listaCategorias.isEmpty()) {
                        repository.inicializarPadroes(new ContasCategoriaRepository.Callback() {
                            @Override
                            public void onSucesso() {
                                carregarCategorias(); // Recarrega após criar as padrões
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

                // Monta o objeto Model conforme a nova regra
                ContasCategoriaModel novaCat = new ContasCategoriaModel();
                novaCat.setNome(nome);
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

                // Regra: Só exclui se não houver movimentações vinculadas a este ID
                repository.verificarEExcluir(categoriaParaExcluir, new ContasCategoriaRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        listaCategorias.remove(pos);
                        adapter.notifyItemRemoved(pos);
                        Toast.makeText(SelecionarCategoriaContasActivity.this, "Excluída!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String erro) {
                        // Se houver erro (categoria em uso), desfaz o swipe na tela
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                });
            }
        };
        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }
}