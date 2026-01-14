package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.adapter.AdapterCategoriaVendas;
import com.gussanxz.orgafacil.model.Categoria;

import java.util.ArrayList;
import java.util.List;

public class ListaCategoriasActivity extends AppCompatActivity {

    private RecyclerView recyclerCategorias;
    private AdapterCategoriaVendas adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>();
    private List<Categoria> listaFiltrada = new ArrayList<>();
    private LinearLayout emptyState;

    // Filtros
    private TextInputEditText editBusca;
    private ChipGroup chipGroupFiltro;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener eventListenerRef;
    private DatabaseReference categoriasRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_categoria_lista_categorias); // Nome do seu layout XML

        // Configurações iniciais
        inicializarComponentes();
        configurarFirebase();
        configurarRecyclerView();
        configurarListenerDeFiltro();
        swipe();

    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarCategoriasDoFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (categoriasRef != null && eventListenerRef != null) {
            categoriasRef.removeEventListener(eventListenerRef);
        }
    }

    private void recuperarCategoriasDoFirebase() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Caminho exato que definimos: vendas > uid > ID_USUARIO > cadastros > categorias
        categoriasRef = mDatabase.child("vendas").child("uid").child(uid).child("cadastros").child("categorias");

        eventListenerRef = categoriasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaCategoriasTotal.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Categoria cat = ds.getValue(Categoria.class);
                    if (cat != null) {

                        // Fallback: se o id não veio do objeto, usa a key do Firebase
                        if (cat.getId() == null || cat.getId().trim().isEmpty()) {
                            cat.setId(ds.getKey());
                        }

                        listaCategoriasTotal.add(cat);
                    }
                }

                // Após baixar tudo, aplicamos os filtros atuais (texto + chips)
                filtrarDados();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ListaCategoriasActivity.this, "Erro ao carregar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LÓGICA DO SWIPE (IGUAL CONTAS ACTIVITY) ---
    public void swipe() {
        ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {

            @Override // Desenha o fundo Vermelho/Verde
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                Paint backgroundPaint = new Paint();
                Paint textPaint = new Paint();

                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(40f);
                textPaint.setAntiAlias(true);

                float textY = itemView.getTop() + itemView.getHeight() / 2f + 15;

                if (dX > 0) { // Direita -> Editar (Verde)
                    backgroundPaint.setColor(Color.parseColor("#4CAF50"));
                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                            (float) itemView.getBottom(), backgroundPaint);
                    c.drawText("Editar", itemView.getLeft() + 50, textY, textPaint);

                } else if (dX < 0) { // Esquerda -> Excluir (Vermelho)
                    backgroundPaint.setColor(Color.parseColor("#F44336"));
                    c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom(), backgroundPaint);
                    c.drawText("Excluir", itemView.getRight() - 200, textY, textPaint);
                }
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Permite arrastar para esquerda e direita
                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // --- CORREÇÃO DE SEGURANÇA AQUI ---
                // Verifica se a lista está vazia ou se a posição é inválida
                if (listaFiltrada.isEmpty() || position >= listaFiltrada.size()) {
                    // Se der erro, manda a lista se redesenhar para desfazer o swipe visualmente
                    adapter.notifyDataSetChanged();
                    return; // Para o código aqui para não crashar
                }
                // ----------------------------------

                // Pega a categoria correta da lista atual (mesmo se estiver filtrada)
                Categoria categoriaSelecionada = listaFiltrada.get(position);

                if (direction == ItemTouchHelper.START) {
                    // Swipe Esquerda -> Excluir
                    excluirCategoria(categoriaSelecionada, position);
                } else if (direction == ItemTouchHelper.END) {
                    // Swipe Direita -> Editar
                    editarCategoria(categoriaSelecionada, position);
                }
            }
        };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerCategorias);
    }


    // --- AÇÕES DO SWIPE ---

    private void excluirCategoria(Categoria categoria, int position) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Excluir Categoria");
        alertDialog.setMessage("Tem certeza que deseja excluir a categoria: " + categoria.getNome() + "?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", (dialog, which) -> {
            // Remove do Firebase
            if (mAuth.getCurrentUser() != null && categoria.getId() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                mDatabase.child("vendas").child("uid").child(uid)
                        .child("cadastros").child("categorias")
                        .child(categoria.getId())
                        .removeValue();

                Toast.makeText(this, "Categoria excluída!", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("Cancelar", (dialog, which) -> {
            Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position); // Restaura o item na tela
        });

        alertDialog.show();
    }
    private void editarCategoria(Categoria categoria, int position) {
        // Envia os dados para a tela de Cadastro para edição
        Intent intent = new Intent(this, CadastroCategoriaActivity.class);
        intent.putExtra("modoEditar", true); // Avisa que é edição
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("nome", categoria.getNome());
        intent.putExtra("descricao", categoria.getDescricao());
        intent.putExtra("iconeIndex", categoria.getIndexIcone());
        intent.putExtra("ativa", categoria.isAtiva());

        startActivity(intent);

        // Restaura o visual do item (remove o verde do fundo)
        adapter.notifyItemChanged(position);
    }

    // --- Lógica de Filtros (Poderosa!) ---
    private void filtrarDados() {

        // 1. Pega o texto digitado (minúsculo para facilitar a busca)
        String textoDigitado = "";
        if (editBusca.getText() != null) {
            textoDigitado = editBusca.getText().toString().toLowerCase();
        }

        // 2. Pega qual Chip está marcado
        int chipId = chipGroupFiltro.getCheckedChipId();

        // --- CORREÇÃO AQUI ---
        // NÃO crie uma nova lista (List<Categoria> lista = new...).
        // Limpe a lista GLOBAL para receber os novos dados.
        this.listaFiltrada.clear();

        // 3. Loop na lista TOTAL para decidir quem entra na lista FILTRADA
        for (Categoria c : listaCategoriasTotal) {

            // A. Verificação de Texto (Nome ou Descrição)
            boolean matchTexto = c.getNome().toLowerCase().contains(textoDigitado) ||
                    c.getDescricao().toLowerCase().contains(textoDigitado);

            // B. Verificação do Chip (Todas, Ativas ou Inativas)
            boolean matchStatus = true; // Por padrão aceita tudo (Chip Todas)

            if (chipId == R.id.chipAtivas) {
                matchStatus = c.isAtiva(); // Só aceita se for true
            } else if (chipId == R.id.chipInativas) {
                matchStatus = !c.isAtiva(); // Só aceita se for false
            }
            // Se for R.id.chipTodas, matchStatus continua true pra sempre

            // C. Se passar nos DOIS testes, adiciona na lista final
            if (matchTexto && matchStatus) {
                listaFiltrada.add(c);
            }
        }

        // 4. Manda a lista filtrada para o Adapter
        adapter.setListaFiltrada(listaFiltrada);

        // 5. Controla o desenho de "Nenhum item encontrado"
        atualizarEmptyState(listaFiltrada.isEmpty());
    }

    private void atualizarEmptyState(boolean estaVazia) {
        if (estaVazia) {
            recyclerCategorias.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerCategorias.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void configurarListenerDeFiltro() {
        // Listener para o Texto de Busca
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarDados(); // Chama o filtro a cada letra digitada
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Listener para quando CLICAR NOS BOTÕES (CHIPS)
        chipGroupFiltro.setOnCheckedChangeListener((group, checkedId) -> {
            filtrarDados(); // Chama o filtro quando troca o botão
        });
    }

    // --- Configurações Básicas ---

    private void configurarRecyclerView() {

        // Começa com lista vazia, mas tipada corretamente
        adapter = new AdapterCategoriaVendas(new ArrayList<>(), this);
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategorias.setAdapter(adapter);
    }

    private void configurarFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    private void inicializarComponentes() {
        recyclerCategorias = findViewById(R.id.recyclerCategorias);
        editBusca = findViewById(R.id.editBusca);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltroStatus);
        emptyState = findViewById(R.id.emptyState);
    }

    // Métodos dos botões
    public void acessarCadastroCategoria(View view) {
        startActivity(new Intent(this, CadastroCategoriaActivity.class));
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}