package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
// IMPORTANTE: Use o Repository específico de Vendas
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;

import java.util.ArrayList;
import java.util.List;

public class ListaCategoriasCatalogoActivity extends AppCompatActivity implements AdapterItemListaCategoriasCatalogoVendas.OnCategoriaActionListener {

    // Componentes Visuais
    private RecyclerView recyclerCategorias;
    private LinearLayout emptyState;
    private TextInputEditText editBusca;
    private ChipGroup chipGroupFiltro;

    // Dados e Adaptador
    private AdapterItemListaCategoriasCatalogoVendas adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>();
    private final List<Categoria> listaFiltrada = new ArrayList<>();

    // Repositório (Atualizado para Vendas)
    private CategoriaCatalogoRepository repository;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        repository = new CategoriaCatalogoRepository();

        inicializarComponentes();
        configurarRecyclerView();
        configurarListenerDeFiltro();
    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarCategoriasEmTempoReal();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // --- LÓGICA CENTRALIZADA DE EXCLUSÃO ---
    private void confirmarExclusao(Categoria categoria, int positionParaRestaurar) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Categoria")
                .setMessage("Tem certeza que deseja excluir: " + categoria.getNome() + "?")
                .setCancelable(false)
                .setPositiveButton("Sim", (dialog, which) -> {
                    // Chama o repositório de Vendas
                    repository.excluir(categoria.getId(), new CategoriaCatalogoRepository.Callback() {
                        @Override
                        public void onSucesso(String mensagem) {
                            Toast.makeText(ListaCategoriasCatalogoActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                            // O listener do onStart atualizará a lista automaticamente
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ListaCategoriasCatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                            // Se deu erro, restaura o item swipado
                            if (positionParaRestaurar != -1) adapter.notifyItemChanged(positionParaRestaurar);
                        }
                    });
                })
                .setNegativeButton("Não", (dialog, which) -> {
                    // Restaura visualmente se cancelou o swipe
                    if (positionParaRestaurar != -1) {
                        adapter.notifyItemChanged(positionParaRestaurar);
                    }
                })
                .show();
    }

    // --- MÉTODOS DA INTERFACE DO ADAPTER ---
    @Override
    public void onEditarClick(Categoria categoria) {
        abrirTelaEdicao(categoria);
    }

    @Override
    public void onExcluirClick(Categoria categoria) {
        confirmarExclusao(categoria, -1);
    }

    // --- CONFIGURAÇÃO DO RECYCLER VIEW ---
    private void configurarRecyclerView() {
        adapter = new AdapterItemListaCategoriasCatalogoVendas(listaFiltrada, this, this); // Context e Listener
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategorias.setAdapter(adapter);

        SwipeCallback swipeHelper = new SwipeCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                Categoria categoriaSelecionada = listaFiltrada.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    confirmarExclusao(categoriaSelecionada, position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position); // Fecha o swipe
                    abrirTelaEdicao(categoriaSelecionada);
                }
            }
        };

        new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerCategorias);
    }

    // --- NAVEGAÇÃO ---
    private void abrirTelaEdicao(Categoria categoria) {
        // Aponta para a Activity de Cadastro que criamos
        Intent intent = new Intent(this, CadastroCategoriaCatalogoActivity.class);

        intent.putExtra("modoEditar", true);
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("tipo", "PRODUTO"); // OBRIGATÓRIO: Define o contexto da tela

        intent.putExtra("nome", categoria.getNome());
        intent.putExtra("descricao", categoria.getDescricao());
        intent.putExtra("iconeIndex", categoria.getIndexIcone());
        intent.putExtra("ativa", categoria.isAtiva());

        // Passa a URL da foto (se houver)
        intent.putExtra("urlImagem", categoria.getUrlImagem());

        startActivity(intent);
    }

    public void acessarCadastroCategoria(View view) {
        Intent intent = new Intent(this, CadastroCategoriaCatalogoActivity.class);
        intent.putExtra("tipo", "PRODUTO"); // Define que estamos criando um Produto
        startActivity(intent);
    }

    // --- DADOS E FILTROS ---
    private void recuperarCategoriasEmTempoReal() {
        listenerRegistration = repository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategoriasTotal.clear();
                listaCategoriasTotal.addAll(lista);
                filtrarDados();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaCategoriasCatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarDados() {
        String texto = (editBusca.getText() != null) ? editBusca.getText().toString().toLowerCase() : "";
        int chipId = chipGroupFiltro.getCheckedChipId();

        listaFiltrada.clear();

        for (Categoria c : listaCategoriasTotal) {
            boolean matchTexto = c.getNome().toLowerCase().contains(texto) ||
                    (c.getDescricao() != null && c.getDescricao().toLowerCase().contains(texto));

            boolean matchStatus = true;
            if (chipId == R.id.chipAtivas) matchStatus = c.isAtiva();
            else if (chipId == R.id.chipInativas) matchStatus = !c.isAtiva();

            if (matchTexto && matchStatus) {
                listaFiltrada.add(c);
            }
        }

        // Importante: Notificar o adapter da mudança completa
        adapter.atualizarLista(listaFiltrada);
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

    private void inicializarComponentes() {
        recyclerCategorias = findViewById(R.id.recyclerCategorias);
        editBusca = findViewById(R.id.editBusca);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltroStatus);
        emptyState = findViewById(R.id.emptyState);
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}