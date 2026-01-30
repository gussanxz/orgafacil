package com.gussanxz.orgafacil.features.vendas.cadastros.catalogo.categoria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.ui.contas.categorias.CadastroCategoriaViewModel;

public class CadastroCategoriaActivity extends AppCompatActivity {

    // --- Componentes da Interface (UI) ---
    private GridLayout containerIcones;
    private LinearLayout layoutSelecao;
    private TextView textViewHeader;

    // Botões de Visual
    private ImageButton imgBtnSelecionarIcones;
    private ImageView imgBtnGaleria;
    private MaterialCardView cardBtnGaleria;

    // Inputs
    private TextInputEditText editNome, editDesc;
    private MaterialSwitch switchAtiva;

    // --- ViewModel (Lógica) ---
    private CadastroCategoriaViewModel viewModel;

    // --- Launcher para pegar foto da Galeria ---
    private final ActivityResultLauncher<Intent> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imagemUri = result.getData().getData();
                    // Passa a uri para o ViewModel processar
                    viewModel.selecionarFoto(imagemUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_cadastro_categoria);

        ajustarInsets();

        // 1. Inicializa o ViewModel
        viewModel = new ViewModelProvider(this).get(CadastroCategoriaViewModel.class);

        // 2. Inicializa Views e Cliques
        inicializarComponentes();

        // 3. Configura Contexto (Receita/Despesa/etc) e Edição
        processarIntentInicial();

