package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.pessoas.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.main.vendas.ResumoVendasActivity;

public class CadastroClienteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_pessoas_cadastro_cliente);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void retornarResumoVendas(View view){
        startActivity(new Intent(this, ResumoVendasActivity.class));
    }

    public void confirmarCadastroCliente(View view){

    }

}