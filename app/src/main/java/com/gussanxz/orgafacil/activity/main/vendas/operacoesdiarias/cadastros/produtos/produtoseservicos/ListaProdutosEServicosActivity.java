package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtoseservicos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;

public class ListaProdutosEServicosActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_lista_produtos_e_servicos);

        //Define a area de conteudo segura do app sem a barra de status e navegacao ficar por cima do conteudo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void cadastrarProdutoOuServico (View view) {
        startActivity(new Intent(this, CadastroProdutosServicosActivity.class));
    }

    public void retornarParaVendasCadastros(View view) {
        finish(); //retorna para tela anterior

    }

}

