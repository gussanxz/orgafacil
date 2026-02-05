package com.gussanxz.orgafacil.funcionalidades.vendas.visual.cadastros.catalogo.categoria;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.gussanxz.orgafacil.funcionalidades.comum.negocio.modelos.Categoria;
// IMPORTANTE: Usando o Repository novo de Catálogo (Vendas)
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.CategoriaCatalogoRepository;

public class CadastroCategoriaVendasViewModel extends ViewModel {

    private final CategoriaCatalogoRepository repository;

    // Estados Observáveis
    private final MutableLiveData<Integer> _iconeSelecionado = new MutableLiveData<>(0);
    public LiveData<Integer> iconeSelecionado = _iconeSelecionado;

    private final MutableLiveData<Uri> _imagemUri = new MutableLiveData<>(null);
    public LiveData<Uri> imagemUri = _imagemUri;

    private final MutableLiveData<String> _mensagemSucesso = new MutableLiveData<>();
    public LiveData<String> mensagemSucesso = _mensagemSucesso;

    private final MutableLiveData<String> _mensagemErro = new MutableLiveData<>();
    public LiveData<String> mensagemErro = _mensagemErro;

    // Novo: Estado de carregamento para travar botão salvar/excluir
    private final MutableLiveData<Boolean> _carregando = new MutableLiveData<>(false);
    public LiveData<Boolean> carregando = _carregando;

    // Dados Internos
    private Categoria.Tipo tipoCategoria = Categoria.Tipo.DESPESA;
    private String idEdicao = null;

    public CadastroCategoriaVendasViewModel() {
        this.repository = new CategoriaCatalogoRepository();
    }

    // --- AÇÕES VISUAIS ---
    public void selecionarIcone(int index) {
        _iconeSelecionado.setValue(index);
        _imagemUri.setValue(null);
    }

    public void selecionarFoto(Uri uri) {
        _imagemUri.setValue(uri);
        _iconeSelecionado.setValue(-1);
    }

    // --- CONFIGURAÇÃO ---
    public void definirContexto(String tipoString) {
        if (tipoString != null) {
            try {
                this.tipoCategoria = Categoria.Tipo.valueOf(tipoString);
            } catch (Exception e) {
                this.tipoCategoria = Categoria.Tipo.DESPESA;
            }
        }
    }

    public void carregarDadosEdicao(String id, String nome, String desc, boolean ativa, String urlFoto, int indexIcone) {
        this.idEdicao = id;
        // Na edição, se tem URL de foto, marcamos icone como -1 para a UI saber que é foto
        if (urlFoto != null && !urlFoto.isEmpty()) {
            _iconeSelecionado.setValue(-1);
            // Obs: _imagemUri continua null pois é uma URL remota, a Activity trata de exibir via Glide/Picasso
        } else {
            selecionarIcone(indexIcone);
        }
    }

    public Categoria.Tipo getTipoCategoria() {
        return tipoCategoria;
    }

    // --- SALVAR ---
    public void salvar(String nome, String descricao, boolean ativa) {
        if (nome == null || nome.trim().isEmpty()) {
            _mensagemErro.setValue("O nome da categoria é obrigatório.");
            return;
        }

        // Validação Visual (se é novo cadastro, exige escolha)
        if (idEdicao == null && _iconeSelecionado.getValue() == -1 && _imagemUri.getValue() == null) {
            _mensagemErro.setValue("Selecione um ícone ou uma foto.");
            return;
        }

        _carregando.setValue(true);

        // Monta o objeto
        Categoria categoria = new Categoria();
        categoria.setId(idEdicao);
        categoria.setNome(nome);
        categoria.setDescricao(descricao);
        categoria.setAtiva(ativa);
        categoria.setTipoEnum(tipoCategoria);
        // Se tiver ícone selecionado, usa ele. Se for foto (-1), o repository zera lá.
        categoria.setIndexIcone(_iconeSelecionado.getValue() != null ? _iconeSelecionado.getValue() : 0);

        // Chama Repository (que decide se faz upload ou não)
        repository.salvar(categoria, _imagemUri.getValue(), new CategoriaCatalogoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                _carregando.setValue(false);
                _mensagemSucesso.setValue(msg);
            }

            @Override
            public void onErro(String erro) {
                _carregando.setValue(false);
                _mensagemErro.setValue(erro);
            }
        });
    }

    // --- EXCLUIR (NOVO MÉTODO) ---
    public void excluir() {
        if (idEdicao == null) {
            _mensagemErro.setValue("Erro: Tentando excluir categoria inexistente.");
            return;
        }

        _carregando.setValue(true);

        repository.excluir(idEdicao, new CategoriaCatalogoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                _carregando.setValue(false);
                _mensagemSucesso.setValue(msg); // A Activity observa isso e fecha a tela
            }

            @Override
            public void onErro(String erro) {
                _carregando.setValue(false);
                _mensagemErro.setValue(erro);
            }
        });
    }
}