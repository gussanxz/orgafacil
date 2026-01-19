package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtoseservicos;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.gussanxz.orgafacil.R;

public class CadastroProdutosServicosActivity extends AppCompatActivity {

    private static final int TIPO_PRODUTO = 1;
    private static final int TIPO_SERVICO = 2;

    private FloatingActionButton fabVoltar;
    private FloatingActionButton fabSuperiorSalvarCategoria;
    private FloatingActionButton fabInferiorSalvarCategoria;

    private MaterialButtonToggleGroup toggleTipoCadastro;
    private TextInputLayout textInputCategoria, textInputDescricao, textInputValor;
    private TextView textViewHeader;

    private int tipoSelecionado = TIPO_PRODUTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_produtos_e_servicos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        // --- LÓGICA NOVA AQUI ---
        // Verifica se veio da lista (Modo Edição)
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("modo_edicao", false)) {
            // Se for edição, prepara a tela para editar
            prepararModoEdicao(extras);
        } else {
            // Se for novo, prepara a tela padrão (com toggle)
            configurarToggleListener();
            configurarModoProduto(); // Padrão
        }
    }

    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSuperiorSalvarCategoria = findViewById(R.id.fabSuperiorSalvarCategoria);
        fabInferiorSalvarCategoria = findViewById(R.id.fabInferiorSalvarCategoria);
        toggleTipoCadastro = findViewById(R.id.toggleTipoCadastro);
        textInputCategoria = findViewById(R.id.textInputCategoria);
        textInputDescricao = findViewById(R.id.textInputDescricao);
        textInputValor = findViewById(R.id.textInputValor);
        textViewHeader = findViewById(R.id.textViewHeader);
    }

    // --- LÓGICA DE EDIÇÃO ---
    private void prepararModoEdicao(Bundle dados) {
        // 1. Esconde o Toggle (Não pode mudar o tipo na edição)
        toggleTipoCadastro.setVisibility(View.GONE);

        // 2. Recupera os dados
        int tipo = dados.getInt("tipo");
        String nome = dados.getString("nome");
        String descricao = dados.getString("descricao"); // Usamos descricao como categoria as vezes, ajuste conforme seu uso
        double preco = dados.getDouble("preco");

        // 3. Atualiza a variável global para saber o que estamos salvando
        this.tipoSelecionado = tipo;

        // 4. Preenche os campos com os dados existentes
        // (Nota: getEditText() pode ser null, ideal verificar, mas direto assim funciona na maioria dos casos)
        if(textInputDescricao.getEditText() != null) textInputDescricao.getEditText().setText(nome);
        // Assumindo que o campo categoria recebe a descrição ou vice-versa, ajuste conforme sua lógica de Intent
        if(textInputCategoria.getEditText() != null) textInputCategoria.getEditText().setText(descricao);
        if(textInputValor.getEditText() != null) textInputValor.getEditText().setText(String.valueOf(preco));

        // 5. Ajusta Textos e Hints baseado no Tipo
        if (tipo == TIPO_PRODUTO) {
            textViewHeader.setText("Editar Produto");
            textInputCategoria.setHint("Categoria do Produto");
            textInputDescricao.setHint("Nome do Produto");
            textInputValor.setHint("Preço de Venda");
        } else {
            textViewHeader.setText("Editar Serviço");
            textInputCategoria.setHint("Categoria do Serviço");
            textInputDescricao.setHint("Descrição do Serviço");
            textInputValor.setHint("Valor do Serviço");
        }
    }

    // --- LÓGICA DE NOVO CADASTRO ---
    private void configuringToggleListener() { /* ... */ } // Mantido abaixo para clareza

    private void configurarToggleListener() {
        toggleTipoCadastro.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTipoProduto) {
                    configurarModoProduto();
                } else if (checkedId == R.id.btnTipoServico) {
                    configurarModoServico();
                }
            }
        });
    }

    private void configurarModoProduto() {
        tipoSelecionado = TIPO_PRODUTO;
        textViewHeader.setText("Novo Produto");
        textInputCategoria.setHint("Categoria do Produto");
        textInputDescricao.setHint("Nome do Produto");
        textInputValor.setHint("Preço de Venda");
    }

    private void configurarModoServico() {
        tipoSelecionado = TIPO_SERVICO;
        textViewHeader.setText("Novo Serviço");
        textInputCategoria.setHint("Categoria do Serviço");
        textInputDescricao.setHint("Descrição do Serviço");
        textInputValor.setHint("Valor do Serviço");
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }

    public void salvarProdutoOuServico(View view) {
        String acao = (toggleTipoCadastro.getVisibility() == View.GONE) ? "editado" : "salvo";
        String tipo = (tipoSelecionado == TIPO_PRODUTO) ? "Produto" : "Serviço";

        Toast.makeText(this, tipo + " " + acao + " com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }
}