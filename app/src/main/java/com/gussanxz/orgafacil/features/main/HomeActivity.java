package com.gussanxz.orgafacil.features.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.features.configuracoes.ConfigsActivity;
import com.gussanxz.orgafacil.activity.features.contas.ResumoContasActivity;
import com.gussanxz.orgafacil.features.vendas.ResumoVendasActivity;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private TextView textoContas, textoVendas, textoMercado, textoAtividades, textoConfigs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_intro_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textoContas = findViewById(R.id.textViewContas);
        textoVendas = findViewById(R.id.textViewVendas);
        textoMercado = findViewById(R.id.textViewMercado);
        textoAtividades = findViewById(R.id.textViewAtividades);
        textoConfigs = findViewById(R.id.textViewConfigs);

        configurarBotoesBloqueados();
    }

    public void acessarResumoContasActivity(View view) {
        startActivity(new Intent(this, ResumoContasActivity.class));
        Log.i(TAG, "acessou ResumoContasActivity");
    }

    public void acessarResumoVendasAcitivity(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
        Log.i(TAG, "acessou ResumoVendasActivity");
    }

    public void acessarResumoListaMercado(View view) {
//        startActivity(new Intent(this, ResumoListaMercadoActivity.class));
//        Log.i(TAG, "acessou ResumoListaMercadoActivity");
    }
//
    public void acessarListaAtividades(View view) {
//        startActivity(new Intent(this, ListaAtividadesActivity.class));
//        Log.i(TAG, "acessou ListaAtividadesActivity");
    }
//
    public void acessarBoletos(View view) {
//        startActivity(new Intent(this, BoletosActivity.class));
//        Log.i(TAG, "acessou BoletosActivity");
    }
private void configurarBotoesBloqueados() {
    // Listener único reaproveitado
    View.OnClickListener listenerBloqueio = view -> {
        Toast.makeText(HomeActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();
    };

    // IDs dos overlays definidos no seu XML
    int[] idsBloqueados = {
            R.id.imageViewMercado,
            R.id.imageViewTodo,
            R.id.imageViewBoletoCPF
    };

    // Loop para aplicar o listener em todos
    for (int id : idsBloqueados) {
        View overlay = findViewById(id);
        if (overlay != null) {
            overlay.setOnClickListener(listenerBloqueio);
        }
    }
}

    public void acessarConfigs(View view) {
        startActivity(new Intent(this, ConfigsActivity.class));
        Log.i(TAG, "acessou ConfigsActivity");
    }
}