package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros;

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
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria.ListaCategoriasCatalogoActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.combo.CadastroComboActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.modificador.CadastroModificadorActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.ListaProdutosEServicosActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.pessoas.cliente.CadastroClienteActivity;

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
        // Remover overlays das funcionalidades já disponíveis
        ocultarOverlay(R.id.overlayClientes);
        ocultarOverlay(R.id.overlayCombos);
        ocultarOverlay(R.id.overlayModificadores);

        // Wiring manual do item Clientes (sem android:onClick no XML)
        View itemClientes = findViewById(R.id.itemClientes);
        if (itemClientes != null) {
            itemClientes.setOnClickListener(v -> exibirCadastroClientes(v));
        }
    }

    private void ocultarOverlay(int viewId) {
        View overlay = findViewById(viewId);
        if (overlay != null) overlay.setVisibility(View.GONE);
    }

    private void configurarBotoesBloqueados() {
        // Listener único reaproveitado
        View.OnClickListener listenerBloqueio = view -> {
            Toast.makeText(VendasCadastrosActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();
        };

        // IDs dos overlays ainda sem implementação
        int[] idsBloqueados = {
                R.id.overlayFornecedores,
                R.id.overlayVendedores,
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
        startActivity(new Intent(this, ListaCategoriasCatalogoActivity.class));
    }

    public void exibirListaProdutosServicos(View view) {
        startActivity(new Intent(this, ListaProdutosEServicosActivity.class));
    }

    public void exibirCadastroClientes(View view) {
        startActivity(new Intent(this, CadastroClienteActivity.class));
    }

    public void exibirCadastroCombos(View view) {
        startActivity(new Intent(this, CadastroComboActivity.class));
    }

    public void exibirCadastroModificadores(View view) {
        startActivity(new Intent(this, CadastroModificadorActivity.class));
    }

}