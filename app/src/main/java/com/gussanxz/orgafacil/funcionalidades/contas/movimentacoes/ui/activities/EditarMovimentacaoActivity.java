package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.repository.MovimentacaoRepository;
import com.gussanxz.orgafacil.util_helper.DateHelper;

import java.util.Date;

/**
 * EditarMovimentacaoActivity
 *
 * Tela de visualização e edição de uma movimentação existente.
 *
 * ANTES: ~430 linhas com toda a lógica de UI duplicada da Base
 *        (máscara de moeda, seletores, categoria, status, validação).
 *
 * AGORA: ~200 linhas contendo APENAS o que é exclusivo desta tela:
 *   - Modo visualização/edição (campos desabilitados por padrão, FAB alterna)
 *   - FABs com dois estados (lápis → confirmar)
 *   - Header dinâmico com "(parcela x/y)"
 *   - Banner "Ver série completa"
 *   - Dialog de edição em série ("esta e seguintes")
 *   - Dialog de exclusão em série ("esta e seguintes")
 *   - DIRETO_PRA_EDICAO: entra já no modo de edição
 *
 * Tudo o mais (máscara, data, hora, categoria, status, validação, save)
 * é herdado de BaseMovimentacaoActivity sem reimplementação.
 *
 * ── Como a herança funciona aqui ──────────────────────────────────────────
 *
 * getTipo()          → retorna o tipo DO OBJETO CARREGADO (não é estático como
 *                       nas subclasses Despesa/Receita). Funciona porque a Base
 *                       chama carregarModoEdicao() que popula itemEmEdicao antes
 *                       de qualquer chamada a getTipo().
 *
 * getLayoutResId()   → decidido dinamicamente pelo tipo da movimentação.
 *
 * getCategoriaDefault() → não é usado no modo edição (itemEmEdicao já tem categoria).
 *
 * getTituloExclusao() / getMensagemExclusao() → usados pelo btnExcluir da Base,
 *   mas esta tela tem seu próprio confirmarExclusao() com lógica de série,
 *   então o btnExcluir da Base é sobrescrito via onModoEdicaoAlternado().
 */
public class EditarMovimentacaoActivity extends BaseMovimentacaoActivity {

    private static final String TAG = "EditarMovActivity";

    // ── Estado exclusivo desta tela ────────────────────────────────────────────
    private boolean isModoEdicaoAtivo = false;
    private boolean acaoEmAndamento   = false;

    // ── FABs exclusivos desta tela (dois estados: lápis ↔ confirmar) ──────────
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabSuperior;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabInferior;

    // =========================================================================
    // IMPLEMENTAÇÃO DOS MÉTODOS ABSTRATOS DA BASE
    // =========================================================================

    /**
     * O tipo vem do objeto carregado, não é definido estaticamente.
     * itemEmEdicao é populado pela Base em carregarModoEdicao() que roda
     * antes de qualquer chamada a getTipo() no fluxo normal de onCreate().
     */
    @Override
    protected TipoCategoriaContas getTipo() {
        return (itemEmEdicao != null)
                ? itemEmEdicao.getTipoEnum()
                : TipoCategoriaContas.DESPESA; // fallback seguro
    }

