package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.produtos_e_servicos.servicos;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.Servico;
import com.gussanxz.orgafacil.data.repository.ServicoRepository;

public class CadastroServicoActivity extends AppCompatActivity {

    // Componentes UI
    private FloatingActionButton fabVoltar, fabSalvar;
    private TextInputLayout textInputCategoria, textInputDescricao, textInputValor;
    // Futuro: private TextInputLayout textInputTempoDuracao;
    private TextView textViewHeader;

    // Lógica
    private ServicoRepository repository;
    private String idEmEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_servico); // Layout específico para Serviço

        // Ajuste de Insets (Barra de status)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializa Repositório
        repository = new ServicoRepository();

        // 2. Inicializa UI
        inicializarComponentes();

        // 3. Configura Listeners (Cliques)
        fabVoltar.setOnClickListener(v -> finish());
        fabSalvar.setOnClickListener(this::salvarServico);

        // 4. Verifica se é Edição
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            prepararEdicao(extras);
        }
    }

    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSalvar = findViewById(R.id.fabSalvar);

        // IDs devem bater com o novo XML de serviço
        textInputCategoria = findViewById(R.id.textInputCategoria);
        textInputDescricao = findViewById(R.id.textInputDescricao);
        textInputValor = findViewById(R.id.textInputValor);
        textViewHeader = findViewById(R.id.textViewHeader);

        textViewHeader.setText("Novo Serviço");
    }

    private void prepararEdicao(Bundle dados) {
        textViewHeader.setText("Editar Serviço");
        idEmEdicao = dados.getString("id");

        // Preenche campos com segurança (verifica null)
        if(textInputDescricao.getEditText() != null)
            textInputDescricao.getEditText().setText(dados.getString("nome")); // ou "descricao", dependendo de como enviou na Intent

        if(textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setText(dados.getString("categoria"));

        if(textInputValor.getEditText() != null)
            textInputValor.getEditText().setText(String.valueOf(dados.getDouble("preco"))); // ou "valor"
    }

    public void salvarServico(View view) {
        // 1. Coleta dados
        String descricao = "";
        String categoria = "";
        String valorStr = "";

        if(textInputDescricao.getEditText() != null) descricao = textInputDescricao.getEditText().getText().toString();
        if(textInputCategoria.getEditText() != null) categoria = textInputCategoria.getEditText().getText().toString();
        if(textInputValor.getEditText() != null) valorStr = textInputValor.getEditText().getText().toString();

        // 2. Validações
        if (descricao.isEmpty()) { textInputDescricao.setError("Descrição obrigatória"); return; }
        if (valorStr.isEmpty()) { textInputValor.setError("Valor obrigatório"); return; }

        // 3. Conversão de valor
        double valor = 0.0;
        try {
            valor = Double.parseDouble(valorStr.replace(",", "."));
        } catch (NumberFormatException e) {
            textInputValor.setError("Valor inválido");
            return;
        }

        // 4. Cria Objeto
        Servico servico = new Servico();
        servico.setId(idEmEdicao); // Se null cria novo, se tem ID atualiza
        servico.setDescricao(descricao);
        servico.setCategoria(categoria);
        servico.setValor(valor);

        // 5. Chama Repositório
        repository.salvar(servico, new ServicoRepository.Callback() {
            @Override
            public void onSucesso(String mensagem) {
                Toast.makeText(CadastroServicoActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(CadastroServicoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}