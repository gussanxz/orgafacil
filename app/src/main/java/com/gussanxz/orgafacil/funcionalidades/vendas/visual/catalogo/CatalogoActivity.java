package com.gussanxz.orgafacil.funcionalidades.vendas.visual.catalogo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.produtos_e_servicos.CadastroCatalogoActivity;
import com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.AdapterFiltroCategoriasNovaVenda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogoActivity extends AppCompatActivity {

    public static final String ID_TODOS_PRODUTOS = "todos_produtos";

    private RecyclerView rvCategorias;
    private RecyclerView rvGridCategorias;
    private RecyclerView rvGradeProdutos;
    private EditText etBuscarProduto;
    private ChipGroup chipGroupExibicoes;
    private ChipGroup chipGroupFiltroCategorias;
    private ImageButton btnAlternarModo;
    private LinearLayout layoutEstadoVazio;

    private AdapterFiltroCategoriasNovaVenda adapterFiltroCategorias;
    private AdapterCategoriasCatalogo adapterGridCategorias;
    private AdapterItensCatalogo adapterProdutos;

    private final List<Categoria> categoriasBrutas = new ArrayList<>();
    private final List<Categoria> categoriasExibidas = new ArrayList<>();
    private final List<CatalogoModel> catalogoCompleto = new ArrayList<>();
    private final List<CatalogoModel> produtosExibidos = new ArrayList<>();
    private final List<String> ordemCategoriasAtiva = new ArrayList<>();
    private final Map<String, List<String>> ordemItensPorCategoria = new HashMap<>();
    private final Map<String, Boolean> statusCategoriasOriginais = new HashMap<>();
    private final Map<String, Boolean> statusItensOriginais = new HashMap<>();
    private final Map<String, Boolean> statusCategoriasPendentes = new HashMap<>();
    private final Map<String, Boolean> statusItensPendentes = new HashMap<>();

    private final CatalogoRepository catalogoRepository = new CatalogoRepository();
    private final CategoriaCatalogoRepository categoriaRepository = new CategoriaCatalogoRepository();

    private ListenerRegistration listenerCatalogo;
    private ListenerRegistration listenerCategorias;
    private ListenerRegistration listenerOrganizacao;

    private Categoria categoriaAtiva = null;
    private String textoBusca = null;
    private String exibicaoAtiva = CategoriaCatalogoRepository.EXIBICAO_ALFABETICA;
    private boolean modoCategorias = true;
    private boolean atualizandoChipExibicao = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_catalogo);

        vincularViews();
        configurarAdapters();
        configurarBotoes();
        configurarDrag();
    }

    @Override
    protected void onStart() {
        super.onStart();
        iniciarListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerCatalogo != null) listenerCatalogo.remove();
        if (listenerCategorias != null) listenerCategorias.remove();
        if (listenerOrganizacao != null) listenerOrganizacao.remove();
    }

    private void vincularViews() {
        rvCategorias = findViewById(R.id.rvCategorias);
        rvGridCategorias = findViewById(R.id.rvGridCategorias);
        rvGradeProdutos = findViewById(R.id.rvGradeProdutos);
        etBuscarProduto = findViewById(R.id.etBuscarProduto);
        chipGroupExibicoes = findViewById(R.id.chipGroupExibicoes);
        chipGroupFiltroCategorias = findViewById(R.id.chipGroupFiltroCategorias);
        btnAlternarModo = findViewById(R.id.btnAlternarModoExibicao);
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazio);

        View btnVoltar = findViewById(R.id.btnVoltarCatalogo);
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> tratarSaida());
    }

    private void configurarAdapters() {
        rvCategorias.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterFiltroCategorias = new AdapterFiltroCategoriasNovaVenda(categoriasExibidas, this,
                (categoria, position) -> abrirCategoria(categoria));
        rvCategorias.setAdapter(adapterFiltroCategorias);

        rvGridCategorias.setLayoutManager(new GridLayoutManager(this, 3));
        rvGridCategorias.setNestedScrollingEnabled(false);
        adapterGridCategorias = new AdapterCategoriasCatalogo(categoriasExibidas, new AdapterCategoriasCatalogo.OnCategoriaActionListener() {
            @Override public void onCategoriaClick(Categoria categoria) {
                abrirCategoria(categoria);
            }

            @Override public void onStatusChanged(Categoria categoria, boolean ativa) {
                alterarStatusCategoria(categoria, ativa);
            }
        });
        rvGridCategorias.setAdapter(adapterGridCategorias);

        rvGradeProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        rvGradeProdutos.setNestedScrollingEnabled(false);
        adapterProdutos = new AdapterItensCatalogo(produtosExibidos, new AdapterItensCatalogo.OnItemActionListener() {
            @Override public void onItemClick(CatalogoModel item) {
                abrirEdicaoItem(item);
            }

            @Override public void onStatusChanged(CatalogoModel item, boolean ativo) {
                alterarStatusItem(item, ativo);
            }
        });
        rvGradeProdutos.setAdapter(adapterProdutos);
    }

    private void configurarBotoes() {
        if (btnAlternarModo != null) {
            btnAlternarModo.setOnClickListener(v -> {
                modoCategorias = !modoCategorias;
                if (modoCategorias) categoriaAtiva = null;
                publicarTela();
            });
        }

        etBuscarProduto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusca = s.toString().trim().isEmpty() ? null : s.toString().trim();
                if (textoBusca != null) modoCategorias = false;
                publicarTela();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupExibicoes.setOnCheckedChangeListener((group, checkedId) -> {
            if (atualizandoChipExibicao) return;
            categoriaRepository.salvarExibicaoAtiva(exibicaoPorChipId(checkedId), new CategoriaCatalogoRepository.Callback() {
                @Override public void onSucesso(String mensagem) {}
                @Override public void onErro(String erro) {
                    Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        });

        chipGroupFiltroCategorias.setOnCheckedChangeListener((group, checkedId) -> publicarTela());
    }

    private void configurarDrag() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int pos = viewHolder.getAdapterPosition();
                boolean podeArrastar = podeReordenarCategorias() && !isCardTodosProdutos(pos);
                return makeMovementFlags(podeArrastar ? ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT : 0, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int origem = viewHolder.getAdapterPosition();
                int destino = target.getAdapterPosition();
                if (isCardTodosProdutos(origem) || isCardTodosProdutos(destino)) return false;
                Collections.swap(categoriasExibidas, origem, destino);
                adapterGridCategorias.notifyItemMoved(origem, destino);
                adapterFiltroCategorias.notifyDataSetChanged();
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (podeReordenarCategorias()) salvarOrdemCategorias();
            }
        }).attachToRecyclerView(rvGridCategorias);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(podeReordenarItens() ? ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT : 0, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int origem = viewHolder.getAdapterPosition();
                int destino = target.getAdapterPosition();
                if (origem == RecyclerView.NO_POSITION || destino == RecyclerView.NO_POSITION) return false;
                Collections.swap(produtosExibidos, origem, destino);
                adapterProdutos.notifyItemMoved(origem, destino);
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (podeReordenarItens()) salvarOrdemItens();
            }
        }).attachToRecyclerView(rvGradeProdutos);
    }

    private void iniciarListeners() {
        listenerCategorias = categoriaRepository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override public void onNovosDados(List<Categoria> lista) {
                categoriasBrutas.clear();
                categoriasBrutas.addAll(lista);
                atualizarStatusOriginaisCategorias(lista);
                publicarTela();
            }

            @Override public void onErro(String erro) {
                Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });

        listenerCatalogo = catalogoRepository.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override public void onNovosDados(List<CatalogoModel> lista) {
                catalogoCompleto.clear();
                catalogoCompleto.addAll(lista);
                atualizarStatusOriginaisItens(lista);
                publicarTela();
            }

            @Override public void onErro(String erro) {
                Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });

        listenerOrganizacao = categoriaRepository.ouvirOrganizacaoCatalogo(new CategoriaCatalogoRepository.OrganizacaoCallback() {
            @Override
            public void onNovosDados(String novaExibicaoAtiva,
                                     List<String> ordemCategoriaIds,
                                     Map<String, List<String>> novaOrdemItensPorCategoria) {
                exibicaoAtiva = novaExibicaoAtiva;
                ordemCategoriasAtiva.clear();
                ordemCategoriasAtiva.addAll(ordemCategoriaIds);
                ordemItensPorCategoria.clear();
                ordemItensPorCategoria.putAll(novaOrdemItensPorCategoria);
                marcarChipExibicao();
                publicarTela();
            }

            @Override public void onErro(String erro) {
                Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirCategoria(Categoria categoria) {
        categoriaAtiva = ID_TODOS_PRODUTOS.equals(categoria.getId()) ? null : categoria;
        modoCategorias = false;
        publicarTela();
    }

    private void publicarTela() {
        aplicarStatusPendentes();
        publicarCategorias();
        publicarProdutos();
        rvGridCategorias.setVisibility(modoCategorias ? View.VISIBLE : View.GONE);
        rvGradeProdutos.setVisibility(modoCategorias ? View.GONE : View.VISIBLE);
        if (btnAlternarModo != null) {
            btnAlternarModo.setImageResource(modoCategorias ? R.drawable.ic_list_24 : R.drawable.ic_grid_24);
        }
        if (layoutEstadoVazio != null) {
            boolean vazio = modoCategorias ? categoriasExibidas.size() <= 1 : produtosExibidos.isEmpty();
            layoutEstadoVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        }
    }

    private void publicarCategorias() {
        List<Categoria> categoriasFiltradas = filtrarCategoriasPorStatus(categoriasBrutas);
        List<Categoria> ordenadas = CategoriaCatalogoRepository.ordenarParaExibicao(
                categoriasFiltradas,
                exibicaoAtiva,
                ordemCategoriasAtiva);

        categoriasExibidas.clear();
        categoriasExibidas.addAll(ordenadas);
        categoriasExibidas.add(criarTodosProdutos());
        adapterFiltroCategorias.notifyDataSetChanged();
        adapterGridCategorias.notifyDataSetChanged();
    }

    private void publicarProdutos() {
        produtosExibidos.clear();
        for (CatalogoModel item : catalogoCompleto) {
            if (categoriaAtiva != null && !categoriaAtiva.getId().equals(item.getCategoriaId())) continue;
            if (categoriaAtiva == null && !itemPassaFiltroStatus(item)) continue;
            if (textoBusca != null && !item.getNome().toLowerCase().contains(textoBusca.toLowerCase())) continue;
            produtosExibidos.add(item);
        }
        produtosExibidos.sort(this::compararItens);
        adapterProdutos.notifyDataSetChanged();
    }

    private List<Categoria> filtrarCategoriasPorStatus(List<Categoria> categorias) {
        int chipId = chipGroupFiltroCategorias.getCheckedChipId();
        List<Categoria> filtradas = new ArrayList<>();
        for (Categoria categoria : categorias) {
            if (chipId == R.id.chipAtivas && !categoria.isAtiva()) continue;
            if (chipId == R.id.chipInativas && categoria.isAtiva()) continue;
            filtradas.add(categoria);
        }
        return filtradas;
    }

    private boolean itemPassaFiltroStatus(CatalogoModel item) {
        int chipId = chipGroupFiltroCategorias.getCheckedChipId();
        if (chipId == R.id.chipAtivas) return item.isStatusAtivo();
        if (chipId == R.id.chipInativas) return !item.isStatusAtivo();
        return true;
    }

    private Categoria criarTodosProdutos() {
        Categoria todos = new Categoria();
        todos.setId(ID_TODOS_PRODUTOS);
        todos.setNome("Todos os produtos");
        todos.setAtiva(true);
        return todos;
    }

    private int compararItens(CatalogoModel itemA, CatalogoModel itemB) {
        int catA = ordemCategoriasAtiva.indexOf(itemA.getCategoriaId());
        int catB = ordemCategoriasAtiva.indexOf(itemB.getCategoriaId());
        catA = catA >= 0 ? catA : Integer.MAX_VALUE;
        catB = catB >= 0 ? catB : Integer.MAX_VALUE;
        if (catA != catB) return Integer.compare(catA, catB);

        int itemPosA = indiceItem(itemA);
        int itemPosB = indiceItem(itemB);
        if (itemPosA != itemPosB) return Integer.compare(itemPosA, itemPosB);
        return itemA.getNome().compareToIgnoreCase(itemB.getNome());
    }

    private int indiceItem(CatalogoModel item) {
        List<String> ordem = ordemItensPorCategoria.get(item.getCategoriaId());
        if (ordem == null) return Integer.MAX_VALUE;
        int index = ordem.indexOf(item.getId());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private boolean podeReordenarCategorias() {
        return modoCategorias
                && textoBusca == null
                && chipGroupFiltroCategorias.getCheckedChipId() == R.id.chipTodas
                && CategoriaCatalogoRepository.isExibicaoPersonalizada(exibicaoAtiva);
    }

    private boolean podeReordenarItens() {
        return !modoCategorias
                && textoBusca == null
                && categoriaAtiva != null
                && CategoriaCatalogoRepository.isExibicaoPersonalizada(exibicaoAtiva);
    }

    private void salvarOrdemCategorias() {
        List<Categoria> semTodos = new ArrayList<>();
        for (Categoria categoria : categoriasExibidas) {
            if (!ID_TODOS_PRODUTOS.equals(categoria.getId())) semTodos.add(categoria);
        }
        categoriaRepository.salvarOrdemExibicao(exibicaoAtiva, semTodos, callbackSilencioso());
    }

    private boolean isCardTodosProdutos(int position) {
        return position < 0
                || position >= categoriasExibidas.size()
                || ID_TODOS_PRODUTOS.equals(categoriasExibidas.get(position).getId());
    }

    private void alterarStatusCategoria(Categoria categoria, boolean ativa) {
        if (ID_TODOS_PRODUTOS.equals(categoria.getId())) return;
        Boolean statusOriginal = statusCategoriasOriginais.get(categoria.getId());
        if (statusOriginal != null && statusOriginal == ativa) {
            statusCategoriasPendentes.remove(categoria.getId());
        } else {
            statusCategoriasPendentes.put(categoria.getId(), ativa);
        }
        categoria.setAtiva(ativa);
        publicarTela();
    }

    private void alterarStatusItem(CatalogoModel item, boolean ativo) {
        Boolean statusOriginal = statusItensOriginais.get(item.getId());
        if (statusOriginal != null && statusOriginal == ativo) {
            statusItensPendentes.remove(item.getId());
        } else {
            statusItensPendentes.put(item.getId(), ativo);
        }
        item.setStatusAtivo(ativo);
        publicarTela();
    }

    private void salvarOrdemItens() {
        if (categoriaAtiva == null) return;
        List<String> ids = new ArrayList<>();
        for (CatalogoModel item : produtosExibidos) {
            ids.add(item.getId());
        }
        categoriaRepository.salvarOrdemItensExibicao(
                exibicaoAtiva,
                categoriaAtiva.getId(),
                ids,
                callbackSilencioso());
    }

    private CategoriaCatalogoRepository.Callback callbackSilencioso() {
        return new CategoriaCatalogoRepository.Callback() {
            @Override public void onSucesso(String mensagem) {}
            @Override public void onErro(String erro) {
                Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void abrirEdicaoItem(CatalogoModel c) {
        Intent intent = new Intent(this, CadastroCatalogoActivity.class);
        intent.putExtra("id", c.getId());
        intent.putExtra("nome", c.getNome());
        intent.putExtra("tipo", c.getTipoStr());
        intent.putExtra("preco", c.getPreco());
        intent.putExtra("categoriaId", c.getCategoriaId());
        intent.putExtra("categoria", c.getCategoria());
        intent.putExtra("descricao", c.getDescricao());
        intent.putExtra("statusAtivo", c.isStatusAtivo());
        intent.putExtra("iconeIndex", c.getIconeIndex());
        intent.putExtra("urlFoto", c.getUrlFoto());
        startActivity(intent);
    }

    private void marcarChipExibicao() {
        int chipId = chipIdPorExibicao(exibicaoAtiva);
        if (chipGroupExibicoes.getCheckedChipId() == chipId) return;
        atualizandoChipExibicao = true;
        chipGroupExibicoes.check(chipId);
        atualizandoChipExibicao = false;
    }

    private int chipIdPorExibicao(String exibicao) {
        switch (CategoriaCatalogoRepository.normalizarExibicao(exibicao)) {
            case CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_1: return R.id.chipExibicao1;
            case CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_2: return R.id.chipExibicao2;
            case CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_3: return R.id.chipExibicao3;
            case CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_4: return R.id.chipExibicao4;
            case CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_5: return R.id.chipExibicao5;
            default: return R.id.chipExibicaoAlfabetica;
        }
    }

    private String exibicaoPorChipId(int chipId) {
        if (chipId == R.id.chipExibicao1) return CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_1;
        if (chipId == R.id.chipExibicao2) return CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_2;
        if (chipId == R.id.chipExibicao3) return CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_3;
        if (chipId == R.id.chipExibicao4) return CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_4;
        if (chipId == R.id.chipExibicao5) return CategoriaCatalogoRepository.EXIBICAO_PERSONALIZADA_5;
        return CategoriaCatalogoRepository.EXIBICAO_ALFABETICA;
    }

    @Override
    public void onBackPressed() {
        tratarSaida();
    }

    private void tratarSaida() {
        if (!modoCategorias) {
            modoCategorias = true;
            categoriaAtiva = null;
            publicarTela();
            return;
        }
        if (!temAlteracoesPendentes()) {
            finish();
            return;
        }
        exibirDialogAlteracoesPendentes();
    }

    private boolean temAlteracoesPendentes() {
        return !statusCategoriasPendentes.isEmpty() || !statusItensPendentes.isEmpty();
    }

    private void exibirDialogAlteracoesPendentes() {
        new AlertDialog.Builder(this)
                .setTitle("Salvar alteracoes?")
                .setMessage(montarResumoAlteracoes())
                .setPositiveButton("Salvar", (dialog, which) -> salvarAlteracoesPendentes())
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    statusCategoriasPendentes.clear();
                    statusItensPendentes.clear();
                    finish();
                })
                .setNeutralButton("Voltar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private CharSequence montarResumoAlteracoes() {
        SpannableStringBuilder resumo = new SpannableStringBuilder("Historico de alteracoes:\n");
        for (Map.Entry<String, Boolean> entry : statusCategoriasPendentes.entrySet()) {
            Categoria categoria = encontrarCategoria(entry.getKey());
            Boolean anterior = statusCategoriasOriginais.get(entry.getKey());
            if (categoria == null || anterior == null) continue;
            appendAlteracao(
                    resumo,
                    "Status da categoria " + categoria.getNome(),
                    rotuloCategoriaStatus(anterior),
                    rotuloCategoriaStatus(entry.getValue()));
        }
        for (Map.Entry<String, Boolean> entry : statusItensPendentes.entrySet()) {
            CatalogoModel item = encontrarItem(entry.getKey());
            Boolean anterior = statusItensOriginais.get(entry.getKey());
            if (item == null || anterior == null) continue;
            appendAlteracao(
                    resumo,
                    "Status do " + (item.isServico() ? "servico " : "produto ") + item.getNome(),
                    rotuloItemStatus(anterior),
                    rotuloItemStatus(entry.getValue()));
        }
        resumo.append("\nSalvar aplica as alteracoes. Cancelar descarta tudo.");
        return resumo;
    }

    private void appendAlteracao(@NonNull SpannableStringBuilder resumo,
                                 @NonNull String campo,
                                 @NonNull String anterior,
                                 @NonNull String novo) {
        resumo.append(campo).append(": ");
        int inicio = resumo.length();
        resumo.append(anterior);
        resumo.setSpan(new StrikethroughSpan(), inicio, resumo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        resumo.append(" -> ").append(novo).append("\n");
    }

    private void salvarAlteracoesPendentes() {
        int total = statusCategoriasPendentes.size() + statusItensPendentes.size();
        if (total == 0) {
            finish();
            return;
        }

        final int[] restantes = { total };
        final boolean[] falhou = { false };
        CategoriaCatalogoRepository.Callback callback = new CategoriaCatalogoRepository.Callback() {
            @Override public void onSucesso(String mensagem) {
                concluirSavePendente(restantes, falhou);
            }

            @Override public void onErro(String erro) {
                falhou[0] = true;
                Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                concluirSavePendente(restantes, falhou);
            }
        };

        for (Map.Entry<String, Boolean> entry : new HashMap<>(statusCategoriasPendentes).entrySet()) {
            Categoria categoria = encontrarCategoria(entry.getKey());
            Boolean anterior = statusCategoriasOriginais.get(entry.getKey());
            if (categoria == null || anterior == null) {
                concluirSavePendente(restantes, falhou);
                continue;
            }
            categoriaRepository.atualizarStatus(categoria, anterior, entry.getValue(), callback);
        }

        for (Map.Entry<String, Boolean> entry : new HashMap<>(statusItensPendentes).entrySet()) {
            CatalogoModel item = encontrarItem(entry.getKey());
            Boolean anterior = statusItensOriginais.get(entry.getKey());
            if (item == null || anterior == null) {
                concluirSavePendente(restantes, falhou);
                continue;
            }
            catalogoRepository.atualizarStatus(item, anterior, entry.getValue(), new CatalogoRepository.Callback() {
                @Override public void onSucesso(String mensagem) {
                    concluirSavePendente(restantes, falhou);
                }

                @Override public void onErro(String erro) {
                    falhou[0] = true;
                    Toast.makeText(CatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    concluirSavePendente(restantes, falhou);
                }
            });
        }
    }

    private void concluirSavePendente(int[] restantes, boolean[] falhou) {
        restantes[0]--;
        if (restantes[0] > 0) return;
        if (falhou[0]) {
            publicarTela();
            return;
        }
        statusCategoriasPendentes.clear();
        statusItensPendentes.clear();
        finish();
    }

    private void atualizarStatusOriginaisCategorias(List<Categoria> categorias) {
        Map<String, Boolean> atualizados = new HashMap<>();
        for (Categoria categoria : categorias) {
            if (categoria.getId() != null) atualizados.put(categoria.getId(), categoria.isAtiva());
        }
        statusCategoriasOriginais.clear();
        statusCategoriasOriginais.putAll(atualizados);
        statusCategoriasPendentes.keySet().removeIf(id -> !atualizados.containsKey(id));
    }

    private void atualizarStatusOriginaisItens(List<CatalogoModel> itens) {
        Map<String, Boolean> atualizados = new HashMap<>();
        for (CatalogoModel item : itens) {
            if (item.getId() != null) atualizados.put(item.getId(), item.isStatusAtivo());
        }
        statusItensOriginais.clear();
        statusItensOriginais.putAll(atualizados);
        statusItensPendentes.keySet().removeIf(id -> !atualizados.containsKey(id));
    }

    private void aplicarStatusPendentes() {
        for (Categoria categoria : categoriasBrutas) {
            Boolean pendente = statusCategoriasPendentes.get(categoria.getId());
            if (pendente != null) categoria.setAtiva(pendente);
        }
        for (CatalogoModel item : catalogoCompleto) {
            Boolean pendente = statusItensPendentes.get(item.getId());
            if (pendente != null) item.setStatusAtivo(pendente);
        }
    }

    private Categoria encontrarCategoria(String id) {
        for (Categoria categoria : categoriasBrutas) {
            if (id.equals(categoria.getId())) return categoria;
        }
        return null;
    }

    private CatalogoModel encontrarItem(String id) {
        for (CatalogoModel item : catalogoCompleto) {
            if (id.equals(item.getId())) return item;
        }
        return null;
    }

    private String rotuloCategoriaStatus(boolean ativa) {
        return ativa ? "Ativa" : "Inativa";
    }

    private String rotuloItemStatus(boolean ativo) {
        return ativo ? "Ativo" : "Inativo";
    }
}
