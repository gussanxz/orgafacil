package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
 *   - FABs com dois estados (lápis → confirmar), texto e cor incluídos
 *   - Header dinâmico com "(parcela x/y)"
 *   - Banner "Ver série completa"
 *   - Dialog de edição em série ("esta e seguintes")
 *   - Dialog de exclusão em série ("esta e seguintes")
 *   - DIRETO_PRA_EDICAO: entra já no modo de edição
 *
 * Tudo o mais (máscara, data, hora, categoria, status, validação, save)
 * é herdado de BaseMovimentacaoActivity sem reimplementação.
 *
 * ── Comportamento dos botões ──────────────────────────────────────────────
 *
 * Modo visualização (isModoEdicaoAtivo = false):
 *   fabSuperior  → ícone lápis, cor neutra semitransparente no header
 *   fabInferior  → ícone lápis, texto "Editar", cor cinza neutra
 *
 * Modo edição (isModoEdicaoAtivo = true):
 *   fabSuperior  → ícone confirmar, cor semitransparente no header
 *   fabInferior  → ícone confirmar, texto "Salvar", cor accent da movimentação
 *
 * ── CORREÇÃO BUG 4 ────────────────────────────────────────────────────────
 *
 * ANTES: getLayoutResId() desserializava o Parcelable do Intent de forma
 * independente apenas para decidir o layout, criando uma instância descartada.
 * A Base então desserializava novamente em verificarModoEdicao(), produzindo
 * uma segunda instância diferente atribuída a itemEmEdicao.
 * Resultado: dois objetos distintos para a mesma movimentação, com risco de
 * inconsistência em edge cases de memória baixa.
 *
 * DEPOIS: getLayoutResId() lê o Parcelable UMA ÚNICA VEZ e armazena em
 * itemEmEdicao imediatamente (campo herdado de BaseMovimentacaoActivity).
 * A Base detecta que itemEmEdicao já está preenchido e não lê o Intent de
 * novo — a desserialização ocorre exatamente uma vez durante todo o ciclo.
 *
 * Para isso funcionar, BaseMovimentacaoActivity.carregarModoEdicao() foi
 * ajustado para verificar se itemEmEdicao já tem valor antes de ler o Intent.
 * Ver comentário em carregarModoEdicao() na Base.
 */
public class EditarMovimentacaoActivity extends BaseMovimentacaoActivity {

    private static final String TAG = "EditarMovActivity";

    // ── Estado exclusivo desta tela ────────────────────────────────────────────
    private boolean isModoEdicaoAtivo = false;
    private boolean acaoEmAndamento   = false;

    // ── FABs exclusivos desta tela ─────────────────────────────────────────────
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabSuperior;
    private ExtendedFloatingActionButton fabInferior;

    // =========================================================================
    // IMPLEMENTAÇÃO DOS MÉTODOS ABSTRATOS DA BASE
    // =========================================================================

    @Override
    protected TipoCategoriaContas getTipo() {
        return (itemEmEdicao != null)
                ? itemEmEdicao.getTipoEnum()
                : TipoCategoriaContas.DESPESA;
    }

    /**
     * CORREÇÃO BUG 4 — leitura única do Parcelable.
     *
     * Este método é chamado pela Base em setContentView(), antes de
     * verificarModoEdicao(). Aproveitamos essa chamada obrigatória para
     * popular itemEmEdicao de uma vez, eliminando a segunda leitura do Intent
     * que ocorreria em carregarModoEdicao() da Base.
     *
     * A Base foi ajustada para respeitar itemEmEdicao já preenchido:
     * se itemEmEdicao != null ao entrar em carregarModoEdicao(), ela não
     * chama getParcelableExtra() novamente.
     */
    @Override
    protected int getLayoutResId() {
        // Só lê do Intent se ainda não foi populado (ex: rotação de tela
        // onde onCreate() é chamado novamente mas o campo já foi salvo).
        if (itemEmEdicao == null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                itemEmEdicao = getIntent().getParcelableExtra(
                        "movimentacaoSelecionada", MovimentacaoModel.class);
            } else {
                itemEmEdicao = getIntent().getParcelableExtra("movimentacaoSelecionada");
            }
        }

