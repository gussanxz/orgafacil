package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.produtos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;

public class CadastroProdutoActivity extends AppCompatActivity {

    private FloatingActionButton fabVoltar;
    private View btnSalvarSuperior;
    private View btnSalvarInferior;
    private ImageButton btnExcluirHeader;

    private TextInputLayout textInputProduto;
    private TextInputLayout textInputDescricao;
    private TextInputLayout textInputPreco;
    private TextInputEditText editProduto;
    private TextInputEditText editDescricao;
    private TextView textViewHeader;
    private MaterialSwitch switchStatusAtivo;
    private View loadingOverlay;

    private NestedScrollView scrollView;
    private GridLayout containerIcones;
    private LinearLayout layoutSelecao;
    private ImageButton imgBtnSelecionarIcones;
    private ImageView imgBtnGaleria;
    private MaterialCardView cardBtnGaleria;
    private MaterialCardView cardPreviewFoto;
    private MaterialCardView cardContainerGrid;
    private ImageView imgPreviewLarge;
    private TextView txtTituloSelecao;

    private CatalogoRepository repository;
    private String idEmEdicao = null;
    private int iconeSelecionado = 7;
    private Uri imagemSelecionadaUri = null;
    private String urlFotoAtual = null;

    private final ActivityResultLauncher<Intent> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        imagemSelecionadaUri = uri;
                        atualizarVisualGaleria(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_produto);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new CatalogoRepository();
        inicializarComponentes();
        configurarAcoesBasicas();
        configurarCliquesGridIcones();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            prepararEdicao(extras);
        } else {
            atualizarVisualGridIcones(iconeSelecionado);
        }
    }

    private void inicializarComponentes() {
        fabVoltar = findViewById(R.id.fabVoltar);
        btnSalvarSuperior = findViewById(R.id.fabSuperiorSalvarProduto);
        btnSalvarInferior = findViewById(R.id.fabInferiorSalvarProduto);
        btnExcluirHeader = findViewById(R.id.btnExcluirHeader);

        textInputProduto = findViewById(R.id.textInputProduto);
        textInputDescricao = findViewById(R.id.textInputDescricao);
        textInputPreco = findViewById(R.id.textInputPreco);
        editProduto = findViewById(R.id.editProduto);
        editDescricao = findViewById(R.id.editDescricao);
        textViewHeader = findViewById(R.id.textViewHeader);
        switchStatusAtivo = findViewById(R.id.switchStatusAtivo);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        scrollView = findViewById(R.id.scrollViewContent);
        containerIcones = findViewById(R.id.containerIcones);
        layoutSelecao = findViewById(R.id.layoutSelecao);
        imgBtnSelecionarIcones = findViewById(R.id.imgBtnSelecionarIcones);
        imgBtnGaleria = findViewById(R.id.imgBtnGaleria);
        cardBtnGaleria = findViewById(R.id.cardBtnGaleria);
        cardPreviewFoto = findViewById(R.id.cardPreviewFoto);
        cardContainerGrid = findViewById(R.id.cardContainerGrid);
        imgPreviewLarge = findViewById(R.id.imgPreviewLarge);
        txtTituloSelecao = findViewById(R.id.txtTituloSelecao);

        textViewHeader.setText("Novo Produto");
        switchStatusAtivo.setChecked(true);
        btnExcluirHeader.setVisibility(View.GONE);
    }

    private void configurarAcoesBasicas() {
        fabVoltar.setOnClickListener(v -> finish());
        btnSalvarSuperior.setOnClickListener(this::salvarProduto);
        btnSalvarInferior.setOnClickListener(this::salvarProduto);

        View.OnClickListener acaoAbrirGaleria = v -> abrirGaleria();
        cardBtnGaleria.setOnClickListener(acaoAbrirGaleria);
        imgBtnGaleria.setOnClickListener(acaoAbrirGaleria);
    }

    private void prepararEdicao(Bundle dados) {
        textViewHeader.setText("Editar Produto");
        idEmEdicao = dados.getString("id");
        btnExcluirHeader.setVisibility(View.VISIBLE);

        if (editProduto != null) {
            editProduto.setText(dados.getString("nome", ""));
        }

        if (editDescricao != null) {
            editDescricao.setText(dados.getString("descricao", ""));
        }

        if (textInputPreco.getEditText() != null) {
            double preco = dados.getDouble("preco", 0.0);
            textInputPreco.getEditText().setText(String.valueOf(preco));
        }

        switchStatusAtivo.setChecked(dados.getBoolean("statusAtivo", true));
        iconeSelecionado = dados.getInt("iconeIndex", 7);
        urlFotoAtual = dados.getString("urlFoto", null);
        atualizarVisualGridIcones(iconeSelecionado);
    }

    public void salvarProduto(View view) {
        limparErros();

        String nome = editProduto != null && editProduto.getText() != null
                ? editProduto.getText().toString().trim() : "";
        String descricao = editDescricao != null && editDescricao.getText() != null
                ? editDescricao.getText().toString().trim() : "";
        String precoStr = textInputPreco.getEditText() != null
                ? textInputPreco.getEditText().getText().toString().trim() : "";

        if (nome.isEmpty()) {
            textInputProduto.setError("Obrigatório");
            return;
        }

        if (precoStr.isEmpty()) {
            textInputPreco.setError("Obrigatório");
            return;
        }

        double preco;
        try {
            preco = Double.parseDouble(precoStr.replace(",", "."));
        } catch (NumberFormatException e) {
            textInputPreco.setError("Preço inválido");
            return;
        }

        exibirLoading(true);

        CatalogoModel produtoModel = new CatalogoModel();
        produtoModel.setId(idEmEdicao);
        produtoModel.setNome(nome);
        produtoModel.setDescricao(descricao);
        produtoModel.setPreco(preco);
        produtoModel.setStatusAtivo(switchStatusAtivo.isChecked());
        produtoModel.setIconeIndex(iconeSelecionado);
        produtoModel.setTipo(CatalogoModel.TIPO_STR_PRODUTO);

        if (imagemSelecionadaUri == null && urlFotoAtual != null)
            produtoModel.setUrlFoto(urlFotoAtual);

        repository.salvar(produtoModel, imagemSelecionadaUri, new CatalogoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                exibirLoading(false);
                Toast.makeText(CadastroProdutoActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                exibirLoading(false);
                Toast.makeText(CadastroProdutoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void exibeSelecaoDeIcones(View view) {
        boolean painelAberto = layoutSelecao.getVisibility() == View.VISIBLE;
        boolean mostrandoGrid = cardContainerGrid.getVisibility() == View.VISIBLE;

        if (painelAberto && mostrandoGrid) {
            layoutSelecao.setVisibility(View.GONE);
            return;
        }

        abrirPainelDeIcones();
    }

    private void abrirPainelDeIcones() {
        layoutSelecao.setVisibility(View.VISIBLE);
        cardContainerGrid.setVisibility(View.VISIBLE);
        cardPreviewFoto.setVisibility(View.GONE);
        txtTituloSelecao.setText("Selecione um Ícone:");

        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcherGaleria.launch(intent);
    }

    private void atualizarVisualGaleria(Uri uri) {
        imagemSelecionadaUri = uri;
        cardBtnGaleria.setStrokeColor(Color.parseColor("#2196F3"));
        cardBtnGaleria.setStrokeWidth(4);
        imgBtnGaleria.setColorFilter(null);
        imgBtnGaleria.setImageURI(uri);
        imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (imgPreviewLarge != null) {
            imgPreviewLarge.setImageURI(uri);
            imgPreviewLarge.setImageTintList(null);
            imgPreviewLarge.setColorFilter(null);
            imgPreviewLarge.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        if (layoutSelecao != null) layoutSelecao.setVisibility(View.VISIBLE);
        if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.GONE);
        if (cardPreviewFoto != null) cardPreviewFoto.setVisibility(View.VISIBLE);
        if (txtTituloSelecao != null) txtTituloSelecao.setText("Pré-visualização:");

        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void resetarVisualGaleria() {
        imagemSelecionadaUri = null;
        cardBtnGaleria.setStrokeColor(Color.parseColor("#E0E0E0"));
        cardBtnGaleria.setStrokeWidth(1);
        imgBtnGaleria.setImageResource(R.drawable.ic_camera_alt_120);
        imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgBtnGaleria.setColorFilter(Color.parseColor("#757575"));
    }

    private void atualizarVisualGridIcones(int indexSelecionado) {
        int verde = Color.parseColor("#25D366");
        int cinzaBg = Color.parseColor("#F5F5F5");
        int cinzaIcon = Color.parseColor("#9E9E9E");
        int borda = Color.parseColor("#E0E0E0");

        if (containerIcones == null) return;

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            MaterialCardView c = (MaterialCardView) containerIcones.getChildAt(i);
            ImageView icon = (ImageView) c.getChildAt(0);
            boolean isSelected = (i == indexSelecionado);

            c.setCardBackgroundColor(isSelected ? verde : cinzaBg);
            c.setStrokeWidth(isSelected ? 0 : 3);
            c.setStrokeColor(borda);
            if (icon != null) icon.setColorFilter(isSelected ? Color.WHITE : cinzaIcon);
        }

        if (indexSelecionado >= 0) {
            imgBtnSelecionarIcones.setImageResource(getIconePorIndex(indexSelecionado));
            imgBtnSelecionarIcones.setColorFilter(Color.parseColor("#2196F3"));
        }
    }

    private void configurarCliquesGridIcones() {
        if (containerIcones == null) return;

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            int finalI = i;
            containerIcones.getChildAt(i).setOnClickListener(v -> {
                iconeSelecionado = finalI;
                atualizarVisualGridIcones(finalI);
                resetarVisualGaleria();
            });
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

    private void limparErros() {
        textInputProduto.setError(null);
        textInputDescricao.setError(null);
        textInputPreco.setError(null);
    }

    private void exibirLoading(boolean exibir) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(exibir ? View.VISIBLE : View.GONE);
        }
        btnSalvarSuperior.setEnabled(!exibir);
        btnSalvarInferior.setEnabled(!exibir);
    }
}