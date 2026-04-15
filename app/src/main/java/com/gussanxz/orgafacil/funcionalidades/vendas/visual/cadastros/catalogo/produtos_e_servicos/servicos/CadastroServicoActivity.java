package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.servicos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.model.CatalogoModel;

import java.util.ArrayList;
import java.util.List;

public class CadastroServicoActivity extends AppCompatActivity {

    // Componentes UI
    private FloatingActionButton fabVoltar, fabSalvar;
    private TextInputLayout textInputCategoria, textInputDescricao, textInputValor;
    // Futuro: private TextInputLayout textInputTempoDuracao;
    private TextView textViewHeader;
    private com.google.android.material.card.MaterialCardView cardBtnFoto;

    // Lógica
    private CatalogoRepository repository;
    private String idEmEdicao = null;
    private String categoriaSelecionadaId   = CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO;
    private String categoriaSelecionadaNome = CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO;
    private CategoriaCatalogoRepository categoriaRepo;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private ListenerRegistration listenerCategorias;
    private String urlFotoAtual = null;
    private Uri imagemSelecionadaUri = null;
    private Uri uriCameraTemp;
    private final ActivityResultLauncher<Intent> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) atualizarVisualFoto(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> launcherCamera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            sucesso -> {
                if (sucesso && uriCameraTemp != null) atualizarVisualFoto(uriCameraTemp);
            }
    );

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
        repository = new CatalogoRepository();
        categoriaRepo = new CategoriaCatalogoRepository();

        // 2. Inicializa UI
        inicializarComponentes();
        carregarCategorias();
        configurarCliquesCategoria();

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
        cardBtnFoto = findViewById(R.id.cardBtnFoto);
        textViewHeader.setText("Novo Serviço");
        textInputCategoria.getEditText().setText(categoriaSelecionadaNome);

        if (cardBtnFoto != null)
            cardBtnFoto.setOnClickListener(v -> exibirDialogFonte());
    }

    private void prepararEdicao(Bundle dados) {
        textViewHeader.setText("Editar Serviço");
        categoriaSelecionadaId   = dados.getString("categoriaId", CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO);
        categoriaSelecionadaNome = dados.getString("categoria",   CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);
        urlFotoAtual = dados.getString("urlFoto", null);
        idEmEdicao = dados.getString("id");
        if (cardBtnFoto != null)
            cardBtnFoto.post(() -> inicializarVisualFoto(urlFotoAtual));

        // Preenche campos com segurança (verifica null)
        if(textInputDescricao.getEditText() != null)
            textInputDescricao.getEditText().setText(dados.getString("nome")); // ou "descricao", dependendo de como enviou na Intent

        if(textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setText(dados.getString("categoria"));

        if(textInputValor.getEditText() != null) {
            // Busca o objeto bruto para verificar se é Integer ou Double
            Object precoObj = dados.get("preco");
            double precoParaExibir = 0.0;

            if (precoObj instanceof Integer) {
                // Novo padrão: Centavos (int) -> Divide por 100 para exibir R$
                precoParaExibir = ((Integer) precoObj) / 100.0;
            } else if (precoObj instanceof Double) {
                // Padrão antigo: Reais (double) -> Usa direto
                precoParaExibir = (Double) precoObj;
            }

            // Formata com 2 casas decimais para manter o padrão visual de moeda
            textInputValor.getEditText().setText(String.format(java.util.Locale.US, "%.2f", precoParaExibir));
        }

        if (textInputCategoria != null && textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setText(categoriaSelecionadaNome);
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
        int valorCentavos = 0;
        try {
            double valorTemp = Double.parseDouble(valorStr.replace(",", "."));
            valorCentavos = (int) Math.round(valorTemp * 100);
        } catch (NumberFormatException e) {
            textInputValor.setError("Valor inválido");
            return;
        }

        // 4. Cria Objeto
        CatalogoModel servicoModel = new CatalogoModel();
        servicoModel.setId(idEmEdicao);
        servicoModel.setTipo(CatalogoModel.TIPO_STR_SERVICO);
        servicoModel.setNome(descricao);
        servicoModel.setPreco(valorCentavos);
        servicoModel.setCategoriaId(categoriaSelecionadaId);
        servicoModel.setCategoria(categoriaSelecionadaNome);
        servicoModel.setStatusAtivo(true);

        if (imagemSelecionadaUri == null && urlFotoAtual != null)
            servicoModel.setUrlFoto(urlFotoAtual);

        // 5. Chama Repositório
        repository.salvar(servicoModel, imagemSelecionadaUri, new CatalogoRepository.Callback() {
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
    private void configurarCliquesCategoria() {
        if (textInputCategoria == null) return;
        // Clique em qualquer parte do campo abre o dialog
        textInputCategoria.setOnClickListener(v -> exibirDialogCategorias());
        textInputCategoria.setEndIconOnClickListener(v -> exibirDialogCategorias());
        if (textInputCategoria.getEditText() != null)
            textInputCategoria.getEditText().setOnClickListener(v -> exibirDialogCategorias());
    }

    private void carregarCategorias() {
        listenerCategorias = categoriaRepo.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategorias.clear();
                // "Não alocado" sempre aparece primeiro
                Categoria padrao = new Categoria();
                padrao.setId(CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO);
                padrao.setNome(CategoriaCatalogoRepository.NOME_CATEGORIA_PADRAO);
                listaCategorias.add(padrao);
                for (Categoria c : lista) {
                    if (!CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO.equals(c.getId()))
                        listaCategorias.add(c);
                }
            }
            @Override
            public void onErro(String erro) { /* silencioso */ }
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
                .setTitle("Categoria do Serviço")
                .setItems(nomes, (dialog, which) -> {
                    Categoria c = listaCategorias.get(which);
                    categoriaSelecionadaId   = c.getId();
                    categoriaSelecionadaNome = c.getNome();
                    if (textInputCategoria != null && textInputCategoria.getEditText() != null)
                        textInputCategoria.getEditText().setText(c.getNome());
                })
                .show();
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

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcherGaleria.launch(intent);
    }

    private void abrirCamera() {
        java.io.File arquivo = new java.io.File(getCacheDir(), "foto_temp_" + System.currentTimeMillis() + ".jpg");
        uriCameraTemp = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", arquivo);
        launcherCamera.launch(uriCameraTemp);
    }

    private void atualizarVisualFoto(Uri uri) {
        imagemSelecionadaUri = uri;
        if (cardBtnFoto != null) {
            // Mostra a foto dentro do card usando Glide
            android.widget.ImageView imgPreview = cardBtnFoto.findViewById(R.id.imgBtnFoto);
            if (imgPreview != null) {
                com.bumptech.glide.Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(imgPreview);
            }
            cardBtnFoto.setStrokeColor(android.graphics.Color.parseColor("#2196F3"));
            cardBtnFoto.setStrokeWidth(4);
        }
        Toast.makeText(this, "Foto selecionada", Toast.LENGTH_SHORT).show();
    }

    private void inicializarVisualFoto(String urlFotoExistente) {
        if (urlFotoExistente == null || urlFotoExistente.isEmpty()) return;
        if (cardBtnFoto == null) return;
        android.widget.ImageView imgPreview = cardBtnFoto.findViewById(R.id.imgBtnFoto);
        if (imgPreview != null) {
            com.bumptech.glide.Glide.with(this)
                    .load(urlFotoExistente)
                    .centerCrop()
                    .placeholder(R.drawable.ic_camera_alt_120)
                    .into(imgPreview);
        }
        cardBtnFoto.setStrokeColor(android.graphics.Color.parseColor("#2196F3"));
        cardBtnFoto.setStrokeWidth(4);
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
}