package com.gussanxz.orgafacil.activity.vendas;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.contas.ContasActivity;
import com.gussanxz.orgafacil.activity.vendas.VendasCadastrosActivity;
import com.gussanxz.orgafacil.helper.VisibilidadeHelper;

public class ResumoVendasActivity extends AppCompatActivity {
    private final String TAG = "ResumoVendasActivity";
    private String saldoOriginal = "R$ 150,00"; //ISSO AQUI EH PRA SER O VALOR PUXADO DO FIREBASE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resumo_vendas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Encontrar componentes na tela/xml
        TextView textSaldo = findViewById(R.id.textSaldo);
        ImageView imgOlhoSaldo = findViewById(R.id.imgOlhoSaldo);

        //Definir estado inicial
        textSaldo.setText(saldoOriginal); //VAi puxar o valor atual de vendas do dia
        imgOlhoSaldo.setTag(true); //vamos definir que a imgOlho eh o de visivel TRUE

        //Chamada do helper
        imgOlhoSaldo.setOnClickListener (view -> {
            VisibilidadeHelper.alternarVisibilidadeSaldo(textSaldo, imgOlhoSaldo, saldoOriginal); //passa dos parametros para o helper
        });

    }


    public void acessarVendasCadastrosActivity(View view) {
        startActivity(new Intent(this, VendasCadastrosActivity.class));
        Log.i(TAG, "acessou ContasActivity");
    }



}