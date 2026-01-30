package com.gussanxz.orgafacil.features.vendas;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.features.vendas.novavenda.RegistrarVendasActivity;
import com.gussanxz.orgafacil.activity.features.vendas.operacoesdiarias.cadastros.VendasCadastrosActivity;
import com.gussanxz.orgafacil.activity.features.vendas.gestaoerelatorios.VendasEmAbertoActivity;
import com.gussanxz.orgafacil.util_helper.VisibilidadeHelper;

public class ResumoVendasActivity extends AppCompatActivity {
    private final String TAG = "ResumoVendasActivity";
    private String saldoOriginal = "R$ 150,00"; //ISSO AQUI EH PRA SER O VALOR PUXADO DO FIREBASE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // 1. Configuração do Saldo
        configurarSaldo();

        // 2. Configuração dos botões bloqueados
        configurarBotoesBloqueados();

        LinearLayout btnVendas = findViewById(R.id.cardVendas);

        // 2. Define o clique via código (Muito mais seguro!)
        if (btnVendas != null) {
            btnVendas.setOnClickListener(view -> {
                // Chama o seu método aqui
                acessarVendasEmAbertoActivity(view);
            });
        }

    }


    private void configurarSaldo() {
        TextView textSaldo = findViewById(R.id.textSaldo);
        ImageView imgOlhoSaldo = findViewById(R.id.imgOlhoSaldo);

        textSaldo.setText(saldoOriginal);
        imgOlhoSaldo.setTag(true);

        imgOlhoSaldo.setOnClickListener(view -> {
            VisibilidadeHelper.alternarVisibilidadeSaldo(textSaldo, imgOlhoSaldo, saldoOriginal);
        });
    }

    private void configurarBotoesBloqueados() {
        // Criamos um listener único para economizar memória e código
        View.OnClickListener listenerBloqueio = view -> {
            Toast.makeText(ResumoVendasActivity.this, "Funcionalidade futura", Toast.LENGTH_SHORT).show();
        };

        // Lista com todos os IDs dos overlays que criamos no XML
        int[] idsBloqueados = {
                R.id.overlayStatusCaixa,
                R.id.overlayCatalogo,
                R.id.overlayEstoque,
                R.id.overlayDevolucoes,
                R.id.overlayRelatorios,
                R.id.overlayControleCaixa,
                R.id.overlayFinanceiro
        };

        // Loop que percorre a lista e aplica o click em cada um
        for (int id : idsBloqueados) {
            View overlay = findViewById(id);
            if (overlay != null) {
                overlay.setOnClickListener(listenerBloqueio);
            }
        }
    }

    public void acessarVendasCadastrosActivity(View view) {
        startActivity(new Intent(this, VendasCadastrosActivity.class));
        Log.i(TAG, "acessou RegistrarVendasActivity");
    }
    public void acessarRegistrarVendasActivity(View view) {
        startActivity(new Intent(this, RegistrarVendasActivity.class));
        Log.i(TAG, "acessou RegistrarVendasActivity");
    }
    public void acessarVendasEmAbertoActivity(View view) {
        startActivity(new Intent(this, VendasEmAbertoActivity.class));
        Log.i(TAG, "acessou VendasEmAbertoActivity");
    }

}