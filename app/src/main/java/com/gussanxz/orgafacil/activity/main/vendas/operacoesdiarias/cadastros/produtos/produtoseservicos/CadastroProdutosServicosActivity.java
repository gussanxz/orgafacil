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

// Se você tiver a classe ItemVenda criada, descomente a linha abaixo:
// import com.gussanxz.orgafacil.model.ItemVenda;

public class CadastroProdutosServicosActivity extends AppCompatActivity {

    // Constantes para controle
    private static final int TIPO_PRODUTO = 1;
    private static final int TIPO_SERVICO = 2;

    // Elementos da Interface
    private FloatingActionButton fabVoltar;
    private FloatingActionButton fabSuperiorSalvarCategoria;
    private FloatingActionButton fabInferiorSalvarCategoria;

    private MaterialButtonToggleGroup toggleTipoCadastro;
    private TextInputLayout textInputCategoria, textInputDescricao, textInputValor;
    private TextView textViewHeader;

    // Variável para saber o que estamos salvando (Começa como Produto)
    private int tipoSelecionado = TIPO_PRODUTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Verifique se o nome do layout abaixo está correto com o nome do arquivo XML criado
        setContentView(R.layout.activity_main_vendas_operacoesdiarias_cadastros_produtos_produtos_e_servicos_cadastro_produtos_servicos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();

        // Configura o comportamento do clique nos botões de Produto/Serviço
        configurarToggleListener();

        // Garante que a tela comece com os textos de "Produto"
        configurarModoProduto();
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

        // Altera as dicas (Hints) dos campos
        textInputCategoria.setHint("Categoria do Produto");
        textInputDescricao.setHint("Nome do Produto");
        textInputValor.setHint("Preço de Venda");
    }

    private void configurarModoServico() {
        tipoSelecionado = TIPO_SERVICO;

        textViewHeader.setText("Novo Serviço");

        // Altera as dicas (Hints) dos campos
        textInputCategoria.setHint("Categoria do Serviço");
        textInputDescricao.setHint("Descrição do Serviço");
        textInputValor.setHint("Valor do Serviço");
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }

    public void salvarProdutoOuServico(View view) {
        String mensagem;

        if (tipoSelecionado == TIPO_PRODUTO) {
            // Lógica para salvar Produto
            // ItemVenda produto = new ItemVenda(..., TIPO_PRODUTO);
            mensagem = "Produto salvo com sucesso!";
        } else {
            // Lógica para salvar Serviço
            // ItemVenda servico = new ItemVenda(..., TIPO_SERVICO);
            mensagem = "Serviço salvo com sucesso!";
        }

        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        finish();
    }
}