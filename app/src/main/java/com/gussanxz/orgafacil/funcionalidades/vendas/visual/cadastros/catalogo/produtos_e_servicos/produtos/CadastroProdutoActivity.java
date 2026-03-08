package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.produtos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ProdutoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.ProdutoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria.SelecionarCategoriaVendasActivity;

public class CadastroProdutoActivity extends AppCompatActivity {

    private FloatingActionButton fabVoltar, fabSalvar;
    private TextInputLayout textInputCategoria, textInputNome, textInputPreco;
    // Futuro: private TextInputLayout textInputEstoque, textInputCodigoBarras;
    private TextView textViewHeader;

    private ProdutoRepository repository;
    private String idEmEdicao = null;
    private String categoriaIdSelecionada = null;
    private String categoriaNomeSelecionada = null;
    private MaterialSwitch switchStatusAtivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_produto); // Crie um layout só pra produto

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ProdutoRepository();
        inicializarComponentes();
        configurarCampoCategoria();

        // Configura Listener
        fabSalvar.setOnClickListener(this::salvarProduto);
        fabVoltar.setOnClickListener(v -> finish());

        // Verifica Edição
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            prepararEdicao(extras);
        }
    }

    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSalvar = findViewById(R.id.fabSalvar); // Um botão só, fixo
        textInputCategoria = findViewById(R.id.textInputCategoria);
        textInputNome = findViewById(R.id.textInputNome);
        textInputPreco = findViewById(R.id.textInputPreco);
        textViewHeader = findViewById(R.id.textViewHeader);

        textViewHeader.setText("Novo Produto");

        switchStatusAtivo = findViewById(R.id.switchStatusAtivo);
        switchStatusAtivo.setChecked(true);
    }

    private void configurarCampoCategoria() {
        if (textInputCategoria.getEditText() != null) {
            textInputCategoria.getEditText().setFocusable(false);
            textInputCategoria.getEditText().setClickable(true);
            textInputCategoria.getEditText().setCursorVisible(false);
            textInputCategoria.getEditText().setKeyListener(null);

            textInputCategoria.getEditText().setOnClickListener(v -> abrirSelecaoCategoria());
        }

        textInputCategoria.setOnClickListener(v -> abrirSelecaoCategoria());
        textInputCategoria.setEndIconOnClickListener(v -> abrirSelecaoCategoria());
    }

    private void abrirSelecaoCategoria() {
        Intent intent = new Intent(this, SelecionarCategoriaVendasActivity.class);
        selecionarCategoriaLauncher.launch(intent);
    }

    private void prepararEdicao(Bundle dados) {
        textViewHeader.setText("Editar Produto");
        idEmEdicao = dados.getString("id");

        if(textInputNome.getEditText() != null)
            textInputNome.getEditText().setText(dados.getString("nome"));

        categoriaIdSelecionada = dados.getString("categoriaId");
        categoriaNomeSelecionada = dados.getString("categoria");

        if (textInputCategoria.getEditText() != null) {
            textInputCategoria.getEditText().setText(categoriaNomeSelecionada != null ? categoriaNomeSelecionada : "");
        }

        if(textInputPreco.getEditText() != null)
            textInputPreco.getEditText().setText(String.valueOf(dados.getDouble("preco")));
    }

    public void salvarProduto(View view) {
        // Validações
        String nome = textInputNome.getEditText().getText().toString();
        String precoStr = textInputPreco.getEditText().getText().toString();

        if (nome.isEmpty()) { textInputNome.setError("Obrigatório"); return; }
        if (precoStr.isEmpty()) { textInputPreco.setError("Obrigatório"); return; }

        double preco = Double.parseDouble(precoStr.replace(",", "."));

        // Criação do Objeto
        ProdutoModel produtoModel = new ProdutoModel();
        produtoModel.setId(idEmEdicao);
        produtoModel.setNome(nome);
        produtoModel.setCategoriaId(categoriaIdSelecionada);
        produtoModel.setCategoria(categoriaNomeSelecionada);
        produtoModel.setPreco(preco);

        // Salvar
        repository.salvar(produtoModel, new ProdutoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Toast.makeText(CadastroProdutoActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(CadastroProdutoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> selecionarCategoriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    categoriaIdSelecionada = data.getStringExtra("idCategoria");
                    categoriaNomeSelecionada = data.getStringExtra("nomeCategoria");

                    if (textInputCategoria.getEditText() != null) {
                        textInputCategoria.getEditText().setText(categoriaNomeSelecionada != null ? categoriaNomeSelecionada : "");
                    }
                }
            });
}