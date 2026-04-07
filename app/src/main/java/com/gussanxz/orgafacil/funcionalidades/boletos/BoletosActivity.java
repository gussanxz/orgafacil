package com.gussanxz.orgafacil.funcionalidades.boletos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;

public class BoletosActivity extends AppCompatActivity {

    private BoletoRepository repository = new BoletoRepository();

    private final ActivityResultLauncher<Intent> launcherAdicionar =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) carregarBoletos();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boletos);

        findViewById(R.id.btnAdicionarBoleto).setOnClickListener(v ->
                launcherAdicionar.launch(new Intent(this, AdicionarBoletoActivity.class)));

        carregarBoletos();
    }

    private void carregarBoletos() {
        repository.listar(new BoletoRepository.ListaCallback() {
            @Override
            public void onSucesso(java.util.List<BoletoModel> lista) {
                // Adapter pode ser expandido depois; por ora exibe contagem
                Toast.makeText(BoletosActivity.this,
                        lista.size() + " boleto(s) encontrado(s).", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(BoletosActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}