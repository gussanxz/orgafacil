package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos;

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
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;

import java.util.ArrayList;
import java.util.List;

public class CadastroCatalogoActivity extends AppCompatActivity {

    // ── Views: header ─────────────────────────────────────────────────
    private FloatingActionButton fabVoltar;
    private View                 btnSalvarSuperior;
    private View                 btnSalvarInferior;
    private ImageButton          btnExcluirHeader;
    private TextView             textViewHeader;
    private TextView             textTipoCadastro;

    // ── Views: seletor de tipo (novo item) ────────────────────────────
    private LinearLayout     layoutSeletorTipo;
    private MaterialCardView cardTipoProduto;
    private MaterialCardView cardTipoServico;
    private ImageView        iconTipoProduto;
    private ImageView        iconTipoServico;
    private TextView         txtTipoProduto;
    private TextView         txtTipoServico;
    private TextInputLayout textInputCategoria;

    // ── Views: formulário ─────────────────────────────────────────────
    private LinearLayout       layoutFormulario;
    private TextInputLayout    textInputNome;
    private TextInputLayout    textInputDescricao;
    private TextInputLayout    textInputPreco;
    private TextInputEditText  editNome;
    private TextInputEditText  editDescricao;
    private MaterialSwitch     switchStatusAtivo;
    private View               loadingOverlay;

    // ── Views: bloco de ícones (produto) ──────────────────────────────
    private LinearLayout       caixaIcones;        // id: caixa
    private NestedScrollView   scrollView;
    private LinearLayout       layoutSelecao;
    private GridLayout         containerIcones;
    private ImageButton        imgBtnSelecionarIcones;
    private ImageView          imgBtnGaleria;
    private MaterialCardView   cardBtnGaleria;
    private MaterialCardView   cardPreviewFoto;
    private MaterialCardView   cardContainerGrid;
    private ImageView          imgPreviewLarge;
    private TextView           txtTituloSelecao;

    // ── Estado ────────────────────────────────────────────────────────
    private CatalogoRepository repository;
    private String  idEmEdicao         = null;
    private String  tipoAtual          = null;   // null = ainda não escolheu
    private int     iconeSelecionado   = 7;
    private Uri     imagemSelecionadaUri = null;
    private String urlFotoAtual = null;
    private Uri uriCameraTemp;

