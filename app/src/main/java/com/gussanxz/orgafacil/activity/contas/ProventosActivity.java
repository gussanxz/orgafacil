package com.gussanxz.orgafacil.activity.contas;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.activity.SelecionarCategoriaActivity;
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.model.DatePickerHelper;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.TimePickerHelper;
import com.gussanxz.orgafacil.model.Usuario;

public class ProventosActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private Movimentacao movimentacao;
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private Double proventosTotal;

    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_proventos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        campoValor = findViewById(R.id.editValor);
        campoData = findViewById(R.id.editData);
        campoCategoria = findViewById(R.id.editCategoria);
        campoDescricao = findViewById(R.id.editDescricao);
        campoHora = findViewById(R.id.editHora);

        campoData.setText(DatePickerHelper.setDataAtual());

        campoData.setFocusable(false);
        campoData.setClickable(true);

        campoData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerHelper.showDatePickerDialog(ProventosActivity.this, campoData);
            }
        });

        campoHora.setText(TimePickerHelper.setHoraAtual());

        campoHora.setFocusable(false);
        campoHora.setClickable(true);

        campoHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerHelper.showTimePickerDialog(ProventosActivity.this, campoHora);
            }
        });

        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });

        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(ProventosActivity.this, SelecionarCategoriaActivity.class);
            launcherCategoria.launch(intent);
        });

        recuperarProventosTotal();

    }

    public void salvarProventos(View view) {

        if (validarCamposProventos()) {

            movimentacao = new Movimentacao();
            String data = campoData.getText().toString();
            Double valorRecuperado = Double.parseDouble(campoValor.getText().toString());

            movimentacao.setValor(valorRecuperado);
            movimentacao.setCategoria(campoCategoria.getText().toString());
            movimentacao.setDescricao(campoDescricao.getText().toString());
            movimentacao.setData(data);
            movimentacao.setHora(campoHora.getText().toString());
            movimentacao.setTipo("r");

            Double proventosAtualizada = proventosTotal + valorRecuperado;
            atualizarProventos(proventosAtualizada);

            movimentacao.salvar(data);
            Toast.makeText(this, "Proventos adicionada!", Toast.LENGTH_SHORT).show();

            finish();
        }
    }

    public void retornarPrincipal(View view) {
        startActivity(new Intent(this, PrincipalActivity.class));
    }

    public Boolean validarCamposProventos() {

        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if (!textoValor.isEmpty()) {
            if (!textoData.isEmpty()) {
                if (!textoCategoria.isEmpty()) {
                    if (!textoDescricao.isEmpty()) {

                        return true;

                    } else {
                        Toast.makeText(ProventosActivity.this,
                                "Descrição não foi preenchida!", Toast.LENGTH_SHORT).show();
                        return false;

                    }
                } else {
                    Toast.makeText(ProventosActivity.this,
                            "Categoria não foi preenchida!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(ProventosActivity.this,
                        "Data não foi preenchida!", Toast.LENGTH_SHORT).show();
                return false;

            }

        } else {
            Toast.makeText(ProventosActivity.this,
                    "Valor não foi preenchido!", Toast.LENGTH_SHORT).show();
            return false;

        }

    }

    public void recuperarProventosTotal() {

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Usuario usuario = dataSnapshot.getValue(Usuario.class);
                proventosTotal = usuario.getProventosTotal();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void atualizarProventos(Double proventos) {

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        usuarioRef.child("proventosTotal").setValue(proventos);

    }

}