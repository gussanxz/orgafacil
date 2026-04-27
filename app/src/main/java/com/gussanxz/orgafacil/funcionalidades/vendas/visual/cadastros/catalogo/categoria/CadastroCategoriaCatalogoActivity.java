package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.gussanxz.orgafacil.R;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;

public class CadastroCategoriaCatalogoActivity extends AppCompatActivity {

    // --- UI Components ---
    private NestedScrollView scrollView;
    private GridLayout containerIcones;
    private LinearLayout layoutSelecao;
    private TextView textViewHeader;
    private ImageButton imgBtnSelecionarIcones;
    private ImageView imgBtnGaleria;
    private MaterialCardView cardBtnGaleria;
    private TextInputEditText editNome, editDesc;
    private MaterialSwitch switchAtiva;
    private View loadingOverlay;
    private MaterialCardView cardPreviewFoto, cardContainerGrid;
    private ImageView imgPreviewLarge;
    private TextView txtTituloSelecao, txtLabelCorIcone;
    private LinearLayout containerCoresIcone;

    private String corIconeSelecionada = "#2ED8CC";
    private final String[] coresIcone = {
            "#2ED8CC", "#46E0A6", "#7B61FF", "#E14FC4",
            "#FFB020", "#FF706A", "#4DA3FF", "#A8B3C7"
    };

    // Botões Salvar
    private View btnSalvarSuperior, btnSalvarInferior;

    // ViewModel
    private CadastroCategoriaVendasViewModel viewModel;

    // Launcher da Galeria
    private final ActivityResultLauncher<Intent> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    viewModel.selecionarFoto(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_cadastro_categoria);
        ajustarInsets();

        viewModel = new ViewModelProvider(this).get(CadastroCategoriaVendasViewModel.class);

