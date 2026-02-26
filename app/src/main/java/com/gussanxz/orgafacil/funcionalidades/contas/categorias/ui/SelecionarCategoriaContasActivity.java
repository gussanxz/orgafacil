package com.gussanxz.orgafacil.funcionalidades.contas.categorias.ui;

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
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.repository.ContasCategoriaRepository;
import com.gussanxz.orgafacil.funcionalidades.contas.categorias.dados.model.ContasCategoriaModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirebaseSession;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;

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

    // [FIX 1] tipoAtual como campo da classe para o dialog acessar
    private TipoCategoriaContas tipoAtual = TipoCategoriaContas.DESPESA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_selecao_categoria);

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
        adapter = new AdapterExibirCategoriasContas(listaCategorias, this);
        recyclerView.setAdapter(adapter);
    }

    private void carregarCategorias() {
        int tipoId = getIntent().getIntExtra("TIPO_CATEGORIA", TipoCategoriaContas.DESPESA.getId());

        // [FIX 2] Atualiza o campo da classe para o dialog usar
        tipoAtual = (tipoId == TipoCategoriaContas.RECEITA.getId())
                ? TipoCategoriaContas.RECEITA
                : TipoCategoriaContas.DESPESA;

        repository.listarAtivasPorTipo(tipoAtual)
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaCategorias.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                            cat.setId(doc.getId()); // [FIX 3] garantir ID setado
                            listaCategorias.add(cat);
                        }
                    }

                    // [FIX 4] Ordenação no cliente por nome (substitui o orderBy removido)
                    listaCategorias.sort((a, b) -> {
                        String nomeA = (a.getVisual() != null && a.getVisual().getNome() != null)
                                ? a.getVisual().getNome() : "";
                        String nomeB = (b.getVisual() != null && b.getVisual().getNome() != null)
                                ? b.getVisual().getNome() : "";
                        return nomeA.compareToIgnoreCase(nomeB);
                    });

                    if (listaCategorias.isEmpty()) {
                        // Verifica se já existe QUALQUER categoria antes de criar padrões
                        // para não duplicar se o usuário tiver só um tipo cadastrado
                        FirestoreSchema.contasCategoriasCol()
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snapGlobal -> {
                                    if (snapGlobal.isEmpty()) {
                                        // Banco vazio de verdade — cria padrões
                                        repository.inicializarPadroes(new ContasCategoriaRepository.Callback() {
                                            @Override
                                            public void onSucesso() { carregarCategorias(); }
                                            @Override
                                            public void onErro(String erro) {
                                                Toast.makeText(SelecionarCategoriaContasActivity.this,
                                                        "Erro ao criar padrões: " + erro, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    // Se existe algo mas não do tipo atual, só mostra lista vazia
                                    // sem duplicar os padrões
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> adapter.notifyDataSetChanged());
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Erro ao carregar categorias: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

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
                novaCat.getVisual().setNome(nome);
                novaCat.getVisual().setIcone("ic_default");
                novaCat.setTipo(tipoAtual.getId()); // [FIX 5] usa o tipo correto da tela
                novaCat.setAtiva(true);

                repository.salvar(novaCat, new ContasCategoriaRepository.Callback() {
                    @Override
                    public void onSucesso() {
                        Toast.makeText(SelecionarCategoriaContasActivity.this,
                                "Categoria criada!", Toast.LENGTH_SHORT).show();
                        carregarCategorias(); // Atualiza a lista automaticamente
                    }
                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(SelecionarCategoriaContasActivity.this,
                                erro, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(SelecionarCategoriaContasActivity.this, erro, Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                });
            }
        };
        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }
}