    /**
     * O layout é decidido pelo tipo da movimentação.
     * itemEmEdicao é lido do Intent aqui, pois getLayoutResId() é chamado
     * ANTES de carregarModoEdicao() — é o primeiro método chamado pela Base.
     */
    @Override
    protected int getLayoutResId() {
        MovimentacaoModel mov = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            mov = getIntent().getParcelableExtra("movimentacaoSelecionada", MovimentacaoModel.class);
        } else {
            mov = getIntent().getParcelableExtra("movimentacaoSelecionada");
        }
        return (mov != null && mov.getTipoEnum() == TipoCategoriaContas.RECEITA)
                ? R.layout.tela_add_receita
                : R.layout.tela_add_despesa;
    }

    @Override
    protected String getCategoriaDefault() {
        // Nunca usado no modo edição — itemEmEdicao já tem categoria.
        return "";
    }

    @Override
    protected String getTituloExclusao() {
        // Usado como fallback — esta tela usa confirmarExclusao() próprio.
        return "Excluir lançamento";
    }

    @Override
    protected String getMensagemExclusao() {
        return "Deseja realmente excluir? O saldo será corrigido automaticamente.";
    }

    // =========================================================================
    // CICLO DE VIDA
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // A Base cuida de: setContentView, inicializar campos, máscara, data/hora,
        // categoria, status, carregar itemEmEdicao, preencher campos.
        super.onCreate(savedInstanceState);

        if (itemEmEdicao == null) {
            // Se chegou sem movimentação, não há o que editar.
            finish();
            return;
        }

        // Vincula os FABs exclusivos desta tela
        fabSuperior = findViewById(R.id.fabSuperiorSalvarCategoria);
        fabInferior = findViewById(R.id.fabInferiorSalvarCategoria);

        // Configura o header e o banner de série
        configurarHeader();
        configurarBannerSerie();

        // Esconde o painel de recorrência (não aplicável na edição)
        if (layoutRecorrencia != null) layoutRecorrencia.setVisibility(View.GONE);

        // Configura os FABs desta tela
        configurarFabs();

        // Verifica se deve entrar direto no modo edição
        boolean diretoPraEdicao = getIntent().getBooleanExtra("DIRETO_PRA_EDICAO", false);
        alternarModoEdicao(diretoPraEdicao);
    }

    // =========================================================================
    // HEADER E BANNER DE SÉRIE
    // =========================================================================

    private void configurarHeader() {
        TextView textViewHeader = findViewById(R.id.textViewHeader);
        if (textViewHeader == null) return;

        boolean isDespesa = itemEmEdicao.getTipoEnum() == TipoCategoriaContas.DESPESA;
        String titulo = isDespesa ? "Detalhes da Despesa" : "Detalhes da Receita";

        if (itemEmEdicao.getTotal_parcelas() > 1) {
            titulo += " (" + itemEmEdicao.getParcela_atual()
                    + "/" + itemEmEdicao.getTotal_parcelas() + ")";
        }
        textViewHeader.setText(titulo);
    }

    private void configurarBannerSerie() {
        if (itemEmEdicao.getTotal_parcelas() <= 1) return;

        View bannerSerie = findViewById(R.id.bannerVerSerie);
        if (bannerSerie == null) return;

        bannerSerie.setVisibility(View.VISIBLE);

        TextView txtBanner = bannerSerie.findViewById(R.id.textBannerSerie);
        if (txtBanner != null) {
            txtBanner.setText("Parcela " + itemEmEdicao.getParcela_atual()
                    + " de " + itemEmEdicao.getTotal_parcelas()
                    + " — Ver série completa →");
        }
        bannerSerie.setOnClickListener(v -> abrirResumoDaSerie());
    }

    private void abrirResumoDaSerie() {
        if (itemEmEdicao.getRecorrencia_id() == null
                || itemEmEdicao.getRecorrencia_id().isEmpty()) {
            Toast.makeText(this, "Esta parcela não tem série vinculada.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ResumoParcelasActivity.class);
        intent.putExtra("recorrencia_id", itemEmEdicao.getRecorrencia_id());
        intent.putExtra("movimentacaoSelecionada", itemEmEdicao);
        startActivity(intent);
    }

    // =========================================================================
    // FABs E MODO EDIÇÃO
    // =========================================================================

    private void configurarFabs() {
        if (fabSuperior != null) fabSuperior.setOnClickListener(v -> processarAcaoFab());
        if (fabInferior != null) fabInferior.setOnClickListener(v -> processarAcaoFab());
    }

    /**
     * Hook da Base: chamado sempre que o modo de edição é alternado.
     * Aqui habilitamos/desabilitamos os campos e trocamos os ícones dos FABs.
     */
    @Override
    protected void onModoEdicaoAlternado(boolean modoEdicaoAtivo) {
        // Habilita ou desabilita todos os campos de input
        if (campoValor    != null) campoValor.setEnabled(modoEdicaoAtivo);
        if (campoDescricao!= null) campoDescricao.setEnabled(modoEdicaoAtivo);
        if (campoCategoria!= null) {
            campoCategoria.setEnabled(modoEdicaoAtivo);
            campoCategoria.setClickable(modoEdicaoAtivo);
        }
        if (campoData != null) {
            campoData.setEnabled(modoEdicaoAtivo);
            campoData.setClickable(modoEdicaoAtivo);
        }
        if (campoHora != null) {
            campoHora.setEnabled(modoEdicaoAtivo);
            campoHora.setClickable(modoEdicaoAtivo);
        }
        if (switchStatusPago != null) {
            if (modoEdicaoAtivo && campoData != null) {
                try {
                    Date dataAtual = DateHelper.parsearData(campoData.getText().toString());
                    aplicarRegraStatusPorData(dataAtual);
                } catch (Exception e) {
                    switchStatusPago.setEnabled(true);
                }
            } else {
                switchStatusPago.setEnabled(false);
            }
        }

        // Botão excluir só aparece no modo edição
        if (btnExcluir != null) {
            btnExcluir.setVisibility(modoEdicaoAtivo ? View.VISIBLE : View.GONE);
            // Sobrescreve o listener padrão da Base para usar o confirmarExclusao() desta tela
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }

        // Troca o ícone dos FABs
        int icone = modoEdicaoAtivo
                ? R.drawable.ic_confirmar_branco_48
                : R.drawable.ic_lapis_editar_24;
        if (fabSuperior != null) fabSuperior.setImageResource(icone);

        int iconePequeno = modoEdicaoAtivo
                ? R.drawable.ic_confirmar_branco_24
                : R.drawable.ic_lapis_editar_24;
        if (fabInferior != null) fabInferior.setImageResource(iconePequeno);
    }

    /**
     * Alterna o modo edição e notifica a Base via hook.
     */
    private void alternarModoEdicao(boolean habilitar) {
        isModoEdicaoAtivo = habilitar;
        onModoEdicaoAlternado(habilitar);
    }

    /**
     * Lógica do FAB: primeiro clique entra em modo edição, segundo salva.
     */
    private void processarAcaoFab() {
        if (!isModoEdicaoAtivo) {
            alternarModoEdicao(true);
            Toast.makeText(this, "Modo de edição habilitado", Toast.LENGTH_SHORT).show();
            if (campoValor != null) campoValor.requestFocus();
        } else {
            confirmarEdicao();
        }
    }

    // =========================================================================
    // SALVAR — lógica exclusiva desta tela (série, parcelas, etc.)
    // =========================================================================

    /**
     * Botões de salvar do XML chamam este método.
     * Compatível com os IDs usados nos layouts tela_add_despesa e tela_add_receita.
     */
    public void salvarDespesa(View v)   { processarAcaoFab(); }
    public void salvarProventos(View v) { processarAcaoFab(); }

    private void confirmarEdicao() {
        if (acaoEmAndamento) return;
        if (valorCentavosAtual <= 0) {
            if (campoValor != null) campoValor.setError("Preencha um valor válido");
            return;
        }
        acaoEmAndamento = true;

        try {
            // Monta a movimentação nova preservando todos os campos de série da original
            MovimentacaoModel movNova = new MovimentacaoModel();
            movNova.setId(itemEmEdicao.getId());
            movNova.setTipoEnum(itemEmEdicao.getTipoEnum());
            movNova.setData_criacao(itemEmEdicao.getData_criacao());
            movNova.setRecorrencia_id(itemEmEdicao.getRecorrencia_id());
            movNova.setParcela_atual(itemEmEdicao.getParcela_atual());
            movNova.setTotal_parcelas(itemEmEdicao.getTotal_parcelas());
            movNova.setValor(valorCentavosAtual);
            movNova.setDescricao(campoDescricao.getText().toString().trim());
            movNova.setCategoria_nome(campoCategoria.getText().toString());
            movNova.setCategoria_id(categoriaIdSelecionada);

            // Monta data+hora usando o DateHelper centralizado (igual à Base)
            String dataStr = campoData.getText().toString().trim();
            String horaStr = (campoHora != null && !campoHora.getText().toString().isEmpty())
                    ? campoHora.getText().toString().trim()
                    : "00:00";

            Date date = DateHelper.parsearDataHora(dataStr, horaStr);
            movNova.setData_movimentacao(new Timestamp(date));

            boolean ok = switchStatusPago != null
                    ? switchStatusPago.isChecked()
                    : itemEmEdicao.isPago();
            movNova.setPago(ok);

            // Se for parcela de uma série, pergunta se atualiza só esta ou todas as seguintes
            boolean ehSerie = itemEmEdicao.getTotal_parcelas() > 1
                    && itemEmEdicao.getParcela_atual() < itemEmEdicao.getTotal_parcelas();

            if (ehSerie) {
                new AlertDialog.Builder(this)
                        .setTitle("Atualizar Série")
                        .setMessage("Deseja aplicar as alterações apenas a esta parcela ou a todas as seguintes também?")
                        .setPositiveButton("Esta e Seguintes", (d, w) -> enviarEdicaoAoBanco(movNova, true))
                        .setNegativeButton("Apenas Esta",      (d, w) -> enviarEdicaoAoBanco(movNova, false))
                        .setNeutralButton("Cancelar",          (d, w) -> acaoEmAndamento = false)
                        .show();
            } else {
                enviarEdicaoAoBanco(movNova, false);
            }

        } catch (Exception e) {
            acaoEmAndamento = false;
            Log.e(TAG, "Erro ao montar movimentação para edição", e);
            Toast.makeText(this, "Erro inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void enviarEdicaoAoBanco(MovimentacaoModel movNova, boolean todasSeguintes) {
        repository.editarMultiplos(itemEmEdicao, movNova, todasSeguintes,
                new MovimentacaoRepository.Callback() {
                    @Override public void onSucesso(String msg) {
                        Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                    @Override public void onErro(String erro) {
                        acaoEmAndamento = false;
                        Toast.makeText(EditarMovimentacaoActivity.this,
                                "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================================
    // EXCLUSÃO — lógica exclusiva desta tela (série)
    // =========================================================================

    private void confirmarExclusao() {
        boolean ehSerie = itemEmEdicao.getTotal_parcelas() > 1
                && itemEmEdicao.getParcela_atual() < itemEmEdicao.getTotal_parcelas();

        if (ehSerie) {
            new AlertDialog.Builder(this)
                    .setTitle("Excluir Lançamento Repetido")
                    .setMessage("Como deseja tratar a exclusão desta conta?")
                    .setPositiveButton("Esta e Seguintes", (d, w) -> enviarExclusaoAoBanco(true))
                    .setNegativeButton("Apenas Esta", (d, w) -> {
                        Toast.makeText(this,
                                "Apenas este mês será removido. As próximas estão mantidas.",
                                Toast.LENGTH_LONG).show();
                        enviarExclusaoAoBanco(false);
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Excluir Lançamento")
                    .setMessage("Deseja realmente excluir? O saldo será corrigido automaticamente.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Excluir", (d, w) -> enviarExclusaoAoBanco(false))
                    .show();
        }
    }

    private void enviarExclusaoAoBanco(boolean todasSeguintes) {
        repository.excluirMultiplos(itemEmEdicao, todasSeguintes,
                new MovimentacaoRepository.Callback() {
                    @Override public void onSucesso(String msg) {
                        Toast.makeText(EditarMovimentacaoActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                    @Override public void onErro(String erro) {
                        Toast.makeText(EditarMovimentacaoActivity.this,
                                erro, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================================
    // NAVEGAÇÃO
    // =========================================================================

    @Override
    public void retornarPrincipal(View view) {
        if (acaoEmAndamento) return;
        finish();
    }
}