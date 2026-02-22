package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView txtTituloSelecao;

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

            String urlFoto = intent.getStringExtra("urlImagem");

            // Se tiver foto antiga (URL), carregar com Glide
            if (urlFoto != null && !urlFoto.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(urlFoto)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(imgBtnGaleria); // Carrega no botão pequeno apenas visualmente

                // Ajustes visuais manuais pois o Glide é assíncrono
                imgBtnGaleria.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cardBtnGaleria.setStrokeColor(Color.parseColor("#2196F3"));
                cardBtnGaleria.setStrokeWidth(4);
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
                switchAtiva.isChecked()
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
        cardBtnGaleria.setStrokeColor(Color.parseColor("#2196F3"));
        cardBtnGaleria.setStrokeWidth(4);
        imgBtnGaleria.setColorFilter(Color.parseColor("#2196F3"));

        // 2. Atualiza o PREVIEW GRANDE com a foto real
        if (imgPreviewLarge != null) {
            imgPreviewLarge.setImageURI(uri);
            imgPreviewLarge.setImageTintList(null); // Remove tint
            imgPreviewLarge.setColorFilter(null);   // Remove filtro cinza
            imgPreviewLarge.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        // 3. Controles de Visibilidade
        if (layoutSelecao != null) layoutSelecao.setVisibility(View.VISIBLE);
        if (cardContainerGrid != null) cardContainerGrid.setVisibility(View.GONE);
        if (cardPreviewFoto != null) cardPreviewFoto.setVisibility(View.VISIBLE);
        if (txtTituloSelecao != null) txtTituloSelecao.setText("Pré-visualização:");

        // 4. Scroll Automático
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void resetarVisualGaleria() {
        // Volta o botão da galeria para o estado inativo (cinza)
        cardBtnGaleria.setStrokeColor(Color.parseColor("#E0E0E0"));
        cardBtnGaleria.setStrokeWidth(1);
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

    private void resetarVisualGridIcones() {
        atualizarVisualGridIcones(-1);
        imgBtnSelecionarIcones.setColorFilter(Color.parseColor("#757575"));
    }

    private void configurarCliquesGridIcones() {
        if (containerIcones == null) return;
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            int finalI = i;
            containerIcones.getChildAt(i).setOnClickListener(v -> viewModel.selecionarIcone(finalI));
        }
    }

    // --- HELPERS ---

    private void atualizarTituloHeader() {
        String titulo = "Nova Categoria";
        if (viewModel.getTipoCategoria() != null) {
            switch (viewModel.getTipoCategoria()) {
                case RECEITA:
                    titulo = "Nova Categoria de Receita";
                    break;
                case PRODUTO:
                    titulo = "Nova Categoria de Produto";
                    break;
                case SERVICO:
                    titulo = "Nova Categoria de Serviço";
                    break;
                case DESPESA:
                    titulo = "Nova Categoria de Despesa";
                    break;
            }
        }
        textViewHeader.setText(titulo);
    }

    private String obterNomeTipoBonito() {
        return textViewHeader.getText().toString().replace("Nova ", "").replace("Editar ", "");
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