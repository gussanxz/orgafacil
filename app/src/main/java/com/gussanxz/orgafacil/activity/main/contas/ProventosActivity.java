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
        setContentView(R.layout.activity_main_contas_proventos);
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
                DatePickerHelper.showDatePickerDialog(ProventosActivity.this, campoData));

        campoHora.setText(TimePickerHelper.setHoraAtual());

        campoHora.setFocusable(false);
        campoHora.setClickable(true);
        campoHora.setOnClickListener(v ->
                TimePickerHelper.showTimePickerDialog(ProventosActivity.this, campoHora));

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

        // 🔹 NOVO: buscar no Firebase o último provento e oferecer para reaproveitar
        recuperarUltimoProventoDoFirebase();
    }

    public void salvarProventos(View view) {

        if (validarCamposProventos()) {

            movimentacao = new Movimentacao();
            String data = campoData.getText().toString();
            Double valorRecuperado = Double.parseDouble(campoValor.getText().toString());
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            movimentacao.setValor(valorRecuperado);
            movimentacao.setCategoria(campoCategoria.getText().toString());
            movimentacao.setDescricao(campoDescricao.getText().toString());
            movimentacao.setData(data);
            movimentacao.setHora(campoHora.getText().toString());
            movimentacao.setTipo("r");

            Double proventosAtualizada = proventosTotal + valorRecuperado;
            atualizarProventos(proventosAtualizada);

            movimentacao.salvar(uid, data, movimentacao);
            Toast.makeText(this, "Provento adicionado!", Toast.LENGTH_SHORT).show();

            finish();
        }
    }

    public void retornarPrincipal(View view) {
        startActivity(new Intent(this, ContasActivity.class));
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

        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

        usuarioRef.child("proventosTotal").setValue(proventos);

    }

    // =========================
    // 🔹 NOVO: BUSCAR ÚLTIMO PROVENTO NO FIREBASE E MOSTRAR POPUP
    // =========================

    /**
     * Busca no Firebase o último provento (tipo "r") do usuário
     * em todos os meses e, se existir, mostra um popup
     * perguntando se quer reaproveitar Categoria + Descrição.
     */
    private void recuperarUltimoProventoDoFirebase() {
        String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference movUsuarioRef = firebaseRef
                .child("movimentacao")
                .child(idUsuario);

        movUsuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                Movimentacao ultimoProvento = null;
                java.util.Date ultimaDataHora = null;

                // Nível 1: nós de mesAno (ex: "122025", "112025", etc.)
                for (DataSnapshot mesSnapshot : snapshot.getChildren()) {

                    // Nível 2: cada movimentação dentro daquele mesAno
                    for (DataSnapshot movSnapshot : mesSnapshot.getChildren()) {
                        Movimentacao m = movSnapshot.getValue(Movimentacao.class);
                        if (m != null && "r".equals(m.getTipo())) {

                            java.util.Date dataHoraMov = parseDataHora(m.getData(), m.getHora());

                            if (ultimoProvento == null) {
                                // primeiro provento encontrado
                                ultimoProvento = m;
                                ultimaDataHora = dataHoraMov;
                            } else {
                                if (dataHoraMov != null && ultimaDataHora != null) {
                                    if (dataHoraMov.after(ultimaDataHora)) {
                                        ultimoProvento = m;
                                        ultimaDataHora = dataHoraMov;
                                    }
                                }
                            }
                        }
                    }
                }

                if (ultimoProvento != null) {
                    mostrarPopupAproveitarUltimoProvento(ultimoProvento);
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

            // Mesmo formato usado nas movimentações: "dd/MM/yyyy" e "HH:mm"
            String texto = dataStr + " " + horaStr;
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            return sdf.parse(texto);
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarPopupAproveitarUltimoProvento(Movimentacao ultimo) {
        String categoria = ultimo.getCategoria();
        String descricao = ultimo.getDescricao();

        String categoriaLabel = TextUtils.isEmpty(categoria)
                ? "sem categoria"
                : categoria;

        String descricaoLabel = TextUtils.isEmpty(descricao)
                ? "sem descrição"
                : descricao;

        String mensagem = "Deseja aproveitar as informações do último provento?\n\n"
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