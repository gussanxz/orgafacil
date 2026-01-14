package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import com.gussanxz.orgafacil.adapter.AdapterCategoriaVendas;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gussanxz.orgafacil.model.Categoria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelecionarCategoriaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterCategoriaVendas adapter;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private DatabaseReference firebaseRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_categoria_lista_categorias);

        //Inicializar o firebase
        firebaseRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        //Configura o recyclerView
        recyclerView = findViewById(R.id.recyclerCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //Configurando Adapter (passando a lista de Objetos)
        adapter = new AdapterCategoriaVendas(listaCategorias, this);
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
                Toast.makeText(SelecionarCategoriaActivity.this,
                        "Erro ao carregar: " + error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }
}





