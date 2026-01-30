package com.gussanxz.orgafacil.features.vendas.cadastros.catalogo.modificador;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gussanxz.orgafacil.R;

public class CadastroModificadorActivity extends AppCompatActivity {

    //Criando os objetos dos elementos de Fab (FloatingActionButton)
    FloatingActionButton fabVoltar;
    FloatingActionButton fabSuperiorSalvarCategoria;
    FloatingActionButton fabInferiorSalvarCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Habilita o edge to edge
        EdgeToEdge.enable(this);

        //Definimos o xml/layout que iremos apresentar na activity
        setContentView(R.layout.ac_main_vendas_opd_cadastro_modificadores);

        //Define a area de conteudo segura do app sem a barra de status e navegacao ficar por cima do conteudo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

    }

    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSuperiorSalvarCategoria = findViewById(R.id.fabSuperiorSalvarCategoria);
        fabInferiorSalvarCategoria = findViewById(R.id.fabInferiorSalvarCategoria);

    }

    public void retornarParaVendasCadastros(View view) {

        finish(); //retorna para tela anterior

    }

    //É interessante adicionar uma lógica pra identificar se é um produto ou serviço e salvar de acordo
    public void salvarModificador(View view) {
        Toast toast = Toast.makeText(this, "Modificador salvo com sucesso!\nApenas mensagem de Teste", Toast.LENGTH_SHORT);
        toast.show();

        finish(); //retorna para tela anterior
    }
}
