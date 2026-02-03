package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;

import java.util.ArrayList;
import java.util.List;

public class SelecionarCategoriaVendasActivity extends AppCompatActivity implements AdapterItemListaCategoriasCatalogoVendas.OnCategoriaActionListener {

    private RecyclerView recyclerView;
    private AdapterItemListaCategoriasCatalogoVendas adapter;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private DatabaseReference firebaseRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        //Inicializar o firebase
        firebaseRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

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

        if (mAuth.getCurrentUser() == null) return;

        String idUsuario = mAuth.getCurrentUser().getUid();

        // CAMINHO
        // vendas -> uid -> idUsuario -> cadastros -> categorias
        DatabaseReference categoriasRef = firebaseRef
                .child("vendas")
                .child("uid")
                .child(idUsuario)
                .child("cadastros")
                .child("categorias");

        //Listener para ler os dados
        categoriasRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                listaCategorias.clear(); // Limpa a lista antes de adicionar para não duplicar

                for (DataSnapshot ds : snapshot.getChildren()) {

                    //Converte JSON do Firebase para Objeto Categoria
                    Categoria categoria = ds.getValue(Categoria.class);

                    //Verifica se a categoria não está vazia
                    if (categoria != null) {
                        listaCategorias.add(categoria);
                    }
                }

                //Notifica o adapter que os dados foram atualizados
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelecionarCategoriaVendasActivity.this,
                        "Erro ao carregar: " + error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    // --- 3. IMPLEMENTAÇÃO DOS CLIQUES (Obrigatório por causa da Interface) ---

    @Override
    public void onEditarClick(Categoria categoria) {
        // LÓGICA DE SELEÇÃO:
        // Como essa tela é "SelecionarCategoria", ao clicar no item (que no adapter chama onEditarClick),
        // nós devolvemos o resultado para a tela anterior em vez de abrir a edição.

        Intent intent = new Intent();
        intent.putExtra("categoriaSelecionada", categoria); // Sua classe Categoria precisa ser Serializable ou Parcelable
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
}






