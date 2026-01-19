package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class ListaCategoriasActivity extends AppCompatActivity implements AdapterCategoriaVendas.OnCategoriaActionListener {

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
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        // Configurações iniciais
        inicializarComponentes();
        configurarFirebase();
        configurarRecyclerView();
        configurarListenerDeFiltro();

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
    @Override
    public void onEditarClick(Categoria categoria) {
        Intent intent = new Intent(this, CadastroCategoriaActivity.class);
        intent.putExtra("modoEditar", true);
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("nome", categoria.getNome());
        intent.putExtra("descricao", categoria.getDescricao());
        intent.putExtra("iconeIndex", categoria.getIndexIcone());
        intent.putExtra("ativa", categoria.isAtiva());
        startActivity(intent);
    }

    @Override
    public void onExcluirClick(Categoria categoria) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Excluir Categoria");
        alertDialog.setMessage("Tem certeza que deseja excluir a categoria: " + categoria.getNome() + "?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", (dialog, which) -> {
            // Remove do Firebase
            if (mAuth.getCurrentUser() != null && categoria.getId() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                mDatabase.child("vendas")
                          .child("uid")
                          .child(uid)
                          .child("cadastros")
                          .child("categorias")
                          .child(categoria.getId())
                          .removeValue();

                Toast.makeText(this, "Categoria excluída!", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("Cancelar", null);
        alertDialog.show();

    }

    // --- Lógica de Filtros  ---
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
        adapter = new AdapterCategoriaVendas(new ArrayList<>(), this,this);
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