package com.gussanxz.orgafacil.activity.main.configuracoes;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.gussanxz.orgafacil.R;

public class PerfilActivity extends AppCompatActivity {

    private TextView textNomePerfil, textEmailPerfil;
    private ImageView imagePerfilUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // IMPORTANTE: O nome do layout deve ser o mesmo do arquivo XML que criamos
        setContentView(R.layout.activity_perfil);

        inicializarComponentes();
        recuperarDadosUsuario();
    }

    private void inicializarComponentes() {
        textNomePerfil = findViewById(R.id.textNomePerfil);
        textEmailPerfil = findViewById(R.id.textEmailPerfil);
        imagePerfilUsuario = findViewById(R.id.imagePerfilUsuario);
    }

    private void recuperarDadosUsuario() {
        // Recupera a Intent enviada pela ConfigsActivity
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            // 1. Recupera as Strings
            String nome = bundle.getString("nomeUsuario");
            String email = bundle.getString("emailUsuario");
            String fotoUrl = bundle.getString("fotoUsuario");

            // 2. Define nos TextViews
            if (nome != null) {
                textNomePerfil.setText(nome);
            } else {
                textNomePerfil.setText("Usuário sem nome");
            }

            if (email != null) {
                textEmailPerfil.setText(email);
            }

            // 3. Carrega a Foto usando Glide
            if (fotoUrl != null) {
                Glide.with(this)
                        .load(fotoUrl)
                        .circleCrop() // Deixa redonda automaticamente
                        .placeholder(R.drawable.ic_person_24) // Imagem enquanto carrega
                        .into(imagePerfilUsuario);
            } else {
                // Se não tiver foto, garante que mostra o ícone padrão
                imagePerfilUsuario.setImageResource(R.drawable.ic_person_24);
            }
        } else {
            Toast.makeText(this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
        }
    }
}