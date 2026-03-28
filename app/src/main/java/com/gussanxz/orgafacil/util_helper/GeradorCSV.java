package com.gussanxz.orgafacil.util_helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeradorCSV {

    // ── interface de callback ───────────────────────────────────────────────
    public interface GeradorCallback {
        void onSucesso(File arquivoGerado);
        void onErro(String erro);
    }

    // Usamos um Executor com uma única thread em background para não sobrecarregar o app
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // O Handler serve para devolver a resposta para a Thread Principal (UI) e podermos atualizar a tela
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void exportar(Context context, List<MovimentacaoModel> movimentacoes, GeradorCallback callback) {
        // Tudo o que está dentro do executor.execute() vai rodar "por debaixo dos panos"
        executor.execute(() -> {
            try {
                // 1. Criar a pasta temporária no cache do app
                File pastaCache = new File(context.getCacheDir(), "extratos");
                if (!pastaCache.exists()) {
                    pastaCache.mkdirs();
                }

                // 2. Definir o nome do arquivo (usamos o timestamp para nunca dar conflito de nome)
                String nomeArquivo = "Extrato_OrgaFacil_" + System.currentTimeMillis() + ".csv";
                File arquivoCSV = new File(pastaCache, nomeArquivo);

                // 3. Preparar a escrita do arquivo
                FileWriter writer = new FileWriter(arquivoCSV);

                // 4. Escrever o cabeçalho das colunas no CSV
                writer.append("Data,Categoria,Descricao,Valor,Tipo\n");

                // 5. Iterar sobre a lista e usar o Mapper que criamos na etapa anterior
                for (MovimentacaoModel mov : movimentacoes) {
                    // Aqui a mágica da formatação e da divisão do dinheiro (int -> double) acontece
                    String linha = ExportacaoMapper.converterParaLinhaCsv(mov);
                    writer.append(linha);
                }

                // Garantir que tudo foi salvo e fechar o arquivo
                writer.flush();
                writer.close();

                // 6. Devolver o arquivo gerado para a tela através do callback na Main Thread
                mainHandler.post(() -> callback.onSucesso(arquivoCSV));

            } catch (IOException e) {
                // Se der erro (ex: falta de espaço), devolvemos o erro para a Main Thread
                mainHandler.post(() -> callback.onErro("Falha ao criar o arquivo: " + e.getMessage()));
            }
        });
    }
}