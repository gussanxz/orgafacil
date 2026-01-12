package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

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

public class CadastroCategoriaActivity extends AppCompatActivity {

    FloatingActionButton fabVoltar; //Criando o objeto
    FloatingActionButton fabSalvarCategoria; //Criando o objeto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Habilita o edge to edge
        EdgeToEdge.enable(this);

        //Definimos o xml/layout que iremos apresentar na activity
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_categoria_cadastro_categorias); //--> o ID do layout eh "main"

        //Define a area de conteudo segura do app sem a barra de status e navegacao ficar por cima do conteudo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();
    }

    //Inicializando os componentes, so pra separar mesmo
    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSalvarCategoria = findViewById(R.id.fabSuperiorSalvarCategoria);
    }

    //As acitivitys são pilhas, então não abrimos Vendas de novo, e sim fechamos a atual para poder retornar
    public void retornarParaVendasCadastros(View view) {

        finish(); //retorna para tela anterior

    }

    public void salvarCategoria(View view) {
        Toast toast = Toast.makeText(this, "Categoria salva com sucesso!\nApenas mensagem de Teste", Toast.LENGTH_SHORT);
        toast.show();

        finish(); //retorna para tela anterior
    }
}