        return (itemEmEdicao != null && itemEmEdicao.getTipoEnum() == TipoCategoriaContas.RECEITA)
                ? R.layout.tela_add_receita
                : R.layout.tela_add_despesa;
    }

    @Override
    protected String getCategoriaDefault() {
        return "";
    }

    @Override
    protected String getTituloExclusao() {
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
        // Nota: super.onCreate() já chama getLayoutResId() internamente,
        // que por sua vez popula itemEmEdicao. Quando chegarmos na linha
        // abaixo do super, itemEmEdicao já está disponível.
        super.onCreate(savedInstanceState);

        if (itemEmEdicao == null) {
            finish();
            return;
        }

        fabSuperior = findViewById(R.id.fabSuperiorSalvarCategoria);
        fabInferior = findViewById(R.id.fabInferiorSalvarCategoria);

        configurarHeader();
        configurarBannerSerie();

        if (layoutRecorrencia != null) layoutRecorrencia.setVisibility(View.GONE);

        configurarFabs();

        boolean diretoPraEdicao = getIntent().getBooleanExtra("DIRETO_PRA_EDICAO", false);

        // Modo somente visualização — aberto pelo Top 5 de relatórios.
        // Oculta os FABs e o banner de série para que o usuário veja os
        // dados mas não possa editar ou navegar para a série neste contexto.
        boolean apenasVisualizacao = getIntent().getBooleanExtra("MODO_APENAS_VISUALIZACAO", false);
        if (apenasVisualizacao) {
            if (fabSuperior != null) fabSuperior.setVisibility(View.GONE);
            if (fabInferior != null) fabInferior.setVisibility(View.GONE);
            // Oculta a label solta "Recorrência" e o card inteiro — o card de
            // recorrência não faz sentido em modo somente visualização.
            // O bannerVerSerie fica visível para que o usuário possa navegar
            // para o resumo de parcelas normalmente.
            View labelRecorrencia = findViewById(R.id.labelRecorrencia);
            View cardRecorrencia  = findViewById(R.id.cardRecorrencia);
            if (labelRecorrencia != null) labelRecorrencia.setVisibility(View.GONE);
            if (cardRecorrencia  != null) cardRecorrencia.setVisibility(View.GONE);
            // Garante modo visualização — campos desabilitados, sem botão excluir
            alternarModoEdicao(false);
            return;
        }

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
     *
     * FIX 2 — comportamento dos botões por modo:
     *   Visualizando → fabInferior: ícone lápis, texto "Editar", cor neutra cinza
     *   Editando     → fabInferior: ícone confirmar, texto "Salvar", cor accent
     *   fabSuperior  → apenas troca ícone (está no header colorido)
     */
    @Override
    protected void onModoEdicaoAlternado(boolean modoEdicaoAtivo) {
        // Habilita ou desabilita todos os campos de input
        if (campoValor     != null) campoValor.setEnabled(modoEdicaoAtivo);
        if (campoDescricao != null) campoDescricao.setEnabled(modoEdicaoAtivo);
        if (campoCategoria != null) {
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
            btnExcluir.setOnClickListener(v -> confirmarExclusao());
        }

        // ── fabSuperior: apenas troca o ícone ─────────────────────────────────
        int iconeSuperior = modoEdicaoAtivo
                ? R.drawable.ic_confirmar_branco_48
                : R.drawable.ic_lapis_editar_24;
        if (fabSuperior != null) fabSuperior.setImageResource(iconeSuperior);

        // ── fabInferior: troca ícone + texto + cor de fundo ───────────────────
        if (fabInferior != null) {
            if (modoEdicaoAtivo) {
                // Modo edição: destaque total — cor accent + "Salvar"
                fabInferior.setIconResource(R.drawable.ic_confirmar_branco_24);
                fabInferior.setText("Salvar");

                // Cor accent depende do tipo da movimentação
                int corAccent = (itemEmEdicao != null
                        && itemEmEdicao.getTipoEnum() == TipoCategoriaContas.RECEITA)
                        ? R.color.colorAccentProventos
                        : R.color.colorAccentDespesa;
                fabInferior.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, corAccent)));
            } else {
                // Modo visualização: neutro — cinza + "Editar"
                fabInferior.setIconResource(R.drawable.ic_lapis_editar_24);
                fabInferior.setText("Editar");
                fabInferior.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryDark)));
            }
        }
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