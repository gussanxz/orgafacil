package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.CatalogoModel;
import com.gussanxz.orgafacil.util_helper.SwipeCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListaCategoriasCatalogoActivity extends AppCompatActivity implements AdapterItemListaCategoriasCatalogoVendas.OnCategoriaActionListener {

    private RecyclerView recyclerCategorias;
    private LinearLayout emptyState;
    private TextInputEditText editBusca;
    private ChipGroup chipGroupFiltro;
    private Chip chipTodas, chipAtivas, chipInativas;
    private TextView txtSubtituloCategorias;

    private AdapterItemListaCategoriasCatalogoVendas adapter;
    private final List<Categoria> listaCategoriasTotal = new ArrayList<>();
    private final List<Categoria> listaFiltrada = new ArrayList<>();
    private final Map<String, Integer> quantidadeItensPorCategoria = new HashMap<>();

    private CategoriaCatalogoRepository repository;
    private CatalogoRepository catalogoRepository;
    private ListenerRegistration listenerRegistration;
    private ListenerRegistration listenerCatalogoRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main_vendas_opd_lista_categorias);

        repository = new CategoriaCatalogoRepository();
        catalogoRepository = new CatalogoRepository();
        repository.garantirCategoriaPadrao(new CategoriaCatalogoRepository.Callback() {
            @Override public void onSucesso(String mensagem) {}
            @Override public void onErro(String erro) {}
        });

        inicializarComponentes();
        configurarRecyclerView();
        configurarListenerDeFiltro();
    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarCategoriasEmTempoReal();
        recuperarQuantidadesEmTempoReal();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        if (listenerCatalogoRegistration != null) {
            listenerCatalogoRegistration.remove();
            listenerCatalogoRegistration = null;
        }
    }

    private void confirmarExclusao(Categoria categoria, int positionParaRestaurar) {
        restaurarItemSeNecessario(positionParaRestaurar);

        String motivoBloqueio = getMotivoBloqueioExclusao(categoria);
        if (motivoBloqueio != null) {
            exibirDialogExclusaoBloqueada(motivoBloqueio);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Excluir categoria")
                .setMessage("Tem certeza que deseja excluir " + categoria.getNome() + "? Esta ação não pode ser desfeita.")
                .setCancelable(false)
                .setPositiveButton("Excluir", (dialog, which) -> repository.excluir(categoria.getId(), new CategoriaCatalogoRepository.Callback() {
                    @Override
                    public void onSucesso(String mensagem) {
                        Toast.makeText(ListaCategoriasCatalogoActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String erro) {
                        exibirDialogExclusaoBloqueada(erro);
                    }
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String getMotivoBloqueioExclusao(Categoria categoria) {
        if (isCategoriaPadrao(categoria)) {
            return "A categoria Não alocado é usada como backup para produtos e serviços sem categoria definida e não pode ser excluída.";
        }

        int quantidadeItens = getQuantidadeItens(categoria);
        if (quantidadeItens > 0) {
            return "Esta categoria possui " + quantidadeItens + " produto(s) ou serviço(s) cadastrado(s). Remova ou altere a categoria desses itens antes de excluir.";
        }

        return null;
    }

    private void exibirDialogExclusaoBloqueada(String mensagem) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exclusao_bloqueada_categoria, null);
        TextView textMensagem = view.findViewById(R.id.textMensagemDialog);
        View btnEntendi = view.findViewById(R.id.btnEntendiDialog);

        textMensagem.setText(mensagem);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnEntendi.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void restaurarItemSeNecessario(int positionParaRestaurar) {
        if (positionParaRestaurar != -1 && adapter != null) {
            recyclerCategorias.post(() -> adapter.notifyItemChanged(positionParaRestaurar));
        }
    }

    @Override
    public void onEditarClick(Categoria categoria) {
        abrirTelaEdicao(categoria);
    }

    @Override
    public void onExcluirClick(Categoria categoria) {
        confirmarExclusao(categoria, -1);
    }

    private void configurarRecyclerView() {
        adapter = new AdapterItemListaCategoriasCatalogoVendas(listaFiltrada, this, this);
        adapter.atualizarQuantidades(quantidadeItensPorCategoria);
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategorias.setAdapter(adapter);

        SwipeCallback swipeHelper = new SwipeCallback(this) {
            @Override
            protected void onMovimentoSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                             int direction,
                                             int position) {
                Categoria categoriaSelecionada = listaFiltrada.get(position);
                if (direction == ItemTouchHelper.LEFT) {
                    confirmarExclusao(categoriaSelecionada, position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position);
                    abrirTelaEdicao(categoriaSelecionada);
                }
            }
        };

        new ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerCategorias);
    }

    private void abrirTelaEdicao(Categoria categoria) {
        Intent intent = new Intent(this, CadastroCategoriaCatalogoActivity.class);
        intent.putExtra("modoEditar", true);
        intent.putExtra("idCategoria", categoria.getId());
        intent.putExtra("tipo", "PRODUTO");
        intent.putExtra("nome", categoria.getNome());
        intent.putExtra("descricao", categoria.getDescricao());
        intent.putExtra("iconeIndex", categoria.getIndexIcone());
        intent.putExtra("corIcone", categoria.getCorIcone());
        intent.putExtra("ativa", categoria.isAtiva());
        intent.putExtra("urlImagem", categoria.getUrlImagem());
        startActivity(intent);
    }

    public void acessarCadastroCategoria(View view) {
        Intent intent = new Intent(this, CadastroCategoriaCatalogoActivity.class);
        intent.putExtra("tipo", "PRODUTO");
        startActivity(intent);
    }

    private void recuperarCategoriasEmTempoReal() {
        listenerRegistration = repository.listarTempoReal(new CategoriaCatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<Categoria> lista) {
                listaCategoriasTotal.clear();
                listaCategoriasTotal.addAll(lista);
                filtrarDados();
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaCategoriasCatalogoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recuperarQuantidadesEmTempoReal() {
        listenerCatalogoRegistration = catalogoRepository.listarTempoReal(new CatalogoRepository.ListaCallback() {
            @Override
            public void onNovosDados(List<CatalogoModel> lista) {
                quantidadeItensPorCategoria.clear();
                for (CatalogoModel item : lista) {
                    String categoriaId = item.getCategoriaId();
                    if (categoriaId == null || categoriaId.trim().isEmpty()) {
                        categoriaId = CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO;
                    }

                    int quantidadeAtual = quantidadeItensPorCategoria.containsKey(categoriaId)
                            ? quantidadeItensPorCategoria.get(categoriaId)
                            : 0;
                    quantidadeItensPorCategoria.put(categoriaId, quantidadeAtual + 1);
                }

                if (adapter != null) {
                    adapter.atualizarQuantidades(quantidadeItensPorCategoria);
                }
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(ListaCategoriasCatalogoActivity.this,
                        "Erro ao carregar itens: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarDados() {
        String texto = editBusca.getText() != null ? editBusca.getText().toString().toLowerCase() : "";
        int chipId = chipGroupFiltro.getCheckedChipId();

        listaFiltrada.clear();
        for (Categoria c : listaCategoriasTotal) {
            boolean matchTexto = c.getNome().toLowerCase().contains(texto)
                    || (c.getDescricao() != null && c.getDescricao().toLowerCase().contains(texto));

            boolean matchStatus = true;
            if (chipId == R.id.chipAtivas) matchStatus = c.isAtiva();
            else if (chipId == R.id.chipInativas) matchStatus = !c.isAtiva();

            if (matchTexto && matchStatus) listaFiltrada.add(c);
        }

        ordenarCategoriasParaExibicao(listaFiltrada);
        adapter.atualizarLista(listaFiltrada);
        atualizarResumo();
        atualizarEmptyState(listaFiltrada.isEmpty());
    }

    private void ordenarCategoriasParaExibicao(List<Categoria> categorias) {
        Collections.sort(categorias, (a, b) -> {
            boolean aPadrao = isCategoriaPadrao(a);
            boolean bPadrao = isCategoriaPadrao(b);
            if (aPadrao && !bPadrao) return 1;
            if (!aPadrao && bPadrao) return -1;

            String nomeA = a.getNome() != null ? a.getNome() : "";
            String nomeB = b.getNome() != null ? b.getNome() : "";
            return nomeA.compareToIgnoreCase(nomeB);
        });
    }

    private void atualizarResumo() {
        int ativas = 0;
        int inativas = 0;
        for (Categoria categoria : listaCategoriasTotal) {
            if (categoria.isAtiva()) ativas++;
            else inativas++;
        }

        chipTodas.setText("Todas \u00b7 " + listaCategoriasTotal.size());
        chipAtivas.setText("Ativas \u00b7 " + ativas);
        chipInativas.setText("Inativas \u00b7 " + inativas);
        txtSubtituloCategorias.setText(ativas + (ativas == 1 ? " ativa" : " ativas"));
    }

    private void atualizarEmptyState(boolean estaVazia) {
        recyclerCategorias.setVisibility(estaVazia ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(estaVazia ? View.VISIBLE : View.GONE);
    }

    private boolean isCategoriaPadrao(Categoria categoria) {
        return categoria != null
                && CategoriaCatalogoRepository.ID_CATEGORIA_PADRAO.equals(categoria.getId());
    }

    private int getQuantidadeItens(Categoria categoria) {
        if (categoria == null || categoria.getId() == null || categoria.getId().trim().isEmpty()) {
            return 0;
        }

        Integer quantidade = quantidadeItensPorCategoria.get(categoria.getId());
        return quantidade != null ? quantidade : 0;
    }

    private void configurarListenerDeFiltro() {
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrarDados(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupFiltro.setOnCheckedChangeListener((group, checkedId) -> filtrarDados());
    }

    private void inicializarComponentes() {
        recyclerCategorias = findViewById(R.id.recyclerCategorias);
        editBusca = findViewById(R.id.editBusca);
        chipGroupFiltro = findViewById(R.id.chipGroupFiltroStatus);
        chipTodas = findViewById(R.id.chipTodas);
        chipAtivas = findViewById(R.id.chipAtivas);
        chipInativas = findViewById(R.id.chipInativas);
        txtSubtituloCategorias = findViewById(R.id.txtSubtituloCategorias);
        emptyState = findViewById(R.id.emptyState);
    }

    public void retornarParaVendasCadastros(View view) {
        finish();
    }
}
