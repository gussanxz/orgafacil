package com.gussanxz.orgafacil.activity.vendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;

public class VendasCadastrosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vendas_cadastros);

        View main = findViewById(R.id.rootCadastros); // 2) precisa existir no layout
        if (main == null) {
            throw new IllegalStateException("View R.id.main não existe em activity_vendas_cadastros.xml");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootCadastros), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void exibirCadastroCategorias(View view) {
        Intent intent = new Intent(this, CadastroCategoriaActivity.class);
        startActivity(intent);
    }

    public void exibirCadastroProdutosServicos(View view) {
        Intent intent = new Intent(this, CadastroProdutosServicosActivity.class);
        startActivity(intent);
    }

    public void exibirCadastroCombos(View view) {
        Intent intent = new Intent(this, CadastroComboActivity.class);
        startActivity(intent);
    }

}