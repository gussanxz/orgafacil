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
import com.gussanxz.orgafacil.util_helper.CategoriaIdHelper;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;

import java.util.ArrayList;
import java.util.List;

public class SelecionarCategoriaContasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterExibirCategoriasContas adapter;

    private final List<ContasCategoriaModel> listaCategorias = new ArrayList<>();
    private ContasCategoriaRepository repository;

    // Tipo atual filtrado na tela — definido pelo extra recebido
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

        // Resolve o tipo ANTES de qualquer coisa — tanto criar quanto listar dependem disso
        int tipoId = getIntent().getIntExtra("TIPO_CATEGORIA", TipoCategoriaContas.DESPESA.getId());
        tipoAtual = (tipoId == TipoCategoriaContas.RECEITA.getId())
                ? TipoCategoriaContas.RECEITA
                : TipoCategoriaContas.DESPESA;

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
        repository.listarAtivasPorTipo(tipoAtual)
                .get()
                .addOnSuccessListener(snapshot -> {
                    listaCategorias.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ContasCategoriaModel cat = doc.toObject(ContasCategoriaModel.class);
                            cat.setId(doc.getId());
                            listaCategorias.add(cat);
                        }
                    }

                    // Ordenação no cliente por nome
                    listaCategorias.sort((a, b) -> {
                        String nomeA = (a.getVisual() != null && a.getVisual().getNome() != null)
                                ? a.getVisual().getNome() : "";
                        String nomeB = (b.getVisual() != null && b.getVisual().getNome() != null)
                                ? b.getVisual().getNome() : "";
                        return nomeA.compareToIgnoreCase(nomeB);
                    });

                    if (listaCategorias.isEmpty()) {
                        // Verifica se há QUALQUER categoria antes de criar padrões
                        // para não duplicar se o usuário só não tem do tipo atual
                        FirestoreSchema.contasCategoriasCol()
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snapGlobal -> {
                                    if (snapGlobal.isEmpty()) {
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
        builder.setTitle("Nova Categoria de " + tipoAtual.getDescricao().toLowerCase()
                .substring(0, 1).toUpperCase() + tipoAtual.getDescricao().substring(1).toLowerCase());

        final EditText input = new EditText(this);
        input.setHint("Nome da categoria");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String nome = input.getText().toString().trim();
            if (TextUtils.isEmpty(nome)) {
                Toast.makeText(this, "Informe um nome", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verifica duplicidade ANTES de salvar
            verificarDuplicidadeESalvar(nome);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Verifica se já existe uma categoria com o mesmo nome e tipo antes de salvar.
     * O ID é gerado por slug do nome, então nomes iguais gerariam o mesmo ID
     * e sobreporiam silenciosamente — aqui bloqueamos isso.
     */
    private void verificarDuplicidadeESalvar(String nome) {
        String idCandidato = CategoriaIdHelper.slugify(nome);

        // Busca pelo ID gerado — se existir, é duplicata
        FirestoreSchema.contasCategoriaDoc(idCandidato)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Documento com esse slug já existe — verifica se é do mesmo tipo
                        ContasCategoriaModel existente = doc.toObject(ContasCategoriaModel.class);
                        if (existente != null && existente.getTipo() == tipoAtual.getId()) {
                            Toast.makeText(this,
                                    "Já existe uma categoria chamada \"" + nome + "\" neste tipo.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Slug existe mas é de outro tipo — gera ID com sufixo do tipo
                            salvarNovaCategoria(nome, idCandidato + "_" + tipoAtual.getId());
                        }
                    } else {
                        // ID livre — salva normalmente
                        salvarNovaCategoria(nome, idCandidato);
                    }
                })
                .addOnFailureListener(e -> {
                    // Em caso de falha na verificação, tenta salvar mesmo assim
                    salvarNovaCategoria(nome, idCandidato);
                });
    }

    private void salvarNovaCategoria(String nome, String idForcado) {
        ContasCategoriaModel novaCat = new ContasCategoriaModel();
        novaCat.setId(idForcado); // força o ID para evitar que o repository gere outro
        novaCat.getVisual().setNome(nome);
        novaCat.getVisual().setIcone("ic_default");
        novaCat.getVisual().setCor("#757575");
        novaCat.setTipo(tipoAtual.getId()); // usa o tipo correto da tela
        novaCat.setAtiva(true);

        repository.salvar(novaCat, new ContasCategoriaRepository.Callback() {
            @Override
            public void onSucesso() {
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        "Categoria criada!", Toast.LENGTH_SHORT).show();
                carregarCategorias();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarSwipeParaExcluir() {

        ItemTouchHelper.SimpleCallback itemTouch =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView r,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder t) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) {
                            adapter.notifyDataSetChanged();
                            return;
                        }
                        ContasCategoriaModel categoria = listaCategorias.get(pos);
                        excluirCategoria(viewHolder, categoria);
                    }
                };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
    }

    public void excluirCategoria(RecyclerView.ViewHolder holder, ContasCategoriaModel categoria) {
        repository.verificarEExcluir(categoria, new ContasCategoriaRepository.Callback() {
            @Override
            public void onSucesso() {
                int posAtual = holder.getAdapterPosition();
                if (posAtual == RecyclerView.NO_POSITION) return;
                listaCategorias.remove(posAtual);
                adapter.notifyItemRemoved(posAtual);
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        "Excluída!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        erro, Toast.LENGTH_SHORT).show();
                int posAtual = holder.getAdapterPosition();
                if (posAtual != RecyclerView.NO_POSITION) adapter.notifyItemChanged(posAtual);
            }
        });
    }

    void mostrarDialogEditarCategoria(ContasCategoriaModel categoria) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Categoria");

        final EditText input = new EditText(this);
        input.setHint("Nome da categoria");

        final String nomeAtual =
                (categoria.getVisual() != null &&
                        categoria.getVisual().getNome() != null)
                        ? categoria.getVisual().getNome()
                        : "";

        input.setText(nomeAtual);
        input.setSelection(nomeAtual.length());

        builder.setView(input);

        builder.setPositiveButton("Confirmar", (dialog, which) -> {

            String novoNome = input.getText().toString().trim();

            if (TextUtils.isEmpty(novoNome)) {
                Toast.makeText(this, "Informe um nome", Toast.LENGTH_SHORT).show();
                return;
            }

            if (novoNome.equalsIgnoreCase(nomeAtual)) {
                return;
            }

            atualizarCategoria(categoria, novoNome);
        });

        builder.setNegativeButton("Sair", null);
        builder.show();
    }

    private void atualizarCategoria(ContasCategoriaModel categoria, String novoNome) {

        categoria.getVisual().setNome(novoNome);

        repository.salvar(categoria, new ContasCategoriaRepository.Callback() {
            @Override
            public void onSucesso() {
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        "Categoria atualizada!", Toast.LENGTH_SHORT).show();
                carregarCategorias();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(SelecionarCategoriaContasActivity.this,
                        erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}