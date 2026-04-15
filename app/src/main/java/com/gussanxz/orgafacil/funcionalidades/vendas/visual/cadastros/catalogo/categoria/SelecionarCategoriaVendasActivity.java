package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.CategoriaCatalogoRepository;

import java.util.ArrayList;
import java.util.List;

public class SelecionarCategoriaVendasActivity extends AppCompatActivity implements AdapterItemListaCategoriasCatalogoVendas.OnCategoriaActionListener {

    private RecyclerView recyclerView;
    private AdapterItemListaCategoriasCatalogoVendas adapter;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private CategoriaCatalogoRepository repository;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        repository = new CategoriaCatalogoRepository();

        //Configura o recyclerView
        recyclerView = findViewById(R.id.recyclerCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //Configurando Adapter (passando a lista de Objetos)
        adapter = new AdapterItemListaCategoriasCatalogoVendas(listaCategorias, this, this);
        recyclerView.setAdapter(adapter);

        //Buscar os dados
        carregarCategoriasDoFirebase();
    }

    private void carregarCategoriasDoFirebase() {
        listenerRegistration = repository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategorias.clear();
                listaCategorias.addAll(lista);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(SelecionarCategoriaVendasActivity.this,
                        "Erro ao carregar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // --- 3. IMPLEMENTAÇÃO DOS CLIQUES (Obrigatório por causa da Interface) ---

    @Override
    public void onEditarClick(Categoria categoria) {
        // LÓGICA DE SELEÇÃO:
        // Como essa tela é "SelecionarCategoria", ao clicar no item (que no adapter chama onEditarClick),
        // nós devolvemos o resultado para a tela anterior em vez de abrir a edição.

        Intent intent = new Intent();
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("nomeCategoria", categoria.getNome());

        setResult(RESULT_OK, intent);
        finish(); // Fecha a tela e volta
    }

    @Override
    public void onExcluirClick(Categoria categoria) {
        // Como é uma tela de SELEÇÃO, talvez você não queira permitir excluir por aqui.
        // Se quiser bloquear, mostre apenas um Toast:
        Toast.makeText(this, "Para excluir, vá ao menu Cadastros", Toast.LENGTH_SHORT).show();

        // Se quiser permitir excluir mesmo assim, copie a lógica do AlertDialog da outra Activity para cá.
    }
    public void retornarParaVendasCadastros(View view) {
        finish();
    }

    public void acessarCadastroCategoria(View view) {
        startActivity(new Intent(this, CadastroCategoriaCatalogoActivity.class));
    }
}






