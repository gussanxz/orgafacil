package com.gussanxz.orgafacil.ui.contas.categorias;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.gussanxz.orgafacil.data.model.Categoria;
import com.gussanxz.orgafacil.data.repository.CategoriaRepository;

public class CadastroCategoriaViewModel extends ViewModel {

    private final CategoriaRepository repository;

    // Estados Observáveis (A Activity "assiste" essas variáveis)
    private final MutableLiveData<Integer> _iconeSelecionado = new MutableLiveData<>(0); // Começa com o 1º ícone
    public LiveData<Integer> iconeSelecionado = _iconeSelecionado;

    private final MutableLiveData<Uri> _imagemUri = new MutableLiveData<>(null);
    public LiveData<Uri> imagemUri = _imagemUri;

    private final MutableLiveData<String> _mensagemSucesso = new MutableLiveData<>();
    public LiveData<String> mensagemSucesso = _mensagemSucesso;

    private final MutableLiveData<String> _mensagemErro = new MutableLiveData<>();
    public LiveData<String> mensagemErro = _mensagemErro;

    // Dados Internos
    private Categoria.Tipo tipoCategoria = Categoria.Tipo.DESPESA;
    private String idEdicao = null;

    public CadastroCategoriaViewModel() {
        this.repository = new CategoriaRepository();
    }

    // --- AÇÕES DE SELEÇÃO (Lógica de Exclusividade) ---

    public void selecionarIcone(int index) {
        _iconeSelecionado.setValue(index);
        _imagemUri.setValue(null); // Regra: Se escolheu ícone, zera a foto
    }

    public void selecionarFoto(Uri uri) {
        _imagemUri.setValue(uri);
        _iconeSelecionado.setValue(-1); // Regra: Se escolheu foto, zera o ícone
    }

    // --- CONFIGURAÇÃO INICIAL ---

    public void definirContexto(String tipoString) {
        if (tipoString != null) {
            try {
                this.tipoCategoria = Categoria.Tipo.valueOf(tipoString);
            } catch (IllegalArgumentException e) {
                this.tipoCategoria = Categoria.Tipo.DESPESA;
            }
        }
    }

    public void carregarDadosEdicao(String id, String nome, String desc, boolean ativa, String urlFoto, int indexIcone) {
        this.idEdicao = id;
        // Se tivesse lógica de carregar foto da URL, faríamos aqui
        if (urlFoto != null && !urlFoto.isEmpty()) {
            // Lógica futura para URL
        } else {
            selecionarIcone(indexIcone);
        }
    }

    public Categoria.Tipo getTipoCategoria() {
        return tipoCategoria;
    }

    // --- SALVAR ---

    public void salvar(String nome, String descricao, boolean ativa) {
        if (nome == null || nome.isEmpty()) {
            _mensagemErro.setValue("O nome da categoria é obrigatório.");
            return;
        }

        // Validação: Tem que ter visual
        if (_iconeSelecionado.getValue() == -1 && _imagemUri.getValue() == null) {
            _mensagemErro.setValue("Selecione um ícone ou uma foto da galeria.");
            return;
        }

        // TODO: Aqui entraria o Upload da Imagem para o Storage se (_imagemUri != null)
        // Como ainda não temos o Storage, vamos salvar simulando que é ícone ou URL futura

        Categoria categoria = new Categoria(
                null,
                nome,
                descricao,
                _iconeSelecionado.getValue() != null ? _iconeSelecionado.getValue() : 0,
                null, // URL da imagem (seria preenchida após upload)
                ativa,
                tipoCategoria
        );
        categoria.setId(idEdicao);

        repository.salvar(categoria, new CategoriaRepository.CategoriaCallback() {
            @Override
            public void onSucesso(String mensagem) {
                _mensagemSucesso.postValue(mensagem);
            }

            @Override
            public void onErro(String erro) {
                _mensagemErro.postValue(erro);
            }
        });
    }
}