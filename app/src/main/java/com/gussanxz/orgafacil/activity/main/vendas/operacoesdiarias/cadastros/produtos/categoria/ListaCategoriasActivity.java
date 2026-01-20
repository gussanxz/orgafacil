package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

// --- IMPORTS FIRESTORE / REPOSITORY ---
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.adapter.AdapterCategoriaVendas;
import com.gussanxz.orgafacil.model.Categoria;
import com.gussanxz.orgafacil.repository.CategoriaRepository;

import java.util.ArrayList;
import java.util.List;

public class ListaCategoriasActivity extends AppCompatActivity implements AdapterCategoriaVendas.OnCategoriaActionListener {

    // Componentes Visuais
    private RecyclerView recyclerCategorias;
    private LinearLayout emptyState;
    private TextInputEditText editBusca;
    private ChipGroup chipGroupFiltro;

    // Dados e Adaptador
    private AdapterCategoriaVendas adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>(); // Fonte da verdade (Todos os dados)
    private final List<Categoria> listaFiltrada = new ArrayList<>();        // Dados exibidos (Filtrados)

    // --- REPOSITÓRIO E LISTENER ---
    private CategoriaRepository repository;
    private ListenerRegistration listenerRegistration; // Variável para controlar o "ouvido" do banco

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        // 1. Inicializa o Repositório
        repository = new CategoriaRepository();

        // 2. Configura a tela
        inicializarComponentes();
        configurarRecyclerView();
        configurarListenerDeFiltro();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 3. Ao abrir a tela, começa a escutar o banco em Tempo Real
        recuperarCategoriasEmTempoReal();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 4. Ao sair da tela, PARA de escutar (Economiza bateria e dados)
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // --- CONEXÃO COM O REPOSITÓRIO ---

    private void recuperarCategoriasEmTempoReal() {
        // O repositório sabe onde buscar (vendas_categorias). A Activity só recebe a lista pronta.
        listenerRegistration = repository.listarTempoReal(new CategoriaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                // Atualiza nossa lista total
                listaCategoriasTotal.clear();
                listaCategoriasTotal.addAll(lista);

                // Aplica os filtros (Busca/Chips) para atualizar a tela
                filtrarDados();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaCategoriasActivity.this, "Erro ao carregar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- AÇÕES DO USUÁRIO (EDITAR / EXCLUIR) ---

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
        new AlertDialog.Builder(this)
                .setTitle("Excluir Categoria")
                .setMessage("Tem certeza que deseja excluir: " + categoria.getNome() + "?")
                .setCancelable(false)
                .setPositiveButton("Confirmar", (dialog, which) -> {

                    // Chama o repositório para excluir
                    repository.excluir(categoria.getId(), new CategoriaRepository.CategoriaCallback() {
                        @Override
                        public void onSucesso(String mensagem) {
                            Toast.makeText(ListaCategoriasActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                            // Não precisamos recarregar nada manualmente.
                            // O listener do onStart vai perceber a exclusão e atualizar a lista sozinho!
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ListaCategoriasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- LÓGICA DE FILTROS (MANTIDA IGUAL) ---
    // Essa lógica roda localmente no celular, sem gastar internet extra

    private void filtrarDados() {
        String texto = (editBusca.getText() != null) ? editBusca.getText().toString().toLowerCase() : "";
        int chipId = chipGroupFiltro.getCheckedChipId();

        listaFiltrada.clear();

        for (Categoria c : listaCategoriasTotal) {
            // 1. Filtro de Texto
            boolean matchTexto = c.getNome().toLowerCase().contains(texto) ||
                    c.getDescricao().toLowerCase().contains(texto);

            // 2. Filtro de Status (Ativo/Inativo)
            boolean matchStatus = true;
            if (chipId == R.id.chipAtivas) matchStatus = c.isAtiva();
            else if (chipId == R.id.chipInativas) matchStatus = !c.isAtiva();

            // 3. Se passou em tudo, adiciona
            if (matchTexto && matchStatus) {
                listaFiltrada.add(c);
            }
        }

        adapter.setListaFiltrada(listaFiltrada);
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
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrarDados(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupFiltro.setOnCheckedChangeListener((group, checkedId) -> filtrarDados());
    }

    // --- CONFIGURAÇÕES BÁSICAS ---

    private void configurarRecyclerView() {
        // Inicializa o adapter com lista vazia. Ele será preenchido pelo filtrarDados()
        adapter = new AdapterCategoriaVendas(new ArrayList<>(), this, this);
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategorias.setAdapter(adapter);
    }

    private void inicializarComponentes() {
        recyclerCategorias = findViewById(R.id.recyclerCategorias);
        editBusca = findViewById(R.id.editBusca);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltroStatus);
        emptyState = findViewById(R.id.emptyState);
    }

    public void acessarCadastroCategoria(View view) {
        startActivity(new Intent(this, CadastroCategoriaActivity.class));
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}