    private final ActivityResultLauncher<Uri> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            sucesso -> {
                if (sucesso && uriCameraTemp != null) {
                    java.io.File f = new java.io.File(uriCameraTemp.getPath());
                    if (f.exists() && f.length() > 0)
                        atualizarVisualGaleria(uriCameraTemp);
                    else
                        Toast.makeText(this, "Erro ao capturar foto", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private ListenerRegistration listenerCategorias;
    private String categoriaSelecionadaId   = CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO;

    private String categoriaSelecionadaNome = CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO;
    private CategoriaCatalogoRepository categoriaRepo;
    private List<Categoria> listaCategorias = new ArrayList<>();

    // Cores de destaque por tipo
    private static final int COR_PRODUTO = 0xFFEF6C00;
    private static final int COR_SERVICO = 0xFF1565C0;
    private static final int COR_NEUTRO  = 0xFF9E9E9E;

    private final ActivityResultLauncher<Intent> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) atualizarVisualGaleria(uri);
                }
            }
    );

    // ── Ciclo de vida ─────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_main_vendas_opd_cadastro_catalogo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        repository    = new CatalogoRepository();
        categoriaRepo = new CategoriaCatalogoRepository();
        vincularViews();
        configurarAcoes();
        carregarCategorias();
        configurarCliquesGridIcones();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            // Edição: já tem tipo definido
            prepararEdicao(extras);
        } else {
            // Novo item: verifica se veio com tipo pré-definido (ex: da ListaProdutosEServicos)
            String tipoIntent = getIntent().getStringExtra("tipo");
            if (tipoIntent != null) {
                selecionarTipo(tipoIntent, false);
            } else {
                // Sem tipo: mostra seletor inline
                mostrarSeletorTipo();
            }
        }
    }

    // ── Bind ──────────────────────────────────────────────────────────

    private void vincularViews() {
        fabVoltar         = findViewById(R.id.fabVoltar);
        btnSalvarSuperior = findViewById(R.id.fabSuperiorSalvarProduto);
        btnSalvarInferior = findViewById(R.id.fabInferiorSalvarProduto);
        btnExcluirHeader  = findViewById(R.id.btnExcluirHeader);
        textViewHeader    = findViewById(R.id.textViewHeader);

        layoutSeletorTipo = findViewById(R.id.layoutSeletorTipo);
        cardTipoProduto   = findViewById(R.id.cardTipoProduto);
        cardTipoServico   = findViewById(R.id.cardTipoServico);
        iconTipoProduto   = findViewById(R.id.iconTipoProduto);
        iconTipoServico   = findViewById(R.id.iconTipoServico);
        txtTipoProduto    = findViewById(R.id.txtTipoProduto);
        txtTipoServico    = findViewById(R.id.txtTipoServico);

        layoutFormulario  = findViewById(R.id.layoutFormulario);
        textInputNome     = findViewById(R.id.textInputProduto);
        textInputDescricao= findViewById(R.id.textInputDescricao);
        textInputPreco    = findViewById(R.id.textInputPreco);
        editNome          = findViewById(R.id.editProduto);
        editDescricao     = findViewById(R.id.editDescricao);
        switchStatusAtivo = findViewById(R.id.switchStatusAtivo);
        loadingOverlay    = findViewById(R.id.loadingOverlay);

        caixaIcones           = findViewById(R.id.caixa);
        scrollView            = findViewById(R.id.scrollViewContent);
        containerIcones       = findViewById(R.id.containerIcones);
        layoutSelecao         = findViewById(R.id.layoutSelecao);
        imgBtnSelecionarIcones= findViewById(R.id.imgBtnSelecionarIcones);
        textInputCategoria = findViewById(R.id.textInputCategoria);
        imgBtnGaleria         = findViewById(R.id.imgBtnGaleria);
        cardBtnGaleria        = findViewById(R.id.cardBtnGaleria);
        cardPreviewFoto       = findViewById(R.id.cardPreviewFoto);
        cardContainerGrid     = findViewById(R.id.cardContainerGrid);
        imgPreviewLarge       = findViewById(R.id.imgPreviewLarge);
        txtTituloSelecao      = findViewById(R.id.txtTituloSelecao);

        switchStatusAtivo.setChecked(true);
        btnExcluirHeader.setVisibility(View.GONE);
    }

    private void configurarAcoes() {
        fabVoltar.setOnClickListener(v -> finish());
        btnSalvarSuperior.setOnClickListener(v -> salvarItem());
        btnSalvarInferior.setOnClickListener(v -> salvarItem());
        btnExcluirHeader.setOnClickListener(v -> confirmarExclusao());

        cardTipoProduto.setOnClickListener(v -> selecionarTipo(CatalogoModel.TIPO_STR_PRODUTO, true));
        cardTipoServico.setOnClickListener(v -> selecionarTipo(CatalogoModel.TIPO_STR_SERVICO, true));

        configurarCliquesCategoria();

        View.OnClickListener abrirOpcoesFoto = v -> exibirDialogFonte();
        if (cardBtnGaleria != null) cardBtnGaleria.setOnClickListener(abrirOpcoesFoto);
        if (imgBtnGaleria  != null) imgBtnGaleria.setOnClickListener(abrirOpcoesFoto);
    }

    private void exibirDialogFonte() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Adicionar foto")
                .setItems(new String[]{"Câmera", "Galeria"}, (d, which) -> {
                    if (which == 0) abrirCamera();
                    else            abrirGaleria();
                })
                .show();
    }

    private void carregarCategorias() {
        listenerCategorias = categoriaRepo.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override public void onNovosDados(List<Categoria> lista) {
                listaCategorias.clear();
                listaCategorias.addAll(lista);
            }
            @Override public void onErro(String erro) { /* silencioso */ }
        });
    }

    private void exibirDialogCategorias() {
        if (listaCategorias.isEmpty()) {
            Toast.makeText(this, "Carregando categorias, tente novamente", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] nomes = new String[listaCategorias.size()];
        for (int i = 0; i < listaCategorias.size(); i++)
            nomes[i] = listaCategorias.get(i).getNome();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Selecionar Categoria")
                .setItems(nomes, (dialog, which) -> {
                    Categoria c = listaCategorias.get(which);
                    categoriaSelecionadaId   = c.getId();
                    categoriaSelecionadaNome = c.getNome();
                    if (textInputCategoria != null && textInputCategoria.getEditText() != null)
                        textInputCategoria.getEditText().setText(c.getNome());
                })
                .show();
    }

    // ── Seletor de tipo (estado inicial de novo item) ─────────────────

    private void mostrarSeletorTipo() {
        tipoAtual = null;
        textViewHeader.setText("Novo Item");
        layoutSeletorTipo.setVisibility(View.VISIBLE);
        layoutFormulario.setVisibility(View.GONE);
        // FABs de salvar ficam ocultos até o tipo ser escolhido
        btnSalvarSuperior.setVisibility(View.GONE);
        btnSalvarInferior.setVisibility(View.GONE);
        // Reseta visual dos cards para neutro
        aplicarVisualCardTipo(cardTipoProduto, iconTipoProduto, txtTipoProduto, COR_NEUTRO, false);
        aplicarVisualCardTipo(cardTipoServico, iconTipoServico, txtTipoServico, COR_NEUTRO, false);
    }

    /**
     * Chamado ao clicar em um card de tipo OU ao receber tipo via Intent.
     *
     * @param tipo         "produto" ou "servico"
     * @param viaClique    true = usuário clicou no card (anima a seleção);
     *                     false = veio via Intent, vai direto ao formulário
     */
    private void selecionarTipo(String tipo, boolean viaClique) {
        tipoAtual = tipo;
        boolean isProduto = CatalogoModel.TIPO_STR_PRODUTO.equals(tipo);

        // Destaca o card escolhido e apaga o outro
        int corEscolhida = isProduto ? COR_PRODUTO : COR_SERVICO;
        aplicarVisualCardTipo(cardTipoProduto, iconTipoProduto, txtTipoProduto,
                isProduto ? corEscolhida : COR_NEUTRO, isProduto);
        aplicarVisualCardTipo(cardTipoServico, iconTipoServico, txtTipoServico,
                isProduto ? COR_NEUTRO : corEscolhida, !isProduto);

        // Se veio via clique, mantém o seletor visível por um breve momento
        // e depois revela o formulário. Se veio direto, pula a animação.
        if (viaClique) {
            // Pequeno delay para o usuário ver o feedback visual antes do formulário aparecer
            cardTipoProduto.postDelayed(() -> revelarFormulario(isProduto), 200);
        } else {
            revelarFormulario(isProduto);
        }
    }

    private void revelarFormulario(boolean isProduto) {
        // Oculta seletor e revela formulário
        if (idEmEdicao == null) {
            layoutSeletorTipo.setVisibility(View.VISIBLE);
            layoutFormulario.setVisibility(View.VISIBLE);
            btnSalvarSuperior.setVisibility(View.VISIBLE);
            btnSalvarInferior.setVisibility(View.VISIBLE);
        }

        // Bloco de ícones apenas para produto
        if (caixaIcones != null) {
            caixaIcones.setVisibility(isProduto ? View.VISIBLE : View.GONE);
        }

        // Atualiza header e hint do campo nome
        if (idEmEdicao == null) {
            textViewHeader.setText(isProduto ? "Novo Produto" : "Novo Serviço");
        } else {
            textViewHeader.setText(isProduto ? "Editar Produto" : "Editar Serviço");
        }
        if (textInputNome != null) {
            textInputNome.setHint(isProduto ? "Nome do Produto" : "Nome do Serviço");
        }
        if (textInputCategoria != null && textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setText(categoriaSelecionadaNome);

        if (isProduto) atualizarVisualGridIcones(iconeSelecionado);
    }

    /** Aplica estilo visual ao card de seleção de tipo. */
    private void aplicarVisualCardTipo(MaterialCardView card, ImageView icon,
                                       TextView txt, int cor, boolean selecionado) {
        if (card == null) return;
        card.setStrokeColor(selecionado ? cor : 0xFFE0E0E0);
        card.setStrokeWidth(selecionado ? 4 : 2);
        card.setCardBackgroundColor(selecionado
                ? (cor & 0x00FFFFFF | 0x15000000)   // ~8% de opacidade da cor
                : Color.WHITE);
        if (icon != null) icon.setColorFilter(selecionado ? cor : COR_NEUTRO);
        if (txt  != null) {
            txt.setTextColor(selecionado ? cor : COR_NEUTRO);
        }
    }

    // ── Edição ────────────────────────────────────────────────────────

    private void prepararEdicao(Bundle dados) {
        idEmEdicao = dados.getString("id");
        String tipo = dados.getString("tipo", CatalogoModel.TIPO_STR_PRODUTO);
        boolean isProduto = CatalogoModel.TIPO_STR_PRODUTO.equals(tipo);
        categoriaSelecionadaId   = dados.getString("categoriaId", CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO);
        categoriaSelecionadaNome = dados.getString("categoria",   CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);
        urlFotoAtual = dados.getString("urlFoto", null);
        tipoAtual = tipo;
        textViewHeader.setText(isProduto ? "Editar Produto" : "Editar Serviço");
        btnExcluirHeader.setVisibility(View.VISIBLE);

        // Em edição o seletor não aparece, vai direto ao formulário
        layoutSeletorTipo.setVisibility(View.VISIBLE);
        layoutFormulario.setVisibility(View.VISIBLE);
        btnSalvarSuperior.setVisibility(View.VISIBLE);
        btnSalvarInferior.setVisibility(View.VISIBLE);

        // Aplica destaque visual no card do tipo atual sem animar/revelar formulário
        int corAtual = isProduto ? COR_PRODUTO : COR_SERVICO;
        aplicarVisualCardTipo(cardTipoProduto, iconTipoProduto, txtTipoProduto,
                isProduto ? corAtual : COR_NEUTRO, isProduto);
        aplicarVisualCardTipo(cardTipoServico, iconTipoServico, txtTipoServico,
                isProduto ? COR_NEUTRO : corAtual, !isProduto);

        if (caixaIcones != null)
            caixaIcones.setVisibility(isProduto ? View.VISIBLE : View.GONE);
        if (textInputNome != null)
            textInputNome.setHint(isProduto ? "Nome do Produto" : "Nome do Serviço");

        if (editNome != null)
            editNome.setText(dados.getString("nome", ""));
        if (editDescricao != null)
            editDescricao.setText(dados.getString("descricao", ""));
        if (textInputPreco.getEditText() != null)
            textInputPreco.getEditText().setText(String.valueOf(dados.getDouble("preco", 0.0)));

        switchStatusAtivo.setChecked(dados.getBoolean("statusAtivo", true));
        if (textInputCategoria != null && textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setText(categoriaSelecionadaNome);

        iconeSelecionado = dados.getInt("iconeIndex", 7);
        if (isProduto) atualizarVisualGridIcones(iconeSelecionado);
    }

    // ── Salvar ────────────────────────────────────────────────────────

    private void salvarItem() {
        if (tipoAtual == null) {
            Toast.makeText(this, "Selecione o tipo antes de salvar", Toast.LENGTH_SHORT).show();
            return;
        }

        limparErros();

        String nome = editNome != null && editNome.getText() != null
                ? editNome.getText().toString().trim() : "";
        String descricao = editDescricao != null && editDescricao.getText() != null
                ? editDescricao.getText().toString().trim() : "";
        String precoStr = textInputPreco.getEditText() != null
                ? textInputPreco.getEditText().getText().toString().trim() : "";

        if (nome.isEmpty()) {
            textInputNome.setError("Obrigatório");
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

        CatalogoModel model = new CatalogoModel();
        model.setId(idEmEdicao);
        model.setTipo(tipoAtual);
        model.setNome(nome);
        model.setDescricao(descricao);
        model.setCategoriaId(categoriaSelecionadaId);
        model.setCategoria(categoriaSelecionadaNome);
        model.setPreco(preco);
        model.setStatusAtivo(switchStatusAtivo.isChecked());

        if (CatalogoModel.TIPO_STR_PRODUTO.equals(tipoAtual)) {
            model.setIconeIndex(iconeSelecionado);
        }

        if (imagemSelecionadaUri == null && urlFotoAtual != null)
            model.setUrlFoto(urlFotoAtual);

        repository.salvar(model, imagemSelecionadaUri, new CatalogoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                exibirLoading(false);
                Toast.makeText(CadastroCatalogoActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onErro(String erro) {
                exibirLoading(false);
                Toast.makeText(CadastroCatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Ícones ────────────────────────────────────────────────────────

    public void exibeSelecaoDeIcones(View view) {
        boolean painelAberto  = layoutSelecao != null && layoutSelecao.getVisibility() == View.VISIBLE;
        boolean mostrandoGrid = cardContainerGrid != null && cardContainerGrid.getVisibility() == View.VISIBLE;
        if (painelAberto && mostrandoGrid) {
            layoutSelecao.setVisibility(View.GONE);
            return;
        }
        abrirPainelDeIcones();
    }

    private void abrirPainelDeIcones() {
        if (layoutSelecao     != null) layoutSelecao.setVisibility(View.VISIBLE);
        if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.VISIBLE);
        if (cardPreviewFoto   != null) cardPreviewFoto.setVisibility(View.GONE);
        if (txtTituloSelecao  != null) txtTituloSelecao.setText("Selecione um Ícone:");
        if (scrollView        != null) scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcherGaleria.launch(intent);
    }

    private void atualizarVisualGaleria(Uri uri) {
        imagemSelecionadaUri = uri;
        if (cardBtnGaleria != null) { cardBtnGaleria.setStrokeColor(Color.parseColor("#2196F3")); cardBtnGaleria.setStrokeWidth(4); }
        if (imgBtnGaleria  != null) { imgBtnGaleria.setColorFilter(null); imgBtnGaleria.setImageURI(uri); imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP); }
        if (imgPreviewLarge!= null) { imgPreviewLarge.setImageURI(uri); imgPreviewLarge.setImageTintList(null); imgPreviewLarge.setColorFilter(null); imgPreviewLarge.setScaleType(ImageView.ScaleType.CENTER_CROP); }
        if (layoutSelecao     != null) layoutSelecao.setVisibility(View.VISIBLE);
        if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.GONE);
        if (cardPreviewFoto   != null) cardPreviewFoto.setVisibility(View.VISIBLE);
        if (txtTituloSelecao  != null) txtTituloSelecao.setText("Pré-visualização:");
        if (scrollView        != null) scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void resetarVisualGaleria() {
        imagemSelecionadaUri = null;
        if (cardBtnGaleria != null) { cardBtnGaleria.setStrokeColor(Color.parseColor("#E0E0E0")); cardBtnGaleria.setStrokeWidth(1); }
        if (imgBtnGaleria  != null) { imgBtnGaleria.setImageResource(R.drawable.ic_camera_alt_120); imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP); imgBtnGaleria.setColorFilter(Color.parseColor("#757575")); }
    }

    private void atualizarVisualGridIcones(int indexSelecionado) {
        if (containerIcones == null) return;
        int verde   = Color.parseColor("#25D366");
        int cinzaBg = Color.parseColor("#F5F5F5");
        int cinzaIc = Color.parseColor("#9E9E9E");
        int borda   = Color.parseColor("#E0E0E0");

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            MaterialCardView c    = (MaterialCardView) containerIcones.getChildAt(i);
            ImageView        icon = (ImageView) c.getChildAt(0);
            boolean sel = (i == indexSelecionado);
            c.setCardBackgroundColor(sel ? verde : cinzaBg);
            c.setStrokeWidth(sel ? 0 : 3);
            c.setStrokeColor(borda);
            if (icon != null) icon.setColorFilter(sel ? Color.WHITE : cinzaIc);
        }

        if (indexSelecionado >= 0 && imgBtnSelecionarIcones != null) {
            imgBtnSelecionarIcones.setImageResource(getIconePorIndex(indexSelecionado));
            imgBtnSelecionarIcones.setColorFilter(Color.parseColor("#2196F3"));
        }
    }

    private void configurarCliquesGridIcones() {
        if (containerIcones == null) return;
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            int fi = i;
            containerIcones.getChildAt(i).setOnClickListener(v -> {
                iconeSelecionado = fi;
                atualizarVisualGridIcones(fi);
                resetarVisualGaleria();
            });
        }
    }

    private int getIconePorIndex(int index) {
        switch (index) {
            case 0:  return R.drawable.ic_categorias_mercado_24;
            case 1:  return R.drawable.ic_categorias_roupas_24;
            case 2:  return R.drawable.ic_categorias_comida_24;
            case 3:  return R.drawable.ic_categorias_bebidas_24;
            case 4:  return R.drawable.ic_categorias_eletronicos_24;
            case 5:  return R.drawable.ic_categorias_spa_24;
            case 6:  return R.drawable.ic_categorias_fitness_24;
            case 7:  return R.drawable.ic_categorias_geral_24;
            case 8:  return R.drawable.ic_categorias_ferramentas_24;
            case 9:  return R.drawable.ic_categorias_papelaria_24;
            case 10: return R.drawable.ic_categorias_casa_24;
            case 11: return R.drawable.ic_categorias_brinquedos_24;
            default: return R.drawable.ic_categorias_geral_24;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void limparErros() {
        if (textInputNome      != null) textInputNome.setError(null);
        if (textInputDescricao != null) textInputDescricao.setError(null);
        if (textInputPreco     != null) textInputPreco.setError(null);
    }

    private void exibirLoading(boolean exibir) {
        if (loadingOverlay    != null) loadingOverlay.setVisibility(exibir ? View.VISIBLE : View.GONE);
        if (btnSalvarSuperior != null) btnSalvarSuperior.setEnabled(!exibir);
        if (btnSalvarInferior != null) btnSalvarInferior.setEnabled(!exibir);
    }
    private void configurarCliquesCategoria() {
        if (textInputCategoria == null) return;
        textInputCategoria.setOnClickListener(v -> exibirDialogCategorias());
        textInputCategoria.setEndIconOnClickListener(v -> exibirDialogCategorias());
        if (textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setOnClickListener(v -> exibirDialogCategorias());
    }

    private void abrirCamera() {
        java.io.File arquivo = new java.io.File(getCacheDir(), "foto_temp_" + System.currentTimeMillis() + ".jpg");
        uriCameraTemp = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", arquivo);
        launcherCamera.launch(uriCameraTemp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerCategorias != null) listenerCategorias.remove();
        if (uriCameraTemp != null) {
            new java.io.File(uriCameraTemp.getPath()).delete();
            uriCameraTemp = null;
        }
    }

    private void confirmarExclusao() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir item")
                .setMessage("Tem certeza que deseja excluir este item? Esta ação não pode ser desfeita.")
                .setPositiveButton("Excluir", (dialog, which) -> excluirItem())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirItem() {
        if (idEmEdicao == null || idEmEdicao.isEmpty()) {
            Toast.makeText(this, "ID do item não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        exibirLoading(true);
        repository.excluir(idEmEdicao, urlFotoAtual, new CatalogoRepository.Callback() {
            @Override
            public void onSucesso(String mensagem) {
                exibirLoading(false);
                Toast.makeText(CadastroCatalogoActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onErro(String erro) {
                exibirLoading(false);
                Toast.makeText(CadastroCatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

}