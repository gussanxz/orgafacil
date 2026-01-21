package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
//import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria.CadastroCategoriaActivity;
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria.ListaCategoriasActivity;
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.combo.CadastroComboActivity;
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.modificador.CadastroModificadorActivity;
import com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtos_e_servicos.ListaProdutosEServicosActivity;

public class VendasCadastrosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastros);

        View main = findViewById(R.id.rootCadastros); // 2) precisa existir no layout
        if (main == null) {
            throw new IllegalStateException("View R.id.main não existe em activity_vendas_cadastros.xml");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootCadastros), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Chamada do método para configurar os bloqueios
        configurarBotoesBloqueados();
    }

    private void configurarBotoesBloqueados() {
        // Listener único reaproveitado
        View.OnClickListener listenerBloqueio = view -> {
            Toast.makeText(VendasCadastrosActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();
        };

        // IDs dos overlays definidos no seu XML
        int[] idsBloqueados = {
                R.id.overlayClientes,
                R.id.overlayFornecedores,
                R.id.overlayVendedores,
                R.id.overlayCombos,
                R.id.overlayModificadores
        };

        // Loop para aplicar o listener em todos
        for (int id : idsBloqueados) {
            View overlay = findViewById(id);
            if (overlay != null) {
                overlay.setOnClickListener(listenerBloqueio);
            }
        }
    }

    public void exibirCadastroCategorias(View view) {
        Intent intent = new Intent(this, ListaCategoriasActivity.class);
        startActivity(intent);
    }

    public void exibirListaProdutosServicos(View view) {
        Intent intent = new Intent(this, ListaProdutosEServicosActivity.class);
        startActivity(intent);
    }

//    public void exibirCadastroCombos(View view) {
//        Intent intent = new Intent(this, CadastroComboActivity.class);
//        startActivity(intent);
//    }
//
//    public void exibirCadastroModificadores(View view) {
//        Intent intent = new Intent(this, CadastroModificadorActivity.class);
//        startActivity(intent);
//    }

}