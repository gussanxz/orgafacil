package com.gussanxz.orgafacil.activity.main.contas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.gussanxz.orgafacil.config.ConfiguracaoFirebase;
import com.gussanxz.orgafacil.model.Movimentacao;
import com.gussanxz.orgafacil.model.Usuario;
import com.gussanxz.orgafacil.model.DatePickerHelper;
import com.gussanxz.orgafacil.model.TimePickerHelper;

public class DespesasActivity extends AppCompatActivity {

    private TextInputEditText campoData, campoDescricao, campoHora;
    private EditText campoValor, campoCategoria;
    private Movimentacao movimentacao;
    private DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
    private FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private Double despesaTotal;
    private ActivityResultLauncher<Intent> launcherCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_contas_despesas);
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
        campoData.setOnClickListener(v ->
                DatePickerHelper.showDatePickerDialog(DespesasActivity.this, campoData));

        campoHora.setText(TimePickerHelper.setHoraAtual());

        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v ->
                TimePickerHelper.showTimePickerDialog(DespesasActivity.this, campoHora));

        launcherCategoria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String categoria = result.getData().getStringExtra("categoriaSelecionada");
                        campoCategoria.setText(categoria);
                    }
                });
        
        campoCategoria.setOnClickListener(v -> {
            Intent intent = new Intent(DespesasActivity.this, SelecionarCategoriaActivity.class);
            launcherCategoria.launch(intent);
        });

        recuperarDespesaTotal();

        // 🔹 NOVO: buscar no Firebase a última despesa e oferecer para reaproveitar
        recuperarUltimaDespesaDoFirebase();
    }

    public void retornarPrincipal(View view){
        startActivity(new Intent(this, ContasActivity.class));
    }

    public void salvarDespesa(View view) {

        if (validarCamposDespesas()) {

            movimentacao = new Movimentacao();
            String data = campoData.getText().toString();
            Double valorRecuperado = Double.parseDouble(campoValor.getText().toString());

            movimentacao.setValor( valorRecuperado );
            movimentacao.setCategoria(campoCategoria.getText().toString());
            movimentacao.setDescricao(campoDescricao.getText().toString());
            movimentacao.setData(data);
            movimentacao.setHora(campoHora.getText().toString());
            movimentacao.setTipo("d");

            Double despesaAtualizada = despesaTotal + valorRecuperado;
            atualizarDespesa( despesaAtualizada );

            movimentacao.salvar(data);
            Toast.makeText(this, "Despesa adicionada!", Toast.LENGTH_SHORT).show();

            finish();
        }
    }

    public Boolean validarCamposDespesas() {

        String textoValor = campoValor.getText().toString();
        String textoData = campoData.getText().toString();
        String textoCategoria = campoCategoria.getText().toString();
        String textoDescricao = campoDescricao.getText().toString();

        if ( !textoValor.isEmpty()) {
            if ( !textoData.isEmpty()) {
                if ( !textoCategoria.isEmpty()) {
                    if ( !textoDescricao.isEmpty()) {

                        return true;

                    }else {
                        Toast.makeText(DespesasActivity.this,
                                "Descrição não foi preenchida!", Toast.LENGTH_SHORT).show();
                        return false;

                    }
                }else {
                    Toast.makeText(DespesasActivity.this,
                            "Categoria não foi preenchida!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }else {
                Toast.makeText(DespesasActivity.this,
                        "Data não foi preenchida!", Toast.LENGTH_SHORT).show();
                return false;

            }

        }else {
            Toast.makeText(DespesasActivity.this,
                    "Valor não foi preenchido!", Toast.LENGTH_SHORT).show();
            return false;

        }

    }

    public void recuperarDespesaTotal() {

        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child( idUsuario );

        usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Usuario usuario = dataSnapshot.getValue( Usuario.class );
                despesaTotal = usuario.getDespesaTotal();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void atualizarDespesa(Double despesa) {

        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        usuarioRef.child("despesaTotal").setValue(despesa);

    }

    // =========================
    // 🔹 NOVO: BUSCAR ÚLTIMA DESPESA NO FIREBASE E MOSTRAR POPUP
    // =========================
    private void recuperarUltimaDespesaDoFirebase() {
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Agora buscamos em movimentacao/idUsuario (todos os meses)
        DatabaseReference movUsuarioRef = firebaseRef
                .child("movimentacao")
                .child(idUsuario);

        movUsuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Movimentacao ultimaDespesa = null;
                java.util.Date ultimaDataHora = null;

                // Nível 1: nós de mesAno (ex: "122025", "112025" etc.)
                for (DataSnapshot mesSnapshot : snapshot.getChildren()) {

                    // Nível 2: cada movimentação dentro daquele mesAno
                    for (DataSnapshot movSnapshot : mesSnapshot.getChildren()) {
                        Movimentacao m = movSnapshot.getValue(Movimentacao.class);
                        if (m != null && "d".equals(m.getTipo())) {

                            java.util.Date dataHoraMov = parseDataHora(m.getData(), m.getHora());

                            if (ultimaDespesa == null) {
                                // primeira despesa encontrada
                                ultimaDespesa = m;
                                ultimaDataHora = dataHoraMov;
                            } else {
                                if (dataHoraMov != null && ultimaDataHora != null) {
                                    if (dataHoraMov.after(ultimaDataHora)) {
                                        ultimaDespesa = m;
                                        ultimaDataHora = dataHoraMov;
                                    }
                                } else {
                                    // se não conseguir parsear data/hora, usa última encontrada
                                    // ou se quiser, pode assumir essa como mais recente
                                }
                            }
                        }
                    }
                }

                if (ultimaDespesa != null) {
                    mostrarPopupAproveitarUltimaDespesa(ultimaDespesa);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Se der erro na leitura, apenas não mostra popup
            }
        });
    }

    private java.util.Date parseDataHora(String dataStr, String horaStr) {
        try {
            if (dataStr == null || dataStr.isEmpty()) return null;
            if (horaStr == null || horaStr.isEmpty()) horaStr = "00:00";

            // Seu formato de data/hora: "dd/MM/yyyy" e "HH:mm"
            String texto = dataStr + " " + horaStr;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            return sdf.parse(texto);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarPopupAproveitarUltimaDespesa(Movimentacao ultima) {
        String categoria = ultima.getCategoria();
        String descricao = ultima.getDescricao();

        String categoriaLabel = TextUtils.isEmpty(categoria)
                ? "sem categoria"
                : categoria;

        String descricaoLabel = TextUtils.isEmpty(descricao)
                ? "sem descrição"
                : descricao;

        String mensagem = "Deseja aproveitar as informações da última despesa?\n\n"
                + "Categoria: " + categoriaLabel + "\n"
                + "Descrição do produto ou serviço: " + descricaoLabel
                + "\n\nOu prefere começar do zero?";

        new AlertDialog.Builder(this)
                .setTitle("Aproveitar último lançamento")
                .setMessage(mensagem)
                .setPositiveButton("Aproveitar", (dialog, which) -> {
                    if (!TextUtils.isEmpty(categoria)) {
                        campoCategoria.setText(categoria);
                    }
                    if (!TextUtils.isEmpty(descricao)) {
                        campoDescricao.setText(descricao);
                    }
                })
                .setNegativeButton("Começar do zero", null)
                .show();
    }
}