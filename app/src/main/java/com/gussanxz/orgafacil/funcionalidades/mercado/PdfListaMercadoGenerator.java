package com.gussanxz.orgafacil.funcionalidades.mercado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RF08 – Gera um arquivo PDF legível da lista de mercado.
 * RN01 – todos os valores são divididos por 100 SOMENTE aqui, na camada de apresentação.
 */
public class PdfListaMercadoGenerator {

    private static final int PAGE_WIDTH  = 595;  // A4 em pontos (72dpi)
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN      = 40;

    /**
     * Gera o PDF e retorna o File resultante.
     *
     * @param context   contexto para acesso ao cache dir
     * @param itens     lista completa de itens
     * @return          File do PDF gerado
     * @throws IOException se não for possível escrever o arquivo
     */
    public static File gerar(Context context, List<ItemMercado> itens) throws IOException {
        PdfDocument document = new PdfDocument();

        // Configuração da página
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Paints
        Paint paintTitulo = new Paint();
        paintTitulo.setColor(Color.parseColor("#1A237E"));
        paintTitulo.setTextSize(22f);
        paintTitulo.setFakeBoldText(true);

        Paint paintSubtitulo = new Paint();
        paintSubtitulo.setColor(Color.parseColor("#555555"));
        paintSubtitulo.setTextSize(11f);

        Paint paintHeader = new Paint();
        paintHeader.setColor(Color.WHITE);
        paintHeader.setTextSize(12f);
        paintHeader.setFakeBoldText(true);

        Paint paintHeaderBg = new Paint();
        paintHeaderBg.setColor(Color.parseColor("#3F51B5"));

        Paint paintItem = new Paint();
        paintItem.setColor(Color.parseColor("#212121"));
        paintItem.setTextSize(11f);

        Paint paintItemAlt = new Paint();
        paintItemAlt.setColor(Color.parseColor("#F5F5F5"));

        Paint paintValor = new Paint();
        paintValor.setColor(Color.parseColor("#1B5E20"));
        paintValor.setTextSize(11f);
        paintValor.setFakeBoldText(true);

        Paint paintDivisor = new Paint();
        paintDivisor.setColor(Color.parseColor("#E0E0E0"));
        paintDivisor.setStrokeWidth(1f);

        Paint paintTotal = new Paint();
        paintTotal.setColor(Color.parseColor("#1A237E"));
        paintTotal.setTextSize(13f);
        paintTotal.setFakeBoldText(true);

        float y = MARGIN;

        // ─── Cabeçalho ──────────────────────────────────────────────────────
        canvas.drawText("OrgaFácil — Lista de Mercado", MARGIN, y, paintTitulo);
        y += 20;

        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        canvas.drawText("Gerado em: " + data, MARGIN, y, paintSubtitulo);
        y += 24;

        // Linha divisora
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivisor);
        y += 12;

        // ─── Header da tabela ────────────────────────────────────────────────
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintHeaderBg);
        canvas.drawText("Produto",      MARGIN + 6,             y + 14, paintHeader);
        canvas.drawText("Categoria",    MARGIN + 200,           y + 14, paintHeader);
        canvas.drawText("Qtd",          MARGIN + 330,           y + 14, paintHeader);
        canvas.drawText("Unitário",     MARGIN + 370,           y + 14, paintHeader);
        canvas.drawText("Subtotal",     PAGE_WIDTH - MARGIN - 70, y + 14, paintHeader);
        y += 22;

        // ─── Linhas de itens ─────────────────────────────────────────────────
        int totalCentavos          = 0;
        int totalCarrinhoCentavos  = 0;
        int totalItens             = itens.size();

        for (int i = 0; i < itens.size(); i++) {
            ItemMercado item = itens.get(i);

            // Fundo alternado
            if (i % 2 == 0) {
                Paint bgAlt = new Paint();
                bgAlt.setColor(Color.parseColor("#F8F9FF"));
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18, bgAlt);
            }

            // Indicador de carrinho
            Paint paintCheck = new Paint();
            paintCheck.setColor(item.isNoCarrinho()
                    ? Color.parseColor("#43A047") : Color.parseColor("#BDBDBD"));
            paintCheck.setTextSize(10f);
            canvas.drawText(item.isNoCarrinho() ? "✓" : "○", MARGIN + 1, y + 13, paintCheck);

            // Dados (RN01: divisão por 100 aqui, na camada de apresentação)
            long subtotalCentavos = item.getSubtotalCentavos();
            totalCentavos += (int) Math.min(subtotalCentavos, Integer.MAX_VALUE);
            if (item.isNoCarrinho()) {
                totalCarrinhoCentavos += (int) Math.min(subtotalCentavos, Integer.MAX_VALUE);
            }

            Paint pItem = new Paint();
            pItem.setColor(item.isNoCarrinho()
                    ? Color.parseColor("#9E9E9E") : Color.parseColor("#212121"));
            pItem.setTextSize(11f);

            canvas.drawText(truncar(item.getNome(), 28),
                    MARGIN + 14,                y + 13, pItem);
            canvas.drawText(truncar(item.getCategoria(), 16),
                    MARGIN + 200,               y + 13, pItem);
            canvas.drawText(String.valueOf(item.getQuantidade()),
                    MARGIN + 340,               y + 13, pItem);
            canvas.drawText(formatarMoeda(item.getValorCentavos()),
                    MARGIN + 365,               y + 13, pItem);
            canvas.drawText(formatarMoeda((int) subtotalCentavos),
                    PAGE_WIDTH - MARGIN - 70,   y + 13, paintValor);

            y += 20;

            // Nova página se necessário
            if (y > PAGE_HEIGHT - 80) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT,
                        document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = MARGIN;
            }
        }

        // ─── Totais finais ───────────────────────────────────────────────────
        y += 10;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivisor);
        y += 16;

        canvas.drawText("Total de itens: " + totalItens,
                MARGIN, y, paintSubtitulo);
        y += 18;

        canvas.drawText("Total da Lista: " + formatarMoeda(totalCentavos),
                MARGIN, y, paintTotal);
        y += 18;

        canvas.drawText("Total no Carrinho: " + formatarMoeda(totalCarrinhoCentavos),
                MARGIN, y, paintTotal);

        document.finishPage(page);

        // ─── Salvar arquivo ──────────────────────────────────────────────────
        File outputDir = new File(context.getCacheDir(), "pdf");
        if (!outputDir.exists()) outputDir.mkdirs();

        String nomeArquivo = "lista_mercado_"
                + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date())
                + ".pdf";
        File outputFile = new File(outputDir, nomeArquivo);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }

        return outputFile;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** RN01 – divisão por 100 na camada de apresentação. */
    private static String formatarMoeda(int centavos) {
        double valor = centavos / 100.0;
        return String.format(Locale.getDefault(), "R$ %,.2f", valor)
                .replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }
}