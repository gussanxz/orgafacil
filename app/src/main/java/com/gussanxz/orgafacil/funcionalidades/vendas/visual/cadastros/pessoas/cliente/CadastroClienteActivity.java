package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.pessoas.cliente;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.dados.RepoVoidCallback;
import com.gussanxz.orgafacil.funcionalidades.firebase.FirestoreSchema;
import com.gussanxz.orgafacil.funcionalidades.vendas.ResumoVendasActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.VendasRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.historico.HistoricoVendasActivity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CadastroClienteActivity extends AppCompatActivity {

    private TextInputLayout textInputNome;
    private TextInputLayout textInputNumero;
    private TextInputLayout textInputEndereco;
    private TextInputEditText editNomeCliente;
    private TextInputEditText editTelefoneCliente;
    private TextInputEditText editEmailCliente;
    private TextInputEditText editEnderecoCliente;
    private TextView txtPreviewEndereco;
    private LinearLayout cardPreviewEndereco;
    private LinearLayout btnAbrirWhatsapp;
    private LinearLayout btnHistoricoCliente;

    private final VendasRepository vendasRepository = new VendasRepository();
    private String clienteIdSalvo;
    private String clienteNomeSalvo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_cliente);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularViews();
        configurarAcoes();
    }

    private void vincularViews() {
        textInputNome = findViewById(R.id.textInputNome);
        textInputNumero = findViewById(R.id.textInputNumero);
        textInputEndereco = findViewById(R.id.textInputEndereco);
        editNomeCliente = findViewById(R.id.editNomeCliente);
        editTelefoneCliente = findViewById(R.id.editTelefoneCliente);
        editEmailCliente = findViewById(R.id.editEmailCliente);
        editEnderecoCliente = findViewById(R.id.editEnderecoCliente);
        txtPreviewEndereco = findViewById(R.id.txtPreviewEndereco);
        cardPreviewEndereco = findViewById(R.id.cardPreviewEndereco);
        btnAbrirWhatsapp = findViewById(R.id.btnAbrirWhatsapp);
        btnHistoricoCliente = findViewById(R.id.btnHistoricoCliente);
    }

    private void configurarAcoes() {
        if (btnAbrirWhatsapp != null) {
            btnAbrirWhatsapp.setOnClickListener(v -> abrirWhatsapp());
        }
        if (cardPreviewEndereco != null) {
            cardPreviewEndereco.setOnClickListener(v -> abrirOpcoesEntrega());
        }
        if (btnHistoricoCliente != null) {
            btnHistoricoCliente.setOnClickListener(v -> abrirHistoricoConsumo());
        }
        if (editEnderecoCliente != null) {
            editEnderecoCliente.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) atualizarPreviewEndereco();
            });
        }
    }

    public void retornarResumoVendas(View view) {
        startActivity(new Intent(this, ResumoVendasActivity.class));
        finish();
    }

    public void confirmarCadastroCliente(View view) {
        limparErros();

        String nome = obterTexto(editNomeCliente);
        String telefone = obterTexto(editTelefoneCliente);
        String email = obterTexto(editEmailCliente);
        String endereco = obterTexto(editEnderecoCliente);

        if (nome.isEmpty()) {
            textInputNome.setError("Informe o nome do cliente");
            return;
        }
        if (telefone.isEmpty()) {
            textInputNumero.setError("Informe um telefone para entrega");
            return;
        }

        String clienteId = clienteIdSalvo != null
                ? clienteIdSalvo
                : FirestoreSchema.vendasClientesCol().document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("nome", nome);
        data.put("telefone", telefone);
        data.put("telefoneWhatsapp", normalizarTelefoneWhatsapp(telefone));
        data.put("email", email);
        data.put("endereco", endereco);
        data.put("statusAtivo", true);

        vendasRepository.salvarCliente(clienteId, data, new RepoVoidCallback() {
            @Override
            public void onSuccess() {
                clienteIdSalvo = clienteId;
                clienteNomeSalvo = nome;
                atualizarPreviewEndereco();
                Toast.makeText(CadastroClienteActivity.this,
                        "Cliente salvo.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CadastroClienteActivity.this,
                        "Erro ao salvar cliente: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirWhatsapp() {
        String telefone = normalizarTelefoneWhatsapp(obterTexto(editTelefoneCliente));
        if (telefone.isEmpty()) {
            textInputNumero.setError("Informe o telefone");
            return;
        }
        abrirUri("https://wa.me/" + telefone);
    }

    private void abrirOpcoesEntrega() {
        atualizarPreviewEndereco();
        String endereco = obterTexto(editEnderecoCliente);
        if (endereco.isEmpty()) {
            textInputEndereco.setError("Informe o endereço");
            return;
        }

        String[] opcoes = {"Google Maps", "Uber", "99"};
        new AlertDialog.Builder(this)
                .setTitle("Abrir endereço")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) abrirMaps(endereco);
                    else if (which == 1) abrirUber(endereco);
                    else abrir99(endereco);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirHistoricoConsumo() {
        if (clienteIdSalvo == null || clienteIdSalvo.trim().isEmpty()) {
            Toast.makeText(this,
                    "Salve o cliente antes de abrir o histórico.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, HistoricoVendasActivity.class);
        intent.putExtra("clienteFiltroId", clienteIdSalvo);
        intent.putExtra("clienteFiltroNome", clienteNomeSalvo);
        startActivity(intent);
    }

    private void atualizarPreviewEndereco() {
        if (txtPreviewEndereco == null) return;
        String endereco = obterTexto(editEnderecoCliente);
        txtPreviewEndereco.setText(endereco.isEmpty()
                ? "Informe o endereço para abrir no Maps, Uber ou 99"
                : endereco);
    }

    private void abrirMaps(String endereco) {
        abrirUri("geo:0,0?q=" + encode(endereco));
    }

    private void abrirUber(String endereco) {
        abrirUri("https://m.uber.com/ul/?action=setPickup&dropoff[formatted_address]=" + encode(endereco));
    }

    private void abrir99(String endereco) {
        abrirUri("https://www.google.com/search?q=99%20app%20" + encode(endereco));
    }

    private void abrirUri(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Nenhum app encontrado para abrir.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void limparErros() {
        if (textInputNome != null) textInputNome.setError(null);
        if (textInputNumero != null) textInputNumero.setError(null);
        if (textInputEndereco != null) textInputEndereco.setError(null);
    }

    private String obterTexto(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String normalizarTelefoneWhatsapp(String telefone) {
        String apenasDigitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (apenasDigitos.isEmpty()) return "";
        if (apenasDigitos.startsWith("55")) return apenasDigitos;
        return "55" + apenasDigitos;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
