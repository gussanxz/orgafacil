package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.Categoria;
import com.gussanxz.orgafacil.data.repository.CategoriaRepository; // Importe o repositório criado

public class CadastroCategoriaActivity extends AppCompatActivity {

    // --- Componentes da Interface (UI) ---
    private GridLayout containerIcones;
    private LinearLayout layoutSelecao;
    private TextView textViewHeader;
    private ImageButton imgBtnSelecionarIcones;
    private TextInputEditText editNome, editDesc;
    private MaterialSwitch switchAtiva;

    // --- Lógica de Negócio ---
    private CategoriaRepository repository; // Nosso gerenciador de dados
    private String chaveCategoriaEdicao = null; // Guarda o ID se for edição
    private int iconeSelecionadoIndex = -1; // Guarda qual ícone foi clicado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_cadastro_categoria);

        // Ajuste visual para telas modernas (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializa o Repositório
        repository = new CategoriaRepository();

        // 2. Inicializa componentes visuais e cliques
        inicializarComponentes();

        // 3. Verifica se veio dados de outra tela para editar
        verificarEdicao();
    }

    private void inicializarComponentes() {
        // Vincula variáveis com o XML
        containerIcones = findViewById(R.id.containerIcones);
        layoutSelecao = findViewById(R.id.layoutSelecao);
        textViewHeader = findViewById(R.id.textViewHeader);
        imgBtnSelecionarIcones = findViewById(R.id.imgBtnSelecionarIcones);
        editNome = findViewById(R.id.editCategoria);
        editDesc = findViewById(R.id.editDescricao);
        switchAtiva = findViewById(R.id.switchAtiva);

        // Configura cliques dos botões de ação
        // OBS: Se você usa android:onClick no XML, mantenha os métodos públicos abaixo.
        // Se preferir java, use assim:
        findViewById(R.id.fabVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.fabSuperiorSalvarCategoria).setOnClickListener(this::salvarCategoria);

        // Configura o grid de ícones
        configurarSelecaoIcones();
    }

    /**
     * Método principal de SALVAR.
     * Note como não tem código Firebase aqui. Apenas validação e chamada ao Repo.
     */
    public void salvarCategoria(View view) {
        // 1. Validação de UI: Usuário escolheu ícone?
        if (iconeSelecionadoIndex == -1) {
            Toast.makeText(this, "Por favor, selecione um ícone.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validação de UI: Nome preenchido?
        String nome = editNome.getText().toString();
        if (nome.isEmpty()) {
            editNome.setError("Nome da categoria é obrigatório.");
            return;
        }

        // 3. Montagem do Objeto
        // Passamos null no ID do usuário pois o Repository já pega o UID atual
        Categoria categoria = new Categoria(
                null,
                nome,
                editDesc.getText().toString(),
                iconeSelecionadoIndex,
                switchAtiva.isChecked()
        );

        // Se for edição, injetamos o ID recuperado. Se for novo, chaveCategoriaEdicao é null.
        categoria.setId(chaveCategoriaEdicao);

        // 4. Chamada ao Repositório (Assíncrona)
        repository.salvar(categoria, new CategoriaRepository.CategoriaCallback() {
            @Override
            public void onSucesso(String mensagem) {
                // Deu tudo certo, avisa e fecha
                Toast.makeText(CadastroCategoriaActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                // Deu erro, avisa e mantém na tela
                Toast.makeText(CadastroCategoriaActivity.this, "Erro: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- MÉTODOS DE CONTROLE VISUAL (ÍCONES E CORES) ---

    // Configura o clique em cada card do GridLayout
    private void configurarSelecaoIcones() {
        if (containerIcones == null) return;
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            View view = containerIcones.getChildAt(i);
            final int index = i;
            if (view instanceof MaterialCardView) {
                view.setOnClickListener(v -> selecionarIconeVisualmente(index, (MaterialCardView) view));
            }
        }
    }

    // Atualiza a variável de controle e as cores dos cards
    private void selecionarIconeVisualmente(int index, MaterialCardView card) {
        iconeSelecionadoIndex = index;
        atualizarCoresDosCards(card);

        // Atualiza o ícone pequeno no botão de abrir seleção
        if (imgBtnSelecionarIcones != null)
            imgBtnSelecionarIcones.setImageResource(getIconePorIndex(index));
    }

    // Verifica se estamos editando uma categoria existente
    private void verificarEdicao() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("modoEditar")) {
            // Recupera dados
            chaveCategoriaEdicao = bundle.getString("idCategoria");
            editNome.setText(bundle.getString("nome"));
            editDesc.setText(bundle.getString("descricao"));
            switchAtiva.setChecked(bundle.getBoolean("ativa"));

            // Ajusta título
            textViewHeader.setText("Editar Categoria");

            // Seleciona o ícone salvo visualmente
            int index = bundle.getInt("iconeIndex");
            if (index >= 0 && containerIcones != null) {
                View view = containerIcones.getChildAt(index);
                if (view instanceof MaterialCardView) {
                    selecionarIconeVisualmente(index, (MaterialCardView) view);
                }
            }
        }
    }

    // Exibe/Esconde painel de ícones (chamado pelo botão na UI)
    public void exibeSelecaoDeIcones(View view) {
        esconderTeclado();
        layoutSelecao.setVisibility(layoutSelecao.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void esconderTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Pinta o card selecionado de verde e reseta os outros
    private void atualizarCoresDosCards(MaterialCardView cardClicado) {
        int verde = Color.parseColor("#25D366");
        int cinzaBg = Color.parseColor("#F5F5F5");
        int cinzaIcon = Color.parseColor("#9E9E9E");
        int borda = Color.parseColor("#E0E0E0");

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            View v = containerIcones.getChildAt(i);
            if (v instanceof MaterialCardView) {
                MaterialCardView c = (MaterialCardView) v;
                ImageView icon = (ImageView) c.getChildAt(0);

                boolean isSelected = (c == cardClicado);

                c.setCardBackgroundColor(isSelected ? verde : cinzaBg);
                c.setStrokeWidth(isSelected ? 0 : 3);
                c.setStrokeColor(borda);

                if(icon != null) icon.setColorFilter(isSelected ? Color.WHITE : cinzaIcon);
            }
        }
    }

    // Retorna o drawable correto baseado no índice
    private int getIconePorIndex(int index) {
        switch (index) {
            case 0: return R.drawable.ic_categorias_mercado_24;
            case 1: return R.drawable.ic_categorias_roupas_24;
            case 2: return R.drawable.ic_categorias_comida_24;
            case 3: return R.drawable.ic_categorias_bebidas_24;
            case 4: return R.drawable.ic_categorias_eletronicos_24;
            case 5: return R.drawable.ic_categorias_spa_24;
            case 6: return R.drawable.ic_categorias_fitness_24;
            case 7: return R.drawable.ic_categorias_geral_24;
            case 8: return R.drawable.ic_categorias_ferramentas_24;
            case 9: return R.drawable.ic_categorias_papelaria_24;
            case 10: return R.drawable.ic_categorias_casa_24;
            case 11: return R.drawable.ic_categorias_brinquedos_24;
            default: return R.drawable.ic_categorias_geral_24;
        }
    }
}