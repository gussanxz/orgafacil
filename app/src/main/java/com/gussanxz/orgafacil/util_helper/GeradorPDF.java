package com.gussanxz.orgafacil.funcionalidades.contas.exportacao;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.enums.TipoCategoriaContas;
import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.MoedaHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeradorPDF {

    public interface GeradorPdfCallback {
        void onSucesso(File arquivoGerado);
        void onErro(String erro);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Constantes de layout do A4
    private static final int LARGURA_PAGINA = 595;
    private static final int ALTURA_PAGINA = 842;
    private static final int MARGEM_INFERIOR = 800; // Ponto de corte para criar nova página

    public static void exportar(Context context, String periodo, List<MovimentacaoModel> movimentacoes, GeradorPdfCallback callback) {
        executor.execute(() -> {
            try {
                PdfDocument document = new PdfDocument();
                int numeroPagina = 1;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(LARGURA_PAGINA, ALTURA_PAGINA, numeroPagina).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();

                // --- 1. CÁLCULO DO RESUMO FINANCEIRO ---
                long totalReceitas = 0;
                long totalDespesas = 0;

                for (MovimentacaoModel mov : movimentacoes) {
                    if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                        totalReceitas += mov.getValor();
                    } else {
                        totalDespesas += mov.getValor();
                    }
                }
                long saldoPeriodo = totalReceitas - totalDespesas;

                // --- 2. CABEÇALHO DO DOCUMENTO ---
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(24);
                paint.setColor(Color.parseColor("#2E7D32"));
                canvas.drawText("Extrato OrgaFacil", 50, 60, paint);

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(14);
                paint.setColor(Color.DKGRAY);
                canvas.drawText("Período: " + periodo, 50, 85, paint);

                // --- 3. BLOCO DE RESUMO ---
                paint.setColor(Color.parseColor("#F5F5F5"));
                canvas.drawRoundRect(new RectF(50, 100, 545, 160), 8, 8, paint);

                paint.setColor(Color.BLACK);
                paint.setTextSize(12);

                String strReceitas = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalReceitas));
                String strDespesas = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(totalDespesas));
                String strSaldo    = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(saldoPeriodo));

                canvas.drawText("Receitas:", 60, 125, paint);
                paint.setColor(Color.parseColor("#2E7D32"));
                canvas.drawText(strReceitas, 120, 125, paint);

                paint.setColor(Color.BLACK);
                canvas.drawText("Despesas:", 220, 125, paint);
                paint.setColor(Color.parseColor("#D32F2F"));
                canvas.drawText(strDespesas, 285, 125, paint);

                paint.setColor(Color.BLACK);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("Saldo:", 390, 125, paint);

                paint.setColor(saldoPeriodo >= 0 ? Color.parseColor("#2E7D32") : Color.parseColor("#D32F2F"));
                canvas.drawText(strSaldo, 435, 125, paint);

                // --- 4. LISTAGEM COM PAGINAÇÃO ---
                int yPosition = 200;
                desenharTitulosColunas(canvas, paint, yPosition);
                yPosition += 25;

                // Cenário: Lista Vazia
                if (movimentacoes.isEmpty()) {
                    paint.setColor(Color.GRAY);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                    canvas.drawText("Nenhuma movimentação registrada neste período.", 50, yPosition, paint);
                } else {
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

                    for (MovimentacaoModel mov : movimentacoes) {
                        // Verifica se precisa quebrar a página
                        if (yPosition > MARGEM_INFERIOR) {
                            document.finishPage(page); // Fecha a página atual

                            numeroPagina++;
                            pageInfo = new PdfDocument.PageInfo.Builder(LARGURA_PAGINA, ALTURA_PAGINA, numeroPagina).create();
                            page = document.startPage(pageInfo); // Abre a nova página
                            canvas = page.getCanvas();

                            yPosition = 60; // Volta para o topo da nova página
                            desenharTitulosColunas(canvas, paint, yPosition);
                            yPosition += 25;
                            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        }

                        // Formatação e Truncamento de Textos (Cenário de textos longos)
                        String dataStr = mov.getData_movimentacao() != null
                                ? dateFormat.format(mov.getData_movimentacao().toDate()) : "--/--/----";

                        String descricao = mov.getDescricao() != null ? mov.getDescricao() : "";
                        if (descricao.length() > 25) descricao = descricao.substring(0, 22) + "...";

                        String categoria = mov.getCategoria_nome() != null ? mov.getCategoria_nome() : "";
                        if (categoria.length() > 18) categoria = categoria.substring(0, 15) + "...";

                        String valorFormatado = MoedaHelper.formatarParaBRL(MoedaHelper.centavosParaDouble(mov.getValor()));

                        // Desenhando os dados da linha
                        paint.setColor(Color.BLACK);
                        paint.setTextAlign(Paint.Align.LEFT); // Alinhamento padrão

                        canvas.drawText(dataStr, 50, yPosition, paint);
                        canvas.drawText(descricao, 120, yPosition, paint);
                        canvas.drawText(categoria, 290, yPosition, paint);

                        // Lógica de Cor e Alinhamento à Direita para o Dinheiro
                        if (mov.getTipoEnum() == TipoCategoriaContas.RECEITA) {
                            paint.setColor(Color.parseColor("#2E7D32"));
                        } else {
                            paint.setColor(Color.parseColor("#D32F2F"));
                        }

                        paint.setTextAlign(Paint.Align.RIGHT); // Ancora o texto pela direita
                        canvas.drawText(valorFormatado, 545, yPosition, paint); // 545 é o limite direito da tabela

                        paint.setTextAlign(Paint.Align.LEFT); // Reseta o alinhamento para a próxima linha

                        yPosition += 20;
                    }
                }

                // Finaliza a última página desenhada
                document.finishPage(page);

                // --- 5. SALVAMENTO DO ARQUIVO NO CACHE ---
                File pastaCache = new File(context.getCacheDir(), "extratos");
                if (!pastaCache.exists()) pastaCache.mkdirs();

                File arquivoPdf = new File(pastaCache, "Extrato_OrgaFacil_" + System.currentTimeMillis() + ".pdf");
                document.writeTo(new FileOutputStream(arquivoPdf));
                document.close();

                // Devolve o sucesso para a Thread Principal
                mainHandler.post(() -> callback.onSucesso(arquivoPdf));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onErro("Falha ao salvar o documento: " + e.getMessage()));
            } catch (Exception e) {
                // Cenário: Captura genérica para evitar que o app feche por erros inesperados de memória
                mainHandler.post(() -> callback.onErro("Erro inesperado ao gerar PDF: " + e.getMessage()));
            }
        });
    }

    // Método auxiliar para evitar repetição de código no cabeçalho das páginas
    private static void desenharTitulosColunas(Canvas canvas, Paint paint, int yPosition) {
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.drawText("Data", 50, yPosition, paint);
        canvas.drawText("Descrição", 120, yPosition, paint);
        canvas.drawText("Categoria", 290, yPosition, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Valor", 545, yPosition, paint);

        paint.setTextAlign(Paint.Align.LEFT); // Reseta

        // Linha sublinhada do cabeçalho
        paint.setStrokeWidth(1f);
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(50, yPosition + 5, 545, yPosition + 5, paint);
    }
}