        inicializarComponentes();
        processarIntentInicial();
        observarViewModel();
    }

    private void inicializarComponentes() {
        // 1. Vincular IDs
        scrollView = findViewById(R.id.scrollViewContent);
        containerIcones = findViewById(R.id.containerIcones);
        layoutSelecao = findViewById(R.id.layoutSelecao);
        textViewHeader = findViewById(R.id.textViewHeader);

        imgBtnSelecionarIcones = findViewById(R.id.imgBtnSelecionarIcones);
        imgBtnGaleria = findViewById(R.id.imgBtnGaleria);
        cardBtnGaleria = findViewById(R.id.cardBtnGaleria);

        editNome = findViewById(R.id.editCategoria);
        editDesc = findViewById(R.id.editDescricao);
        switchAtiva = findViewById(R.id.switchAtiva);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        btnSalvarSuperior = findViewById(R.id.fabSuperiorSalvarCategoria);
        btnSalvarInferior = findViewById(R.id.fabInferiorSalvarCategoria);

        cardPreviewFoto = findViewById(R.id.cardPreviewFoto);
        cardContainerGrid = findViewById(R.id.cardContainerGrid);
        imgPreviewLarge = findViewById(R.id.imgPreviewLarge);
        txtTituloSelecao = findViewById(R.id.txtTituloSelecao);
        txtLabelCorIcone = findViewById(R.id.txtLabelCorIcone);
        containerCoresIcone = findViewById(R.id.containerCoresIcone);

        // 2. Configurar Cliques Básicos
        findViewById(R.id.fabVoltar).setOnClickListener(v -> finish());
        btnSalvarSuperior.setOnClickListener(this::salvarCategoria);
        btnSalvarInferior.setOnClickListener(this::salvarCategoria);

        // Listener Galeria (Aplica no Card e na Imagem para garantir o clique)
        View.OnClickListener acaoAbrirGaleria = v -> abrirGaleria();
        if (cardBtnGaleria != null) cardBtnGaleria.setOnClickListener(acaoAbrirGaleria);
        if (imgBtnGaleria != null) imgBtnGaleria.setOnClickListener(acaoAbrirGaleria);

        // 3. Configurar Grid de Ícones
        configurarCliquesGridIcones();
        configurarSeletorCoresIcone();
    }

    private void processarIntentInicial() {
        Intent intent = getIntent();
        String tipo = intent.getStringExtra("tipo");

        // --- CORREÇÃO DE CASE SENSITIVITY ---
        // Garante que "Produto" vire "PRODUTO" para bater com o Enum
        if (tipo != null) {
            tipo = tipo.toUpperCase();
        }

        viewModel.definirContexto(tipo);

        if (intent.hasExtra("modoEditar")) {
            editNome.setText(intent.getStringExtra("nome"));
            editDesc.setText(intent.getStringExtra("descricao"));
            switchAtiva.setChecked(intent.getBooleanExtra("ativa", true));
            selecionarCorIcone(intent.getStringExtra("corIcone"), false);

            String urlFoto = intent.getStringExtra("urlImagem");

            // Se tiver foto antiga (URL), carregar com Glide
            if (urlFoto != null && !urlFoto.isEmpty()) {
                // Botão pequeno
                com.bumptech.glide.Glide.with(this)
                        .load(urlFoto)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(imgBtnGaleria);

                imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP);
                androidx.core.widget.ImageViewCompat.setImageTintList(imgBtnGaleria, null);
                cardBtnGaleria.setStrokeColor(ContextCompat.getColor(this, R.color.cadastro_categoria_accent));
                cardBtnGaleria.setStrokeWidth(4);

                // ── ADICIONE ISTO: Preview grande com a foto existente ──
                if (imgPreviewLarge != null) {
                    com.bumptech.glide.Glide.with(this)
                            .load(urlFoto)
                            .centerCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(imgPreviewLarge);
                    androidx.core.widget.ImageViewCompat.setImageTintList(imgPreviewLarge, null);
                    imgPreviewLarge.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
                if (layoutSelecao != null)    layoutSelecao.setVisibility(View.VISIBLE);
                if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.GONE);
                if (cardPreviewFoto != null)   cardPreviewFoto.setVisibility(View.VISIBLE);
                setCoresIconeVisibility(false);
                if (txtTituloSelecao != null)  txtTituloSelecao.setText("Pré-visualização:");
            }

            viewModel.carregarDadosEdicao(
                    intent.getStringExtra("idCategoria"),
                    intent.getStringExtra("nome"),
                    intent.getStringExtra("descricao"),
                    intent.getBooleanExtra("ativa", true),
                    urlFoto,
                    intent.getIntExtra("iconeIndex", 0)
            );
            textViewHeader.setText("Editar Categoria");
        } else {
            atualizarTituloHeader();
        }
    }

    private void observarViewModel() {
        // Observa mudança no índice do ícone
        viewModel.iconeSelecionado.observe(this, index -> {
            if (index != -1) {
                atualizarVisualGridIcones(index);
                resetarVisualGaleria();
            }
        });

        // Observa mudança na URI da imagem (Galeria)
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
                editNome.requestFocus();
            } else {
                Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
            }
        });

        // Loading
        viewModel.carregando.observe(this, isLoading -> {
            btnSalvarSuperior.setEnabled(!isLoading);
            btnSalvarInferior.setEnabled(!isLoading);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    // --- AÇÕES ---

    public void salvarCategoria(View view) {
        viewModel.salvar(
                editNome.getText().toString(),
                editDesc.getText().toString(),
                switchAtiva.isChecked(),
                corIconeSelecionada
        );
    }

    // --- LÓGICA DE INTERAÇÃO VISUAL ---

    public void exibeSelecaoDeIcones(View view) {
        esconderTeclado();

        // 1. Lógica de Toggle (se já está aberto, fecha)
        boolean painelAberto = layoutSelecao.getVisibility() == View.VISIBLE;
        boolean mostrandoGrid = cardContainerGrid.getVisibility() == View.VISIBLE;

        if (painelAberto && mostrandoGrid) {
            layoutSelecao.setVisibility(View.GONE);
            return;
        }

        // 2. Verifica se tem FOTO selecionada
        Integer currentIndex = viewModel.iconeSelecionado.getValue();
        boolean temFotoSelecionada = (currentIndex != null && currentIndex == -1);

        if (temFotoSelecionada) {
            mostrarDialogConfirmacaoTroca(); // <--- CHAMA O NOVO DIALOG AQUI
        } else {
            abrirPainelDeIcones();
        }
    }

    // --- NOVO MÉTODO DO DIALOG PERSONALIZADO ---
    private void mostrarDialogConfirmacaoTroca() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Infla o layout que criamos
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmar_troca_foto_categoria, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // IMPORTANTE: Deixa o fundo do Dialog transparente para o CardView arredondado aparecer
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Configura os botões do layout customizado
        View btnConfirmar = view.findViewById(R.id.btnConfirmarDialog);
        View btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        btnConfirmar.setOnClickListener(v -> {
            abrirPainelDeIcones(); // Ação real
            dialog.dismiss();      // Fecha o dialog
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void abrirPainelDeIcones() {
        esconderTeclado();

        layoutSelecao.setVisibility(View.VISIBLE);
        cardContainerGrid.setVisibility(View.VISIBLE);
        cardPreviewFoto.setVisibility(View.GONE);
        setCoresIconeVisibility(true);
        txtTituloSelecao.setText("Selecione um Ícone:");

        // Rola a tela para baixo para mostrar os ícones
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcherGaleria.launch(intent);
    }

    // --- ATUALIZAÇÃO VISUAL ---

    private void atualizarVisualGaleria(Uri uri) {
        // 1. Atualiza visual do botão pequeno (apenas borda e ícone azul)
        cardBtnGaleria.setStrokeColor(ContextCompat.getColor(this, R.color.cadastro_categoria_accent));
        cardBtnGaleria.setStrokeWidth(4);
        imgBtnGaleria.setColorFilter(ContextCompat.getColor(this, R.color.cadastro_categoria_accent));

        // 2. Atualiza o PREVIEW GRANDE com a foto real
        if (imgPreviewLarge != null) {
            imgPreviewLarge.setImageURI(uri);
            imgPreviewLarge.setImageTintList(null); // Remove tint
            androidx.core.widget.ImageViewCompat.setImageTintList(imgPreviewLarge, null);
            imgPreviewLarge.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        // 3. Controles de Visibilidade
        if (layoutSelecao != null) layoutSelecao.setVisibility(View.VISIBLE);
        if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.GONE);
        if (cardPreviewFoto != null) cardPreviewFoto.setVisibility(View.VISIBLE);
        setCoresIconeVisibility(false);
        if (txtTituloSelecao != null) txtTituloSelecao.setText("Pré-visualização:");

        // 4. Scroll Automático
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void resetarVisualGaleria() {
        // Volta o botão da galeria para o estado inativo (cinza)
        cardBtnGaleria.setStrokeColor(ContextCompat.getColor(this, R.color.cadastro_categoria_stroke));
        cardBtnGaleria.setStrokeWidth(1);
        imgBtnGaleria.setColorFilter(ContextCompat.getColor(this, R.color.cadastro_categoria_icon_muted));
    }

    private void atualizarVisualGridIcones(int indexSelecionado) {
        int accent = corIconeInt();
        int selectedBg = ContextCompat.getColor(this, R.color.cadastro_categoria_card_selected);
        int cardBg = ContextCompat.getColor(this, R.color.cadastro_categoria_card);
        int iconMuted = ContextCompat.getColor(this, R.color.cadastro_categoria_icon_muted);
        int borda = ContextCompat.getColor(this, R.color.cadastro_categoria_stroke);

        if (containerIcones == null) return;

        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            MaterialCardView c = (MaterialCardView) containerIcones.getChildAt(i);
            ImageView icon = (ImageView) c.getChildAt(0);
            boolean isSelected = (i == indexSelecionado);

            c.setCardBackgroundColor(isSelected ? selectedBg : cardBg);
            c.setStrokeWidth(isSelected ? 0 : 3);
            c.setStrokeColor(borda);
            if (icon != null) icon.setColorFilter(isSelected ? accent : iconMuted);
        }

        if (indexSelecionado >= 0) {
            imgBtnSelecionarIcones.setImageResource(getIconePorIndex(indexSelecionado));
            imgBtnSelecionarIcones.setColorFilter(accent);
        }
    }

    private void resetarVisualGridIcones() {
        atualizarVisualGridIcones(-1);
        imgBtnSelecionarIcones.setColorFilter(ContextCompat.getColor(this, R.color.cadastro_categoria_icon_muted));
    }

    private void configurarCliquesGridIcones() {
        if (containerIcones == null) return;
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            int finalI = i;
            containerIcones.getChildAt(i).setOnClickListener(v -> viewModel.selecionarIcone(finalI));
        }
    }

    private void configurarSeletorCoresIcone() {
        if (containerCoresIcone == null) return;
        containerCoresIcone.removeAllViews();

        for (String cor : coresIcone) {
            MaterialCardView swatch = new MaterialCardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
            params.setMarginEnd(dp(10));
            swatch.setLayoutParams(params);
            swatch.setRadius(dp(21));
            swatch.setCardElevation(0);
            swatch.setClickable(true);
            swatch.setFocusable(true);
            swatch.setTag(cor);
            swatch.setOnClickListener(v -> selecionarCorIcone((String) v.getTag(), true));

            View bolinha = new View(this);
            swatch.addView(bolinha, new MaterialCardView.LayoutParams(
                    MaterialCardView.LayoutParams.MATCH_PARENT,
                    MaterialCardView.LayoutParams.MATCH_PARENT));

            containerCoresIcone.addView(swatch);
        }

        atualizarPreviewCorIcone();
    }

    private void setCoresIconeVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (txtLabelCorIcone != null) txtLabelCorIcone.setVisibility(visibility);
        if (containerCoresIcone != null && containerCoresIcone.getParent() instanceof View) {
            ((View) containerCoresIcone.getParent()).setVisibility(visibility);
        }
    }

    private void selecionarCorIcone(String corHex, boolean atualizarIcone) {
        if (corHex != null && !corHex.trim().isEmpty()) {
            corIconeSelecionada = corHex;
        }

        atualizarPreviewCorIcone();

        if (atualizarIcone) {
            Integer index = viewModel.iconeSelecionado.getValue();
            if (index == null || index == -1) {
                viewModel.selecionarIcone(0);
            } else {
                atualizarVisualGridIcones(index);
            }
        }
    }

    private void atualizarPreviewCorIcone() {
        if (containerCoresIcone == null) return;
        int strokeSelected = ContextCompat.getColor(this, R.color.cadastro_categoria_text_primary);
        int strokeDefault = ContextCompat.getColor(this, R.color.cadastro_categoria_stroke);

        for (int i = 0; i < containerCoresIcone.getChildCount(); i++) {
            View child = containerCoresIcone.getChildAt(i);
            if (!(child instanceof MaterialCardView)) continue;
            MaterialCardView swatch = (MaterialCardView) child;
            String cor = (String) swatch.getTag();
            boolean selecionada = corIconeSelecionada.equalsIgnoreCase(cor);
            swatch.setCardBackgroundColor(Color.TRANSPARENT);
            swatch.setStrokeWidth(dp(selecionada ? 3 : 1));
            swatch.setStrokeColor(selecionada ? strokeSelected : strokeDefault);
            if (swatch.getChildCount() > 0) {
                swatch.getChildAt(0).setBackground(criarDrawableCor(Color.parseColor(cor)));
            }
        }
    }

    private GradientDrawable criarDrawableCor(int cor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(cor);
        return drawable;
    }

    private int corIconeInt() {
        try {
            return Color.parseColor(corIconeSelecionada);
        } catch (IllegalArgumentException e) {
            return ContextCompat.getColor(this, R.color.cadastro_categoria_accent);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // --- HELPERS ---

    private void atualizarTituloHeader() {
        textViewHeader.setText("Nova Categoria");
    }

    private void esconderTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void ajustarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
