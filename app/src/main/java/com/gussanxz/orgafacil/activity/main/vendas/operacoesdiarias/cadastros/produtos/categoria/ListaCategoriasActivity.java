package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.gussanxz.orgafacil.adapter.AdapterCategoria;
import com.gussanxz.orgafacil.model.Categoria;

import java.util.ArrayList;
import java.util.List;

public class ListaCategoriasActivity extends AppCompatActivity {

    private RecyclerView recyclerCategorias;
    private AdapterCategoria adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>();
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
        //configurarFiltros();
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

    // --- Lógica de Filtros (Poderosa!) ---
    private void filtrarDados() {

        // 1. Pega o texto digitado (minúsculo para facilitar a busca)
        String textoDigitado = "";
        if (editBusca.getText() != null) {
            textoDigitado = editBusca.getText().toString().toLowerCase();
        }

        // 2. Pega qual Chip está marcado
        int chipId = chipGroupFiltro.getCheckedChipId();

        List<Categoria> listaFiltrada = new ArrayList<>();

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
        adapter = new AdapterCategoria(new ArrayList<>(), this);
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