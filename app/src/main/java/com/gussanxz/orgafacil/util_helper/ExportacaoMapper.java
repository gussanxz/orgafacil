package com.gussanxz.orgafacil.util_helper;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.dados.model.MovimentacaoModel;
import com.gussanxz.orgafacil.util_helper.MoedaHelper; // Importando seu helper!

public class ExportacaoMapper {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static String converterParaLinhaCsv(MovimentacaoModel model) {
        String dataStr = "";
        if (model.getData_movimentacao() != null) {
            dataStr = dateFormat.format(model.getData_movimentacao().toDate());
        }

        // Usamos seu Helper para converter os centavos em double!
        double valorDecimal = MoedaHelper.centavosParaDouble(model.getValor());

        // Formatamos para garantir que tenha 2 casas decimais e separador com ponto (padrão CSV/Excel em inglês)
        String valorFormatado = String.format(Locale.US, "%.2f", valorDecimal);

        String categoria = model.getCategoria_nome() != null ? model.getCategoria_nome() : "Sem Categoria";
        String descricao = model.getDescricao() != null ? model.getDescricao() : "";
        String tipo = model.getTipoEnum() != null ? model.getTipoEnum().name() : "INDEFINIDO";

        // A descrição fica entre aspas para evitar que uma vírgula no texto quebre as colunas do CSV
        return String.format(Locale.getDefault(), "%s,%s,\"%s\",%s,%s\n",
                dataStr, categoria, descricao, valorFormatado, tipo);
    }
}