        // 4. Observa as mudanças de estado (A Mágica do MVVM)
        observarViewModel();
    }

    private void ajustarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void inicializarComponentes() {
        // Vincula IDs
        containerIcones = findViewById(R.id.containerIcones);
        layoutSelecao = findViewById(R.id.layoutSelecao);
        textViewHeader = findViewById(R.id.textViewHeader);

        imgBtnSelecionarIcones = findViewById(R.id.imgBtnSelecionarIcones);
        imgBtnGaleria = findViewById(R.id.imgBtnGaleria);
        cardBtnGaleria = findViewById(R.id.cardBtnGaleria);

        editNome = findViewById(R.id.editCategoria);
        editDesc = findViewById(R.id.editDescricao);
        switchAtiva = findViewById(R.id.switchAtiva);

        // Configura Cliques dos Botões Principais
        findViewById(R.id.fabVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.fabSuperiorSalvarCategoria).setOnClickListener(this::salvarCategoria);
        findViewById(R.id.fabInferiorSalvarCategoria).setOnClickListener(this::salvarCategoria);

        // Clique para abrir Galeria
        if (cardBtnGaleria != null) {
            cardBtnGaleria.setOnClickListener(v -> abrirGaleria());
        }

        // Configura cliques do Grid de Ícones
        configurarCliquesGridIcones();
    }

    private void processarIntentInicial() {
        // Define o Tipo (Receita, Despesa...)
        viewModel.definirContexto(getIntent().getStringExtra("tipo"));
        atualizarTituloHeader();

        // Verifica se é Edição
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("modoEditar")) {
            editNome.setText(bundle.getString("nome"));
            editDesc.setText(bundle.getString("descricao"));
            switchAtiva.setChecked(bundle.getBoolean("ativa"));

            // Passa dados visuais para o VM
            viewModel.carregarDadosEdicao(
                    bundle.getString("idCategoria"),
                    bundle.getString("nome"),
                    bundle.getString("descricao"),
                    bundle.getBoolean("ativa"),
                    bundle.getString("urlImagem"),
                    bundle.getInt("iconeIndex", 0)
            );

            textViewHeader.setText("Editar " + obterNomeTipoBonito());
        }
    }

    // --- PADRÃO OBSERVER (Atualiza a UI quando o ViewModel mudar) ---
    private void observarViewModel() {

        // Quando o ÍCONE mudar
        viewModel.iconeSelecionado.observe(this, index -> {
            if (index != -1) {
                atualizarVisualGridIcones(index);
                resetarVisualGaleria();
            }
        });

        // Quando a FOTO mudar
        viewModel.imagemUri.observe(this, uri -> {
            if (uri != null) {
                atualizarVisualGaleria(uri);
                resetarVisualGridIcones();
            }
        });

        // Sucesso
        viewModel.mensagemSucesso.observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            finish();
        });

        // Erro
        viewModel.mensagemErro.observe(this, erro -> {
            if (erro.toLowerCase().contains("nome")) {
                editNome.setError(erro);
            } else {
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- AÇÕES DE UI ---

    public void salvarCategoria(View view) {
        viewModel.salvar(
                editNome.getText().toString(),
                editDesc.getText().toString(),
                switchAtiva.isChecked()
        );
    }

    public void exibeSelecaoDeIcones(View view) {
        esconderTeclado();
        layoutSelecao.setVisibility(layoutSelecao.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcherGaleria.launch(intent);
    }

    // --- MÉTODOS DE PINTURA E VISUAL ---

    private void atualizarTituloHeader() {
        textViewHeader.setText("Nova " + obterNomeTipoBonito());
    }

    private String obterNomeTipoBonito() {
        switch (viewModel.getTipoCategoria()) {
            case RECEITA: return "Receita";
            case PRODUTO: return "Produto";
            case SERVICO: return "Serviço";
            default: return "Despesa";
        }
    }

    private void atualizarVisualGaleria(Uri uri) {
        imgBtnGaleria.setImageURI(uri);
        imgBtnGaleria.setColorFilter(null); // Tira o cinza
        imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cardBtnGaleria.setStrokeColor(Color.parseColor("#2196F3")); // Azul
        cardBtnGaleria.setStrokeWidth(4);

        // Fecha o painel de ícones se estiver aberto
        layoutSelecao.setVisibility(View.GONE);
    }

    private void resetarVisualGaleria() {
        imgBtnGaleria.setImageResource(R.drawable.ic_launcher_foreground); // Ícone padrão placeholder
        imgBtnGaleria.setColorFilter(Color.parseColor("#757575"));
        imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        cardBtnGaleria.setStrokeColor(Color.parseColor("#E0E0E0"));
        cardBtnGaleria.setStrokeWidth(1);
    }

    private void atualizarVisualGridIcones(int indexSelecionado) {
        int verde = Color.parseColor("#25D366");
        int cinzaBg = Color.parseColor("#F5F5F5");
        int cinzaIcon = Color.parseColor("#9E9E9E");
        int borda = Color.parseColor("#E0E0E0");

        if (containerIcones == null) return;

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            View v = containerIcones.getChildAt(i);
            if (v instanceof MaterialCardView) {
                MaterialCardView c = (MaterialCardView) v;
                ImageView icon = (ImageView) c.getChildAt(0);

                boolean isSelected = (i == indexSelecionado);

                c.setCardBackgroundColor(isSelected ? verde : cinzaBg);
                c.setStrokeWidth(isSelected ? 0 : 3);
                c.setStrokeColor(borda);

                if(icon != null) icon.setColorFilter(isSelected ? Color.WHITE : cinzaIcon);
            }
        }

        // Atualiza o preview pequeno
        imgBtnSelecionarIcones.setImageResource(getIconePorIndex(indexSelecionado));
        imgBtnSelecionarIcones.setColorFilter(Color.parseColor("#2196F3"));
    }

    private void resetarVisualGridIcones() {
        atualizarVisualGridIcones(-1); // -1 remove a cor de todos
        imgBtnSelecionarIcones.setColorFilter(Color.parseColor("#757575"));
    }

    private void configuringCliquesGridIcones() {
        if (containerIcones == null) return;
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            final int index = i;
            containerIcones.getChildAt(i).setOnClickListener(v ->
                    viewModel.selecionarIcone(index) // Chama ViewModel
            );
        }
    }

    // Método auxiliar renomeado para ficar claro no inicializar
    private void configurarCliquesGridIcones() {
        configuringCliquesGridIcones();
    }

    private void esconderTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

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

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}