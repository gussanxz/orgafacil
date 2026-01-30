package com.gussanxz.orgafacil.features.vendas.cadastros.catalogo.categoria;

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
import com.gussanxz.orgafacil.ui.vendas.AdapterItemListaCategoriasVendas;
import com.gussanxz.orgafacil.util_helper.SwipeCallback; // Certifique-se que sua classe Helper está importada
import com.gussanxz.orgafacil.data.model.Categoria;
import com.gussanxz.orgafacil.data.repository.CategoriaRepository;

import java.util.ArrayList;
import java.util.List;

public class ListaCategoriasActivity extends AppCompatActivity implements AdapterItemListaCategoriasVendas.OnCategoriaActionListener {

    // Componentes Visuais
    private RecyclerView recyclerCategorias;
    private LinearLayout emptyState;
    private TextInputEditText editBusca;
    private ChipGroup chipGroupFiltro;

    // Dados e Adaptador
    private AdapterItemListaCategoriasVendas adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>();
    private final List<Categoria> listaFiltrada = new ArrayList<>();

    // Repositório
    private CategoriaRepository repository;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        repository = new CategoriaRepository();

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
    // Este método é usado tanto pelo botão de lixeira quanto pelo Swipe
    private void confirmarExclusao(Categoria categoria, int positionParaRestaurar) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Categoria")
                .setMessage("Tem certeza que deseja excluir: " + categoria.getNome() + "?")
                .setCancelable(false)
                .setPositiveButton("Sim", (dialog, which) -> {
                    // Chama o repositório
                    repository.excluir(categoria.getId(), new CategoriaRepository.CategoriaCallback() {
                        @Override
                        public void onSucesso(String mensagem) {
                            Toast.makeText(ListaCategoriasActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                            // O listener do onStart atualizará a lista automaticamente
                        }

                        @Override
                        public void onErro(String erro) {
                            Toast.makeText(ListaCategoriasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                            // Se deu erro, restaura o item swipado (se houver)
                            if (positionParaRestaurar != -1) adapter.notifyItemChanged(positionParaRestaurar);
                        }
                    });
                })
                .setNegativeButton("Não", (dialog, which) -> {
                    // SE O USUÁRIO CANCELAR E TIVER FEITO SWIPE, PRECISAMOS RESTAURAR O ITEM VISUALMENTE
                    if (positionParaRestaurar != -1) {
                        adapter.notifyItemChanged(positionParaRestaurar);
                    }
                })
                .show();
    }

    // --- MÉTODOS DA INTERFACE (Cliques no Item/Lixeira) ---

    @Override
    public void onEditarClick(Categoria categoria) {
        abrirTelaEdicao(categoria);
    }

    @Override
    public void onExcluirClick(Categoria categoria) {
        // Ao clicar na lixeira, não temos posição de swipe para restaurar, então passamos -1
        confirmarExclusao(categoria, -1);
    }

    // --- CONFIGURAÇÃO DO RECYCLER VIEW COM SWIPE ---

    private void configurarRecyclerView() {
        // Passamos a lista filtrada (que começa vazia ou com dados) para o adapter
        adapter = new AdapterItemListaCategoriasVendas(listaFiltrada, this, this);
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategorias.setAdapter(adapter);

        SwipeCallback swipeHelper = new SwipeCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (position == RecyclerView.NO_POSITION) return;

                // BUSCA SEMPRE NA LISTA QUE O ADAPTER ESTÁ USANDO (listaFiltrada)
                Categoria categoriaSelecionada = listaFiltrada.get(position);

                // Use LEFT e RIGHT para bater com o desenho do SwipeCallback genérico
                if (direction == ItemTouchHelper.LEFT) {
                    // <--- ESQUERDA (EXCLUIR)
                    confirmarExclusao(categoriaSelecionada, position);

                } else if (direction == ItemTouchHelper.RIGHT) {
                    // ---> DIREITA (EDITAR)
                    adapter.notifyItemChanged(position); // Fecha o swipe visualmente
                    abrirTelaEdicao(categoriaSelecionada);
                }
            }
        };

        new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerCategorias);
    }

    // --- MÉTODOS AUXILIARES ---

    private void abrirTelaEdicao(Categoria categoria) {
        Intent intent = new Intent(this, CadastroCategoriaActivity.class);
        intent.putExtra("modoEditar", true);
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("nome", categoria.getNome());
        intent.putExtra("descricao", categoria.getDescricao());
        intent.putExtra("iconeIndex", categoria.getIndexIcone());
        intent.putExtra("ativa", categoria.isAtiva());
        startActivity(intent);
    }

    private void recuperarCategoriasEmTempoReal() {
        listenerRegistration = repository.listarTempoReal(new CategoriaRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategoriasTotal.clear();
                listaCategoriasTotal.addAll(lista);
                filtrarDados();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaCategoriasActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarDados() {
        String texto = (editBusca.getText() != null) ? editBusca.getText().toString().toLowerCase() : "";
        int chipId = chipGroupFiltro.getCheckedChipId();

        listaFiltrada.clear();

        for (Categoria c : listaCategoriasTotal) {
            boolean matchTexto = c.getNome().toLowerCase().contains(texto) ||
                    c.getDescricao().toLowerCase().contains(texto);

            boolean matchStatus = true;
            if (chipId == R.id.chipAtivas) matchStatus = c.isAtiva();
            else if (chipId == R.id.chipInativas) matchStatus = !c.isAtiva();

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